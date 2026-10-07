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
    private val baselines = WeakHashMap<View, DetailInsetState>()

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
                changed = (adjustments.computeIfAbsent(bar, lambda@ { v -> Adjustment(bar, true, false, baselines.getOrPut(bar) { DetailInsetState() }) }).apply(inset) || changed)
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

    private fun visibleContent(view: View, id: Int): View? {
        if (!view.isShown) return null
        if (view is ViewGroup) {
            for (index in view.childCount - 1 downTo 0) {
                visibleContent(view.getChildAt(index), id)?.let { return it }
            }
        }
        return if (view.id == id) view else null
    }

    private fun adjust(content: View, id: Int, fixed: Boolean, inset: Int, present: MutableSet<View>): Boolean {
        val view = if (id == 0) null else visibleContent(content, id)
        if (((!(view is ViewGroup) || ((view)!!.getLayoutParams() == null)) || !(view)!!.isLaidOut())) {
            return false
        }
        if ((!fixed && !isRecycler(view))) {
            return false
        }
        present.add(view)
        var adjustment: Adjustment = adjustments.computeIfAbsent(view, lambda@ { v -> Adjustment((v as ViewGroup), fixed, !fixed, baselines.getOrPut(v) { DetailInsetState() }) })
        return adjustment.apply(inset)
    }

    private fun adjustSafeArea(content: View, id: Int, inset: Int, present: MutableSet<View>): Boolean {
        val view = if (id == 0) null else visibleContent(content, id)
        if (((!(view is ViewGroup) || ((view)!!.getLayoutParams() == null)) || !(view)!!.isLaidOut())) {
            return false
        }
        present.add(view)
        return adjustments.computeIfAbsent(view, lambda@ { v -> Adjustment((v as ViewGroup), false, false, baselines.getOrPut(v) { DetailInsetState() }) }).apply(inset)
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
        // Baselines contain only numbers; keep them while a stopped or cached view is alive.
    }

    private class Adjustment {
        lateinit var view: ViewGroup

        @JvmField var fixed: Boolean = false
        @JvmField var scroll: Boolean = false

        @JvmField var clip: Boolean = false

        private lateinit var metrics: DetailInsetState

        @JvmField var originalBackground: Drawable? = null
        @JvmField var extendedBackground: Drawable? = null

        constructor(view: ViewGroup, fixed: Boolean, scroll: Boolean, metrics: DetailInsetState) {
            this.view = view
            this.fixed = fixed
            this.scroll = scroll
            this.metrics = metrics
            clip = (view)!!.getClipToPadding()
        }

        fun apply(inset: Int): Boolean {
            var params: ViewGroup.LayoutParams = (view)!!.getLayoutParams()
            if (!metrics.applied) originalBackground = view.background
            metrics.apply(view.paddingBottom, params.height, view.height, inset, fixed)
            if ((fixed && ((view)!!.getBackground() == null))) {
                var fill: Drawable? = childBackground(view)
                if ((fill != null)) {
                    var state: Drawable.ConstantState? = (fill)!!.getConstantState()
                    extendedBackground = (if ((state == null)) fill else (state)!!.newDrawable((view)!!.getResources()).mutate())
                    (view)!!.setBackground(extendedBackground)
                }
            }
            var changed: Boolean = false
            if (((view)!!.getPaddingBottom() != metrics.lastPadding)) {
                (view)!!.setPadding((view)!!.getPaddingLeft(), (view)!!.getPaddingTop(), (view)!!.getPaddingRight(), metrics.lastPadding)
                changed = true
            }
            if ((params.height != metrics.lastHeight)) {
                params.height = metrics.lastHeight
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
            if (!metrics.applied) {
                return false
            }
            metrics.restored()
            var changed: Boolean = false
            if (((extendedBackground != null) && ((view)!!.getBackground() == extendedBackground))) {
                (view)!!.setBackground(originalBackground)
                extendedBackground = null
                changed = true
            }
            if (((view)!!.getPaddingBottom() == metrics.lastPadding)) {
                (view)!!.setPadding((view)!!.getPaddingLeft(), (view)!!.getPaddingTop(), (view)!!.getPaddingRight(), metrics.padding)
                changed = true
            }
            var params: ViewGroup.LayoutParams = (view)!!.getLayoutParams()
            if ((((params != null) && (params.height == metrics.lastHeight)) && (metrics.lastHeight != metrics.height))) {
                params.height = metrics.height
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
