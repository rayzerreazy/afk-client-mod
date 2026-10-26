package com.afk.client.module;

import com.afk.client.modules.BoneSell;
import com.afk.client.modules.SkyBox;
import com.afk.client.modules.SpawnerProtect;
import com.afk.client.modules.Visuals;
import com.afk.client.util.AlarmPlayer;
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
    public static int guiKey = GLFW.GLFW_KEY_RIGHT_CONTROL;

    /** Set by the /afk command; the menu opens on the next tick (after the chat screen has closed). */
    public static volatile boolean openGuiRequested;

    private static boolean hinted;

    private static final Map<Integer, Boolean> previous = new HashMap<>();

    private ModuleManager() { }

    public static <T extends Module> T get(Class<T> type) {
        for (Module m : MODULES) {
            if (type.isInstance(m)) return type.cast(m);
        }
        return null;
    }

    public static void init() {
        MODULES.add(new SpawnerProtect());
        MODULES.add(new BoneSell());
        MODULES.add(new Visuals());
        MODULES.add(new SkyBox());
    }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();

        AlarmPlayer.tick();
        com.afk.client.util.PackToggle.tick();

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

        if (mc.player != null && !hinted) {
            hinted = true;
            ChatUtil.info("Press " + com.afk.client.util.Keys.name(guiKey) + " or type /afk to open the menu.");
        }

        if (openGuiRequested && canUseKeys) {
            openGuiRequested = false;
            openGui();
        }
        if (edge(guiKey) && canUseKeys) openGui();

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

    private static void openGui() {
        try {
            MinecraftClient.getInstance().setScreen(new ClickGui());
        } catch (Throwable t) {
            t.printStackTrace();
            ChatUtil.error("Couldn't open the menu: " + t);
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
