/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import java.lang.reflect.*
import java.util.WeakHashMap
import java.util.function.Supplier

/** KHome's Compose tab container is measured away, releasing the content viewport. */
internal class ComposeHomeScroll(private val settings: Supplier<Config>) {
    private val states = WeakHashMap<Activity, State>()
    private lateinit var appBarType: Class<*>
    private lateinit var listenerType: Class<*>
    private lateinit var addOffset: Method
    private lateinit var removeOffset: Method
    private lateinit var scrollRange: Method
    private lateinit var currentOffset: Field
    private inner class State(val activity: Activity) {
        var bar: View? = null
        var appBar: View? = null
        var offsetListener: Any? = null
        var downX = 0f
        var downY = 0f
        var vertical = false
        var until = 0L
        var threshold = 1
        var expandedHeight = 0
        val scroll = DetailBarScroll()
        val handler = Handler(Looper.getMainLooper())
        var resumed = false
        val tick = object : Runnable {
            override fun run() {
                if (!resumed) return
                if (!eligible() || keyboard() || bar?.isAttachedToWindow != true) reset()
                else syncAppBar()
                handler.postDelayed(this, 100)
            }
        }
        fun eligible() = resumed && settings.get().b("HOME_HIDE_BOTTOM_BAR") &&
            !activity.isFinishing && !activity.isDestroyed && !activity.isInMultiWindowMode &&
            !activity.isInPictureInPictureMode &&
            activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        fun keyboard(): Boolean {
            val insets = activity.window.decorView.rootWindowInsets ?: return false
            return if (android.os.Build.VERSION.SDK_INT >= 30) insets.isVisible(WindowInsets.Type.ime())
                else insets.systemWindowInsetBottom > insets.stableInsetBottom + 80 * activity.resources.displayMetrics.density
        }
        fun bind(): Boolean {
            val id = activity.resources.getIdentifier("tab_host", "id", activity.packageName)
            val next = if (id == 0) null else activity.window.decorView.findViewById<View>(id)
            if (next?.javaClass?.name != HOST) return false
            if (bar !== next) { reset(); bar = next; expandedHeight = 0 }
            expandedHeight = maxOf(expandedHeight, next.height)
            val appId = activity.resources.getIdentifier("app_bar", "id", activity.packageName)
            val nextApp = (if (appId == 0) null else activity.window.decorView.findViewById<View>(appId))
                ?.takeIf { appBarType.isInstance(it) }
            if (appBar !== nextApp) {
                detachAppBar(); appBar = nextApp
                if (nextApp != null) {
                    offsetListener = Proxy.newProxyInstance(listenerType.classLoader, arrayOf(listenerType)) { proxy, method, args ->
                        when (method.name) {
                            "onOffsetChanged" -> { syncAppBar(); null }
                            "equals" -> proxy === args?.get(0)
                            "hashCode" -> System.identityHashCode(proxy)
                            "toString" -> "ComposeHomeOffsetListener"
                            else -> null
                        }
                    }
                    addOffset.invoke(nextApp, offsetListener)
                }
            }
            return true
        }
        fun reset() { scroll.bind(null); until = 0; vertical = false; ComposeHomeHook.collapse(0f) }
        fun detachAppBar() {
            if (appBar != null && offsetListener != null) removeOffset.invoke(appBar, offsetListener)
            appBar = null; offsetListener = null
        }
        fun syncAppBar(): Boolean {
            val target = appBar ?: return false
            if (!eligible() || keyboard() || !target.isShown || !target.isAttachedToWindow) return false
            ComposeHomeHook.collapse(HomeBarSync.fraction(currentOffset.getInt(target), scrollRange.invoke(target) as Int))
            return true
        }
        fun touch(event: MotionEvent) {
            if (!bind() || !eligible() || keyboard()) { reset(); return }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val bounds = Rect()
                    val target = bar ?: return
                    val hitBar = target.getGlobalVisibleRect(bounds) && bounds.contains(event.rawX.toInt(), event.rawY.toInt())
                    if (hitBar) { reset(); return }
                    downX = event.rawX; downY = event.rawY; vertical = false
                    threshold = maxOf((52 * activity.resources.displayMetrics.density).toInt(), expandedHeight)
                    until = SystemClock.uptimeMillis() + 1500
                    scroll.bind(target); scroll.touch(SystemClock.uptimeMillis())
                }
                MotionEvent.ACTION_MOVE -> {
                    if (until == 0L) return
                    val dx = kotlin.math.abs(event.rawX - downX)
                    val dy = kotlin.math.abs(event.rawY - downY)
                    vertical = dy > 8 * activity.resources.displayMetrics.density && dy > dx
                    until = SystemClock.uptimeMillis() + 1500
                    scroll.touch(SystemClock.uptimeMillis())
                }
                MotionEvent.ACTION_CANCEL -> reset()
            }
        }
        fun scrolled(view: View, dy: Int) {
            val decor = activity.window.decorView
            val now = SystemClock.uptimeMillis()
            if (!eligible() || !vertical || now > until || keyboard() || view.rootView !== decor ||
                !view.isShown || view.width < decor.width * .6 || view.height < decor.height * .25) return
            until = now + 1500
            if (syncAppBar()) return
            val fraction = scroll.scrolled(dy, threshold, !view.canScrollVertically(-1), now)
            ComposeHomeHook.collapse(fraction)
        }
    }

    fun install(application: Application): Boolean {
        try { Class.forName(HOST, false, application.classLoader) }
        catch (_: ClassNotFoundException) { return false }
        val appBar = Class.forName("com.google.android.material.appbar.AppBarLayout", false, application.classLoader)
        appBarType = appBar
        listenerType = Class.forName("com.google.android.material.appbar.AppBarLayout\$OnOffsetChangedListener", false, application.classLoader)
        addOffset = appBar.getMethod("addOnOffsetChangedListener", listenerType)
        removeOffset = appBar.getMethod("removeOnOffsetChangedListener", listenerType)
        scrollRange = appBar.getMethod("getTotalScrollRange")
        currentOffset = Reflector.findField(appBar, "currentOffset")
        val recycler = Class.forName("androidx.recyclerview.widget.RecyclerView", false, application.classLoader)
        val method = recycler.getDeclaredMethod("dispatchOnScrolled", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        HookRuntime.hookMethod(method, object : MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) {
                val view = p.thisObject as View
                states.values.forEach { it.scrolled(view, p.args[1] as Int) }
            }
        })
        for (name in listOf("android.widget.ScrollView", "android.webkit.WebView", "androidx.core.widget.NestedScrollView")) {
            try {
            val type = Class.forName(name, false, application.classLoader)
            Reflector.findAndHookMethod(type, "onScrollChanged", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, object : MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam) {
                        states.values.forEach { it.scrolled(p.thisObject as View, (p.args[1] as Int) - (p.args[3] as Int)) }
                    }
                })
            } catch (_: ReflectiveOperationException) {
                HookRuntime.log("[EasyBiliTool] COMPOSE_HOME_SCROLL_NOT_APPLICABLE $name")
            }
        }
        Reflector.findAndHookMethod(Activity::class.java, "dispatchTouchEvent", MotionEvent::class.java, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) { states[p.thisObject]?.touch(p.args[0] as MotionEvent) }
        })
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, b: Bundle?) {
                if (a.javaClass.name == "tv.danmaku.bili.MainActivityV2") states[a] = State(a)
            }
            override fun onActivityResumed(a: Activity) { states[a]?.let { it.resumed = true; it.handler.removeCallbacks(it.tick); it.handler.post(it.tick) } }
            override fun onActivityPaused(a: Activity) { states[a]?.let { it.resumed = false; it.handler.removeCallbacks(it.tick); it.detachAppBar(); it.reset() } }
            override fun onActivityDestroyed(a: Activity) { states.remove(a)?.let { it.handler.removeCallbacks(it.tick); it.detachAppBar(); it.reset() } }
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
        })
        return true
    }
    companion object { private const val HOST = "tv.danmaku.bili.home.components.bottomtab.WindowAwareComposeView" }
}
