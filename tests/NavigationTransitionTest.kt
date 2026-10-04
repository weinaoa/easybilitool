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

object NavigationTransitionTest {
    @JvmStatic fun main(args: Array<String>) {
        val outgoing = NavigationVisibility()
        val incoming = NavigationVisibility()
        check(!outgoing.visible && !incoming.visible)
        outgoing.started()
        check(outgoing.visible)
        // Pause precedes creation of the player, but does not hide the home window.
        incoming.started()
        check(outgoing.visible && incoming.visible)
        outgoing.stopped()
        check(!outgoing.visible && incoming.visible)
        outgoing.started()
        check(outgoing.visible && incoming.visible)
        incoming.stopped()
        check(outgoing.visible && !incoming.visible)
        outgoing.stopped()
        check(!outgoing.visible)
        outgoing.started()
        outgoing.started()
        check(outgoing.visible)
        // Restored layouts and host-reused inset layouts must produce the same height.
        var original = 183
        var lastWritten = 238
        repeat(4) {
            original = InsetBaseline.value(true, lastWritten, lastWritten, original)
            lastWritten = original + 55
            check(lastWritten == 238)
        }
        check(InsetBaseline.value(false, 238, 238, 183) == 238)
        check(InsetBaseline.value(true, 200, 238, 183) == 200)
        check(InsetBaseline.value(true, 183, 238, 183) == 183)
        check(InsetBaseline.value(true, 55, 55, 0) == 0)
        check(InsetBaseline.value(true, -2, 238, -2) == -2)
        check(InsetBaseline.value(true, 238, 238, 183) + 43 == 226)
        println("Navigation transition lifecycle and inset tests passed: 18")
    }
}
