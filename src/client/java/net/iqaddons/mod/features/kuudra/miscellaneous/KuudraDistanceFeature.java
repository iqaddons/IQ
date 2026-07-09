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

    private static final RenderColor ORANGE = RenderColor.fromHex(0xFFAA00);
    private static final RenderColor YELLOW = RenderColor.fromHex(0xFFFF55);
    private static final RenderColor GREEN = RenderColor.green;
    private static final RenderColor RED = RenderColor.red;

    private boolean wasInThrowBoneRange = false;

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

    @Override
    protected void onKuudraDeactivate() {
        wasInThrowBoneRange = false;
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

        boolean inThrowBoneRange = isThrowBoneRange(distance);
        if (PhaseFourConfig.kuudraDistanceThrowBoneTitle && inThrowBoneRange && !wasInThrowBoneRange) {
            MessageUtil.showTitle("", "§aThrow Bone", 0, 10, 5);
        }
        wasInThrowBoneRange = inThrowBoneRange;
    }

    private RenderColor getDistanceColor(double distance) {
        if (distance <= 8.9) return RED;
        if (distance <= 11.0) return ORANGE;
        if (distance < 13.0) return YELLOW;
        if (distance <= 15.0) return GREEN;
        if (distance <= 17.0) return YELLOW;
        return ORANGE;
    }

    private boolean isThrowBoneRange(double distance) {
        return distance >= 13.0 && distance <= 15.0;
    }
}
