package com.afk.client;

import com.afk.client.module.Module;
import com.afk.client.module.ModuleManager;
import com.afk.client.setting.Keybind;
import com.afk.client.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Saves your settings and key binds to config/afk-client.json. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Config() { }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("afk-client.json");
    }

    public static void load() {
        Path file = path();
        if (!Files.exists(file)) {
            save();
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();

            // (older configs saved the old default menu key - only trust it from version 2 on)
            if (root.has("version") && root.get("version").getAsInt() >= 2 && root.has("guiKey")) {
                ModuleManager.guiKey = root.get("guiKey").getAsInt();
            }

            JsonObject modules = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module module : ModuleManager.MODULES) {
                if (!modules.has(module.name)) continue;
                JsonObject m = modules.getAsJsonObject(module.name);

                if (m.has("bind")) {
                    int key = m.get("bind").getAsInt();
                    module.bind = key >= 0 ? Keybind.of(key) : Keybind.none();
                }

                JsonObject saved = m.has("settings") ? m.getAsJsonObject("settings") : new JsonObject();
                for (Setting<?> setting : module.settings.all()) {
                    if (saved.has(setting.name)) setting.load(saved.get(setting.name));
                }
            }
        } catch (Exception e) {
            System.err.println("[AFK Client] Couldn't read the config, using defaults: " + e);
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("version", 2);
            root.addProperty("guiKey", ModuleManager.guiKey);

            JsonObject modules = new JsonObject();
            for (Module module : ModuleManager.MODULES) {
                JsonObject m = new JsonObject();
                m.addProperty("bind", module.bind.key);

                JsonObject settings = new JsonObject();
                for (Setting<?> setting : module.settings.all()) {
                    JsonElement value = setting.save();
                    settings.add(setting.name, value);
                }
                m.add("settings", settings);
                modules.add(module.name, m);
            }
            root.add("modules", modules);

            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(root));
        } catch (Exception e) {
            System.err.println("[AFK Client] Couldn't save the config: " + e);
        }
    }
}
