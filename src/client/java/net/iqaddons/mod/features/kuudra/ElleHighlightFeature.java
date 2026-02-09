package net.iqaddons.mod.features.kuudra;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class ElleHighlightFeature extends KuudraFeature {

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
        subscribe(WorldRenderEvent.class, this::onRender);
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        var optionalElle = EntityDetectorUtil.findElle();
        if (optionalElle.isEmpty()) return;

        var elle = optionalElle.get();
        event.drawHitbox(elle, true, RenderColor.fromArgb(PhaseTwoConfig.elleHighlightColor));
    }
}
