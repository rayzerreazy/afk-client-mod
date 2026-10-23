package com.afk.client.setting;

import java.util.ArrayList;
import java.util.List;

/** All the settings groups of one module. */
public class Settings {
    private final List<SettingGroup> groups = new ArrayList<>();
    private final SettingGroup defaultGroup = new SettingGroup("General");

    public Settings() {
        groups.add(defaultGroup);
    }

    public SettingGroup getDefaultGroup() {
        return defaultGroup;
    }

    public SettingGroup createGroup(String name) {
        SettingGroup group = new SettingGroup(name);
        groups.add(group);
        return group;
    }

    public List<SettingGroup> groups() {
        return groups;
    }

    public List<Setting<?>> all() {
        List<Setting<?>> out = new ArrayList<>();
        for (SettingGroup g : groups) out.addAll(g.all());
        return out;
    }
}
