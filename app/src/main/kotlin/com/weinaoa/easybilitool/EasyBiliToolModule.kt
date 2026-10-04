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
import android.content.Context
import io.github.libxposed.api.XposedModule
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Installs only the settings entry and the three requested UI features.
 */
class EasyBiliToolModule : XposedModule() {
    private val installed: AtomicBoolean = AtomicBoolean()

    private var config: Config = Config()

    override fun onPackageReady(param: io.github.libxposed.api.XposedModuleInterface.PackageReadyParam) {
        if (!SettingsRoute.HOST.equals(param.getPackageName())) {
            return
        }
        HookRuntime.initialize(this)
        try {
            Reflector.findAndHookMethod(Application::class.java, "attach", Context::class.java, object : MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if ((param.hasThrowable() || !installed.compareAndSet(false, true))) {
                        return
                    }
                    var context: Context = (param.args[0] as Context)
                    var application: Application = (param.thisObject as Application)
                    try {
                        config = HostSettings.read(context)
                    } catch (error: Throwable) {
                        HookRuntime.log(("[EasyBiliTool] SETTINGS_READ_ERROR " + error))
                    }
                    install("SETTINGS_ENTRY", lambda@ { BiliSettingsHook.install(application, context.getClassLoader()) })
                    install("IMMERSIVE_STATUS", lambda@ { ImmersiveStatusBarHook(lambda@ { config }).install(application) })
                    install("HOME_BOTTOM_BAR", lambda@ { HomeBottomBarHook(lambda@ { config }).install(application) })
                    install("DETAIL_BOTTOM_BAR", lambda@ { DetailBottomBarHook(lambda@ { config }).install(application) })
                    install("NAVIGATION_EDGE", lambda@ { NavigationBarHook(lambda@ { config }).install(application) })
                }
            })
        } catch (error: ReflectiveOperationException) {
            HookRuntime.log(("[EasyBiliTool] APPLICATION_ATTACH_UNSUPPORTED " + error))
        }
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
}
