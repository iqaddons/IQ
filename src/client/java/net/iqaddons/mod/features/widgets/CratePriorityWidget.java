package net.iqaddons.mod.features.widgets;

import net.iqaddons.mod.IQConstants;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.CratePriorityHudEvent;
import net.iqaddons.mod.hud.HudManager;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.utils.HudRenderer;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.TextColor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class CratePriorityWidget extends HudWidget {

    private static final int FADE_TICKS = 4;
    private static final int SLIDE_OFFSET_PIXELS = 8;
    private static final String MIN_REFERENCE_TEXT = "§lGO X CANNON";

    private @NotNull String text = "";
    private int totalTicks = 0;
    private int ticksRemaining = 0;

    public CratePriorityWidget() {
        super(
                "crate_priority",
                "Crate Priority",
                0f,
                120f,
                2.0f,
                HudAnchor.CENTER
        );

        setEnabledSupplier(() -> PhaseOneConfig.cratePriority);
        setVisibilityCondition(() ->
                ScoreboardUtils.isInArea(IQConstants.KUUDRA_AREA_ID)
                        || HudManager.get().isEditorOpen()
        );
    }

    @Override
    protected void onActivate() {
        reset();
        subscribe(CratePriorityHudEvent.class, this::onPriorityUpdate);
        subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onDeactivate() {
        reset();
    }

    private void onPriorityUpdate(@NotNull CratePriorityHudEvent event) {
        text = event.text();
        totalTicks = Math.max(1, event.durationTicks());
        ticksRemaining = totalTicks;
        markDimensionsDirty();

        if (PhaseOneConfig.CratePriorityConfig.cratePrioritySound && mc.player != null) {
            mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.6f, 1.0f);
        }
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || ticksRemaining <= 0) {
            return;
        }

        ticksRemaining--;
        if (ticksRemaining == 0) {
            text = "";
            totalTicks = 0;
            markDimensionsDirty();
        }
    }

    @Override
    public void render(@NotNull GuiGraphicsExtractor context, double mouseX, double mouseY, float delta) {
        renderWidget(context, mouseX, mouseY, false);
    }

    @Override
    public void renderExample(@NotNull GuiGraphicsExtractor context, double mouseX, double mouseY, float delta) {
        renderWidget(context, mouseX, mouseY, true);
    }

    private void renderWidget(@NotNull GuiGraphicsExtractor context, double mouseX, double mouseY, boolean preview) {
        String renderText = getRenderText(preview);
        if (renderText.isEmpty()) {
            return;
        }

        var textRenderer = mc.font;
        if (textRenderer == null) {
            return;
        }

        float alpha = preview ? 1.0f : getAlpha();
        float slideOffset = preview ? 0.0f : getSlideOffset();

        int textArgb = toArgb(PhaseOneConfig.CratePriorityConfig.cratePriorityColor, alpha);

        float scale = getScale();
        int widgetWidth = getWidth();
        int widgetHeight = getHeight();

        float scaledX = getAbsoluteX() / scale;
        float scaledY = (getAbsoluteY() / scale) - slideOffset;
        int centerX = Math.round(scaledX + (widgetWidth / 2.0f));
        int textY = Math.round(scaledY);

        context.pose().pushMatrix();
        context.pose().scale(scale, scale);

        if (preview) {
            renderEditorOverlay(context, mouseX, mouseY, textRenderer, (int) scaledX, textY, widgetWidth, widgetHeight);
        }

        HudRenderer.drawCenteredText(context, renderText, centerX, textY, textArgb);

        context.pose().popMatrix();
    }

    @Override
    public int getWidth() {
        var textRenderer = mc.font;
        if (textRenderer == null) {
            return 20;
        }

        int referenceWidth = textRenderer.width(MIN_REFERENCE_TEXT);
        int dynamicWidth = text.isEmpty() ? 0 : textRenderer.width(text);
        return Math.max(referenceWidth, dynamicWidth);
    }

    @Override
    public int getHeight() {
        var textRenderer = mc.font;
        if (textRenderer == null) {
            return 1;
        }

        return textRenderer.lineHeight;
    }

    private float getAlpha() {
        if (PhaseOneConfig.CratePriorityConfig.cratePriorityAnimation == PhaseOneConfig.CratePriorityAnimation.NONE) {
            return 1.0f;
        }

        if (totalTicks <= 0) {
            return 0.0f;
        }

        int elapsed = totalTicks - ticksRemaining;
        float fadeIn = Math.min(1.0f, elapsed / (float) FADE_TICKS);
        float fadeOut = Math.min(1.0f, ticksRemaining / (float) FADE_TICKS);
        return clamp01(Math.min(fadeIn, fadeOut));
    }

    private float getSlideOffset() {
        if (PhaseOneConfig.CratePriorityConfig.cratePriorityAnimation != PhaseOneConfig.CratePriorityAnimation.SLIDE || totalTicks <= 0) {
            return 0.0f;
        }

        int elapsed = totalTicks - ticksRemaining;
        float progress = clamp01(elapsed / (float) FADE_TICKS);
        return (1.0f - progress) * SLIDE_OFFSET_PIXELS;
    }

    private int toArgb(@NotNull TextColor color, float alpha) {
        int rgb = switch (color) {
            case BLACK -> 0x000000;
            case DARK_BLUE -> 0x0000AA;
            case DARK_GREEN -> 0x00AA00;
            case DARK_AQUA -> 0x00AAAA;
            case DARK_RED -> 0xAA0000;
            case DARK_PURPLE -> 0xAA00AA;
            case GOLD -> 0xFFAA00;
            case GRAY -> 0xAAAAAA;
            case DARK_GRAY -> 0x555555;
            case BLUE -> 0x5555FF;
            case GREEN -> 0x55FF55;
            case AQUA -> 0x55FFFF;
            case RED -> 0xFF5555;
            case LIGHT_PURPLE -> 0xFF55FF;
            case YELLOW -> 0xFFFF55;
            case WHITE -> 0xFFFFFF;
        };

        int alphaByte = Math.max(0, Math.min(255, (int) (clamp01(alpha) * 255.0f)));
        return (alphaByte << 24) | rgb;
    }

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private @NotNull String getRenderText(boolean preview) {
        if (preview) {
            return text.isEmpty() ? MIN_REFERENCE_TEXT : text;
        }

        if (ticksRemaining <= 0) {
            return "";
        }

        return text;
    }

    private void renderEditorOverlay(
            @NotNull GuiGraphicsExtractor context,
            double mouseX,
            double mouseY,
            @NotNull Font textRenderer,
            int x,
            int y,
            int width,
            int height
    ) {
        if (isSelected()) {
            renderSelectionBorder(context, textRenderer, x, y, width, height);
        }

        if (isMouseOver(mouseX, mouseY)) {
            context.fill(x, y, x + width, y + height, new Color(0, 0, 0, 100).getRGB());
        }
    }

    private void renderSelectionBorder(
            @NotNull GuiGraphicsExtractor context,
            @NotNull Font textRenderer,
            int x,
            int y,
            int width,
            int height
    ) {
        int borderColor = new Color(255, 0, 0, 170).getRGB();

        context.fill(x, y, x + width, y + 1, borderColor);
        context.fill(x, y + height - 1, x + width, y + height, borderColor);
        context.fill(x, y, x + 1, y + height, borderColor);
        context.fill(x + width - 1, y, x + width, y + height, borderColor);

        String widgetName = getDisplayName();
        String widgetLocation = String.format("X: %.0f Y: %.0f", getX(), getY());

        int nameX = x + (width - textRenderer.width(widgetName)) / 2;
        int locationX = x + (width - textRenderer.width(widgetLocation)) / 2;

        context.text(
                textRenderer,
                widgetName,
                nameX,
                y - textRenderer.lineHeight - 2,
                new Color(255, 255, 255, 220).getRGB()
        );

        context.text(
                textRenderer,
                widgetLocation,
                locationX,
                y + height + 2,
                new Color(255, 255, 255, 200).getRGB()
        );
    }

    private void reset() {
        text = "";
        totalTicks = 0;
        ticksRemaining = 0;
        markDimensionsDirty();
    }
}


