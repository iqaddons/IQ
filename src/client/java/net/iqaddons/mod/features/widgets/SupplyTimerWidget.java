package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.events.impl.skyblock.SkyblockAreaChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.CountdownLagCompensationUtil;
import net.iqaddons.mod.utils.ScoreboardUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.iqaddons.mod.IQConstants.KUUDRA_AREA_ID;

@Slf4j
public class SupplyTimerWidget extends HudWidget {

    private static final long SUPPLY_SPAWN_COUNTDOWN_MS = 8850L;
    private static final long FULL_RATE_TICK_INTERVAL_MS = 50L;
    private static final long INSTANCE_EXIT_CONFIRMATION_MS = 1200L;

    private final SupplyStateManager supplyState = SupplyStateManager.get();

    private final List<SupplyPickupEntry> pickupHistory = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean persistUntilInstanceChange = false;
    private long supplySpawnCountdownEndMillis = -1L;
    private long lastCountdownTickMillis = -1L;
    private long pendingExitSinceMillis = -1L;

    public SupplyTimerWidget() {
        super(
                "supplyTimer",
                "Supply Timer",
                6.5f, 115.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> PhaseOneConfig.supplyTimers);
        setVisibilityCondition(() -> {
            if (!ScoreboardUtils.isInArea(KUUDRA_AREA_ID)) {
                return false;
            }

            var phase = KuudraStateManager.get().phase();
            boolean inTrackedPhases = KuudraPhase.isOneOf(
                    KuudraPhase.SUPPLIES, KuudraPhase.BUILD, KuudraPhase.EATEN,
                    KuudraPhase.STUN,  KuudraPhase.DPS, KuudraPhase.SKIP,
                    KuudraPhase.BOSS, KuudraPhase.COMPLETED
            ).test(phase);
            return inTrackedPhases || persistUntilInstanceChange;
        });

        setExampleLines(List.of(
                HudLine.of("§b§lSupply Times §8[§a4§8/§a6§8]"),
                HudLine.of("§bDarkJota §8(1/6) §f§l14.85s"),
                HudLine.of("§aPeHenrii §8(2/6) §f§l15.23s"),
                HudLine.of("§bckac10 §8(3/6) §f§l15.39s"),
                HudLine.of("§amennytb §8(4/6) §f§l16.04s"),
                HudLine.of("§bDarkJota §8(5/6) §9§l21.55s"),
                HudLine.of("§bckac10 §8(6/6) §9§l22.48s")
        ));
    }

    @Override
    protected void onActivate() {
        if (KuudraStateManager.get().phase() == KuudraPhase.SUPPLIES) {
            beginNewRunWindow();
        }

        subscribe(SupplyPlaceEvent.class, this::onSupplyPlace);
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
        subscribe(SkyblockAreaChangeEvent.class, this::onAreaChange);
        subscribe(KuudraRunEndEvent.class, this::onRunEnd);
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
        subscribe(ClientTickEvent.class, this::onTick);

        updateDisplay();
    }

    @Override
    protected void onDeactivate() {
        persistUntilInstanceChange = false;
        resetLocalState();
    }

    private void resetLocalState() {
        pickupHistory.clear();
        supplySpawnCountdownEndMillis = -1L;
        lastCountdownTickMillis = -1L;
        clearPendingExit();
    }

    private void beginNewRunWindow() {
        persistUntilInstanceChange = true;
        supplyState.startSuppliesPhase();
        resetLocalState();
        if (PhaseOneConfig.supplyTimerCountdown) {
            supplySpawnCountdownEndMillis = System.currentTimeMillis() + SUPPLY_SPAWN_COUNTDOWN_MS;
            lastCountdownTickMillis = System.currentTimeMillis();
        }
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.isEnteringKuudra()) {
            resetOnInstanceChange();
        }

        if (event.currentPhase() == KuudraPhase.SUPPLIES) {
            beginNewRunWindow();
            updateDisplay();
        }
    }

    private void onRunEnd(@NotNull KuudraRunEndEvent event) {
        if (event.isUnexpectedlyEnded()) {
            resetOnInstanceChange();
        }
    }

    private void onAreaChange(@NotNull SkyblockAreaChangeEvent event) {
        if (!event.onSkyBlock()) {
            resetOnInstanceChange();
            return;
        }

        // Ignore transient blank area updates that occur when the scoreboard changes format
        // between phases (e.g. DPS -> SKIP). A blank newArea while still on SkyBlock does
        // not mean the player left the Kuudra instance.
        if (event.newArea().isBlank()) {
            return;
        }

        boolean stillInKuudraInstance = event.newArea().contains(KUUDRA_AREA_ID);
        if (stillInKuudraInstance) {
            clearPendingExit();
            return;
        }

        armPendingExit();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) {
            return;
        }

        confirmPendingExitIfNeeded();

        long now = System.currentTimeMillis();
        if (supplySpawnCountdownEndMillis > 0L) {
            supplySpawnCountdownEndMillis = CountdownLagCompensationUtil.applyLagCompensation(
                    supplySpawnCountdownEndMillis,
                    lastCountdownTickMillis,
                    now,
                    FULL_RATE_TICK_INTERVAL_MS
            );
            lastCountdownTickMillis = now;
        } else {
            lastCountdownTickMillis = -1L;
        }

        // Tick updates are countdown-only; instance resets should come from dedicated lifecycle events.

        if (pickupHistory.isEmpty() && hasActiveSupplySpawnCountdown()) {
            updateDisplay();
            return;
        }

        if (pickupHistory.isEmpty() && supplySpawnCountdownEndMillis > 0L) {
            supplySpawnCountdownEndMillis = -1L;
            lastCountdownTickMillis = -1L;
            updateDisplay();
        }
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        if (isInstanceTransferMessage(event.getStrippedMessage())) {
            resetOnInstanceChange();
        }
    }

    private void resetOnInstanceChange() {
        if (!persistUntilInstanceChange && pickupHistory.isEmpty()) {
            return;
        }

        persistUntilInstanceChange = false;
        resetLocalState();
        updateDisplay();
    }

    private void armPendingExit() {
        if (pendingExitSinceMillis < 0L) {
            pendingExitSinceMillis = System.currentTimeMillis();
        }
    }

    private void clearPendingExit() {
        pendingExitSinceMillis = -1L;
    }

    private void confirmPendingExitIfNeeded() {
        if (pendingExitSinceMillis < 0L) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - pendingExitSinceMillis < INSTANCE_EXIT_CONFIRMATION_MS) {
            return;
        }

        if (ScoreboardUtils.isInArea(KUUDRA_AREA_ID)) {
            clearPendingExit();
            return;
        }

        clearPendingExit();
        resetOnInstanceChange();
    }

    private boolean isInstanceTransferMessage(@NotNull String message) {
        return message.contains("Sending to server")
                || (message.contains("Starting in ") && message.contains(" seconds"));
    }

    private void onSupplyPlace(@NotNull SupplyPlaceEvent event) {
        if (supplySpawnCountdownEndMillis > 0L) {
            supplySpawnCountdownEndMillis = -1L;
            lastCountdownTickMillis = -1L;
        }

        pickupHistory.add(new SupplyPickupEntry(
                event.playerName(),
                supplyState.getTimeColor(),
                event.currentSupply(),
                event.placedAt()
        ));

        updateDisplay();
    }

    private void updateDisplay() {
        clearLines();

        int totalCollected = supplyState.getSuppliesCollected();
        if (totalCollected == 0 && !pickupHistory.isEmpty()) {
            totalCollected = pickupHistory.size();
        }

        addLine(HudLine.of(String.format(
                "§b§lSupply Times §8[%s%d§8/§a6§8]",
                totalCollected >= 6 ? "§a" : "§e",
                totalCollected
        )));

        if (pickupHistory.isEmpty()) {
            if (hasActiveSupplySpawnCountdown()) {
                long remainingMs = Math.max(0L, supplySpawnCountdownEndMillis - System.currentTimeMillis());
                addLine(HudLine.of(String.format("§7Spawning in: %s%.2fs", getSupplySpawnCountdownColor(remainingMs), remainingMs / 1000.0)));
            } else {
                addLine(HudLine.of("§7No placed supplies yet..."));
            }
            markDimensionsDirty();
            return;
        }

        for (SupplyPickupEntry entry : pickupHistory) {
            addLine(HudLine.of(String.format(
                    "%s §8(%d/6) %s%.2fs",
                    entry.playerName(), entry.supplyNumber(),
                    entry.color, entry.pickupAt()
            )));
        }

        markDimensionsDirty();
    }

    private boolean hasActiveSupplySpawnCountdown() {
        if (!PhaseOneConfig.supplyTimerCountdown) {
            return false;
        }

        return supplySpawnCountdownEndMillis > System.currentTimeMillis();
    }

    private @NotNull String getSupplySpawnCountdownColor(long remainingMs) {
        double ratio = Math.min(1.0, Math.max(0.0, (double) remainingMs / SUPPLY_SPAWN_COUNTDOWN_MS));
        if (ratio > 0.75) {
            return "§a";
        }
        if (ratio > 0.50) {
            return "§e";
        }
        if (ratio > 0.25) {
            return "§6";
        }
        return "§c";
    }

    private record SupplyPickupEntry(
            String playerName,
            String color,
            int supplyNumber,
            double pickupAt
    ) {
    }
}