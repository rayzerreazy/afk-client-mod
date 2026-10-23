package com.afk.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.BooleanSupplier;

public class BoolSetting extends Setting<Boolean> {
    private BoolSetting(String name, String description, boolean defaultValue, BooleanSupplier visible) {
        super(name, description, defaultValue, visible);
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement element) {
        try {
            if (element != null && element.isJsonPrimitive()) set(element.getAsBoolean());
        } catch (Exception ignored) { }
    }

    public static class Builder extends SettingBuilder<Builder, Boolean, BoolSetting> {
        @Override
        public BoolSetting build() {
            return new BoolSetting(name, description, defaultValue != null && defaultValue, visible);
        }
    }
}
