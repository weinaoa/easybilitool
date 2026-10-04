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

/**
 * Let the main feed draw behind navigation; inset only the fixed tab controls.
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

    private var lastHeight: Int = 0
    private var lastPadding: Int = 0
    private var lastBackgroundHeight: Int = 0
    private var lastBackgroundMargin: Int = 0

    private var controlsHeight: Int = 0
    private var controlsMeasuredHeight: Int = 0
    private var lastControlsHeight: Int = 0

    private var backgroundScale: ImageView.ScaleType? = null

    constructor(activity: Activity) {
        this.activity = activity
    }

    fun bind(content: View): Boolean {
        if (!activity.javaClass.getName().equals("tv.danmaku.bili.MainActivityV2")) {
            return false
        }
        var id: Int = activity.getResources().getIdentifier("bottom_navigation", "id", "tv.danmaku.bili")
        var next: View? = (if ((id == 0)) null else content.findViewById(id))
        if (((next == null) || !(next)!!.javaClass.getName().equals("com.bilibili.lib.homepage.widget.TabHost"))) {
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
        return ((controls is ViewGroup && (background != null)) && (background)!!.getLayoutParams() is ViewGroup.MarginLayoutParams)
    }

    fun apply(inset: Int): Boolean {
        if ((((controls == null) || !(controls)!!.isLaidOut()) || ((controls)!!.getHeight() <= 0))) {
            return false
        }
        var p: ViewGroup.LayoutParams = (bar)!!.getLayoutParams()
        var cp: ViewGroup.LayoutParams = (controls)!!.getLayoutParams()
        var bg: ViewGroup.MarginLayoutParams = ((background)!!.getLayoutParams() as ViewGroup.MarginLayoutParams)
        if (!applied) {
            height = InsetBaseline.value(captured, p.height, lastHeight, height)
            padding = InsetBaseline.value(captured, controls!!.paddingBottom, lastPadding, padding)
            controlsHeight = InsetBaseline.value(captured, cp.height, lastControlsHeight, controlsHeight)
            controlsMeasuredHeight = InsetBaseline.value(captured, controls!!.height, lastControlsHeight, controlsMeasuredHeight)
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
            }
            if ((bg.bottomMargin != lastBackgroundMargin)) {
                backgroundMargin = bg.bottomMargin
            }
        }
        lastHeight = (if ((height > 0)) (height + inset) else height)
        lastPadding = (padding + inset)
        lastControlsHeight = (((if ((controlsHeight > 0)) controlsHeight else controlsMeasuredHeight)) + inset)
        lastBackgroundHeight = (if ((backgroundHeight > 0)) (backgroundHeight + inset) else backgroundHeight)
        lastBackgroundMargin = backgroundMargin
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
        return changed
    }

    fun restore() {
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
