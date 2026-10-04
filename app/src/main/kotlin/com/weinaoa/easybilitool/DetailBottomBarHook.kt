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
import android.graphics.Rect
import android.os.*
import android.view.*
import java.util.*
import java.util.function.Supplier

/**

 * Detail list scrolling drives the native toolbar.

 */
class DetailBottomBarHook {
    private lateinit var settings: Supplier<Config>

    private val states: MutableMap<Activity, State> = WeakHashMap()

    constructor(settings: Supplier<Config>) {
        this.settings = settings
    }

    @Throws(Exception::class)
    fun install(application: Application) {
        var loader: ClassLoader = application.getClassLoader()
        Reflector.findAndHookMethod(Activity::class.java, "dispatchTouchEvent", MotionEvent::class.java, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                var s: State? = states.get(p.thisObject)
                if ((s != null)) {
                    (s)!!.touch((p.args[0] as MotionEvent))
                }
            }
        })
        try {
            Reflector.findAndHookMethod(Class.forName("androidx.recyclerview.widget.RecyclerView", false, loader), "dispatchOnScrolled", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, object : MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) {
                    scrolled((p.thisObject as View), (p.args[1] as Int))
                }
            })
        } catch (unavailable: ReflectiveOperationException) {
            HookRuntime.log("[EasyBiliTool] DETAIL_RECYCLER_SCROLL_UNAVAILABLE")
        }
        for (type in arrayOf("android.widget.ScrollView", "androidx.core.widget.NestedScrollView")) {

            try {
                Reflector.findAndHookMethod(Class.forName(type, false, loader), "onScrollChanged", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, object : MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam) {
                        scrolled((p.thisObject as View), ((p.args[1] as Int) - (p.args[3] as Int)))
                    }
                })
            } catch (unavailable: ReflectiveOperationException) {
                HookRuntime.log(("[EasyBiliTool] DETAIL_SCROLL_UNAVAILABLE " + type))
            }
        }
        Reflector.findAndHookMethod(View::class.java, "onScrollChanged", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, object : MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) {
                if (p.thisObject!!.javaClass.getName().equals(OPUS_SCROLL)) {
                    scrolled((p.thisObject as View), ((p.args[1] as Int) - (p.args[3] as Int)))
                }
            }
        })
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, b: Bundle?) {
                if (a.javaClass.getName().equals("tv.danmaku.bili.MainActivityV2")) {
                    return
                }
                var s: State = State(a)
                states.put(a, s)
                (s)!!.decor.getViewTreeObserver().addOnPreDrawListener((s)!!.draw)
            }
            override fun onActivityResumed(a: Activity) {
                var s: State? = states.get(a)
                if ((s != null)) {
                    (s)!!.resumed = true
                    (s)!!.handler.removeCallbacks((s)!!.tick)
                    (s)!!.handler.post((s)!!.tick)
                }
            }
            override fun onActivityPaused(a: Activity) {
                var s: State? = states.get(a)
                if ((s != null)) {
                    (s)!!.resumed = false
                    (s)!!.handler.removeCallbacks((s)!!.tick)
                    (s)!!.restore()
                }
            }
            override fun onActivityDestroyed(a: Activity) {
                var s: State? = states.remove(a)
                if ((s != null)) {
                    (s)!!.handler.removeCallbacks((s)!!.tick)
                    (s)!!.restore()
                    if ((s)!!.decor.getViewTreeObserver().isAlive()) {
                        (s)!!.decor.getViewTreeObserver().removeOnPreDrawListener((s)!!.draw)
                    }
                }
            }
            override fun onActivityStarted(a: Activity) {

            }
            override fun onActivityStopped(a: Activity) {

            }
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {

            }
        })
    }

    private fun scrolled(view: View, dy: Int) {
        if (((dy == 0) || !(settings.get())!!.b("HOME_HIDE_BOTTOM_BAR"))) {
            return
        }
        for (s in states.values) {

            if ((view.getRootView() == (s)!!.decor)) {
                (s)!!.scrolled(view, dy)
                break
            }
        }
    }

    private inner class State {
        lateinit var activity: Activity

        lateinit var decor: View

        @JvmField val handler: Handler = Handler(Looper.getMainLooper())

        @JvmField val scroll: DetailBarScroll = DetailBarScroll()

        @JvmField var opusId: Int = 0
        @JvmField var commentsId: Int = 0

        @JvmField val draw: ViewTreeObserver.OnPreDrawListener = ViewTreeObserver.OnPreDrawListener(lambda@ { update(false)
            return@lambda true })

        @JvmField val tick: Runnable = object : Runnable {
            override fun run() {
                update(true)
                if (resumed) {
                    handler.postDelayed(this, 100)
                }
            }
        }

        @JvmField var bar: ViewGroup? = null

        @JvmField var surface: View? = null

        @JvmField var viewport: DetailScrollViewport? = null

        @JvmField var resumed: Boolean = false
        @JvmField var changing: Boolean = false
        @JvmField var applied: Boolean = false

        @JvmField var checked: Long = 0L

        @JvmField var originalTranslation: Float = 0F
        @JvmField var lastTranslation: Float = 0F
        @JvmField var originalAlpha: Float = 0F
        @JvmField var lastAlpha: Float = 0F

        @JvmField var originalAccessibility: Int = 0

        constructor(activity: Activity) {
            this.activity = activity
            decor = activity.getWindow().getDecorView()
            opusId = id("opus_nested_scroll")
            commentsId = id("cmt3_recycler")
        }

        fun id(name: String): Int {
            return activity.getResources().getIdentifier(name, "id", "tv.danmaku.bili")
        }

        fun eligible(): Boolean {
            if ((((((((!resumed || !(settings.get())!!.b("HOME_HIDE_BOTTOM_BAR")) || !decor.hasWindowFocus()) || activity.isFinishing()) || activity.isDestroyed()) || (activity.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT)) || activity.isInMultiWindowMode()) || activity.isInPictureInPictureMode())) {
                return false
            }
            var insets: WindowInsets? = decor.getRootWindowInsets()
            if ((insets == null)) {
                return true
            }
            if ((Build.VERSION.SDK_INT >= 30)) {
                return !(insets)!!.isVisible(WindowInsets.Type.ime())
            }
            return ((insets)!!.getSystemWindowInsetBottom() <= ((insets)!!.getStableInsetBottom() + (80 * activity.getResources().getDisplayMetrics().density)))
        }

        fun visibleContent(view: View, id: Int): View? {
            if (((id == 0) || !view.isShown())) {
                return null
            }
            if ((view.getId() == id)) {
                return view
            }
            val group = view as? ViewGroup
            if (group != null) {
                run {
                    var i: Int = ((group)!!.getChildCount() - 1)
                    while ((i >= 0)) {
                        var found: View? = visibleContent((group)!!.getChildAt(i), id)
                        if ((found != null)) {
                            return found
                        }
                        i--
                    }
                }
            }
            return null
        }

        fun update(force: Boolean) {
            if (changing) {
                return
            }
            if (!eligible()) {
                restore()
                return
            }
            var now: Long = SystemClock.uptimeMillis()
            if ((!force && ((now - checked) < 100))) {
                return
            }
            changing = true
            checked = now
            try {
                var next: ViewGroup? = DetailBarLocator.findBar(activity, decor)
                if ((next !== bar)) {
                    restore()
                    bar = next
                }
                if ((bar == null)) {
                    return
                }
                var video: Boolean = ((bar)!!.getId() == id("cmt3_fake_input_bar_container"))
                var nextSurface: View? = visibleContent(decor, (if (video) commentsId else opusId))
                if (((nextSurface == null) && !video)) {
                    nextSurface = visibleContent(decor, commentsId)
                }
                if ((surface !== nextSurface)) {
                    restoreNative()
                    surface = nextSurface
                    scroll.bind(surface)
                }
                if ((surface == null)) {
                    restore()
                    return
                }
                apply()
            } catch (error: Throwable) {
                restore()
                HookRuntime.log(("[EasyBiliTool] DETAIL_BOTTOM_BAR_BYPASS " + error.javaClass.getSimpleName()))
            } finally {
                changing = false
            }
        }

        fun touch(event: MotionEvent) {
            var action: Int = event.getActionMasked()
            if (((action != MotionEvent.ACTION_DOWN) && (action != MotionEvent.ACTION_MOVE))) {
                return
            }
            update((action == MotionEvent.ACTION_DOWN))
            if (((surface == null) || !eligible())) {
                return
            }
            var bounds: Rect = Rect()
            if (((surface)!!.getGlobalVisibleRect(bounds) && bounds.contains((event.getRawX()).toInt(), (event.getRawY()).toInt()))) {
                scroll.touch(SystemClock.uptimeMillis())
            }
        }

        fun belongs(view: View): Boolean {
            run {
                var current: View? = view
                while ((current != null)) {
                    if ((current === surface)) {
                        return true
                    }
                    var parent: ViewParent? = current!!.getParent()
                    current = (if (parent is View) (parent as View) else null)

                }
            }
            return false
        }

        fun scrolled(view: View, dy: Int) {
            update(false)
            if ((((((bar == null) || (surface == null)) || !view.isShown()) || !belongs(view)) || !eligible())) {
                return
            }
            var atTop: Boolean = (!view.canScrollVertically(-1) && !(surface)!!.canScrollVertically(-1))
            scroll.scrolled(dy, (bar)!!.getHeight(), atTop, SystemClock.uptimeMillis())
            apply()
        }

        fun apply() {
            if ((bar == null)) {
                return
            }
            var fraction: Float = scroll.fraction()
            run {
                if (!applied) {
                    originalTranslation = (bar)!!.getTranslationY()
                    originalAlpha = (bar)!!.getAlpha()
                    originalAccessibility = (bar)!!.getImportantForAccessibility()
                    applied = true
                } else {
                    if ((Math.abs(((bar)!!.getTranslationY() - lastTranslation)) > .5F)) {
                        originalTranslation = (bar)!!.getTranslationY()
                    }
                    if ((Math.abs(((bar)!!.getAlpha() - lastAlpha)) > .0001F)) {
                        originalAlpha = (bar)!!.getAlpha()
                    }
                }
                var distance: Float = (fraction * (bar)!!.getHeight())
                lastTranslation = (originalTranslation + distance)
                lastAlpha = (originalAlpha * HomeBarSync.opacity(fraction))
                if ((Math.abs(((bar)!!.getTranslationY() - lastTranslation)) > .01F)) {
                    (bar)!!.setTranslationY(lastTranslation)
                }
                if ((Math.abs(((bar)!!.getAlpha() - lastAlpha)) > .0001F)) {
                    (bar)!!.setAlpha(lastAlpha)
                }
                translations.put(bar!!, distance)
                var accessibility: Int = (if ((fraction >= .999F)) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else originalAccessibility)
                if (((bar)!!.getImportantForAccessibility() != accessibility)) {
                    (bar)!!.setImportantForAccessibility(accessibility)
                }
            }
            if ((viewport == null)) {
                viewport = DetailScrollViewport(activity, bar!!, decor)
            }
            (viewport)!!.apply()
            fractions.put(bar!!, fraction)
        }

        fun restoreViewport() {
            if ((viewport != null)) {
                (viewport)!!.restore()
                viewport = null
            }
        }

        fun restoreNative() {
            restoreViewport()
            if ((bar != null)) {
                translations.remove(bar!!)
            }
            if ((!applied || (bar == null))) {
                return
            }
            if ((Math.abs(((bar)!!.getTranslationY() - lastTranslation)) < .5F)) {
                (bar)!!.setTranslationY(originalTranslation)
            }
            if ((Math.abs(((bar)!!.getAlpha() - lastAlpha)) < .0001F)) {
                (bar)!!.setAlpha(originalAlpha)
            }
            if (((bar)!!.getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)) {
                (bar)!!.setImportantForAccessibility(originalAccessibility)
            }
            applied = false
        }

        fun restore() {
            restoreNative()
            if ((bar != null)) {
                fractions.remove(bar!!)
            }
            scroll.bind(null)
            surface = null
            bar = null
        }
    }

    companion object {
        private val fractions: MutableMap<View, Float> = WeakHashMap()
        private val translations: MutableMap<View, Float> = WeakHashMap()

        private val OPUS_SCROLL: String = "com.bilibili.bplus.followinglist.page.opus.OpusNestedScrollParent"

        @JvmStatic fun collapseFraction(bar: View): Float {
            return fractions.getOrDefault(bar, 0F)
        }

        @JvmStatic fun managed(bar: View): Boolean {
            return fractions.containsKey(bar)
        }

        @JvmStatic fun translation(bar: View): Float {
            return translations.getOrDefault(bar, 0F)
        }
    }
}
