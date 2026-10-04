/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

/** Keep the native baseline when a resumed host reuses our last inset value. */
object InsetBaseline {
    fun value(captured: Boolean, current: Int, lastWritten: Int, original: Int): Int =
        if (captured && current == lastWritten) original else current
}
