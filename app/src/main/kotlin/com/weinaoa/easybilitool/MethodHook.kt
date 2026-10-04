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
import java.util.function.Consumer

/**
 * Local before/after convention backed exclusively by the modern interceptor chain.
 */
abstract class MethodHook {
    fun interface Unhook {
        fun unhook()
    }

    class MethodHookParam {
        lateinit var method: Executable

        @JvmField var thisObject: Any? = null

        lateinit var args: Array<Any?>

        @JvmField var result: Any? = null

        @JvmField var throwable: Throwable? = null

        @JvmField var returnEarly: Boolean = false

        constructor(chain: XposedInterface.Chain) {
            method = chain.getExecutable()
            thisObject = chain.getThisObject()
            args = chain.getArgs().toTypedArray()
        }

        fun getResult(): Any? {
            return result
        }

        fun hasThrowable(): Boolean {
            return (throwable != null)
        }

        fun getThrowable(): Throwable? {
            return throwable
        }

        fun setResult(value: Any?) {
            result = value
            throwable = null
            returnEarly = true
        }

        fun setThrowable(value: Throwable) {
            throwable = value
            result = null
            returnEarly = true
        }
    }

    @Throws(Throwable::class)
    protected open fun beforeHookedMethod(param: MethodHookParam) {

    }

    @Throws(Throwable::class)
    protected open fun afterHookedMethod(param: MethodHookParam) {

    }

    @Throws(Throwable::class)
    open fun intercept(chain: XposedInterface.Chain, logger: Consumer<Throwable>): Any? {
        var param: MethodHookParam = MethodHookParam(chain)
        var originalArgs: Array<Any?> = param.args.clone()
        try {
            beforeHookedMethod(param)
        } catch (e: Throwable) {
            logger.accept(e)
            param.args = originalArgs
            param.result = null
            param.throwable = null
            param.returnEarly = false
        }
        if (!param.returnEarly) {
            try {
                param.result = chain.proceed(param.args)
            } catch (e: Throwable) {
                param.throwable = e
            }
        }
        var result: Any? = param.result
        var throwable: Throwable? = param.throwable
        try {
            afterHookedMethod(param)
        } catch (e: Throwable) {
            logger.accept(e)
            param.result = result
            param.throwable = throwable
        }
        if ((param.throwable != null)) {
            throw param.throwable!!
        }
        return param.result
    }
}
