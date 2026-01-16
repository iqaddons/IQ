package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.KuudraDirectionUtil;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.entity.mob.MagmaCubeEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Slf4j
public class KuudraDirectionAlertFeature extends KuudraFeature {

    private static final int CHECK_INTERVAL_TICKS = 2;

    public KuudraDirectionAlertFeature() {
        super(
                "kuudraDirectionAlert",
                "Kuudra Direction Alert",
                () -> Configuration.PhaseFourConfig.kuudraDirectionAlert,
                KuudraPhase.BOSS
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(EventBus.subscribe(ClientTickEvent.class, this::onTick));
        log.info("Kuudra Spawn Alert activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        log.info("Kuudra Spawn Alert deactivated");
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(CHECK_INTERVAL_TICKS)) return;

        var optionalKuudra = KuudraDirectionUtil.findKuudra();
        if (optionalKuudra.isEmpty()) return;

        MagmaCubeEntity kuudra = optionalKuudra.get();
        var direction = KuudraDirectionUtil.getSpawnDirection(kuudra);
        log.info("Detected Kuudra spawn, direction: {}", direction.getName());

        if (direction != KuudraDirectionUtil.SpawnDirection.UNKNOWN) {
            MessageUtil.showTitle(direction.getFormattedName(), "", 0, 25, 5);
            log.info("Kuudra spawn alert: {}", direction.getName());
        }
    }
}