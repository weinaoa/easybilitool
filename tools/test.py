# Copyright (c) 2026 weinaoa
# EasyBiliTool is licensed under Mulan PubL v2.
# You can use this software according to the terms and conditions of the Mulan PubL v2.
# You may obtain a copy of Mulan PubL v2 at:
#     http://license.coscl.org.cn/MulanPubL-2.0
# THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
# EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
# MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
# See the Mulan PubL v2 for more details.

"""Run the module's JVM checks on Windows, Linux or macOS."""
import os
import subprocess
from project import ROOT

wrapper = ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
subprocess.run([str(wrapper), "--no-daemon", ":app:jvmChecks", "--console=plain"], cwd=ROOT, check=True)
