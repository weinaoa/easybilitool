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
import android.widget.ImageView
import java.util.WeakHashMap

/**
 * Keep tab controls safe and preserve the finite My page end position.
 */
class MainNavigationInsets {
    private lateinit var activity: Activity

    private var bar: View? = null
    private var background: View? = null
    private var controls: View? = null

    private var applied: Boolean = false
    private var captured: Boolean = false
    private var clip: Boolean = false

    private var height: Int = 0
    private var padding: Int = 0
    private var backgroundHeight: Int = 0
    private var backgroundMargin: Int = 0
    private var backgroundMeasuredHeight = 0
    private val bottomAvoidance = MainBottomAvoidance()
    private var lastGeometry = ""

    private var lastHeight: Int = 0
    private var lastPadding: Int = 0
    private var lastBackgroundHeight: Int = 0
    private var lastBackgroundMargin: Int = 0

    private var controlsHeight: Int = 0
    private var controlsMeasuredHeight: Int = 0
    private var lastControlsHeight: Int = 0

    private var backgroundScale: ImageView.ScaleType? = null
    private var content: View? = null
    private var mine: ViewGroup? = null
    private var mineClip = true
    private val mineBaselines = WeakHashMap<View, DetailInsetState>()

    constructor(activity: Activity) {
        this.activity = activity
    }

    fun bind(content: View): Boolean {
        this.content = content
        if (!activity.javaClass.getName().equals("tv.danmaku.bili.MainActivityV2")) {
            return false
        }
        var id: Int = activity.getResources().getIdentifier("bottom_navigation", "id", "tv.danmaku.bili")
        var next: View? = (if ((id == 0)) null else content.findViewById(id))
        if (((next == null) || !(next)!!.javaClass.getName().equals("com.bilibili.lib.homepage.widget.TabHost"))) {
            RuntimeStatus.record("MAIN_NAV_BIND bar=${next?.javaClass?.name} id=$id reason=bar_type")
            return false
        }
        var bgId: Int = activity.getResources().getIdentifier("tab_background", "id", "tv.danmaku.bili")
        var controlsId: Int = activity.getResources().getIdentifier("container", "id", "tv.danmaku.bili")
        var nextBackground: View? = (if ((bgId == 0)) null else (next)!!.findViewById(bgId))
        var nextControls: View? = (if ((controlsId == 0)) null else (next)!!.findViewById(controlsId))
        if ((((bar !== next) || (background !== nextBackground)) || (controls !== nextControls))) {
            restore()
            captured = false
            bar = next
            background = nextBackground
            controls = nextControls
        }
        val valid = controls is ViewGroup && background?.layoutParams is ViewGroup.MarginLayoutParams
        RuntimeStatus.record("MAIN_NAV_BIND bar=${next.javaClass.name} controls=${controls?.javaClass?.name} " +
            "background=${background?.javaClass?.name} bgParams=${background?.layoutParams?.javaClass?.name} valid=$valid")
        return valid
    }

    fun apply(inset: Int): Boolean {
        if ((((controls == null) || !(controls)!!.isLaidOut()) || ((controls)!!.getHeight() <= 0))) {
            RuntimeStatus.record("MAIN_NAV_LAYOUT waiting controls=${controls?.height} laidOut=${controls?.isLaidOut}")
            return false
        }
        RuntimeStatus.record("MAIN_NAV_LAYOUT applying inset=$inset")
        var p: ViewGroup.LayoutParams = (bar)!!.getLayoutParams()
        var cp: ViewGroup.LayoutParams = (controls)!!.getLayoutParams()
        var bg: ViewGroup.MarginLayoutParams = ((background)!!.getLayoutParams() as ViewGroup.MarginLayoutParams)
        if (!applied) {
            height = InsetBaseline.value(captured, p.height, lastHeight, height)
            padding = InsetBaseline.value(captured, controls!!.paddingBottom, lastPadding, padding)
            controlsHeight = InsetBaseline.value(captured, cp.height, lastControlsHeight, controlsHeight)
            controlsMeasuredHeight = InsetBaseline.value(captured, controls!!.height, lastControlsHeight, controlsMeasuredHeight)
            backgroundMeasuredHeight = InsetBaseline.value(captured, background!!.height, lastBackgroundHeight, backgroundMeasuredHeight)
            backgroundHeight = InsetBaseline.value(captured, bg.height, lastBackgroundHeight, backgroundHeight)
            backgroundMargin = InsetBaseline.value(captured, bg.bottomMargin, lastBackgroundMargin, backgroundMargin)
            val scale = (background as? ImageView)?.scaleType
            if (!captured || scale != ImageView.ScaleType.FIT_XY) backgroundScale = scale
            if (!captured || (bar as ViewGroup).clipToPadding) clip = (bar as ViewGroup).clipToPadding
            captured = true
            applied = true
        } else {
            if ((p.height != lastHeight)) {
                height = p.height
            }
            if (((controls)!!.getPaddingBottom() != lastPadding)) {
                padding = (controls)!!.getPaddingBottom()
            }
            if ((cp.height != lastControlsHeight)) {
                controlsHeight = cp.height
            }
            if ((bg.height != lastBackgroundHeight)) {
                backgroundHeight = bg.height
                backgroundMeasuredHeight = background!!.height
            }
            if ((bg.bottomMargin != lastBackgroundMargin)) {
                backgroundMargin = bg.bottomMargin
            }
        }
        lastHeight = (if ((height > 0)) (height + inset) else height)
        lastPadding = (padding + inset)
        lastControlsHeight = (((if ((controlsHeight > 0)) controlsHeight else controlsMeasuredHeight)) + inset)
        // A skin may use WRAP_CONTENT rather than a fixed dimension. Its measured
        // baseline still has to grow, and an existing nav-sized margin must not
        // leave the expanded bitmap ending above the gesture area.
        lastBackgroundHeight = MainBottomGeometry.backgroundHeight(backgroundHeight, backgroundMeasuredHeight, inset)
        lastBackgroundMargin = backgroundMargin - MainBottomGeometry.consume(backgroundMargin, inset)
        var changed: Boolean = false
        if ((p.height != lastHeight)) {
            p.height = lastHeight
            (bar)!!.setLayoutParams(p)
            changed = true
        }
        if ((cp.height != lastControlsHeight)) {
            cp.height = lastControlsHeight
            (controls)!!.setLayoutParams(cp)
            changed = true
        }
        if (((controls)!!.getPaddingBottom() != lastPadding)) {
            (controls)!!.setPadding((controls)!!.getPaddingLeft(), (controls)!!.getPaddingTop(), (controls)!!.getPaddingRight(), lastPadding)
            changed = true
        }
        if (((bg.height != lastBackgroundHeight) || (bg.bottomMargin != lastBackgroundMargin))) {
            bg.height = lastBackgroundHeight
            bg.bottomMargin = lastBackgroundMargin
            (background)!!.setLayoutParams(bg)
            changed = true
        }
        if (((bar as ViewGroup)).getClipToPadding()) {
            ((bar as ViewGroup)).setClipToPadding(false)
        }
        val image = background as? ImageView
        if ((image != null && ((image)!!.getScaleType() != ImageView.ScaleType.FIT_XY))) {
            (image)!!.setScaleType(ImageView.ScaleType.FIT_XY)
        }
        changed = bottomAvoidance.apply(bar!!, content!!, activity.window.decorView.height, inset) || changed
        val geometry = "inset=$inset window=${activity.window.decorView.height} " +
            "content=${bottom(content!!)} bar=${bottom(bar!!)} background=${bottom(background!!)} " +
            "bgHeight=${background!!.height}/${bg.height} bgMargin=${bg.bottomMargin} " +
            "controls=${controls!!.height} padding=${controls!!.paddingBottom} " +
            "bg=${background!!.javaClass.simpleName}"
        if (geometry != lastGeometry) {
            lastGeometry = geometry
            RuntimeStatus.record("MAIN_NAV_GEOMETRY $geometry changed=$changed")
        }
        return applyMine(inset) || changed
    }

    private fun bottom(view: View): Int {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        return location[1] + view.height
    }

    private fun visibleMine(view: View, id: Int): ViewGroup? {
        if (!view.isShown) return null
        if (view is ViewGroup) {
            if (view.id == id) return view
            for (index in view.childCount - 1 downTo 0) {
                visibleMine(view.getChildAt(index), id)?.let { return it }
            }
        }
        return null
    }

    private fun applyMine(inset: Int): Boolean {
        val id = activity.resources.getIdentifier("mine_recycle", "id", activity.packageName)
        val next = if (id == 0) null else content?.let { visibleMine(it, id) }
        if (mine !== next) {
            restoreMine()
            mine = next
            mineClip = next?.clipToPadding ?: true
        }
        val view = mine ?: return false
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val extra = MainContentSafeArea.extra(location[1] + view.height, activity.window.decorView.height, inset)
        val metrics = mineBaselines.getOrPut(view) { DetailInsetState() }
        metrics.apply(view.paddingBottom, view.layoutParams.height, view.height, extra, false)
        var changed = false
        if (view.paddingBottom != metrics.lastPadding) {
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, metrics.lastPadding)
            changed = true
        }
        if (view.clipToPadding) { view.clipToPadding = false; changed = true }
        return changed
    }

    private fun restoreMine() {
        mine?.let { view ->
            mineBaselines[view]?.let { metrics ->
                if (metrics.applied && view.paddingBottom == metrics.lastPadding) {
                    view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, metrics.padding)
                }
                metrics.restored()
            }
            if (mineClip && !view.clipToPadding) view.clipToPadding = true
        }
        mine = null
    }

    fun restore() {
        bottomAvoidance.restore()
        restoreMine()
        if (!applied) {
            return
        }
        applied = false
        var p: ViewGroup.LayoutParams = (bar)!!.getLayoutParams()
        if ((p.height == lastHeight)) {
            p.height = height
            (bar)!!.setLayoutParams(p)
        }
        if (((controls)!!.getPaddingBottom() == lastPadding)) {
            (controls)!!.setPadding((controls)!!.getPaddingLeft(), (controls)!!.getPaddingTop(), (controls)!!.getPaddingRight(), padding)
        }
        var cp: ViewGroup.LayoutParams = (controls)!!.getLayoutParams()
        if ((cp.height == lastControlsHeight)) {
            cp.height = controlsHeight
            (controls)!!.setLayoutParams(cp)
        }
        val bg = (background)!!.getLayoutParams() as? ViewGroup.MarginLayoutParams
        if (bg != null) {
            var changed: Boolean = false
            if (((bg)!!.height == lastBackgroundHeight)) {
                (bg)!!.height = backgroundHeight
                changed = true
            }
            if (((bg)!!.bottomMargin == lastBackgroundMargin)) {
                (bg)!!.bottomMargin = backgroundMargin
                changed = true
            }
            if (changed) {
                (background)!!.setLayoutParams(bg)
            }
        }
        val image = background as? ImageView
        if (((image != null && ((image)!!.getScaleType() == ImageView.ScaleType.FIT_XY)) && (backgroundScale != null))) {
            (image)!!.setScaleType(backgroundScale)
        }
        if (!((bar as ViewGroup)).getClipToPadding()) {
            ((bar as ViewGroup)).setClipToPadding(clip)
        }
    }
}
