package com.afk.client.module;

import com.afk.client.setting.Keybind;
import com.afk.client.setting.Settings;
import com.afk.client.util.ChatUtil;
import net.minecraft.client.MinecraftClient;

/** Base class of every module. */
public abstract class Module {
    protected final MinecraftClient mc = MinecraftClient.getInstance();

    public final Category category;
    public final String name;
    public final String description;
    public final Settings settings = new Settings();

    /** Key that turns the module on/off (set in the GUI). */
    public Keybind bind = Keybind.none();

    private boolean active;

    protected Module(Category category, String name, String description) {
        this.category = category;
        this.name = name;
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void toggle() {
        if (active) {
            active = false;
            try {
                onDeactivate();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } else {
            active = true;
            try {
                onActivate();
            } catch (Throwable t) {
                t.printStackTrace();
                active = false;
            }
        }
    }

    public void onActivate() { }

    public void onDeactivate() { }

    /** Called every client tick while the module is on (also on menu screens, so rejoining works). */
    public void onTick() { }

    protected void info(String message) {
        ChatUtil.info(message);
    }

    protected void warning(String message) {
        ChatUtil.warning(message);
    }

    protected void error(String message) {
        ChatUtil.error(message);
    }
}
