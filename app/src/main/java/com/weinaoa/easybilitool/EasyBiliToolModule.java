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
