package net.iqaddons.mod.manager;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.SubscriptionOwner;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.events.impl.skyblock.SkyblockAreaChangeEvent;
import net.iqaddons.mod.model.kuudra.validator.KuudraStateValidator;
import net.iqaddons.mod.model.kuudra.KuudraContext;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static net.iqaddons.mod.IQConstants.DEFAULT_CHECK_INTERVAL_TICKS;
import static net.iqaddons.mod.IQConstants.KUUDRA_AREA_ID;

@Slf4j
public final class KuudraStateManager extends SubscriptionOwner {

    private static KuudraStateManager instance;

    private final AtomicReference<KuudraContext> contextRef = new AtomicReference<>(KuudraContext.empty());
    private final KuudraStateValidator validator = new KuudraStateValidator();
    private final Map<KuudraPhase, Duration> phaseDurations = new EnumMap<>(KuudraPhase.class);

    public void start() {
        subscribe(ClientTickEvent.class, this::onClientTick);
        EventBus.subscribe(ChatReceivedEvent.class, this::onChatReceived);
        EventBus.subscribe(SkyblockAreaChangeEvent.class, this::onSkyBlockAreaChange);

        instance = this;
    }

    private void onClientTick(@NotNull ClientTickEvent event) {
        if (!event.isNthTick(DEFAULT_CHECK_INTERVAL_TICKS)) return;

        KuudraContext current = contextRef.get();
        if (current.phase() == KuudraPhase.NONE) {
            return;
        }

        KuudraStateValidator.ValidationResult result = validator.validate(current);
        if (result.isValid()) {
            KuudraContext validated = current.validated();
            contextRef.compareAndSet(current, validated);
            log.trace("Validation passed for phase {}", current.phase());
        } else {
            forceReset("Validation failures: " + result.reason());
        }
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        KuudraPhase detected = KuudraPhase.fromMessage(message);
        if (detected == null) return;
        log.debug("Detected phase trigger in chat: {} -> {}", message.substring(0, Math.min(50, message.length())), detected);

        if (detected == KuudraPhase.NONE) {
            if (isInKuudra()) {
                forceReset("Exit message detected: " + StringUtils.getShortMessage(message));
            }

            return;
        }

        setPhase(detected);

        if (message.contains("Sending to server") || message.contains("Starting in 5 seconds...")) {
            forceReset("Server transfer detected: " + StringUtils.getShortMessage(message));
        }
    }

    private void onSkyBlockAreaChange(@NotNull SkyblockAreaChangeEvent event) {
        if (!event.newArea().contains(KUUDRA_AREA_ID) && isInKuudra()) {
            log.info("Detected leaving Kuudra area (now in: {})", event.newArea());
            forceReset("Left Kuudra area -> " + event.newArea());
        }

        if (!event.onSkyBlock() && isInKuudra()) {
            log.info("Detected leaving SkyBlock");
            forceReset("Left SkyBlock");
        }
    }

    public @NotNull KuudraPhase phase() {
        return contextRef.get().phase();
    }

    public @NotNull KuudraContext context() {
        return contextRef.get();
    }

    public boolean isInKuudra() {
        return contextRef.get().phase() != KuudraPhase.NONE;
    }

    public boolean isInRun() {
        return contextRef.get().isInRun();
    }

    public boolean setPhase(@NotNull KuudraPhase newPhase) {
        KuudraContext current = contextRef.get();
        if (current.phase() == newPhase) return false;
        if (newPhase == KuudraPhase.NONE) {
            return handleRunEnd(current, "Phase set to NONE");
        }

        if (current.phase() == KuudraPhase.NONE && newPhase == KuudraPhase.SUPPLIES) {
            return handleRunStart();
        }

        return performPhaseTransition(current, newPhase);
    }

    public void forceReset(@NotNull String reason) {
        KuudraContext current = contextRef.get();
        if (current.phase() == KuudraPhase.NONE) {
            return;
        }

        log.warn("Forcing state reset: {} (was in phase {})", reason, current.phase());
        handleRunEnd(current, reason);
    }

    public @NotNull Optional<Duration> getPhaseDuration(@NotNull KuudraPhase phase) {
        return Optional.ofNullable(phaseDurations.get(phase));
    }

    public @NotNull Optional<Duration> currentPhaseDuration() {
        KuudraContext ctx = contextRef.get();
        if (ctx.phase() == KuudraPhase.NONE) {
            return Optional.empty();
        }

        return Optional.of(ctx.phaseDuration());
    }

    private boolean handleRunStart() {
        KuudraStateValidator.AreaInfo areaInfo = validator.detectAreaInfo();
        if (!areaInfo.canBeInRun()) {
            log.warn("Cannot start run - not in Kuudra area");
            return false;
        }

        KuudraContext newContext = KuudraContext.entering(areaInfo.areaName());
        KuudraContext old = contextRef.getAndSet(newContext);

        phaseDurations.clear();
        EventBus.post(new KuudraPhaseChangeEvent(
                old.phase(),
                KuudraPhase.SUPPLIES,
                0
        ));

        return true;
    }

    private boolean handleRunEnd(@NotNull KuudraContext current, @NotNull String reason) {
        if (current.phase().isInRun()) {
            phaseDurations.put(current.phase(), current.phaseDuration());
        }

        KuudraPhase previousPhase = current.phase();
        long phaseDurationMs = current.phaseDuration().toMillis();

        contextRef.set(KuudraContext.empty());
        Duration totalDuration = phaseDurations.values().stream()
                .reduce(Duration.ZERO, Duration::plus);

        log.info("Kuudra run ended: {} (total: {}ms)", reason, totalDuration.toMillis());
        EventBus.post(new KuudraPhaseChangeEvent(
                previousPhase,
                KuudraPhase.NONE,
                phaseDurationMs
        ));

        EventBus.post(new KuudraRunEndEvent(
                reason,
                previousPhase == KuudraPhase.COMPLETED,
                totalDuration,
                Map.copyOf(phaseDurations)
        ));

        phaseDurations.clear();
        return true;
    }

    private boolean performPhaseTransition(
            @NotNull KuudraContext current,
            @NotNull KuudraPhase newPhase
    ) {
        KuudraPhase previousPhase = current.phase();
        Duration phaseDuration = current.phaseDuration();
        if (previousPhase.isInRun()) {
            phaseDurations.put(previousPhase, phaseDuration);
        }

        KuudraContext newContext = current.withPhase(newPhase);
        boolean updated = contextRef.compareAndSet(current, newContext);
        if (!updated) {
            log.warn("Concurrent modification during phase transition");
            return false;
        }

        log.info("Phase transition: {} -> {} ({}ms)",
                previousPhase.getDisplayName(),
                newPhase.getDisplayName(),
                phaseDuration.toMillis());

        EventBus.post(new KuudraPhaseChangeEvent(
                previousPhase,
                newPhase,
                phaseDuration.toMillis()
        ));

        return true;
    }

    public static KuudraStateManager get() {
        if (instance == null) {
            throw new IllegalStateException("KuudraStateManager not initialized yet");
        }

        return instance;
    }
}