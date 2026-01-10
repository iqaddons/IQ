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

    private volatile boolean alertShown = false;
    private volatile float lastKuudraHealth = 0;

    public KuudraDirectionAlertFeature() {
        super(
                "kuudraSpawnAlert",
                "Kuudra Spawn Alert",
                () -> Configuration.PhaseFourConfig.kuudraSpawnAlert,
                KuudraPhase.STUN, KuudraPhase.DPS, KuudraPhase.BOSS
        );
    }

    @Override
    protected void onKuudraActivate() {
        alertShown = false;
        lastKuudraHealth = 0;
        subscribe(EventBus.subscribe(ClientTickEvent.class, this::onTick));
        log.info("Kuudra Spawn Alert activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        alertShown = false;
        log.info("Kuudra Spawn Alert deactivated");
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(CHECK_INTERVAL_TICKS)) return;

        Optional<MagmaCubeEntity> optionalKuudra = KuudraDirectionUtil.findKuudra();
        if (optionalKuudra.isEmpty()) {
            if (alertShown) {
                alertShown = false;
                lastKuudraHealth = 0;
            }
            return;
        }

        MagmaCubeEntity kuudra = optionalKuudra.get();
        float currentHealth = kuudra.getHealth();

        if (KuudraDirectionUtil.justBossSpawned(kuudra)) {
            if (!alertShown || currentHealth > lastKuudraHealth) {
                KuudraDirectionUtil.SpawnDirection direction = KuudraDirectionUtil.getSpawnDirection(kuudra);

                if (direction != KuudraDirectionUtil.SpawnDirection.UNKNOWN) {
                    MessageUtil.showTitle(direction.getFormattedName(), "", 0, 25, 5);
                    log.debug("Kuudra spawn alert: {}", direction.getName());
                }
                alertShown = true;
            }
        }

        lastKuudraHealth = currentHealth;
    }
}