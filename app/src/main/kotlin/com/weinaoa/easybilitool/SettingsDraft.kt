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



/**
 * An editing session that writes nothing until Save succeeds.
 */
class SettingsDraft {
    private lateinit var committed: Config
    private lateinit var current: Config

    constructor(config: Config) {
        run { current = config; committed = current; committed }
    }

    fun committed(): Config {
        return committed
    }

    fun current(): Config {
        return current
    }

    fun dirty(): Boolean {
        return !current.values.equals(committed.values)
    }

    fun edit(key: String, value: Boolean) {
        current = current.with(key, value)
    }

    fun discard() {
        current = committed
    }

    fun saved() {
        committed = current
    }
}
