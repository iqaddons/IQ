package net.iqaddons.mod.features.kuudra.miscellaneous;

import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class KuudraDistanceFeature extends KuudraFeature {

    public KuudraDistanceFeature() {
        super(
                "kuudraDistanceDisplay",
                "Kuudra Distance Display",
                () -> PhaseFourConfig.kuudraDistanceDisplay,
                KuudraPhase.SKIP, KuudraPhase.BOSS
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(WorldRenderEvent.class, this::onWorldRender);
    }

    private void onWorldRender(@NotNull WorldRenderEvent event) {
        var bossInfo = currentContext().bossInfo();
        if (!bossInfo.isAlive() || mc.player == null) return;

        var kuudraPos = bossInfo.bossEntity().position();
        var playerPos = mc.player.position();
        var distance = playerPos.distanceTo(kuudraPos);

        event.drawText(
                kuudraPos.add(0, 8.5, 0),
                Component.literal(String.format(Locale.ROOT, "%.1fm", distance)),
                0.20f,
                true,
                getDistanceColor(distance)
        );
    }

    private RenderColor getDistanceColor(double distance) {
        if (distance > 16) return RenderColor.fromHex(0xFFFFFF);
        if (distance < 14.5) return RenderColor.red;
        return RenderColor.green;
    }
}
