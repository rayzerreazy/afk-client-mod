package com.afk.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Angles from your eyes to a block. */
public final class Rotations {
    private Rotations() { }

    public static double getYaw(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d eye = mc.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        double dx = center.x - eye.x;
        double dz = center.z - eye.z;
        return MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }

    public static double getPitch(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d eye = mc.player.getEyePos();
        Vec3d center = Vec3d.ofCenter(pos);
        double dx = center.x - eye.x;
        double dy = center.y - eye.y;
        double dz = center.z - eye.z;
        return -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    }
}
