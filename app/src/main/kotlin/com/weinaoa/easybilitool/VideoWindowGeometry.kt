/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.app.Activity
import android.os.Build
import android.os.SystemClock
import java.util.WeakHashMap
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets

/** Bounded geometry diagnostics, with no account, text, URLs or media metadata. */
internal object VideoWindowGeometry {
    private val sampled = WeakHashMap<Activity, Long>()
    fun record(activity: Activity) {
        if (activity.javaClass.name != "com.bilibili.ship.theseus.detail.UnitedBizDetailsActivity") return
        if (Build.VERSION.SDK_INT < 30) return
        val now = SystemClock.uptimeMillis()
        if (now - (sampled[activity] ?: -1000L) < 1000L) return
        sampled[activity] = now
        val decor = activity.window.decorView
        val insets = decor.rootWindowInsets
        RuntimeStatus.record("VIDEO_WINDOW decor=${decor.width}x${decor.height} ui=${decor.systemUiVisibility} " +
            "status=${insets?.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars())?.top} " +
            "nav=${insets?.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars())?.bottom} " +
            "ime=${insets?.isVisible(WindowInsets.Type.ime())}")
        val ids = listOf("fl_root_container", "video_area", "comment_container", "nav_top_bar")
        for (name in ids) {
            val id = activity.resources.getIdentifier(name, "id", activity.packageName)
            var view: View? = if (id == 0) null else decor.findViewById(id)
            var depth = 0
            while (view != null && depth < (if (name == "fl_root_container") 3 else 1)) {
                val v = view
                val p = v.layoutParams
                val margin = p as? ViewGroup.MarginLayoutParams
                val location = IntArray(2); v.getLocationInWindow(location)
                RuntimeStatus.record("VIDEO_${name.uppercase()}_$depth ${v.javaClass.name} " +
                    "xy=${location.joinToString()} size=${v.width}x${v.height} lp=${p?.width}x${p?.height} " +
                    "padding=${v.paddingTop},${v.paddingBottom} margin=${margin?.topMargin},${margin?.bottomMargin} " +
                    "fits=${v.fitsSystemWindows} bg=${(v.background as? ColorDrawable)?.color}")
                if (v === decor) break
                view = v.parent as? View
                depth++
            }
        }
    }
}
