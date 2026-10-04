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

class ConfigDraftTest {
    companion object {
        private var checks: Int = 0

        @JvmStatic private fun check(value: Boolean, message: String) {
            checks++
            if (!value) {
                throw AssertionError(message)
            }
        }

        @JvmStatic fun main(args: Array<String>) {
            var defaults: Config = Config()
            check((defaults.values.size == 3), "only three options")
            for (field in Config.FIELDS) {
                check(!defaults.b(field.key()), (("fresh install keeps " + field.key()) + " off"))
            }
            run {
                var bits: Int = 0
                while ((bits < 8)) {
                    var config: Config = defaults
                    run {
                        var i: Int = 0
                        while ((i < 3)) {
                            config = config.with((Config.FIELDS.get(i))!!.key(), (((bits and ((1 shl i)))) != 0))
                            i++
                        }
                    }
                    var independent: Boolean = true
                    run {
                        var i: Int = 0
                        while ((i < 3)) {
                            independent = independent and (config.b((Config.FIELDS.get(i))!!.key()) == ((((bits and ((1 shl i)))) != 0)))
                            i++
                        }
                    }
                    check(independent, ("independent combination " + bits))
                    bits++
                }
            }
            check(defaults.values.values.stream().noneMatch({ it }), "edits do not mutate defaults")
            var draft: SettingsDraft = SettingsDraft(defaults)
            draft.edit("IMMERSIVE_STATUS_BAR", true)
            check((draft.dirty() && !draft.committed().b("IMMERSIVE_STATUS_BAR")), "edit remains a draft")
            check(!SettingsDraft(draft.committed()).current().b("IMMERSIVE_STATUS_BAR"), "reopening sees only saved config")
            draft.discard()
            check((!draft.dirty() && !draft.current().b("IMMERSIVE_STATUS_BAR")), "discard restores config")
            draft.edit("HOME_HIDE_BOTTOM_BAR", true)
            draft.edit("HOME_HIDE_BOTTOM_BAR", false)
            check(!draft.dirty(), "reverting to original is clean")
            draft.edit("NAVIGATION_EDGE_TO_EDGE", true)
            draft.saved()
            check((!draft.dirty() && draft.committed().b("NAVIGATION_EDGE_TO_EDGE")), "durable save commits draft")
            var before: Config = draft.current()
            try {
                draft.edit("unknown", true)
                throw AssertionError("Unknown option accepted")
            } catch (expected: IllegalArgumentException) {
                check((draft.current() == before), "invalid edit preserves draft")
            }
            var invalid: MutableMap<String, Boolean?> = HashMap()
            invalid.put("IMMERSIVE_STATUS_BAR", null)
            try {
                Config(invalid)
                throw AssertionError("Null accepted")
            } catch (expected: IllegalArgumentException) {
                check(true, "corrupt value rejected")
            }
            try {
                defaults.values.put("HOME_HIDE_BOTTOM_BAR", true)
                throw AssertionError("Mutable config")
            } catch (expected: UnsupportedOperationException) {
                check(true, "config is immutable")
            }
            check(((HomeBarSync.fraction(-50, 100) == .5F) && (HomeBarSync.opacity(.5F) == .5F)), "home offset and opacity match")
            check(((HomeBarSync.fraction(-200, 100) == 1F) && (HomeBarSync.fraction(50, 100) == 0F)), "home progress clamps")
            check((HomeBarSync.fraction(-100, 0) == 0F), "unmeasured app bar stays visible")
            System.out.println(("Config, draft and home progress tests passed: " + checks))
        }
    }
}
