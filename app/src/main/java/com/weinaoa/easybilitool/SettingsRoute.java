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
