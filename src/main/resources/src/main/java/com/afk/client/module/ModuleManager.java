package com.afk.client.module;

import com.afk.client.modules.BoneSell;
import com.afk.client.modules.SpawnerProtect;
import com.afk.client.setting.KeybindSetting;
import com.afk.client.setting.Setting;
import com.afk.client.gui.ClickGui;
import com.afk.client.util.ChatUtil;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();

    /** Key that opens the GUI. */
    public static int guiKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private static final Map<Integer, Boolean> previous = new HashMap<>();

    private ModuleManager() { }

    public static void init() {
        MODULES.add(new SpawnerProtect());
        MODULES.add(new BoneSell());
    }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();

        // 1. Run the modules that are on.
        for (Module m : new ArrayList<>(MODULES)) {
            if (!m.isActive()) continue;
            try {
                m.onTick();
            } catch (Throwable t) {
                t.printStackTrace();
                ChatUtil.error(m.name + " crashed and was turned off: " + t);
                if (m.isActive()) m.toggle();
            }
        }

        // 2. Keys.
        if (mc.getWindow() == null) return;
        boolean canUseKeys = mc.currentScreen == null && mc.player != null;

        if (edge(guiKey) && canUseKeys) mc.setScreen(new ClickGui());

        for (Module m : new ArrayList<>(MODULES)) {
            if (m.bind.isSet() && edge(m.bind.key) && canUseKeys) m.toggle();

            for (Setting<?> s : m.settings.all()) {
                if (s instanceof KeybindSetting ks && ks.get().isSet() && edge(ks.get().key)
                    && canUseKeys && m.isActive()) {
                    ks.run();
                }
            }
        }
    }

    /** True once, on the tick a key goes down. */
    private static boolean edge(int key) {
        boolean now = isDown(key);
        boolean was = previous.getOrDefault(key, false);
        previous.put(key, now);
        return now && !was;
    }

    public static boolean isDown(int key) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null || key < 0) return false;
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }
}
