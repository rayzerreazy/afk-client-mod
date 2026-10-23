package com.afk.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.BooleanSupplier;

public class StringSetting extends Setting<String> {
    private StringSetting(String name, String description, String defaultValue, BooleanSupplier visible) {
        super(name, description, defaultValue, visible);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement element) {
        try {
            if (element != null && element.isJsonPrimitive()) set(element.getAsString());
        } catch (Exception ignored) { }
    }

    public static class Builder extends SettingBuilder<Builder, String, StringSetting> {
        @Override
        public StringSetting build() {
            return new StringSetting(name, description, defaultValue == null ? "" : defaultValue, visible);
        }
    }
}
