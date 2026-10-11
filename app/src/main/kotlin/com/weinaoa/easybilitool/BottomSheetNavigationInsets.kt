/*
Copyright (c) 2026 weinaoa
EasyBiliTool is licensed under Mulan PubL v2.
You can use this software according to the terms and conditions of the Mulan PubL v2.
You may obtain a copy of Mulan PubL v2 at:
    http://license.coscl.org.cn/MulanPubL-2.0
THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
See the Mulan PubL v2 for more details.
*/

package com.weinaoa.easybilitool

import android.view.ViewGroup

/** Extend the sheet surface, retaining the controls' original distance from navigation. */
internal class BottomSheetNavigationInsets(private val sheet: ViewGroup) {
    private val padding = BottomSheetInsetState()
    private val originalClip = sheet.clipToPadding
    private val surface = NavigationInsetSurface(sheet)

    fun apply(inset: Int): Boolean {
        surface.apply(sheet.paddingBottom)
        val target = padding.apply(sheet.paddingBottom, inset)
        val changed = sheet.paddingBottom != target || sheet.clipToPadding
        if (sheet.paddingBottom != target) {
            sheet.setPadding(sheet.paddingLeft, sheet.paddingTop, sheet.paddingRight, target)
        }
        sheet.clipToPadding = false
        surface.apply(target)
        return changed
    }

    fun surfaceColor(): Int? = surface.surfaceColor()

    fun restore() {
        surface.restore()
        val target = padding.restore(sheet.paddingBottom)
        if (sheet.paddingBottom != target) {
            sheet.setPadding(sheet.paddingLeft, sheet.paddingTop, sheet.paddingRight, target)
        }
        sheet.clipToPadding = originalClip
    }

}
