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

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import java.lang.reflect.*
import java.lang.ref.WeakReference
import java.util.*

/**
 * Adds a separate entry without replacing host or other modules' preferences.
 */
class BiliSettingsHook {
    companion object {
        private val windows: MutableMap<Activity, WeakReference<InAppSettings>> = WeakHashMap()

        @JvmStatic private fun open(context: Context) {
            var activity: Activity = HostActions.activity(context)
            var reference: WeakReference<InAppSettings>? = windows.get(activity)
            var existing: InAppSettings? = (if ((reference == null)) null else (reference)!!.get())
            if (((existing != null) && (existing)!!.isShowing())) {
                return
            }
            var settings: InAppSettings = InAppSettings(activity)
            windows.put(activity, WeakReference(settings))
            settings.show()
        }

        @JvmStatic @Throws(Exception::class)
        fun install(application: Application, loader: ClassLoader) {
            installRoute(application, loader)
            var preference: Class<*> = Class.forName("androidx.preference.Preference", false, loader)
            var group: Class<*> = Class.forName("androidx.preference.PreferenceGroup", false, loader)
            var listenerSetter: Method? = null
            for (m in preference.getMethods()) {

                if ((m.getName().equals("setOnPreferenceClickListener") && (m.getParameterCount() == 1))) {
                    listenerSetter = m
                }
            }
            if ((listenerSetter == null)) {
                throw NoSuchMethodException("preference listener")
            }
            var setter: Method = listenerSetter
            var installed: Int = 0
            for (name in arrayOf("com.bilibili.app.preferences.BiliPreferencesActivity\$BiliPreferencesFragment", "com.bilibili.app.preferences.fragment.WideBiliPreferencesFragment")) {

                try {
                    var fragment: Class<*> = Class.forName(name, false, loader)
                    HookRuntime.hookMethod(fragment.getDeclaredMethod("onCreatePreferences", Bundle::class.java, String::class.java), object : MethodHook() {
                        override fun afterHookedMethod(p: MethodHookParam) {
                            if (p.hasThrowable()) {
                                return
                            }
                            try {
                                var screen: Any? = Reflector.callMethod(p.thisObject, "getPreferenceScreen")
                                if ((screen == null)) {
                                    return
                                }
                                if ((group.getMethod("findPreference", CharSequence::class.java).invoke(screen, "easy_bili_tool_entry") != null)) {
                                    return
                                }
                                var context: Context = (Reflector.callMethod(p.thisObject, "getContext") as Context)
                                if ((context == null)) {
                                    return
                                }
                                var entry: Any? = preference.getConstructor(Context::class.java).newInstance(context)
                                preference.getMethod("setKey", String::class.java).invoke(entry, "easy_bili_tool_entry")
                                preference.getMethod("setTitle", CharSequence::class.java).invoke(entry, "简单bili小工具")
                                preference.getMethod("setSummary", CharSequence::class.java).invoke(entry, "沉浸状态栏 · 滑动时收起底栏 · 沉浸导航栏")
                                preference.getMethod("setOrder", Int::class.javaPrimitiveType!!).invoke(entry, -100)
                                var listener: Class<*> = setter.getParameterTypes()[0]
                                setter.invoke(entry, Proxy.newProxyInstance(loader, arrayOf(listener), lambda@ { proxy, method, args -> if (method.getName().equals("onPreferenceClick")) {
                                        open(context)
                                        return@lambda true
                                    }
                                    if (method.getName().equals("hashCode")) {
                                        return@lambda System.identityHashCode(proxy)
                                    }
                                    if (method.getName().equals("equals")) {
                                        return@lambda (proxy === args[0])
                                    }
                                    if (method.getName().equals("toString")) {
                                        return@lambda "EasyBiliToolPreferenceClick"
                                    }
                                    return@lambda null }))
                                group.getMethod("addPreference", preference).invoke(screen, entry)
                                HookRuntime.log("[EasyBiliTool] SETTINGS_ENTRY_ADDED")
                            } catch (e: Throwable) {
                                HookRuntime.log(("[EasyBiliTool] SETTINGS_ENTRY_ERROR " + e))
                            }
                        }
                    })
                    installed++
                } catch (ignored: ClassNotFoundException) {

                }
            }
            if ((installed == 0)) {
                throw ClassNotFoundException("Bili preferences fragment")
            }
        }

        @JvmStatic @Throws(Exception::class)
        private fun installRoute(application: Application, loader: ClassLoader) {
            var methods: MutableSet<Method> = HashSet()
            for (name in arrayOf("tv.danmaku.bili.MainActivityV2", SettingsRoute.PREFERENCES)) {

                var type: Class<*> = Class.forName(name, false, loader)
                run {
                    var c: Class<*>? = type
                    while (((c != null) && Activity::class.java.isAssignableFrom(c))) {
                        for (method in c.getDeclaredMethods()) {

                            var parameters: Array<Class<*>> = method.getParameterTypes()
                            if ((((!method.getName().equals("onNewIntent") || (parameters.size < 1)) || (parameters[0] != Intent::class.java)) || !methods.add(method))) {
                                continue
                            }
                            HookRuntime.hookMethod(method, object : MethodHook() {
                                override fun afterHookedMethod(p: MethodHookParam) {
                                    var activity: Activity = (p.thisObject as Activity)
                                    var intent: Intent = (p.args[0] as Intent)
                                    if ((((intent != null) && SettingsRoute.ACTION.equals(intent.getAction())) && intent.getBooleanExtra(SettingsRoute.EXTRA, false))) {
                                        activity.setIntent(intent)
                                    }
                                }
                            })
                        }
                        c = c!!.getSuperclass()
                    }
                }
            }
            application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    if (!SettingsRoute.consume(activity.getIntent())) {
                        return
                    }
                    activity.getWindow().getDecorView().post(lambda@ { if ((activity.isFinishing() || activity.isDestroyed())) {
                            return@lambda
                        }
                        try {
                            if (SettingsRoute.PREFERENCES.equals(activity.javaClass.getName())) {
                                open(activity)
                            } else {
                                activity.startActivity(SettingsRoute.mark(Intent().setClassName(SettingsRoute.HOST, SettingsRoute.PREFERENCES)))
                            }
                            android.util.Log.i("EasyBiliTool", ("SETTINGS_ROUTE_OPENED " + activity.javaClass.getSimpleName()))
                        } catch (e: Throwable) {
                            HookRuntime.log(("[EasyBiliTool] SETTINGS_ROUTE_ERROR " + e.javaClass.getSimpleName()))
                        } })
                }
                override fun onActivityDestroyed(activity: Activity) {
                    windows.remove(activity)
                }
                override fun onActivityCreated(a: Activity, b: Bundle?) {

                }
                override fun onActivityStarted(a: Activity) {

                }
                override fun onActivityPaused(a: Activity) {

                }
                override fun onActivityStopped(a: Activity) {

                }
                override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {

                }
            })
        }
    }
}
