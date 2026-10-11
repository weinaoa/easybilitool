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

object BottomSheetInsetStateTest {
    @JvmStatic fun main(args: Array<String>) {
        val state = BottomSheetInsetState()
        var padding = state.apply(12, 55)
        check(padding == 67)
        repeat(20) { padding = state.apply(padding, 55); check(padding == 67) }
        check(state.restore(padding) == 12)
        padding = state.apply(12, 43)
        check(padding == 55)
        check(state.apply(padding, 0) == 12)
        check(state.apply(20, 55) == 75)
        check(state.restore(33) == 33) // A later host write remains owned by the host.
        check(BottomSheetInsetState().restore(18) == 18)
        println("Bottom sheet inset state: repeated layout, inset changes and host ownership passed")
    }
}
