/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

/** Only add the navigation space actually exposed by an edge-to-edge viewport. */
internal object MainContentSafeArea {
    fun extra(viewportBottom: Int, windowBottom: Int, navigationInset: Int): Int =
        (viewportBottom - (windowBottom - navigationInset.coerceAtLeast(0))).coerceAtLeast(0)
}
