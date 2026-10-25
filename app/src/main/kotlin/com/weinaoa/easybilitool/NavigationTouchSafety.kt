/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import java.util.WeakHashMap

/** Touch ownership follows the surface that paints navigation safety, not the whole window. */
internal object NavigationTouchSafety {
    private class Surface(var inset: Int) { val gesture = NavigationTouchSequence() }
    private val surfaces = WeakHashMap<ViewGroup, Surface>()
    private var installed = false

    fun install() {
        if (installed) return
        Reflector.findAndHookMethod(ViewGroup::class.java, "dispatchTouchEvent", MotionEvent::class.java,
            object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) {
                    val view = p.thisObject as ViewGroup
                    val surface = surfaces[view] ?: return
                    val event = p.args[0] as MotionEvent
                    val handled = surface.gesture.onEvent(event.actionMasked, event.x, event.y,
                        view.width, view.height, surface.inset, visible(view))
                    if (surface.inset == 0 && !surface.gesture.consuming) surfaces.remove(view)
                    if (handled) p.setResult(true)
                }
            })
        installed = true
    }

    private fun visible(view: View): Boolean {
        if (!view.isShown) return false
        var current: View? = view
        while (current != null) {
            if (current.alpha <= .001f) return false
            current = current.parent as? View
        }
        return true
    }

    fun protect(view: View?, inset: Int) {
        if (view !is ViewGroup) return
        if (inset <= 0) { restore(view); return }
        surfaces.getOrPut(view) { Surface(inset) }.inset = inset
    }

    fun restore(view: View?) {
        if (view !is ViewGroup) return
        val surface = surfaces[view] ?: return
        // Finish an already owned stream even if the surface disappears mid-gesture.
        surface.inset = 0
        if (!surface.gesture.consuming) surfaces.remove(view)
    }
}
