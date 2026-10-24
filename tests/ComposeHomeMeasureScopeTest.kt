/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

object ComposeHomeMeasureScopeTest {
    @JvmStatic fun main(args: Array<String>) {
        var checks = 0
        fun verify(value: Boolean) { checks++; check(value) }
        val outer = ComposeHomeMeasureScope(0, 20f)
        verify(outer.height(63f) == 83f)
        verify(outer.height(52f) == 52f) // Nested icons cannot consume the container inset.
        val skin = ComposeHomeMeasureScope(1, 20f)
        verify(skin.height(.5f) == .5f)
        verify(skin.height(52f) == 72f)
        verify(skin.height(52f) == 52f)
        verify(skin.takeRowInset() == 0f)
        skin.rowMeasured()
        verify(skin.takeRowInset() == 20f)
        verify(skin.takeRowInset() == 0f)
        skin.rowMeasured()
        verify(skin.takeRowInset() == 0f) // A nested row must not receive another inset.
        val plain = ComposeHomeMeasureScope(1, 20f)
        plain.rowMeasured()
        verify(plain.height(52f) == 52f) // No skin: a later icon's 52dp is unrelated.
        verify(plain.takeRowInset() == 20f)
        for (kind in 0..1) {
            val disabled = ComposeHomeMeasureScope(kind, 0f)
            verify(disabled.height(52f) == 52f)
            disabled.rowMeasured()
            verify(disabled.takeRowInset() == 0f)
        }
        // Phone's measured geometry: keep the content/control top fixed while
        // extending the skin and outer container by the actual 60px inset.
        val density = 3f
        val originalBottom = 2548f
        val windowBottom = 2608f
        val height = ComposeHomeMeasureScope(0, 60f / density).height(63f) * density
        verify(windowBottom - height == originalBottom - 63f * density)
        verify(windowBottom - 60f == originalBottom)
        for (inset in listOf(0f, 20f)) {
            for (fraction in listOf(0f, .25f, .5f, .75f, 1f)) {
                val outerMotion = ComposeHomeMeasureScope(0, inset, fraction)
                verify(outerMotion.takeMotionFraction() == fraction)
                verify(outerMotion.takeMotionFraction() == null)
                verify(outerMotion.height(63f) == 63f + inset)
                verify(outerMotion.height(24f) == 24f)
                val skinMotion = ComposeHomeMeasureScope(1, inset, fraction)
                verify(skinMotion.takeMotionFraction() == null)
                verify(skinMotion.height(52f) == 52f + inset)
            }
        }
        println("Compose home measure checks passed: $checks")
    }
}
