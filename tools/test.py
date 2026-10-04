"""Run the module's JVM checks on Windows, Linux or macOS."""
import os
import subprocess
from project import ROOT, sdk

android_jar = sdk() / "platforms/android-36/android.jar"
if not android_jar.is_file():
    raise RuntimeError("Android SDK platform 36 is required.")
output = ROOT / "tests/build"
output.mkdir(parents=True, exist_ok=True)
source_root = ROOT / "app/src/main/java/com/weinaoa/easybilitool"
names = ("Config", "SettingsDraft", "HomeBarSync", "MainTabBarScroll", "DetailBarScroll",
         "MethodHook", "HookRuntime", "Reflector")
sources = [source_root / (name + ".java") for name in names]
sources += sorted((ROOT / "tests").glob("*Test.java"))
classpath = os.pathsep.join(map(str, (output, ROOT / "app/libs/libxposed-api-102.0.0.jar", android_jar)))
subprocess.run(["javac", "-encoding", "UTF-8", "-cp", classpath, "-d", str(output),
                *map(str, sources)], check=True)
for test in ("ConfigDraftTest", "MainTabBarScrollTest", "DetailBarScrollTest", "ModernHookTest"):
    subprocess.run(["java", "-cp", classpath, "com.weinaoa.easybilitool." + test], check=True)
