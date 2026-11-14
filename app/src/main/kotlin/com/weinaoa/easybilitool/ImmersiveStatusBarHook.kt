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
import android.graphics.Color
import android.graphics.Insets
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import androidx.core.view.WindowCompat
import android.os.*
import android.view.*
import java.util.*
import java.util.function.Supplier

/**
 * Transparent status bar in the verified video and live-room windows.
 */
class ImmersiveStatusBarHook {
    private lateinit var settings: Supplier<Config>

    private val activities: MutableMap<Activity, State> = WeakHashMap()

    private val decors: MutableMap<View, State> = WeakHashMap()

    private val windows: MutableMap<Window, State> = WeakHashMap()

    private val controllers: MutableMap<WindowInsetsController, State> = WeakHashMap()
    private val requestedDecorFits = WeakHashMap<Window, Boolean>()

    private val storyInsets by lazy { StoryStatusInsets(settings) }

    constructor(settings: Supplier<Config>) {
        this.settings = settings
    }

    @Throws(Exception::class)
    fun install(application: Application) {
        PlayerSpeedTipLayout.install(application.classLoader, Supplier { settings.get().b("IMMERSIVE_STATUS_BAR") })
        storyInsets.install(application)
        Reflector.findAndHookMethod(View::class.java, "dispatchApplyWindowInsets", WindowInsets::class.java, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                val view = p.thisObject as View
                val state = decors[view] ?: if (view.id == android.R.id.content) decors[view.rootView] else null
                if (state == null || !state.eligible()) {
                    return
                }
                var insets: WindowInsets = (p.args[0] as WindowInsets)
                var top: Int = (if ((Build.VERSION.SDK_INT >= 30)) insets.getInsetsIgnoringVisibility((WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout())).top else insets.getSystemWindowInsetTop())
                if ((top > 0)) {
                    (state)!!.statusHeight = top
                }
                // Keep real insets on DecorView for AndroidX's system-bar monitor.
                // Only the host content consumes the top inset for immersive layout.
                if (view === state.decor) return
                if ((Build.VERSION.SDK_INT >= 30)) {
                    p.args[0] = WindowInsets.Builder(insets).setInsets(WindowInsets.Type.statusBars(), Insets.NONE).setInsetsIgnoringVisibility(WindowInsets.Type.statusBars(), Insets.NONE).setDisplayCutout(null).build()
                } else {
                    p.args[0] = insets.replaceSystemWindowInsets(Rect(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()))
                }
            }
        })
        Reflector.findAndHookMethod(View::class.java, "setSystemUiVisibility", Int::class.javaPrimitiveType!!, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                var state: State? = decors.get(p.thisObject)
                if ((((state == null) || (state)!!.changing) || !(state)!!.applied)) {
                    return
                }
                (state)!!.originalUi = ((((state)!!.originalUi and (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR).inv())) or (((p.args[0] as Int) and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)))
                if ((state)!!.eligible() && (!(state)!!.live || Build.VERSION.SDK_INT < 30)) {
                    p.args[0] = (p.args[0] as Int) or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                }
            }
        })
        Reflector.findAndHookMethod(Class.forName("com.android.internal.policy.PhoneWindow"), "setStatusBarColor", Int::class.javaPrimitiveType!!, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                var state: State? = windows.get(p.thisObject)
                if ((((state == null) || (state)!!.changing) || !(state)!!.applied)) {
                    return
                }
                (state)!!.originalColor = (p.args[0] as Int)
                if ((state)!!.eligible()) {
                    p.args[0] = Color.TRANSPARENT
                }
            }
        })
        if ((Build.VERSION.SDK_INT >= 30)) {
            Reflector.findAndHookMethod(Window::class.java, "setDecorFitsSystemWindows", Boolean::class.javaPrimitiveType!!, object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) {
                    val window=p.thisObject as Window
                    val state=windows[window]
                    if(state?.changing==true) return
                    requestedDecorFits[window]=p.args[0] as Boolean
                    if(state?.live==true && state.applied && state.eligible()) p.args[0]=false
                }
            })
            Reflector.findAndHookMethod(Class.forName("android.view.InsetsController"), "setSystemBarsAppearance", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) {
                    var state: State? = controllers.get(p.thisObject)
                    if ((((state == null) || (state)!!.changing) || !(state)!!.applied)) {
                        return
                    }
                    var value: Int = (p.args[0] as Int)
                    var mask: Int = (p.args[1] as Int)
                    (state)!!.appearance = ((((state)!!.appearance and (mask).inv())) or ((value and mask)))
                }
            })
        }
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, b: Bundle?) {
                var name: String = a.javaClass.getName()
                if (name !in setOf("com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity",
                    "com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity",
                    "com.bilibili.video.story.StoryVideoActivity",
                    "com.bilibili.bililive.room.ui.roomv3.LiveRoomActivityV3")) {
                    return
                }
                var state: State = State(a)
                activities.put(a, state)
                decors.put((state)!!.decor, state)
                windows.put((state)!!.window, state)
                (state)!!.decor.getViewTreeObserver().addOnGlobalLayoutListener((state)!!.layout)
                (state)!!.decor.getViewTreeObserver().addOnPreDrawListener((state)!!.preDraw)
            }
            override fun onActivityResumed(a: Activity) {
                var s: State? = activities.get(a)
                if ((s != null)) {
                    (s)!!.resumed = true
                    (s)!!.update()
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
                    (s)!!.handler.removeCallbacks((s)!!.tick)
                    if ((s)!!.decor.getViewTreeObserver().isAlive()) {
                        (s)!!.decor.getViewTreeObserver().removeOnGlobalLayoutListener((s)!!.layout)
                        (s)!!.decor.getViewTreeObserver().removeOnPreDrawListener((s)!!.preDraw)
                    }
                    decors.remove((s)!!.decor)
                    windows.remove((s)!!.window)
                    controllers.values.removeIf(lambda@ { value -> (value == s) })
                }
            }
            override fun onActivityStarted(a: Activity) {

            }
            override fun onActivityStopped(a: Activity) {

            }
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {

            }
        })
        HookRuntime.log("[EasyBiliTool] IMMERSIVE_STATUS_READY video and live")
    }

    private inner class State {
        lateinit var activity: Activity

        lateinit var window: Window

        lateinit var decor: View

        @JvmField val handler: Handler = Handler(Looper.getMainLooper())

        @JvmField var resumed: Boolean = false
        @JvmField var applied: Boolean = false
        @JvmField var changing: Boolean = false
        @JvmField var contrast: Boolean = false

        @JvmField var originalColor: Int = 0
        @JvmField var originalUi: Int = 0
        @JvmField var originalFlags: Int = 0
        @JvmField var fitTypes: Int = 0
        @JvmField var cutout: Int = 0
        @JvmField var appearance: Int = 0

        @JvmField var statusHeight: Int = 0
        private var originalDecorFits = true
        private val livePadding = WeakHashMap<View, MenuInset>()
        private val legacyLiveProtection = WeakHashMap<View, Int>()
        private val liveBottomPadding = WeakHashMap<View, MenuInset>()
        private var originalNavigationColor = Color.BLACK
        private val protection by lazy { StatusBarProtection(window) }

        @JvmField var live: Boolean = false

        @JvmField var topMenuId: Int = 0
        @JvmField var liveHeaderId: Int = 0
        @JvmField var liveOperationsId: Int = 0
        @JvmField var liveSecondLineId: Int = 0
        @JvmField var liveBusinessId: Int = 0

        @JvmField val menus: MutableMap<View, MenuInset> = WeakHashMap()

        @JvmField val layout: ViewTreeObserver.OnGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener(this::update)

        @JvmField val preDraw: ViewTreeObserver.OnPreDrawListener = ViewTreeObserver.OnPreDrawListener(lambda@ { update()
            return@lambda PlaybackDiagnostics.allowFrame(activity, adjustMenu()) })

        @JvmField val tick: Runnable = object : Runnable {
            override fun run() {
                if (!resumed) {
                    return
                }
                update()
                handler.postDelayed(this, 1000)
            }
        }

        constructor(activity: Activity) {
            this.activity = activity
            window = activity.getWindow()
            decor = window.getDecorView()
            live = activity.javaClass.getName().equals("com.bilibili.bililive.room.ui.roomv3.LiveRoomActivityV3")
            topMenuId = id("nav_top_bar")
            liveHeaderId = id("native_top_container")
            liveOperationsId = id("top_operation_container")
            liveSecondLineId = id("ll_lynx_top_left_second_line")
            liveBusinessId = id("business_container")
        }

        fun id(name: String): Int {
            return activity.getResources().getIdentifier(name, "id", activity.getPackageName())
        }

        fun eligible(): Boolean {
            return (((((((resumed && (settings.get())!!.b("IMMERSIVE_STATUS_BAR")) && !activity.isFinishing()) && !activity.isDestroyed()) && (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)) && !activity.isInPictureInPictureMode()) && !activity.isInMultiWindowMode()) && (((window.getAttributes().flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)) == 0))
        }

        fun update() {
            if (changing) {
                return
            }
            try {
                if (activity.javaClass.name == "com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity")
                    RuntimeStatus.record("VIDEO_STATUS eligible=${eligible()} applied=$applied resumed=$resumed " +
                        "status=${settings.get().b("IMMERSIVE_STATUS_BAR")} protection=${settings.get().b("STATUS_BAR_PROTECTION")}")
                if (!eligible()) {
                    restore()
                    return
                }
                changing = true
                if (!applied) {
                    originalColor = window.getStatusBarColor()
                    originalNavigationColor = window.navigationBarColor
                    originalUi = decor.getSystemUiVisibility()
                    originalDecorFits = requestedDecorFits[window] ?: true
                    originalFlags = window.getAttributes().flags
                    if ((Build.VERSION.SDK_INT >= 29)) {
                        contrast = window.isStatusBarContrastEnforced()
                    }
                    if ((Build.VERSION.SDK_INT >= 30)) {
                        fitTypes = window.getAttributes().getFitInsetsTypes()
                        cutout = window.getAttributes().layoutInDisplayCutoutMode
                        var c: WindowInsetsController? = window.getInsetsController()
                        appearance = (if ((c == null)) 0 else (c)!!.getSystemBarsAppearance())
                    }
                    if(live && Build.VERSION.SDK_INT>=35) {
                        // Jetpack Window API avoids casting an injected host to a different ComponentActivity.
                        val navigationColor=window.navigationBarColor
                        val navigationContrast=window.isNavigationBarContrastEnforced
                        WindowCompat.enableEdgeToEdge(window)
                        if(!settings.get().b("NAVIGATION_EDGE_TO_EDGE")) {
                            window.navigationBarColor=navigationColor
                            window.isNavigationBarContrastEnforced=navigationContrast
                        }
                    } else if(live && Build.VERSION.SDK_INT>=30) {
                        WindowCompat.setDecorFitsSystemWindows(window,false)
                    }
                    applied = true
                    if ((statusHeight <= 0)) {
                        var id: Int = activity.getResources().getIdentifier("status_bar_height", "dimen", "android")
                        statusHeight = (if ((id == 0)) Math.round((24 * activity.getResources().getDisplayMetrics().density)) else activity.getResources().getDimensionPixelSize(id))
                    }
                    decor.requestApplyInsets()
                }
                // Preserve the host's requested icon contrast on both system-bar APIs.
                if(!live || Build.VERSION.SDK_INT<30) {
                    val ui=decor.systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    if(decor.systemUiVisibility!=ui) decor.systemUiVisibility=ui
                }
                var flags: Int = window.getAttributes().flags
                if ((((flags and FLAGS_MASK)) != WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS, FLAGS_MASK)
                }
                if ((window.getStatusBarColor() != Color.TRANSPARENT)) {
                    window.setStatusBarColor(Color.TRANSPARENT)
                }
                if (((Build.VERSION.SDK_INT >= 29) && window.isStatusBarContrastEnforced())) {
                    window.setStatusBarContrastEnforced(false)
                }
                if ((Build.VERSION.SDK_INT >= 30)) {
                    var p: WindowManager.LayoutParams = window.getAttributes()
                    var types: Int = (p.getFitInsetsTypes() and (WindowInsets.Type.statusBars()).inv())
                    if(live && !settings.get().b("NAVIGATION_EDGE_TO_EDGE")) {
                        val navigation=WindowInsets.Type.navigationBars()
                        types=(types and navigation.inv()) or (fitTypes and navigation)
                    }
                    if (((types != p.getFitInsetsTypes()) || (p.layoutInDisplayCutoutMode != WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS))) {
                        p.setFitInsetsTypes(types)
                        p.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                        window.setAttributes(p)
                    }
                    var c: WindowInsetsController? = window.getInsetsController()
                    if ((c != null)) {
                        controllers.put(c, this)
                    }
                }
                storyInsets.applyPending(activity)
                if(live && Build.VERSION.SDK_INT>=30) {
                    protection.apply(settings.get().b("STATUS_BAR_PROTECTION"),
                        originalNavigationColor.takeIf { !settings.get().b("NAVIGATION_EDGE_TO_EDGE") })
                } else if (settings.get().b("STATUS_BAR_PROTECTION")) protection.apply() else protection.restore()
                if (activity.javaClass.name == "com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity")
                    RuntimeStatus.record("VIDEO_PROTECTION ${protection.diagnostic()}")
                if (!decor.isLaidOut()) {
                    decor.requestApplyInsets()
                }
            } catch (e: Throwable) {
                changing = false
                restore()
                HookRuntime.log(("[EasyBiliTool] IMMERSIVE_STATUS_BYPASS " + e.javaClass.getSimpleName()))
            } finally {
                changing = false
            }
        }

        fun adjustMenu(): Boolean {
            if ((!applied || !eligible())) {
                return false
            }
            if (live) {
                var changed = insetLiveHeader(if(liveHeaderId==0) null else decor.findViewById(liveHeaderId),statusHeight)
                changed = insetLiveBottom() || changed
                changed = (offsetMenu(liveSecondaryHeader(), statusHeight) || changed)
                return (offsetMenu((if ((liveOperationsId == 0)) null else decor.findViewById(liveOperationsId)), statusHeight) || changed)
            }
            if ((topMenuId == 0)) {
                return false
            }
            var menu: View? = decor.findViewById(topMenuId)
            val p = menu?.layoutParams as? ViewGroup.MarginLayoutParams
            if ((((menu == null) || !(menu)!!.javaClass.getName().equals("com.bilibili.ship.theseus.united.widget.TouchAwareToolbar")) || !(p != null))) {
                return false
            }
            return offsetMenu(menu, MENU_OFFSET_PX)
        }

        fun liveSecondaryHeader(): View? {
            var line: View? = (if ((liveSecondLineId == 0)) null else decor.findViewById(liveSecondLineId))
            if (((line == null) || (liveBusinessId == 0))) {
                return null
            }
            run {
                var current: View = line
                while ((current !== decor)) {
                    if ((current.getId() == liveHeaderId)) {
                        return null
                    }
                    val parent = current!!.getParent() as? View
                    if (!(parent != null)) {
                        return null
                    }
                    if (((parent)!!.getId() == liveBusinessId)) {
                        return current
                    }
                    current = parent

                }
            }
            return null
        }

        private fun insetLiveBottom(): Boolean {
            val rootId=id("room_root_layout")
            val content=if(rootId==0) null else decor.findViewById<View>(rootId)
            if(content==null) return false
            val bottom=if(settings.get().b("NAVIGATION_EDGE_TO_EDGE")) 0 else {
                if(Build.VERSION.SDK_INT>=30) decor.rootWindowInsets?.getInsets(WindowInsets.Type.navigationBars())?.bottom ?: 0
                else decor.rootWindowInsets?.systemWindowInsetBottom ?: 0
            }
            val saved=liveBottomPadding.getOrPut(content) { MenuInset(content.paddingBottom) }
            if(content.paddingBottom!=saved.applied) saved.original=content.paddingBottom
            val target=maxOf(saved.original,bottom)
            saved.applied=target
            if(content.paddingBottom==target) return false
            content.setPadding(content.paddingLeft,content.paddingTop,content.paddingRight,target)
            return true
        }

        private fun insetLiveHeader(view: View?, inset: Int): Boolean {
            val header=view as? ViewGroup ?: return false
            if(header.height<=0) return false
            // Retire the native legacy scrim; optional system-bar protection is AndroidX ProtectionLayout.
            header.getChildAt(0)?.let { shade ->
                if(shade.javaClass==View::class.java && shade.background is GradientDrawable && shade.visibility==View.VISIBLE) {
                    legacyLiveProtection.putIfAbsent(shade,shade.visibility)
                    shade.visibility=View.INVISIBLE
                }
            }
            val saved=livePadding.getOrPut(header) { MenuInset(header.paddingTop) }
            if(header.paddingTop!=saved.applied) saved.original=header.paddingTop
            val top=saved.original+inset
            saved.applied=top
            if(header.paddingTop==top) return false
            header.setPadding(header.paddingLeft,top,header.paddingRight,header.paddingBottom)
            return true
        }

        fun offsetMenu(menu: View?, offset: Int): Boolean {
            val p = menu?.layoutParams as? ViewGroup.MarginLayoutParams
            if ((((menu == null) || ((menu)!!.getHeight() <= 0)) || !(p != null))) {
                return false
            }
            var saved: MenuInset? = menus.get(menu)
            if ((saved == null)) {
                saved = MenuInset((p)!!.topMargin)
                menus.put(menu, saved)
            } else {
                if (((p)!!.topMargin != (saved)!!.applied)) {
                    (saved)!!.original = (p)!!.topMargin
                }
            }
            var target: Int = ((saved)!!.original + offset)
            (saved)!!.applied = target
            if (((p)!!.topMargin == target)) {
                return false
            }
            (p)!!.topMargin = target
            (menu)!!.setLayoutParams(p)
            return true
        }

        fun restoreMenus() {
            liveBottomPadding.forEach { (view,saved) ->
                if(view.paddingBottom==saved.applied) view.setPadding(view.paddingLeft,view.paddingTop,view.paddingRight,saved.original)
            }
            liveBottomPadding.clear()
            livePadding.forEach { (view,saved) ->
                if(view.paddingTop==saved.applied) view.setPadding(view.paddingLeft,saved.original,view.paddingRight,view.paddingBottom)
            }
            livePadding.clear()
            legacyLiveProtection.forEach { (view,visibility) -> if(view.visibility==View.INVISIBLE) view.visibility=visibility }
            legacyLiveProtection.clear()
            for (entry in menus.entries) {

                var menu: View = entry.key
                var saved: MenuInset = entry.value
                val p = (menu)!!.getLayoutParams() as? ViewGroup.MarginLayoutParams
                if ((p != null && ((p)!!.topMargin == (saved)!!.applied))) {
                    (p)!!.topMargin = (saved)!!.original
                    (menu)!!.setLayoutParams(p)
                }
            }
            menus.clear()
        }

        fun restore() {
            if ((!applied || changing)) {
                return
            }
            changing = true
            applied = false
            try {
                protection.restore()
                restoreMenus()
                if(live && Build.VERSION.SDK_INT>=30) WindowCompat.setDecorFitsSystemWindows(window,originalDecorFits)
                window.setStatusBarColor(originalColor)
                window.setFlags(originalFlags, FLAGS_MASK)
                decor.setSystemUiVisibility((((decor.getSystemUiVisibility() and (UI_MASK).inv())) or ((originalUi and UI_MASK))))
                if ((Build.VERSION.SDK_INT >= 29)) {
                    window.setStatusBarContrastEnforced(contrast)
                }
                if ((Build.VERSION.SDK_INT >= 30)) {
                    var p: WindowManager.LayoutParams = window.getAttributes()
                    var status: Int = WindowInsets.Type.statusBars()
                    (p)!!.setFitInsetsTypes(((((p)!!.getFitInsetsTypes() and (status).inv())) or ((fitTypes and status))))
                    (p)!!.layoutInDisplayCutoutMode = cutout
                    window.setAttributes(p)
                    var c: WindowInsetsController? = window.getInsetsController()
                    if ((c != null)) {
                        (c)!!.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                    }
                }
                decor.requestApplyInsets()
            } catch (e: Throwable) {
                HookRuntime.log(("[EasyBiliTool] IMMERSIVE_STATUS_RESTORE " + e.javaClass.getSimpleName()))
            } finally {
                changing = false
            }
        }
    }

    private class MenuInset {
        @JvmField var original: Int = 0
        @JvmField var applied: Int = 0

        constructor(margin: Int) {
            original = margin
            applied = margin
        }
    }

    companion object {
        private val MENU_OFFSET_PX: Int = 80

        private val UI_MASK: Int = ((View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE) or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)

        private val FLAGS_MASK: Int = (WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    }
}
