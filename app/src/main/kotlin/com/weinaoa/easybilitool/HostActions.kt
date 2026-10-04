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
import android.content.*

class HostActions {
    companion object {
        @JvmStatic fun activity(context: Context?): Activity {
            var context = context

            run {
                var i: Int = 0
                while (((i < 16) && (context != null))) {
                    val activity = context as? Activity
                    if (activity != null) {
                        return activity
                    }
                    val wrapper = context as? ContextWrapper
                    if (!(wrapper != null)) {
                        break
                    }
                    context = (wrapper)!!.getBaseContext()
                    i++
                }
            }
            throw IllegalStateException("无法获取设置页面")
        }

        @JvmStatic fun restart(context: Context) {
            var activity: Activity = activity(context)
            var launch: Intent? = (activity)!!.getPackageManager().getLaunchIntentForPackage((activity)!!.getPackageName())
            if ((launch == null)) {
                throw IllegalStateException("找不到哔哩哔哩启动入口")
            }
            (activity)!!.finishAffinity()
            (activity)!!.startActivity(launch)
            System.exit(0)
        }
    }
}
