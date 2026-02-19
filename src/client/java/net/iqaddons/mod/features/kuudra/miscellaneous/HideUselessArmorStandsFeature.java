package net.iqaddons.mod.features.kuudra.miscellaneous;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ArmorStandRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;

public class HideUselessArmorStandsFeature extends KuudraFeature {

    public HideUselessArmorStandsFeature() {
        super("hideUselessArmorStands",
                "Hide Useless ArmorStand",
                () -> KuudraGeneralConfig.hideUselessArmorStands,
                KuudraPhase.RUN_PHASES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(ArmorStandRenderEvent.class, this::onArmorStandRender);
    }

    private void onArmorStandRender(ArmorStandRenderEvent event) {
        if (!isActive()) return;

        var state = event.getRenderState();
        if (state == null) return;
        if (state.invisible && state.displayName == null) {
            event.setCancelled(true);
        }
    }
}
