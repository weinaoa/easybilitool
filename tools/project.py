# Copyright (c) 2026 weinaoa
# EasyBiliTool is licensed under Mulan PubL v2.
# You can use this software according to the terms and conditions of the Mulan PubL v2.
# You may obtain a copy of Mulan PubL v2 at:
#     http://license.coscl.org.cn/MulanPubL-2.0
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
# EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
# MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PubL v2 for more details.

"""Project metadata and local SDK discovery shared by development tools."""
import os
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def version():
    gradle = (ROOT / "app/build.gradle").read_text(encoding="utf-8")
    return (re.search(r"versionName\s+'([^']+)'", gradle).group(1),
            int(re.search(r"versionCode\s+(\d+)", gradle).group(1)))


def sdk():
    properties = ROOT / "local.properties"
    if properties.is_file():
        match = re.search(r"^sdk\.dir=(.+)$", properties.read_text(encoding="utf-8"), re.M)
        if match:
            return Path(match.group(1).strip().replace("\\:", ":").replace("\\\\", "\\"))
    location = os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME")
    if not location:
        raise RuntimeError("Set ANDROID_SDK_ROOT, ANDROID_HOME or sdk.dir in local.properties.")
    return Path(location)
