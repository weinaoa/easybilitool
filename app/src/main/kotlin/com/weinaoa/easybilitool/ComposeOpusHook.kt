/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.graphics.Rect
import android.os.*
import android.view.*
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.function.Supplier
import kotlin.math.roundToInt

/** Exact 9.16 Opus contracts; VertexActivity is shared, so the Opus bar must bind first. */
internal class ComposeOpusHook(private val settings: Supplier<Config>) {
    private val states = WeakHashMap<Activity, State>()
    private var current = WeakReference<Activity>(null)
    private class Scope(val fraction: Float, val keepBottom: Int = 0, val navigationBottom: Int? = null,
                        val measured: ((Int) -> Unit)? = null) { var motionUsed = false }
    private val scopes = ThreadLocal.withInitial { java.util.ArrayDeque<Scope>() }
    private inner class State(val activity: Activity, val snapshot: Any, val insetSnapshot: Any) {
        var surface: View? = null
        val lists = WeakHashMap<Any, Boolean>()
        val scroll = DetailBarScroll()
        var until = 0L
        var vertical = false
        var x = 0f
        var y = 0f
        var progress = 0f
        var barHeight = 0
        var insetKey: List<Any?>? = null
        var insetEpoch = 0f
        val handler = Handler(Looper.getMainLooper())
        val tick = object : Runnable {
            override fun run() {
                if (current.get() !== activity) return
                if (!eligible(activity) || ime(activity) || !settings.get().b("HOME_HIDE_BOTTOM_BAR")) reset()
                val key = listOf(ime(activity), settings.get().b("NAVIGATION_EDGE_TO_EDGE"),
                    surface?.height, activity.window.decorView.height,
                    surface?.let { ComposeBarMotion.navigationOverlap(activity, it) })
                if (key != insetKey) {
                    insetKey = key
                    writeState(insetSnapshot, ++insetEpoch)
                }
                handler.postDelayed(this, 100)
            }
        }
        fun show(value: Float) {
            val next = value.coerceIn(0f, 1f)
            if (kotlin.math.abs(progress - next) < .0001f) return
            progress = next; writeState(snapshot, next)
        }
        fun reset() { until = 0; vertical = false; scroll.bind(null); show(0f) }
    }
    private lateinit var writeState: (Any, Float) -> Unit
    private fun eligible(a: Activity) = !a.isFinishing && !a.isDestroyed && !a.isInMultiWindowMode &&
        !a.isInPictureInPictureMode && a.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    private fun ime(a: Activity) = if (Build.VERSION.SDK_INT >= 30)
        a.window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
        else a.window.decorView.rootWindowInsets?.let { it.systemWindowInsetBottom > it.stableInsetBottom + 80 * a.resources.displayMetrics.density } == true

    fun install(application: Application): Boolean {
        val loader = application.classLoader
        fun type(name: String) = Class.forName(name, false, loader)
        val bar = try { type("kntr.app.following.opusDetail.ui.chrome.s") } catch (_: ClassNotFoundException) { return false }
        val modifier = type("androidx.compose.ui.Modifier")
        val composer = type("androidx.compose.runtime.Composer")
        val render = bar.getDeclaredMethod("e", type("J71.a"), type("J71.m"), type("kotlin.jvm.functions.Function1"),
            modifier, Int::class.javaPrimitiveType, type("kotlin.jvm.functions.Function1"), composer, Int::class.javaPrimitiveType)
        val create = type("androidx.compose.runtime.SnapshotStateKt").getMethod("mutableStateOf\$default", Any::class.java,
            type("androidx.compose.runtime.SnapshotMutationPolicy"), Int::class.javaPrimitiveType, Any::class.java)
        val read = type("androidx.compose.runtime.State").getMethod("getValue")
        val write = type("androidx.compose.runtime.MutableState").getMethod("setValue", Any::class.java)
        writeState = { target, value -> write.invoke(target, value); Unit }
        val localView = type("androidx.compose.ui.platform.AndroidCompositionLocals_androidKt").getMethod("getLocalView").invoke(null)
        val consume = composer.getMethod("consume", type("androidx.compose.runtime.CompositionLocal"))
        val motion = ComposeBarMotion(loader)
        val navPadding = type("androidx.compose.foundation.layout.WindowInsetsPadding_androidKt").getMethod("navigationBarsPadding", modifier)
        val createInsets = type("androidx.compose.foundation.layout.WindowInsetsKt").getMethod("WindowInsets",
            Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        val insetPadding = type("androidx.compose.foundation.layout.WindowInsetsPaddingKt").getMethod("windowInsetsPadding",
            modifier, type("androidx.compose.foundation.layout.WindowInsets"))
        val lazy = type("androidx.compose.foundation.lazy.LazyListState")
        val backwards = lazy.getMethod("getCanScrollBackward")
        val onScroll = lazy.getDeclaredMethod("onScroll\$foundation", Float::class.javaPrimitiveType)
        val connection = type("kntr.app.following.opusDetail.ui.component.OpusNestPagerKt\$OpusNestPager\$pagerNestedScroll\$1\$1")
        val listField = connection.getDeclaredField("c").apply { isAccessible = true }
        check(listField.type == lazy)
        val preScroll = connection.getDeclaredMethod("onPreScroll-OzD1aCk", Long::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        val createConnection = connection.getDeclaredConstructor(type("kntr.app.following.opusDetail.ui.component.e"),
            lazy, type("androidx.compose.runtime.State"), type("androidx.compose.foundation.gestures.DefaultFlingBehavior"))
        val listRenderer = type("kntr.common.comment.page.ui.c0")
        val commentField = listRenderer.getDeclaredField("c").apply { isAccessible = true }
        check(commentField.type == lazy)
        val commentRender = listRenderer.getDeclaredMethod("invoke", Any::class.java, Any::class.java)

        HookRuntime.hookMethod(render, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                scopes.get().push(Scope(0f))
                val a = current.get() ?: return
                val s = states[a] ?: return
                val view = consume.invoke(p.args[6], localView) as? View ?: return
                if (view.rootView !== a.window.decorView) return
                s.surface = view
                roots[a.window.decorView] = true
                read.invoke(s.insetSnapshot)
                val fraction = (read.invoke(s.snapshot) as Number).toFloat()
                if (eligible(a) && !ime(a)) {
                    val hide = settings.get().b("HOME_HIDE_BOTTOM_BAR")
                    val bottom = if (settings.get().b("NAVIGATION_EDGE_TO_EDGE")) ComposeBarMotion.navigationOverlap(a, view) else null
                    val keep = ComposeBarMotion.navigationOverlap(a, view)
                    scopes.get().pop(); scopes.get().push(Scope(fraction, keep, bottom,
                        if (hide) { height -> s.barHeight = height } else null))
                }
                p.args[7] = (p.args[7] as Int) or 1
            }
            override fun afterHookedMethod(p: MethodHookParam) { scopes.get().poll() }
        })
        HookRuntime.hookMethod(navPadding, object : MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) {
                val scope = scopes.get().peek() ?: return
                if (scope.motionUsed) return
                scope.motionUsed = true
                val safe = scope.navigationBottom?.let {
                    insetPadding.invoke(null, p.args[0], createInsets.invoke(null, 0, 0, 0, it))
                } ?: p.result
                p.setResult(scope.measured?.let { motion.apply(safe, scope.fraction, scope.keepBottom, it) } ?: safe)
            }
        })
        fun remember(value: Any?) { current.get()?.let { a -> value?.let { states[a]?.lists?.set(it, true) } } }
        // The body scrolls before the nested pager receives its first gesture.
        // Capture its exact list when the connection is created, not only later
        // when a gesture begins inside the comments pager.
        HookRuntime.hookMethod(createConnection, object : MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) { remember(listField.get(p.thisObject)) }
        })
        HookRuntime.hookMethod(preScroll, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) { remember(listField.get(p.thisObject)) }
        })
        HookRuntime.hookMethod(commentRender, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) { remember(commentField.get(p.thisObject)) }
        })
        HookRuntime.hookMethod(onScroll, object : MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) {
                val a = current.get() ?: return; val s = states[a] ?: return
                val now = SystemClock.uptimeMillis()
                if (!s.lists.containsKey(p.thisObject) || s.surface?.isShown != true || !s.vertical || now > s.until ||
                    !settings.get().b("HOME_HIDE_BOTTOM_BAR") || !eligible(a) || ime(a)) return
                val delta = (p.result as? Number)?.toFloat() ?: return
                if (delta == 0f) return
                s.until = now + 1500
                s.scroll.scrolled((-delta).roundToInt(), maxOf(s.barHeight, (46 * a.resources.displayMetrics.density).roundToInt()),
                    backwards.invoke(p.thisObject) == false, now)
                s.show(s.scroll.fraction())
            }
        })
        Reflector.findAndHookMethod(Activity::class.java, "dispatchTouchEvent", MotionEvent::class.java, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                val a = p.thisObject as Activity; val s = states[a] ?: return
                val event = p.args[0] as MotionEvent
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    val view = s.surface ?: return; val bounds = Rect()
                    if (!view.getGlobalVisibleRect(bounds) || !bounds.contains(event.rawX.toInt(), event.rawY.toInt())) { s.until = 0; return }
                    s.x = event.rawX; s.y = event.rawY; s.vertical = false
                    s.scroll.bind(view); s.scroll.touch(SystemClock.uptimeMillis()); s.until = SystemClock.uptimeMillis() + 1500
                } else if (event.actionMasked == MotionEvent.ACTION_MOVE && s.until > 0) {
                    val dx = kotlin.math.abs(event.rawX - s.x); val dy = kotlin.math.abs(event.rawY - s.y)
                    s.vertical = dy > 8 * a.resources.displayMetrics.density && dy > dx
                    s.scroll.touch(SystemClock.uptimeMillis()); s.until = SystemClock.uptimeMillis() + 1500
                } else if (event.actionMasked == MotionEvent.ACTION_CANCEL) s.reset()
            }
        })
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, b: Bundle?) {
                if (a.javaClass.name == "kntr.common.compose.launcher.VertexActivity") {
                    states[a] = State(a, create.invoke(null, 0f, null, 2, null)!!,
                        create.invoke(null, 0f, null, 2, null)!!); current = WeakReference(a)
                }
            }
            override fun onActivityResumed(a: Activity) { states[a]?.let { current = WeakReference(a); it.handler.removeCallbacks(it.tick); it.handler.post(it.tick) } }
            override fun onActivityPaused(a: Activity) { states[a]?.let { it.handler.removeCallbacks(it.tick); it.reset() }; if (current.get() === a) current.clear() }
            override fun onActivityDestroyed(a: Activity) { roots.remove(a.window.decorView); states.remove(a)?.let { it.handler.removeCallbacks(it.tick) } }
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
        })
        return true
    }
    companion object {
        // Vertex hosts other Compose routes too: bind only after the exact Opus
        // interaction bar has rendered, never by Activity name alone.
        private val roots = WeakHashMap<View, Boolean>()
        fun owns(decor: View): Boolean = roots[decor] == true
    }
}
