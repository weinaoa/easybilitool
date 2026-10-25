/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

/** Android-free stream policy; moves that started in content always stay with content. */
internal class NavigationTouchSequence {
    var consuming = false
        private set

    fun onEvent(action: Int, x: Float, y: Float, width: Int, height: Int,
                inset: Int, visible: Boolean): Boolean {
        // MotionEvent actionMasked: DOWN=0, UP=1, CANCEL=3.
        if (action == 0) {
            val safe = inset.coerceIn(0, height.coerceAtLeast(0))
            consuming = visible && safe > 0 && x.isFinite() && y.isFinite() &&
                x >= 0 && x < width && y >= height - safe && y < height
        }
        val handled = consuming
        if (action == 1 || action == 3) consuming = false
        return handled
    }
}
