/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

object MainContentSafeAreaTest {
    @JvmStatic fun main(args: Array<String>) {
        var checks = 0
        fun verify(value: Boolean) { checks++; check(value) }
        verify(MainContentSafeArea.extra(2340, 2340, 55) == 55)
        verify(MainContentSafeArea.extra(2285, 2340, 55) == 0)
        verify(MainContentSafeArea.extra(2200, 2340, 55) == 0)
        verify(MainContentSafeArea.extra(2400, 2400, 120) == 120)
        verify(MainContentSafeArea.extra(2340, 2340, 0) == 0)
        verify(MainContentSafeArea.extra(2340, 2340, -1) == 0)
        // Finite page end stays in the same screen position after viewport expansion.
        val originalPadding = 173
        val state = DetailInsetState()
        repeat(10) {
            state.apply(originalPadding, -1, 1523, MainContentSafeArea.extra(2340, 2340, 55), false)
            verify(state.lastPadding == 228 && state.lastHeight == -1)
            state.restored()
        }
        state.apply(originalPadding, -1, 1468, MainContentSafeArea.extra(2285, 2340, 55), false)
        verify(state.lastPadding == originalPadding)
        println("Main content safe area tests passed: $checks")
    }
}
