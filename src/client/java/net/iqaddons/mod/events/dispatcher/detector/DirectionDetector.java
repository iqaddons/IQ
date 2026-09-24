package net.iqaddons.mod.events.dispatcher.detector;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.EntityTrackingUpdateEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraDirectionChangeEvent;
import net.iqaddons.mod.model.kuudra.KuudraContext;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.KuudraLocationUtil;
import net.minecraft.world.entity.monster.MagmaCube;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static net.iqaddons.mod.utils.KuudraLocationUtil.SpawnDirection.UNKNOWN;

@Slf4j
public class DirectionDetector {

    private volatile KuudraLocationUtil.SpawnDirection currentDirection = UNKNOWN;
    private long lastDirectionChangeTime = 0;
    private static final long DIRECTION_CHANGE_COOLDOWN_MS = 1000; // 1 second cooldown

    public void detect(@NotNull ClientTickEvent event, KuudraContext context, Consumer<Event> postEvent) {
        if (!event.isInGame()) return;
        detect(context, postEvent);
    }

    public void detect(@NotNull EntityTrackingUpdateEvent event, KuudraContext context, Consumer<Event> postEvent) {
        if (!(event.entity() instanceof MagmaCube cube)) return;
        if (!KuudraLocationUtil.isKuudra(cube)) return;
        detect(context, cube, postEvent);
    }

    private void detect(KuudraContext context, Consumer<Event> postEvent) {
        var phase = context.phase();
        if (phase != KuudraPhase.SKIP && phase != KuudraPhase.BOSS) return;

        var bossInfo = context.bossInfo();
        MagmaCube kuudraEntity = null;
        if (bossInfo.isAlive() && bossInfo.bossEntity() instanceof MagmaCube cube) {
            kuudraEntity = cube;
        }
        if (kuudraEntity == null) {
            kuudraEntity = KuudraLocationUtil.findKuudra().orElse(null);
        }

        if (kuudraEntity == null || !kuudraEntity.isAlive()) return;

        detect(context, kuudraEntity, postEvent);
    }

    private void detect(KuudraContext context, @NotNull MagmaCube kuudraEntity, Consumer<Event> postEvent) {
        var phase = context.phase();
        if (phase != KuudraPhase.SKIP && phase != KuudraPhase.BOSS) return;

        var direction = KuudraLocationUtil.getDirection(kuudraEntity);
        long currentTime = System.currentTimeMillis();

        if (direction != UNKNOWN && direction != currentDirection &&
                (currentTime - lastDirectionChangeTime) >= DIRECTION_CHANGE_COOLDOWN_MS) {
            postEvent.accept(new KuudraDirectionChangeEvent(
                    currentDirection,
                    direction
            ));

            currentDirection = direction;
            lastDirectionChangeTime = currentTime;
            log.info("Kuudra direction changed: {}", direction);
        }
    }

    public void reset() {
        currentDirection = UNKNOWN;
        lastDirectionChangeTime = 0;
    }
}
