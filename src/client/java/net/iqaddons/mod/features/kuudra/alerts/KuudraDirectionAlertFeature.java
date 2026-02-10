package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.KuudraLocationUtil;
import net.iqaddons.mod.utils.MessageUtil;
import org.jetbrains.annotations.NotNull;

import static net.iqaddons.mod.utils.KuudraLocationUtil.SpawnDirection.UNKNOWN;

@Slf4j
public class KuudraDirectionAlertFeature extends KuudraFeature {

    private static final int CHECK_INTERVAL_TICKS = 2;

    private volatile KuudraLocationUtil.SpawnDirection currentDirection = UNKNOWN;

    public KuudraDirectionAlertFeature() {
        super(
                "kuudraDirectionAlert",
                "Kuudra Direction Alert",
                () -> PhaseFourConfig.kuudraDirectionAlert,
                KuudraPhase.BOSS
        );
    }

    @Override
    protected void onKuudraActivate() {
        currentDirection = null;
        subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onKuudraDeactivate() {
        currentDirection = null;
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(CHECK_INTERVAL_TICKS)) return;

        var bossInfo = currentContext().bossInfo();
        if (!bossInfo.isAlive()) return;

        var direction = KuudraLocationUtil.getDirection(bossInfo.bossEntity());
        if (direction != UNKNOWN && direction != currentDirection) {
            currentDirection = direction;

            MessageUtil.showTitle(direction.getFormattedName(), "", 0, 25, 5);
            log.info("Kuudra spawn direction alert: {}", direction.getName());
        }
    }
}