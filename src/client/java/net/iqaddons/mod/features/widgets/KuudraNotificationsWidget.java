package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.IQConstants;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.HudNotificationEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class KuudraNotificationsWidget extends HudWidget {

    private String currentNotification = "";
    private int ticksRemaining = 0;

    private final HudLine notificationLine;

    public KuudraNotificationsWidget() {
        super(
                "kuudra_notifications",
                "Kuudra Notifications",
                440f,
                280f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        notificationLine = HudLine.of("§c§lNOTIFICATION")
                .showWhen(() -> !currentNotification.isEmpty());

        setEnabledSupplier(() -> true);
        setVisibilityCondition(() ->
                ScoreboardUtils.isInArea(IQConstants.KUUDRA_AREA_ID)
        );

        setExampleLines(HudLine.of("§e§lSOS REMINDER"));
    }

    @Override
    protected void onActivate() {
        currentNotification = "";
        ticksRemaining = 0;

        clearLines();
        addLine(notificationLine);

        subscribe(HudNotificationEvent.class, this::onNotification);
        subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onDeactivate() {
        currentNotification = "";
        ticksRemaining = 0;
    }

    private void onNotification(@NotNull HudNotificationEvent event) {
        if (mc.player == null) return;

        currentNotification = event.text();
        ticksRemaining = Math.max(event.durationTicks(), 1);

        if (KuudraGeneralConfig.kuudraNotificationsSound) {
            mc.player.playSound(
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                    2.0f, 1.0f
            );
        }

        updateDisplay();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        if (ticksRemaining > 0) {
            ticksRemaining--;
            if (ticksRemaining == 0) {
                currentNotification = "";
            }
        }

        updateDisplay();
    }

    private void updateDisplay() {
        if (ticksRemaining <= 0) return;

        notificationLine.text(currentNotification);
        markDimensionsDirty();
    }
}
