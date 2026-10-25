package com.afk.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Plays the alarm sounds. Each alarm is a little pattern of vanilla sounds. */
public final class AlarmPlayer {
    public static final List<String> NAMES = List.of("Bell", "Siren", "Dragon", "Wither", "Anvil", "Ghast", "Chime", "None");

    private record Step(int delay, SoundEvent sound, float pitch) { }

    private record Pattern(int length, List<Step> steps) { }

    private record Cue(long at, SoundEvent sound, float pitch, float volume) { }

    private static final List<Cue> QUEUE = new ArrayList<>();
    private static long now;

    private AlarmPlayer() { }

    /** Play the named alarm. volume is 0 to 1, repeats is how many times the pattern is played. */
    public static void play(String name, float volume, int repeats) {
        if (volume <= 0 || repeats <= 0) return;
        Pattern pattern = pattern(name);
        if (pattern == null) return;

        for (int r = 0; r < repeats; r++) {
            for (Step step : pattern.steps()) {
                QUEUE.add(new Cue(now + (long) r * pattern.length() + step.delay(), step.sound(), step.pitch(), volume));
            }
        }
    }

    /** Called every client tick. */
    public static void tick() {
        now++;
        if (QUEUE.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        for (Iterator<Cue> it = QUEUE.iterator(); it.hasNext(); ) {
            Cue cue = it.next();
            if (cue.at() > now) continue;
            it.remove();
            try {
                mc.getSoundManager().play(PositionedSoundInstance.master(cue.sound(), cue.pitch(), cue.volume()));
            } catch (Throwable ignored) { }
        }
    }

    private static Pattern pattern(String name) {
        switch (name.toLowerCase()) {
            case "bell":
                return new Pattern(26, List.of(
                    new Step(0, SoundEvents.BLOCK_BELL_USE, 0.8f),
                    new Step(5, SoundEvents.BLOCK_BELL_USE, 1.0f),
                    new Step(10, SoundEvents.BLOCK_BELL_USE, 0.8f),
                    new Step(15, SoundEvents.BLOCK_BELL_USE, 1.0f)));
            case "siren":
                return new Pattern(40, List.of(
                    new Step(0, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.8f),
                    new Step(8, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.9f),
                    new Step(16, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.8f),
                    new Step(24, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.9f)));
            case "dragon":
                return new Pattern(60, List.of(
                    new Step(0, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0f),
                    new Step(28, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.8f)));
            case "wither":
                return new Pattern(70, List.of(new Step(0, SoundEvents.ENTITY_WITHER_SPAWN, 1.0f)));
            case "anvil":
                return new Pattern(34, List.of(
                    new Step(0, SoundEvents.BLOCK_ANVIL_LAND, 0.6f),
                    new Step(4, SoundEvents.BLOCK_ANVIL_LAND, 0.6f),
                    new Step(8, SoundEvents.BLOCK_ANVIL_LAND, 0.6f),
                    new Step(12, SoundEvents.BLOCK_ANVIL_LAND, 0.6f),
                    new Step(16, SoundEvents.BLOCK_ANVIL_LAND, 0.6f)));
            case "ghast":
                return new Pattern(50, List.of(
                    new Step(0, SoundEvents.ENTITY_GHAST_SCREAM, 1.0f),
                    new Step(22, SoundEvents.ENTITY_GHAST_SCREAM, 0.8f)));
            case "chime":
                return new Pattern(40, List.of(new Step(0, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f)));
            default:
                return null; // "None" or unknown
        }
    }
}
