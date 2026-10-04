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
