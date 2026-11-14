# Entry loaded from META-INF/xposed/java_init.list by LSPosed.
-keep,allowoptimization class com.weinaoa.easybilitool.EasyBiliToolModule {
    public <init>();
    public void onPackageReady(io.github.libxposed.api.XposedModuleInterface$PackageReadyParam);
}
# These reflection strings identify the host's AndroidX classes, not our copy.
# Keep only names; shrinking and optimization remain enabled.
-keep,allowshrinking,allowoptimization,allowaccessmodification class androidx.core.view.WindowInsetsCompat { *; }
-keep,allowshrinking,allowoptimization,allowaccessmodification class androidx.core.view.WindowInsetsCompat$Builder { *; }
-keep,allowshrinking,allowoptimization,allowaccessmodification class androidx.core.view.WindowInsetsCompat$Type { *; }
-keep,allowshrinking,allowoptimization,allowaccessmodification class androidx.core.graphics.Insets { *; }
