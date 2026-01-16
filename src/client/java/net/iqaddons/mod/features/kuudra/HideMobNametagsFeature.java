package net.iqaddons.mod.features.kuudra;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ArmorStandRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.kuudra.KuudraPhase;

@Slf4j
public class HideMobNametagsFeature extends KuudraFeature {

    @Getter
    private static HideMobNametagsFeature instance;

    public HideMobNametagsFeature() {
        super(
                "hideMobNametags",
                "Hide Mob Nametags",
                () -> Configuration.hideMobNametags,
                KuudraPhase.RUN_PHASES
        );
        instance = this;
    }

    @Override
    protected void onKuudraActivate() {
        EventBus.subscribe(ArmorStandRenderEvent.class, this::onArmorStandRender);
        log.info("Hide Mob Nametags activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        log.info("Hide Mob Nametags deactivated");
    }

    private void onArmorStandRender(ArmorStandRenderEvent event) {
        if (instance == null || !instance.isActive()) {
            return;
        }

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