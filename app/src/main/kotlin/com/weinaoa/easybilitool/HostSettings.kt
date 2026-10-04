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

import android.content.Context
import android.content.SharedPreferences

/**
 * A separate private preference file in the host; no cross-app service or migration.
 */
class HostSettings {
    companion object {
        private val FILE: String = "com.weinaoa.easybilitool.settings"

        @JvmStatic private fun preferences(context: Context): SharedPreferences {
            try {
                var host: Context = context.createPackageContext(SettingsRoute.HOST, 0)
                return host.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            } catch (error: android.content.pm.PackageManager.NameNotFoundException) {
                throw IllegalStateException("无法打开哔哩哔哩配置", error)
            }
        }

        @JvmStatic fun read(context: Context): Config {
            var preferences: SharedPreferences = preferences(context)
            var values: MutableMap<String, Boolean> = LinkedHashMap()
            for (field in Config.FIELDS) {

                values.put(field.key(), preferences.getBoolean(field.key(), false))
            }
            return Config(values)
        }

        @JvmStatic fun write(context: Context, config: Config) {
            var edit: SharedPreferences.Editor = preferences(context).edit()
            for (field in Config.FIELDS) {

                edit.putBoolean(field.key(), config.b(field.key()))
            }
            if (!edit.commit()) {
                throw IllegalStateException("设置保存失败，请重试")
            }
        }
    }
}
