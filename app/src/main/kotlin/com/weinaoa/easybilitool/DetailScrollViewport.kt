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
import java.lang.reflect.Field
import java.util.*

/**

 * Let the actual list draw behind a collapsing native bar, with stable end padding.

 */
class DetailScrollViewport {
    private lateinit var activity: Activity

    private lateinit var bar: ViewGroup

    private lateinit var decor: View

    private val anchors: MutableMap<View, Anchor> = IdentityHashMap()

    private val lists: MutableMap<ViewGroup, SafeScroll> = IdentityHashMap()

    constructor(activity: Activity, bar: ViewGroup, decor: View) {
        this.activity = activity
        this.bar = bar
        this.decor = decor
    }

    fun apply() {
        for (name in arrayOf("opus_nested_scroll", "comment_main")) {

            var id: Int = id(name)
            var content: View? = (if ((id == 0)) null else decor.findViewById(id))
            if (((content != null) && ((content)!!.getParent() == bar.getParent()))) {
                var anchor: Anchor? = anchors[content] ?: Anchor.read(content, bar.id)?.also { anchors[content] = it }
                if ((anchor != null)) {
                    (anchor)!!.apply()
                }
            }
        }
        var at: IntArray = IntArray(2)
        bar.getLocationOnScreen(at)
        var nativeTop: Int = (at[1] - Math.round(DetailBottomBarHook.translation(bar)))
        reserve(decor, id("cmt3_recycler"), nativeTop)
    }

    private fun id(name: String): Int {
        return activity.getResources().getIdentifier(name, "id", "tv.danmaku.bili")
    }

    private fun reserve(view: View, id: Int, barTop: Int) {
        if ((id == 0)) {
            return
        }
        val group = view as? ViewGroup
        if ((((view.getId() == id) && group != null) && view.isShown())) {
            var at: IntArray = IntArray(2)
            view.getLocationOnScreen(at)
            lists.computeIfAbsent(group, ::SafeScroll).apply(JvmNumbers.max(0, ((at[1] + view.getHeight()) - barTop)))
        }
        if (group != null) {
            run {
                var i: Int = 0
                while ((i < (group)!!.getChildCount())) {
                    reserve((group)!!.getChildAt(i), id, barTop)
                    i++
                }
            }
        }
    }

    fun restore() {
        for (anchor in anchors.values) {

            if ((anchor != null)) {
                (anchor)!!.restore()
            }
        }
        anchors.clear()
        for (list in lists.values) {

            list.restore()
        }
        lists.clear()
    }

    private class Anchor {
        lateinit var view: View

        @JvmField var params: Any? = null

        lateinit var top: Field
        lateinit var bottom: Field

        @JvmField var originalTop: Int = 0
        @JvmField var originalBottom: Int = 0

        constructor(view: View, params: Any?, top: Field, bottom: Field, originalTop: Int, originalBottom: Int) {
            this.view = view
            this.params = params
            this.top = top
            this.bottom = bottom
            this.originalTop = originalTop
            this.originalBottom = originalBottom
        }

        fun apply() {
            if ((view.getLayoutParams() != params)) {
                return
            }
            try {
                if (((top.getInt(params) == originalTop) && (bottom.getInt(params) == originalBottom))) {
                    top.setInt(params, -1)
                    bottom.setInt(params, 0)
                    view.setLayoutParams((params as ViewGroup.LayoutParams))
                }
            } catch (ignored: ReflectiveOperationException) {

            }
        }

        fun restore() {
            if ((view.getLayoutParams() != params)) {
                return
            }
            try {
                if (((top.getInt(params) == -1) && (bottom.getInt(params) == 0))) {
                    top.setInt(params, originalTop)
                    bottom.setInt(params, originalBottom)
                    view.setLayoutParams((params as ViewGroup.LayoutParams))
                }
            } catch (ignored: ReflectiveOperationException) {

            }
        }

        companion object {
            @JvmStatic fun read(view: View, barId: Int): Anchor? {
                try {
                    var params: Any? = view.getLayoutParams()
                    var top: Field = Reflector.findField((params)!!.javaClass, "bottomToTop")
                    var bottom: Field = Reflector.findField((params)!!.javaClass, "bottomToBottom")
                    var t: Int = top.getInt(params)
                    var b: Int = bottom.getInt(params)
                    return (if ((t == barId)) Anchor(view, params, top, bottom, t, b) else null)
                } catch (unsupported: ReflectiveOperationException) {
                    return null
                }
            }
        }
    }

    private class SafeScroll {
        lateinit var view: ViewGroup

        @JvmField var clip: Boolean = false

        @JvmField var original: Int = 0
        @JvmField var last: Int = 0

        @JvmField var applied: Boolean = false

        constructor(view: ViewGroup) {
            this.view = view
            clip = view.getClipToPadding()
            original = view.getPaddingBottom()
        }

        fun apply(inset: Int) {
            if ((applied && (view.getPaddingBottom() != last))) {
                original = view.getPaddingBottom()
            }
            last = (original + inset)
            applied = true
            if ((view.getPaddingBottom() != last)) {
                view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), last)
            }
            if (view.getClipToPadding()) {
                view.setClipToPadding(false)
            }
        }

        fun restore() {
            if ((applied && (view.getPaddingBottom() == last))) {
                view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), original)
            }
            if ((!view.getClipToPadding() && clip)) {
                view.setClipToPadding(clip)
            }
        }
    }
}
