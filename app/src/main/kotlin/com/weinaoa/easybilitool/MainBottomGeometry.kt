/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

/** Bounded navigation avoidance: never consume more than the actual bottom gap. */
internal object MainBottomGeometry {
    fun consume(available: Int, gap: Int): Int = minOf(maxOf(0, available), maxOf(0, gap))
    fun backgroundHeight(dimension: Int, measured: Int, inset: Int): Int =
        if (dimension > 0) dimension + maxOf(0, inset)
        else if (measured > 0) measured + maxOf(0, inset)
        else dimension
}
