package com.afk.client.gui;

import com.afk.client.AfkClient;
import com.afk.client.module.Module;
import com.afk.client.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** The on-screen display: a watermark and the list of modules that are on. */
public final class Hud {
    private Hud() { }

    public static void register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.MISC_OVERLAYS,
            Identifier.of("afk-client", "hud"),
            (context, tickCounter) -> render(context)
        );
    }

    private static void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        // Watermark (top left)
        String title = AfkClient.NAME;
        String version = "v" + AfkClient.VERSION;
        int w = font.getWidth(title) + font.getWidth(version) + 18;
        Galaxy.draw(context, 4, 4, 4 + w, 20);
        context.fill(4, 4, 6, 20, Theme.ACCENT);
        context.drawTextWithShadow(font, title, 10, 8, Theme.ACCENT);
        context.drawTextWithShadow(font, version, 14 + font.getWidth(title), 8, Theme.DIM);

        // Active modules (top right), longest first
        List<String> names = new ArrayList<>();
        for (Module m : ModuleManager.MODULES) if (m.isActive()) names.add(m.name);
        names.sort((a, b) -> font.getWidth(b) - font.getWidth(a));

        int screenW = mc.getWindow().getScaledWidth();
        int y = 4;
        for (String name : names) {
            int tw = font.getWidth(name);
            int x = screenW - tw - 10;
            Galaxy.draw(context, x - 4, y, screenW - 4, y + 12);
            context.fill(screenW - 6, y, screenW - 4, y + 12, Theme.ACCENT_2);
            context.drawTextWithShadow(font, name, x, y + 2, Theme.TEXT);
            y += 13;
        }
    }
}
