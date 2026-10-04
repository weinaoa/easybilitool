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

import android.app.Activity
import android.graphics.Rect
import android.view.*
import java.util.*

/**
 * Find the current fixed native detail toolbar, including a collapsed one.
 */
class DetailBarLocator {
    constructor() {

    }

    companion object {
        @JvmStatic fun findBar(activity: Activity, root: View): ViewGroup? {
            if (activity.javaClass.getName().equals("tv.danmaku.bili.MainActivityV2")) {
                return null
            }
            var activityName: String = activity.javaClass.getName()
            if ((activityName.equals("com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity") || activityName.equals("com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity"))) {
                var pagerId: Int = activity.getResources().getIdentifier("pager", "id", "tv.danmaku.bili")
                var pager: View? = (if ((pagerId == 0)) null else root.findViewById(pagerId))
                if ((pager != null)) {
                    try {
                        var current: Any? = (pager)!!.javaClass.getMethod("getCurrentItem").invoke(pager)
                        val page = current as? Number
                        if ((page != null && ((page)!!.toInt() != 1))) {
                            return null
                        }
                    } catch (unavailable: ReflectiveOperationException) {

                    }
                }
            }
            var ids: MutableSet<Int> = HashSet()
            for (name in arrayOf("cmt3_fake_input_bar_container", "opus_bottom_layout", "lay_action")) {

                var id: Int = activity.getResources().getIdentifier(name, "id", "tv.danmaku.bili")
                if ((id != 0)) {
                    ids.add(id)
                }
            }
            return visibleBar(root, root, ids, activity.getResources().getDisplayMetrics().density)
        }

        @JvmStatic fun visibleBar(view: View, root: View, ids: MutableSet<Int>, density: Float): ViewGroup? {
            if ((!view.isShown() || ((view.getAlpha() <= .01F) && !DetailBottomBarHook.managed(view)))) {
                return null
            }
            val group = view as? ViewGroup
            if (group != null) {
                if ((((ids.contains(view.getId()) && (view.getWidth() > (root.getWidth() * .65))) && (view.getHeight() >= (24 * density))) && (view.getHeight() < (180 * density)))) {
                    var at: IntArray = IntArray(2)
                    view.getLocationInWindow(at)
                    var visible: Rect = Rect()
                    var nativeTop: Float = (at[1] - DetailBottomBarHook.translation(view))
                    var collapsed: Boolean = (DetailBottomBarHook.collapseFraction(view) > 0)
                    if ((((nativeTop > (root.getHeight() * .65)) && ((nativeTop + view.getHeight()) <= (root.getHeight() + 1))) && ((collapsed || (view.getGlobalVisibleRect(visible) && (visible.height() >= (view.getHeight() * .8))))))) {
                        return group
                    }
                }
                run {
                    var i: Int = ((group)!!.getChildCount() - 1)
                    while ((i >= 0)) {
                        var found: ViewGroup? = visibleBar((group)!!.getChildAt(i), root, ids, density)
                        if ((found != null)) {
                            return found
                        }
                        i--
                    }
                }
            }
            return null
        }
    }
}
