package net.iqaddons.mod.events.dispatcher.detector;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraDirectionChangeEvent;
import net.iqaddons.mod.model.kuudra.KuudraContext;
import net.iqaddons.mod.utils.KuudraLocationUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static net.iqaddons.mod.utils.KuudraLocationUtil.SpawnDirection.UNKNOWN;

@Slf4j
public class DirectionDetector {

    private volatile KuudraLocationUtil.SpawnDirection currentDirection = UNKNOWN;

    public void detect(@NotNull ClientTickEvent event, KuudraContext context, Consumer<Event> postEvent) {
        if (!event.isInGame()) return;

        var bossInfo = context.bossInfo();
        if (!bossInfo.isAlive()) return;

        var direction = KuudraLocationUtil.getDirection(bossInfo.bossEntity());
        if (direction != UNKNOWN && direction != currentDirection) {
            postEvent.accept(new KuudraDirectionChangeEvent(
                    currentDirection,
                    direction
            ));

            currentDirection = direction;
            log.info("Kuudra direction changed: {}", direction);
        }
    }

    public void reset() {
        currentDirection = UNKNOWN;
    }
}
