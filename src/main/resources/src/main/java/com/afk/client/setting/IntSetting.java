package com.afk.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.BooleanSupplier;

public class IntSetting extends Setting<Integer> {
    public final int min, max, sliderMin, sliderMax;

    private IntSetting(String name, String description, int defaultValue, BooleanSupplier visible,
                       int min, int max, int sliderMin, int sliderMax) {
        super(name, description, defaultValue, visible);
        this.min = min;
        this.max = max;
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;
    }

    @Override
    public void set(Integer newValue) {
        value = Math.max(min, Math.min(max, newValue));
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement element) {
        try {
            if (element != null && element.isJsonPrimitive()) set(element.getAsInt());
        } catch (Exception ignored) { }
    }

    public static class Builder extends SettingBuilder<Builder, Integer, IntSetting> {
        private int min = Integer.MIN_VALUE;
        private int max = Integer.MAX_VALUE;
        private Integer sliderMin;
        private Integer sliderMax;

        public Builder min(int min) {
            this.min = min;
            return this;
        }

        public Builder max(int max) {
            this.max = max;
            return this;
        }

        public Builder sliderMin(int sliderMin) {
            this.sliderMin = sliderMin;
            return this;
        }

        public Builder sliderMax(int sliderMax) {
            this.sliderMax = sliderMax;
            return this;
        }

        public Builder sliderRange(int sliderMin, int sliderMax) {
            this.sliderMin = sliderMin;
            this.sliderMax = sliderMax;
            return this;
        }

        @Override
        public IntSetting build() {
            int def = defaultValue == null ? 0 : defaultValue;
            int sMin = sliderMin != null ? sliderMin : (min != Integer.MIN_VALUE ? min : 0);
            int sMax = sliderMax != null ? sliderMax : (max != Integer.MAX_VALUE ? max : Math.max(10, def * 2));
            return new IntSetting(name, description, def, visible, min, max, sMin, sMax);
        }
    }
}
