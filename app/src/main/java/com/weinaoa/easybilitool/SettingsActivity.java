package com.weinaoa.easybilitool;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.*;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;

/** Launcher introduction; feature switches live inside Bilibili. */
public final class SettingsActivity extends Activity {
    private int ink, muted;
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int color) {
        TextView text = new TextView(this); text.setText(value); text.setTextSize(size); text.setTextColor(color);
        text.setPadding(0, dp(8), 0, dp(8)); return text;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        ink = dark ? 0xffeeeeee : 0xff25252a; muted = dark ? 0xffa0a0a5 : 0xff74747c;
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(24), dp(24), dp(24), dp(24));
        ImageView icon = new ImageView(this); icon.setImageResource(R.mipmap.ic_launcher);
        body.addView(icon, new LinearLayout.LayoutParams(dp(80), dp(80)));
        TextView title = text(getString(R.string.app_name), 25, ink); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); body.addView(title);
        body.addView(text("版本 " + BuildConfig.VERSION_NAME, 13, muted));
        for (Config.Field field : Config.FIELDS) {
            TextView name = text(field.label(), 17, ink); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD); body.addView(name);
            body.addView(text(field.help(), 14, muted));
        }
        body.addView(text("在 LSPosed 启用本模块，作用域选择哔哩哔哩，然后重新打开哔哩哔哩。", 14, muted));
        body.addView(text("功能设置：哔哩哔哩 → 我的 → 设置 → 简单bili小工具。", 14, muted));
        Button open = new Button(this); open.setText("打开功能设置"); open.setOnClickListener(v -> openHost());
        body.addView(open, new LinearLayout.LayoutParams(-1, -2));
        Button about = new Button(this); about.setText("关于"); about.setOnClickListener(v -> About.show(this));
        body.addView(about, new LinearLayout.LayoutParams(-1, -2));
        scroll.addView(body); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                scroll.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            } else scroll.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        scroll.requestApplyInsets();
    }
    private void openHost() {
        Intent intent = SettingsRoute.launch(this);
        if (intent == null) { Toast.makeText(this, "请先安装哔哩哔哩", Toast.LENGTH_LONG).show(); return; }
        try { startActivity(intent); }
        catch (RuntimeException error) { Toast.makeText(this, "无法打开哔哩哔哩", Toast.LENGTH_LONG).show(); }
    }
}
