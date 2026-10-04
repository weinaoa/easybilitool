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
import android.content.Intent
import android.content.res.Configuration
import android.os.*
import android.graphics.Typeface
import android.view.*
import android.widget.*

/**
 * Launcher introduction; feature switches live inside Bilibili.
 */
class SettingsActivity : Activity() {
    private var ink: Int = 0
    private var muted: Int = 0

    private fun dp(value: Int): Int {
        return Math.round((value * getResources().getDisplayMetrics().density))
    }

    private fun text(value: String, size: Int, color: Int): TextView {
        var text: TextView = TextView(this)
        text.setText(value)
        text.setTextSize(size.toFloat())
        text.setTextColor(color)
        text.setPadding(0, dp(8), 0, dp(8))
        return text
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        var dark: Boolean = (((getResources().getConfiguration().uiMode and Configuration.UI_MODE_NIGHT_MASK)) == Configuration.UI_MODE_NIGHT_YES)
        ink = (if (dark) 0xffeeeeee.toInt() else 0xff25252a.toInt())
        muted = (if (dark) 0xffa0a0a5.toInt() else 0xff74747c.toInt())
        var scroll: ScrollView = ScrollView(this)
        scroll.setFillViewport(true)
        var body: LinearLayout = LinearLayout(this)
        body.setOrientation(LinearLayout.VERTICAL)
        body.setPadding(dp(24), dp(24), dp(24), dp(24))
        var icon: ImageView = ImageView(this)
        icon.setImageResource(R.mipmap.ic_launcher)
        body.addView(icon, LinearLayout.LayoutParams(dp(80), dp(80)))
        var title: TextView = text(getString(R.string.app_name), 25, ink)
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        body.addView(title)
        body.addView(text(("版本 " + BuildConfig.VERSION_NAME), 13, muted))
        for (field in Config.FIELDS) {

            var name: TextView = text(field.label(), 17, ink)
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            body.addView(name)
            body.addView(text(field.help(), 14, muted))
        }
        body.addView(text("在 LSPosed 启用本模块，作用域选择哔哩哔哩，然后重新打开哔哩哔哩。", 14, muted))
        body.addView(text("功能设置：哔哩哔哩 → 我的 → 设置 → 简单bili小工具。", 14, muted))
        var open: Button = Button(this)
        open.setText("打开功能设置")
        open.setOnClickListener(lambda@ { v -> openHost() })
        body.addView(open, LinearLayout.LayoutParams(-1, -2))
        var about: Button = Button(this)
        about.setText("关于")
        about.setOnClickListener(lambda@ { v -> About.show(this) })
        body.addView(about, LinearLayout.LayoutParams(-1, -2))
        scroll.addView(body)
        setContentView(scroll)
        scroll.setOnApplyWindowInsetsListener(lambda@ { view, insets -> if ((Build.VERSION.SDK_INT >= 30)) {
                var safe: android.graphics.Insets = insets.getInsets((WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()))
                scroll.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            } else {
                scroll.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom())
            }
            return@lambda insets })
        if ((Build.VERSION.SDK_INT >= 30)) {
            getWindow().setDecorFitsSystemWindows(false)
        }
        scroll.requestApplyInsets()
    }

    private fun openHost() {
        var intent: Intent? = SettingsRoute.launch(this)
        if ((intent == null)) {
            Toast.makeText(this, "请先安装哔哩哔哩", Toast.LENGTH_LONG).show()
            return
        }
        try {
            startActivity(intent)
        } catch (error: RuntimeException) {
            Toast.makeText(this, "无法打开哔哩哔哩", Toast.LENGTH_LONG).show()
        }
    }
}
