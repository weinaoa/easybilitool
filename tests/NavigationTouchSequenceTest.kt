/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

object NavigationTouchSequenceTest {
    @JvmStatic fun main(args: Array<String>) {
        val stream = NavigationTouchSequence()
        fun touch(action: Int, y: Float, shown: Boolean = true, x: Float = 400f,
                  inset: Int = 55, height: Int = 238) =
            stream.onEvent(action, x, y, 1080, height, inset, shown)
        check(touch(0, 183f))
        check(touch(2, 100f)) // Drag out of safety stays owned.
        check(touch(5, 100f))
        check(touch(6, 100f)) // Multi-touch pointer up is not the terminal up.
        check(touch(1, 100f, false, inset = 0)) // Restore/hide midway must finish ownership.
        check(!stream.consuming)
        check(!touch(2, 200f))
        check(!touch(0, 182.99f))
        check(!touch(2, 200f)) // Content scroll entering safety must remain intact.
        check(!touch(1, 200f))
        check(touch(0, 200f))
        check(touch(3, 200f))
        check(!stream.consuming)
        check(!touch(2, 200f))
        check(!touch(0, 200f, false)) // Fully collapsed/floating/IME/disabled surface.
        check(!touch(0, 200f, inset = 0))
        check(!touch(0, 200f, inset = -55))
        check(!touch(0, 238f))
        check(!touch(0, 200f, x = -1f))
        check(!touch(0, 200f, x = 1080f))
        check(!touch(0, Float.NaN))
        check(!touch(0, 200f, x = Float.POSITIVE_INFINITY))
        check(!touch(0, 0f, height = 0))
        check(touch(0, 20f, inset = 300, height = 60))
        check(!touch(0, 120f)) // Fresh content down replaces a lost/cancelled stream.
        val other = NavigationTouchSequence()
        check(touch(0, 200f))
        check(!other.onEvent(2, 400f, 200f, 1080, 238, 55, true))
        println("Navigation touch safety: bounds, stream ownership, content drags, multitouch, visibility and restore passed")
    }
}
