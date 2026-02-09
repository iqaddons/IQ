package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.manager.state.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.TimeUtils;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class FreshCountdownWidget extends HudWidget {

    private static final long FRESH_DURATION_MS = 10_000;

    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private long freshStartTime = 0;
    private boolean freshActive = false;

    private EventBus.Subscription<PlayerFreshEvent> playerFreshSubscription;
    private EventBus.Subscription<ClientTickEvent> tickSubscription;

    private final HudLine countdownLine;

    public FreshCountdownWidget() {
        super(
                "freshCountdown",
                "Fresh Countdown",
                10.0f, 220.0f,
                1.5f,
                HudAnchor.TOP_LEFT
        );

        countdownLine = HudLine.of("§a10.0s");

        setEnabledSupplier(() -> PhaseTwoConfig.freshCountdown);
        setVisibilityCondition(() -> freshActive && stateManager.phase() == KuudraPhase.BUILD);

        setExampleLines(List.of(
                HudLine.of("§a8.5s")
        ));
    }

    @Override
    protected void onActivate() {
        freshActive = false;
        freshStartTime = 0;

        clearLines();
        addLine(countdownLine);

        playerFreshSubscription = EventBus.subscribe(PlayerFreshEvent.class, this::onPlayerFresh);
        tickSubscription = EventBus.subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onDeactivate() {
        if (playerFreshSubscription != null) {
            playerFreshSubscription.unsubscribe();
            playerFreshSubscription = null;
        }
        if (tickSubscription != null) {
            tickSubscription.unsubscribe();
            tickSubscription = null;
        }

        freshActive = false;
    }

    private void onPlayerFresh(@NotNull PlayerFreshEvent event) {
        freshStartTime = event.freshAt();
        freshActive = true;

        updateDisplay();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!freshActive) return;
        if (!event.isNthTick(2)) return;

        updateDisplay();
    }

    private void updateDisplay() {
        long elapsed = System.currentTimeMillis() - freshStartTime;
        long remaining = FRESH_DURATION_MS - elapsed;

        if (remaining <= 0) {
            freshActive = false;
            return;
        }

        countdownLine.text(getCountdownColor(remaining) + TimeUtils.formatTime(remaining));
        markDimensionsDirty();
        log.info("Fresh countdown updated: {}s remaining", remaining);
    }

    private @NotNull String getCountdownColor(double remaining) {
        double remainingSeconds = remaining / 1000;
        if (remainingSeconds > 6.0) return "§a";
        else if (remainingSeconds > 3.0) return "§e";
        else return "§c";
    }
}