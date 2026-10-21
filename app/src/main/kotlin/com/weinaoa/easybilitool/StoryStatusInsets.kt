/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.view.WindowManager
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.Collections
import java.util.IdentityHashMap
import java.util.WeakHashMap
import java.util.function.Supplier

/** 8.95 Story renderer keeps a status inset separate from the window insets. */
internal class StoryStatusInsets(private val settings: Supplier<Config>) {
    private class Request(activity: Activity, val values: IntArray, val top: Int) {
        val activity = WeakReference(activity)
        var handled = false
    }

    private val requests = Collections.synchronizedMap(WeakHashMap<Any, Request>())
    private var viewportMethod: Method? = null

    private fun activity(context: Context): Activity? {
        var current = context
        val visited = Collections.newSetFromMap(IdentityHashMap<Context, Boolean>())
        while (visited.add(current)) {
            if (current is Activity) return current
            current = (current as? ContextWrapper)?.baseContext ?: return null
        }
        return null
    }

    private fun eligible(a: Activity): Boolean = settings.get().b("IMMERSIVE_STATUS_BAR") &&
        a.javaClass.name == "com.bilibili.video.story.StoryVideoActivity" &&
        !a.isFinishing && !a.isDestroyed && !a.isInPictureInPictureMode && !a.isInMultiWindowMode &&
        a.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT &&
        a.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN == 0

    fun install(application: Application) {
        runCatching {
            val player = Class.forName("com.bilibili.video.story.player.StoryPlayer", false, application.classLoader)
            val int = Int::class.javaPrimitiveType!!
            val contextField = Reflector.findField(player, "H")
            viewportMethod = player.getDeclaredMethod("p3", int, int, int, int, int).apply { isAccessible = true }
            Reflector.findAndHookMethod(player, "p3", int, int, int, int, int, object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) {
                    val context = contextField.get(p.thisObject) as? Context ?: return
                    val a = activity(context) ?: return
                    val dimen = a.resources.getIdentifier("status_bar_height", "dimen", "android")
                    val top = if (dimen == 0) 0 else a.resources.getDimensionPixelSize(dimen)
                    val request = Request(a, IntArray(5) { p.args[it] as Int }, top)
                    requests[p.thisObject!!] = request
                    // p3(offsetY, left, top, right, bottom): change only a top inset
                    // equal to the status bar, leaving toolbar spacing and crop intact.
                    if (eligible(a) && top > 0 && request.values[2] == top) {
                        p.args[2] = 0
                        request.handled = true
                    }
                }
            })
        }.onFailure { HookRuntime.log("[EasyBiliTool] STORY_STATUS_BYPASS ${it.javaClass.simpleName}") }
    }

    fun applyPending(a: Activity) {
        if (!eligible(a)) return
        // Player startup may precede settings or the end of the fullscreen launch.
        // Replay the retained native request once the activity can be immersive.
        val pending = synchronized(requests) { requests.entries.map { it.key to it.value } }
        for ((player, request) in pending) {
            if (request.activity.get() !== a || request.handled || request.top <= 0 ||
                request.values[2] != request.top) continue
            request.handled = true
            runCatching { viewportMethod?.invoke(player, *Array<Any?>(5) { request.values[it] }) }
                .onFailure { HookRuntime.log("[EasyBiliTool] STORY_INSET_BYPASS ${it.javaClass.simpleName}") }
        }
    }
}
