package net.iqaddons.mod.screen;

import net.minecraft.client.gui.Font;
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

    static void drawRoundedRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color, int radius) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }

        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int inset = cornerInset(i, r);
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
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
        for (int i = 0; i < r; i++) {
            int inset = cornerInset(i, r);
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }

        ctx.fill(x, y + r, x + 1, y + h - r, color);
        ctx.fill(x + w - 1, y + r, x + w, y + h - r, color);
    }

    static List<String> wrapTextSimple(Font textRenderer, String text, int maxWidth) {
        if (maxWidth <= 0) {
            return List.of(text);
        }
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (textRenderer.width(candidate) <= maxWidth) {
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

    static List<String> wrapTextParagraphs(Font textRenderer, String text, int maxWidth) {
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
                if (textRenderer.width(candidate) <= maxWidth) {
                    line = new StringBuilder(candidate);
                } else {
                    if (!line.isEmpty()) {
                        lines.add(line.toString());
                        line = new StringBuilder();
                    }
                    if (textRenderer.width(word) > maxWidth) {
                        lines.add(textRenderer.plainSubstrByWidth(word, Math.max(0, maxWidth - 6)) + "\u2026");
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



