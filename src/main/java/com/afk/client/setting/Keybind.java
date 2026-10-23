package com.afk.client.setting;

import com.afk.client.util.Keys;

/** A keyboard key (GLFW key code), or none. */
public class Keybind {
    public final int key;

    private Keybind(int key) {
        this.key = key;
    }

    public static Keybind none() {
        return new Keybind(-1);
    }

    public static Keybind of(int key) {
        return new Keybind(key);
    }

    public boolean isSet() {
        return key >= 0;
    }

    @Override
    public String toString() {
        return isSet() ? Keys.name(key) : "None";
    }
}
