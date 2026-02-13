package net.iqaddons.mod.features.kuudra.miscellaneous;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.BossBarRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

public class HideKuudraBossBarFeature extends KuudraFeature {

    private static final String KUUDRA_BAR_NAME = "Kuudra";

    public HideKuudraBossBarFeature() {
        super(
                "hideKuudraBossBar",
                "Hide Kuudra Boss Bar",
                () -> KuudraGeneralConfig.hideKuudraBossBar,
                KuudraPhase.RUN_PHASES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(BossBarRenderEvent.class, event -> {
            if (event.getStrippedTitle().contains(KUUDRA_BAR_NAME)) {
                event.setCancelled(true);
            }
        });
    }
}
