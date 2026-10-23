package com.afk.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ChatUtil {
    private ChatUtil() { }

    public static void info(String message) {
        send(Formatting.GRAY, message);
    }

    public static void warning(String message) {
        send(Formatting.YELLOW, message);
    }

    public static void error(String message) {
        send(Formatting.RED, message);
    }

    private static void send(Formatting color, String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null) return;
        Text text = Text.literal("[AFK] ").formatted(Formatting.AQUA)
            .append(Text.literal(message).formatted(color));
        mc.inGameHud.getChatHud().addMessage(text);
    }
}
