package com.afk.client.modules;

import com.afk.client.module.Category;
import com.afk.client.module.Module;
import com.afk.client.setting.*;
import com.afk.client.util.AlarmPlayer;
import com.afk.client.util.InvUtils;
import com.afk.client.util.Prices;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Locale;

/**
 * Auto List: lists your items on the auction house one at a time, quickly.
 *
 * For each listing it takes ONE item out of the stack into an empty hotbar slot, holds it, and sends the list command
 * (for example /ah sell 1500). If the server asks you to confirm, it clicks the confirm button.
 *
 * Set it up with:  /afk set auto-list item diamond      and      /afk set auto-list price 1500
 */
public class AutoList extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<String> item = sg.add(new StringSetting.Builder()
        .name("item").description("Which item to list: part of its name, like diamond or elytra. Change it with /afk set auto-list item <name>.")
        .defaultValue("").build());

    private final Setting<String> price = sg.add(new StringSetting.Builder()
        .name("price").description("The price for each listing (you can write 1500, 1.5M, 10m ...). Change it with /afk set auto-list price <amount>.")
        .defaultValue("").build());

    private final Setting<String> listCommand = sg.add(new StringSetting.Builder()
        .name("list-command").description("The command that lists the item you're holding. {price} is replaced by the price number.")
        .defaultValue("ah sell {price}").build());

    private final Setting<Boolean> onlyOne = sg.add(new BoolSetting.Builder()
        .name("list-only-one").description("List a single item from the stack, not the whole stack. Needs one empty hotbar slot.")
        .defaultValue(true).build());

    private final Setting<Integer> delayTicks = sg.add(new IntSetting.Builder()
        .name("delay-ticks").description("Ticks between each step. Lower = faster (1 is the fastest, 20 ticks = 1 second). If the server ignores some listings, raise it.")
        .defaultValue(4).min(1).max(40).sliderMax(20).build());

    private final Setting<Integer> maxListings = sg.add(new IntSetting.Builder()
        .name("max-listings").description("Stop after this many listings (a safety limit). 0 = no limit.")
        .defaultValue(20).min(0).max(500).sliderMax(100).build());

    private final Setting<String> confirmText = sg.add(new StringSetting.Builder()
        .name("confirm-button-text").description("If the server shows a confirm menu, click the item whose name contains this. Leave empty to use the green glass pane.")
        .defaultValue("confirm").build());

    private final Setting<Boolean> debugMenu = sg.add(new BoolSetting.Builder()
        .name("debug-menu").description("Print the items of any menu the server opens while listing, to help set the confirm button text.")
        .defaultValue(false).build());

    private enum Step { FIND, SPLIT_PICKUP, SPLIT_PLACE, SPLIT_RETURN, SELECT, COMMAND, CONFIRM }

    private Step step = Step.FIND;
    private int timer, listed, source, target, failures, lastTotal, confirmWait;
    private boolean clickedConfirm;
    private double priceValue;

    public AutoList() {
        super(Category.AUTOMATION, "auto-list",
            "Lists your items on the auction house one at a time, fast.");
    }

    @Override
    public void onActivate() {
        step = Step.FIND;
        timer = 0;
        listed = 0;
        failures = 0;
        lastTotal = -1;
        confirmWait = 0;
        clickedConfirm = false;
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null && menuOpen()) mc.player.closeHandledScreen();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.getNetworkHandler() == null) return;

        if (timer > 0) {
            timer--;
            return;
        }
        if (menuOpen() && step != Step.CONFIRM) { // some other menu is open: pause
            timer = 5;
            return;
        }

        switch (step) {
            case FIND -> find();
            case SPLIT_PICKUP -> {
                click(slotId(source), 0, SlotActionType.PICKUP);
                step = Step.SPLIT_PLACE;
                timer = delayTicks.get();
            }
            case SPLIT_PLACE -> {
                click(slotId(target), 1, SlotActionType.PICKUP); // right click on an empty slot puts down exactly one
                step = Step.SPLIT_RETURN;
                timer = delayTicks.get();
            }
            case SPLIT_RETURN -> {
                click(slotId(source), 0, SlotActionType.PICKUP); // put the rest back
                step = Step.SELECT;
                timer = delayTicks.get();
            }
            case SELECT -> {
                InvUtils.swap(target, false);
                step = Step.COMMAND;
                timer = delayTicks.get();
            }
            case COMMAND -> command();
            case CONFIRM -> confirm();
        }
    }

    // ---------------------------------------------------------------- steps

    private void find() {
        priceValue = Prices.parse(price.get());
        if (priceValue <= 0) {
            error("Set a price first: /afk set auto-list price 1500");
            toggle();
            return;
        }
        if (item.get().trim().isEmpty()) {
            error("Set the item first: /afk set auto-list item diamond");
            toggle();
            return;
        }
        if (maxListings.get() > 0 && listed >= maxListings.get()) {
            finish("Reached the limit of " + maxListings.get() + " listings.");
            return;
        }

        // Did the last listing actually use up an item? If not several times in a row, something is wrong.
        int total = totalCount();
        if (lastTotal >= 0) {
            if (total >= lastTotal) {
                if (++failures >= 3) {
                    finish("The listings don't seem to go through (is the auction house full, or is the command wrong?). Stopping.");
                    return;
                }
            } else {
                failures = 0;
            }
        }
        lastTotal = total;

        int idx = findSource();
        if (idx < 0) {
            finish("No more " + item.get().trim() + " to list.");
            return;
        }
        ItemStack stack = mc.player.getInventory().getStack(idx);

        if (!onlyOne.get() || stack.getCount() == 1) {
            // Just get it into the hotbar.
            if (idx < 9) {
                target = idx;
            } else {
                target = emptyHotbarSlot() >= 0 ? emptyHotbarSlot() : 8;
                click(slotId(idx), target, SlotActionType.SWAP);
            }
            step = Step.SELECT;
            timer = delayTicks.get();
            return;
        }

        // A stack of several: split one off into an empty hotbar slot.
        int empty = emptyHotbarSlot();
        if (empty < 0) {
            error("Keep one hotbar slot empty so I can split a single item off.");
            toggle();
            return;
        }
        source = idx;
        target = empty;
        step = Step.SPLIT_PICKUP;
    }

    private void command() {
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty() || !matches(held) || (onlyOne.get() && held.getCount() != 1)) {
            warning("The item to list isn't in my hand - trying again.");
            step = Step.FIND;
            timer = delayTicks.get();
            return;
        }

        String cmd = listCommand.get().trim()
            .replace("{price}", String.valueOf((long) priceValue))
            .replace("{amount}", "1");
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        mc.getNetworkHandler().sendChatCommand(cmd);

        confirmWait = 0;
        clickedConfirm = false;
        step = Step.CONFIRM;
        timer = Math.max(1, delayTicks.get() / 2);
    }

    /** Some servers ask you to confirm the listing in a menu. If one shows up, click confirm. */
    private void confirm() {
        if (menuOpen()) {
            if (!clickedConfirm) {
                if (debugMenu.get()) logMenu();
                int slot = findConfirmSlot();
                if (slot < 0) {
                    if (++confirmWait > 20) {
                        warning("A menu opened but I couldn't find a confirm button - closing it.");
                        mc.player.closeHandledScreen();
                        listed++;
                        step = Step.FIND;
                        timer = delayTicks.get();
                    }
                    return;
                }
                click(slot, 0, SlotActionType.PICKUP);
                clickedConfirm = true;
                timer = delayTicks.get();
                return;
            }
            // clicked, but the menu is still open for a moment
            if (++confirmWait > 20) {
                mc.player.closeHandledScreen();
            }
            return;
        }

        // No menu (any more): the listing went straight through, or the confirm worked.
        if (clickedConfirm || ++confirmWait > 6) {
            listed++;
            step = Step.FIND;
            timer = delayTicks.get();
        }
    }

    private void finish(String message) {
        info(message + " Listed " + listed + ".");
        AlarmPlayer.play("Chime", 1.0f, 1);
        if (isActive()) toggle();
    }

    // ---------------------------------------------------------------- helpers

    private boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String wanted = item.get().trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (wanted.isEmpty()) return false;
        String id = Registries.ITEM.getId(stack.getItem()).getPath();
        return id.contains(wanted) || stack.getName().getString().toLowerCase(Locale.ROOT).replace(' ', '_').contains(wanted);
    }

    private int findSource() {
        for (int i = 0; i < 36; i++) {
            if (matches(mc.player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    private int totalCount() {
        int n = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (matches(s)) n += s.getCount();
        }
        return n;
    }

    private int emptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    /** Slot number in your inventory screen: hotbar 0-8 are 36-44, the rest keep their number. */
    private int slotId(int inventoryIndex) {
        return inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
    }

    private void click(int slotId, int button, SlotActionType type) {
        ScreenHandler handler = mc.player.currentScreenHandler;
        mc.interactionManager.clickSlot(handler.syncId, slotId, button, type, mc.player);
    }

    private int findConfirmSlot() {
        ScreenHandler handler = mc.player.currentScreenHandler;
        String text = confirmText.get().trim().toLowerCase(Locale.ROOT);

        if (!text.isEmpty()) {
            for (Slot slot : handler.slots) {
                if (slot.inventory == mc.player.getInventory()) continue;
                ItemStack stack = slot.getStack();
                if (!stack.isEmpty() && stack.getName().getString().toLowerCase(Locale.ROOT).contains(text)) return slot.id;
            }
        }
        // fall back to the green glass pane
        for (Slot slot : handler.slots) {
            if (slot.inventory == mc.player.getInventory()) continue;
            ItemStack stack = slot.getStack();
            if (stack.isOf(Items.LIME_STAINED_GLASS_PANE) || stack.isOf(Items.GREEN_STAINED_GLASS_PANE)) return slot.id;
        }
        return -1;
    }

    private void logMenu() {
        info("--- menu contents ---");
        for (Slot slot : mc.player.currentScreenHandler.slots) {
            if (slot.inventory == mc.player.getInventory()) continue;
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;
            info("slot " + slot.id + ": \"" + stack.getName().getString() + "\" [" + Registries.ITEM.getId(stack.getItem()).getPath() + "]");
        }
    }

    private boolean menuOpen() {
        return mc.player.currentScreenHandler != mc.player.playerScreenHandler;
    }
}
