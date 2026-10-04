package com.weinaoa.easybilitool;

import java.util.*;

/** The complete configuration of the simple module: three independent switches. */
public final class Config {
    public record Field(String key, String label, String help) {}
    public static final List<Field> FIELDS = List.of(
        new Field("IMMERSIVE_STATUS_BAR", "沉浸状态栏", "视频与直播延伸到顶部，控件避让"),
        new Field("HOME_HIDE_BOTTOM_BAR", "滑动时收起底栏", "主界面、动态详情与视频评论；上滑收起，下滑恢复"),
        new Field("NAVIGATION_EDGE_TO_EDGE", "沉浸导航栏", "背景延伸到小白条后方，按钮保留安全距离")
    );
    public final Map<String,Boolean> values;

    public Config() { this(Map.of()); }
    public Config(Map<String,Boolean> overrides) {
        Set<String> keys = new HashSet<>();
        for (Field field : FIELDS) keys.add(field.key());
        if (!keys.containsAll(overrides.keySet())) throw new IllegalArgumentException("未知设置项");
        Map<String,Boolean> result = new LinkedHashMap<>();
        for (Field field : FIELDS) {
            Boolean value = overrides.getOrDefault(field.key(), false);
            if (value == null) throw new IllegalArgumentException("设置值不能为空");
            result.put(field.key(), value);
        }
        values = Collections.unmodifiableMap(result);
    }
    public boolean b(String key) {
        Boolean value = values.get(key);
        if (value == null) throw new IllegalArgumentException("未知设置项：" + key);
        return value;
    }
    public Config with(String key, boolean value) {
        Map<String,Boolean> next = new LinkedHashMap<>(values);
        next.put(key, value);
        return new Config(next);
    }
}
