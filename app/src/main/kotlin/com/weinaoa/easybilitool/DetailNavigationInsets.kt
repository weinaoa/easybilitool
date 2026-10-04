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
import java.util.*
import android.graphics.drawable.Drawable

/**
 * Keep native detail/live surfaces full height; inset only lists or fixed controls.
 */
class DetailNavigationInsets {
    private lateinit var activity: Activity

    private var live: Boolean = false
    private var video: Boolean = false

    private var controlsId: Int = 0
    private var commentsId: Int = 0
    private var introId: Int = 0
    private var interactionId: Int = 0
    private var bannersId: Int = 0

    private val adjustments: MutableMap<View, Adjustment> = IdentityHashMap()

    constructor(activity: Activity) {
        this.activity = activity
        var name: String = activity.javaClass.getName()
        live = name.equals("com.bilibili.bililive.room.ui.roomv3.LiveRoomActivityV3")
        video = ((name.equals("com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity") || name.equals("com.bilibili.ship.theseus.playlist.UnitedPlaylistActivity")) || name.equals("com.bilibili.video.videodetail.VideoDetailsActivity"))
        controlsId = id("bottom_controllers_group")
        commentsId = id("cmt3_fake_input_bar_container")
        introId = id("recycler")
        interactionId = id("interaction_container")
        bannersId = id("live_operation_banner_container")
    }

    private fun id(name: String): Int {
        return activity.getResources().getIdentifier(name, "id", "tv.danmaku.bili")
    }

    fun bind(content: View): Boolean {
        return ((live || video) || detailSurface(content))
    }

    fun detailSurface(content: View?): Boolean {
        return ((content != null) && (DetailBarLocator.findBar(activity, content) != null))
    }

    fun darkSurface(): Boolean {
        return live
    }

    fun videoSurface(): Boolean {
        return video
    }

    fun apply(content: View, inset: Int): Boolean {
        var present: MutableSet<View> = Collections.newSetFromMap(IdentityHashMap())
        var changed: Boolean = false
        if (live) {
            changed = adjust(content, controlsId, true, inset, present)
            changed = (adjustSafeArea(content, interactionId, inset, present) || changed)
            changed = (adjustSafeArea(content, bannersId, inset, present) || changed)
        }
        if (video) {
            changed = (adjust(content, commentsId, true, inset, present) || changed)
            changed = (adjust(content, introId, false, inset, present) || changed)
        }
        if ((!video && !live)) {
            var bar: ViewGroup? = DetailBarLocator.findBar(activity, content)
            if ((bar != null)) {
                present.add(bar)
                changed = (adjustments.computeIfAbsent(bar, lambda@ { v -> Adjustment(bar, true, false) }).apply(inset) || changed)
            }
        }
        run {
            var it: MutableIterator<MutableMap.MutableEntry<View, Adjustment>> = adjustments.entries.iterator()
            while (it.hasNext()) {
                var entry = it.next()
                if (!present.contains(entry.key)) {
                    changed = (entry.value.restore() || changed)
                    it.remove()
                }

            }
        }
        return changed
    }

    private fun adjust(content: View, id: Int, fixed: Boolean, inset: Int, present: MutableSet<View>): Boolean {
        var view: View? = (if ((id == 0)) null else (content)!!.findViewById(id))
        if (((!(view is ViewGroup) || ((view)!!.getLayoutParams() == null)) || !(view)!!.isLaidOut())) {
            return false
        }
        if ((!fixed && !isRecycler(view))) {
            return false
        }
        present.add(view)
        var adjustment: Adjustment = adjustments.computeIfAbsent(view, lambda@ { v -> Adjustment((v as ViewGroup), fixed, !fixed) })
        return adjustment.apply(inset)
    }

    private fun adjustSafeArea(content: View, id: Int, inset: Int, present: MutableSet<View>): Boolean {
        var view: View? = (if ((id == 0)) null else (content)!!.findViewById(id))
        if (((!(view is ViewGroup) || ((view)!!.getLayoutParams() == null)) || !(view)!!.isLaidOut())) {
            return false
        }
        present.add(view)
        return adjustments.computeIfAbsent(view, lambda@ { v -> Adjustment((v as ViewGroup), false, false) }).apply(inset)
    }

    private fun isRecycler(view: View): Boolean {
        run {
            var type: Class<*>? = (view)!!.javaClass
            while ((type != null)) {
                if (type.getName().equals("androidx.recyclerview.widget.RecyclerView")) {
                    return true
                }
                type = type.getSuperclass()
            }
        }
        return false
    }

    fun restore() {
        for (adjustment in adjustments.values) {

            adjustment.restore()
        }
        adjustments.clear()
    }

    private class Adjustment {
        lateinit var view: ViewGroup

        @JvmField var fixed: Boolean = false
        @JvmField var scroll: Boolean = false

        @JvmField var clip: Boolean = false

        @JvmField var padding: Int = 0
        @JvmField var height: Int = 0
        @JvmField var measuredHeight: Int = 0
        @JvmField var lastPadding: Int = 0
        @JvmField var lastHeight: Int = 0

        @JvmField var applied: Boolean = false

        @JvmField var originalBackground: Drawable? = null
        @JvmField var extendedBackground: Drawable? = null

        constructor(view: ViewGroup, fixed: Boolean, scroll: Boolean) {
            this.view = view
            this.fixed = fixed
            this.scroll = scroll
            clip = (view)!!.getClipToPadding()
        }

        fun apply(inset: Int): Boolean {
            var params: ViewGroup.LayoutParams = (view)!!.getLayoutParams()
            if (!applied) {
                padding = (view)!!.getPaddingBottom()
                height = params.height
                measuredHeight = (view)!!.getHeight()
                applied = true
                originalBackground = (view)!!.getBackground()
            } else {
                if (((view)!!.getPaddingBottom() != lastPadding)) {
                    padding = (view)!!.getPaddingBottom()
                }
                if ((params.height != lastHeight)) {
                    height = params.height
                }
            }
            if ((fixed && ((view)!!.getBackground() == null))) {
                var fill: Drawable? = childBackground(view)
                if ((fill != null)) {
                    var state: Drawable.ConstantState? = (fill)!!.getConstantState()
                    extendedBackground = (if ((state == null)) fill else (state)!!.newDrawable((view)!!.getResources()).mutate())
                    (view)!!.setBackground(extendedBackground)
                }
            }
            lastPadding = (padding + inset)
            lastHeight = (if (fixed) (((if ((height > 0)) height else measuredHeight)) + inset) else height)
            var changed: Boolean = false
            if (((view)!!.getPaddingBottom() != lastPadding)) {
                (view)!!.setPadding((view)!!.getPaddingLeft(), (view)!!.getPaddingTop(), (view)!!.getPaddingRight(), lastPadding)
                changed = true
            }
            if ((params.height != lastHeight)) {
                params.height = lastHeight
                (view)!!.setLayoutParams(params)
                changed = true
            }
            if ((scroll && (view)!!.getClipToPadding())) {
                (view)!!.setClipToPadding(false)
                changed = true
            }
            return changed
        }

        private fun childBackground(parent: ViewGroup): Drawable? {
            run {
                var i: Int = 0
                while ((i < parent.getChildCount())) {
                    var child: View = parent.getChildAt(i)
                    if ((child.getBackground() != null)) {
                        return child.getBackground()
                    }
                    val group = child as? ViewGroup
                    if (group != null) {
                        var fill: Drawable? = childBackground(group)
                        if ((fill != null)) {
                            return fill
                        }
                    }
                    i++
                }
            }
            return null
        }

        fun restore(): Boolean {
            if (!applied) {
                return false
            }
            applied = false
            var changed: Boolean = false
            if (((extendedBackground != null) && ((view)!!.getBackground() == extendedBackground))) {
                (view)!!.setBackground(originalBackground)
                extendedBackground = null
                changed = true
            }
            if (((view)!!.getPaddingBottom() == lastPadding)) {
                (view)!!.setPadding((view)!!.getPaddingLeft(), (view)!!.getPaddingTop(), (view)!!.getPaddingRight(), padding)
                changed = true
            }
            var params: ViewGroup.LayoutParams = (view)!!.getLayoutParams()
            if ((((params != null) && (params.height == lastHeight)) && (lastHeight != height))) {
                params.height = height
                (view)!!.setLayoutParams(params)
                changed = true
            }
            if (((scroll && !(view)!!.getClipToPadding()) && clip)) {
                (view)!!.setClipToPadding(clip)
                changed = true
            }
            return changed
        }
    }
}
