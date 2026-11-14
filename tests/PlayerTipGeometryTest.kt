/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

object PlayerTipGeometryTest {
    @JvmStatic fun main(args: Array<String>) {
        val window = PlayerTipGeometry.Bounds(0, 0, 1080, 2340)
        val half = PlayerTipGeometry.Bounds(0, 0, 1080, 600)
        val fitted = PlayerTipGeometry.Bounds(0, 90, 1080, 690)
        val centreHole = PlayerTipGeometry.Bounds(480, 0, 600, 90)
        val sideHole = PlayerTipGeometry.Bounds(0, 0, 177, 90)
        fun y(player: PlayerTipGeometry.Bounds, status: Int, holes: List<PlayerTipGeometry.Bounds>) =
            PlayerTipGeometry.place(player, window, status, holes, 450, 100, 22)!!.top
        check(y(half, 90, listOf(sideHole)) == 112)
        check(y(fitted, 90, listOf(sideHole)) == 112) // no double status inset
        check(y(window, 0, listOf(sideHole)) == 22) // hidden bars, off-centre camera
        check(y(window, 0, listOf(centreHole)) == 112)
        check(y(window, 90, listOf(centreHole)) == 112) // never add both reserves
        val native = PlayerTipGeometry.place(half, window, 90, listOf(sideHole), 450, 104, 22)!!
        val alternate = PlayerTipGeometry.place(half, window, 90, listOf(sideHole), 390, 96, 22)!!
        check(native.top == alternate.top && native.left+225 == alternate.left+195)
        val landscape = PlayerTipGeometry.Bounds(0, 0, 2340, 1080)
        check(PlayerTipGeometry.place(landscape, landscape, 0,
            listOf(PlayerTipGeometry.Bounds(0, 0, 90, 177)), 450, 100, 22)!!.top == 22)
        val offset = PlayerTipGeometry.Bounds(100, 200, 1180, 1200)
        check(PlayerTipGeometry.place(offset, offset, 290, emptyList(), 450, 100, 22) == PlayerTipGeometry.Position(415, 312))
        check(PlayerTipGeometry.place(half, window, 90, emptyList(), 1081, 100, 22) == null)
        check(PlayerTipGeometry.place(PlayerTipGeometry.Bounds(0, 0, 100, 80), window, 90, emptyList(), 50, 60, 22) == null)
        println("Player tip geometry: fitted/immersive equivalence, status visibility, centred/side cameras, full/half screens, different bubble widths and window offsets passed")
    }
}
