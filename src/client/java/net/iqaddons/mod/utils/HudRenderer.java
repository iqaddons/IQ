package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class HudRenderer {

    private static final Minecraft mc = Minecraft.getInstance();

    public static void drawText(
            @NotNull GuiGraphicsExtractor context,
            @NotNull String text,
            int x, int y,
            int color
    ) {
        Font textRenderer = mc.font;
        if (textRenderer == null) return;

        context.text(textRenderer, text, x, y, color);
    }

    public static void drawCenteredText(
            @NotNull GuiGraphicsExtractor context,
            @NotNull String text,
            int centerX, int y,
            int color
    ) {
        Font textRenderer = mc.font;
        if (textRenderer == null) return;

        int width = textRenderer.width(text);
        context.text(textRenderer, text, centerX - width / 2, y, color);
    }

    public static void drawTooltip(
            @NotNull GuiGraphicsExtractor context,
            @NotNull String text,
            double mouseX,
            double mouseY,
            float scale
    ) {
        Font textRenderer = mc.font;
        if (textRenderer == null) return;

        String[] lines = text.split("\n");
        List<FormattedCharSequence> orderedLines = new ArrayList<>();
        int maxWidth = 0;

        for (String line : lines) {
            FormattedCharSequence ordered = Component.nullToEmpty(line).getVisualOrderText();
            orderedLines.add(ordered);
            maxWidth = Math.max(maxWidth, textRenderer.width(ordered));
        }

        int x = (int) (mouseX / scale) + 8;
        int y = (int) (mouseY / scale) - 4;

        int padding = 4;
        int lineHeight = textRenderer.lineHeight + 2;
        int boxWidth = maxWidth + padding * 2;
        int boxHeight = orderedLines.size() * lineHeight + padding * 2 - 2;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        if (x + boxWidth > screenWidth / scale) {
            x = (int) (mouseX / scale) - boxWidth - 8;
        }
        if (y + boxHeight > screenHeight / scale) {
            y = (int) (mouseY / scale) - boxHeight;
        }

        int bgColor = new Color(20, 20, 20, 230).getRGB();
        int borderColor = new Color(80, 80, 80, 255).getRGB();

        context.fill(x - 1, y - 1, x + boxWidth + 1, y + boxHeight + 1, borderColor);
        context.fill(x, y, x + boxWidth, y + boxHeight, bgColor);

        int textY = y + padding;
        for (FormattedCharSequence line : orderedLines) {
            context.text(textRenderer, line, x + padding, textY, -1, true);
            textY += lineHeight;
        }
    }

    public static void drawSimpleTooltip(
            @NotNull GuiGraphicsExtractor context,
            @NotNull String text,
            double mouseX,
            double mouseY
    ) {
        drawTooltip(context, text, mouseX, mouseY, 1.0f);
    }

    public static void drawBox(
            @NotNull GuiGraphicsExtractor context,
            int x, int y,
            int width, int height,
            int fillColor,
            int borderColor
    ) {
        context.fill(x, y, x + width, y + height, fillColor);
        context.outline(
                x, y,
                width, height,
                borderColor
        );
    }

    public static void drawBackground(
            @NotNull GuiGraphicsExtractor context,
            int x, int y,
            int width, int height,
            int alpha
    ) {
        int color = new Color(0, 0, 0, alpha).getRGB();
        context.fill(x, y, x + width, y + height, color);
    }

    public static void drawHighlight(
            @NotNull GuiGraphicsExtractor context,
            int x, int y,
            int width, int height
    ) {
        int color = new Color(255, 255, 255, 30).getRGB();
        context.fill(x, y, x + width, y + height, color);
    }

    public static void drawProgressBar(
            @NotNull GuiGraphicsExtractor context,
            int x, int y,
            int width, int height,
            float progress,
            int fillColor,
            int emptyColor
    ) {
        if (mc.screen != null) return;
        progress = Math.clamp(progress, 0.0f, 1.0f);
        int filledWidth = (int) (width * progress);

        if (filledWidth < width) {
            context.fill(x + filledWidth, y, x + width, y + height, emptyColor);
        }

        if (filledWidth > 0) {
            context.fill(x, y, x + filledWidth, y + height, fillColor);
        }

        context.outline(
                x, y,
                width, height,
                0xFF3C3C3C
        );
    }

    public static void drawProgressBarAuto(
            @NotNull GuiGraphicsExtractor context,
            int x, int y,
            int width, int height,
            float progress
    ) {
        int fillColor = getProgressColor(progress);
        int emptyColor = new Color(40, 40, 40, 200).getRGB();
        drawProgressBar(context, x, y, width, height, progress, fillColor, emptyColor);
    }

    public static int getProgressColor(float progress) {
        progress = Math.clamp(progress, 0.0f, 1.0f);

        int r, g;
        if (progress < 0.5f) {
            r = 255;
            g = (int) (255 * (progress * 2));
        } else {
            r = (int) (255 * (1 - (progress - 0.5f) * 2));
            g = 255;
        }

        return new Color(r, g, 0, 255).getRGB();
    }

    public static double @NotNull [] getScaledMousePosition() {
        double scaleFactor = mc.getWindow().getGuiScale();
        double mouseX = mc.mouseHandler.xpos() / scaleFactor;
        double mouseY = mc.mouseHandler.ypos() / scaleFactor;
        return new double[]{mouseX, mouseY};
    }

    public static @NotNull Font getTextRenderer() {
        return mc.font;
    }

    public static int getFontHeight() {
        return mc.font.lineHeight;
    }

    public static int getTextWidth(@NotNull String text) {
        return mc.font.width(text);
    }
}