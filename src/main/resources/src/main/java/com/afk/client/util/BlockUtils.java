package com.afk.client.util;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** Breaking and placing blocks the way the normal game does it. */
public final class BlockUtils {
    private BlockUtils() { }

    /** Call every tick while you want to keep breaking the block. */
    public static boolean breakBlock(BlockPos pos, boolean swing) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerInteractionManager manager = mc.interactionManager;
        if (manager == null || mc.player == null || mc.world == null) return false;

        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir() || state.getHardness(mc.world, pos) < 0) return false;

        if (manager.isBreakingBlock()) manager.updateBlockBreakingProgress(pos, Direction.UP);
        else manager.attackBlock(pos, Direction.UP);

        if (swing) mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    /** Place the block from the given hotbar slot at pos, against a neighbouring block. */
    public static boolean place(BlockPos pos, FindItemResult item, boolean rotate, int priority) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!item.found() || mc.player == null || mc.world == null || mc.interactionManager == null) return false;
        if (!mc.world.getBlockState(pos).isReplaceable()) return false;

        for (Direction dir : Direction.values()) {
            BlockPos neighbour = pos.offset(dir);
            if (mc.world.getBlockState(neighbour).isReplaceable()) continue; // need something solid to place against

            Direction side = dir.getOpposite();
            Vec3d hit = Vec3d.ofCenter(neighbour).add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);

            InvUtils.swap(item.slot(), false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hit, side, neighbour, false));
            mc.player.swingHand(Hand.MAIN_HAND);
            return true;
        }
        return false;
    }
}
