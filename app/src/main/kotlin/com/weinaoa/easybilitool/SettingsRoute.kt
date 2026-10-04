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
import android.content.Intent

/**
 * A launcher request consumed only by the module inside Bilibili.
 */
class SettingsRoute {
    constructor() {

    }

    companion object {
        @JvmField val HOST: String = "tv.danmaku.bili"

        @JvmField val ACTION: String = "com.weinaoa.easybilitool.OPEN_SETTINGS"

        @JvmField val EXTRA: String = "com.weinaoa.easybilitool.open_settings"

        @JvmField val PREFERENCES: String = "com.bilibili.app.preferences.BiliPreferencesActivity"

        @JvmStatic fun launch(context: Context): Intent? {
            var intent: Intent? = context.getPackageManager().getLaunchIntentForPackage(HOST)
            if ((intent == null)) {
                return null
            }
            return mark(intent).addFlags((Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }

        @JvmStatic fun mark(intent: Intent): Intent {
            return (intent)!!.setAction(ACTION).putExtra(EXTRA, true)
        }

        @JvmStatic fun consume(intent: Intent?): Boolean {
            if ((((intent == null) || !ACTION.equals((intent)!!.getAction())) || !(intent)!!.getBooleanExtra(EXTRA, false))) {
                return false
            }
            (intent)!!.removeExtra(EXTRA)
            return true
        }
    }
}
