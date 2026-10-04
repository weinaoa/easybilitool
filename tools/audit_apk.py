"""Check the built APK's identity, scope, feature boundary and lint result."""
import collections
import hashlib
import json
import os
import re
import struct
import subprocess
from zipfile import ZipFile
import xml.etree.ElementTree as ET
from project import ROOT, sdk, version

name, code = version()
apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
build_tools = sdk() / "build-tools/36.0.0"
aapt = build_tools / ("aapt.exe" if os.name == "nt" else "aapt")
badging = subprocess.check_output([str(aapt), "dump", "badging", str(apk)]).decode("utf-8")
assert f"package: name='com.weinaoa.easybilitool' versionCode='{code}' versionName='{name}'" in badging
assert "application-label:'简单bili小工具'" in badging
manifest = subprocess.check_output([str(aapt), "dump", "xmltree", str(apk), "AndroidManifest.xml"]).decode("utf-8")
assert "E: uses-permission" not in manifest and "E: provider" not in manifest
sources = sorted(path.stem for path in (ROOT / "app/src/main/java/com/weinaoa/easybilitool").glob("*.java"))
strings, classes = [], []
with ZipFile(apk) as archive:
    entry = archive.read("META-INF/xposed/java_init.list").decode().strip()
    scope = archive.read("META-INF/xposed/scope.list").decode().splitlines()
    assert entry == "com.weinaoa.easybilitool.EasyBiliToolModule"
    assert scope == ["tv.danmaku.bili"]
    assert not any("pinyin" in path.lower() for path in archive.namelist())
    for path in archive.namelist():
        if not re.fullmatch(r"classes\d*\.dex", path):
            continue
        dex = archive.read(path)

        def u32(offset):
            return struct.unpack_from("<I", dex, offset)[0]

        dex_strings = []
        for index in range(u32(56)):
            offset = u32(u32(60) + 4 * index)
            while dex[offset] & 128:
                offset += 1
            offset += 1
            end = dex.index(0, offset)
            dex_strings.append(dex[offset:end].decode("utf-8", errors="replace"))
        types = [dex_strings[u32(u32(68) + 4 * i)] for i in range(u32(64))]
        classes.extend(types[u32(u32(100) + 32 * i)] for i in range(u32(96)))
        strings.extend(dex_strings)
assert not any(item.startswith("Lio/github/libxposed/") for item in classes)
assert not any("com/weinaoa/bilitool" in item or "com.weinaoa.bilitool" in item for item in strings)
for excluded in ("HOME_GLASS", "DANMAKU_AIRBORNE", "LIVE_MEDAL", "FULLSCREEN_GESTURE",
                 "REPORT_STATS", "APP_THEME", "pakku-pinyin", "ProtoAdapter", "LiquidGlass", "开源许可"):
    assert not any(excluded in item for item in strings), excluded
prefix = "Lcom/weinaoa/easybilitool/"
top = sorted({item.removeprefix(prefix).removesuffix(";").split("$")[0]
              for item in classes if item.startswith(prefix)})
assert set(sources).issubset(top)
runtime_prefixes = ("Lkotlin/", "Lorg/intellij/lang/annotations/", "Lorg/jetbrains/annotations/", "Lcom/android/tools/r8/")
assert not [item for item in classes if not item.startswith((prefix,) + runtime_prefixes)]
lint = collections.Counter(issue.get("severity")
                           for issue in ET.parse(ROOT / "app/build/reports/lint-results-debug.xml").getroot())
assert lint.get("Error", 0) == 0
subprocess.run(["java", "-jar", str(build_tools / "lib/apksigner.jar"), "verify", "--verbose", str(apk)], check=True)
audit = {
    "apk": f"easy-bili-tool-{name}.apk", "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
    "bytes": apk.stat().st_size, "package": "com.weinaoa.easybilitool", "label": "简单bili小工具",
    "version": name, "versionCode": code, "sourceClasses": sources, "dexTopLevelClasses": top,
    "permissions": [], "providers": [], "excludedFeatureAudit": "passed", "compileOnlyApiExcluded": True,
    "xposedEntry": entry, "scope": scope, "lintErrors": 0, "lintWarnings": lint.get("Warning", 0),
    "generatedOrRuntimeNamespaces": ["kotlin", "org.intellij.lang.annotations", "org.jetbrains.annotations", "com.android.tools.r8"],
}
(ROOT / "qa/apk-audit.json").write_text(json.dumps(audit, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({"version": name, "sha256": audit["sha256"], "sourceClasses": len(sources), "lint": dict(lint)}, indent=2))
