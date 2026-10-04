"""Package only the standalone module, build wrapper, tests and documentation."""
from pathlib import Path
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED
import hashlib
import json
import re
import shutil

root = Path(__file__).resolve().parent
dist = root / "dist"
dist.mkdir(exist_ok=True)
version = re.search(r"versionName\s+'([^']+)'", (root / "app/build.gradle").read_text(encoding="utf-8")).group(1)
metadata = json.loads((root / "app/build/outputs/apk/debug/output-metadata.json").read_text(encoding="utf-8"))
if metadata["elements"][0]["versionName"] != version:
    raise RuntimeError("Build the current version before packaging.")
apk = dist / f"easy-bili-tool-{version}.apk"
shutil.copy2(root / "app/build/outputs/apk/debug/app-debug.apk", apk)
source = dist / f"easy-bili-tool-{version}-source.zip"
fixed = [".gitignore", ".gitattributes", "README.md", "THIRD_PARTY_NOTICES.md", "build.gradle", "settings.gradle", "gradle.properties",
         "gradlew", "gradlew.bat", "test.ps1", "package_source.py", "app/build.gradle", "app/lint.xml"]
files = [root / name for name in fixed]
if (root / "LICENSE").is_file():
    files.append(root / "LICENSE")
for directory in ["app/src/main", "app/libs", "gradle/wrapper", "tools", "docs", "licenses"]:
    files.extend(path for path in (root / directory).rglob("*")
                 if path.is_file() and "__pycache__" not in path.parts)
files.extend(path for path in (root / "qa").iterdir() if path.suffix in {".md", ".json", ".png"})
files.extend(sorted((root / "tests").glob("*Test.java")))
with ZipFile(source, "w", ZIP_DEFLATED) as archive:
    for path in sorted(files):
        name = "easybilitool/" + path.relative_to(root).as_posix()
        if path.name == "gradlew":
            info = ZipInfo(name)
            info.create_system = 3
            info.external_attr = 0o100755 << 16
            info.compress_type = ZIP_DEFLATED
            archive.writestr(info, path.read_bytes())
        else:
            archive.write(path, name)
checksums = [hashlib.sha256(path.read_bytes()).hexdigest() + "  " + path.name for path in [apk, source]]
(dist / "SHA256SUMS.txt").write_text("\n".join(checksums) + "\n", encoding="utf-8")
print("\n".join(checksums))
