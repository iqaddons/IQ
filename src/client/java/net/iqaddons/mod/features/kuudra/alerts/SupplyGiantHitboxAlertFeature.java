package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyProgressEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.spot.SupplyPosition;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SupplyGiantHitboxAlertFeature extends KuudraFeature {

    private static final RenderColor ALERT_COLOR = new RenderColor(255, 0, 0, 120);
    private static final double PLAYER_GIANT_MAX_DISTANCE = 5.5;

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

        GiantEntity giant = findMatchingCarrier(event);
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

    private @Nullable GiantEntity findMatchingCarrier(@NotNull SupplyProgressEvent event) {
        if (mc.player == null || mc.world == null) return null;

        Box playerBox = mc.player.getBoundingBox();
        SupplyPosition position = event.getPosition();
        if (position != null) {
            Entity trackedEntity = mc.world.getEntityById(position.entityId());
            if (trackedEntity instanceof GiantEntity trackedGiant && isValidCarrierHit(trackedGiant, playerBox)) {
                return trackedGiant;
            }
        }

        return EntityDetectorUtil.getSupplyCarriers().stream()
                .filter(giant -> isValidCarrierHit(giant, playerBox))
                .min((a, b) -> Double.compare(a.squaredDistanceTo(mc.player), b.squaredDistanceTo(mc.player)))
                .orElse(null);
    }

    private boolean isValidCarrierHit(@NotNull GiantEntity giant, @NotNull Box playerBox) {
        if (mc.player == null) return false;

        return giant.getBoundingBox().intersects(playerBox)
                && giant.squaredDistanceTo(mc.player) <= PLAYER_GIANT_MAX_DISTANCE * PLAYER_GIANT_MAX_DISTANCE;
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
