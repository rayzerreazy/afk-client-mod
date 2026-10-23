package com.afk.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.Predicate;

/** Small inventory helpers. */
public final class InvUtils {
    private InvUtils() { }

    public static FindItemResult findInHotbar(Predicate<ItemStack> predicate) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return new FindItemResult(-1, 0);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (predicate.test(stack)) return new FindItemResult(i, stack.getCount());
        }
        return new FindItemResult(-1, 0);
    }

    public static FindItemResult findInHotbar(Item... items) {
        return findInHotbar(stack -> {
            for (Item item : items) if (stack.isOf(item)) return true;
            return false;
        });
    }

    /** Select a hotbar slot (0-8). */
    public static boolean swap(int slot, boolean swapBack) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || slot < 0 || slot > 8) return false;
        mc.player.getInventory().setSelectedSlot(slot);
        return true;
    }

    public static Action shiftClick() {
        return new Action();
    }

    public static class Action {
        /** Shift-click a slot of the open menu. */
        public void slotId(int id) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.interactionManager == null) return;
            ScreenHandler handler = mc.player.currentScreenHandler;
            mc.interactionManager.clickSlot(handler.syncId, id, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }
}
