package com.afk.client;

import com.afk.client.module.ModuleManager;
import com.afk.client.modules.SpawnerProtect;
import com.afk.client.util.AlarmPlayer;
import com.afk.client.util.ChatUtil;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import java.util.List;

/**
 * Chat commands:
 *   /afk                          open the menu
 *   /afk whitelist add <name>     players who never set Spawner Protect off
 *   /afk whitelist remove <name>
 *   /afk whitelist list
 *   /afk whitelist clear
 *   /afk alarm [name]             play the chosen alarm (or a named one) to hear it
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
}
