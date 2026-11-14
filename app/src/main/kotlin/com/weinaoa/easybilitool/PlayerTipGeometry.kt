/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

/** All inputs use the same screen coordinate system; an already inset player
 * must not receive the status height a second time. */
internal object PlayerTipGeometry {
    data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int)
    data class Position(val left: Int, val top: Int)

    fun place(player: Bounds, window: Bounds, statusBottom: Int, cutouts: List<Bounds>,
        width: Int, height: Int, gap: Int): Position? {
        val left = maxOf(player.left, window.left)
        val top = maxOf(player.top, window.top)
        val right = minOf(player.right, window.right)
        val bottom = minOf(player.bottom, window.bottom)
        if (width <= 0 || height <= 0 || width > right-left || height > bottom-top) return null
        val x = left + (right-left-width)/2
        var y = maxOf(top, statusBottom) + gap
        // A side camera in landscape must not push a centred toast down. A
        // centred hole/notch is avoided even when the status bar is hidden.
        for (hole in cutouts.sortedBy { it.top }) {
            if (x < hole.right && x+width > hole.left && y < hole.bottom && y+height > hole.top)
                y = hole.bottom + gap
        }
        if (y+height > bottom-gap) return null
        return Position(x, y)
    }
}
