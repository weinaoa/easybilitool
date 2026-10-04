/*
Copyright (c) 2026 weinaoa
EasyBiliTool is licensed under Mulan PubL v2.
You can use this software according to the terms and conditions of the Mulan PubL v2.
You may obtain a copy of Mulan PubL v2 at:
    http://license.coscl.org.cn/MulanPubL-2.0
THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
See the Mulan PubL v2 for more details.
*/

package com.weinaoa.easybilitool

import android.app.*
import android.content.res.Configuration
import android.os.*
import android.view.*
import android.widget.TextView
import java.lang.reflect.*
import java.util.*
import java.util.function.Supplier

/**
 * Home follows its AppBar; other main tabs follow actual vertical scrolling.
 */
class HomeBottomBarHook {
    private lateinit var settings: Supplier<Config>

    private val activities: MutableMap<Activity, State> = WeakHashMap()

    private var listenerType: Class<*>? = null

    private var addListener: Method? = null
    private var removeListener: Method? = null
    private var scrollRange: Method? = null

    private var currentOffset: Field? = null

    constructor(settings: Supplier<Config>) {
        this.settings = settings
    }

    @Throws(Exception::class)
    fun install(application: Application) {
        var loader: ClassLoader = application.getClassLoader()
        var nativeBar: Class<*> = Class.forName("com.google.android.material.appbar.AppBarLayout", false, loader)
        Class.forName(APP_BAR, false, loader)
        Class.forName(TAB_HOST, false, loader)
        listenerType = Class.forName("com.google.android.material.appbar.AppBarLayout\$OnOffsetChangedListener", false, loader)
        addListener = nativeBar.getMethod("addOnOffsetChangedListener", listenerType)
        removeListener = nativeBar.getMethod("removeOnOffsetChangedListener", listenerType)
        scrollRange = nativeBar.getMethod("getTotalScrollRange")
        currentOffset = Reflector.findField(nativeBar, "currentOffset")
        Reflector.findAndHookMethod(Activity::class.java, "dispatchTouchEvent", MotionEvent::class.java, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                var state: State? = activities.get(p.thisObject)
                if ((state != null)) {
                    (state)!!.touch((p.args[0] as MotionEvent))
                }
            }
        })
        var recycler: Class<*> = Class.forName("androidx.recyclerview.widget.RecyclerView", false, loader)
        Reflector.findAndHookMethod(recycler, "dispatchOnScrolled", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, object : MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) {
                scrolled((p.thisObject as View), (p.args[1] as Int))
            }
        })
        for (type in arrayOf("android.widget.ScrollView", "android.webkit.WebView", "androidx.core.widget.NestedScrollView")) {

            try {
                Reflector.findAndHookMethod(Class.forName(type, false, loader), "onScrollChanged", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, object : MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam) {
                        scrolled((p.thisObject as View), ((p.args[1] as Int) - (p.args[3] as Int)))
                    }
                })
            } catch (unsupported: ReflectiveOperationException) {
                HookRuntime.log(("[EasyBiliTool] MAIN_SCROLL_UNAVAILABLE " + type))
            }
        }
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, b: Bundle?) {
                if (!a.javaClass.getName().equals("tv.danmaku.bili.MainActivityV2")) {
                    return
                }
                var s: State = State(a)
                activities.put(a, s)
                s.decor.getViewTreeObserver().addOnGlobalLayoutListener(s.layout)
                s.decor.getViewTreeObserver().addOnPreDrawListener(s.preDraw)
            }
            override fun onActivityResumed(a: Activity) {
                var s: State? = activities.get(a)
                if ((s != null)) {
                    (s)!!.resumed = true
                    (s)!!.handler.removeCallbacks((s)!!.tick)
                    (s)!!.handler.post((s)!!.tick)
                }
            }
            override fun onActivityPaused(a: Activity) {
                var s: State? = activities.get(a)
                if ((s != null)) {
                    (s)!!.resumed = false
                    (s)!!.handler.removeCallbacks((s)!!.tick)
                    (s)!!.restore()
                }
            }
            override fun onActivityDestroyed(a: Activity) {
                var s: State? = activities.remove(a)
                if ((s != null)) {
                    (s)!!.destroy()
                }
            }
            override fun onActivityStarted(a: Activity) {

            }
            override fun onActivityStopped(a: Activity) {

            }
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {

            }
        })
        HookRuntime.log("[EasyBiliTool] HOME_BOTTOM_BAR_READY 8.95 native offset sync")
    }

    fun scrolled(view: View, dy: Int) {
        if (((dy == 0) || !(settings.get())!!.b("HOME_HIDE_BOTTOM_BAR"))) {
            return
        }
        for (state in activities.values) {

            if ((view.getRootView() == (state)!!.decor)) {
                (state)!!.scrolled(view, dy)
                break
            }
        }
    }

    private inner class State {
        lateinit var activity: Activity

        lateinit var decor: View

        @JvmField var barId: Int = 0
        @JvmField var bottomId: Int = 0
        @JvmField var labelId: Int = 0

        @JvmField val handler: Handler = Handler(Looper.getMainLooper())

        @JvmField val layout: ViewTreeObserver.OnGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener(this::update)

        @JvmField val preDraw: ViewTreeObserver.OnPreDrawListener = ViewTreeObserver.OnPreDrawListener(lambda@ { update()
            return@lambda true })

        @JvmField val tick: Runnable = object : Runnable {
            override fun run() {
                update()
                if (resumed) {
                    handler.postDelayed(this, 500)
                }
            }
        }

        @JvmField var appBar: View? = null
        @JvmField var bottom: View? = null

        @JvmField var listener: Any? = null

        @JvmField var resumed: Boolean = false
        @JvmField var updating: Boolean = false
        @JvmField var failed: Boolean = false
        @JvmField var applied: Boolean = false
        @JvmField var opacityApplied: Boolean = false

        @JvmField var originalTranslation: Float = 0F
        @JvmField var lastTranslation: Float = 0F
        @JvmField var originalAlpha: Float = 0F
        @JvmField var lastAlpha: Float = 0F

        @JvmField var originalAccessibility: Int = 0

        @JvmField var page: String = ""

        @JvmField val scroll: MainTabBarScroll = MainTabBarScroll()

        @JvmField var scrollUntil: Long = 0L

        constructor(a: Activity) {
            activity = a
            decor = a.getWindow().getDecorView()
            barId = id("app_bar")
            bottomId = id("bottom_navigation")
            labelId = id("tab_text")
        }

        fun id(name: String): Int {
            return activity.getResources().getIdentifier(name, "id", "tv.danmaku.bili")
        }

        @Throws(Exception::class)
        fun bind() {
            if (((bottom != null) && (bottom)!!.isAttachedToWindow())) {
                if (((appBar != null) && (appBar)!!.isAttachedToWindow())) {
                    return
                }
                if (((appBar == null) && (((barId == 0) || (decor.findViewById<View>(barId) == null))))) {
                    return
                }
            }
            detach()
            var bar: View? = (if ((barId == 0)) null else decor.findViewById(barId))
            var nav: View? = (if ((bottomId == 0)) null else decor.findViewById(bottomId))
            if (((nav == null) || !(nav)!!.javaClass.getName().equals(TAB_HOST))) {
                return
            }
            bottom = nav
            if (((bar == null) || !(bar)!!.javaClass.getName().equals(APP_BAR))) {
                return
            }
            appBar = bar
            listener = Proxy.newProxyInstance((listenerType)!!.getClassLoader(), arrayOf(listenerType), lambda@ { proxy, method, args -> if (method.getName().equals("onOffsetChanged")) {
                    update()
                    return@lambda null
                }
                if (method.getName().equals("equals")) {
                    return@lambda (proxy === args[0])
                }
                if (method.getName().equals("hashCode")) {
                    return@lambda System.identityHashCode(proxy)
                }
                if (method.getName().equals("toString")) {
                    return@lambda "EasyBiliToolHomeOffsetListener"
                }
                return@lambda null })
            (addListener)!!.invoke(appBar, listener)
        }

        fun selectedPage(view: View): String {
            val label = view as? TextView
            if ((((view.getId() == labelId) && label != null) && view.isSelected())) {
                return (label)!!.getText().toString()
            }
            val group = view as? ViewGroup
            if (group != null) {
                run {
                    var i: Int = 0
                    while ((i < (group)!!.getChildCount())) {
                        var found: String = selectedPage((group)!!.getChildAt(i))
                        if (!found.isEmpty()) {
                            return found
                        }
                        i++
                    }
                }
            }
            return ""
        }

        fun touch(event: MotionEvent) {
            if (((event.getActionMasked() == MotionEvent.ACTION_DOWN) || (event.getActionMasked() == MotionEvent.ACTION_MOVE))) {
                scrollUntil = (SystemClock.uptimeMillis() + 1500)
            }
        }

        fun scrolled(view: View, dy: Int) {
            if ((((!resumed || !view.isShown()) || (view.getWidth() < (decor.getWidth() * .6))) || (view.getHeight() < (decor.getHeight() * .25)))) {
                return
            }
            update()
            if (((((bottom == null) || !applied) || !MainTabBarScroll.supports(page)) || (SystemClock.uptimeMillis() > scrollUntil))) {
                return
            }
            scrollUntil = (SystemClock.uptimeMillis() + 1500)
            var fraction: Float = scroll.scrolled(dy, (bottom)!!.getHeight(), !view.canScrollVertically(-1))
            apply(fraction, (bottom)!!.getHeight())
        }

        fun update() {
            if ((updating || failed)) {
                return
            }
            updating = true
            try {
                if ((!resumed || !(settings.get())!!.b("HOME_HIDE_BOTTOM_BAR"))) {
                    restore()
                    return
                }
                bind()
                if ((((((bottom == null) || !(bottom)!!.isShown()) || (activity.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT)) || activity.isInMultiWindowMode()) || activity.isInPictureInPictureMode())) {
                    restore()
                    return
                }
                var selected: String = selectedPage(bottom!!)
                if (!page.equals(selected)) {
                    restore()
                    page = selected
                    scrollUntil = 0
                }
                var height: Int = (bottom)!!.getHeight()
                if ((height <= 0)) {
                    restore()
                    return
                }
                var fraction: Float = 0F
                if (((page.equals("首页") && (appBar != null)) && (appBar)!!.isShown())) {
                    var range: Int = ((scrollRange)!!.invoke(appBar) as Int)
                    fraction = HomeBarSync.fraction((currentOffset)!!.getInt(appBar), range)
                } else {
                    if (MainTabBarScroll.supports(page)) {
                        fraction = scroll.fraction()
                    } else {
                        restore()
                        return
                    }
                }
                apply(fraction, height)
            } catch (e: Throwable) {
                restore()
                failed = true
                HookRuntime.log(("[EasyBiliTool] HOME_BOTTOM_BAR_BYPASS: " + e.javaClass.getSimpleName()))
            } finally {
                updating = false
            }
        }

        fun apply(fraction: Float, height: Int) {
            var translation: Float = (fraction * height)
            if (!applied) {
                originalTranslation = (bottom)!!.getTranslationY()
                originalAccessibility = (bottom)!!.getImportantForAccessibility()
                applied = true
            } else {
                if ((Math.abs(((bottom)!!.getTranslationY() - lastTranslation)) > .5F)) {
                    originalTranslation = (bottom)!!.getTranslationY()
                }
            }
            lastTranslation = (originalTranslation + translation)
            if ((Math.abs(((bottom)!!.getTranslationY() - lastTranslation)) > .01F)) {
                (bottom)!!.setTranslationY(lastTranslation)
            }
            run {
                if (!opacityApplied) {
                    originalAlpha = (bottom)!!.getAlpha()
                    opacityApplied = true
                } else {
                    if ((Math.abs(((bottom)!!.getAlpha() - lastAlpha)) > .0001F)) {
                        originalAlpha = (bottom)!!.getAlpha()
                    }
                }
                lastAlpha = (originalAlpha * HomeBarSync.opacity(fraction))
                if ((Math.abs(((bottom)!!.getAlpha() - lastAlpha)) > .0001F)) {
                    (bottom)!!.setAlpha(lastAlpha)
                }
            }
            var accessibility: Int = (if ((translation >= (height - .01F))) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else originalAccessibility)
            if (((bottom)!!.getImportantForAccessibility() != accessibility)) {
                (bottom)!!.setImportantForAccessibility(accessibility)
            }
        }

        fun restoreOpacity() {
            if ((opacityApplied && (bottom != null))) {
                if ((Math.abs(((bottom)!!.getAlpha() - lastAlpha)) < .0001F)) {
                    (bottom)!!.setAlpha(originalAlpha)
                }
                opacityApplied = false
            }
        }

        fun restore() {
            scroll.reset()
            restoreOpacity()
            if ((!applied || (bottom == null))) {
                return
            }
            if ((Math.abs(((bottom)!!.getTranslationY() - lastTranslation)) < .5F)) {
                (bottom)!!.setTranslationY(originalTranslation)
            }
            if (((bottom)!!.getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)) {
                (bottom)!!.setImportantForAccessibility(originalAccessibility)
            }
            applied = false
        }

        fun detach() {
            restore()
            if (((appBar != null) && (listener != null))) {
                try {
                    (removeListener)!!.invoke(appBar, listener)
                } catch (ignored: Exception) {

                }
            }
            appBar = null
            bottom = null
            listener = null
        }

        fun destroy() {
            resumed = false
            handler.removeCallbacks(tick)
            detach()
            if (decor.getViewTreeObserver().isAlive()) {
                decor.getViewTreeObserver().removeOnGlobalLayoutListener(layout)
                decor.getViewTreeObserver().removeOnPreDrawListener(preDraw)
            }
        }
    }

    companion object {
        private val APP_BAR: String = "tv.danmaku.bili.ui.main2.widget.HomeAppBarLayout"

        private val TAB_HOST: String = "com.bilibili.lib.homepage.widget.TabHost"
    }
}
