package net.iqaddons.mod.features.kuudra;

import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.TitleReceivedEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class HideDamageTitleFeature extends KuudraFeature {

    private static final Pattern KUUDRA_DAMAGE_TITLE = Pattern.compile("^☠\\s*[\\d.,]+[KMBT]?/[\\d.,]+[KMBT]?❤$");

    public HideDamageTitleFeature() {
        super(
                "hideDamageTitle",
                "Hide Kuudra Damage Title",
                () -> PhaseFourConfig.hideDamageTitle,
                KuudraPhase.STUN,
                KuudraPhase.DPS,
                KuudraPhase.BOSS
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(TitleReceivedEvent.class, this::onTitleReceived);
    }

    private void onTitleReceived(@NotNull TitleReceivedEvent event) {
        if (KUUDRA_DAMAGE_TITLE.matcher(event.getStrippedMessage()).matches()) {
            event.setCancelled(true);
        }
    }
}