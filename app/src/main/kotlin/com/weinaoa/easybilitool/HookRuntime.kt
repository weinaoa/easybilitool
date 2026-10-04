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

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable

class HookRuntime {
    companion object {
        private var framework: XposedInterface? = null

        @JvmStatic @Synchronized fun initialize(api: XposedInterface) {
            if (((framework != null) && (framework !== api))) {
                throw IllegalStateException("Hook runtime already attached")
            }
            framework = api
        }

        @JvmStatic fun hookMethod(method: Executable, callback: MethodHook): MethodHook.Unhook {
            if ((framework == null)) {
                throw IllegalStateException("Framework not attached")
            }
            var handle: XposedInterface.HookHandle = (framework)!!.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(lambda@ { chain -> callback.intercept(chain, lambda@ { error -> (framework)!!.log(6, "EasyBiliTool", "Hook callback failed", error) }) })
            return MethodHook.Unhook { handle.unhook() }
        }

        @JvmStatic fun log(message: String) {
            (framework)!!.log(4, "EasyBiliTool", message)
        }
    }
}
