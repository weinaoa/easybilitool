/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.view.View
import android.view.ViewGroup
import java.util.Collections
import java.util.IdentityHashMap

/** Keep navigation safety inside each horizontal page's scrolling list. */
internal class QuickConsumeInsets {
    private class Saved(val list: ViewGroup) {
        val baseline = DetailInsetState()
        val clip = list.clipToPadding

        fun apply(bottom: Int): Boolean {
            baseline.apply(list.paddingBottom, list.layoutParams.height, list.height, bottom, false)
            val changed = list.paddingBottom != baseline.lastPadding || list.clipToPadding
            if (list.paddingBottom != baseline.lastPadding) {
                list.setPadding(list.paddingLeft, list.paddingTop, list.paddingRight, baseline.lastPadding)
            }
            list.clipToPadding = false
            return changed
        }

        fun restore() {
            if (list.paddingBottom == baseline.lastPadding) {
                list.setPadding(list.paddingLeft, list.paddingTop, list.paddingRight, baseline.padding)
            }
            list.clipToPadding = clip
            baseline.restored()
        }
    }

    private val saved = IdentityHashMap<ViewGroup, Saved>()

    fun apply(content: View, inset: Int): Boolean {
        val id = content.resources.getIdentifier("dy_pager", "id", content.context.packageName)
        val pager = if (id == 0) null else content.findViewById<ViewGroup>(id)
        val visible = Collections.newSetFromMap(IdentityHashMap<ViewGroup, Boolean>())
        val location = IntArray(2)
        var changed = false
        fun visit(node: View) {
            if (node !is ViewGroup) return
            if (node.javaClass.name == "androidx.recyclerview.widget.RecyclerView" && node.isLaidOut) {
                visible.add(node)
                node.getLocationInWindow(location)
                // Some host lists extend below the window. Their extra height must
                // also be scrollable before the final item clears the gesture area.
                val bottom = (location[1] + node.height - content.rootView.height + inset).coerceAtLeast(0)
                changed = saved.getOrPut(node) { Saved(node) }.apply(bottom) || changed
            } else {
                for (index in 0 until node.childCount) visit(node.getChildAt(index))
            }
        }
        pager?.let(::visit)
        val entries = saved.entries.iterator()
        while (entries.hasNext()) {
            val entry = entries.next()
            if (entry.key !in visible) {
                entry.value.restore()
                entries.remove()
            }
        }
        return changed
    }

    fun restore() {
        saved.values.forEach { it.restore() }
        saved.clear()
    }
}
