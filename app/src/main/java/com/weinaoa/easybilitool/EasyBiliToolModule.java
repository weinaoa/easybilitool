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

import android.app.Application;
import android.content.Context;
import io.github.libxposed.api.XposedModule;
import java.util.concurrent.atomic.AtomicBoolean;

/** Installs only the settings entry and the three requested UI features. */
public final class EasyBiliToolModule extends XposedModule {
    private final AtomicBoolean installed = new AtomicBoolean();
    private Config config = new Config();

    @Override public void onPackageReady(PackageReadyParam param) {
        if (!SettingsRoute.HOST.equals(param.getPackageName())) return;
        HookRuntime.initialize(this);
        try {
            Reflector.findAndHookMethod(Application.class, "attach", Context.class, new MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (param.hasThrowable() || !installed.compareAndSet(false, true)) return;
                    Context context = (Context)param.args[0];
                    Application application = (Application)param.thisObject;
                    try { config = HostSettings.read(context); }
                    catch (Throwable error) { HookRuntime.log("[EasyBiliTool] SETTINGS_READ_ERROR " + error); }
                    install("SETTINGS_ENTRY", () -> BiliSettingsHook.install(application, context.getClassLoader()));
                    install("IMMERSIVE_STATUS", () -> new ImmersiveStatusBarHook(() -> config).install(application));
                    install("HOME_BOTTOM_BAR", () -> new HomeBottomBarHook(() -> config).install(application));
                    install("DETAIL_BOTTOM_BAR", () -> new DetailBottomBarHook(() -> config).install(application));
                    install("NAVIGATION_EDGE", () -> new NavigationBarHook(() -> config).install(application));
                }
            });
        } catch (ReflectiveOperationException error) { HookRuntime.log("[EasyBiliTool] APPLICATION_ATTACH_UNSUPPORTED " + error); }
    }
    private interface Installer { void run() throws Exception; }
    private void install(String name, Installer installer) {
        try { installer.run(); HookRuntime.log("[EasyBiliTool] " + name + "_READY"); }
        catch (Throwable error) { HookRuntime.log("[EasyBiliTool] " + name + "_UNSUPPORTED " + error); }
    }
}
