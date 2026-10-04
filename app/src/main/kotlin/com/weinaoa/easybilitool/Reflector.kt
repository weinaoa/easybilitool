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

import java.lang.reflect.*

class Reflector {
    constructor() {

    }

    companion object {
        @JvmStatic @Throws(ReflectiveOperationException::class)
        fun findAndHookMethod(type: Class<*>, name: String, vararg signature: Any?): MethodHook.Unhook {
            val callback = signature.lastOrNull() as? MethodHook
            if ((((signature)!!.size == 0) || !(callback != null))) {
                throw IllegalArgumentException("Missing hook callback")
            }
            val parameters = Array((signature)!!.size - 1) { signature[it] as Class<*> }
            run {
                var i: Int = 0
                while ((i < parameters.size)) {
                    parameters[i] = ((signature)!![i] as Class<*>)
                    i++
                }
            }
            var method: Method = type.getDeclaredMethod(name, *parameters)
            method.setAccessible(true)
            return HookRuntime.hookMethod(method, callback)
        }

        @JvmStatic @Throws(NoSuchFieldException::class)
        fun findField(type: Class<*>, name: String): Field {
            run {
                var current: Class<*>? = type
                while ((current != null)) {
                    try {
                        var field: Field = current.getDeclaredField(name)
                        field.setAccessible(true)
                        return field
                    } catch (ignored: NoSuchFieldException) {

                    }
                    current = current.getSuperclass()
                }
            }
            throw NoSuchFieldException(((type.getName() + ".") + name))
        }

        @JvmStatic @Throws(ReflectiveOperationException::class)
        fun callMethod(target: Any?, name: String): Any? {
            run {
                var current: Class<*>? = (target)!!.javaClass
                while ((current != null)) {
                    try {
                        var method: Method = current.getDeclaredMethod(name)
                        method.setAccessible(true)
                        return method.invoke(target)
                    } catch (ignored: NoSuchMethodException) {

                    }
                    current = current.getSuperclass()
                }
            }
            throw NoSuchMethodException((((target)!!.javaClass.getName() + ".") + name))
        }
    }
}
