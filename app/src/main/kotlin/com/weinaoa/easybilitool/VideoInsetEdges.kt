/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

/** Tappable insets can contain both status and navigation edges. */
internal data class VideoInsetEdges(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun without(status: Boolean, navigation: Boolean) = copy(
        top = if (status) 0 else top,
        bottom = if (navigation) 0 else bottom)
}
