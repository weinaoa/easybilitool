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

import android.content.Context;
import android.content.SharedPreferences;
import java.util.LinkedHashMap;
import java.util.Map;

/** A separate private preference file in the host; no cross-app service or migration. */
final class HostSettings {
    private static final String FILE = "com.weinaoa.easybilitool.settings";
    private static SharedPreferences preferences(Context context) {
        // Bilibili's themed Activity can redirect preference storage. Both the
        // hook and editor must use the same ordinary host package context.
        try {
            Context host = context.createPackageContext(SettingsRoute.HOST, 0);
            return host.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        } catch (android.content.pm.PackageManager.NameNotFoundException error) {
            throw new IllegalStateException("无法打开哔哩哔哩配置", error);
        }
    }
    static Config read(Context context) {
        SharedPreferences preferences = preferences(context);
        Map<String,Boolean> values = new LinkedHashMap<>();
        for (Config.Field field : Config.FIELDS) values.put(field.key(), preferences.getBoolean(field.key(), false));
        return new Config(values);
    }
    static void write(Context context, Config config) {
        SharedPreferences.Editor edit = preferences(context).edit();
        for (Config.Field field : Config.FIELDS) edit.putBoolean(field.key(), config.b(field.key()));
        if (!edit.commit()) throw new IllegalStateException("设置保存失败，请重试");
    }
}
