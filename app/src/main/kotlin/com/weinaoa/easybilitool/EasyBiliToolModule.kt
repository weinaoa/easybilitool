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

import android.app.Application
import io.github.libxposed.api.XposedModule
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Installs only the settings entry and the three requested UI features.
 */
class EasyBiliToolModule : XposedModule() {
    private val startupHooked = AtomicBoolean()
    private val installed = AtomicBoolean()
    private var config = Config()

    override fun onPackageReady(param: io.github.libxposed.api.XposedModuleInterface.PackageReadyParam) {
        if (param.packageName != SettingsRoute.HOST || !startupHooked.compareAndSet(false, true)) return
        HookRuntime.initialize(this)
        // Bootstrap from the fully attached application that actually receives
        // onCreate; an attach-only hook can be missed or observe a wrapper app.
        install("APPLICATION_STARTUP") {
            Reflector.findAndHookMethod(android.app.Instrumentation::class.java, "callApplicationOnCreate", Application::class.java, object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) { start(p.args[0] as Application) }
            })
            var type: Class<*>? = Class.forName(param.applicationInfo.className ?: Application::class.java.name, false, param.classLoader)
            while (type != null && Application::class.java.isAssignableFrom(type)) {
                val method = runCatching { type!!.getDeclaredMethod("onCreate") }.getOrNull()
                if (method != null) {
                    HookRuntime.hookMethod(method, object : MethodHook() {
                        override fun beforeHookedMethod(p: MethodHookParam) { start(p.thisObject as Application) }
                    })
                    break
                }
                type = type.superclass
            }
        }
    }

    private fun start(application: Application) {
        val context = application.baseContext ?: return
        if (context.packageName != SettingsRoute.HOST || !installed.compareAndSet(false, true)) return
        try {
            config = HostSettings.read(context)
        } catch (error: Throwable) {
            HookRuntime.log("[EasyBiliTool] SETTINGS_READ_ERROR $error")
        }
        RuntimeStatus.attach(application, config)
        val settings = java.util.function.Supplier { RuntimeStatus.active ?: config }
        install("SETTINGS_ENTRY") { BiliSettingsHook.install(application, context.classLoader) }
        installOptional("VIDEO_NATIVE_INSETS") { TheseusWindowInsetsHook(settings).install(application) }
        install("IMMERSIVE_STATUS") { ImmersiveStatusBarHook(settings).install(application) }
        install("HOME_BOTTOM_BAR") { HomeBottomBarHook(settings).install(application) }
        installOptional("COMPOSE_COMMENTS") { ComposeCommentHook(settings).install(application) }
        installOptional("COMPOSE_OPUS") { ComposeOpusHook(settings).install(application) }
        install("DETAIL_BOTTOM_BAR") { DetailBottomBarHook(settings).install(application) }
        installOptional("COMPOSE_HOME") {
            if (!ComposeHomeHook.install(application)) false
            else ComposeHomeScroll(settings).install(application)
        }
        install("NAVIGATION_EDGE") { NavigationBarHook(settings).install(application) }
    }

    private fun interface Installer {
        @Throws(Exception::class)
        fun run()
    }

    private fun install(name: String, installer: Installer) {
        try {
            installer.run()
            HookRuntime.log((("[EasyBiliTool] " + name) + "_READY"))
        } catch (error: Throwable) {
            HookRuntime.log(((("[EasyBiliTool] " + name) + "_UNSUPPORTED ") + error))
        }
    }

    private fun installOptional(name: String, installer: () -> Boolean) {
        try {
            HookRuntime.log("[EasyBiliTool] ${name}_${if (installer()) "READY" else "NOT_APPLICABLE"}")
        } catch (error: Throwable) {
            HookRuntime.log("[EasyBiliTool] ${name}_UNSUPPORTED $error")
        }
    }
}
