package com.afk.client;

import com.afk.client.gui.Hud;
import com.afk.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class AfkClient implements ClientModInitializer {
    public static final String NAME = "AFK Client";
    public static final String VERSION = "1.1";

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();

        // Every tick (also on menu screens, so the rejoin steps keep running).
        ClientTickEvents.START_CLIENT_TICK.register(client -> ModuleManager.tick());

        // The on-screen display (watermark + list of active modules).
        Hud.register();

        // /afk, /afk whitelist ..., /afk alarm
        Commands.register();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save());
    }
}
