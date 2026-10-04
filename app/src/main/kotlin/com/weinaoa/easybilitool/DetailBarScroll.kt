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



/**
 * Ignore layout/restored positions; follow only a touched detail list and its fling.
 */
class DetailBarScroll {
    private val progress: MainTabBarScroll = MainTabBarScroll()

    private var surface: Any? = null

    private var scrollUntil: Long = 0L

    fun bind(next: Any?) {
        if ((surface !== next)) {
            surface = next
            reset()
        }
    }

    fun touch(now: Long) {
        if ((surface != null)) {
            scrollUntil = (now + 1500)
        }
    }

    fun fraction(): Float {
        return progress.fraction()
    }

    fun scrolled(dy: Int, height: Int, atTop: Boolean, now: Long): Float {
        if (((((surface == null) || (scrollUntil == 0L)) || (now > scrollUntil)) || (dy == 0))) {
            return fraction()
        }
        scrollUntil = (now + 1500)
        return progress.scrolled(dy, height, atTop)
    }

    fun reset() {
        scrollUntil = 0
        progress.reset()
    }
}
