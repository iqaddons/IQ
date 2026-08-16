package net.iqaddons.mod.screen;

import net.iqaddons.mod.screen.render.RoundedPaneRenderState;
import net.iqaddons.mod.screen.render.RoundedPaneRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

final class ScreenUiUtil {

    private ScreenUiUtil() {
    }

    static int lerpArgb(int c0, int c1, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        int a0 = (c0 >> 24) & 0xFF;
        int r0 = (c0 >> 16) & 0xFF;
        int g0 = (c0 >> 8) & 0xFF;
        int b0 = c0 & 0xFF;
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        return ((int) (a0 + (a1 - a0) * clamped) << 24)
                | ((int) (r0 + (r1 - r0) * clamped) << 16)
                | ((int) (g0 + (g1 - g0) * clamped) << 8)
                | (int) (b0 + (b1 - b0) * clamped);
    }

    static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    static boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    static void drawHollowRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    static final int SLIDER_TRACK_H = 6;
    static final int SLIDER_KNOB = 10;

    static void drawPill(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        if (h <= 0 || w <= 0) {
            return;
        }
        float radius = Math.min(w, h) * 0.5f;
        RoundedPaneRenderer.submit(ctx, x, y, w, h, color, radius, radius, radius, radius, 0f, 0f, color);
    }

    /** Rounded track + circular knob. {@code t} is 0..1. Track top-left is {@code (x, y)}. */
    static void drawSlider(
            GuiGraphicsExtractor ctx,
            int x,
            int y,
            int w,
            float t,
            int trackColor,
            int fillColor,
            int knobColor
    ) {
        if (w <= 0) {
            return;
        }
        float clamped = (float) clamp01(t);
        int trackH = SLIDER_TRACK_H;
        int knob = SLIDER_KNOB;
        int radius = trackH / 2;

        drawRoundedRect(ctx, x, y, w, trackH, trackColor, radius);
        int fillW = Math.round(w * clamped);
        if (fillW > 0) {
            int fillRadius = Math.min(radius, Math.max(1, fillW / 2));
            drawRoundedRect(ctx, x, y, fillW, trackH, fillColor, fillRadius);
        }

        int kx = Math.round(x + clamped * w - knob * 0.5f);
        int ky = y + trackH / 2 - knob / 2;
        drawPill(ctx, kx, ky, knob, knob, knobColor);
    }

    static void drawPillGlow(
            GuiGraphicsExtractor ctx,
            int x,
            int y,
            int w,
            int h,
            int fillColor,
            int glowColor,
            float glowWidth
    ) {
        if (h <= 0 || w <= 0) {
            return;
        }
        float radius = Math.min(w, h) * 0.5f;
        boolean hasGlow = glowWidth > 0f && ((glowColor >>> 24) & 0xFF) != 0;
        RoundedPaneRenderer.submit(
                ctx, x, y, w, h, fillColor,
                radius, radius, radius, radius,
                hasGlow ? Math.max(0f, glowWidth) : 0f,
                hasGlow ? 1.0f : 0f,
                glowColor
        );
    }

    static void drawRoundedRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color, int radius) {
        drawRoundedRect(ctx, x, y, w, h, color, radius, radius, radius, radius);
    }

    static void drawRoundedRect(
            GuiGraphicsExtractor ctx,
            int x,
            int y,
            int w,
            int h,
            int color,
            int topLeftRadius,
            int topRightRadius,
            int bottomRightRadius,
            int bottomLeftRadius
    ) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int maxR = Math.min(w, h) / 2;
        int tl = Math.max(0, Math.min(topLeftRadius, maxR));
        int tr = Math.max(0, Math.min(topRightRadius, maxR));
        int br = Math.max(0, Math.min(bottomRightRadius, maxR));
        int bl = Math.max(0, Math.min(bottomLeftRadius, maxR));
        if (tl == 0 && tr == 0 && br == 0 && bl == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        RoundedPaneRenderer.submit(ctx, x, y, w, h, color, tl, tr, br, bl, 0f, 0f, color);
    }

    static void drawRoundedGlow(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color, int radius, float glowWidth) {
        drawRoundedGlow(ctx, x, y, w, h, color, color, radius, glowWidth);
    }

    static void drawRoundedGlow(
            GuiGraphicsExtractor ctx,
            int x,
            int y,
            int w,
            int h,
            int fillColor,
            int glowColor,
            int radius,
            float glowWidth
    ) {
        drawRoundedGlow(ctx, x, y, w, h, fillColor, glowColor, radius, radius, radius, radius, glowWidth);
    }

    static void drawRoundedGlow(
            GuiGraphicsExtractor ctx,
            int x,
            int y,
            int w,
            int h,
            int fillColor,
            int glowColor,
            int topLeftRadius,
            int topRightRadius,
            int bottomRightRadius,
            int bottomLeftRadius,
            float glowWidth
    ) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int maxR = Math.min(w, h) / 2;
        int tl = Math.max(0, Math.min(topLeftRadius, maxR));
        int tr = Math.max(0, Math.min(topRightRadius, maxR));
        int br = Math.max(0, Math.min(bottomRightRadius, maxR));
        int bl = Math.max(0, Math.min(bottomLeftRadius, maxR));
        boolean hasGlow = glowWidth > 0f && ((glowColor >>> 24) & 0xFF) != 0;
        if (!hasGlow && tl == 0 && tr == 0 && br == 0 && bl == 0) {
            ctx.fill(x, y, x + w, y + h, fillColor);
            return;
        }
        RoundedPaneRenderer.submit(ctx, x, y, w, h, fillColor, tl, tr, br, bl, Math.max(0f, glowWidth), 1.0f, glowColor);
    }

    static void drawRoundedFillMode(
            GuiGraphicsExtractor ctx,
            int x,
            int y,
            int w,
            int h,
            int colorA,
            int colorB,
            int radius,
            int fillMode
    ) {
        if (w <= 0 || h <= 0) {
            return;
        }
        float r = Math.max(0, radius);
        RoundedPaneRenderer.submitFillMode(ctx, x, y, w, h, colorA, colorB, r, r, r, r, fillMode);
    }

    static void drawRoundedSv(GuiGraphicsExtractor ctx, int x, int y, int size, float hue, int radius) {
        int huePacked = (Math.round((float) clamp01(hue) * 255f) << 16) | 0xFF000000;
        drawRoundedFillMode(ctx, x, y, size, size, 0xFFFFFFFF, huePacked, radius, RoundedPaneRenderState.FILL_SV);
    }

    static void drawRoundedHueTrack(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius) {
        drawRoundedFillMode(ctx, x, y, w, h, 0xFFFFFFFF, 0xFFFFFFFF, radius, RoundedPaneRenderState.FILL_HUE);
    }

    static void drawRoundedAlphaTrack(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int opaqueColor, int radius) {
        drawRoundedFillMode(ctx, x, y, w, h, opaqueColor | 0xFF000000, 0, radius, RoundedPaneRenderState.FILL_ALPHA);
    }

    static void drawRoundedCheckerColor(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int argb, int radius) {
        drawRoundedFillMode(ctx, x, y, w, h, argb, 0, radius, RoundedPaneRenderState.FILL_CHECKER_COLOR);
    }

    static void drawRoundedHollowRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color, int radius) {
        if (w <= 1 || h <= 1) {
            return;
        }
        if (radius <= 0) {
            drawHollowRect(ctx, x, y, w, h, color);
            return;
        }

        int r = Math.min(radius, Math.min(w, h) / 2);
        ctx.fill(x + r, y, x + w - r, y + 1, color);
        ctx.fill(x + r, y + h - 1, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + 1, y + h - r, color);
        ctx.fill(x + w - 1, y + r, x + w, y + h - r, color);

        for (int i = 0; i < r; i++) {
            int inset = cornerInset(i, r);
            ctx.fill(x + inset, y + i, x + inset + 1, y + i + 1, color);
            ctx.fill(x + w - inset - 1, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - i - 1, x + inset + 1, y + h - i, color);
            ctx.fill(x + w - inset - 1, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    /** Covers pixels outside a rounded rect so gradient/checker fills appear rounded. */
    static void maskOutsideRoundedRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.min(Math.max(0, radius), Math.min(w, h) / 2);
        if (r <= 0) {
            return;
        }
        for (int i = 0; i < r; i++) {
            int inset = cornerInset(i, r);
            if (inset <= 0) {
                continue;
            }
            ctx.fill(x, y + i, x + inset, y + i + 1, color);
            ctx.fill(x + w - inset, y + i, x + w, y + i + 1, color);
            ctx.fill(x, y + h - i - 1, x + inset, y + h - i, color);
            ctx.fill(x + w - inset, y + h - i - 1, x + w, y + h - i, color);
        }
    }

    static List<String> wrapTextSimple(java.util.function.ToIntFunction<String> widthOf, String text, int maxWidth) {
        if (maxWidth <= 0) {
            return List.of(text);
        }
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (widthOf.applyAsInt(candidate) <= maxWidth) {
                line = new StringBuilder(candidate);
            } else {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                }
                line = new StringBuilder(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    static List<String> wrapTextParagraphs(java.util.function.ToIntFunction<String> widthOf, String text, int maxWidth) {
        if (maxWidth <= 0) {
            return List.of(text);
        }
        List<String> lines = new ArrayList<>();
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\n", -1);

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (widthOf.applyAsInt(candidate) <= maxWidth) {
                    line = new StringBuilder(candidate);
                } else {
                    if (!line.isEmpty()) {
                        lines.add(line.toString());
                        line = new StringBuilder();
                    }
                    if (widthOf.applyAsInt(word) > maxWidth) {
                        String trimmed = word;
                        while (!trimmed.isEmpty() && widthOf.applyAsInt(trimmed + "\u2026") > maxWidth) {
                            trimmed = trimmed.substring(0, trimmed.length() - 1);
                        }
                        lines.add(trimmed + "\u2026");
                    } else {
                        line = new StringBuilder(word);
                    }
                }
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
        }

        return lines.isEmpty() ? List.of(text) : lines;
    }

    private static int cornerInset(int row, int radius) {
        double dy = radius - row - 0.5;
        double dx = Math.sqrt(Math.max(0.0, (radius * (double) radius) - (dy * dy)));
        return Math.max(0, (int) Math.ceil(radius - dx));
    }
}



