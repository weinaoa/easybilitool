package com.weinaoa.easybilitool;

import android.app.*;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.*;
import android.widget.*;

/** One screen, three switches, explicit save and restart. */
final class InAppSettings {
    private final Activity activity;
    private Context context;
    private SettingsDraft draft;
    private Dialog dialog;
    private boolean closePrompt;
    private int ink, muted, background, surface, accent;

    InAppSettings(Context context) { activity = HostActions.activity(context); }
    boolean isShowing() { return dialog != null && dialog.isShowing(); }
    private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    private TextView label(String text, int size, int color) {
        TextView view = new TextView(context);
        view.setText(text); view.setTextSize(size); view.setTextColor(color);
        view.setIncludeFontPadding(false);
        return view;
    }
    void show() {
        boolean dark = (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        context = new ContextThemeWrapper(activity, dark ? android.R.style.Theme_Material_NoActionBar : android.R.style.Theme_Material_Light_NoActionBar);
        ink = dark ? 0xffeeeeee : 0xff25252a;
        muted = dark ? 0xffa0a0a5 : 0xff74747c;
        background = dark ? 0xff111111 : 0xfff4f4f7;
        surface = dark ? 0xff1f1f1f : Color.WHITE;
        accent = dark ? 0xffed82a5 : 0xffba3c67;
        try { draft = new SettingsDraft(HostSettings.read(activity)); }
        catch (RuntimeException error) { message("无法读取设置：" + error.getMessage()); return; }
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setOnKeyListener((d, key, event) -> {
            if (key != KeyEvent.KEYCODE_BACK) return false;
            if (event.getAction() == KeyEvent.ACTION_UP) close();
            return true;
        });
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(background);
        LinearLayout toolbar = new LinearLayout(context);
        toolbar.setGravity(Gravity.CENTER_VERTICAL); toolbar.setBackgroundColor(surface);
        toolbar.setPadding(dp(4), 0, dp(8), 0);
        TextView back = label("‹", 28, ink);
        back.setGravity(Gravity.CENTER); back.setContentDescription("返回");
        back.setOnClickListener(v -> close());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(40), dp(56)));
        TextView title = label("简单bili小工具", 19, ink);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button done = new Button(context); done.setText("完成");
        done.setOnClickListener(v -> close()); toolbar.addView(done);
        root.addView(toolbar);
        ScrollView scroll = new ScrollView(context); scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(context); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(20), dp(16), dp(24));
        LinearLayout card = new LinearLayout(context); card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setColor(surface); cardBackground.setCornerRadius(dp(16));
        card.setBackground(cardBackground);
        for (Config.Field field : Config.FIELDS) {
            if (card.getChildCount() > 0) {
                View divider = new View(context); divider.setBackgroundColor(dark ? 0xff303030 : 0xffe9e9ee);
                LinearLayout.LayoutParams line = new LinearLayout.LayoutParams(-1, dp(1));
                line.leftMargin = line.rightMargin = dp(16); card.addView(divider, line);
            }
            LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(76)); row.setPadding(dp(16), dp(12), dp(16), dp(12));
            LinearLayout labels = new LinearLayout(context); labels.setOrientation(LinearLayout.VERTICAL);
            TextView name = label(field.label(), 16, ink); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            labels.addView(name);
            TextView help = label(field.help(), 13, muted);
            LinearLayout.LayoutParams description = new LinearLayout.LayoutParams(-1, -2); description.topMargin = dp(4);
            labels.addView(help, description); row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
            Switch toggle = new Switch(context); toggle.setContentDescription(field.label());
            toggle.setChecked(draft.current().b(field.key())); toggle.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
            toggle.setOnCheckedChangeListener((button, checked) -> draft.edit(field.key(), checked));
            LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(-2, -2); switchParams.leftMargin = dp(12);
            row.addView(toggle, switchParams); row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
            card.addView(row);
        }
        body.addView(card);
        TextView note = label("修改后点击“保存并重启”生效。", 13, muted);
        note.setPadding(dp(4), dp(16), dp(4), dp(16)); body.addView(note);
        Button save = new Button(context); save.setText("保存并重启"); save.setOnClickListener(v -> save());
        body.addView(save, new LinearLayout.LayoutParams(-1, -2));
        Button about = new Button(context); about.setText("关于"); about.setOnClickListener(v -> About.show(context));
        body.addView(about, new LinearLayout.LayoutParams(-1, -2));
        TextView version = label("版本 " + BuildConfig.VERSION_NAME + " · 适配哔哩哔哩 8.95.0", 12, muted);
        version.setPadding(dp(4), dp(16), dp(4), 0); body.addView(version);
        scroll.addView(body); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        boolean edge = draft.committed().b("NAVIGATION_EDGE_TO_EDGE");
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = safe.left; top = safe.top; right = safe.right; bottom = safe.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft(); top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight(); bottom = insets.getSystemWindowInsetBottom();
            }
            root.setPadding(left, top, right, edge ? 0 : bottom);
            scroll.setPadding(0, 0, 0, edge ? bottom : 0); scroll.setClipToPadding(!edge);
            return insets;
        });
        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(background));
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.setStatusBarColor(Color.TRANSPARENT); window.setNavigationBarColor(edge ? Color.TRANSPARENT : background);
            if (Build.VERSION.SDK_INT >= 29) { window.setStatusBarContrastEnforced(false); window.setNavigationBarContrastEnforced(false); }
            if (Build.VERSION.SDK_INT >= 30) {
                window.setDecorFitsSystemWindows(false);
                WindowManager.LayoutParams params = window.getAttributes(); params.setFitInsetsTypes(0); window.setAttributes(params);
            } else window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | (dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
        }
        dialog.show();
        if (Build.VERSION.SDK_INT >= 33) dialog.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::close);
        if (window != null) {
            window.setLayout(-1, -1);
            if (Build.VERSION.SDK_INT >= 30 && window.getInsetsController() != null)
                window.getInsetsController().setSystemBarsAppearance(dark ? 0 : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
        root.requestApplyInsets();
    }
    private void close() {
        if (!draft.dirty()) { dialog.dismiss(); return; }
        if (closePrompt) return;
        closePrompt = true;
        AlertDialog prompt = new AlertDialog.Builder(context).setTitle("设置尚未保存")
            .setMessage("保存并重启哔哩哔哩后生效。")
            .setPositiveButton("保存并重启", (d, which) -> save())
            .setNegativeButton("放弃修改", (d, which) -> { draft.discard(); dialog.dismiss(); })
            .setNeutralButton("继续编辑", null).create();
        prompt.setOnDismissListener(d -> closePrompt = false); prompt.show();
    }
    private void save() {
        try {
            HostSettings.write(activity, draft.current()); draft.saved();
            HostActions.restart(activity);
        } catch (RuntimeException error) { message(error.getMessage()); }
    }
    private void message(String value) { Toast.makeText(activity, value, Toast.LENGTH_LONG).show(); }
}
