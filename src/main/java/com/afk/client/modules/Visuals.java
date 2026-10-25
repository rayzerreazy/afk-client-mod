package com.afk.client.modules;

import com.afk.client.module.Category;
import com.afk.client.module.Module;
import com.afk.client.setting.*;
import com.afk.client.util.PackToggle;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.List;
import java.util.Random;

/**
 * Visuals: galaxy-themed effects. A star aura around you, sparkles coming off every piece of armor you wear,
 * a star trail when you move, and (optionally) the galaxy armor textures.
 */
public class Visuals extends Module {
    private static final String ARMOR_PACK_FILE = "AFK-Galaxy-Armor.zip";
    private static final String ARMOR_PACK_RESOURCE = "/afk-packs/galaxy-armor.zip";

    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Boolean> armorTextures = sg.add(new BoolSetting.Builder()
        .name("galaxy-armor-textures").description("Give your armor (and everyone's) a galaxy look with a built-in texture pack. The game reloads for a few seconds when this changes.")
        .defaultValue(true).build());

    private final Setting<Boolean> aura = sg.add(new BoolSetting.Builder()
        .name("star-aura").description("A swirl of stars around you.")
        .defaultValue(true).build());

    private final Setting<Boolean> armorSparkle = sg.add(new BoolSetting.Builder()
        .name("armor-sparkle").description("Sparkles coming off each piece of armor you wear.")
        .defaultValue(true).build());

    private final Setting<Boolean> trail = sg.add(new BoolSetting.Builder()
        .name("star-trail").description("A trail of stars behind you while you move.")
        .defaultValue(true).build());

    private final Setting<String> style = sg.add(new ModeSetting.Builder()
        .name("style").description("Which stars to use.")
        .options("Galaxy", "Nebula", "Starlight").defaultValue("Galaxy").build());

    private final Setting<Integer> density = sg.add(new IntSetting.Builder()
        .name("density").description("How many stars. Higher looks richer but can cost a little FPS.")
        .defaultValue(3).min(1).max(10).sliderMax(10).build());

    private final Setting<Boolean> firstPerson = sg.add(new BoolSetting.Builder()
        .name("show-in-first-person").description("Also draw the effects when you're in first person (can get in the way of your view).")
        .defaultValue(false).build());

    private final Random random = new Random();
    private boolean armorPackOn;

    public Visuals() {
        super(Category.VISUALS, "visuals", "Galaxy effects: stars around you, sparkling armor and a star trail.");
    }

    @Override
    public void onActivate() {
        armorPackOn = false;
        syncArmorPack();
    }

    @Override
    public void onDeactivate() {
        if (armorPackOn) {
            PackToggle.disable(ARMOR_PACK_FILE);
            armorPackOn = false;
        }
    }

    @Override
    public void onTick() {
        syncArmorPack();

        if (mc.world == null || mc.player == null) return;
        if (!firstPerson.get() && mc.options.getPerspective().isFirstPerson()) return;

        int n = density.get();
        int age = mc.player.age;
        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();

        // star aura: a slow spiral going up around the body
        if (aura.get()) {
            for (int i = 0; i < n; i++) {
                double angle = age * 0.18 + i * (Math.PI * 2 / n);
                double height = ((age * 0.03 + i * 0.37) % 1.0) * 1.9;
                double radius = 0.75;
                spawn(px + Math.cos(angle) * radius, py + 0.1 + height, pz + Math.sin(angle) * radius,
                    0, 0.01, 0);
            }
        }

        // sparkles from each worn armor piece
        if (armorSparkle.get()) {
            sparkle(EquipmentSlot.HEAD, py + 1.65, px, pz);
            sparkle(EquipmentSlot.CHEST, py + 1.15, px, pz);
            sparkle(EquipmentSlot.LEGS, py + 0.7, px, pz);
            sparkle(EquipmentSlot.FEET, py + 0.15, px, pz);
        }

        // trail behind you while moving
        if (trail.get()) {
            double vx = mc.player.getVelocity().x;
            double vz = mc.player.getVelocity().z;
            if (Math.sqrt(vx * vx + vz * vz) > 0.08) {
                for (int i = 0; i < Math.max(1, n / 2); i++) {
                    spawn(px - vx * (1.5 + random.nextDouble() * 2) + (random.nextDouble() - 0.5) * 0.4,
                        py + 0.2 + random.nextDouble() * 1.4,
                        pz - vz * (1.5 + random.nextDouble() * 2) + (random.nextDouble() - 0.5) * 0.4,
                        0, 0.005, 0);
                }
            }
        }
    }

    private void sparkle(EquipmentSlot slot, double y, double px, double pz) {
        ItemStack stack = mc.player.getEquippedStack(slot);
        if (stack.isEmpty()) return;
        int count = Math.max(1, density.get() / 2);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r = 0.3 + random.nextDouble() * 0.15;
            spawn(px + Math.cos(angle) * r, y + (random.nextDouble() - 0.5) * 0.3, pz + Math.sin(angle) * r,
                0, 0.004, 0);
        }
    }

    private void spawn(double x, double y, double z, double vx, double vy, double vz) {
        try {
            mc.world.addParticleClient(pick(), x, y, z, vx, vy, vz);
        } catch (Throwable ignored) { }
    }

    /** Pick a star type for the chosen style. */
    private ParticleEffect pick() {
        List<ParticleEffect> pool;
        switch (style.get()) {
            case "Nebula":
                pool = List.of(ParticleTypes.PORTAL, ParticleTypes.WITCH, ParticleTypes.ENCHANT, ParticleTypes.WITCH);
                break;
            case "Starlight":
                pool = List.of(ParticleTypes.END_ROD, ParticleTypes.ENCHANT, ParticleTypes.ELECTRIC_SPARK);
                break;
            default:
                pool = List.of(ParticleTypes.PORTAL, ParticleTypes.END_ROD, ParticleTypes.ENCHANT, ParticleTypes.WITCH);
                break;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    /** Keeps the armor texture pack in step with the setting. */
    private void syncArmorPack() {
        if (armorTextures.get() && !armorPackOn) {
            info("Loading the galaxy armor textures... the game freezes for a few seconds.");
            armorPackOn = PackToggle.enable(ARMOR_PACK_FILE, ARMOR_PACK_RESOURCE);
        } else if (!armorTextures.get() && armorPackOn) {
            PackToggle.disable(ARMOR_PACK_FILE);
            armorPackOn = false;
        }
    }
}
