package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyProgressEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;

public class SupplyGiantHitboxAlertFeature extends KuudraFeature {

    private static final RenderColor ALERT_COLOR = new RenderColor(255, 0, 0, 120);

    private int highlightedGiantId = -1;

    public SupplyGiantHitboxAlertFeature() {
        super(
                "supplyGiantHitboxAlert",
                "Supply Giant Hitbox Alert",
                () -> PhaseOneConfig.supplyGiantHitboxAlert,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(SupplyProgressEvent.class, this::onSupplyProgress);
        subscribe(WorldRenderEvent.class, this::onRender);
    }

    @Override
    protected void onKuudraDeactivate() {
        highlightedGiantId = -1;
    }

    private void onSupplyProgress(@NotNull SupplyProgressEvent event) {
        if (mc.player == null || mc.world == null || event.getCurrentProgress() <= 0 || event.getCurrentProgress() >= 100) {
            highlightedGiantId = -1;
            return;
        }

        Box playerBox = mc.player.getBoundingBox();
        GiantEntity giant = EntityDetectorUtil.getSupplyCarriers().stream()
                .filter(carrier -> carrier.getBoundingBox().intersects(playerBox))
                .findFirst()
                .orElse(null);

        if (giant == null) {
            highlightedGiantId = -1;
            return;
        }

        if (highlightedGiantId != giant.getId()) {
            mc.world.playSound(
                    mc.player,
                    mc.player.getBlockPos(),
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                    SoundCategory.PLAYERS,
                    1.0f,
                    1.6f
            );
        }

        highlightedGiantId = giant.getId();
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        if (mc.world == null || highlightedGiantId < 0) return;

        Entity entity = mc.world.getEntityById(highlightedGiantId);
        if (!(entity instanceof GiantEntity giant)) {
            highlightedGiantId = -1;
            return;
        }

        event.drawStyledHitbox(giant, true, ALERT_COLOR, PhaseOneConfig.supplyGiantHitboxStyle);
    }
}
