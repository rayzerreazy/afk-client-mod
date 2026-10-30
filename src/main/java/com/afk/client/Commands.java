package com.afk.client;

import com.afk.client.module.Module;
import com.afk.client.module.ModuleManager;
import com.afk.client.modules.SpawnerProtect;
import com.afk.client.setting.*;
import com.afk.client.util.AlarmPlayer;
import com.afk.client.util.ChatUtil;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import java.util.List;

/**
 * Chat commands:
 *   /afk                                   open the menu
 *   /afk set <module> <setting> <value>    change any setting, e.g. /afk set auto-list price 1.5M
 *   /afk whitelist add|remove <name>, list, clear      (Spawner Protect)
 *   /afk alarm [name]                      play the chosen alarm (or a named one) to hear it
 */
public final class Commands {
    private Commands() { }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("afk")
                .executes(ctx -> {
                    ModuleManager.openGuiRequested = true;
                    return 1;
                })
                .then(ClientCommandManager.literal("set")
                    .then(ClientCommandManager.argument("module", StringArgumentType.word())
                        .then(ClientCommandManager.argument("setting", StringArgumentType.word())
                            .then(ClientCommandManager.argument("value", StringArgumentType.greedyString())
                                .executes(ctx -> set(
                                    StringArgumentType.getString(ctx, "module"),
                                    StringArgumentType.getString(ctx, "setting"),
                                    StringArgumentType.getString(ctx, "value")))))))
                .then(ClientCommandManager.literal("whitelist")
                    .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                SpawnerProtect sp = sp();
                                if (sp == null) return 0;
                                if (sp.whitelistAdd(name)) {
                                    Config.save();
                                    ChatUtil.info(name + " is on the whitelist.");
                                } else {
                                    ChatUtil.warning(name + " is already on the whitelist.");
                                }
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                SpawnerProtect sp = sp();
                                if (sp == null) return 0;
                                if (sp.whitelistRemove(name)) {
                                    Config.save();
                                    ChatUtil.info(name + " is no longer on the whitelist.");
                                } else {
                                    ChatUtil.warning(name + " wasn't on the whitelist.");
                                }
                                return 1;
                            })))
                    .then(ClientCommandManager.literal("list")
                        .executes(ctx -> {
                            SpawnerProtect sp = sp();
                            if (sp == null) return 0;
                            List<String> names = sp.whitelistNames();
                            ChatUtil.info(names.isEmpty() ? "The whitelist is empty." : "Whitelist: " + String.join(", ", names));
                            return 1;
                        }))
                    .then(ClientCommandManager.literal("clear")
                        .executes(ctx -> {
                            SpawnerProtect sp = sp();
                            if (sp == null) return 0;
                            sp.whitelistClear();
                            Config.save();
                            ChatUtil.info("The whitelist is cleared.");
                            return 1;
                        })))
                .then(ClientCommandManager.literal("alarm")
                    .executes(ctx -> {
                        SpawnerProtect sp = sp();
                        if (sp != null) sp.previewAlarm();
                        return 1;
                    })
                    .then(ClientCommandManager.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            AlarmPlayer.play(name, 1.0f, 1);
                            ChatUtil.info("Alarms: " + String.join(", ", AlarmPlayer.NAMES));
                            return 1;
                        })))));
    }

    private static SpawnerProtect sp() {
        SpawnerProtect sp = ModuleManager.get(SpawnerProtect.class);
        if (sp == null) ChatUtil.error("Spawner Protect isn't loaded.");
        return sp;
    }

    /** /afk set <module> <setting> <value> */
    private static int set(String moduleName, String settingName, String value) {
        Module module = null;
        for (Module m : ModuleManager.MODULES) {
            if (m.name.equalsIgnoreCase(moduleName)) module = m;
        }
        if (module == null) {
            StringBuilder names = new StringBuilder();
            for (Module m : ModuleManager.MODULES) names.append(names.length() == 0 ? "" : ", ").append(m.name);
            ChatUtil.error("No module called " + moduleName + ". Modules: " + names);
            return 0;
        }

        Setting<?> setting = null;
        for (Setting<?> s : module.settings.all()) {
            if (s.name.equalsIgnoreCase(settingName)) setting = s;
        }
        if (setting == null) {
            StringBuilder names = new StringBuilder();
            for (Setting<?> s : module.settings.all()) {
                if (s instanceof KeybindSetting) continue;
                names.append(names.length() == 0 ? "" : ", ").append(s.name);
            }
            ChatUtil.error(module.name + " has no setting called " + settingName + ". Settings: " + names);
            return 0;
        }

        String text = value.trim();
        try {
            if (setting instanceof BoolSetting b) {
                b.set(text.equalsIgnoreCase("true") || text.equalsIgnoreCase("on") || text.equalsIgnoreCase("yes"));
            } else if (setting instanceof IntSetting i) {
                i.set((int) Math.round(Double.parseDouble(text)));
            } else if (setting instanceof DoubleSetting d) {
                d.set(Double.parseDouble(text));
            } else if (setting instanceof ModeSetting m) {
                m.set(text);
            } else if (setting instanceof StringSetting s) {
                s.set(text);
            } else {
                ChatUtil.error("That setting can't be changed with a command - use the menu.");
                return 0;
            }
        } catch (NumberFormatException e) {
            ChatUtil.error("\"" + text + "\" isn't a number.");
            return 0;
        }

        Config.save();
        ChatUtil.info(module.name + " " + setting.name + " = " + setting.get());
        return 1;
    }
}
