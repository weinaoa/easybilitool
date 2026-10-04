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
