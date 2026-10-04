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

import android.app.*
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.*
import android.widget.*

/**
 * One screen, three switches, explicit save and restart.
 */
class InAppSettings {
    private lateinit var activity: Activity

    private var context: Context? = null

    private var draft: SettingsDraft? = null

    private var dialog: Dialog? = null

    private var closePrompt: Boolean = false

    private var ink: Int = 0
    private var muted: Int = 0
    private var background: Int = 0
    private var surface: Int = 0
    private var accent: Int = 0

    constructor(context: Context) {
        activity = HostActions.activity(context)
    }

    fun isShowing(): Boolean {
        return ((dialog != null) && (dialog)!!.isShowing())
    }

    private fun dp(value: Int): Int {
        return Math.round((value * (context)!!.getResources().getDisplayMetrics().density))
    }

    private fun label(text: String, size: Int, color: Int): TextView {
        var view: TextView = TextView(context)
        view.setText(text)
        view.setTextSize(size.toFloat())
        view.setTextColor(color)
        view.setIncludeFontPadding(false)
        return view
    }

    fun show() {
        var dark: Boolean = (((activity.getResources().getConfiguration().uiMode and Configuration.UI_MODE_NIGHT_MASK)) == Configuration.UI_MODE_NIGHT_YES)
        context = ContextThemeWrapper(activity, (if (dark) android.R.style.Theme_Material_NoActionBar else android.R.style.Theme_Material_Light_NoActionBar))
        ink = (if (dark) 0xffeeeeee.toInt() else 0xff25252a.toInt())
        muted = (if (dark) 0xffa0a0a5.toInt() else 0xff74747c.toInt())
        background = (if (dark) 0xff111111.toInt() else 0xfff4f4f7.toInt())
        surface = (if (dark) 0xff1f1f1f.toInt() else Color.WHITE)
        accent = (if (dark) 0xffed82a5.toInt() else 0xffba3c67.toInt())
        try {
            draft = SettingsDraft(HostSettings.read(activity))
        } catch (error: RuntimeException) {
            message(("无法读取设置：" + error.message))
            return
        }
        dialog = Dialog(context!!)
        (dialog)!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        (dialog)!!.setCancelable(false)
        (dialog)!!.setOnKeyListener(lambda@ { d, key, event -> if ((key != KeyEvent.KEYCODE_BACK)) {
                return@lambda false
            }
            if ((event.getAction() == KeyEvent.ACTION_UP)) {
                close()
            }
            return@lambda true })
        var root: LinearLayout = LinearLayout(context)
        root.setOrientation(LinearLayout.VERTICAL)
        root.setBackgroundColor(background)
        var toolbar: LinearLayout = LinearLayout(context)
        toolbar.setGravity(Gravity.CENTER_VERTICAL)
        toolbar.setBackgroundColor(surface)
        toolbar.setPadding(dp(4), 0, dp(8), 0)
        var back: TextView = label("‹", 28, ink)
        back.setGravity(Gravity.CENTER)
        back.setContentDescription("返回")
        back.setOnClickListener(lambda@ { v -> close() })
        toolbar.addView(back, LinearLayout.LayoutParams(dp(40), dp(56)))
        var title: TextView = label("简单bili小工具", 19, ink)
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        toolbar.addView(title, LinearLayout.LayoutParams(0, -2, 1F))
        var done: Button = Button(context)
        done.setText("完成")
        done.setOnClickListener(lambda@ { v -> close() })
        toolbar.addView(done)
        root.addView(toolbar)
        var scroll: ScrollView = ScrollView(context)
        scroll.setFillViewport(true)
        var body: LinearLayout = LinearLayout(context)
        body.setOrientation(LinearLayout.VERTICAL)
        body.setPadding(dp(16), dp(20), dp(16), dp(24))
        var card: LinearLayout = LinearLayout(context)
        card.setOrientation(LinearLayout.VERTICAL)
        var cardBackground: GradientDrawable = GradientDrawable()
        cardBackground.setColor(surface)
        cardBackground.setCornerRadius(dp(16).toFloat())
        card.setBackground(cardBackground)
        for (field in Config.FIELDS) {

            if ((card.getChildCount() > 0)) {
                var divider: View = View(context)
                divider.setBackgroundColor((if (dark) 0xff303030.toInt() else 0xffe9e9ee.toInt()))
                var line: LinearLayout.LayoutParams = LinearLayout.LayoutParams(-1, dp(1))
                run { line.rightMargin = dp(16); line.leftMargin = line.rightMargin; line.leftMargin }
                card.addView(divider, line)
            }
            var row: LinearLayout = LinearLayout(context)
            row.setGravity(Gravity.CENTER_VERTICAL)
            row.setMinimumHeight(dp(76))
            row.setPadding(dp(16), dp(12), dp(16), dp(12))
            var labels: LinearLayout = LinearLayout(context)
            labels.setOrientation(LinearLayout.VERTICAL)
            var name: TextView = label(field.label(), 16, ink)
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            labels.addView(name)
            var help: TextView = label(field.help(), 13, muted)
            var description: LinearLayout.LayoutParams = LinearLayout.LayoutParams(-1, -2)
            description.topMargin = dp(4)
            labels.addView(help, description)
            row.addView(labels, LinearLayout.LayoutParams(0, -2, 1F))
            var toggle: Switch = Switch(context)
            toggle.setContentDescription(field.label())
            toggle.setChecked((draft)!!.current().b(field.key()))
            toggle.setThumbTintList(android.content.res.ColorStateList.valueOf(accent))
            toggle.setOnCheckedChangeListener(lambda@ { button, checked -> (draft)!!.edit(field.key(), checked) })
            var switchParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(-2, -2)
            switchParams.leftMargin = dp(12)
            row.addView(toggle, switchParams)
            row.setOnClickListener(lambda@ { v -> toggle.setChecked(!toggle.isChecked()) })
            card.addView(row)
        }
        body.addView(card)
        var note: TextView = label("修改后点击“保存并重启”生效。", 13, muted)
        note.setPadding(dp(4), dp(16), dp(4), dp(16))
        body.addView(note)
        var save: Button = Button(context)
        save.setText("保存并重启")
        save.setOnClickListener(lambda@ { v -> save() })
        body.addView(save, LinearLayout.LayoutParams(-1, -2))
        var about: Button = Button(context)
        about.setText("关于")
        about.setOnClickListener(lambda@ { v -> About.show(context!!) })
        body.addView(about, LinearLayout.LayoutParams(-1, -2))
        var version: TextView = label((("版本 " + BuildConfig.VERSION_NAME) + " · 适配哔哩哔哩 8.95.0"), 12, muted)
        version.setPadding(dp(4), dp(16), dp(4), 0)
        body.addView(version)
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1F))
        var edge: Boolean = (draft)!!.committed().b("NAVIGATION_EDGE_TO_EDGE")
        root.setOnApplyWindowInsetsListener(lambda@ { view, insets -> var left: Int = 0
            var top: Int = 0
            var right: Int = 0
            var bottom: Int = 0
            if ((Build.VERSION.SDK_INT >= 30)) {
                var safe: android.graphics.Insets = insets.getInsets((WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()))
                left = safe.left
                top = safe.top
                right = safe.right
                bottom = safe.bottom
            } else {
                left = insets.getSystemWindowInsetLeft()
                top = insets.getSystemWindowInsetTop()
                right = insets.getSystemWindowInsetRight()
                bottom = insets.getSystemWindowInsetBottom()
            }
            root.setPadding(left, top, right, (if (edge) 0 else bottom))
            scroll.setPadding(0, 0, 0, (if (edge) bottom else 0))
            scroll.setClipToPadding(!edge)
            return@lambda insets })
        (dialog)!!.setContentView(root)
        var window: Window? = (dialog)!!.getWindow()
        if ((window != null)) {
            (window)!!.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(background))
            (window)!!.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            (window)!!.clearFlags((WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION))
            (window)!!.setStatusBarColor(Color.TRANSPARENT)
            (window)!!.setNavigationBarColor((if (edge) Color.TRANSPARENT else background))
            if ((Build.VERSION.SDK_INT >= 29)) {
                (window)!!.setStatusBarContrastEnforced(false)
                (window)!!.setNavigationBarContrastEnforced(false)
            }
            if ((Build.VERSION.SDK_INT >= 30)) {
                (window)!!.setDecorFitsSystemWindows(false)
                var params: WindowManager.LayoutParams = (window)!!.getAttributes()
                params.setFitInsetsTypes(0)
                (window)!!.setAttributes(params)
            } else {
                (window)!!.getDecorView().setSystemUiVisibility((((View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) or ((if (dark) 0 else (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)))))
            }
        }
        (dialog)!!.show()
        if ((Build.VERSION.SDK_INT >= 33)) {
            (dialog)!!.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::close)
        }
        if ((window != null)) {
            (window)!!.setLayout(-1, -1)
            if (((Build.VERSION.SDK_INT >= 30) && ((window)!!.getInsetsController() != null))) {
                (window)!!.getInsetsController()!!.setSystemBarsAppearance((if (dark) 0 else (WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)), (WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS))
            }
        }
        root.requestApplyInsets()
    }

    private fun close() {
        if (!(draft)!!.dirty()) {
            (dialog)!!.dismiss()
            return
        }
        if (closePrompt) {
            return
        }
        closePrompt = true
        var prompt: AlertDialog = AlertDialog.Builder(context).setTitle("设置尚未保存").setMessage("保存并重启哔哩哔哩后生效。").setPositiveButton("保存并重启", lambda@ { d, which -> save() }).setNegativeButton("放弃修改", lambda@ { d, which -> (draft)!!.discard()
            (dialog)!!.dismiss() }).setNeutralButton("继续编辑", null).create()
        prompt.setOnDismissListener(lambda@ { d -> closePrompt = false })
        prompt.show()
    }

    private fun save() {
        try {
            HostSettings.write(activity, (draft)!!.current())
            (draft)!!.saved()
            HostActions.restart(activity)
        } catch (error: RuntimeException) {
            message(error.message ?: error.javaClass.simpleName)
        }
    }

    private fun message(value: String) {
        Toast.makeText(activity, value, Toast.LENGTH_LONG).show()
    }
}
