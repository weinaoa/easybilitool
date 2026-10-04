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

import java.util.*

/**
 * The complete configuration of the simple module: three independent switches.
 */
class Config {
    data class Field(val key: String, val label: String, val help: String) {
        fun key(): String = key

        fun label(): String = label

        fun help(): String = help
    }

    lateinit var values: MutableMap<String, Boolean>

    constructor() : this(mutableMapOf()) {

    }

    constructor(overrides: Map<String, Boolean?>) {
        var keys: MutableSet<String> = HashSet()
        for (field in FIELDS) {

            keys.add(field.key())
        }
        if (!keys.containsAll(overrides.keys)) {
            throw IllegalArgumentException("未知设置项")
        }
        var result: MutableMap<String, Boolean> = LinkedHashMap()
        for (field in FIELDS) {

            val value: Boolean? = overrides.getOrDefault(field.key(), false)
            if ((value == null)) {
                throw IllegalArgumentException("设置值不能为空")
            }
            result.put(field.key(), value)
        }
        values = Collections.unmodifiableMap(result)
    }

    fun b(key: String): Boolean {
        var value: Boolean? = values.get(key)
        if ((value == null)) {
            throw IllegalArgumentException(("未知设置项：" + key))
        }
        return value
    }

    fun with(key: String, value: Boolean): Config {
        var next: MutableMap<String, Boolean> = LinkedHashMap(values)
        next.put(key, value)
        return Config(next)
    }

    companion object {
        @JvmField val FIELDS: MutableList<Field> = mutableListOf(Field("IMMERSIVE_STATUS_BAR", "沉浸状态栏", "视频与直播延伸到顶部，控件避让"), Field("HOME_HIDE_BOTTOM_BAR", "滑动时收起底栏", "主界面、动态详情与视频评论；上滑收起，下滑恢复"), Field("NAVIGATION_EDGE_TO_EDGE", "沉浸导航栏", "背景延伸到小白条后方，按钮保留安全距离"))
    }
}
