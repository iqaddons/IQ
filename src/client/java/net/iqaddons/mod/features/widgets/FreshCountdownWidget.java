package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.state.KuudraStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.hud.component.HudLine;
import net.iqaddons.mod.utils.hud.element.HudAnchor;
import net.iqaddons.mod.utils.hud.element.HudWidget;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class FreshCountdownWidget extends HudWidget {

    private static final String FRESH_TOOLS_MESSAGE = "Your Fresh Tools Perk bonus doubles your building speed for the next 10 seconds!";
    private static final long FRESH_DURATION_MS = 10_000;

    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private long freshStartTime = 0;
    private boolean freshActive = false;

    private EventBus.Subscription<ChatReceivedEvent> chatSubscription;
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

        chatSubscription = EventBus.subscribe(ChatReceivedEvent.class, this::onChat);
        tickSubscription = EventBus.subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onDeactivate() {
        if (chatSubscription != null) {
            chatSubscription.unsubscribe();
            chatSubscription = null;
        }
        if (tickSubscription != null) {
            tickSubscription.unsubscribe();
            tickSubscription = null;
        }

        freshActive = false;
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();

        if (message.contains(FRESH_TOOLS_MESSAGE)) {
            freshStartTime = System.currentTimeMillis();
            freshActive = true;
            updateDisplay();
        }
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!freshActive) return;
        if (!event.isNthTick(2)) return;

        long elapsed = System.currentTimeMillis() - freshStartTime;
        long remaining = FRESH_DURATION_MS - elapsed;

        if (remaining <= 0) {
            freshActive = false;
            return;
        }

        updateDisplay();
    }

    private void updateDisplay() {
        long elapsed = System.currentTimeMillis() - freshStartTime;
        long remaining = FRESH_DURATION_MS - elapsed;

        if (remaining <= 0) {
            freshActive = false;
            return;
        }

        double remainingSeconds = remaining / 1000.0;
        String color = getCountdownColor(remainingSeconds);

        countdownLine.text(String.format("%s%.1fs", color, remainingSeconds));
        markDimensionsDirty();
    }

    private @NotNull String getCountdownColor(double remainingSeconds) {
        if (remainingSeconds > 6.0) {
            return "§a";
        } else if (remainingSeconds > 3.0) {
            return "§e";
        } else {
            return "§c";
        }
    }
}