/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.app.Activity
import android.app.Application
import android.view.View
import android.view.ViewGroup
import java.lang.reflect.Method

/** 9.16's optional KHome tab host has no native TabHost/background children. */
internal object ComposeHomeHook {
    private var snapshot: Any? = null
    private lateinit var read: Method
    private lateinit var write: Method
    private var extra = 0f
    private var progress = 0f
    private var progressSnapshot: Any? = null
    private val scopes = ThreadLocal.withInitial { java.util.ArrayDeque<ComposeHomeMeasureScope>() }

    fun setInset(dp: Float) {
        val state = snapshot ?: return
        if (kotlin.math.abs(dp - extra) < .01f) return
        extra = dp
        write.invoke(state, dp)
    }

    fun collapse(value: Float) {
        val state = progressSnapshot ?: return
        val next = value.coerceIn(0f, 1f)
        if (kotlin.math.abs(progress - next) < .0001f) return
        progress = next
        write.invoke(state, next)
    }

    fun install(application: Application): Boolean {
        val loader = application.classLoader
        fun type(name: String) = Class.forName(name, false, loader)
        // KHome short class names already existed in 8.95 with other contracts.
        // Gate the adapter on the new host view before resolving those names.
        try { type("tv.danmaku.bili.home.components.bottomtab.WindowAwareComposeView") }
            catch (_: ClassNotFoundException) { return false }
        val outer = type("tv.danmaku.bili.khome.widget.bottomtab.h")
        val inner = type("tv.danmaku.bili.khome.widget.bottomtab.e")
        val modifier = type("androidx.compose.ui.Modifier")
        val composer = type("androidx.compose.runtime.Composer")
        val policy = type("androidx.compose.runtime.SnapshotMutationPolicy")
        val create = type("androidx.compose.runtime.SnapshotStateKt").getMethod("mutableStateOf\$default", Any::class.java, policy, Int::class.javaPrimitiveType, Any::class.java)
        read = type("androidx.compose.runtime.State").getMethod("getValue")
        write = type("androidx.compose.runtime.MutableState").getMethod("setValue", Any::class.java)
        val pad = type("androidx.compose.foundation.layout.PaddingKt").getMethod("padding-qDBjuR0", modifier,
            Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType)
        val scopeMethods = listOf(outer.getDeclaredMethod("invoke", Any::class.java, Any::class.java),
            inner.getDeclaredMethod("invoke", Any::class.java, Any::class.java, Any::class.java))
        val height = type("androidx.compose.foundation.layout.SizeKt").getMethod("height-3ABfNKs", modifier, Float::class.javaPrimitiveType)
        val row = type("androidx.compose.foundation.layout.RowKt").declaredMethods.single { it.name == "rowMeasurePolicy" }
        val materialize = type("androidx.compose.ui.ComposedModifierKt").getMethod("materializeModifier", composer, modifier)
        val motion = ComposeBarMotion(loader)
        snapshot = create.invoke(null, 0f, null, 2, null)
        progressSnapshot = create.invoke(null, 0f, null, 2, null)
        scopeMethods.forEachIndexed { kind, method ->
            HookRuntime.hookMethod(method, object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) {
                    val dp = (read.invoke(snapshot) as Number).toFloat()
                    val collapsed = (read.invoke(progressSnapshot) as Number).toFloat()
                    scopes.get().push(ComposeHomeMeasureScope(kind, dp, collapsed))
                    // The snapshot is read at the host lambda's caller scope. Force
                    // its body to re-evaluate when that caller recomposes.
                    p.args[p.args.lastIndex] = (p.args.last() as Int) or 1
                }
                override fun afterHookedMethod(p: MethodHookParam) { scopes.get().pop() }
            })
        }
        HookRuntime.hookMethod(height, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                val scope = scopes.get().peek() ?: return
                scope.takeMotionFraction()?.let { p.args[0] = motion.apply(p.args[0], it) }
                p.args[1] = scope.height(p.args[1] as Float)
            }
        })
        HookRuntime.hookMethod(row, object : MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) {
                scopes.get().peek()?.rowMeasured()
            }
        })
        HookRuntime.hookMethod(materialize, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                val scope = scopes.get().peek() ?: return
                val dp = scope.takeRowInset()
                if (dp > 0f) p.args[1] = pad.invoke(null, p.args[1], 0f, 0f, 0f, dp)
            }
        })
        return true
    }
}

internal class ComposeHomeNavigation(private val activity: Activity) {
    private var view: View? = null
    private var content: View? = null
    private val avoidance = MainBottomAvoidance()
    fun bind(content: View): Boolean {
        if (activity.javaClass.name != "tv.danmaku.bili.MainActivityV2") return false
        val id = activity.resources.getIdentifier("tab_host", "id", activity.packageName)
        val next = if (id == 0) null else content.findViewById<View>(id)
        if (next?.javaClass?.name != "tv.danmaku.bili.home.components.bottomtab.WindowAwareComposeView") return false
        if (view !== next) { restore(); view = next }
        this.content = content
        return true
    }
    fun apply(inset: Int): Boolean {
        val target = view ?: return false
        ComposeHomeHook.setInset(inset / activity.resources.displayMetrics.density)
        val changed = avoidance.apply(target, content!!, activity.window.decorView.height, inset)
        val parts = ArrayList<String>()
        var current: View? = target
        while (current != null) {
            val v = current
            val at = IntArray(2); v.getLocationInWindow(at)
            val margin = (v.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin
            parts.add("${v.javaClass.simpleName}:${at[1]}+${v.height},p=${v.paddingBottom},m=$margin")
            if (v === content) break
            current = v.parent as? View
        }
        RuntimeStatus.record("MAIN_NAV_COMPOSE inset=$inset window=${activity.window.decorView.height} ${parts.joinToString(";")}")
        return changed
    }
    fun restore() {
        if (view != null) ComposeHomeHook.setInset(0f)
        avoidance.restore()
        view = null
        content = null
    }
}
