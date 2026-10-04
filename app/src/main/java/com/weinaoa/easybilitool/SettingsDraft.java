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

/** An editing session that writes nothing until Save succeeds. */
public final class SettingsDraft {
    private Config committed, current;
    public SettingsDraft(Config config) { committed = current = config; }
    public Config committed() { return committed; }
    public Config current() { return current; }
    public boolean dirty() { return !current.values.equals(committed.values); }
    public void edit(String key, boolean value) { current = current.with(key, value); }
    public void discard() { current = committed; }
    public void saved() { committed = current; }
}
