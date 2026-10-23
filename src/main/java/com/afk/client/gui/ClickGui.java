package com.afk.client.gui;

import com.afk.client.AfkClient;
import com.afk.client.Config;
import com.afk.client.module.Module;
import com.afk.client.module.ModuleManager;
import com.afk.client.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The AFK Client menu. Everything is drawn and handled here, no outside GUI library. */
public class ClickGui extends Screen {
    private static final int SIDEBAR_W = 130;
    private static final int TITLE_H = 26;
    private static final int ROW_H = 28;
    private static final int GAP = 6;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private int selected;
    private int scroll;
    private int contentHeight;

    private boolean prevMouseDown;
    private Setting<?> dragging;
    private KeybindSetting listening;
    private boolean listeningForModuleBind;
    private boolean listeningForGuiKey;
    private String hint = "";

    private final Map<Integer, Boolean> keyPrev = new HashMap<>();

    public ClickGui() {
        super(Text.literal(AfkClient.NAME));
        prevMouseDown = mouseDown();
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) keyPrev.put(k, ModuleManager.isDown(k));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        Config.save();
    }

    private boolean mouseDown() {
        return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    /** True once, the moment a key goes down. */
    private boolean keyEdge(int key) {
        boolean now = ModuleManager.isDown(key);
        boolean was = keyPrev.getOrDefault(key, false);
        keyPrev.put(key, now);
        return now && !was;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // --- input state for this frame
        boolean down = mouseDown();
        boolean clicked = down && !prevMouseDown;
        prevMouseDown = down;
        if (!down) dragging = null;

        // --- key handling
        if (listening != null || listeningForModuleBind || listeningForGuiKey) {
            captureKey();
        } else {
            // the menu key closes the menu again
            if (keyEdge(ModuleManager.guiKey)) {
                close();
                return;
            }
            if (ModuleManager.isDown(GLFW.GLFW_KEY_UP)) scroll -= 5;
            if (ModuleManager.isDown(GLFW.GLFW_KEY_DOWN)) scroll += 5;
        }

        // --- window geometry
        int w = Math.min(580, width - 20);
        int h = Math.min(340, height - 20);
        int x0 = (width - w) / 2;
        int y0 = (height - h) / 2;

        hint = "";

        ctx.fill(0, 0, width, height, 0x80000000);
        ctx.fill(x0, y0, x0 + w, y0 + h, Theme.BACKGROUND);

        // title bar
        ctx.fillGradient(x0, y0, x0 + w, y0 + TITLE_H, Theme.TITLE_BAR, 0xFF111120);
        ctx.fill(x0, y0 + TITLE_H - 2, x0 + w, y0 + TITLE_H, Theme.ACCENT);
        ctx.drawText(mc.textRenderer, AfkClient.NAME, x0 + 10, y0 + 9, Theme.ACCENT, true);
        ctx.drawText(mc.textRenderer, "v" + AfkClient.VERSION, x0 + 12 + mc.textRenderer.getWidth(AfkClient.NAME), y0 + 9, Theme.DIM, false);

        // menu key button (top right)
        String guiKeyLabel = listeningForGuiKey ? "Press a key..." : "Menu key: " + com.afk.client.util.Keys.name(ModuleManager.guiKey);
        int gkW = mc.textRenderer.getWidth(guiKeyLabel) + 12;
        int gkX = x0 + w - gkW - 8;
        int gkY = y0 + 5;
        boolean gkHover = hit(mouseX, mouseY, gkX, gkY, gkW, 16);
        ctx.fill(gkX, gkY, gkX + gkW, gkY + 16, gkHover ? Theme.ROW_HOVER : Theme.ROW);
        ctx.drawText(mc.textRenderer, guiKeyLabel, gkX + 6, gkY + 4, Theme.TEXT, false);
        if (gkHover) hint = "Click, then press the key that should open and close this menu.";
        if (gkHover && clicked) {
            listeningForGuiKey = true;
            clearKeys();
        }

        // sidebar
        int sbX = x0;
        int sbY = y0 + TITLE_H;
        int sbH = h - TITLE_H;
        ctx.fill(sbX, sbY, sbX + SIDEBAR_W, sbY + sbH, Theme.SIDEBAR);
        ctx.drawText(mc.textRenderer, "MODULES", sbX + 10, sbY + 8, Theme.DIM, false);

        List<Module> modules = ModuleManager.MODULES;
        if (selected >= modules.size()) selected = 0;
        for (int i = 0; i < modules.size(); i++) {
            Module m = modules.get(i);
            int ey = sbY + 22 + i * 26;
            boolean hover = hit(mouseX, mouseY, sbX + 6, ey, SIDEBAR_W - 12, 22);
            boolean sel = i == selected;
            ctx.fill(sbX + 6, ey, sbX + SIDEBAR_W - 6, ey + 22, sel ? Theme.ROW_HOVER : (hover ? Theme.ROW : Theme.SIDEBAR));
            if (sel) ctx.fill(sbX + 6, ey, sbX + 8, ey + 22, Theme.ACCENT);
            ctx.fill(sbX + SIDEBAR_W - 18, ey + 8, sbX + SIDEBAR_W - 12, ey + 14, m.isActive() ? Theme.GOOD : 0xFF3A3A4E);
            ctx.drawText(mc.textRenderer, prettify(m.name), sbX + 14, ey + 7, sel ? Theme.TEXT : Theme.DIM, false);
            if (hover && clicked) {
                selected = i;
                scroll = 0;
                listening = null;
            }
        }

        // content
        Module m = modules.get(selected);
        int cx = x0 + SIDEBAR_W + 12;
        int cw = w - SIDEBAR_W - 24;
        int top = y0 + TITLE_H + 8;

        ctx.drawText(mc.textRenderer, prettify(m.name), cx, top, Theme.ACCENT, true);
        ctx.drawText(mc.textRenderer, clip(m.description, cw - 190), cx, top + 12, Theme.DIM, false);

        // ON/OFF button
        int btnW = 56;
        int btnX = x0 + w - 12 - btnW;
        boolean onHover = hit(mouseX, mouseY, btnX, top - 2, btnW, 18);
        ctx.fill(btnX, top - 2, btnX + btnW, top + 16, m.isActive() ? (onHover ? 0xFF2FB36B : Theme.GOOD) : (onHover ? 0xFFE04565 : Theme.BAD));
        String onText = m.isActive() ? "ON" : "OFF";
        ctx.drawText(mc.textRenderer, onText, btnX + (btnW - mc.textRenderer.getWidth(onText)) / 2, top + 3, 0xFF0B0B12, false);
        if (onHover) hint = "Turn " + prettify(m.name) + " on or off.";
        if (onHover && clicked) m.toggle();

        // bind button
        String bindLabel = listeningForModuleBind ? "Press a key..." : "Bind: " + m.bind;
        int bindW = mc.textRenderer.getWidth(bindLabel) + 12;
        int bindX = btnX - bindW - 6;
        boolean bindHover = hit(mouseX, mouseY, bindX, top - 2, bindW, 18);
        ctx.fill(bindX, top - 2, bindX + bindW, top + 16, bindHover ? Theme.ROW_HOVER : Theme.ROW);
        ctx.drawText(mc.textRenderer, bindLabel, bindX + 6, top + 3, Theme.TEXT, false);
        if (bindHover) hint = "Click, then press a key to turn this module on/off. Backspace clears it.";
        if (bindHover && clicked) {
            listeningForModuleBind = true;
            clearKeys();
        }

        // settings area
        int viewY = top + 28;
        int viewH = y0 + h - viewY - 18;
        int maxScroll = Math.max(0, contentHeight - viewH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        ctx.enableScissor(cx - 2, viewY, cx + cw + 2, viewY + viewH);
        drawSettings(ctx, m, cx, viewY - scroll, cw, mouseX, mouseY, clicked, viewY, viewH);
        ctx.disableScissor();

        if (maxScroll > 0) {
            ctx.drawText(mc.textRenderer, "Up / Down arrows to scroll", cx, y0 + h - 12, Theme.DIM, false);
            // scrollbar
            int barH = Math.max(14, viewH * viewH / contentHeight);
            int barY = viewY + (int) ((viewH - barH) * (scroll / (double) maxScroll));
            ctx.fill(x0 + w - 6, barY, x0 + w - 3, barY + barH, Theme.ACCENT_DARK);
        }

        // footer hint (setting description)
        if (!hint.isEmpty()) {
            ctx.drawText(mc.textRenderer, clip(hint, cw - 150), cx + 150, y0 + h - 12, Theme.TEXT, false);
        }
    }

    private void drawSettings(DrawContext ctx, Module m, int cx, int startY, int cw, int mx, int my,
                              boolean clicked, int viewY, int viewH) {
        int colW = (cw - GAP) / 2;
        int y = startY;
        int col = 0;

        for (SettingGroup group : m.settings.groups()) {
            boolean any = false;
            for (Setting<?> s : group.all()) if (s.isVisible()) { any = true; break; }
            if (!any) continue;

            if (col != 0) { col = 0; y += ROW_H + GAP; }
            ctx.drawText(mc.textRenderer, group.name.toUpperCase(Locale.ROOT), cx, y + 2, Theme.ACCENT_2, false);
            y += 14;

            for (Setting<?> s : group.all()) {
                if (!s.isVisible()) continue;
                int x = cx + col * (colW + GAP);
                boolean inView = my >= viewY && my <= viewY + viewH;
                drawSetting(ctx, s, x, y, colW, mx, my, clicked && inView, inView);
                col++;
                if (col == 2) { col = 0; y += ROW_H + GAP; }
            }
            if (col != 0) { col = 0; y += ROW_H + GAP; }
            y += 4;
        }
        contentHeight = y - startY;
    }

    private void drawSetting(DrawContext ctx, Setting<?> s, int x, int y, int w, int mx, int my,
                             boolean clicked, boolean inView) {
        boolean hover = inView && hit(mx, my, x, y, w, ROW_H);
        ctx.fill(x, y, x + w, y + ROW_H, hover ? Theme.ROW_HOVER : Theme.ROW);
        if (hover) hint = s.description;
        String label = prettify(s.name);

        if (s instanceof BoolSetting b) {
            ctx.drawText(mc.textRenderer, clip(label, w - 34), x + 8, y + 10, Theme.TEXT, false);
            int bx = x + w - 24;
            ctx.fill(bx, y + 8, bx + 16, y + 20, b.get() ? Theme.ACCENT_DARK : 0xFF3A3A4E);
            int knob = b.get() ? bx + 10 : bx + 2;
            ctx.fill(knob, y + 10, knob + 4, y + 18, b.get() ? Theme.ACCENT : Theme.DIM);
            if (hover && clicked) b.set(!b.get());

        } else if (s instanceof IntSetting i) {
            slider(ctx, s, label, String.valueOf(i.get()), x, y, w, mx, my, clicked, hover,
                (i.get() - i.sliderMin) / (double) Math.max(1, i.sliderMax - i.sliderMin),
                t -> i.set((int) Math.round(i.sliderMin + t * (i.sliderMax - i.sliderMin))));

        } else if (s instanceof DoubleSetting d) {
            String text = String.format(Locale.ROOT, "%." + d.decimals + "f", d.get());
            slider(ctx, s, label, text, x, y, w, mx, my, clicked, hover,
                (d.get() - d.sliderMin) / Math.max(1e-9, d.sliderMax - d.sliderMin),
                t -> {
                    double v = d.sliderMin + t * (d.sliderMax - d.sliderMin);
                    double p = Math.pow(10, d.decimals);
                    d.set(Math.round(v * p) / p);
                });

        } else if (s instanceof StringSetting str) {
            ctx.drawText(mc.textRenderer, clip(label, w - 12), x + 8, y + 4, Theme.TEXT, false);
            ctx.drawText(mc.textRenderer, clip("\"" + str.get() + "\"", w - 16), x + 8, y + 16, Theme.DIM, false);
            if (hover) hint = s.description + " (edit it in config/afk-client.json)";

        } else if (s instanceof KeybindSetting k) {
            ctx.drawText(mc.textRenderer, clip(label, w - 90), x + 8, y + 10, Theme.TEXT, false);
            String keyText = listening == k ? "Press a key..." : k.get().toString();
            int kw = mc.textRenderer.getWidth(keyText) + 12;
            int kx = x + w - kw - 6;
            ctx.fill(kx, y + 6, kx + kw, y + 22, listening == k ? Theme.ACCENT_DARK : 0xFF2F2F45);
            ctx.drawText(mc.textRenderer, keyText, kx + 6, y + 10, Theme.TEXT, false);
            if (hover && clicked) {
                listening = k;
                clearKeys();
            }
        }
    }

    private void slider(DrawContext ctx, Setting<?> s, String label, String valueText, int x, int y, int w,
                        int mx, int my, boolean clicked, boolean hover, double fraction,
                        java.util.function.DoubleConsumer apply) {
        ctx.drawText(mc.textRenderer, clip(label, w - 60), x + 8, y + 5, Theme.TEXT, false);
        ctx.drawText(mc.textRenderer, valueText, x + w - 8 - mc.textRenderer.getWidth(valueText), y + 5, Theme.ACCENT, false);

        int barX = x + 8;
        int barW = w - 16;
        int barY = y + 19;
        double t = Math.max(0, Math.min(1, fraction));
        ctx.fill(barX, barY, barX + barW, barY + 4, 0xFF34344A);
        ctx.fill(barX, barY, barX + (int) (barW * t), barY + 4, Theme.ACCENT);
        int knobX = barX + (int) (barW * t);
        ctx.fill(knobX - 2, barY - 2, knobX + 2, barY + 6, Theme.TEXT);

        if (hover && clicked && my >= y + 14) dragging = s;
        if (dragging == s) {
            double nt = Math.max(0, Math.min(1, (mx - barX) / (double) barW));
            apply.accept(nt);
        }
    }

    // ------------------------------------------------------------------ keys

    private void clearKeys() {
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) keyPrev.put(k, ModuleManager.isDown(k));
    }

    /** While "Press a key..." is showing, take the first key that goes down. */
    private void captureKey() {
        for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
            if (!keyEdge(k)) continue;
            if (k == GLFW.GLFW_KEY_ESCAPE) { // cancel
                listening = null;
                listeningForModuleBind = false;
                listeningForGuiKey = false;
                return;
            }
            boolean clear = k == GLFW.GLFW_KEY_BACKSPACE;
            Keybind newBind = clear ? Keybind.none() : Keybind.of(k);

            if (listening != null) {
                listening.set(newBind);
                listening = null;
            } else if (listeningForModuleBind) {
                ModuleManager.MODULES.get(selected).bind = newBind;
                listeningForModuleBind = false;
            } else if (listeningForGuiKey) {
                if (!clear) ModuleManager.guiKey = k;
                listeningForGuiKey = false;
            }
            return;
        }
    }

    // ------------------------------------------------------------------ helpers

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String clip(String text, int maxWidth) {
        if (maxWidth <= 8) return "";
        if (mc.textRenderer.getWidth(text) <= maxWidth) return text;
        String t = text;
        while (!t.isEmpty() && mc.textRenderer.getWidth(t + "...") > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    /** "spawner-protect" -> "Spawner Protect" */
    private static String prettify(String name) {
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (char c : name.toCharArray()) {
            if (c == '-' || c == '_') {
                sb.append(' ');
                up = true;
            } else if (up) {
                sb.append(Character.toUpperCase(c));
                up = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
