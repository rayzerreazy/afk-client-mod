package com.afk.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.BooleanSupplier;

/** A key that runs an action when pressed (only while the module is on). */
public class KeybindSetting extends Setting<Keybind> {
    private final Runnable action;

    private KeybindSetting(String name, String description, Keybind defaultValue, BooleanSupplier visible, Runnable action) {
        super(name, description, defaultValue, visible);
        this.action = action;
    }

    public void run() {
        if (action != null) action.run();
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value.key);
    }

    @Override
    public void load(JsonElement element) {
        try {
            if (element != null && element.isJsonPrimitive()) {
                int key = element.getAsInt();
                set(key >= 0 ? Keybind.of(key) : Keybind.none());
            }
        } catch (Exception ignored) { }
    }

    public static class Builder extends SettingBuilder<Builder, Keybind, KeybindSetting> {
        private Runnable action;

        public Builder action(Runnable action) {
            this.action = action;
            return this;
        }

        @Override
        public KeybindSetting build() {
            return new KeybindSetting(name, description, defaultValue == null ? Keybind.none() : defaultValue, visible, action);
        }
    }
}
