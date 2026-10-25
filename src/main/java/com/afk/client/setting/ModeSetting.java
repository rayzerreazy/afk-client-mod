package com.afk.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.function.BooleanSupplier;

/** A setting where you pick one option from a list (click it in the menu to go to the next one). */
public class ModeSetting extends Setting<String> {
    public final List<String> options;
    private final Runnable onChange;

    private ModeSetting(String name, String description, String defaultValue, BooleanSupplier visible,
                        List<String> options, Runnable onChange) {
        super(name, description, defaultValue, visible);
        this.options = options;
        this.onChange = onChange;
    }

    @Override
    public void set(String newValue) {
        for (String option : options) {
            if (option.equalsIgnoreCase(newValue)) {
                value = option;
                return;
            }
        }
    }

    /** Go to the next option. */
    public void cycle() {
        int index = options.indexOf(value);
        value = options.get((index + 1) % options.size());
        if (onChange != null) onChange.run();
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

    public static class Builder extends SettingBuilder<Builder, String, ModeSetting> {
        private List<String> options = List.of("None");
        private Runnable onChange;

        public Builder options(List<String> options) {
            this.options = options;
            return this;
        }

        public Builder options(String... options) {
            this.options = List.of(options);
            return this;
        }

        /** Runs after the player clicks to change the option (handy for previewing a sound). */
        public Builder onChange(Runnable onChange) {
            this.onChange = onChange;
            return this;
        }

        @Override
        public ModeSetting build() {
            String def = defaultValue != null ? defaultValue : options.get(0);
            return new ModeSetting(name, description, def, visible, options, onChange);
        }
    }
}
