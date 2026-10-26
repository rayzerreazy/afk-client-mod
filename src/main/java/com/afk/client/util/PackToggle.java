package com.afk.client.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Turns one of the built-in resource packs (galaxy armor, galaxy sky) on or off.
 * The pack is copied into your resourcepacks folder, then switched on and the game reloads its resources.
 */
public final class PackToggle {
    private static final Deque<Runnable> PENDING = new ArrayDeque<>();

    private PackToggle() { }

    /**
     * Reloading resources while the menu is still being drawn can crash the game, so pack changes are queued
     * and done at the start of the next tick instead (ModuleManager calls tick()).
     */
    public static void enableLater(String fileName, String resourcePath, java.util.function.Consumer<Boolean> done) {
        PENDING.add(() -> {
            boolean ok = enable(fileName, resourcePath);
            if (done != null) done.accept(ok);
        });
    }

    public static void disableLater(String fileName) {
        PENDING.add(() -> disable(fileName));
    }

    /** Run one queued pack change (they are slow, so only one per tick). */
    public static void tick() {
        Runnable next = PENDING.poll();
        if (next != null) next.run();
    }

    public static boolean enable(String fileName, String resourcePath) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            Path dir = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);

            try (InputStream in = PackToggle.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    ChatUtil.error("The pack " + resourcePath + " is missing from the mod.");
                    return false;
                }
                try {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    if (!Files.exists(target)) throw e; // an older copy is already there (maybe in use) - use that
                }
            }

            ResourcePackManager manager = mc.getResourcePackManager();
            manager.scanPacks();

            String id = "file/" + fileName;
            if (manager.getProfile(id) == null) {
                ChatUtil.error("The game didn't find the pack " + fileName + " in the resourcepacks folder.");
                return false;
            }

            List<String> ids = new ArrayList<>(manager.getEnabledIds());
            if (!ids.contains(id)) {
                ids.add(id); // last = strongest
                manager.setEnabledProfiles(ids);
                mc.reloadResources();
            }
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            ChatUtil.error("Couldn't turn the pack on: " + t);
            return false;
        }
    }

    public static void disable(String fileName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            ResourcePackManager manager = mc.getResourcePackManager();
            String id = "file/" + fileName;
            List<String> ids = new ArrayList<>(manager.getEnabledIds());
            if (ids.remove(id)) {
                manager.setEnabledProfiles(ids);
                mc.reloadResources();
            }
        } catch (Throwable t) {
            t.printStackTrace();
            ChatUtil.error("Couldn't turn the pack off: " + t);
        }
    }
}
