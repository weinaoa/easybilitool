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



class MainTabBarScrollTest {
    companion object {
        @JvmField var checks: Int = 0

        @JvmStatic fun check(ok: Boolean, message: String) {
            checks++
            if (!ok) {
                throw AssertionError(message)
            }
        }

        @JvmStatic fun main(args: Array<String>) {
            var s: MainTabBarScroll = MainTabBarScroll()
            for (page in arrayOf("关注", "动态", "会员购", "我的", "我")) {
                check(MainTabBarScroll.supports(page), page)
            }
            check((!MainTabBarScroll.supports("首页") && !MainTabBarScroll.supports("发布")), "home offset and publish stay separate")
            check((s.scrolled(50, 200, false) == .25F), "actual content scroll collapses proportionally")
            check((s.scrolled(500, 200, false) == 1F), "long scroll clamps fully hidden")
            check((s.scrolled(-50, 200, false) == .75F), "reverse starts showing immediately")
            check((s.scrolled(-500, 200, false) == 0F), "long reverse clamps fully visible")
            s.scrolled(80, 200, false)
            check((s.scrolled(1, 200, true) == 0F), "top and overscroll show bar")
            s.scrolled(80, 200, false)
            s.reset()
            check((s.fraction() == 0F), "page switch or disabling resets progress")
            check((s.scrolled(80, 0, false) == 0F), "unmeasured bar stays visible")
            System.out.println(("Main tab scroll tests passed: " + checks))
        }
    }
}
