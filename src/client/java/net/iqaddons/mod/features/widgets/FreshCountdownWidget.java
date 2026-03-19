package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.events.impl.skyblock.SkyblockAreaChangeEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.TimeUtils;
import org.jetbrains.annotations.NotNull;

import static net.iqaddons.mod.IQConstants.KUUDRA_AREA_ID;

@Slf4j
public class FreshCountdownWidget extends HudWidget {

    private static final long FRESH_DURATION_MS = 10_000;

    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private long freshStartTime = 0;
    private boolean freshActive = false;

    private final HudLine countdownLine;

    public FreshCountdownWidget() {
        super(
                "freshCountdown",
                "Fresh Countdown",
                460.0f, 90.0f,
                1.5f,
                HudAnchor.TOP_LEFT
        );

        countdownLine = HudLine.of("§a8.5s");

        setEnabledSupplier(() -> PhaseTwoConfig.freshCountdown);
        setVisibilityCondition(() -> freshActive && stateManager.phase() == KuudraPhase.BUILD);

        setExampleLines(HudLine.of("§a8.5s"));
    }

    @Override
    protected void onActivate() {
        freshActive = false;
        freshStartTime = 0;

        clearLines();
        addLine(countdownLine);

        subscribe(PlayerFreshEvent.class, this::onPlayerFresh);
        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);
        subscribe(SkyblockAreaChangeEvent.class, this::onAreaChange);
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
    }

    @Override
    protected void onDeactivate() {
        resetFreshState();
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

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        if (event.isUnexpectedlyEnded()) {
            resetFreshState();
        }
    }

    private void onAreaChange(@NotNull SkyblockAreaChangeEvent event) {
        boolean stillInKuudraInstance = event.onSkyBlock() && event.newArea().contains(KUUDRA_AREA_ID);
        if (!stillInKuudraInstance) {
            resetFreshState();
        }
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        if (isInstanceTransferMessage(event.getStrippedMessage())) {
            resetFreshState();
        }
    }

    private void updateDisplay() {
        long elapsed = System.currentTimeMillis() - freshStartTime;
        long remaining = FRESH_DURATION_MS - elapsed;
        if (remaining <= 0) {
            resetFreshState();
            return;
        }

        countdownLine.text(getCountdownColor(remaining) + TimeUtils.formatTime(remaining));
        markDimensionsDirty();
        log.debug("Fresh countdown updated: {}s remaining", remaining);
    }

    private void resetFreshState() {
        freshActive = false;
        freshStartTime = 0;
        countdownLine.text(getCountdownColor(FRESH_DURATION_MS) + TimeUtils.formatTime(FRESH_DURATION_MS));
        markDimensionsDirty();
    }

    private boolean isInstanceTransferMessage(@NotNull String message) {
        return message.contains("Sending to server") || message.contains("Starting in 4 seconds...");
    }

    private @NotNull String getCountdownColor(double remaining) {
        double remainingSeconds = remaining / 1000;
        if (remainingSeconds > 6.0) return "§a";
        else if (remainingSeconds > 3.0) return "§e";
        else return "§c";
    }
}