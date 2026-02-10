package net.iqaddons.mod.features.kuudra;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.render.RenderColor;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class KuudraHitboxFeature extends KuudraFeature {

    public KuudraHitboxFeature() {
        super(
                "kuudraHitbox",
                "Kuudra Hitbox",
                () -> PhaseThreeConfig.KuudraHitbox.enabled,
                KuudraPhase.RUN_PHASES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(WorldRenderEvent.class, this::onRender);
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        var bossInfo = currentContext().bossInfo();
        if (!bossInfo.isAlive()) return;
        
        event.drawStyledHitbox(bossInfo.bossEntity(), true,
                RenderColor.fromArgb(PhaseThreeConfig.KuudraHitbox.color),
                PhaseThreeConfig.KuudraHitbox.style
        );
    }
}


