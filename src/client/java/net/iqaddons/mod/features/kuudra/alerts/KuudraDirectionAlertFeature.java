package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.KuudraLocationUtil;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.entity.mob.MagmaCubeEntity;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class KuudraDirectionAlertFeature extends KuudraFeature {

    private static final int CHECK_INTERVAL_TICKS = 2;

    private volatile boolean alertShown = false;

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
        alertShown = false;

        subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onKuudraDeactivate() {
        alertShown = false;
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(CHECK_INTERVAL_TICKS)) return;
        if (alertShown) return;

        var optionalKuudra = KuudraLocationUtil.findKuudra();
        if (optionalKuudra.isEmpty()) return;

        MagmaCubeEntity kuudra = optionalKuudra.get();
        var direction = KuudraLocationUtil.getSpawnDirection(kuudra);
        log.debug("Detected Kuudra spawn, direction: {}", direction.getName());

        if (direction != KuudraLocationUtil.SpawnDirection.UNKNOWN) {
            MessageUtil.showTitle(direction.getFormattedName(), "", 0, 25, 5);
            alertShown = true;
            log.debug("Kuudra spawn alert: {}", direction.getName());
        }
    }
}