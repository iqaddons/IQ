package net.iqaddons.mod.state;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.KuudraPhaseChangeEvent;
import net.iqaddons.mod.state.data.KuudraPhase;
import net.iqaddons.mod.state.data.KuudraRun;
import net.iqaddons.mod.state.data.KuudraTier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages Kuudra run state with proper encapsulation.
 * Single source of truth for all Kuudra-related state.
 *
 * <p>Thread-safe for read operations. Write operations should only
 * occur from the main client thread via trackers.
 */
@Slf4j
public final class KuudraStateManager {

    private static final KuudraStateManager INSTANCE = new KuudraStateManager();

    private volatile KuudraPhase currentPhase = KuudraPhase.NONE;
    private volatile KuudraTier currentTier = KuudraTier.NONE;

    private Instant phaseStartTime;
    private Instant runStartTime;
    private final Map<KuudraPhase, Duration> phaseDurations = new EnumMap<>(KuudraPhase.class);

    private KuudraStateManager() {}

    public static KuudraStateManager get() {
        return INSTANCE;
    }

    public @NotNull KuudraPhase phase() {
        return currentPhase;
    }

    public @NotNull KuudraTier tier() {
        return currentTier;
    }

    public boolean isInKuudra() {
        return currentPhase != KuudraPhase.NONE;
    }

    public boolean isInRun() {
        return currentPhase.isInRun();
    }

    public Optional<Duration> currentPhaseDuration() {
        if (phaseStartTime == null) return Optional.empty();
        return Optional.of(Duration.between(phaseStartTime, Instant.now()));
    }

    public Optional<Duration> runDuration() {
        if (runStartTime == null) return Optional.empty();
        return Optional.of(Duration.between(runStartTime, Instant.now()));
    }

    public @NotNull Optional<Duration> getPhaseDuration(@NotNull KuudraPhase phase) {
        return Optional.ofNullable(phaseDurations.get(phase));
    }

    public boolean setPhase(@NotNull KuudraPhase newPhase) {
        if (currentPhase == newPhase) return false;
        if (!currentPhase.canTransitionTo(newPhase)) {
            log.warn("Invalid phase transition: {} -> {}", currentPhase, newPhase);
            return false;
        }

        KuudraPhase previous = currentPhase;
        long phaseDurationMs = recordPhaseEnd();

        currentPhase = newPhase;
        phaseStartTime = Instant.now();

        if (newPhase == KuudraPhase.SUPPLIES && previous == KuudraPhase.QUEUE) {
            runStartTime = phaseStartTime;
            phaseDurations.clear();
        }

        log.info("Phase: {} -> {} ({}ms)", previous.getDisplayName(), newPhase.getDisplayName(), phaseDurationMs);
        EventBus.post(new KuudraPhaseChangeEvent(previous, newPhase, phaseDurationMs));

        sendPhaseChangeMessage(previous, newPhase, phaseDurationMs);

        return true;
    }

    private void sendPhaseChangeMessage(@NotNull KuudraPhase previous, @NotNull KuudraPhase newPhase, long durationMs) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String durationText = durationMs > 0 ? String.format(" (%.2fs)", durationMs / 1000.0) : "";

        Text message = Text.literal("[IQ] ")
                .formatted(Formatting.GOLD)
                .append(Text.literal("Phase: ")
                        .formatted(Formatting.GRAY))
                .append(Text.literal(previous.getDisplayName())
                        .formatted(Formatting.RED))
                .append(Text.literal(" → ")
                        .formatted(Formatting.GRAY))
                .append(Text.literal(newPhase.getDisplayName())
                        .formatted(Formatting.GREEN))
                .append(Text.literal(durationText)
                        .formatted(Formatting.YELLOW));

        client.player.sendMessage(message, false);
    }

    public void setTier(@NotNull KuudraTier tier) {
        if (currentTier != tier) {
            currentTier = tier;
            log.info("Tier set: {}", tier.getDisplayName());
        }
    }

    public void reset() {
        if (currentPhase != KuudraPhase.NONE) {
            KuudraPhase previous = currentPhase;
            currentPhase = KuudraPhase.NONE;
            currentTier = KuudraTier.NONE;
            phaseStartTime = null;
            runStartTime = null;
            phaseDurations.clear();

            log.info("State reset from {}", previous.getDisplayName());
            EventBus.post(new KuudraPhaseChangeEvent(previous, KuudraPhase.NONE, 0));
        }
    }

    public Optional<KuudraRun> buildRunSnapshot() {
        if (runStartTime == null) return Optional.empty();

        return Optional.of(new KuudraRun(
                currentTier,
                runStartTime,
                Map.copyOf(phaseDurations)
        ));
    }

    private long recordPhaseEnd() {
        if (phaseStartTime == null || !currentPhase.isInRun()) {
            return 0;
        }

        Duration duration = Duration.between(phaseStartTime, Instant.now());
        phaseDurations.put(currentPhase, duration);
        return duration.toMillis();
    }
}


