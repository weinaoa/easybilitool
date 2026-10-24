/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.app.*
import android.os.*

/** Small, local diagnostic report: configuration and hook status, never page content. */
internal object RuntimeStatus {
    @Volatile var active: Config? = null
        private set
    var framework = "未记录"
    private var host = "未初始化"
    private var applicationClass = "未初始化"
    private var activityClass = "尚未收到 Activity 回调"
    private val events = linkedMapOf<String, String>()

    fun configuration(config: Config) {
        active = config
        HookRuntime.log("[EasyBiliTool] CONFIG_ACTIVE ${config.values}")
    }
    fun attach(application: Application, config: Config) {
        applicationClass = application.javaClass.name
        val info = application.packageManager.getPackageInfo(application.packageName, 0)
        host = "${info.versionName} / ${if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode}"
        configuration(config)
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) { activityClass = a.javaClass.name }
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivityDestroyed(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
        })
    }
    @Synchronized fun record(message: String) {
        val entry = message.removePrefix("[EasyBiliTool] ")
        val key = entry.substringBefore(' ').substringBefore(':')
        val previous = events.put(key, entry.take(700))
        if ((key.startsWith("MAIN_NAV_") || key.startsWith("VIDEO_")) && previous != entry.take(700)) {
            android.util.Log.i("EasyBiliNav", entry.take(700))
        }
        if (events.size > 40) events.remove(events.keys.first())
    }
    @Synchronized fun report(saved: Config): String = buildString {
        appendLine("EasyBiliTool ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Host: $host")
        appendLine("Android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT} / ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Framework: $framework")
        appendLine("Application: $applicationClass")
        appendLine("Last resumed activity: $activityClass")
        appendLine("Active: ${active?.values ?: "未初始化"}")
        appendLine("Saved: ${saved.values}")
        events.values.forEach(::appendLine)
    }
}
