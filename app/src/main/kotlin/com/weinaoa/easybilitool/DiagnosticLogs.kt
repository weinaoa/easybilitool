/* Copyright (c) 2026 weinaoa. Licensed under Mulan PubL v2; see LICENSE. */
package com.weinaoa.easybilitool

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import java.io.File
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.WeakHashMap
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Local module events only: no page text, media identifiers, account data or system logcat. */
internal object DiagnosticLogs {
    private const val REQUEST = 0x5b7c
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "Module-diagnostics").apply { isDaemon = true } }
    private val queue = ArrayBlockingQueue<String>(256)
    private val dropped = AtomicInteger()
    private val errors = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val callbacks = WeakHashMap<Activity, (Int, Intent?) -> Unit>()
    private val hooked = mutableSetOf<Method>()
    @Volatile private var journal: DiagnosticJournal? = null
    @Volatile private var recording = true
    @Volatile private var failure = ""
    private var header: () -> String = { "" }

    fun attach(app: Application, flags: () -> String) {
        if (journal != null || (Build.VERSION.SDK_INT >= 28 && Application.getProcessName() != app.packageName)) return
        journal = DiagnosticJournal(File(app.filesDir, "easy-bili-diagnostics"))
        val host = app.packageManager.getPackageInfo(app.packageName, 0)
        header = { "Module ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nHost ${host.versionName}\n" +
            "Android ${Build.VERSION.RELEASE} API ${Build.VERSION.SDK_INT} ${Build.MANUFACTURER} ${Build.MODEL}\n${flags()}\n" }
        record("SESSION", "start pid=${android.os.Process.myPid()} module=${BuildConfig.VERSION_NAME} host=${host.versionName}")
        worker.scheduleWithFixedDelay({ flush() }, 250, 250, TimeUnit.MILLISECONDS)
    }

    fun record(category: String, detail: String) {
        if (!recording) return
        val entry = "${System.currentTimeMillis()} +${SystemClock.uptimeMillis()} ${category.take(48)} ${DiagnosticJournal.clean(detail)}"
        if (!queue.offer(entry)) dropped.incrementAndGet()
    }

    fun hook(message: String) {
        val clean = message.replace(Regex("^\\[[^]]+]\\s*"), "")
        val key = clean.substringBefore(' ').substringBefore(':')
        if (key.startsWith("CONFIG") || key.startsWith("SETTINGS_")) return
        if (key.endsWith("_READY") || key.contains("UNSUPPORTED") ||
            listOf("VIDEO_", "MAIN_NAV_", "PLAYER_", "GESTURE_", "IMMERSIVE_", "NAVIGATION_").any(key::startsWith))
            record(key, clean.substringAfter(' ', ""))
    }

    fun error(category: String, error: Throwable) {
        val now = SystemClock.uptimeMillis()
        val previous = errors.put(category, now) ?: -2000L
        if(now-previous >= 2000) record(category, error.javaClass.name + " " +
            error.stackTrace.take(6).joinToString(" <- ") { "${it.className}.${it.methodName}:${it.lineNumber}" })
    }

    private fun flush() {
        val target = journal ?: return
        try {
            val lost = dropped.getAndSet(0)
            if (lost > 0) target.append("${System.currentTimeMillis()} DROPPED count=$lost")
            repeat(256) { target.append(queue.poll() ?: return) }
        } catch (error: Exception) { failure = error.javaClass.simpleName }
    }

    private fun snapshot(callback: (String) -> Unit) {
        worker.execute {
            flush()
            val text = runCatching { header() + "Local chronological log; two files, 512 KiB maximum.\n" + journal?.snapshot().orEmpty() }
                .getOrElse { "Log read failed: ${it.javaClass.simpleName}" }
            main.post { callback(text) }
        }
    }

    fun show(activity: Activity) {
        val actions = arrayOf("标记刚才的问题", "导出日志文件", "复制最近日志", if(recording) "暂停本次记录" else "继续记录", "清空日志")
        AlertDialog.Builder(activity).setTitle("运行日志").setItems(actions) { _, index ->
                    when(index) {
                        0 -> { record("USER_MARK", "problem observed"); toast(activity, "已标记当前时间，可立即导出日志") }
                        1 -> snapshot { text -> if(!activity.isDestroyed && !activity.isFinishing) export(activity, text) }
                        2 -> snapshot { text ->
                            (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("运行日志", text.takeLast(48 * 1024)))
                            toast(activity, "已复制最近日志")
                        }
                        3 -> { recording = !recording; toast(activity, if(recording) "已继续记录" else "已暂停，重启后自动恢复") }
                        4 -> AlertDialog.Builder(activity).setMessage("清空本模块保留的运行日志？").setPositiveButton("清空") { _, _ ->
                            worker.execute {
                                val result = runCatching { queue.clear(); journal?.clear() }
                                main.post { toast(activity, if(result.isSuccess) "已清空日志" else "清空失败，请重试") }
                            }
                        }.setNegativeButton("取消", null).show()
                    }
                }.setNeutralButton("说明") { _, _ ->
                    AlertDialog.Builder(activity).setTitle("运行日志说明")
                        .setMessage("自动保留最近运行信息，最多 512 KB，重启后保留。仅记录模块、窗口和手势状态，不采集账号、页面文字或视频内容。" +
                            if(failure.isEmpty()) "" else "\n最近写入失败：$failure")
                        .setPositiveButton("关闭", null).show()
                }.setNegativeButton("关闭", null).show()
    }

    private fun export(activity: Activity, text: String) {
        try {
            var owner: Class<*>? = activity.javaClass
            var method: Method? = null
            while(owner != null && method == null) {
                method = runCatching { owner!!.getDeclaredMethod("onActivityResult", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Intent::class.java) }.getOrNull()
                owner = owner.superclass
            }
            val target = method ?: error("Activity result callback unavailable")
            if(hooked.add(target)) HookRuntime.hookMethod(target, object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) {
                    if(p.args[0] != REQUEST) return
                    val callback = callbacks.remove(p.thisObject as Activity) ?: return
                    p.setResult(null)
                    callback(p.args[1] as Int, p.args[2] as? Intent)
                }
            })
            callbacks[activity] = { result, data ->
                if(result == Activity.RESULT_OK && data?.data != null) {
                    val resolver = activity.contentResolver; val uri = data.data!!
                    worker.execute {
                        val success = runCatching { resolver.openOutputStream(uri, "wt").use { stream ->
                            checkNotNull(stream) { "Output unavailable" }.write(text.toByteArray(Charsets.UTF_8))
                        } }
                        main.post { toast(activity, if(success.isSuccess) "日志已导出" else "导出失败：${success.exceptionOrNull()?.javaClass?.simpleName}") }
                    }
                }
            }
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
            activity.startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain")
                .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "module-log-$stamp.txt"), REQUEST)
        } catch (error: Exception) { callbacks.remove(activity); toast(activity, "无法导出：${error.javaClass.simpleName}") }
    }

    fun destroyed(activity: Activity) { callbacks.remove(activity) }
    private fun toast(context: Context, text: String) { Toast.makeText(context, text, Toast.LENGTH_LONG).show() }
}
