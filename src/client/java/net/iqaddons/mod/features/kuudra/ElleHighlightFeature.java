package net.iqaddons.mod.features.kuudra;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.render.EntityGlowUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Slf4j
public class ElleHighlightFeature extends KuudraFeature {

    private static final int UPDATE_INTERVAL_TICKS = 5;

    private volatile int trackedElleId = -1;

    public ElleHighlightFeature() {
        super(
                "elleHighlight",
                "Elle Highlight",
                () -> PhaseTwoConfig.elleHighlight,
                KuudraPhase.BUILD
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(EventBus.subscribe(ClientTickEvent.class, this::onTick));
        log.info("Elle Highlight (glow) activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        removeElleGlow();
        log.info("Elle Highlight (glow) deactivated");
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(UPDATE_INTERVAL_TICKS)) return;

        Optional<ArmorStandEntity> elleOpt = EntityDetectorUtil.findElle();
        if (elleOpt.isEmpty()) {
            removeElleGlow();
            return;
        }

        ArmorStandEntity elle = elleOpt.get();
        int elleId = elle.getId();
        if (trackedElleId != -1 && trackedElleId != elleId) {
            EntityGlowUtil.removeGlowing(trackedElleId);
        }

        trackedElleId = elleId;
        RenderColor glowColor = RenderColor.fromArgb(PhaseTwoConfig.elleHighlightColor);
        EntityGlowUtil.setGlowing(elleId, glowColor);
    }

    private void removeElleGlow() {
        if (trackedElleId != -1) {
            EntityGlowUtil.removeGlowing(trackedElleId);
            trackedElleId = -1;
        }
    }
}
