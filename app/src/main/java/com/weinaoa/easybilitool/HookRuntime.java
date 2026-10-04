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

import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Executable;

final class HookRuntime {
    private static XposedInterface framework;
    static synchronized void initialize(XposedInterface api) {
        if (framework != null && framework != api) throw new IllegalStateException("Hook runtime already attached");
        framework = api;
    }
    static MethodHook.Unhook hookMethod(Executable method, MethodHook callback) {
        if (framework == null) throw new IllegalStateException("Framework not attached");
        XposedInterface.HookHandle handle = framework.hook(method)
            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
            .intercept(chain -> callback.intercept(chain, error -> framework.log(6, "EasyBiliTool", "Hook callback failed", error)));
        return handle::unhook;
    }
    static void log(String message) { framework.log(4, "EasyBiliTool", message); }
}
