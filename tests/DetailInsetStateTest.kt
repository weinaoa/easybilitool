/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

object DetailInsetStateTest {
    @JvmStatic fun main(args: Array<String>) {
        var checks = 0
        fun verify(value: Boolean) { checks++; check(value) }
        for (nativeHeight in listOf(-2, 121)) {
            val state = DetailInsetState()
            state.apply(0, nativeHeight, 121, 55, true)
            verify(state.lastHeight == 176 && state.lastPadding == 55)
            repeat(10) {
                state.restored()
                // onStart can run before the restored wrap-content view is measured again.
                state.apply(0, nativeHeight, 176, 55, true)
                verify(state.lastHeight == 176 && state.height == nativeHeight)
                // A host may also reuse the last parameters and padding.
                state.restored()
                state.apply(55, 176, 176, 55, true)
                verify(state.lastHeight == 176 && state.lastPadding == 55)
            }
            state.restored()
            state.apply(0, nativeHeight, 121, 43, true)
            verify(state.lastHeight == 164 && state.lastPadding == 43)
        }
        val changed = DetailInsetState()
        changed.apply(4, 121, 121, 55, true)
        changed.restored()
        changed.apply(8, 140, 140, 55, true)
        verify(changed.height == 140 && changed.padding == 8)
        verify(changed.lastHeight == 195 && changed.lastPadding == 63)
        changed.apply(8, 140, 140, 0, true)
        verify(changed.lastHeight == 140 && changed.lastPadding == 8)
        val list = DetailInsetState()
        list.apply(12, -1, 1900, 55, false)
        verify(list.lastHeight == -1 && list.lastPadding == 67)
        list.restored()
        list.apply(12, -1, 1900, 55, false)
        verify(list.lastHeight == -1 && list.lastPadding == 67)
        println("Detail inset state tests passed: $checks")
    }
}
