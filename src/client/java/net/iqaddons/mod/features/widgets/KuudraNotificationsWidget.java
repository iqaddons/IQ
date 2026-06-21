package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.IQConstants;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.HudNotificationEvent;
import net.iqaddons.mod.hud.HudManager;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class KuudraNotificationsWidget extends HudWidget {

    private static final int FADE_IN_TICKS = 4;
    private static final int FADE_OUT_TICKS = 4;
    private static final String MIN_REFERENCE_TEXT = "§l§lNOTIFICATIONS";

    private int displayTicksRemaining = 0;
    private int fadeTicksRemaining = 0;
    private FadeState fadeState = FadeState.HIDDEN;

    private final HudLine notificationLine;

    public KuudraNotificationsWidget() {
        super(
                "kuudra_notifications",
                "Kuudra Notifications",
                0f,
                80f,
                1.0f,
                HudAnchor.CENTER
        );

        notificationLine = HudLine.of("§c§lLOADING NOTIFICATIONS..")
                .showWhen(() -> fadeState != FadeState.HIDDEN);

        setEnabledSupplier(() -> true);
        setVisibilityCondition(() ->
                ScoreboardUtils.isInArea(IQConstants.KUUDRA_AREA_ID)
        );

        setExampleLines(HudLine.of("§e§lNOTIFICATIONS"));
    }

    @Override
    protected void onActivate() {
        resetNotification();

        clearLines();
        addLine(notificationLine);

        subscribe(HudNotificationEvent.class, this::onNotification);
        subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onDeactivate() {
        resetNotification();
    }

    private void onNotification(@NotNull HudNotificationEvent event) {
        notificationLine.text(event.text());
        displayTicksRemaining = Math.max(event.durationTicks(), 1);
        fadeTicksRemaining = FADE_IN_TICKS;
        fadeState = FadeState.FADING_IN;
        markDimensionsDirty();

        if (mc.player != null) {
            var soundEvent = event.soundEvent() != null
                    ? event.soundEvent()
                    : SoundEvents.NOTE_BLOCK_PLING.value();

            mc.player.playSound(soundEvent, 2.0f, 1.0f);
        }
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        switch (fadeState) {
            case FADING_IN -> {
                if (fadeTicksRemaining > 0) {
                    fadeTicksRemaining--;
                }

                if (fadeTicksRemaining <= 0) {
                    fadeState = FadeState.VISIBLE;
                    fadeTicksRemaining = 0;
                }
            }
            case VISIBLE -> {
                if (displayTicksRemaining > 0) {
                    displayTicksRemaining--;
                }

                if (displayTicksRemaining <= 0) {
                    fadeState = FadeState.FADING_OUT;
                    fadeTicksRemaining = FADE_OUT_TICKS;
                }
            }
            case FADING_OUT -> {
                if (fadeTicksRemaining > 0) {
                    fadeTicksRemaining--;
                }

                if (fadeTicksRemaining <= 0) {
                    resetNotification();
                }
            }
            case HIDDEN -> {
                // Nothing to update while hidden.
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphicsExtractor context, double mouseX, double mouseY, float a) {
        if (HudManager.get().isEditorOpen()) {
            super.render(context, mouseX, mouseY, a);
            return;
        }

        if (fadeState == FadeState.HIDDEN || notificationLine.getText().isEmpty()) {
            return;
        }

        float alpha = getFadeAlpha();
        var textRenderer = mc.font;
        if (textRenderer == null) return;

        int widgetWidth = getWidth();
        int textWidth = notificationLine.getWidth(textRenderer);
        float centeredOffsetX = Math.max(0, (widgetWidth - textWidth) / 2.0f);

        float scale = getScale();
        float scaledX = (getAbsoluteX() / scale) + centeredOffsetX;
        float scaledY = getAbsoluteY() / scale;
        int alphaByte = Math.max(0, Math.min(255, (int) (alpha * 255.0f)));
        int color = (alphaByte << 24) | 0xFFFFFF;

        context.pose().pushMatrix();
        context.pose().scale(scale, scale);
        context.text(
                textRenderer,
                notificationLine.getOrderedText(),
                (int) scaledX,
                (int) scaledY,
                color,
                true
        );
        context.pose().popMatrix();
    }

    private float getFadeAlpha() {
        return switch (fadeState) {
            case FADING_IN -> {
                float progress = 1.0f - (fadeTicksRemaining / (float) FADE_IN_TICKS);
                yield clamp01(progress);
            }
            case FADING_OUT -> {
                float progress = fadeTicksRemaining / (float) FADE_OUT_TICKS;
                yield clamp01(progress);
            }
            case VISIBLE -> 1.0f;
            case HIDDEN -> 0.0f;
        };
    }

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    @Override
    public int getWidth() {
        int baseWidth = super.getWidth();
        var textRenderer = mc.font;
        if (textRenderer == null) {
            return baseWidth;
        }

        return Math.max(baseWidth, textRenderer.width(MIN_REFERENCE_TEXT));
    }

    private void resetNotification() {
        displayTicksRemaining = 0;
        fadeTicksRemaining = 0;
        fadeState = FadeState.HIDDEN;
        notificationLine.text("");
        markDimensionsDirty();
    }

    private enum FadeState {
        HIDDEN,
        FADING_IN,
        VISIBLE,
        FADING_OUT
    }
}
