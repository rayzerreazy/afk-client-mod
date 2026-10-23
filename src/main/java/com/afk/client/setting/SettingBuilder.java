package com.afk.client.setting;

import java.util.function.BooleanSupplier;

/** Shared builder methods (name, description, default value, visibility). */
public abstract class SettingBuilder<B extends SettingBuilder<B, T, S>, T, S extends Setting<T>> {
    protected String name = "";
    protected String description = "";
    protected T defaultValue;
    protected BooleanSupplier visible;

    @SuppressWarnings("unchecked")
    protected B self() {
        return (B) this;
    }

    public B name(String name) {
        this.name = name;
        return self();
    }

    public B description(String description) {
        this.description = description;
        return self();
    }

    public B defaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return self();
    }

    public B visible(BooleanSupplier visible) {
        this.visible = visible;
        return self();
    }

    public abstract S build();
}
