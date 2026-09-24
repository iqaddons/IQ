package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.SoundReceivedEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class IceSprayAlertFeature extends KuudraFeature {

    public IceSprayAlertFeature() {
        super(
                "iceSprayAlert",
                "Ice Spray Alert",
                () -> PhaseFourConfig.iceSprayAlert,
                KuudraPhase.BOSS
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(SoundReceivedEvent.class, this::onSoundReceived);
    }

    private void onSoundReceived(@NotNull SoundReceivedEvent event) {
        if (!event.packet().getSound().is(SoundEvents.ENDER_DRAGON_GROWL.location())) {
            return;
        }

        double bossPhaseSeconds = currentContext().phaseDuration().toMillis() / 1000.0;
        MessageUtil.PARTY.sendMessage("[IQ] Used Ice Spray at %.2fs!".formatted(bossPhaseSeconds));
        log.debug("Ice Spray detected from dragon growl at {}s", bossPhaseSeconds);
    }
}
