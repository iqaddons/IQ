package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class HudRenderer {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void drawText(
            @NotNull DrawContext context,
            @NotNull String text,
            int x, int y,
            int color
    ) {
        TextRenderer textRenderer = mc.textRenderer;
        if (textRenderer == null) return;

        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }

    public static void drawCenteredText(
            @NotNull DrawContext context,
            @NotNull String text,
            int centerX, int y,
            int color
    ) {
        TextRenderer textRenderer = mc.textRenderer;
        if (textRenderer == null) return;

        int width = textRenderer.getWidth(text);
        context.drawTextWithShadow(textRenderer, text, centerX - width / 2, y, color);
    }

    public static void drawTooltip(
            @NotNull DrawContext context,
            @NotNull String text,
            double mouseX,
            double mouseY,
            float scale
    ) {
        TextRenderer textRenderer = mc.textRenderer;
        if (textRenderer == null) return;

        String[] lines = text.split("\n");
        List<OrderedText> orderedLines = new ArrayList<>();
        int maxWidth = 0;

        for (String line : lines) {
            OrderedText ordered = Text.of(line).asOrderedText();
            orderedLines.add(ordered);
            maxWidth = Math.max(maxWidth, textRenderer.getWidth(ordered));
        }

        int x = (int) (mouseX / scale) + 8;
        int y = (int) (mouseY / scale) - 4;

        int padding = 4;
        int lineHeight = textRenderer.fontHeight + 2;
        int boxWidth = maxWidth + padding * 2;
        int boxHeight = orderedLines.size() * lineHeight + padding * 2 - 2;

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

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
        for (OrderedText line : orderedLines) {
            context.drawText(textRenderer, line, x + padding, textY, -1, true);
            textY += lineHeight;
        }
    }

    public static void drawSimpleTooltip(
            @NotNull DrawContext context,
            @NotNull String text,
            double mouseX,
            double mouseY
    ) {
        drawTooltip(context, text, mouseX, mouseY, 1.0f);
    }

    public static void drawBox(
            @NotNull DrawContext context,
            int x, int y,
            int width, int height,
            int fillColor,
            int borderColor
    ) {
        context.fill(x, y, x + width, y + height, fillColor);
        context.drawStrokedRectangle(
                x, y,
                width, height,
                borderColor
        );
    }

    public static void drawBackground(
            @NotNull DrawContext context,
            int x, int y,
            int width, int height,
            int alpha
    ) {
        int color = new Color(0, 0, 0, alpha).getRGB();
        context.fill(x, y, x + width, y + height, color);
    }

    public static void drawHighlight(
            @NotNull DrawContext context,
            int x, int y,
            int width, int height
    ) {
        int color = new Color(255, 255, 255, 30).getRGB();
        context.fill(x, y, x + width, y + height, color);
    }

    public static void drawProgressBar(
            @NotNull DrawContext context,
            int x, int y,
            int width, int height,
            float progress,
            int fillColor,
            int emptyColor
    ) {
        if (mc.currentScreen != null) return;
        progress = Math.clamp(progress, 0.0f, 1.0f);
        int filledWidth = (int) (width * progress);

        if (filledWidth < width) {
            context.fill(x + filledWidth, y, x + width, y + height, emptyColor);
        }

        if (filledWidth > 0) {
            context.fill(x, y, x + filledWidth, y + height, fillColor);
        }

        context.drawStrokedRectangle(
                x, y,
                width, height,
                0xFF3C3C3C
        );
    }

    public static void drawProgressBarAuto(
            @NotNull DrawContext context,
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
        double scaleFactor = mc.getWindow().getScaleFactor();
        double mouseX = mc.mouse.getX() / scaleFactor;
        double mouseY = mc.mouse.getY() / scaleFactor;
        return new double[]{mouseX, mouseY};
    }

    public static @NotNull TextRenderer getTextRenderer() {
        return mc.textRenderer;
    }

    public static int getFontHeight() {
        return mc.textRenderer.fontHeight;
    }

    public static int getTextWidth(@NotNull String text) {
        return mc.textRenderer.getWidth(text);
    }
}