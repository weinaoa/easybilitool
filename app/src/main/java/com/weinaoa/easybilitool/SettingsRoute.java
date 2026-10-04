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
import android.content.Intent;

/** A launcher request consumed only by the module inside Bilibili. */
public final class SettingsRoute {
    public static final String HOST = "tv.danmaku.bili";
    public static final String ACTION = "com.weinaoa.easybilitool.OPEN_SETTINGS";
    public static final String EXTRA = "com.weinaoa.easybilitool.open_settings";
    public static final String PREFERENCES = "com.bilibili.app.preferences.BiliPreferencesActivity";

    private SettingsRoute() {}

    public static Intent launch(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(HOST);
        if (intent == null) return null;
        return mark(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    static Intent mark(Intent intent) {
        return intent.setAction(ACTION).putExtra(EXTRA, true);
    }

    static boolean consume(Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction()) || !intent.getBooleanExtra(EXTRA, false)) return false;
        intent.removeExtra(EXTRA);
        return true;
    }
}
