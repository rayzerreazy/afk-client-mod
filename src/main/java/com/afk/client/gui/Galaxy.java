package com.afk.client.gui;

import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/** Draws the galaxy background: deep blue to purple, soft nebula glows and twinkling stars. */
public final class Galaxy {
    private Galaxy() { }

    /** A galaxy panel. Use this for any background. */
    public static void draw(DrawContext ctx, int x1, int y1, int x2, int y2) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) return;

        // deep blue at the top fading into purple at the bottom
        ctx.fillGradient(x1, y1, x2, y2, 0xF0080826, 0xF02A0B54);

        // nebula glows
        glow(ctx, x1 + w * 25 / 100, y1 + h * 35 / 100, w * 30 / 100, h * 30 / 100, 0x8A3CFF, 0x16);
        glow(ctx, x1 + w * 72 / 100, y1 + h * 65 / 100, w * 28 / 100, h * 32 / 100, 0x2F6BFF, 0x16);
        glow(ctx, x1 + w * 55 / 100, y1 + h * 18 / 100, w * 20 / 100, h * 20 / 100, 0xC24BFF, 0x10);

        stars(ctx, x1, y1, x2, y2, 1.0f);
    }

    /** Just the stars (for use over something that already has its own colour). */
    public static void stars(DrawContext ctx, int x1, int y1, int x2, int y2, float density) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) return;

        int count = Math.min(500, (int) ((w * (long) h) / 650 * density));
        Random rnd = new Random(1337L + x1 * 31L + y1 * 17L + w * 7L + h);
        double t = System.currentTimeMillis() / 1000.0;

        int[] palette = {0xFFFFFF, 0xB5CDFF, 0xDCBEFF, 0x9DB8FF};
        for (int i = 0; i < count; i++) {
            int sx = x1 + rnd.nextInt(w);
            int sy = y1 + rnd.nextInt(h);
            double phase = rnd.nextDouble() * Math.PI * 2;
            double speed = 0.6 + rnd.nextDouble() * 1.6;
            double brightness = 0.35 + rnd.nextDouble() * 0.65;
            int rgb = palette[rnd.nextInt(palette.length)];
            boolean big = rnd.nextInt(14) == 0;

            double twinkle = 0.5 + 0.5 * Math.sin(t * speed + phase);
            int alpha = (int) (70 + 160 * twinkle * brightness);
            int color = (alpha << 24) | rgb;

            ctx.fill(sx, sy, sx + 1, sy + 1, color);
            if (big) {
                int dim = ((alpha / 3) << 24) | rgb;
                ctx.fill(sx - 1, sy, sx, sy + 1, dim);
                ctx.fill(sx + 1, sy, sx + 2, sy + 1, dim);
                ctx.fill(sx, sy - 1, sx + 1, sy, dim);
                ctx.fill(sx, sy + 1, sx + 1, sy + 2, dim);
            }
        }
    }

    /** A soft round glow made of a few stacked ellipses. */
    private static void glow(DrawContext ctx, int cx, int cy, int rx, int ry, int rgb, int alpha) {
        if (rx < 2 || ry < 2) return;
        int layers = 4;
        for (int layer = 0; layer < layers; layer++) {
            double scale = 1.0 - layer * (0.75 / layers);
            int lrx = (int) (rx * scale);
            int lry = (int) (ry * scale);
            int color = (alpha << 24) | rgb;
            for (int dy = -lry; dy <= lry; dy++) {
                double f = dy / (double) lry;
                int half = (int) (lrx * Math.sqrt(Math.max(0, 1 - f * f)));
                if (half <= 0) continue;
                ctx.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
            }
        }
    }
}
