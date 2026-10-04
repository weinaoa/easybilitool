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

package com.weinaoa.easybilitool;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Shared attribution page for the launcher and host settings. */
final class About {
    private record Reference(String name, String detail, String url) {}
    private static final Reference[] PROJECTS = {
        new Reference("BBZQ", "透明播放器状态栏与设置界面参考",
                "https://github.com/HSSkyBoy/BBZQ"),
        new Reference("哔哩漫游", "宿主设置入口与重启流程参考",
                "https://github.com/yujincheng08/BiliRoaming")
    };
    private static final Reference[] DOCUMENTS = {
        new Reference("Android · WindowInsets", "状态栏、导航栏、刘海与键盘安全区",
                "https://developer.android.com/reference/android/view/WindowInsets"),
        new Reference("Material Components · AppBarLayout", "首页底栏随原生顶栏收起比例联动",
                "https://developer.android.com/reference/com/google/android/material/appbar/AppBarLayout"),
        new Reference("AndroidX · RecyclerView", "主界面与详情列表的纵向滚动接口",
                "https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.OnScrollListener")
    };
    private static final Reference[] OPEN_SOURCE = {
        new Reference("源码与下载", "当前版本对应源码、APK 与校验值",
                "https://github.com/weinaoa/easybilitool/releases/tag/v" + BuildConfig.VERSION_NAME),
        new Reference("Mulan PubL v2 · 木兰公共许可证", "Copyright (c) 2026 weinaoa · 完整许可证",
                "https://github.com/weinaoa/easybilitool/blob/v" + BuildConfig.VERSION_NAME + "/LICENSE")
    };

    static void show(Context context) {
        boolean dark = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int ink = dark ? 0xffeeeeee : 0xff25252a;
        int muted = dark ? 0xffa0a0a5 : 0xff74747c;
        int accent = dark ? 0xffed82a5 : 0xffba3c67;
        LinearLayout body = new LinearLayout(context); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(context, 20), dp(context, 8), dp(context, 20), dp(context, 12));
        body.addView(text(context, "简单bili小工具", 18, ink, true));
        body.addView(text(context, "版本 " + BuildConfig.VERSION_NAME + " · 适配哔哩哔哩 8.95.0", 13, muted, false));
        body.addView(text(context, "沉浸状态栏 · 滑动时收起底栏 · 沉浸导航栏", 13, muted, false));
        group(context, body, "参考项目", PROJECTS, ink, muted, accent);
        group(context, body, "参考文档", DOCUMENTS, ink, muted, accent);
        group(context, body, "开源", OPEN_SOURCE, ink, muted, accent);
        ScrollView scroll = new ScrollView(context); scroll.addView(body);
        new AlertDialog.Builder(context).setTitle("关于").setView(scroll)
                .setPositiveButton("关闭", null).show();
    }

    private static void group(Context context, LinearLayout body, String title, Reference[] references, int ink, int muted, int accent) {
        TextView heading = text(context, title, 16, ink, true);
        heading.setPadding(0, dp(context, 16), 0, dp(context, 6)); body.addView(heading);
        for (Reference reference : references) {
            LinearLayout row = new LinearLayout(context); row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(context, 8), 0, dp(context, 8));
            row.addView(text(context, reference.name() + "  ↗", 15, accent, true));
            row.addView(text(context, reference.detail(), 13, muted, false));
            row.setContentDescription(reference.name() + "，" + reference.detail() + "，打开链接");
            row.setFocusable(true);
            row.setOnClickListener(view -> {
                try { context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(reference.url()))); }
                catch (RuntimeException error) { Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show(); }
            });
            body.addView(row);
        }
    }

    private static TextView text(Context context, String value, int size, int color, boolean bold) {
        TextView text = new TextView(context); text.setText(value); text.setTextSize(size); text.setTextColor(color);
        text.setPadding(0, dp(context, 3), 0, dp(context, 3));
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }
    private static int dp(Context context, int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
}
