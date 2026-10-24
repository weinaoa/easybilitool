/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

/** Per-invocation guard: shared Compose helpers must not resize tab icons. */
internal class ComposeHomeMeasureScope(val kind: Int, val inset: Float, val fraction: Float = 0f) {
    private var rowPending = false
    private var rowSeen = false
    private var heightConsumed = false
    private var motionConsumed = false
    fun takeMotionFraction(): Float? {
        if (kind != 0 || motionConsumed) return null
        motionConsumed = true
        return fraction
    }
    fun height(value: Float): Float {
        if (heightConsumed) return value
        if (inset <= 0f) return value
        if (kind == 0 || (kind == 1 && !rowSeen && kotlin.math.abs(value - 52f) < .01f)) {
            heightConsumed = true
            return value + inset
        }
        return value
    }
    fun rowMeasured() {
        if (kind == 1 && !rowSeen) { rowPending = true; rowSeen = true }
    }
    fun takeRowInset(): Float {
        if (!rowPending) return 0f
        rowPending = false
        return maxOf(0f, inset)
    }
}
