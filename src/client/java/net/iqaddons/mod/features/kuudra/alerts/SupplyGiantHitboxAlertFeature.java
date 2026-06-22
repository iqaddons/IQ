package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyProgressEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.spot.SupplyPosition;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.MessageUtil;
import java.util.Comparator;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SupplyGiantHitboxAlertFeature extends KuudraFeature {

    private static final RenderColor PRIMARY_ALERT_COLOR = new RenderColor(255, 0, 0, 120);
    private static final RenderColor PRIMARY_ALERT_OUTLINE_COLOR = new RenderColor(255, 80, 80, 255);
    private static final RenderColor SECONDARY_ALERT_COLOR = new RenderColor(255, 255, 255, 70);
    private static final RenderColor SECONDARY_ALERT_OUTLINE_COLOR = new RenderColor(255, 255, 255, 230);
    private static final String DOUBLE_PEARL_TITLE = "§c§lDOUBLE PEARL";
    private static final int TITLE_FADE_IN_TICKS = 0;
    private static final int TITLE_STAY_TICKS = 20;
    private static final int TITLE_FADE_OUT_TICKS = 8;
    private static final double ALERT_BOX_EXPAND = 0.08;
    private static final double OUTLINE_LAYER_STEP = 0.03;
    private static final int OUTLINE_LAYERS = 3;
    private static final int SECONDARY_OUTLINE_LAYERS = 2;

    private int highlightedGiantId = -1;
    private AlertLevel highlightedAlertLevel = AlertLevel.NONE;

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
        highlightedAlertLevel = AlertLevel.NONE;
    }

    private void onSupplyProgress(@NotNull SupplyProgressEvent event) {
        if (mc.player == null || mc.level == null || event.getCurrentProgress() <= 0 || event.getCurrentProgress() >= 100) {
            highlightedGiantId = -1;
            highlightedAlertLevel = AlertLevel.NONE;
            return;
        }

        DetectionResult detectionResult = findMatchingCarrier(event);
        if (detectionResult == null) {
            highlightedGiantId = -1;
            highlightedAlertLevel = AlertLevel.NONE;
            return;
        }

        Giant giant = detectionResult.giant();
        AlertLevel currentLevel = detectionResult.alertLevel();

        if (currentLevel == AlertLevel.PRIMARY && (highlightedGiantId != giant.getId() || highlightedAlertLevel != AlertLevel.PRIMARY)) {
            MessageUtil.showTitle(DOUBLE_PEARL_TITLE, "", TITLE_FADE_IN_TICKS, TITLE_STAY_TICKS, TITLE_FADE_OUT_TICKS);
            mc.level.playSound(
                    mc.player,
                    mc.player.blockPosition(),
                    SoundEvents.VILLAGER_NO,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.15f
            );
        }

        highlightedGiantId = giant.getId();
        highlightedAlertLevel = currentLevel;
    }

    private @Nullable DetectionResult findMatchingCarrier(@NotNull SupplyProgressEvent event) {
        if (mc.player == null || mc.level == null) return null;

        Vec3 eyePos = mc.player.getEyePosition();
        AABB playerBox = mc.player.getBoundingBox();
        SupplyPosition position = event.getPosition();
        if (position != null) {
            Entity trackedEntity = mc.level.getEntity(position.entityId());
            if (trackedEntity instanceof Giant trackedGiant) {
                AlertLevel level = getAlertLevel(trackedGiant, eyePos, playerBox);
                if (level != AlertLevel.NONE) {
                    return new DetectionResult(trackedGiant, level);
                }
            }
        }

        return EntityDetectorUtil.getSupplyCarriers().stream()
                .map(giant -> new DetectionResult(giant, getAlertLevel(giant, eyePos, playerBox)))
                .filter(result -> result.alertLevel() != AlertLevel.NONE)
                .min(Comparator
                        .comparingInt((DetectionResult result) -> result.alertLevel().priority()).reversed()
                        .thenComparingDouble(result -> result.giant().distanceToSqr(mc.player)))
                .orElse(null);
    }

    private @NotNull AlertLevel getAlertLevel(@NotNull Giant giant, @NotNull Vec3 eyePos, @NotNull AABB playerBox) {
        AABB giantBox = giant.getBoundingBox();
        if (giantBox.contains(eyePos)) {
            return AlertLevel.PRIMARY;
        }

        if (giantBox.intersects(playerBox)) {
            return AlertLevel.SECONDARY;
        }

        return AlertLevel.NONE;
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        if (mc.level == null || highlightedGiantId < 0) return;

        Entity entity = mc.level.getEntity(highlightedGiantId);
        if (!(entity instanceof Giant giant)) {
            highlightedGiantId = -1;
            highlightedAlertLevel = AlertLevel.NONE;
            return;
        }

        renderAlertHitbox(event, giant, highlightedAlertLevel);
    }

    private void renderAlertHitbox(@NotNull WorldRenderEvent event, @NotNull Giant giant, @NotNull AlertLevel alertLevel) {
        if (alertLevel == AlertLevel.NONE) return;

        AABB alertBox = getInterpolatedGiantBox(giant, event).inflate(ALERT_BOX_EXPAND);
        RenderColor fillColor = alertLevel == AlertLevel.PRIMARY ? PRIMARY_ALERT_COLOR : SECONDARY_ALERT_COLOR;
        RenderColor outlineColor = alertLevel == AlertLevel.PRIMARY ? PRIMARY_ALERT_OUTLINE_COLOR : SECONDARY_ALERT_OUTLINE_COLOR;
        int outlineLayers = alertLevel == AlertLevel.PRIMARY ? OUTLINE_LAYERS : SECONDARY_OUTLINE_LAYERS;

        switch (PhaseOneConfig.supplyGiantHitboxStyle) {
            case SOLID -> event.drawFilled(alertBox, true, fillColor);
            case OUTLINE -> {
            }
            case BOTH -> event.drawFilled(alertBox, true, fillColor.withOpacity(fillColor.a * 0.55f));
        }

        drawStrongOutline(event, alertBox, outlineColor, outlineLayers);
    }

    private void drawStrongOutline(@NotNull WorldRenderEvent event, @NotNull AABB box, @NotNull RenderColor color, int layers) {
        event.drawOutline(box, true, color);

        for (int i = 1; i < layers; i++) {
            event.drawOutline(box.inflate(OUTLINE_LAYER_STEP * i), true, color);
        }
    }

    private @NotNull AABB getInterpolatedGiantBox(@NotNull Giant giant, @NotNull WorldRenderEvent event) {
        float tickDelta = event.tickCounter().getGameTimeDeltaPartialTick(true);

        double x = giant.xo + (giant.getX() - giant.xo) * tickDelta;
        double y = giant.yo + (giant.getY() - giant.yo) * tickDelta;
        double z = giant.zo + (giant.getZ() - giant.zo) * tickDelta;

        float halfWidth = giant.getBbWidth() / 2.0f;
        return new AABB(
                x - halfWidth,
                y,
                z - halfWidth,
                x + halfWidth,
                y + giant.getBbHeight(),
                z + halfWidth
        );
    }

    private enum AlertLevel {
        NONE(0),
        SECONDARY(1),
        PRIMARY(2);

        private final int priority;

        AlertLevel(int priority) {
            this.priority = priority;
        }

        private int priority() {
            return priority;
        }
    }

    private record DetectionResult(
            @NotNull Giant giant,
            @NotNull AlertLevel alertLevel
    ) {
    }
}
