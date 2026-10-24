/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

object MainBottomGeometryTest {
    @JvmStatic fun main(args: Array<String>) {
        check(MainBottomGeometry.backgroundHeight(156, 156, 60) == 216)
        check(MainBottomGeometry.backgroundHeight(-2, 156, 60) == 216)
        check(MainBottomGeometry.backgroundHeight(-1, 156, 60) == 216)
        check(MainBottomGeometry.backgroundHeight(-2, 0, 60) == -2)
        check(MainBottomGeometry.backgroundHeight(156, 156, 0) == 156)
        check(MainBottomGeometry.consume(60, 60) == 60)
        check(MainBottomGeometry.consume(80, 60) == 60)
        check(MainBottomGeometry.consume(12, 60) == 12)
        check(MainBottomGeometry.consume(60, 0) == 0)
        check(MainBottomGeometry.consume(-10, 60) == 0)
        check(MainBottomGeometry.consume(60, -20) == 0)
        println("Main bottom geometry checks passed: 11")
    }
}
