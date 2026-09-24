package net.iqaddons.mod.features.widgets;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.ServerUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class SupplyTimerCountdownWidget extends HudWidget {

    private static final long SUPPLY_SPAWN_COUNTDOWN_MS = 8850L;
    private static final float MIN_EFFECTIVE_TPS = 5.0f;
    private static final float MAX_EFFECTIVE_TPS = 20.0f;

    private final KuudraStateManager stateManager = KuudraStateManager.get();
    private final HudLine countdownLine;

    private long countdownEndMillis = -1L;
    private long lastCountdownTickMillis = -1L;
    private double remainingServerMillis = 0.0;

    public SupplyTimerCountdownWidget() {
        super(
                "supplyTimerCountdownWidget",
                "Supply Timer Countdown Widget",
                414.0f, 365.9712f,
                1.6000001f,
                HudAnchor.TOP_LEFT
        );

        countdownLine = HudLine.of("§b§lSupplies: §a§l8.85s")
                .showWhen(this::hasActiveCountdown);

        setEnabledSupplier(() -> PhaseOneConfig.SupplyTimesConfig.countdownWidget);
        setVisibilityCondition(() -> stateManager.phase() == KuudraPhase.SUPPLIES);
        setExampleLines(HudLine.of("§b§lSupplies: §e§l8.52s"));
    }

    @Override
    protected void onActivate() {
        clearLines();
        addLine(countdownLine);

        if (stateManager.phase() == KuudraPhase.SUPPLIES && countdownEndMillis < 0L) {
            startCountdown();
        }

        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(SupplyPlaceEvent.class, event -> clearCountdown());
        subscribe(KuudraPhaseChangeEvent.class, this::onPhaseChange);
    }

    @Override
    protected void onDeactivate() {
        clearCountdown();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        long now = System.currentTimeMillis();
        if (countdownEndMillis > 0L) {
            updateServerClockCountdown(now);
            lastCountdownTickMillis = now;
        } else {
            lastCountdownTickMillis = -1L;
        }

        if (hasActiveCountdown()) {
            updateCountdownLine();
            return;
        }

        if (countdownEndMillis > 0L) {
            clearCountdown();
        }
    }

    private void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (event.currentPhase() == KuudraPhase.SUPPLIES) {
            startCountdown();
            return;
        }

        clearCountdown();
    }

    private void startCountdown() {
        long now = System.currentTimeMillis();
        remainingServerMillis = SUPPLY_SPAWN_COUNTDOWN_MS;
        countdownEndMillis = now + SUPPLY_SPAWN_COUNTDOWN_MS;
        lastCountdownTickMillis = now;
        updateCountdownLine();
    }

    private void clearCountdown() {
        countdownEndMillis = -1L;
        lastCountdownTickMillis = -1L;
        remainingServerMillis = 0.0;
        countdownLine.text("");
        markDimensionsDirty();
    }

    private void updateServerClockCountdown(long now) {
        if (lastCountdownTickMillis <= 0L || now <= lastCountdownTickMillis) {
            return;
        }

        long elapsedMillis = now - lastCountdownTickMillis;
        float effectiveTps = Math.clamp(ServerUtils.getAverageTps(), MIN_EFFECTIVE_TPS, MAX_EFFECTIVE_TPS);
        double serverProgressMillis = elapsedMillis * (effectiveTps / MAX_EFFECTIVE_TPS);
        remainingServerMillis = Math.max(0.0, remainingServerMillis - serverProgressMillis);
        countdownEndMillis = now + (long) Math.ceil(remainingServerMillis);
    }

    private void updateCountdownLine() {
        long remainingMs = Math.max(0L, countdownEndMillis - System.currentTimeMillis());
        String color = getCountdownColor(remainingMs);
        if (color == null) color = "§c";

        countdownLine.text(String.format(
                "§b§lSupplies: %s§l%ss",
                color,
                formatCountdownSeconds(remainingMs)
        ));
        markDimensionsDirty();
    }

    private boolean hasActiveCountdown() {
        return countdownEndMillis > System.currentTimeMillis();
    }

    private @Nullable String getCountdownColor(long remainingMs) {
        if (remainingMs <= 0) return null;

        double ratio = Math.min(1.0, Math.max(0.0, (double) remainingMs / SUPPLY_SPAWN_COUNTDOWN_MS));
        if (ratio > 0.75) return "§a";
        if (ratio > 0.50) return "§e";
        if (ratio > 0.25) return "§6";
        return "§c";
    }

    private @NotNull String formatCountdownSeconds(long remainingMs) {
        double seconds = Math.max(0L, remainingMs) / 1000.0;
        return String.format(Locale.ROOT, "%.2f", seconds);
    }
}
