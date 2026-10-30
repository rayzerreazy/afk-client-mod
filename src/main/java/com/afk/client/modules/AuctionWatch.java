package com.afk.client.modules;

import com.afk.client.module.Category;
import com.afk.client.module.Module;
import com.afk.client.setting.*;
import com.afk.client.util.AlarmPlayer;
import com.afk.client.util.Prices;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AH Watch: turn it on and it starts. It goes through a built-in list of easy-to-craft items that don't matter much
 * on DonutSMP, types /ah <item> for each one, reads the listings, and plays a ding when an item has a listing
 * priced far above what it should be worth (100M or more by default). Then it closes the menu and checks the next item.
 *
 * It only looks and notifies - it never buys or lists anything.
 */
public class AuctionWatch extends Module {
    /** Common, easy-to-craft items that aren't a big deal in the economy. Edit this list to taste. */
    private static final String[] DEFAULT_ITEMS = {
        "end rod", "lantern", "soul lantern", "sea lantern", "campfire", "soul campfire", "composter", "barrel",
        "lectern", "loom", "smoker", "blast furnace", "stonecutter", "grindstone", "cartography table",
        "fletching table", "bamboo mosaic", "chiseled bookshelf", "decorated pot", "jukebox", "note block",
        "scaffolding", "item frame", "glow item frame", "painting", "armor stand", "flower pot", "iron trapdoor",
        "chain", "prismarine bricks", "dark prismarine", "purpur pillar", "end stone bricks", "polished blackstone bricks",
        "mud bricks", "packed mud", "cut copper", "lightning rod", "spyglass", "crafter", "dried kelp block", "hay block",
        "honey block", "slime block", "target", "tinted glass", "beehive", "candle", "bell", "daylight detector",
        "observer", "dropper", "dispenser", "hopper", "piston", "sticky piston", "redstone lamp", "tripwire hook",
        "lever", "comparator", "repeater", "rail", "detector rail", "activator rail", "cauldron", "brewing stand",
        "enchanting table", "anvil", "loom", "bookshelf", "ladder", "bone block", "terracotta", "glazed terracotta",
        "concrete powder", "stained glass", "calcite", "tuff bricks", "polished tuff", "resin bricks", "pale oak planks"
    };

    private final SettingGroup sg = settings.getDefaultGroup();
    private final SettingGroup sgTiming = settings.createGroup("Timing");
    private final SettingGroup sgAdvanced = settings.createGroup("Advanced");

    // ---- General
    private final Setting<Integer> flagFrom = sg.add(new IntSetting.Builder()
        .name("flag-from-millions").description("Flag an item when one of its listings costs at least this much, in millions (100 = $100M).")
        .defaultValue(100).min(1).max(1_000_000).sliderRange(1, 2000).build());

    private final Setting<Integer> flagUpTo = sg.add(new IntSetting.Builder()
        .name("flag-up-to-millions").description("Ignore listings above this price, in millions. 0 = no upper limit (so $2B is flagged too).")
        .defaultValue(0).min(0).max(1_000_000).sliderRange(0, 5000).build());

    private final Setting<Integer> minListings = sg.add(new IntSetting.Builder()
        .name("listings-needed").description("How many listings in the price range an item needs before it's flagged.")
        .defaultValue(1).min(1).max(20).sliderMax(5).build());

    private final Setting<Integer> dingVolume = sg.add(new IntSetting.Builder()
        .name("ding-volume").description("Volume of the ding, in percent. 0 = silent (chat message only).")
        .defaultValue(100).min(0).max(100).sliderMax(100).build());

    // ---- Timing
    private final Setting<Integer> gapTicks = sgTiming.add(new IntSetting.Builder()
        .name("ticks-between-items").description("Pause between two searches. Lower = faster, but too fast can get you kicked. 20 ticks = 1 second.")
        .defaultValue(30).min(10).max(200).sliderMax(100).build());

    private final Setting<Integer> settleTicks = sgTiming.add(new IntSetting.Builder()
        .name("load-wait-ticks").description("How long to wait after the auction house opens for the listings to appear.")
        .defaultValue(12).min(2).max(100).sliderMax(60).build());

    private final Setting<Integer> pauseAfterList = sgTiming.add(new IntSetting.Builder()
        .name("pause-after-list-seconds").description("How long to rest after going through the whole list, before starting again.")
        .defaultValue(20).min(1).max(3600).sliderRange(1, 300).build());

    private final Setting<Integer> pages = sgTiming.add(new IntSetting.Builder()
        .name("pages-to-check").description("How many pages of results to read for each item (1 = just the first page).")
        .defaultValue(1).min(1).max(5).sliderMax(5).build());

    private final Setting<Integer> realertMinutes = sgTiming.add(new IntSetting.Builder()
        .name("repeat-alert-minutes").description("If the same expensive listing is still there, remind you after this many minutes.")
        .defaultValue(10).min(1).max(240).sliderMax(60).build());

    // ---- Advanced
    private final Setting<String> command = sgAdvanced.add(new StringSetting.Builder()
        .name("command").description("The command that opens the auction house search, without the slash.")
        .defaultValue("ah").build());

    private final Setting<String> priceWord = sgAdvanced.add(new StringSetting.Builder()
        .name("price-word").description("The word on the price line of a listing's description. Use debug-menu to see what listings look like.")
        .defaultValue("price").build());

    private final Setting<String> extraItems = sgAdvanced.add(new StringSetting.Builder()
        .name("extra-items").description("More items to check, separated by commas (optional).")
        .defaultValue("").build());

    private final Setting<Boolean> debugMenu = sgAdvanced.add(new BoolSetting.Builder()
        .name("debug-menu").description("Print what each listing looks like in chat, to help if prices aren't being read.")
        .defaultValue(false).build());

    private enum Step { WAIT, OPENING, READING }

    private Step step = Step.WAIT;
    private int timer, termIndex, openTicks, settle, readTries, page;
    private int listingsSeen, flaggedSeen;
    private double highestFlag;
    private String lastPageSignature = "";
    private final Map<String, String> lastSignature = new HashMap<>();
    private final Map<String, Long> lastTime = new HashMap<>();

    public AuctionWatch() {
        super(Category.AUTOMATION, "ah-watch",
            "Checks the auction house for easy-to-craft items listed at silly prices and dings when it finds one.");
    }

    @Override
    public void onActivate() {
        step = Step.WAIT;
        timer = 20;
        termIndex = 0;
        openTicks = settle = readTries = page = 0;
        lastSignature.clear();
        lastTime.clear();
        info("Watching the auction house: " + terms().size() + " items. Flagging listings of $" + Prices.format(flagFrom.get() * 1e6) + "+.");
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null && menuOpen()) mc.player.closeHandledScreen();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        switch (step) {
            case WAIT -> waiting();
            case OPENING -> opening();
            case READING -> reading();
        }
    }

    // ---------------------------------------------------------------- steps

    private void waiting() {
        if (timer > 0) {
            timer--;
            return;
        }
        if (menuOpen()) { // you're using some menu - wait
            timer = 20;
            return;
        }

        List<String> terms = terms();
        if (termIndex >= terms.size()) termIndex = 0;
        String term = terms.get(termIndex);

        mc.getNetworkHandler().sendChatCommand(command.get().trim() + " " + term);
        openTicks = 0;
        step = Step.OPENING;
    }

    private void opening() {
        if (menuOpen()) {
            settle = settleTicks.get();
            readTries = 0;
            page = 0;
            listingsSeen = 0;
            flaggedSeen = 0;
            highestFlag = 0;
            lastPageSignature = "";
            step = Step.READING;
            return;
        }
        if (++openTicks > 120) {
            finishTerm(); // it didn't open: just move on
        }
    }

    private void reading() {
        if (!menuOpen()) {
            finishTerm();
            return;
        }
        if (settle-- > 0) return;

        PageScan scan = scanPage(page == 0 && readTries == 0);

        // Listings can take a moment to appear: look again a few times before believing "nothing".
        if (scan.listings == 0 && page == 0 && ++readTries < 4) {
            settle = 10;
            return;
        }

        boolean samePage = !lastPageSignature.isEmpty() && lastPageSignature.equals(scan.signature);
        if (!samePage) {
            listingsSeen += scan.listings;
            flaggedSeen += scan.flagged;
            highestFlag = Math.max(highestFlag, scan.highestFlag);
            lastPageSignature = scan.signature;
        }

        // Read another page?
        if (!samePage && page + 1 < pages.get() && scan.listings > 0) {
            int next = findNextButton();
            if (next >= 0) {
                click(next);
                page++;
                settle = settleTicks.get();
                return;
            }
        }

        report(currentTerm());
        mc.player.closeHandledScreen();
        finishTerm();
    }

    private void finishTerm() {
        List<String> terms = terms();
        termIndex++;
        if (termIndex >= terms.size()) {
            termIndex = 0;
            timer = pauseAfterList.get() * 20;
        } else {
            timer = gapTicks.get();
        }
        step = Step.WAIT;
    }

    // ---------------------------------------------------------------- reading the listings

    private static final class PageScan {
        int listings;
        int flagged;
        double highestFlag;
        String signature = "";
    }

    private PageScan scanPage(boolean log) {
        double from = flagFrom.get() * 1e6;
        double to = flagUpTo.get() > 0 ? flagUpTo.get() * 1e6 : Double.MAX_VALUE;

        PageScan result = new PageScan();
        StringBuilder signature = new StringBuilder();
        int shown = 0;

        ScreenHandler handler = mc.player.currentScreenHandler;
        for (Slot slot : handler.slots) {
            if (slot.inventory == mc.player.getInventory()) continue; // not your own items
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            double price = priceOf(stack);

            if (log && debugMenu.get() && shown++ < 12) {
                info("slot " + slot.id + ": \"" + stack.getName().getString() + "\" price=" + (price < 0 ? "none" : Prices.format(price))
                    + " | " + String.join(" / ", loreLines(stack)));
            }

            if (price < 0) continue; // buttons and decorations have no price
            result.listings++;
            signature.append((long) price).append(',');
            if (price >= from && price <= to) {
                result.flagged++;
                result.highestFlag = Math.max(result.highestFlag, price);
            }
        }
        result.signature = signature.toString();
        return result;
    }

    private double priceOf(ItemStack stack) {
        List<String> lines = loreLines(stack);
        String word = priceWord.get().trim().toLowerCase(Locale.ROOT);

        if (!word.isEmpty()) {
            for (String line : lines) {
                String lower = line.toLowerCase(Locale.ROOT);
                if (lower.contains(word)) {
                    double p = Prices.parse(line.substring(lower.indexOf(word) + word.length()));
                    if (p >= 0) return p;
                }
            }
        }
        // Fallback: any line with a dollar sign
        for (String line : lines) {
            if (line.contains("$")) {
                double p = Prices.parse(line);
                if (p >= 0) return p;
            }
        }
        return -1;
    }

    private List<String> loreLines(ItemStack stack) {
        List<String> out = new ArrayList<>();
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return out;
        for (Text line : lore.lines()) {
            String s = line.getString().trim();
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    /** The "next page" button, found by its name. */
    private int findNextButton() {
        for (Slot slot : mc.player.currentScreenHandler.slots) {
            if (slot.inventory == mc.player.getInventory()) continue;
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty() && stack.getName().getString().toLowerCase(Locale.ROOT).contains("next")) return slot.id;
        }
        return -1;
    }

    private void click(int slotId) {
        ScreenHandler handler = mc.player.currentScreenHandler;
        mc.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.PICKUP, mc.player);
    }

    // ---------------------------------------------------------------- reporting

    private void report(String term) {
        if (flaggedSeen < minListings.get()) {
            if (debugMenu.get()) info(term + ": " + listingsSeen + " listed, none flagged.");
            return;
        }

        String signature = flaggedSeen + "@" + (long) highestFlag;
        long now = System.currentTimeMillis();
        String previous = lastSignature.get(term);
        Long previousTime = lastTime.get(term);
        boolean fresh = !signature.equals(previous) || previousTime == null
            || now - previousTime > realertMinutes.get() * 60_000L;
        if (!fresh) return;

        lastSignature.put(term, signature);
        lastTime.put(term, now);

        warning("[AH] " + term + ": listed for $" + Prices.format(highestFlag) + " (" + flaggedSeen + " listing(s) at $"
            + Prices.format(flagFrom.get() * 1e6) + "+, " + listingsSeen + " total)");
        AlarmPlayer.play("Ding", dingVolume.get() / 100f, 1);
    }

    // ---------------------------------------------------------------- helpers

    private List<String> terms() {
        List<String> out = new ArrayList<>();
        for (String t : DEFAULT_ITEMS) if (!out.contains(t)) out.add(t);
        for (String part : extraItems.get().split(",")) {
            String t = part.trim();
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    private String currentTerm() {
        List<String> terms = terms();
        return terms.get(Math.min(termIndex, terms.size() - 1));
    }

    private boolean menuOpen() {
        return mc.player.currentScreenHandler != mc.player.playerScreenHandler;
    }
}
