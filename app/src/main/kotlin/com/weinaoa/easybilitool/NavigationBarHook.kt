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
import android.content.Context
import android.content.ContextWrapper
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
    private val dialogs: MutableMap<Dialog, State> = WeakHashMap()

    private val decors: MutableMap<View, State> = WeakHashMap()

    private val windows: MutableMap<Window, State> = WeakHashMap()

    private val controllers: MutableMap<WindowInsetsController, State> = WeakHashMap()

    constructor(settings: Supplier<Config>) {
        this.settings = settings
    }

    @Throws(Exception::class)
    fun install(application: Application) {
        Reflector.findAndHookMethod(Dialog::class.java, "show", object : MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) {
                val dialog = p.thisObject as Dialog
                val window = dialog.window ?: return
                val owner = dialog.ownerActivity ?: ownerActivity(dialog.context) ?:
                    activities.entries.filter { it.value.resumed }.singleOrNull()?.key ?: return
                val id = owner.resources.getIdentifier("design_bottom_sheet", "id", owner.packageName)
                var sheet = if (id == 0) null else window.decorView.findViewById<View>(id) as? ViewGroup
                // 9.16 shares use a fullscreen ComponentDialog (CENTER), with
                // their actual bottom sheet in j, rather than design_bottom_sheet.
                if (sheet == null && dialog.javaClass.name == "kntr.common.share.api.O") {
                    sheet = runCatching { Reflector.findField(dialog.javaClass, "j").get(dialog) as? ViewGroup }.getOrNull()
                        ?.takeIf { it.javaClass.name == "kntr.common.share.api.V" && it.rootView === window.decorView }
                }
                val nativeSheet = sheet != null
                if (sheet == null) {
                    val content = window.decorView.findViewById<ViewGroup>(android.R.id.content)
                    sheet = content
                }
                if (sheet == null || dialogs.containsKey(dialog)) return
                val state = State(owner, window, sheet, !nativeSheet)
                dialogs[dialog] = state
                decors[state.decor] = state
                windows[state.window] = state
                state.decor.viewTreeObserver.addOnPreDrawListener(state.preDraw)
                state.visibility.started()
                state.resumed = true
                state.update()
                state.handler.post(state.tick)
            }
        })
        Reflector.findAndHookMethod(Dialog::class.java, "dismiss", object : MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) {
                val dialog = p.thisObject as Dialog
                if (Looper.myLooper() == Looper.getMainLooper()) removeDialog(dialog)
                else Handler(Looper.getMainLooper()).post { removeDialog(dialog) }
            }
        })
        Reflector.findAndHookMethod(View::class.java, "dispatchApplyWindowInsets", WindowInsets::class.java, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                var s: State? = decors.get(p.thisObject)
                if ((s == null)) {
                    return
                }
                var insets: WindowInsets = (p.args[0] as WindowInsets)
                (s)!!.captureInsets(insets)
                // IME keeps its own complete inset stream. Its visibility must
                // not tear down the modern video's edge-to-edge window mode.
                if (s.imeVisible || s.keyboardTransition()) return
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
                    if (s.keyboardTransition()) return
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
                dialogs.entries.filter { it.value.activity === a }.map { it.key }.forEach(::removeDialog)
                var s: State? = activities.remove(a)
                if ((s != null)) {
                    (s)!!.visibility.stopped()
                    (s)!!.resumed = false
                    (s)!!.handler.removeCallbacks((s)!!.tick)
                    (s)!!.restore()
                    s.dispose()
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
                dialogs.filter { it.value.activity === a && it.key.isShowing }.values.forEach {
                    it.visibility.started(); it.resumed = true; it.update()
                    it.handler.removeCallbacks(it.tick); it.handler.post(it.tick)
                }
            }
            override fun onActivityStopped(a: Activity) {
                dialogs.values.filter { it.activity === a }.forEach {
                    it.visibility.stopped(); it.resumed = false
                    it.handler.removeCallbacks(it.tick); it.restore()
                }
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

    private fun ownerActivity(context: Context): Activity? {
        var current = context
        val visited = Collections.newSetFromMap(IdentityHashMap<Context, Boolean>())
        while (visited.add(current)) {
            if (current is Activity) return current
            current = (current as? ContextWrapper)?.baseContext ?: return null
        }
        return null
    }

    private fun removeDialog(dialog: Dialog) {
        val state = dialogs.remove(dialog) ?: return
        state.visibility.stopped()
        state.resumed = false
        state.handler.removeCallbacks(state.tick)
        state.restore()
        state.dispose()
        if (state.decor.viewTreeObserver.isAlive) state.decor.viewTreeObserver.removeOnPreDrawListener(state.preDraw)
        decors.remove(state.decor)
        windows.remove(state.window)
        controllers.values.removeIf { it === state }
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

        lateinit var composeHomeInsets: ComposeHomeNavigation

        lateinit var mainInsets: MainNavigationInsets

        lateinit var settingsInsets: SettingsNavigationInsets

        lateinit var detailInsets: DetailNavigationInsets

        @JvmField val pageInsets: PageNavigationInsets = PageNavigationInsets()
        private val quickConsumeInsets = QuickConsumeInsets()
        private lateinit var liveWebInsets: LiveWebInsets

        val visibility = NavigationVisibility()
        private var sheetInsets: BottomSheetNavigationInsets? = null
        private var dialogWindow = false
        private var requireBottomWindow = false
        private var stableVideoIme = false
        private var imeObserver: View? = null
        private var animatedImeBottom: Int? = null
        private val imeAnimations = Collections.newSetFromMap(IdentityHashMap<WindowInsetsAnimation, Boolean>())
        private var nativeReplyInputId = 0

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

        constructor(a: Activity, target: Window = a.window, sheet: ViewGroup? = null, requireBottom: Boolean = false) {
            activity = a
            window = target
            decor = window.getDecorView()
            dialogWindow = sheet != null
            requireBottomWindow = requireBottom
            stableVideoIme = Build.VERSION.SDK_INT >= 30 && a.javaClass.name in setOf(
                "com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity",
                "com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity") && runCatching {
                Class.forName("kntr.common.comment.page.ui.b0", false, a.classLoader)
            }.isSuccess
            if (!dialogWindow && a.javaClass.name == "com.bilibili.lib.ui.GeneralActivity")
                nativeReplyInputId = a.resources.getIdentifier("fake_input_layout", "id", a.packageName)
            sheetInsets = sheet?.let(::BottomSheetNavigationInsets)
            mainInsets = MainNavigationInsets(a)
            composeHomeInsets = ComposeHomeNavigation(a)
            settingsInsets = SettingsNavigationInsets(a)
            detailInsets = DetailNavigationInsets(a)
            liveWebInsets = LiveWebInsets(a)
            if (stableVideoIme) observeImeAnimation()
        }

        fun keyboardTransition(): Boolean = stableVideoIme && (imeVisible || imeAnimations.isNotEmpty())

        @android.annotation.TargetApi(30)
        private fun observeImeAnimation() {
            if (Build.VERSION.SDK_INT < 30) return
            val parent = decor as? ViewGroup ?: return
            // A zero-size sibling observes platform animation completion without
            // replacing any host callback or consuming its inset dispatch.
            val observer = View(window.context).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setWindowInsetsAnimationCallback(object : WindowInsetsAnimation.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                    override fun onPrepare(animation: WindowInsetsAnimation) {
                        if (animation.typeMask and WindowInsets.Type.ime() != 0) {
                            if (imeAnimations.isEmpty()) animatedImeBottom = decor.rootWindowInsets?.getInsets(WindowInsets.Type.ime())?.bottom
                            imeAnimations.add(animation)
                        }
                    }
                    override fun onProgress(insets: WindowInsets, animations: MutableList<WindowInsetsAnimation>): WindowInsets {
                        if (imeAnimations.isNotEmpty()) animatedImeBottom = insets.getInsets(WindowInsets.Type.ime()).bottom
                        return insets
                    }
                    override fun onEnd(animation: WindowInsetsAnimation) {
                        if (imeAnimations.remove(animation)) {
                            if (imeAnimations.isEmpty()) animatedImeBottom = null
                            this@State.handler.post {
                                if (this@State.visibility.visible) { decor.requestApplyInsets(); this@State.update() }
                            }
                        }
                    }
                })
            }
            parent.addView(observer, ViewGroup.LayoutParams(0, 0))
            imeObserver = observer
        }

        fun dispose() {
            imeObserver?.let { (it.parent as? ViewGroup)?.removeView(it) }
            imeObserver = null
            animatedImeBottom = null
            imeAnimations.clear()
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
            val config = settings.get() ?: return false
            // Compose may assign full-width/bottom positioning after Dialog.show().
            // Observe the live attributes rather than rejecting that window before layout.
            if (requireBottomWindow && (window.attributes.width != WindowManager.LayoutParams.MATCH_PARENT ||
                window.attributes.gravity and Gravity.VERTICAL_GRAVITY_MASK != Gravity.BOTTOM)) return false
            return visibility.visible && config.b("NAVIGATION_EDGE_TO_EDGE") &&
                !activity.isDestroyed && (!imeVisible || stableVideoIme) &&
                activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&
                !activity.isInPictureInPictureMode && !activity.isInMultiWindowMode &&
                decor.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
        }

        fun lightBackground(): Boolean {
            if (!dialogWindow) liveWebInsets.background?.let { return Color.luminance(it) > .5 }
            if (dialogWindow) {
                val fill = sheetInsets?.surfaceColor()
                if (fill != null && Color.alpha(fill) == 255) return Color.luminance(fill) > .5
            }
            if (detailInsets.darkSurface()) {
                return false
            }
            val fill = content?.background as? ColorDrawable
            if (((content != null) && fill != null)) {
                return (Color.luminance(fill!!.color) > .5)
            }
            var color: TypedValue = TypedValue()
            if (((window.context.theme.resolveAttribute(android.R.attr.colorBackground, color, true) && ((color)!!.type >= TypedValue.TYPE_FIRST_COLOR_INT)) && ((color)!!.type <= TypedValue.TYPE_LAST_COLOR_INT))) {
                return (Color.luminance((color)!!.data) > .5)
            }
            return (((activity.getResources().getConfiguration().uiMode and Configuration.UI_MODE_NIGHT_MASK)) != Configuration.UI_MODE_NIGHT_YES)
        }

        fun navigationUi(ui: Int): Int {
            if (keyboardTransition()) return ui or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            return (((((ui or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)) and (View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR).inv())) or ((if (lightBackground()) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)))
        }

        private fun windowFlagsMask(): Int = FLAGS_MASK or
            (if (dialogWindow) WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN else 0)

        fun preserveNativeStatusInset(): Boolean {
            // A legacy reply fragment has a real EditText inside fake_input_layout.
            // Its AppCompat root loses the top fit when LAYOUT_HIDE_NAVIGATION is
            // enabled. Compose reply pages own their insets and do not match this.
            val nativeReply = nativeReplyInputId != 0 && content?.findViewById<View>(nativeReplyInputId)?.let { input ->
                    fun hasEditor(view: View): Boolean = view is android.widget.EditText ||
                        (view is ViewGroup && (0 until view.childCount).any { hasEditor(view.getChildAt(it)) })
                    hasEditor(input)
                } == true
            if ((!detailInsets.videoSurface() && !nativeReply) ||
                decor.systemUiVisibility and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN != 0) {
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
                VideoWindowGeometry.record(activity)
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
                val requiredFlags = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                    (if (dialogWindow) WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN else 0)
                if ((window.attributes.flags and windowFlagsMask()) != requiredFlags) {
                    window.setFlags(requiredFlags, windowFlagsMask())
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
                        if (!keyboardTransition() && (((c)!!.getSystemBarsAppearance() and WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS) != appearance)) {
                            (c)!!.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
                        }
                    }
                }
                // Record the raw main-window state even if no tab bar can be bound.
                if (activity.javaClass.name == "tv.danmaku.bili.MainActivityV2") {
                    RuntimeStatus.record("MAIN_NAV_WINDOW inset=$bottomInset ime=$imeVisible " +
                        "decor=${decor.width}x${decor.height} ui=${decor.systemUiVisibility} " +
                        "fit=${if (Build.VERSION.SDK_INT >= 30) window.attributes.fitInsetsTypes else -1}")
                }
                if (keyboardTransition()) {
                    // The host moves the editor with the real animated IME.
                    // Fill only the navigation distance not covered by that IME,
                    // avoiding a separate safe-padding jump after onEnd().
                    if (dialogWindow) {
                        val keyboardBottom = animatedImeBottom ?: (if (Build.VERSION.SDK_INT >= 30) insets?.getInsets(WindowInsets.Type.ime())?.bottom else null) ?: 0
                        return sheetInsets!!.apply((bottomInset - keyboardBottom).coerceAtLeast(0))
                    }
                    return false
                }
                // Prepare the window before its first layout; defer measured safe distances.
                val next: View? = decor.findViewById(android.R.id.content)
                if (activity.javaClass.name == "tv.danmaku.bili.MainActivityV2")
                    RuntimeStatus.record("MAIN_NAV_CONTENT type=${next?.javaClass?.name} height=${next?.height} laidOut=${next?.isLaidOut}")
                if (next == null || !next.isLaidOut || bottomInset <= 0) return false
                if (content !== next) {
                    mainInsets.restore()
                    composeHomeInsets.restore()
                    settingsInsets.restore()
                    detailInsets.restore()
                    pageInsets.restore()
                    liveWebInsets.restore()
                    quickConsumeInsets.restore()
                    restoreTop()
                    content = next
                }
                if (dialogWindow) {
                    return sheetInsets!!.apply(bottomInset)
                }
                var main: Boolean = mainInsets.bind(content!!)
                val composeMain = !main && composeHomeInsets.bind(content!!)
                var preferences: Boolean = (!main && !composeMain && settingsInsets.bind(content!!))
                var detail: Boolean = ((!main && !composeMain && !preferences) && detailInsets.bind(content!!))
                val liveWeb = liveWebInsets.find()
                if (activity.javaClass.name == "com.bilibili.bplus.followinglist.quick.consume.QuickConsumeActivity") {
                    pageInsets.restore()
                    layoutChanged = quickConsumeInsets.apply(content!!, bottomInset) || layoutChanged
                } else if (liveWeb != null) {
                    pageInsets.restore()
                    layoutChanged = liveWebInsets.apply(liveWeb, content!!, bottomInset) || layoutChanged
                } else if (composeMain) {
                    liveWebInsets.restore()
                    pageInsets.restore()
                    layoutChanged = composeHomeInsets.apply(bottomInset) || layoutChanged
                } else if (ComposeOpusHook.owns(decor)) {
                    // Opus protects its controls inside Compose. Padding its
                    // Android parent instead shrinks the entire painted surface.
                    liveWebInsets.restore()
                    pageInsets.restore()
                } else if (((main || preferences) || detail)) {
                    liveWebInsets.restore()
                    pageInsets.restore()
                    layoutChanged = (((if (main) mainInsets.apply(bottomInset) else (if (preferences) settingsInsets.apply(bottomInset) else detailInsets.apply(content!!, bottomInset)))) || layoutChanged)
                } else {
                    liveWebInsets.restore()
                    layoutChanged = (pageInsets.apply(content!!, bottomInset) || layoutChanged)
                }
                layoutChanged = (preserveNativeStatusInset() || layoutChanged)
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
            animatedImeBottom = null
            imeAnimations.clear()
            if (!applied) {
                return
            }
            var previous: Boolean = changing
            changing = true
            applied = false
            try {
                mainInsets.restore()
                composeHomeInsets.restore()
                settingsInsets.restore()
                detailInsets.restore()
                pageInsets.restore()
                liveWebInsets.restore()
                sheetInsets?.restore()
                quickConsumeInsets.restore()
                restoreTop()
                window.setNavigationBarColor(originalColor)
                if ((Build.VERSION.SDK_INT >= 28)) {
                    window.setNavigationBarDividerColor(originalDivider)
                }
                window.setFlags(originalFlags, windowFlagsMask())
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
