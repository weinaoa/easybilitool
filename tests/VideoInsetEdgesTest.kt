/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

object VideoInsetEdgesTest {
    @JvmStatic fun main(args: Array<String>) {
        var checks = 0
        fun verify(value: Boolean) { checks++; check(value) }
        // Theseus takes the edge-wise max of status/cutout, navigation and
        // tappableElement. Clearing statusBars alone still leaves its 144px top.
        fun combine(vararg edges: VideoInsetEdges) = VideoInsetEdges(
            edges.maxOf { it.left }, edges.maxOf { it.top }, edges.maxOf { it.right }, edges.maxOf { it.bottom })
        val none = VideoInsetEdges(0, 0, 0, 0)
        val touch = VideoInsetEdges(0, 144, 0, 0)
        val nav = VideoInsetEdges(0, 0, 0, 60)
        verify(combine(none, none, touch).top == 144) // Previous candidate's black strip.
        verify(combine(none, none, touch.without(true, true)) == none)
        verify(combine(none, nav, touch.without(true, false)) == nav) // Status only.
        verify(combine(touch, none, touch.without(false, true)) == touch) // Navigation only.
        val asymmetric = VideoInsetEdges(7, 144, 9, 60)
        verify(asymmetric.without(true, false) == VideoInsetEdges(7, 0, 9, 60))
        verify(asymmetric.without(false, true) == VideoInsetEdges(7, 144, 9, 0))
        verify(asymmetric.without(true, true) == VideoInsetEdges(7, 0, 9, 0))
        verify(asymmetric.without(false, false) == asymmetric)
        val keyboard = VideoInsetEdges(0, 144, 0, 1000)
        verify(keyboard.without(true, false).bottom == 1000) // Keyboard bottom stays native.
        verify(touch == VideoInsetEdges(0, 144, 0, 0)) // Original raw geometry remains intact.
        println("Video native inset checks passed: $checks")
    }
}
