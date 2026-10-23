package com.afk.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.function.BooleanSupplier;

public class DoubleSetting extends Setting<Double> {
    public final double min, max, sliderMin, sliderMax;
    public final int decimals;

    private DoubleSetting(String name, String description, double defaultValue, BooleanSupplier visible,
                          double min, double max, double sliderMin, double sliderMax, int decimals) {
        super(name, description, defaultValue, visible);
        this.min = min;
        this.max = max;
        this.sliderMin = sliderMin;
        this.sliderMax = sliderMax;
        this.decimals = decimals;
    }

    @Override
    public void set(Double newValue) {
        value = Math.max(min, Math.min(max, newValue));
    }

    @Override
    public JsonElement save() {
        return new JsonPrimitive(value);
    }

    @Override
    public void load(JsonElement element) {
        try {
            if (element != null && element.isJsonPrimitive()) set(element.getAsDouble());
        } catch (Exception ignored) { }
    }

    public static class Builder extends SettingBuilder<Builder, Double, DoubleSetting> {
        private double min = -Double.MAX_VALUE;
        private double max = Double.MAX_VALUE;
        private Double sliderMin;
        private Double sliderMax;
        private int decimals = 2;

        /** Lets you write defaultValue(14) as well as defaultValue(0.5). */
        public Builder defaultValue(double value) {
            this.defaultValue = value;
            return this;
        }

        public Builder min(double min) {
            this.min = min;
            return this;
        }

        public Builder max(double max) {
            this.max = max;
            return this;
        }

        public Builder sliderMin(double sliderMin) {
            this.sliderMin = sliderMin;
            return this;
        }

        public Builder sliderMax(double sliderMax) {
            this.sliderMax = sliderMax;
            return this;
        }

        public Builder sliderRange(double sliderMin, double sliderMax) {
            this.sliderMin = sliderMin;
            this.sliderMax = sliderMax;
            return this;
        }

        public Builder decimalPlaces(int decimals) {
            this.decimals = decimals;
            return this;
        }

        @Override
        public DoubleSetting build() {
            double def = defaultValue == null ? 0 : defaultValue;
            double sMin = sliderMin != null ? sliderMin : (min != -Double.MAX_VALUE ? min : 0);
            double sMax = sliderMax != null ? sliderMax : (max != Double.MAX_VALUE ? max : Math.max(10, def * 2));
            return new DoubleSetting(name, description, def, visible, min, max, sMin, sMax, decimals);
        }
    }
}
