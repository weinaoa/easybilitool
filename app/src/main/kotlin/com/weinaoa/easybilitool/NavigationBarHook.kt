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
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.TypedValue
import android.view.*
import java.util.*
import java.util.function.Supplier

/**
 * Navigation-only edge-to-edge. Preserve host status-bar and keyboard ownership.
 */
class NavigationBarHook {
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
                var s: State? = decors.get(p.thisObject)
                if ((s == null)) {
                    return
                }
                var insets: WindowInsets = (p.args[0] as WindowInsets)
                (s)!!.captureInsets(insets)
                if ((!(s)!!.applied || !(s)!!.eligible())) {
                    return
                }
                if ((Build.VERSION.SDK_INT >= 30)) {
                    p.args[0] = WindowInsets.Builder(insets).setInsets(WindowInsets.Type.navigationBars(), Insets.NONE).setInsetsIgnoringVisibility(WindowInsets.Type.navigationBars(), Insets.NONE).build()
                } else {
                    p.args[0] = insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0)
                }
            }
        })
        var phoneWindow: Class<*> = Class.forName("com.android.internal.policy.PhoneWindow")
        Reflector.findAndHookMethod(phoneWindow, "setNavigationBarColor", Int::class.javaPrimitiveType!!, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                var s: State? = windows.get(p.thisObject)
                if ((((s != null) && (s)!!.applied) && !(s)!!.changing)) {
                    (s)!!.originalColor = (p.args[0] as Int)
                    if ((s)!!.eligible()) {
                        p.args[0] = Color.TRANSPARENT
                    }
                }
            }
        })
        if ((Build.VERSION.SDK_INT >= 28)) {
            Reflector.findAndHookMethod(phoneWindow, "setNavigationBarDividerColor", Int::class.javaPrimitiveType!!, object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) {
                    var s: State? = windows.get(p.thisObject)
                    if ((((s != null) && (s)!!.applied) && !(s)!!.changing)) {
                        (s)!!.originalDivider = (p.args[0] as Int)
                        if ((s)!!.eligible()) {
                            p.args[0] = Color.TRANSPARENT
                        }
                    }
                }
            })
        }
        Reflector.findAndHookMethod(View::class.java, "setSystemUiVisibility", Int::class.javaPrimitiveType!!, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                var s: State? = decors.get(p.thisObject)
                if ((((s == null) || !(s)!!.applied) || (s)!!.changing)) {
                    return
                }
                var requested: Int = (p.args[0] as Int)
                (s)!!.originalUi = ((((s)!!.originalUi and (View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR).inv())) or ((requested and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)))
                if ((s)!!.eligible()) {
                    p.args[0] = (s)!!.navigationUi(requested)
                }
            }
        })
        if ((Build.VERSION.SDK_INT >= 30)) {
            Reflector.findAndHookMethod(Class.forName("android.view.InsetsController"), "setSystemBarsAppearance", Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) {
                    var s: State? = controllers.get(p.thisObject)
                    if ((((s == null) || !(s)!!.applied) || (s)!!.changing)) {
                        return
                    }
                    var mask: Int = (p.args[1] as Int)
                    var requested: Int = (p.args[0] as Int)
                    if ((((mask and WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)) == 0)) {
                        return
                    }
                    (s)!!.originalAppearance = ((((s)!!.originalAppearance and (mask).inv())) or ((requested and mask)))
                    if ((s)!!.eligible()) {
                        p.args[0] = (((requested and (WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS).inv())) or ((if ((s)!!.lightBackground()) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0)))
                    }
                }
            })
        }
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private fun prepare(a: Activity) {
                val state = activities.getOrPut(a) {
                    State(a).also {
                        decors[it.decor] = it
                        windows[it.window] = it
                        it.decor.viewTreeObserver.addOnPreDrawListener(it.preDraw)
                    }
                }
                state.visibility.started()
                state.update()
            }
            override fun onActivityPreCreated(a: Activity, b: Bundle?) = prepare(a)
            override fun onActivityCreated(a: Activity, b: Bundle?) = prepare(a)
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
                }
            }
            override fun onActivityDestroyed(a: Activity) {
                var s: State? = activities.remove(a)
                if ((s != null)) {
                    (s)!!.visibility.stopped()
                    (s)!!.resumed = false
                    (s)!!.handler.removeCallbacks((s)!!.tick)
                    (s)!!.restore()
                    if ((s)!!.decor.getViewTreeObserver().isAlive()) {
                        (s)!!.decor.getViewTreeObserver().removeOnPreDrawListener((s)!!.preDraw)
                    }
                    decors.remove((s)!!.decor)
                    windows.remove((s)!!.window)
                    controllers.values.removeIf(lambda@ { value -> (value == s) })
                }
            }
            override fun onActivityStarted(a: Activity) {
                activities[a]?.let { it.visibility.started(); it.update() }
            }
            override fun onActivityStopped(a: Activity) {
                activities[a]?.let {
                    it.visibility.stopped()
                    it.handler.removeCallbacks(it.tick)
                    it.restore()
                }
            }
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {

            }
        })
        HookRuntime.log("[EasyBiliTool] NAVIGATION_EDGE_READY bottom insets preserved")
    }

    private inner class State {
        lateinit var activity: Activity

        lateinit var window: Window

        lateinit var decor: View

        @JvmField val handler: Handler = Handler(Looper.getMainLooper())

        @JvmField val preDraw: ViewTreeObserver.OnPreDrawListener = ViewTreeObserver.OnPreDrawListener(lambda@ { !update() })

        @JvmField val tick: Runnable = object : Runnable {
            override fun run() {
                update()
                if (resumed) {
                    handler.postDelayed(this, 500)
                }
            }
        }

        @JvmField var content: View? = null

        lateinit var mainInsets: MainNavigationInsets

        lateinit var settingsInsets: SettingsNavigationInsets

        lateinit var detailInsets: DetailNavigationInsets

        @JvmField val pageInsets: PageNavigationInsets = PageNavigationInsets()

        val visibility = NavigationVisibility()

        @JvmField var resumed: Boolean = false
        @JvmField var changing: Boolean = false
        @JvmField var applied: Boolean = false
        @JvmField var contrast: Boolean = false
        @JvmField var imeVisible: Boolean = false
        @JvmField var failed: Boolean = false
        @JvmField var topApplied: Boolean = false

        @JvmField var bottomInset: Int = 0
        @JvmField var statusInset: Int = 0
        @JvmField var originalTop: Int = 0
        @JvmField var lastTop: Int = 0
        @JvmField var originalColor: Int = 0
        @JvmField var originalDivider: Int = 0
        @JvmField var originalUi: Int = 0
        @JvmField var originalFlags: Int = 0
        @JvmField var originalFitTypes: Int = 0
        @JvmField var originalAppearance: Int = 0

        @JvmField val contentLocation: IntArray = IntArray(2)

        constructor(a: Activity) {
            activity = a
            window = a.getWindow()
            decor = window.getDecorView()
            mainInsets = MainNavigationInsets(a)
            settingsInsets = SettingsNavigationInsets(a)
            detailInsets = DetailNavigationInsets(a)
        }

        fun captureInsets(insets: WindowInsets) {
            if ((Build.VERSION.SDK_INT >= 30)) {
                bottomInset = insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom
                statusInset = insets.getInsets(WindowInsets.Type.statusBars()).top
                if (insets.isVisible(WindowInsets.Type.navigationBars())) {
                    bottomInset = JvmNumbers.max(bottomInset, insets.getInsets(WindowInsets.Type.mandatorySystemGestures()).bottom)
                }
                imeVisible = insets.isVisible(WindowInsets.Type.ime())
            } else {
                bottomInset = insets.getStableInsetBottom()
                statusInset = insets.getSystemWindowInsetTop()
                imeVisible = (insets.getSystemWindowInsetBottom() > (bottomInset + Math.round((80 * activity.getResources().getDisplayMetrics().density))))
            }
        }

        fun eligible(): Boolean {
            return ((((((((visibility.visible && (settings.get())!!.b("NAVIGATION_EDGE_TO_EDGE"))) && !activity.isDestroyed()) && !imeVisible) && (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)) && !activity.isInPictureInPictureMode()) && !activity.isInMultiWindowMode()) && (((decor.getSystemUiVisibility() and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)) == 0))
        }

        fun lightBackground(): Boolean {
            if (detailInsets.darkSurface()) {
                return false
            }
            val fill = content?.background as? ColorDrawable
            if (((content != null) && fill != null)) {
                return (Color.luminance(fill!!.color) > .5)
            }
            var color: TypedValue = TypedValue()
            if (((activity.getTheme().resolveAttribute(android.R.attr.colorBackground, color, true) && ((color)!!.type >= TypedValue.TYPE_FIRST_COLOR_INT)) && ((color)!!.type <= TypedValue.TYPE_LAST_COLOR_INT))) {
                return (Color.luminance((color)!!.data) > .5)
            }
            return (((activity.getResources().getConfiguration().uiMode and Configuration.UI_MODE_NIGHT_MASK)) != Configuration.UI_MODE_NIGHT_YES)
        }

        fun navigationUi(ui: Int): Int {
            return (((((ui or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)) and (View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR).inv())) or ((if (lightBackground()) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)))
        }

        fun preserveVideoStatusInset(): Boolean {
            if ((!detailInsets.videoSurface() || (((decor.getSystemUiVisibility() and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)) != 0))) {
                return restoreTop()
            }
            (content)!!.getLocationInWindow(contentLocation)
            var safe: Int = JvmNumbers.max(0, (statusInset - contentLocation[1]))
            if (!topApplied) {
                if (((content)!!.getPaddingTop() >= safe)) {
                    return false
                }
                originalTop = (content)!!.getPaddingTop()
                topApplied = true
            } else {
                if (((content)!!.getPaddingTop() != lastTop)) {
                    originalTop = (content)!!.getPaddingTop()
                }
            }
            lastTop = JvmNumbers.max(originalTop, safe)
            if (((content)!!.getPaddingTop() == lastTop)) {
                return false
            }
            (content)!!.setPadding((content)!!.getPaddingLeft(), lastTop, (content)!!.getPaddingRight(), (content)!!.getPaddingBottom())
            return true
        }

        fun restoreTop(): Boolean {
            if (!topApplied) {
                return false
            }
            topApplied = false
            if (((content)!!.getPaddingTop() != lastTop)) {
                return false
            }
            (content)!!.setPadding((content)!!.getPaddingLeft(), originalTop, (content)!!.getPaddingRight(), (content)!!.getPaddingBottom())
            return true
        }

        fun update(): Boolean {
            if ((changing || failed)) {
                return false
            }
            var layoutChanged: Boolean = false
            changing = true
            try {
                var insets: WindowInsets? = decor.getRootWindowInsets()
                if ((insets != null)) {
                    captureInsets(insets)
                }
                if (!eligible()) {
                    restore()
                    return false
                }
                if (!applied) {
                    originalColor = window.getNavigationBarColor()
                    originalUi = decor.getSystemUiVisibility()
                    originalFlags = window.getAttributes().flags
                    if ((Build.VERSION.SDK_INT >= 28)) {
                        originalDivider = window.getNavigationBarDividerColor()
                    }
                    if ((Build.VERSION.SDK_INT >= 29)) {
                        contrast = window.isNavigationBarContrastEnforced()
                    }
                    if ((Build.VERSION.SDK_INT >= 30)) {
                        originalFitTypes = window.getAttributes().getFitInsetsTypes()
                        var c: WindowInsetsController? = window.getInsetsController()
                        originalAppearance = (if ((c == null)) 0 else (c)!!.getSystemBarsAppearance())
                    }
                    applied = true
                    decor.requestApplyInsets()
                }
                var ui: Int = navigationUi(decor.getSystemUiVisibility())
                if ((ui != decor.getSystemUiVisibility())) {
                    decor.setSystemUiVisibility(ui)
                }
                if ((((window.getAttributes().flags and FLAGS_MASK)) != WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS, FLAGS_MASK)
                }
                if ((window.getNavigationBarColor() != Color.TRANSPARENT)) {
                    window.setNavigationBarColor(Color.TRANSPARENT)
                }
                if (((Build.VERSION.SDK_INT >= 28) && (window.getNavigationBarDividerColor() != Color.TRANSPARENT))) {
                    window.setNavigationBarDividerColor(Color.TRANSPARENT)
                }
                if (((Build.VERSION.SDK_INT >= 29) && window.isNavigationBarContrastEnforced())) {
                    window.setNavigationBarContrastEnforced(false)
                }
                if ((Build.VERSION.SDK_INT >= 30)) {
                    var p: WindowManager.LayoutParams = window.getAttributes()
                    var fit: Int = (p.getFitInsetsTypes() and (WindowInsets.Type.navigationBars()).inv())
                    if ((fit != p.getFitInsetsTypes())) {
                        p.setFitInsetsTypes(fit)
                        window.setAttributes(p)
                    }
                    var c: WindowInsetsController? = window.getInsetsController()
                    if ((c != null)) {
                        controllers.put(c, this)
                        var appearance: Int = (if (lightBackground()) WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS else 0)
                        if (((((c)!!.getSystemBarsAppearance() and WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)) != appearance)) {
                            (c)!!.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
                        }
                    }
                }
                // Prepare the window before its first layout; defer measured safe distances.
                val next: View? = decor.findViewById(android.R.id.content)
                if (next == null || !next.isLaidOut || bottomInset <= 0) return false
                if (content !== next) {
                    mainInsets.restore()
                    settingsInsets.restore()
                    detailInsets.restore()
                    pageInsets.restore()
                    restoreTop()
                    content = next
                }
                var main: Boolean = mainInsets.bind(content!!)
                var preferences: Boolean = (!main && settingsInsets.bind(content!!))
                var detail: Boolean = ((!main && !preferences) && detailInsets.bind(content!!))
                if (((main || preferences) || detail)) {
                    pageInsets.restore()
                    layoutChanged = (((if (main) mainInsets.apply(bottomInset) else (if (preferences) settingsInsets.apply(bottomInset) else detailInsets.apply(content!!, bottomInset)))) || layoutChanged)
                } else {
                    layoutChanged = (pageInsets.apply(content!!, bottomInset) || layoutChanged)
                }
                layoutChanged = (preserveVideoStatusInset() || layoutChanged)
            } catch (e: Throwable) {
                restore()
                failed = true
                HookRuntime.log(("[EasyBiliTool] NAVIGATION_EDGE_BYPASS: " + e.javaClass.getSimpleName()))
            } finally {
                changing = false
            }
            return layoutChanged
        }

        fun restore() {
            if (!applied) {
                return
            }
            var previous: Boolean = changing
            changing = true
            applied = false
            try {
                mainInsets.restore()
                settingsInsets.restore()
                detailInsets.restore()
                pageInsets.restore()
                restoreTop()
                window.setNavigationBarColor(originalColor)
                if ((Build.VERSION.SDK_INT >= 28)) {
                    window.setNavigationBarDividerColor(originalDivider)
                }
                window.setFlags(originalFlags, FLAGS_MASK)
                decor.setSystemUiVisibility((((decor.getSystemUiVisibility() and (UI_MASK).inv())) or ((originalUi and UI_MASK))))
                if ((Build.VERSION.SDK_INT >= 29)) {
                    window.setNavigationBarContrastEnforced(contrast)
                }
                if ((Build.VERSION.SDK_INT >= 30)) {
                    var p: WindowManager.LayoutParams = window.getAttributes()
                    var nav: Int = WindowInsets.Type.navigationBars()
                    p.setFitInsetsTypes((((p.getFitInsetsTypes() and (nav).inv())) or ((originalFitTypes and nav))))
                    window.setAttributes(p)
                    var c: WindowInsetsController? = window.getInsetsController()
                    if ((c != null)) {
                        (c)!!.setSystemBarsAppearance(originalAppearance, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
                    }
                }
                decor.requestApplyInsets()
            } catch (e: Throwable) {
                HookRuntime.log(("[EasyBiliTool] NAVIGATION_EDGE_RESTORE: " + e.javaClass.getSimpleName()))
            } finally {
                changing = previous
            }
        }
    }

    companion object {
        private val UI_MASK: Int = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)

        private val FLAGS_MASK: Int = (WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    }
}
