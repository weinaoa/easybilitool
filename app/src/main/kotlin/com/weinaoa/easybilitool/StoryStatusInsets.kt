/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.view.WindowManager
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.IdentityHashMap
import java.util.WeakHashMap
import java.util.function.Supplier

/** Story renderer keeps a status inset separate from the window insets. */
internal class StoryStatusInsets(private val settings: Supplier<Config>) {
    private class Request(activity: Activity, val values: IntArray, val top: Int, val binding: StoryPlayerCompat.Binding) {
        val activity = WeakReference(activity)
        var handled = false
    }

    private val requests = Collections.synchronizedMap(WeakHashMap<Any, Request>())

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
            val binding = StoryPlayerCompat.resolve(player, Context::class.java)
            HookRuntime.hookMethod(binding.viewport, object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) {
                    val context = binding.context.get(p.thisObject) as? Context ?: return
                    val a = activity(context) ?: return
                    val dimen = a.resources.getIdentifier("status_bar_height", "dimen", "android")
                    val top = if (dimen == 0) 0 else a.resources.getDimensionPixelSize(dimen)
                    val request = Request(a, IntArray(binding.viewport.parameterCount) { p.args[it] as Int }, top, binding)
                    requests[p.thisObject!!] = request
                    // Remove only the status inset; preserve the host's crop and offset.
                    if (eligible(a) && binding.shouldRemoveTop(request.values, top)) {
                        p.args[binding.topIndex] = 0
                        request.handled = true
                    }
                }
            })
            HookRuntime.log("[EasyBiliTool] STORY_STATUS_READY ${binding.viewport.name}/${binding.viewport.parameterCount} context=${binding.context.name}")
        }.onFailure { HookRuntime.log("[EasyBiliTool] STORY_STATUS_BYPASS ${it.javaClass.simpleName}") }
    }

    fun applyPending(a: Activity) {
        if (!eligible(a)) return
        // Player startup may precede settings or the end of the fullscreen launch.
        // Replay the retained native request once the activity can be immersive.
        val pending = synchronized(requests) { requests.entries.map { it.key to it.value } }
        for ((player, request) in pending) {
            if (request.activity.get() !== a || request.handled || !request.binding.shouldRemoveTop(request.values, request.top)) continue
            request.handled = true
            runCatching { request.binding.viewport.invoke(player, *Array<Any?>(request.values.size) { request.values[it] }) }
                .onFailure { HookRuntime.log("[EasyBiliTool] STORY_INSET_BYPASS ${it.javaClass.simpleName}") }
        }
    }
}
