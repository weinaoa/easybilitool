/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import java.util.function.Supplier

/** Adapt the native Theseus padding service, preserving its raw public inset flow. */
internal class TheseusWindowInsetsHook(private val settings: Supplier<Config>) {
    fun install(application: Application): Boolean {
        val loader = application.classLoader
        fun type(name: String) = Class.forName(name, false, loader)
        val service = try { type("com.bilibili.ship.theseus.united.page.activitywindow.ActivityWindowEdgeToEdgePropertyService") }
            catch (_: ClassNotFoundException) { return false }
        val compat = type("androidx.core.view.WindowInsetsCompat")
        val builder = type("androidx.core.view.WindowInsetsCompat\$Builder")
        val types = type("androidx.core.view.WindowInsetsCompat\$Type")
        val insets = type("androidx.core.graphics.Insets")
        val none = insets.getField("NONE").get(null)
        val create = builder.getConstructor(compat)
        val setInsets = builder.getMethod("setInsets", Int::class.javaPrimitiveType, insets)
        val setStable = builder.getMethod("setInsetsIgnoringVisibility", Int::class.javaPrimitiveType, insets)
        val cutout = builder.getMethod("setDisplayCutout", type("androidx.core.view.DisplayCutoutCompat"))
        val build = builder.getMethod("build")
        val getInsets = compat.getMethod("getInsets", Int::class.javaPrimitiveType)
        val getStable = compat.getMethod("getInsetsIgnoringVisibility", Int::class.javaPrimitiveType)
        val of = insets.getMethod("of", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        val left = insets.getField("left"); val topField = insets.getField("top")
        val right = insets.getField("right"); val bottomField = insets.getField("bottom")
        val isVisible = compat.getMethod("isVisible", Int::class.javaPrimitiveType)
        fun mask(name: String) = types.getMethod(name).invoke(null) as Int
        val status = mask("statusBars") or mask("displayCutout")
        val navigation = mask("navigationBars")
        val ime = mask("ime")
        val tappable = mask("tappableElement")
        val owner = service.getDeclaredField("d").apply { isAccessible = true }
        val applyInsets = service.getDeclaredMethod("b", compat, Boolean::class.javaPrimitiveType)
        HookRuntime.hookMethod(applyInsets, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                val activity = owner.get(p.thisObject) as? Activity ?: return
                if (activity.javaClass.name !in OWNERS || p.args[0] == null || p.args[1] == true) return
                val eligible = !activity.isFinishing && !activity.isDestroyed &&
                    !activity.isInMultiWindowMode && !activity.isInPictureInPictureMode &&
                    activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                if (!eligible) return
                val config = settings.get()
                val top = config.b("IMMERSIVE_STATUS_BAR") &&
                    activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN == 0
                val keyboard = isVisible.invoke(p.args[0], ime) == true
                // b() controls only native root padding; c() still publishes all
                // raw IME insets. Keep this root's navigation fit stable while
                // the separate editor window animates in and out.
                val bottom = config.b("NAVIGATION_EDGE_TO_EDGE") &&
                    activity.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0
                val managed = (if (top) status else 0) or (if (bottom) navigation else 0)
                if (managed == 0) return
                val copy = create.newInstance(p.args[0])
                setInsets.invoke(copy, managed, none)
                setStable.invoke(copy, managed, none)
                if (top) cutout.invoke(copy, null)
                // The native navigation helper unions navigationBars with
                // tappableElement, whose TOP also contains the status area.
                // Clear only the managed vertical edges of that second source.
                fun adjusted(value: Any): Any {
                    val edges = VideoInsetEdges(left.getInt(value), topField.getInt(value),
                        right.getInt(value), bottomField.getInt(value)).without(top, bottom)
                    return of.invoke(null, edges.left, edges.top, edges.right, edges.bottom)
                }
                setInsets.invoke(copy, tappable, adjusted(getInsets.invoke(p.args[0], tappable)))
                setStable.invoke(copy, tappable, adjusted(getStable.invoke(p.args[0], tappable)))
                // Only b() sees the normalized copy. c() still publishes the
                // original insets for keyboard, controls and AndroidX protection.
                p.args[0] = build.invoke(copy)
                RuntimeStatus.record("VIDEO_NATIVE_INSETS top=$top bottom=$bottom ime=$keyboard")
            }
        })
        return true
    }

    companion object {
        private val OWNERS = setOf("com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity",
            "com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity")
    }
}
