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

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Shared attribution page for the launcher and host settings.
 */
class About {
    private data class Reference(val name: String, val detail: String, val url: String) {
        fun name(): String = name

        fun detail(): String = detail

        fun url(): String = url
    }

    companion object {
        private val PROJECTS: Array<Reference> = arrayOf(Reference("BBZQ", "透明播放器状态栏与设置界面参考", "https://github.com/HSSkyBoy/BBZQ"), Reference("哔哩漫游", "宿主设置入口与重启流程参考", "https://github.com/yujincheng08/BiliRoaming"))

        private val DOCUMENTS: Array<Reference> = arrayOf(Reference("Android · WindowInsets", "状态栏、导航栏、刘海与键盘安全区", "https://developer.android.com/reference/android/view/WindowInsets"), Reference("Material Components · AppBarLayout", "首页底栏随原生顶栏收起比例联动", "https://developer.android.com/reference/com/google/android/material/appbar/AppBarLayout"), Reference("AndroidX · RecyclerView", "主界面与详情列表的纵向滚动接口", "https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.OnScrollListener"))

        private val OPEN_SOURCE: Array<Reference> = arrayOf(Reference("源码与下载", "当前版本对应源码、APK 与校验值", ("https://github.com/weinaoa/easybilitool/releases/tag/v" + BuildConfig.VERSION_NAME)), Reference("Mulan PubL v2 · 木兰公共许可证", "Copyright (c) 2026 weinaoa · 完整许可证", (("https://github.com/weinaoa/easybilitool/blob/v" + BuildConfig.VERSION_NAME) + "/LICENSE")))

        @JvmStatic fun show(context: Context) {
            var dark: Boolean = (((context.getResources().getConfiguration().uiMode and Configuration.UI_MODE_NIGHT_MASK)) == Configuration.UI_MODE_NIGHT_YES)
            var ink: Int = (if (dark) 0xffeeeeee.toInt() else 0xff25252a.toInt())
            var muted: Int = (if (dark) 0xffa0a0a5.toInt() else 0xff74747c.toInt())
            var accent: Int = (if (dark) 0xffed82a5.toInt() else 0xffba3c67.toInt())
            var body: LinearLayout = LinearLayout(context)
            body.setOrientation(LinearLayout.VERTICAL)
            body.setPadding(dp(context, 20), dp(context, 8), dp(context, 20), dp(context, 12))
            body.addView(text(context, "简单bili小工具", 18, ink, true))
            body.addView(text(context, (("版本 " + BuildConfig.VERSION_NAME) + " · 适配哔哩哔哩 8.95.0"), 13, muted, false))
            body.addView(text(context, "沉浸状态栏 · 滑动时收起底栏 · 沉浸导航栏", 13, muted, false))
            group(context, body, "参考项目", PROJECTS, ink, muted, accent)
            group(context, body, "参考文档", DOCUMENTS, ink, muted, accent)
            group(context, body, "开源", OPEN_SOURCE, ink, muted, accent)
            var scroll: ScrollView = ScrollView(context)
            scroll.addView(body)
            AlertDialog.Builder(context).setTitle("关于").setView(scroll).setPositiveButton("关闭", null).show()
        }

        @JvmStatic private fun group(context: Context, body: LinearLayout, title: String, references: Array<Reference>, ink: Int, muted: Int, accent: Int) {
            var heading: TextView = text(context, title, 16, ink, true)
            heading.setPadding(0, dp(context, 16), 0, dp(context, 6))
            body.addView(heading)
            for (reference in references) {

                var row: LinearLayout = LinearLayout(context)
                row.setOrientation(LinearLayout.VERTICAL)
                row.setPadding(0, dp(context, 8), 0, dp(context, 8))
                row.addView(text(context, (reference.name() + "  ↗"), 15, accent, true))
                row.addView(text(context, reference.detail(), 13, muted, false))
                row.setContentDescription((((reference.name() + "，") + reference.detail()) + "，打开链接"))
                row.setFocusable(true)
                row.setOnClickListener(lambda@ { view -> try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(reference.url())))
                    } catch (error: RuntimeException) {
                        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
                    } })
                body.addView(row)
            }
        }

        @JvmStatic private fun text(context: Context, value: String, size: Int, color: Int, bold: Boolean): TextView {
            var text: TextView = TextView(context)
            text.setText(value)
            text.setTextSize(size.toFloat())
            text.setTextColor(color)
            text.setPadding(0, dp(context, 3), 0, dp(context, 3))
            if (bold) {
                text.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            }
            return text
        }

        @JvmStatic private fun dp(context: Context, value: Int): Int {
            return Math.round((value * context.getResources().getDisplayMetrics().density))
        }
    }
}
