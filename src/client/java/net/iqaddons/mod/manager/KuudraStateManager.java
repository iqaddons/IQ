package net.iqaddons.mod.manager;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.SubscriptionOwner;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.events.impl.skyblock.SkyblockAreaChangeEvent;
import net.iqaddons.mod.model.kuudra.KuudraBossInfo;
import net.iqaddons.mod.model.kuudra.KuudraContext;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.kuudra.validator.KuudraStateValidator;
import net.iqaddons.mod.utils.KuudraLocationUtil;
import net.iqaddons.mod.utils.ScoreboardUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static net.iqaddons.mod.IQConstants.*;

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
        if (!context().isInRun()) return;

        var player = event.client().player;
        if (player == null) return;
        if (player.getEntityPos().getY() < 10
                && (context().phase() == KuudraPhase.SKIP
                || context().phase() == KuudraPhase.DPS)
        ) {
            setPhase(KuudraPhase.BOSS);
        }

        KuudraContext current = contextRef.get();
        if (current.phase().isCombatPhase() || current.phase() == KuudraPhase.BOSS) {
            performBossScan(current);
        } else if (event.isNthTick(2)) {
            performBossScan(current);
        }

        if (!event.isNthTick(DEFAULT_CHECK_INTERVAL_TICKS)) return;
        if (current.phase() == KuudraPhase.NONE) return;

        KuudraStateValidator.ValidationResult result = validator.validate(current);
        if (result.isValid()) {
            KuudraContext validated = current.validated();
            contextRef.compareAndSet(current, validated);
            log.trace("Validation passed for phase {}", current.phase());
        } else {
            forceReset(KuudraRunEndEvent.EndReason.OTHER);
        }
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        if (!isInSkyBlock()) return;

        String message = event.getStrippedMessage();
        if (message.contains("DEFEAT") && isInKuudra()) {
            forceReset(KuudraRunEndEvent.EndReason.DEFEATED);
            return;
        }

        KuudraPhase detected = KuudraPhase.fromMessage(message);
        if (detected == null) return;
        log.debug("Detected phase trigger in chat: {} -> {}", message.substring(0, Math.min(50, message.length())), detected);

        if (detected == KuudraPhase.NONE) {
            if (isInKuudra()) {
                forceReset(KuudraRunEndEvent.EndReason.OTHER);
            }

            return;
        }

        setPhase(detected);
        if (message.contains("Sending to server") || message.contains("Starting in 5 seconds...")) {
            forceReset(KuudraRunEndEvent.EndReason.DISCONNECTED);
        }
    }

    private void onSkyBlockAreaChange(@NotNull SkyblockAreaChangeEvent event) {
        if (!event.newArea().contains(KUUDRA_AREA_ID) && isInKuudra()) {
            log.info("Detected leaving Kuudra area (now in: {})", event.newArea());
            forceReset(KuudraRunEndEvent.EndReason.DISCONNECTED);
        }

        if (!event.onSkyBlock() && isInKuudra()) {
            log.info("Detected leaving SkyBlock");
            forceReset(KuudraRunEndEvent.EndReason.DISCONNECTED);
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

    private boolean isInSkyBlock() {
        return ScoreboardUtils.hasTitle(SKYBLOCK_AREA_ID);
    }

    public boolean isInRun() {
        return contextRef.get().isInRun();
    }

    public boolean setPhase(@NotNull KuudraPhase newPhase) {
        KuudraContext current = contextRef.get();
        if (current.phase() == newPhase) return false;
        if (newPhase == KuudraPhase.NONE) {
            return handleRunEnd(current, KuudraRunEndEvent.EndReason.OTHER);
        }

        if ((current.phase() == KuudraPhase.NONE || current.phase() == KuudraPhase.COMPLETED)
                && newPhase == KuudraPhase.SUPPLIES) {
            return handleRunStart();
        }

        KuudraLocationUtil.invalidateCache();
        return performPhaseTransition(current, newPhase);
    }

    public void forceReset(@NotNull KuudraRunEndEvent.EndReason reason) {
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

        KuudraLocationUtil.invalidateCache();

        KuudraContext newContext = KuudraContext.entering();
        KuudraContext old = contextRef.getAndSet(newContext);

        phaseDurations.clear();
        EventBus.post(new KuudraPhaseChangeEvent(
                old.phase(),
                KuudraPhase.SUPPLIES,
                0
        ));

        return true;
    }

    private boolean handleRunEnd(@NotNull KuudraContext current, @NotNull KuudraRunEndEvent.EndReason reason) {
        if (!isInSkyBlock()) return false;
        if (current.phase().isInRun()) {
            phaseDurations.put(current.phase(), current.phaseDuration());
        }

        KuudraPhase previousPhase = current.phase();
        long phaseDurationMs = current.phaseDuration().toMillis();

        contextRef.set(KuudraContext.empty());
        KuudraLocationUtil.invalidateCache();

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
        if (newPhase.getOrder() < previousPhase.getOrder()) {
            log.warn("Ignoring backward phase transition: {} -> {}", previousPhase, newPhase);
            return false;
        }

        if (previousPhase == KuudraPhase.DPS && newPhase == KuudraPhase.STUN) {
            log.warn("Invalid phase transition: {} -> {}", previousPhase, newPhase);
            return false;
        }

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

        if (newPhase == KuudraPhase.COMPLETED) {
            return handleRunEnd(current, KuudraRunEndEvent.EndReason.COMPLETED);
        }

        return true;
    }

    private void performBossScan(@NotNull KuudraContext current) {
        if (current.phase() == KuudraPhase.NONE) {
            if (current.bossInfo().isAlive()) {
                contextRef.compareAndSet(current, current.withBossInfo(KuudraBossInfo.empty()));
            }

            return;
        }

        var nextBossInfo = KuudraLocationUtil.findKuudra()
                .map(KuudraBossInfo::tracked)
                .orElseGet(KuudraBossInfo::empty);

        if (!current.bossInfo().equals(nextBossInfo)) {
            contextRef.compareAndSet(current, current.withBossInfo(nextBossInfo));
        }
    }

    public static KuudraStateManager get() {
        if (instance == null) {
            throw new IllegalStateException("KuudraStateManager not initialized yet");
        }

        return instance;
    }

    public static boolean isInitialized() {
        return instance != null;
    }
}