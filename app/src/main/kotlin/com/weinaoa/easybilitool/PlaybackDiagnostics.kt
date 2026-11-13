/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import java.util.WeakHashMap

/** Observe playback without replacing the host's touch, surface or window callbacks. */
internal object PlaybackDiagnostics {
    private val states = WeakHashMap<Activity, State>()
    private val windows = WeakHashMap<Window, State>()
    private val owners = setOf("com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity",
        "com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity", "com.bilibili.video.story.StoryVideoActivity")

    fun allowFrame(activity: Activity, changed: Boolean): Boolean {
        if(!changed) return true
        if(activity.javaClass.name !in owners) return false
        states[activity]?.layoutUpdate()
        return true
    }

    fun install(app: Application) {
        if(Build.VERSION.SDK_INT >= 28 && Application.getProcessName() != app.packageName) return
        Reflector.findAndHookMethod(Activity::class.java, "dispatchTouchEvent", MotionEvent::class.java, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                states[p.thisObject as Activity]?.touch(p.args[0] as MotionEvent)
            }
        })
        Reflector.findAndHookMethod(Window::class.java, "setAttributes", WindowManager.LayoutParams::class.java, object : MethodHook() {
            override fun beforeHookedMethod(p: MethodHookParam) {
                windows[p.thisObject as Window]?.attributes(p.args[0] as WindowManager.LayoutParams)
            }
        })
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, b: Bundle?) {
                if(a.javaClass.name in owners) {
                    val state = State(a); states[a] = state; windows[a.window] = state
                    a.window.decorView.viewTreeObserver.addOnGlobalLayoutListener(state.layout)
                    DiagnosticLogs.record("ACTIVITY", "created ${a.javaClass.name}")
                }
            }
            override fun onActivityResumed(a: Activity) { states[a]?.let { DiagnosticLogs.record("ACTIVITY", "resumed ${a.javaClass.name}"); it.geometry() } }
            override fun onActivityPaused(a: Activity) { states[a]?.let { DiagnosticLogs.record("ACTIVITY", "paused ${a.javaClass.name} holding=${it.holding}"); it.holding = false } }
            override fun onActivityDestroyed(a: Activity) {
                states.remove(a)?.close(); windows.remove(a.window); DiagnosticLogs.destroyed(a)
            }
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
        })
    }

    private class State(val activity: Activity) {
        var holding = false
        private var attributes = ""
        private var geometry = ""
        private var lastLayout = 0L
        private var lastMove = 0L
        private var writes = 0
        private var lastWrite = 0L
        private var lastDrawUpdate = 0L
        private val surfaces = WeakHashMap<SurfaceHolder, SurfaceHolder.Callback>()
        val layout = android.view.ViewTreeObserver.OnGlobalLayoutListener { geometry() }

        fun touch(event: MotionEvent) {
            val action = event.actionMasked
            if(action == MotionEvent.ACTION_DOWN) holding = true
            if(action == MotionEvent.ACTION_MOVE && (!holding || event.eventTime - lastMove < 2000)) return
            if(action == MotionEvent.ACTION_MOVE) lastMove = event.eventTime
            DiagnosticLogs.record("PLAYER_TOUCH", "action=${MotionEvent.actionToString(action)} elapsed=${event.eventTime-event.downTime} pointers=${event.pointerCount}")
            if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) holding = false
        }

        fun attributes(p: WindowManager.LayoutParams) {
            writes++
            val value = "flags=${p.flags} soft=${p.softInputMode} size=${p.width}x${p.height} " +
                if(Build.VERSION.SDK_INT >= 30) "fit=${p.fitInsetsTypes} cutout=${p.layoutInDisplayCutoutMode}" else ""
            val now = SystemClock.uptimeMillis()
            if(value != attributes || now-lastWrite >= 2000) {
                val caller = Throwable().stackTrace.firstOrNull { !it.className.contains("PlaybackDiagnostics") &&
                    (it.className.startsWith("com.bilibili.") || it.className.startsWith(BuildConfig.APPLICATION_ID)) }
                DiagnosticLogs.record("PLAYER_WINDOW", "$value writes=$writes holding=$holding source=${caller?.className}.${caller?.methodName}")
                attributes = value; writes = 0; lastWrite = now
            }
        }

        fun layoutUpdate() {
            val now = SystemClock.uptimeMillis()
            if(now-lastDrawUpdate >= 1000) {
                lastDrawUpdate = now
                DiagnosticLogs.record("PLAYER_DRAW", "layout adjusted; current frame retained holding=$holding")
            }
        }

        fun geometry() {
            val now = SystemClock.uptimeMillis()
            if(now-lastLayout < 500) return
            lastLayout = now
            val decor = activity.window.decorView
            val value = "decor=${decor.width}x${decor.height} ui=${decor.systemUiVisibility} orientation=${activity.resources.configuration.orientation} focused=${activity.hasWindowFocus()}"
            if(value != geometry) { geometry = value; DiagnosticLogs.record("PLAYER_LAYOUT", value) }
            var visited = 0
            fun visit(view: View, depth: Int) {
                if(depth > 12 || ++visited > 350) return
                if(view is SurfaceView && !surfaces.containsKey(view.holder)) {
                    val callback = object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) { DiagnosticLogs.record("PLAYER_SURFACE", "created valid=${holder.surface.isValid}") }
                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { DiagnosticLogs.record("PLAYER_SURFACE", "changed ${width}x$height format=$format holding=$holding") }
                        override fun surfaceDestroyed(holder: SurfaceHolder) { DiagnosticLogs.record("PLAYER_SURFACE", "destroyed holding=$holding") }
                    }
                    surfaces[view.holder] = callback; view.holder.addCallback(callback)
                    DiagnosticLogs.record("PLAYER_SURFACE", "attached ${view.width}x${view.height} valid=${view.holder.surface.isValid}")
                }
                if(view is ViewGroup) for(i in 0 until view.childCount) visit(view.getChildAt(i), depth+1)
            }
            visit(decor, 0)
        }

        fun close() {
            val tree = activity.window.decorView.viewTreeObserver
            if(tree.isAlive) tree.removeOnGlobalLayoutListener(layout)
            surfaces.forEach { (holder, callback) -> holder.removeCallback(callback) }; surfaces.clear()
        }
    }
}
