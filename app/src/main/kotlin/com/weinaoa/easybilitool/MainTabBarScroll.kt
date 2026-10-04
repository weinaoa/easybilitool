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
 * Directional progress for main pages without a collapsible home AppBar.
 */
class MainTabBarScroll {
    private var fraction: Float = 0F

    fun fraction(): Float {
        return fraction
    }

    fun scrolled(dy: Int, height: Int, atTop: Boolean): Float {
        if (((height <= 0) || atTop)) {
            reset()
            return fraction
        }
        fraction = JvmNumbers.max(0F, JvmNumbers.min(1F, (fraction + (dy / (height).toFloat()))))
        return fraction
    }

    fun reset() {
        fraction = 0F
    }

    companion object {
        @JvmStatic fun supports(page: String): Boolean {
            return ((((page.equals("关注") || page.equals("动态")) || page.equals("会员购")) || page.equals("我的")) || page.equals("我"))
        }
    }
}
