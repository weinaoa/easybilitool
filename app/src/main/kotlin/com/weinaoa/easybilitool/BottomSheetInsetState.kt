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

internal class BottomSheetInsetState {
    var captured = false
        private set
    var original = 0
        private set
    var written = 0
        private set

    fun apply(current: Int, inset: Int): Int {
        original = InsetBaseline.value(captured, current, written, original)
        captured = true
        written = original + inset.coerceAtLeast(0)
        return written
    }

    fun restore(current: Int): Int = if (captured && current == written) original else current
}
