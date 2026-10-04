package com.weinaoa.easybilitool;

import android.app.Activity;
import android.content.*;

final class HostActions {
    static Activity activity(Context context) {
        for (int i = 0; i < 16 && context != null; i++) {
            if (context instanceof Activity activity) return activity;
            if (!(context instanceof ContextWrapper wrapper)) break;
            context = wrapper.getBaseContext();
        }
        throw new IllegalStateException("无法获取设置页面");
    }
    static void restart(Context context) {
        Activity activity = activity(context);
        Intent launch = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
        if (launch == null) throw new IllegalStateException("找不到哔哩哔哩启动入口");
        activity.finishAffinity();
        activity.startActivity(launch);
        System.exit(0);
    }
}
