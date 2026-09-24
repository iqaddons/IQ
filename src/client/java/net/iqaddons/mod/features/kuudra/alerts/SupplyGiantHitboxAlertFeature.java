package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyProgressEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPickupEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
import net.iqaddons.mod.mixin.accessor.GuiAccessor;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.iqaddons.mod.utils.MessageUtil;
import java.util.Comparator;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.network.chat.Component;
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
    private static final String POSSIBLE_SUBTITLE = "§fPossible Double Pearl";
    private static final String NEED_SUBTITLE = "§cNeed Double Pearl";
    private static final int TITLE_FADE_IN_TICKS = 0;
    private static final int TITLE_STAY_TICKS = 2;
    private static final int TITLE_FADE_OUT_TICKS = 0;

    private boolean collectingSupply;
    private String activeSubtitle;
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
        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(WorldRenderEvent.class, this::onRender);
        subscribe(SupplyProgressEvent.class, event -> {
            collectingSupply = event.getCurrentProgress() > 0 && event.getCurrentProgress() < 100;
            updateDetection();
        });
        subscribe(SupplyPickupEvent.class, event -> stopCollecting());
        subscribe(SupplyDropEvent.class, event -> {
            if (mc.player != null && event.playerName().equals(mc.player.getName().getString())) {
                stopCollecting();
            }
        });
    }

    @Override
    protected void onKuudraDeactivate() {
        stopCollecting();
        highlightedGiantId = -1;
        highlightedAlertLevel = AlertLevel.NONE;
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) {
            collectingSupply = false;
        }
        updateDetection();
    }

    private void stopCollecting() {
        collectingSupply = false;
        clearSubtitle();
    }

    private void clearSubtitle() {
        if (activeSubtitle != null && mc.gui != null) {
            Component current = ((GuiAccessor) mc.gui).iq$getSubtitle();
            if (current != null && activeSubtitle.equals(current.getString())) {
                mc.gui.setSubtitle(Component.empty());
            }
        }
        activeSubtitle = null;
    }

    private void updateDetection() {
        DetectionResult detectionResult = findMatchingCarrier();
        if (detectionResult == null) {
            highlightedGiantId = -1;
            highlightedAlertLevel = AlertLevel.NONE;
            clearSubtitle();
            return;
        }

        Giant giant = detectionResult.giant();
        AlertLevel currentLevel = detectionResult.alertLevel();

        if (collectingSupply) {
            String subtitle = currentLevel == AlertLevel.PRIMARY ? NEED_SUBTITLE : POSSIBLE_SUBTITLE;
            if (currentLevel == AlertLevel.PRIMARY
                    && (!subtitle.equals(activeSubtitle) || highlightedGiantId != giant.getId())) {
                mc.level.playSound(mc.player, mc.player.blockPosition(), SoundEvents.VILLAGER_NO,
                        SoundSource.PLAYERS, 1.0f, 1.15f);
            }
            activeSubtitle = subtitle;
            MessageUtil.showTitle("", subtitle, TITLE_FADE_IN_TICKS, TITLE_STAY_TICKS, TITLE_FADE_OUT_TICKS);
        } else {
            clearSubtitle();
        }

        highlightedGiantId = giant.getId();
        highlightedAlertLevel = currentLevel;
    }

    private @Nullable DetectionResult findMatchingCarrier() {
        if (mc.player == null || mc.level == null) return null;

        Vec3 eyePos = mc.player.getEyePosition();
        AABB playerBox = mc.player.getBoundingBox();
        return EntityDetectorUtil.getSupplyCarriers().stream()
                .map(giant -> new DetectionResult(giant, getAlertLevel(giant, eyePos, playerBox)))
                .filter(result -> result.alertLevel() != AlertLevel.NONE)
                .min(Comparator
                        .comparingInt((DetectionResult result) -> result.alertLevel().priority()).reversed()
                        .thenComparingDouble(result -> result.giant().distanceToSqr(mc.player)))
                .orElse(null);
    }

    private @NotNull AlertLevel getAlertLevel(@NotNull Giant giant, @NotNull Vec3 eyePos, @NotNull AABB playerBox) {
        return classifyAlertLevel(giant.getBoundingBox(), eyePos, playerBox);
    }

    private static @NotNull AlertLevel classifyAlertLevel(
            @NotNull AABB giantBox, @NotNull Vec3 eyePos, @NotNull AABB playerBox) {
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

        AABB alertBox = getInterpolatedGiantBox(giant, event);
        RenderColor fillColor = alertLevel == AlertLevel.PRIMARY ? PRIMARY_ALERT_COLOR : SECONDARY_ALERT_COLOR;
        RenderColor outlineColor = alertLevel == AlertLevel.PRIMARY ? PRIMARY_ALERT_OUTLINE_COLOR : SECONDARY_ALERT_OUTLINE_COLOR;
        switch (PhaseOneConfig.supplyGiantHitboxStyle) {
            case SOLID -> event.drawFilled(alertBox, true, fillColor);
            case OUTLINE -> event.drawOutline(alertBox, true, outlineColor, 2.0f);
            case BOTH -> {
                event.drawFilled(alertBox, true, fillColor.withOpacity(fillColor.a * 0.55f));
                event.drawOutline(alertBox, true, outlineColor, 2.0f);
            }
        }
    }

    private @NotNull AABB getInterpolatedGiantBox(@NotNull Giant giant, @NotNull WorldRenderEvent event) {
        float tickDelta = event.tickCounter().getGameTimeDeltaPartialTick(true);

        double x = giant.xo + (giant.getX() - giant.xo) * tickDelta;
        double y = giant.yo + (giant.getY() - giant.yo) * tickDelta;
        double z = giant.zo + (giant.getZ() - giant.zo) * tickDelta;

        return giant.getBoundingBox().move(x - giant.getX(), y - giant.getY(), z - giant.getZ());
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
