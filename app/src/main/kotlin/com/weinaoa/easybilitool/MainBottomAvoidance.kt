/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.view.View
import android.view.ViewGroup
import java.util.IdentityHashMap

/** Undo only measured bottom avoidance on the main tab's own ancestor path. */
internal class MainBottomAvoidance {
    private class Baseline(val padding: Int, val margin: Int) {
        var lastPadding = padding
        var lastMargin = margin
    }
    private val baselines = IdentityHashMap<View, Baseline>()
    private val location = IntArray(2)

    fun apply(bar: View, content: View, windowBottom: Int, inset: Int): Boolean {
        // The scroll hook owns translation; a hidden/animating bar is not a gap.
        if (!bar.isShown || kotlin.math.abs(bar.translationY) > .5f) return false
        bar.getLocationInWindow(location)
        val spent = baselines.values.sumOf { maxOf(0, it.padding - it.lastPadding) + maxOf(0, it.margin - it.lastMargin) }
        val gap = MainBottomGeometry.consume(windowBottom - location[1] - bar.height, inset - spent)
        if (gap == 0) return false
        var current: View? = bar
        while (current != null) {
            val view = current
            if (kotlin.math.abs(view.translationY) > .5f) return false
            val params = view.layoutParams as? ViewGroup.MarginLayoutParams
            val baseline = baselines[view]
            // Native layout changes take ownership again; do not restore over them.
            if (baseline != null && (view.paddingBottom != baseline.lastPadding ||
                    (params != null && params.bottomMargin != baseline.lastMargin))) baselines.remove(view)
            val padding = if (view !== bar) MainBottomGeometry.consume(view.paddingBottom, gap) else 0
            val margin = MainBottomGeometry.consume(params?.bottomMargin ?: 0, gap - padding)
            if (padding + margin > 0) {
                val saved = baselines.getOrPut(view) { Baseline(view.paddingBottom, params?.bottomMargin ?: 0) }
                saved.lastPadding = view.paddingBottom - padding
                saved.lastMargin = (params?.bottomMargin ?: 0) - margin
                if (padding > 0) view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, saved.lastPadding)
                if (margin > 0 && params != null) {
                    params.bottomMargin = saved.lastMargin
                    view.layoutParams = params
                }
                // Re-measure before spending any more of this gap on another ancestor.
                return true
            }
            if (view === content) break
            current = view.parent as? View
        }
        return false
    }

    fun restore() {
        baselines.forEach { (view, saved) ->
            if (view.paddingBottom == saved.lastPadding)
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, saved.padding)
            val params = view.layoutParams as? ViewGroup.MarginLayoutParams
            if (params != null && params.bottomMargin == saved.lastMargin) {
                params.bottomMargin = saved.margin
                view.layoutParams = params
            }
        }
        baselines.clear()
    }
}
