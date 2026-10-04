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
import android.view.*

/**
 * The native preferences list can scroll through the inset, with a safe final row.
 */
class SettingsNavigationInsets {
    private lateinit var activity: Activity

    private var list: ViewGroup? = null

    private var applied: Boolean = false
    private var clip: Boolean = false

    private var padding: Int = 0
    private var lastPadding: Int = 0

    constructor(activity: Activity) {
        this.activity = activity
    }

    fun bind(content: View): Boolean {
        if (!activity.javaClass.getName().equals("com.bilibili.app.preferences.BiliPreferencesActivity")) {
            return false
        }
        var id: Int = activity.getResources().getIdentifier("recycler_view", "id", "tv.danmaku.bili")
        var next: View? = (if ((id == 0)) null else content.findViewById(id))
        val group = next as? ViewGroup
        if (!(group != null)) {
            return false
        }
        if ((list !== group)) {
            restore()
            list = group
        }
        return true
    }

    fun apply(inset: Int): Boolean {
        if (!applied) {
            padding = (list)!!.getPaddingBottom()
            clip = (list)!!.getClipToPadding()
            applied = true
        } else {
            if (((list)!!.getPaddingBottom() != lastPadding)) {
                padding = (list)!!.getPaddingBottom()
            }
        }
        lastPadding = (padding + inset)
        var changed: Boolean = ((list)!!.getPaddingBottom() != lastPadding)
        if (changed) {
            (list)!!.setPadding((list)!!.getPaddingLeft(), (list)!!.getPaddingTop(), (list)!!.getPaddingRight(), lastPadding)
        }
        if ((list)!!.getClipToPadding()) {
            (list)!!.setClipToPadding(false)
        }
        return changed
    }

    fun restore() {
        if (!applied) {
            return
        }
        applied = false
        if (((list)!!.getPaddingBottom() == lastPadding)) {
            (list)!!.setPadding((list)!!.getPaddingLeft(), (list)!!.getPaddingTop(), (list)!!.getPaddingRight(), padding)
        }
        if (!(list)!!.getClipToPadding()) {
            (list)!!.setClipToPadding(clip)
        }
    }
}
