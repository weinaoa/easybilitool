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
import android.os.*
import android.view.*
import java.util.*
import java.util.function.Supplier

/**
 * Transparent status bar in the verified 8.95 video and live-room windows.
 */
class ImmersiveStatusBarHook {
    private lateinit var settings: Supplier<Config>

    private val activities: MutableMap<Activity, State> = WeakHashMap()

    private val decors: MutableMap<View, State> = WeakHashMap()

    private val windows: MutableMap<Window, State> = WeakHashMap()

    private val controllers: MutableMap<WindowInsetsController, State> = WeakHashMap()

    constructor(settings: Supplier<Config>) {
        this.settings = settings
    }

    @Throws(Exception::class)
    fun install(application: Application) {
        Reflector.findAndHookMethod(View::class.java, "dispatchApplyWindowInsets", WindowInsets::class.java, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                var state: State? = decors.get(p.thisObject)
                if (((state == null) || !(state)!!.eligible())) {
                    return
                }
                var insets: WindowInsets = (p.args[0] as WindowInsets)
                var top: Int = (if ((Build.VERSION.SDK_INT >= 30)) insets.getInsetsIgnoringVisibility((WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout())).top else insets.getSystemWindowInsetTop())
                if ((top > 0)) {
                    (state)!!.statusHeight = top
                }
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
                if ((state)!!.eligible()) {
                    p.args[0] = (((((p.args[0] as Int) or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)) and (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR).inv())
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
            Reflector.findAndHookMethod(Class.forName("android.view.InsetsController"), "setSystemBarsAppearance", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) {
                    var state: State? = controllers.get(p.thisObject)
                    if ((((state == null) || (state)!!.changing) || !(state)!!.applied)) {
                        return
                    }
                    var value: Int = (p.args[0] as Int)
                    var mask: Int = (p.args[1] as Int)
                    (state)!!.appearance = ((((state)!!.appearance and (mask).inv())) or ((value and mask)))
                    if ((state)!!.eligible()) {
                        p.args[0] = (value and (WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS).inv())
                    }
                }
            })
        }
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, b: Bundle?) {
                var name: String = a.javaClass.getName()
                if (((!name.equals("com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity") && !name.equals("com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity")) && !name.equals("com.bilibili.bililive.room.ui.roomv3.LiveRoomActivityV3"))) {
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
        HookRuntime.log("[EasyBiliTool] IMMERSIVE_STATUS_READY 8.95 video and live")
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

        @JvmField val scrim: GradientDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x88000000.toInt(), 0x00000000))

        @JvmField var statusHeight: Int = 0

        @JvmField var live: Boolean = false

        @JvmField var topMenuId: Int = 0
        @JvmField var liveHeaderId: Int = 0
        @JvmField var liveOperationsId: Int = 0
        @JvmField var liveSecondLineId: Int = 0
        @JvmField var liveBusinessId: Int = 0

        @JvmField val menus: MutableMap<View, MenuInset> = WeakHashMap()

        @JvmField val layout: ViewTreeObserver.OnGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener(this::update)

        @JvmField val preDraw: ViewTreeObserver.OnPreDrawListener = ViewTreeObserver.OnPreDrawListener(lambda@ { update()
            return@lambda !adjustMenu() })

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
                if (!eligible()) {
                    restore()
                    return
                }
                changing = true
                if (!applied) {
                    originalColor = window.getStatusBarColor()
                    originalUi = decor.getSystemUiVisibility()
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
                    applied = true
                    decor.getOverlay().add(scrim)
                    if ((statusHeight <= 0)) {
                        var id: Int = activity.getResources().getIdentifier("status_bar_height", "dimen", "android")
                        statusHeight = (if ((id == 0)) Math.round((24 * activity.getResources().getDisplayMetrics().density)) else activity.getResources().getDimensionPixelSize(id))
                    }
                    decor.requestApplyInsets()
                }
                var ui: Int = ((((decor.getSystemUiVisibility() or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)) and (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR).inv())
                if ((decor.getSystemUiVisibility() != ui)) {
                    decor.setSystemUiVisibility(ui)
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
                    if (((types != p.getFitInsetsTypes()) || (p.layoutInDisplayCutoutMode != WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS))) {
                        p.setFitInsetsTypes(types)
                        p.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                        window.setAttributes(p)
                    }
                    var c: WindowInsetsController? = window.getInsetsController()
                    if ((c != null)) {
                        controllers.put(c, this)
                        if (((((c)!!.getSystemBarsAppearance() and WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)) != 0)) {
                            (c)!!.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                        }
                    }
                }
                scrim.setBounds(0, 0, decor.getWidth(), (statusHeight + Math.round((12 * activity.getResources().getDisplayMetrics().density))))
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
                var changed: Boolean = offsetMenu((if ((liveHeaderId == 0)) null else decor.findViewById(liveHeaderId)), statusHeight)
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
                restoreMenus()
                decor.getOverlay().remove(scrim)
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
