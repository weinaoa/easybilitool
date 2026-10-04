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
import java.lang.reflect.*
import java.util.*

class ModernHookTest {
    class Chain : XposedInterface.Chain {
        @JvmField var calls: Int = 0

        @JvmField var received: Array<out Any?>? = null

        @JvmField var failure: Throwable? = null

        override fun getExecutable(): Executable {
            try {
                return ModernHookTest::class.java.getMethod("original", Int::class.javaPrimitiveType!!)
            } catch (e: Exception) {
                throw AssertionError(e)
            }
        }

        override fun getThisObject(): Any? {
            return null
        }

        override fun getArgs(): MutableList<Any?> {
            return Collections.singletonList(3)
        }

        override fun getArg(index: Int): Any? {
            return getArgs().get(index)
        }

        @Throws(Throwable::class)
        override fun proceed(): Any? {
            return proceed(arrayOf(3))
        }

        @Throws(Throwable::class)
        override fun proceed(args: Array<out Any?>): Any? {
            calls++
            received = args
            if ((failure != null)) {
                throw failure!!
            }
            return original(((args)!![0] as Int))
        }

        @Throws(Throwable::class)
        override fun proceedWith(receiver: Any): Any? {
            return proceed()
        }

        @Throws(Throwable::class)
        override fun proceedWith(receiver: Any, args: Array<out Any?>): Any? {
            return proceed(args)
        }
    }

    open class Parent {
        private var code: Int = 17

        private fun inherited(): String {
            return "parent"
        }
    }

    class Host : Parent() {
        private fun own(): String {
            return "host"
        }
    }

    companion object {
        @JvmField var checks: Int = 0

        @JvmStatic fun check(condition: Boolean, message: String) {
            checks++
            if (!condition) {
                throw AssertionError(message)
            }
        }

        @JvmStatic fun original(x: Int): Int {
            return (x * 2)
        }

        @JvmStatic @Throws(Throwable::class)
        fun main(args: Array<String>) {
            var errors: MutableList<Throwable> = ArrayList()
            var chain: Chain = Chain()
            var result: Any? = object : MethodHook() {

            }.intercept(chain, errors::add)
            check(((result)!!.equals(6) && (chain.calls == 1)), "unchanged hook proceeds once")
            check(errors.isEmpty(), "no spurious failure")
            chain = Chain()
            result = object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) {
                    p.args[0] = 8
                }
                override fun afterHookedMethod(p: MethodHookParam) {
                    p.setResult(((p.getResult() as Int) + 1))
                }
            }.intercept(chain, errors::add)
            check((result)!!.equals(17), "changed args and after result")
            check(((chain.calls == 1) && (chain.received!![0])!!.equals(8)), "changed args forwarded to next chain")
            chain = Chain()
            result = object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) {
                    p.setResult(21)
                }
                override fun afterHookedMethod(p: MethodHookParam) {
                    p.setResult(((p.getResult() as Int) + 1))
                }
            }.intercept(chain, errors::add)
            check(((chain.calls == 0) && (result)!!.equals(22)), "short circuit still runs after callback")
            chain = Chain()
            result = object : MethodHook() {
                override fun beforeHookedMethod(p: MethodHookParam) {
                    p.args[0] = 9
                    p.setResult(99)
                    throw IllegalStateException()
                }
            }.intercept(chain, errors::add)
            check((((chain.calls == 1) && (chain.received!![0])!!.equals(3)) && (result)!!.equals(6)), "before failure restores native arguments and result")
            check((errors.size == 1), "before failure logged")
            chain = Chain()
            result = object : MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) {
                    p.setResult(99)
                    throw IllegalStateException()
                }
            }.intercept(chain, errors::add)
            check(((result)!!.equals(6) && (chain.calls == 1)), "after failure retains proceeded result")
            var nativeFailure: IllegalArgumentException = IllegalArgumentException("native")
            chain = Chain()
            chain.failure = nativeFailure
            try {
                object : MethodHook() {

                }.intercept(chain, errors::add)
                throw AssertionError("Native failure swallowed")
            } catch (e: IllegalArgumentException) {
                check((e == nativeFailure), "native exception preserved")
            }
            chain = Chain()
            chain.failure = nativeFailure
            result = object : MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) {
                    if (p.hasThrowable()) {
                        p.setResult(44)
                    }
                }
            }.intercept(chain, errors::add)
            check((result)!!.equals(44), "after callback may handle native failure")
            chain = Chain()
            chain.failure = nativeFailure
            try {
                object : MethodHook() {
                    override fun afterHookedMethod(p: MethodHookParam) {
                        p.setResult(99)
                        throw IllegalStateException()
                    }
                }.intercept(chain, errors::add)
                throw AssertionError()
            } catch (e: IllegalArgumentException) {
                check((e == nativeFailure), "after failure retains native throwable")
            }
            var host: Host = Host()
            check((Reflector.findField(Host::class.java, "code").getInt(host) == 17), "inherited private field")
            check(Reflector.callMethod(host, "inherited")!!.equals("parent"), "inherited no-argument method")
            check(Reflector.callMethod(host, "own")!!.equals("host"), "host no-argument method")
            try {
                Reflector.callMethod(host, "missing")
                throw AssertionError("Missing method accepted")
            } catch (expected: NoSuchMethodException) {
                check(true, "missing method fails safely")
            }
            System.out.println(("Modern API and reflection tests passed: " + checks))
        }
    }
}
