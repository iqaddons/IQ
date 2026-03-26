package net.iqaddons.mod.events.dispatcher.detector;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraDirectionChangeEvent;
import net.iqaddons.mod.model.kuudra.KuudraContext;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.KuudraLocationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static net.iqaddons.mod.utils.KuudraLocationUtil.SpawnDirection.UNKNOWN;

@Slf4j
public class DirectionDetector {

    private static final int DIRECTION_CONFIRM_TICKS = 4;
    private static final int DIRECTION_EVENT_COOLDOWN_TICKS = 20;

    private volatile KuudraLocationUtil.SpawnDirection currentDirection = UNKNOWN;
    private volatile KuudraLocationUtil.SpawnDirection candidateDirection = UNKNOWN;

    private volatile long candidateStartTick = -1;
    private volatile long lastDirectionEventTick = -1;

    public void detect(@NotNull ClientTickEvent event, KuudraContext context, Consumer<Event> postEvent) {
        if (!event.isInGame()) return;

        var phase = context.phase();
        if (phase != KuudraPhase.SKIP && phase != KuudraPhase.BOSS) return;

        var bossInfo = context.bossInfo();
        var kuudraEntity = bossInfo.isAlive()
                ? bossInfo.bossEntity()
                : KuudraLocationUtil.findKuudra().orElse(null);

        if (kuudraEntity == null || !kuudraEntity.isAlive()) {
            clearCandidate();
            return;
        }

        var rawDirection = KuudraLocationUtil.getDirection(kuudraEntity);
        if (rawDirection == UNKNOWN) {
            clearCandidate();
            return;
        }

        long tick = event.tickCount();
        if (rawDirection != candidateDirection) {
            candidateDirection = rawDirection;
            candidateStartTick = tick;
            return;
        }

        if (!isCandidateStable(tick)) return;
        if (candidateDirection == currentDirection) return;
        if (!isOutsideCooldown(tick)) return;

        var previousDirection = currentDirection;
        currentDirection = candidateDirection;
        lastDirectionEventTick = tick;

        postEvent.accept(new KuudraDirectionChangeEvent(
                previousDirection,
                currentDirection
        ));

        log.info("Kuudra direction changed: {}", currentDirection);
    }

    public void reset() {
        currentDirection = UNKNOWN;
        clearCandidate();
        lastDirectionEventTick = -1;
    }

    private boolean isCandidateStable(long currentTick) {
        return candidateStartTick >= 0
                && (currentTick - candidateStartTick + 1) >= DIRECTION_CONFIRM_TICKS;
    }

    private boolean isOutsideCooldown(long currentTick) {
        return lastDirectionEventTick < 0
                || (currentTick - lastDirectionEventTick) >= DIRECTION_EVENT_COOLDOWN_TICKS;
    }

    private void clearCandidate() {
        candidateDirection = UNKNOWN;
        candidateStartTick = -1;
    }
}
