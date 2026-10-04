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

import java.util.*;

public final class ConfigDraftTest {
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        Config defaults = new Config();
        check(defaults.values.size() == 3, "only three options");
        for (Config.Field field : Config.FIELDS) check(!defaults.b(field.key()), "fresh install keeps " + field.key() + " off");
        for (int bits = 0; bits < 8; bits++) {
            Config config = defaults;
            for (int i = 0; i < 3; i++) config = config.with(Config.FIELDS.get(i).key(), (bits & (1 << i)) != 0);
            boolean independent = true;
            for (int i = 0; i < 3; i++) independent &= config.b(Config.FIELDS.get(i).key()) == ((bits & (1 << i)) != 0);
            check(independent, "independent combination " + bits);
        }
        check(defaults.values.values().stream().noneMatch(Boolean::booleanValue), "edits do not mutate defaults");
        SettingsDraft draft = new SettingsDraft(defaults);
        draft.edit("IMMERSIVE_STATUS_BAR", true);
        check(draft.dirty() && !draft.committed().b("IMMERSIVE_STATUS_BAR"), "edit remains a draft");
        check(!new SettingsDraft(draft.committed()).current().b("IMMERSIVE_STATUS_BAR"), "reopening sees only saved config");
        draft.discard(); check(!draft.dirty() && !draft.current().b("IMMERSIVE_STATUS_BAR"), "discard restores config");
        draft.edit("HOME_HIDE_BOTTOM_BAR", true); draft.edit("HOME_HIDE_BOTTOM_BAR", false);
        check(!draft.dirty(), "reverting to original is clean");
        draft.edit("NAVIGATION_EDGE_TO_EDGE", true); draft.saved();
        check(!draft.dirty() && draft.committed().b("NAVIGATION_EDGE_TO_EDGE"), "durable save commits draft");
        Config before = draft.current();
        try { draft.edit("unknown", true); throw new AssertionError("Unknown option accepted"); }
        catch (IllegalArgumentException expected) { check(draft.current() == before, "invalid edit preserves draft"); }
        Map<String,Boolean> invalid = new HashMap<>(); invalid.put("IMMERSIVE_STATUS_BAR", null);
        try { new Config(invalid); throw new AssertionError("Null accepted"); }
        catch (IllegalArgumentException expected) { check(true, "corrupt value rejected"); }
        try { defaults.values.put("HOME_HIDE_BOTTOM_BAR", true); throw new AssertionError("Mutable config"); }
        catch (UnsupportedOperationException expected) { check(true, "config is immutable"); }
        check(HomeBarSync.fraction(-50, 100) == .5f && HomeBarSync.opacity(.5f) == .5f, "home offset and opacity match");
        check(HomeBarSync.fraction(-200, 100) == 1 && HomeBarSync.fraction(50, 100) == 0, "home progress clamps");
        check(HomeBarSync.fraction(-100, 0) == 0, "unmeasured app bar stays visible");
        System.out.println("Config, draft and home progress tests passed: " + checks);
    }
}
