/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.graphics.Rect
import android.os.*
import android.view.*
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.WeakHashMap
import java.util.function.Supplier
import kotlin.math.roundToInt

/** The 9.16.1 comment page owns its input bar and scrolling inside Compose. */
internal class ComposeCommentHook(private val settings: Supplier<Config>) {
    private val states = WeakHashMap<Activity, State>()
    private var current = WeakReference<Activity>(null)
    private lateinit var createState: Method
    private lateinit var getValue: Method
    private lateinit var setValue: Method
    private lateinit var motion: ComposeBarMotion
    private lateinit var windowInsets: Method

    private inner class State(val activity: Activity) {
        val snapshot = createState.invoke(null, 0f, null, 2, null)!!
        val insetSnapshot = createState.invoke(null, 0f, null, 2, null)!!
        var compositionView: View? = null
        var insetKey: List<Any?>? = null
        var insetEpoch = 0f
        val scroll = DetailBarScroll()
        val lists = WeakHashMap<Any, Boolean>()
        var surface: View? = null
        var touchedUntil = 0L
        var downX = 0f
        var downY = 0f
        var vertical = false
        var viewport: View? = null
        var progress = 0f
        var barHeight = 0
        val handler = Handler(Looper.getMainLooper())
        val tick = object : Runnable {
            override fun run() {
                if (current.get() !== activity) return
                if (!eligible(activity) || !settings.get().b("HOME_HIDE_BOTTOM_BAR") || ime(activity)) reset()
                // IME exit can leave progress at zero. Its safe area must still
                // invalidate the composition, including a later viewport resize.
                val key = listOf(ime(activity), settings.get().b("NAVIGATION_EDGE_TO_EDGE"),
                    compositionView?.height, activity.window.decorView.height,
                    compositionView?.let { ComposeBarMotion.navigationOverlap(activity, it) })
                if (key != insetKey) {
                    insetKey = key
                    setValue.invoke(insetSnapshot, ++insetEpoch)
                }
                handler.postDelayed(this, 100)
            }
        }
        fun show(value: Float) {
            val next = value.coerceIn(0f, 1f)
            if (kotlin.math.abs(next - progress) < .0001f) return
            progress = next
            setValue.invoke(snapshot, next)
        }
        fun reset() { scroll.bind(null); surface = null; touchedUntil = 0; vertical = false; show(0f) }
    }

    private fun eligible(a: Activity) = !a.isFinishing && !a.isDestroyed &&
        !a.isInMultiWindowMode && !a.isInPictureInPictureMode &&
        a.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    private fun ime(a: Activity) = if (Build.VERSION.SDK_INT >= 30)
        a.window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
        else a.window.decorView.rootWindowInsets?.let { it.systemWindowInsetBottom > it.stableInsetBottom + 80 * a.resources.displayMetrics.density } == true

    fun install(application: Application): Boolean {
        val loader = application.classLoader
        // All reflected names/signatures are checked against the supplied 9.16.1 DEX.
        val input = try { Class.forName("kntr.common.comment.page.ui.b0", false, loader) }
            catch (_: ClassNotFoundException) { return false } // 8.95 keeps its native adapter.
        val modifier = Class.forName("androidx.compose.ui.Modifier", false, loader)
        val mutable = Class.forName("androidx.compose.runtime.MutableState", false, loader)
        val policy = Class.forName("androidx.compose.runtime.SnapshotMutationPolicy", false, loader)
        val stateKt = Class.forName("androidx.compose.runtime.SnapshotStateKt", false, loader)
        createState = stateKt.getMethod("mutableStateOf\$default", Any::class.java, policy, Int::class.javaPrimitiveType, Any::class.java)
        getValue = Class.forName("androidx.compose.runtime.State", false, loader).getMethod("getValue")
        setValue = mutable.getMethod("setValue", Any::class.java)
        motion = ComposeBarMotion(loader)
        windowInsets = Class.forName("androidx.compose.foundation.layout.WindowInsetsKt", false, loader)
            .getMethod("WindowInsets", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        val render = input.getDeclaredMethod("a", String::class.java, Class.forName("nh1.j", false, loader),
            Boolean::class.javaPrimitiveType, Class.forName("kotlin.jvm.functions.Function1", false, loader), modifier,
            Class.forName("kotlin.jvm.functions.Function0", false, loader), Boolean::class.javaPrimitiveType,
            Class.forName("androidx.compose.foundation.layout.WindowInsets", false, loader),
            Class.forName("androidx.compose.runtime.Composer", false, loader), Int::class.javaPrimitiveType)
        val localView = Class.forName("androidx.compose.ui.platform.AndroidCompositionLocals_androidKt", false, loader)
            .getMethod("getLocalView").invoke(null)
        val consume = Class.forName("androidx.compose.runtime.Composer", false, loader)
            .getMethod("consume", Class.forName("androidx.compose.runtime.CompositionLocal", false, loader))
        val lazy = Class.forName("androidx.compose.foundation.lazy.LazyListState", false, loader)
        val backwards = lazy.getMethod("getCanScrollBackward")
        val onScroll = lazy.getDeclaredMethod("onScroll\$foundation", Float::class.javaPrimitiveType)
        // Capture only states actually rendered by the verified comment-list lambda.
        // Other LazyLists in this Activity must not drive its comment input bar.
        val listRenderer = Class.forName("kntr.common.comment.page.ui.c0", false, loader)
        val listState = listRenderer.getDeclaredField("c").apply { isAccessible = true }
        check(listState.type == lazy)
        val renderList = listRenderer.getDeclaredMethod("invoke", Any::class.java, Any::class.java)
        val viewportInstaller = prepareViewport(loader)
        HookRuntime.hookMethod(renderList, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                val a = current.get() ?: return
                if (!eligible(a)) return
                val s = states[a] ?: return
                listState.get(p.thisObject)?.let { s.lists[it] = true }
            }
        })
        HookRuntime.hookMethod(render, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                val a = current.get() ?: return
                val s = states[a] ?: return
                val view = consume.invoke(p.args[8], localView) as? View ?: return
                if (view.rootView !== a.window.decorView) return
                s.compositionView = view
                getValue.invoke(s.insetSnapshot)
                if (a.javaClass.name == "kntr.common.compose.launcher.VertexActivity") s.surface = view
                // Reading the host snapshot during composition subscribes its parent
                // restart scope; changing it remeasures the actual list and input bar.
                val fraction = (getValue.invoke(s.snapshot) as Number).toFloat()
                if (!eligible(a)) return
                val keyboard = ime(a)
                val video = a.javaClass.name != "kntr.common.compose.launcher.VertexActivity"
                // Modern video editing uses a separate dialog. Do not remove
                // and reinsert this underlying bar's layout/inset modifiers.
                if (settings.get().b("NAVIGATION_EDGE_TO_EDGE") && (!keyboard || video)) {
                    // A parent may already reserve the gesture area. Protect only
                    // the part actually covered by this Compose viewport.
                    val bottom = ComposeBarMotion.navigationOverlap(a, view)
                    p.args[7] = windowInsets.invoke(null, 0, 0, 0, bottom)
                    p.args[9] = (p.args[9] as Int) and 12582912.inv() // Re-evaluate changed insets.
                }
                if (settings.get().b("HOME_HIDE_BOTTOM_BAR") && (!keyboard || video)) {
                    val keep = ComposeBarMotion.navigationOverlap(a, view)
                    p.args[4] = motion.apply(p.args[4], if (keyboard) 0f else fraction, keep) { s.barHeight = it }
                    p.args[9] = (p.args[9] as Int) and 24576.inv() // Re-evaluate changed modifier.
                }
            }
        })
        HookRuntime.hookMethod(onScroll, object : MethodHook() {
            override fun afterHookedMethod(p: MethodHookParam) {
                val a = current.get() ?: return
                val s = states[a] ?: return
                val now = SystemClock.uptimeMillis()
                if (!s.lists.containsKey(p.thisObject) || s.surface?.isShown != true || !s.vertical ||
                    now > s.touchedUntil || !eligible(a) || ime(a) || !settings.get().b("HOME_HIDE_BOTTOM_BAR")) return
                val consumed = (p.result as? Number)?.toFloat() ?: return
                val atTop = backwards.invoke(p.thisObject) == false
                if (consumed == 0f && !atTop) return
                s.touchedUntil = now + 1500
                s.scroll.scrolled((-consumed).roundToInt(), maxOf(s.barHeight, (46 * a.resources.displayMetrics.density).roundToInt()), atTop, now)
                s.show(s.scroll.fraction())
            }
        })
        Reflector.findAndHookMethod(Activity::class.java, "dispatchTouchEvent", MotionEvent::class.java, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                val a = p.thisObject as Activity
                val s = states[a] ?: return
                val event = p.args[0] as MotionEvent
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    val id = a.resources.getIdentifier("comment_container", "id", a.packageName)
                    val surface = (if (id == 0) null else a.window.decorView.findViewById<View>(id))
                        ?: s.surface?.takeIf { it.rootView === a.window.decorView && it.isAttachedToWindow }
                    val bounds = Rect()
                    if (surface == null || !surface.getGlobalVisibleRect(bounds) || !bounds.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        // A touch outside the comment surface can switch tabs or pages.
                        // Restore before the host detaches or reuses this list.
                        s.reset(); return
                    }
                    if (s.surface !== surface) { s.reset(); s.surface = surface }
                    // Compose may supply LocalView before the first touch. Bind even
                    // when it is already the same view so scroll tracking is armed.
                    s.scroll.bind(surface)
                    s.downX = event.rawX; s.downY = event.rawY; s.vertical = false
                    s.touchedUntil = SystemClock.uptimeMillis() + 1500
                    s.scroll.touch(SystemClock.uptimeMillis())
                } else if (event.actionMasked == MotionEvent.ACTION_MOVE && s.touchedUntil > 0) {
                    val dx = kotlin.math.abs(event.rawX - s.downX)
                    val dy = kotlin.math.abs(event.rawY - s.downY)
                    s.vertical = dy > 8 * a.resources.displayMetrics.density && dy > dx
                    s.touchedUntil = SystemClock.uptimeMillis() + 1500
                    s.scroll.touch(SystemClock.uptimeMillis())
                }
            }
        })
        viewportInstaller()
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, b: Bundle?) {
                if (a.javaClass.name in OWNERS) { states[a] = State(a); current = WeakReference(a) }
            }
            override fun onActivityResumed(a: Activity) {
                states[a]?.let { current = WeakReference(a); it.handler.removeCallbacks(it.tick); it.handler.post(it.tick) }
            }
            override fun onActivityPaused(a: Activity) { states[a]?.let { it.handler.removeCallbacks(it.tick); it.reset(); it.viewport?.requestLayout() }; if (current.get() === a) current.clear() }
            override fun onActivityDestroyed(a: Activity) { states.remove(a)?.let { it.handler.removeCallbacks(it.tick) } }
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
        })
        HookRuntime.log("[EasyBiliTool] COMPOSE_COMMENTS_READY 9.16.1")
        return true
    }

    private fun prepareViewport(loader: ClassLoader): () -> Unit {
        val type = Class.forName("com.bilibili.ship.theseus.united.page.comment.i", false, loader)
        val childField = Reflector.findField(type, "e")
        val topField = Reflector.findField(type, "g")
        val widthField = Reflector.findField(type, "h")
        val stableField = Reflector.findField(type, "i")
        val dirtyField = Reflector.findField(type, "j")
        val occlusion = Reflector.findField(type, "k")
        val clipType = Class.forName("com.bilibili.ship.theseus.united.page.comment.i\$a", false, loader)
        val childHeight = Reflector.findField(clipType, "b")
        val update = type.getDeclaredMethod("d")
        return { HookRuntime.hookMethod(update, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                val a = current.get() ?: return
                val s = states[a] ?: return
                val view = p.thisObject as ViewGroup
                s.viewport = view
                if (!settings.get().b("NAVIGATION_EDGE_TO_EDGE") || !eligible(a) || view.rootView !== a.window.decorView || view.width <= 0 || view.height <= 0) return
                val child = childField.get(view) as ViewGroup
                if (!clipType.isInstance(child)) return
                val xy = IntArray(2); view.getLocationInWindow(xy)
                val width = (view.width - view.paddingLeft - view.paddingRight).coerceAtLeast(0)
                val top = xy[1] + view.paddingTop
                val full = minOf(view.height - view.paddingTop - view.paddingBottom, a.window.decorView.height - top).coerceAtLeast(0)
                if (full == 0 || width == 0) return
                val remeasure = dirtyField.getBoolean(view) || child.isLayoutRequested || child.width != width || childHeight.getInt(child) != full || child.height != full
                topField.setInt(view, top); widthField.setInt(view, width); stableField.setInt(view, full); occlusion.set(view, null)
                childHeight.setInt(child, full)
                if (remeasure) {
                    child.forceLayout(); child.getChildAt(0)?.forceLayout()
                    child.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(full, View.MeasureSpec.EXACTLY))
                    child.layout(view.paddingLeft, view.paddingTop, view.paddingLeft + width, view.paddingTop + full)
                    dirtyField.setBoolean(view, false)
                }
                p.setResult(null)
            }
        }); Unit }
    }
    companion object {
        private val OWNERS = setOf("com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity",
            "com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity", "kntr.common.compose.launcher.VertexActivity")
    }
}
