/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

/** Retain native dimensions while the same detail bar is stopped and resumed. */
internal class DetailInsetState {
    var applied = false
        private set
    private var captured = false
    var padding = 0
        private set
    var height = 0
        private set
    private var measuredHeight = 0
    var lastPadding = 0
        private set
    var lastHeight = 0
        private set

    fun apply(currentPadding: Int, currentHeight: Int, currentMeasuredHeight: Int, inset: Int, fixed: Boolean) {
        if (!applied) {
            padding = InsetBaseline.value(captured, currentPadding, lastPadding, padding)
            height = InsetBaseline.value(captured, currentHeight, lastHeight, height)
            measuredHeight = InsetBaseline.value(captured, currentMeasuredHeight, lastHeight, measuredHeight)
            captured = true
        } else {
            if (currentPadding != lastPadding) padding = currentPadding
            if (currentHeight != lastHeight) height = currentHeight
        }
        lastPadding = padding + inset
        lastHeight = if (fixed) (if (height > 0) height else measuredHeight) + inset else height
        applied = true
    }

    fun restored() { applied = false }
}
