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
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AH Watch: every so often, searches the auction house for items you expect almost nobody to be selling,
 * and tells you when a few cheap listings show up.
 *
 * It only looks and notifies - it never buys anything.
 * Set what to watch with:  /afk set ah-watch items netherite_scrap, elytra
 */
public class AuctionWatch extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();
    private final SettingGroup sgAlert = settings.createGroup("Alert");
    private final SettingGroup sgTiming = settings.createGroup("Timing");

    // ---- General
    private final Setting<String> items = sg.add(new StringSetting.Builder()
        .name("items").description("What to search for, separated by commas (whatever you would type after /ah). Change it with /afk set ah-watch items <list>.")
        .defaultValue("").build());

    private final Setting<String> command = sg.add(new StringSetting.Builder()
        .name("command").description("The command that opens the auction house search, without the slash. The search word is added after it.")
        .defaultValue("ah").build());

    private final Setting<String> minPrice = sg.add(new StringSetting.Builder()
        .name("min-price").description("Cheapest price that counts (you can write 1, 1500, 1.5M ...).")
        .defaultValue("1").build());

    private final Setting<String> maxPrice = sg.add(new StringSetting.Builder()
        .name("max-price").description("Most expensive price that still counts as cheap (for example 10M).")
        .defaultValue("10M").build());

    private final Setting<Integer> minCheap = sg.add(new IntSetting.Builder()
        .name("min-cheap-listings").description("Alert when at least this many listings are inside the price range.")
        .defaultValue(1).min(1).max(50).sliderMax(10).build());

    private final Setting<Integer> maxTotal = sg.add(new IntSetting.Builder()
        .name("max-total-listings").description("Only alert if the item has at most this many listings in total (you expect almost none). 0 = don't check.")
        .defaultValue(0).min(0).max(100).sliderMax(20).build());

    private final Setting<String> priceWord = sg.add(new StringSetting.Builder()
        .name("price-word").description("The word on the price line of a listing's description. Use debug-menu to see what the listings look like.")
        .defaultValue("price").build());

    // ---- Alert
    private final Setting<String> alarm = sgAlert.add(new ModeSetting.Builder()
        .name("alarm").description("The sound to play when something cheap is found.")
        .options(AlarmPlayer.NAMES).defaultValue("Chime").build());

    private final Setting<Integer> alarmVolume = sgAlert.add(new IntSetting.Builder()
        .name("alarm-volume").description("Alarm volume in percent.")
        .defaultValue(100).min(0).max(100).sliderMax(100).build());

    private final Setting<Integer> alarmRepeats = sgAlert.add(new IntSetting.Builder()
        .name("alarm-repeats").description("How many times the alarm plays.")
        .defaultValue(2).min(1).max(10).sliderMax(10).build());

    private final Setting<Integer> realertMinutes = sgAlert.add(new IntSetting.Builder()
        .name("repeat-alert-minutes").description("If the same cheap listings are still there, remind you after this many minutes.")
        .defaultValue(5).min(1).max(120).sliderMax(30).build());

    // ---- Timing
    private final Setting<Integer> intervalSeconds = sgTiming.add(new IntSetting.Builder()
        .name("check-every-seconds").description("How often to go through the whole list.")
        .defaultValue(45).min(10).max(3600).sliderRange(10, 300).build());

    private final Setting<Integer> itemGapSeconds = sgTiming.add(new IntSetting.Builder()
        .name("seconds-between-items").description("Pause between two searches (so the server doesn't get spammed).")
        .defaultValue(4).min(1).max(60).sliderMax(20).build());

    private final Setting<Integer> settleTicks = sgTiming.add(new IntSetting.Builder()
        .name("load-wait-ticks").description("How long to wait after the auction house opens for the listings to appear. 20 ticks = 1 second.")
        .defaultValue(12).min(2).max(100).sliderMax(60).build());

    private final Setting<Boolean> debugMenu = sgTiming.add(new BoolSetting.Builder()
        .name("debug-menu").description("Print what each listing looks like in chat, to help set the price word.")
        .defaultValue(false).build());

    private enum Step { WAIT, OPENING, READING }

    private record Scan(int listings, int cheap, double cheapest) { }

    private Step step = Step.WAIT;
    private int timer, termIndex, openTicks, settle, readTries;
    private String lastMessage = "";
    private final Map<String, String> lastSignature = new HashMap<>();
    private final Map<String, Long> lastTime = new HashMap<>();

    public AuctionWatch() {
        super(Category.AUTOMATION, "ah-watch",
            "Watches the auction house for rarely-listed items and tells you when a few cheap listings appear.");
    }

    @Override
    public void onActivate() {
        step = Step.WAIT;
        timer = 40;
        termIndex = 0;
        openTicks = settle = readTries = 0;
        lastMessage = "";
        lastSignature.clear();
        lastTime.clear();
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

        List<String> terms = terms();
        if (terms.isEmpty()) {
            say("Nothing to watch yet. Type: /afk set ah-watch items netherite_scrap, elytra");
            timer = 200;
            return;
        }
        if (menuOpen()) { // you're using some menu - wait
            timer = 20;
            return;
        }

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
            step = Step.READING;
            return;
        }
        if (++openTicks > 120) {
            warning("The auction house didn't open for \"" + currentTerm() + "\" - skipping it.");
            finishTerm();
        }
    }

    private void reading() {
        if (!menuOpen()) {
            warning("The auction house closed before I could read it - skipping \"" + currentTerm() + "\".");
            finishTerm();
            return;
        }
        if (settle-- > 0) return;

        Scan scan = scan(readTries == 0);
        // Listings can take a moment to appear: look again a few times before believing "nothing".
        if (scan.listings() == 0 && ++readTries < 4) {
            settle = 10;
            return;
        }

        report(currentTerm(), scan);
        mc.player.closeHandledScreen();
        finishTerm();
    }

    private void finishTerm() {
        List<String> terms = terms();
        termIndex++;
        if (termIndex >= terms.size()) {
            termIndex = 0;
            timer = Math.max(itemGapSeconds.get(), intervalSeconds.get()) * 20;
        } else {
            timer = itemGapSeconds.get() * 20;
        }
        step = Step.WAIT;
    }

    // ---------------------------------------------------------------- reading the listings

    private Scan scan(boolean log) {
        double min = Prices.parse(minPrice.get());
        double max = Prices.parse(maxPrice.get());
        if (min < 0) min = 1;
        if (max < 0) max = 10_000_000;

        int listings = 0;
        int cheap = 0;
        double cheapest = Double.MAX_VALUE;
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
            listings++;
            if (price >= min && price <= max) {
                cheap++;
                cheapest = Math.min(cheapest, price);
            }
        }
        return new Scan(listings, cheap, cheapest == Double.MAX_VALUE ? -1 : cheapest);
    }

    private double priceOf(ItemStack stack) {
        List<String> lines = loreLines(stack);
        String word = priceWord.get().trim().toLowerCase();

        if (!word.isEmpty()) {
            for (String line : lines) {
                if (line.toLowerCase().contains(word)) {
                    double p = Prices.parse(line.substring(line.toLowerCase().indexOf(word) + word.length()));
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

    // ---------------------------------------------------------------- reporting

    private void report(String term, Scan scan) {
        boolean totalOk = maxTotal.get() == 0 || scan.listings() <= maxTotal.get();
        boolean alert = scan.cheap() >= minCheap.get() && totalOk;

        if (!alert) {
            if (debugMenu.get()) info(term + ": " + scan.listings() + " listed, " + scan.cheap() + " in the price range - no alert.");
            return;
        }

        String signature = scan.cheap() + "@" + (long) scan.cheapest();
        long now = System.currentTimeMillis();
        String previous = lastSignature.get(term);
        Long previousTime = lastTime.get(term);
        boolean fresh = !signature.equals(previous) || previousTime == null
            || now - previousTime > realertMinutes.get() * 60_000L;
        if (!fresh) return;

        lastSignature.put(term, signature);
        lastTime.put(term, now);

        warning("[AH] " + term + ": " + scan.cheap() + " cheap listing(s), cheapest $" + Prices.format(scan.cheapest())
            + " (" + scan.listings() + " listed in total)");
        AlarmPlayer.play(alarm.get(), alarmVolume.get() / 100f, alarmRepeats.get());
    }

    // ---------------------------------------------------------------- helpers

    private List<String> terms() {
        List<String> out = new ArrayList<>();
        for (String part : items.get().split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private String currentTerm() {
        List<String> terms = terms();
        return terms.isEmpty() ? "" : terms.get(Math.min(termIndex, terms.size() - 1));
    }

    private boolean menuOpen() {
        return mc.player.currentScreenHandler != mc.player.playerScreenHandler;
    }

    private void say(String message) {
        if (!message.equals(lastMessage)) {
            lastMessage = message;
            info(message);
        }
    }
}
