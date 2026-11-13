/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.insets.GradientProtection
import androidx.core.view.insets.ColorProtection
import androidx.core.view.insets.Protection
import androidx.core.view.insets.ProtectionLayout

/** AndroidX owns gradient rendering, thickness and system-bar visibility handling. */
internal class StatusBarProtection(private val window: Window) {
    private var layout: ProtectionLayout? = null
    private val top = GradientProtection(WindowInsetsCompat.Side.TOP, Color.BLACK)
    private var configuration: Pair<Boolean,Int?>? = null

    fun apply(statusGradient: Boolean = true, navigationColor: Int? = null) {
        if(!statusGradient && navigationColor==null) { restore();return }
        // Respect the host's icon style and protect its contrast over video content.
        val darkIcons = WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars
        top.color = if (darkIcons) Color.WHITE else Color.BLACK
        val next=statusGradient to navigationColor
        if (layout != null && configuration==next) return
        restore()
        val root = window.decorView as? ViewGroup ?: return
        val protections=mutableListOf<Protection>()
        if(statusGradient) protections.add(top)
        if(navigationColor!=null) protections.add(ColorProtection(WindowInsetsCompat.Side.BOTTOM,navigationColor))
        val protection = ProtectionLayout(root.context, protections).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            isFocusable = false
            isClickable = false
        }
        layout = protection
        configuration = next
        root.addView(protection, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    fun diagnostic(): String {
        val view = layout ?: return "absent"
        val children = (0 until view.childCount).joinToString(";") {
            val child = view.getChildAt(it)
            "${child.width}x${child.height}@${child.x},${child.y} visible=${child.visibility}"
        }
        return "attached=${view.isAttachedToWindow} color=${top.color} children=$children"
    }

    fun restore() {
        layout?.let { (it.parent as? ViewGroup)?.removeView(it) }
        layout = null
        configuration = null
    }
}
