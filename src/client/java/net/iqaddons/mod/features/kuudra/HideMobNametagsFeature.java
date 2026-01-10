package net.iqaddons.mod.features.kuudra;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ArmorStandRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class HideMobNametagsFeature extends KuudraFeature {

    @Getter
    private static HideMobNametagsFeature instance;

    public HideMobNametagsFeature() {
        super(
                "hideMobNametags",
                "Hide Mob Nametags",
                () -> Configuration.hideMobNametags,
                KuudraPhase.SUPPLIES, KuudraPhase.BUILD, KuudraPhase.EATEN,
                KuudraPhase.STUN, KuudraPhase.DPS, KuudraPhase.BOSS
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

        String name = event.getRenderState().displayName.getString();
        String stripped = name.replaceAll("§.", "");
        if (stripped.contains("[Lv")) {
            event.setCancelled(true);
        }
    }

    public static boolean shouldHideNametag(@NotNull String name) {
        if (instance == null || !instance.isActive()) {
            return false;
        }

        String stripped = name.replaceAll("§.", "");
        return stripped.contains("[Lv");
    }
}