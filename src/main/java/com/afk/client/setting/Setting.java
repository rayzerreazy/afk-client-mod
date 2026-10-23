package com.afk.client.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/** One adjustable option of a module. */
public abstract class Setting<T> {
    public final String name;
    public final String description;
    public final T defaultValue;
    protected T value;
    private final BooleanSupplier visible;

    protected Setting(String name, String description, T defaultValue, BooleanSupplier visible) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.visible = visible;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        this.value = newValue;
    }

    public void reset() {
        set(defaultValue);
    }

    public boolean isVisible() {
        return visible == null || visible.getAsBoolean();
    }

    public abstract JsonElement save();

    public abstract void load(JsonElement element);
}
