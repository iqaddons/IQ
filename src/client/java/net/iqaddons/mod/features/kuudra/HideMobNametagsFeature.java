package net.iqaddons.mod.features.kuudra;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.impl.ArmorStandRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;

@Slf4j
public class HideMobNametagsFeature extends KuudraFeature {

    public HideMobNametagsFeature() {
        super(
                "hideMobNametags",
                "Hide Mob Nametags",
                () -> Configuration.hideMobNametags,
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
        if (state == null || state.displayName == null) {
            return;
        }

        String name = state.displayName.getString();
        String stripped = name.replaceAll("§.", "");
        if (stripped.contains("[Lv")) {
            event.setCancelled(true);
        }
    }
}