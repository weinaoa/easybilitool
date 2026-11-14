/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.TextView
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.Collections
import java.util.function.Supplier

/** Only positions the native hold toast. It never changes playback or window
 * insets and uses the decor's unconsumed insets rather than content insets. */
internal object PlayerSpeedTipLayout {
    private val bindings = Collections.synchronizedMap(WeakHashMap<View, NativeTip>())
    private val reports = WeakHashMap<View, String>()
    private val textPattern = Regex("(?:[0-9.]+\\s*)?倍速播放中|(?:锁定)?倍速中")

    fun install(loader: ClassLoader, enabled: Supplier<Boolean>) {
        val prefix = "com.bilibili.playerbizcommonv2.widget.speed."
        var count = 0
        for (name in ('a'..'z').map { prefix+it } + (prefix+"TripleSpeedFunctionWidgetV2")) {
            val type = runCatching { loader.loadClass(name) }.getOrNull() ?: continue
            if (type.superclass?.name != "tv.danmaku.biliplayerv2.widget.AbsFunctionWidget") continue
            for (factory in type.declaredMethods.filter { View::class.java.isAssignableFrom(it.returnType) &&
                it.parameterTypes.contentEquals(arrayOf(Context::class.java)) }) {
                HookRuntime.hookMethod(factory, object : MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val root = param.result as? View ?: return
                        // V2 renders the native speed state in a single ComposeView;
                        // older widgets contain a TextView with a drawable background.
                        val bubble = (if (type.name == prefix+"TripleSpeedFunctionWidgetV2" &&
                            root.javaClass.name == "androidx.compose.ui.platform.ComposeView") root else bubble(root)) ?: return
                        if (!bindings.containsKey(bubble)) bindings[bubble] = NativeTip(bubble, enabled)
                    }
                })
                count++
            }
        }
        HookRuntime.log("PLAYER_TIP_LAYOUT_READY factories=$count")
    }

    private fun bubble(root: View): View? {
        fun find(view: View): TextView? {
            if (view is TextView && view.text.toString().matches(textPattern)) return view
            if (view is ViewGroup) for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
            return null
        }
        var view: View = find(root) ?: return null
        while (view !== root && view.background == null) view = view.parent as? View ?: return null
        return view
    }

    private fun activity(context: Context): Activity? {
        var current = context
        repeat(12) {
            if (current is Activity) return current as Activity
            val next = (current as? ContextWrapper)?.baseContext ?: return null
            if (next === current) return null
            current = next
        }
        return null
    }

    private fun bounds(view: View): PlayerTipGeometry.Bounds {
        val p = IntArray(2); view.getLocationOnScreen(p)
        return PlayerTipGeometry.Bounds(p[0], p[1], p[0]+view.width, p[1]+view.height)
    }

    private fun position(anchor: View, width: Int, height: Int): Pair<PlayerTipGeometry.Position, String>? {
        val window = activity(anchor.context)?.window
        val decor = window?.decorView ?: anchor.rootView
        val viewport = bounds(anchor)
        val frame = bounds(decor)
        val raw = decor.rootWindowInsets
        val status = if (Build.VERSION.SDK_INT >= 30) raw?.getInsets(WindowInsets.Type.statusBars())?.top ?: 0
            else if ((window?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_FULLSCREEN == 0 &&
                decor.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) raw?.systemWindowInsetTop ?: 0 else 0
        val holes = if (Build.VERSION.SDK_INT >= 28) raw?.displayCutout?.boundingRects.orEmpty().map {
            PlayerTipGeometry.Bounds(frame.left+it.left, frame.top+it.top, frame.left+it.right, frame.top+it.bottom)
        } else emptyList()
        val gap = (10*anchor.resources.displayMetrics.density).toInt()
        val statusBottom = frame.top+status
        val result = PlayerTipGeometry.place(viewport, frame, statusBottom, holes, width, height, gap) ?: return null
        return result to "x=${result.left} y=${result.top} w=$width h=$height status=$statusBottom gap=$gap player=$viewport window=$frame holes=$holes"
    }

    private fun report(view: View, kind: String, detail: String) {
        if (reports.put(view, detail) != detail) DiagnosticLogs.record("PLAYER_TIP_POSITION", "kind=$kind $detail")
    }

    private class NativeTip(view: View, private val enabled: Supplier<Boolean>) :
        View.OnAttachStateChangeListener, ViewTreeObserver.OnPreDrawListener {
        private val target = WeakReference(view)
        private var observer = WeakReference<ViewTreeObserver>(null)
        private var dx = 0f
        private var dy = 0f
        init { view.addOnAttachStateChangeListener(this); if (view.isAttachedToWindow) onViewAttachedToWindow(view) }
        override fun onViewAttachedToWindow(v: View) {
            observer.get()?.takeIf { it.isAlive }?.removeOnPreDrawListener(this)
            observer = WeakReference(v.viewTreeObserver)
            v.viewTreeObserver.addOnPreDrawListener(this)
        }
        override fun onViewDetachedFromWindow(v: View) {
            observer.get()?.takeIf { it.isAlive }?.removeOnPreDrawListener(this)
            restore(v)
        }
        private fun restore(v: View) {
            v.translationX -= dx; v.translationY -= dy
            dx = 0f; dy = 0f
        }
        override fun onPreDraw(): Boolean {
            val view = target.get() ?: return true
            if (!enabled.get() || !view.isShown) { restore(view); return true }
            val anchor = view.parent as? View ?: return true
            val (p, detail) = position(anchor, view.width, view.height) ?: return true
            val current = bounds(view)
            val nextX = dx + p.left-current.left
            val nextY = dy + p.top-current.top
            if (nextX != dx) { view.translationX += nextX-dx; dx = nextX }
            if (nextY != dy) { view.translationY += nextY-dy; dy = nextY }
            report(view, "native", detail)
            return true
        }
    }
}
