package net.iqaddons.mod.features.widgets;

import net.iqaddons.mod.IQConstants;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.CratePriorityHudEvent;
import net.iqaddons.mod.hud.HudManager;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.TextColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.NotNull;

public class CratePriorityWidget extends HudWidget {

    private static final int FADE_TICKS = 4;
    private static final int SLIDE_OFFSET_PIXELS = 8;
    private static final String MIN_REFERENCE_TEXT = "Go X Cannon";

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

        setExampleLines(HudLine.of("§eGo Shop"));
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

        if (PhaseOneConfig.cratePrioritySound && mc.player != null) {
            mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.6f, 1.0f);
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
    public void render(@NotNull DrawContext context, double mouseX, double mouseY, float delta) {
        if (HudManager.get().isEditorOpen()) {
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        if (text.isEmpty() || ticksRemaining <= 0) {
            return;
        }

        var textRenderer = mc.textRenderer;
        if (textRenderer == null) {
            return;
        }

        float alpha = getAlpha();
        float slideOffset = getSlideOffset();

        int argb = toArgb(PhaseOneConfig.cratePriorityColor, alpha);

        float scale = getScale();
        int textWidth = textRenderer.getWidth(text);
        int widgetWidth = getWidth();

        float scaledX = (getAbsoluteX() / scale) + Math.max(0, (widgetWidth - textWidth) / 2.0f);
        float scaledY = (getAbsoluteY() / scale) - slideOffset;

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);
        context.drawText(textRenderer, text, (int) scaledX, (int) scaledY, argb, true);
        context.getMatrices().popMatrix();
    }

    @Override
    public int getWidth() {
        int baseWidth = super.getWidth();
        var textRenderer = mc.textRenderer;
        if (textRenderer == null) {
            return baseWidth;
        }

        int dynamicWidth = text.isEmpty() ? 0 : textRenderer.getWidth(text);
        return Math.max(baseWidth, Math.max(dynamicWidth, textRenderer.getWidth(MIN_REFERENCE_TEXT)));
    }

    private float getAlpha() {
        if (PhaseOneConfig.cratePriorityAnimation == PhaseOneConfig.CratePriorityAnimation.NONE) {
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
        if (PhaseOneConfig.cratePriorityAnimation != PhaseOneConfig.CratePriorityAnimation.SLIDE || totalTicks <= 0) {
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

    private void reset() {
        text = "";
        totalTicks = 0;
        ticksRemaining = 0;
        markDimensionsDirty();
    }
}


