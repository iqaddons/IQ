package net.iqaddons.mod.features.kuudra.waypoints;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.config.loader.WaypointConfigLoader;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPickupEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyProgressEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.pearl.PearlWaypoint;
import net.iqaddons.mod.model.pearl.WaypointArea;
import net.iqaddons.mod.utils.AreaDetectionUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@Slf4j
public class PearlWaypointFeature extends KuudraFeature {

    private static final int AREA_CHECK_INTERVAL = 2;
    private static final int SUPPLY_STEP_TIME_MS = 300;
    private static final int TIMER_UPDATE_STEP_MS = 5;
    private static final double PEARL_LABEL_Y_OFFSET = -1.1;
    private static final double TIMER_Y_OFFSET_WITH_TEXT = -3.9;
    private static final double TIMER_Y_OFFSET_ONLY = -1.5;
    private static final double READY_FALLBACK_Y_OFFSET = 0.3;
    private static final double READY_Y_OFFSET_MULTIPLIER = 1.3;
    private static final double READY_EXTRA_Y_OFFSET = 0.7;
    private static final long READY_RESET_TIMEOUT_MS = 900L;
    private static final List<Integer> SUPPLY_TICK_PERCENTAGES = List.of(
            5, 11, 17, 23, 29, 35, 41,
            47, 53, 59, 65, 71, 77, 83,
            89, 95, 100
    );

    private final WaypointConfigLoader configLoader = WaypointConfigLoader.get();
    private final AreaDetectionUtil areaDetection;

    private final SupplyStateManager supplyState = SupplyStateManager.get();

    private int lastSupplyProgress = 0;
    private int lastSupplyProgressIndex = -1;
    private long supplyProgressStartMs = -1L;
    private long lastSupplyProgressUpdateMs = -1L;

    public PearlWaypointFeature() {
        super(
                "pearlWaypoints",
                "Pearl Waypoints",
                () -> PhaseOneConfig.pearlWaypoints,
                KuudraPhase.SUPPLIES
        );

        this.configLoader.load();
        this.areaDetection = new AreaDetectionUtil();
    }

    @Override
    protected void onKuudraActivate() {
        List<WaypointArea> areas = configLoader.getCached();
        areaDetection.setAreas(areas);

        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(WorldRenderEvent.class, this::onRender);
        subscribe(SupplyProgressEvent.class, this::onSupplyProgress);
        subscribe(SupplyDropEvent.class, event -> {
            if (isLocalPlayer(event.playerName())) {
                resetState();
            }
        });
        subscribe(SupplyPlaceEvent.class, event -> {
            if (isLocalPlayer(event.playerName())) {
                resetState();
            }
        });
        subscribe(SupplyPickupEvent.class, event -> resetState());

        resetState();
        log.info("Pearl waypoints activated with {} areas", areas.size());
    }

    @Override
    protected void onKuudraDeactivate() {
        areaDetection.reset();
        resetState();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (event.isNthTick(AREA_CHECK_INTERVAL)) {
            areaDetection.update();
        }

        if (lastSupplyProgressIndex >= 0
                && lastSupplyProgressUpdateMs > 0
                && System.currentTimeMillis() - lastSupplyProgressUpdateMs > READY_RESET_TIMEOUT_MS
        ) {
            resetState();
        }
    }

    private void onSupplyProgress(@NotNull SupplyProgressEvent event) {
        int previousIndex = lastSupplyProgressIndex;
        lastSupplyProgress = event.getCurrentProgress();
        lastSupplyProgressIndex = getProgressIndex(lastSupplyProgress);

        long now = System.currentTimeMillis();
        lastSupplyProgressUpdateMs = now;
        if (lastSupplyProgress <= 0) {
            supplyProgressStartMs = now;
        } else if (lastSupplyProgressIndex >= 0
                && (supplyProgressStartMs < 0 || previousIndex != lastSupplyProgressIndex)
        ) {
            // Re-anchor on each server tick transition to keep timer and READY state in sync.
            supplyProgressStartMs = now - getTargetTimeMs(lastSupplyProgressIndex);
        }

        if (!PhaseOneConfig.pearlThrowAlert || previousIndex < 0
                || lastSupplyProgressIndex < 0
                || previousIndex == lastSupplyProgressIndex
        ) return;


        tryPlayThrowAlert(previousIndex, lastSupplyProgressIndex);
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        WaypointArea area = areaDetection.getCurrentArea();
        if (area == null) return;

        Vec3d commonStandBlockCenter = null;
        for (PearlWaypoint waypoint : area.waypoints()) {
            if (waypoint.hasStandBlock()) {
                commonStandBlockCenter = waypoint.standBlock().add(0.5, 0.5, 0.5);
                break;
            }
        }

        int missingPre = supplyState.getMissingPre();
        for (PearlWaypoint waypoint : area.waypoints()) {
            if (!waypoint.shouldShow(missingPre)) continue;

            renderWaypoint(event, area, waypoint, commonStandBlockCenter);
        }
    }

    private void renderWaypoint(@NotNull WorldRenderEvent event, @NotNull WaypointArea area, @NotNull PearlWaypoint waypoint, Vec3d commonStandBlockCenter) {
        Vec3d target = getRenderTarget(area, waypoint, commonStandBlockCenter);
        if (target.x == 0 && target.y == 0 && target.z == 0) return;

        float adjustedScale = PhaseOneConfig.pearlWaypointsScale;

        int sizeAdjustmentSteps = Math.clamp(PhaseOneConfig.pearlWaypointSize, -5, 5);
        double sizeMultiplier = 1.0 + (sizeAdjustmentSteps * 0.1);
        float size = (float) Math.max(0.05, waypoint.size() * sizeMultiplier);
        float half = size / 2f;

        Box targetBox = new Box(
                target.getX() - half - 0.5, target.getY(), target.getZ() - half - 0.5,
                target.getX() + half - 0.5, target.getY() + size, target.getZ() + half - 0.5
        );

        RenderColor waypointColor = waypoint.color();
        if (waypointColor.a == 0.0f) {
            waypointColor = RenderColor.fromArgb(PhaseOneConfig.pearlWaypointColor);
        }

        int targetIndex = -1;
        String adjustedLabel = waypoint.label();
        boolean isReady = false;
        if (!waypoint.label().isEmpty()) {
            adjustedLabel = getAdjustedPercentage(waypoint.label());
            targetIndex = getAdjustedTargetIndex(adjustedLabel);

            // Change to green if it's time to throw the pearl
            if (PhaseOneConfig.pearlThrowAlert && waypoint.alert() && targetIndex >= 0 && lastSupplyProgress > 0) {
                int currentIndex = lastSupplyProgressIndex;
                if (currentIndex >= 0) {
                    int ticksRemaining = targetIndex - currentIndex;
                    if (ticksRemaining <= 0) {
                        waypointColor = new RenderColor(0, 255, 0, 0xff);
                        isReady = true;
                    }
                }
            }
        }

        switch (PhaseOneConfig.pearlWaypointRenderStyle) {
            case SOLID:
                event.drawFilled(targetBox, true, waypointColor.withOpacity(80.0f));
                break;
            case OUTLINE:
                event.drawOutline(targetBox, true, waypointColor);
                break;
            case BOTH:
                RenderColor fillColor = new RenderColor(
                        (int) (waypointColor.r * 255.0f),
                        (int) (waypointColor.g * 255.0f),
                        (int) (waypointColor.b * 255.0f),
                        153
                );
                RenderColor outlineColor = new RenderColor(
                        (int) (waypointColor.r * 255.0f),
                        (int) (waypointColor.g * 255.0f),
                        (int) (waypointColor.b * 255.0f),
                        255
                );
                event.drawFilled(targetBox, true, fillColor);
                event.drawOutline(targetBox, true, outlineColor);
                break;
            case NONE:
                // do nothing
                break;
        }

        if (!waypoint.label().isEmpty()) {
            String displayLabel = adjustedLabel.replace("%", "");
            RenderColor labelColor = getAlertColor(waypoint, targetIndex);

            if (shouldRenderText()) {
                Vec3d textPos = new Vec3d(target.getX() - 0.5, target.getY() + PEARL_LABEL_Y_OFFSET, target.getZ() - 0.5);
                event.drawText(textPos, Text.literal(displayLabel),
                        adjustedScale, true,
                        labelColor
                );
            }

            long remainingTimerMs = shouldRenderTimer() ? getRemainingTimerMs(targetIndex) : -1L;
            if (remainingTimerMs > 0) {
                double timerYOffset = shouldRenderText() ? TIMER_Y_OFFSET_WITH_TEXT : TIMER_Y_OFFSET_ONLY;
                float scale = shouldRenderText()
                        ? adjustedScale * 0.85f
                        : adjustedScale;

                Vec3d timerPos = new Vec3d(target.getX() - 0.5, target.getY() + timerYOffset, target.getZ() - 0.5);
                event.drawText(timerPos, Text.literal(formatTimerText(remainingTimerMs)),
                        scale, true,
                        getPearlTimerColor(remainingTimerMs, targetIndex)
                );
            } else if (shouldRenderTimer() && isReady) {
                double timerYOffset = shouldRenderText() ? TIMER_Y_OFFSET_WITH_TEXT : TIMER_Y_OFFSET_ONLY;
                float scale = shouldRenderText()
                        ? adjustedScale * 0.85f
                        : adjustedScale;

                Vec3d timerPos = new Vec3d(target.getX() - 0.5, target.getY() + timerYOffset, target.getZ() - 0.5);
                event.drawText(timerPos, Text.literal("READY"), scale, true, new RenderColor(0, 255, 0, 0xff));
            }
        }

        if (waypoint.hasStandBlock()) {
            Vec3d block = waypoint.standBlock();
            Box blockBox = new Box(
                    block.getX(), block.getY(), block.getZ(),
                    block.getX() + 1, block.getY() + 1, block.getZ() + 1
            );

            event.drawOutline(blockBox, true, waypointColor);
        }

        if (isReady && !shouldRenderTimer()) {
            double readyYOffset = getReadyYOffset();
            Vec3d readyPos = new Vec3d(target.getX() - 0.5, target.getY() + size + readyYOffset, target.getZ() - 0.5);
            event.drawText(readyPos, Text.literal("READY"), adjustedScale, true, new RenderColor(0, 255, 0, 0xff));
        }
    }

    private boolean shouldRenderText() {
        return PhaseOneConfig.pearlWaypointTimes == PhaseOneConfig.PearlWaypointType.TEXT_STATIC;
    }

    private boolean shouldRenderTimer() {
        return PhaseOneConfig.pearlWaypointTimes == PhaseOneConfig.PearlWaypointType.TIMER_MS
                || PhaseOneConfig.pearlWaypointTimes == PhaseOneConfig.PearlWaypointType.TIMER_SECONDS;
    }

    private @NotNull Vec3d getRenderTarget(@NotNull WaypointArea area, @NotNull PearlWaypoint waypoint, Vec3d commonStandBlockCenter) {
        Vec3d target = waypoint.target();
        if (!PhaseOneConfig.dynamicPearlWaypoints || commonStandBlockCenter == null || mc.player == null) {
            return target;
        }

        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        Vec3d offset = playerPos.subtract(commonStandBlockCenter);

        Boolean invertForwardBackward = area.invertForwardBackward();
        Boolean invertLeftRight = area.invertLeftRight();

        double heightAdjustment = 0.0;
        if (invertForwardBackward != null) {
            double forwardBackwardDirection = invertForwardBackward ? -1.0 : 1.0;
            heightAdjustment = offset.z
                    * PhaseOneConfig.dynamicPearlWaypointConfig.dynamicPearlWaypointHeightAdjustmentFactor
                    * forwardBackwardDirection;
        }

        Vec3d adjustedOffset = getVec3d(invertLeftRight, offset, heightAdjustment);

        return target.subtract(adjustedOffset);
    }

    private static @NotNull Vec3d getVec3d(Boolean invertLeftRight, Vec3d offset, double heightAdjustment) {
        double lateralHeightAdjustment = 0.0;
        if (invertLeftRight != null) {
            double leftRightDirection = invertLeftRight ? -1.0 : 1.0;
            lateralHeightAdjustment = -offset.x
                    * PhaseOneConfig.dynamicPearlWaypointConfig.dynamicPearlWaypointLeftRightAdjustmentFactor
                    * leftRightDirection;
        }
        Vec3d adjustedOffset = new Vec3d(
                0.0,
                (offset.y * PhaseOneConfig.dynamicPearlWaypointConfig.dynamicPearlWaypointYMultiplier)
                        + heightAdjustment
                        + lateralHeightAdjustment,
                0.0
        );
        return adjustedOffset;
    }

    private @NotNull String getAdjustedPercentage(@NotNull String label) {
        int baseValue;
        try {
            baseValue = Integer.parseInt(label);
        } catch (NumberFormatException ignored) {
            return label;
        }

        int currentIndex = SUPPLY_TICK_PERCENTAGES.indexOf(baseValue);
        if (currentIndex < 0) return label;

        int effectiveDelay = getEffectiveDelayOffset();
        int adjustedIndex = Math.clamp(
                currentIndex + effectiveDelay,
                0,
                SUPPLY_TICK_PERCENTAGES.size() - 1
        );

        return SUPPLY_TICK_PERCENTAGES.get(adjustedIndex) + "%";
    }

    private int getEffectiveDelayOffset() {
        int manualDelayMs = PhaseOneConfig.pearlWaypointsTimerDelay * SUPPLY_STEP_TIME_MS;
        return Math.round((float) manualDelayMs / SUPPLY_STEP_TIME_MS);
    }

    private int getAdjustedTargetIndex(@NotNull String adjustedLabel) {
        if (!adjustedLabel.endsWith("%")) return -1;

        try {
            int value = Integer.parseInt(adjustedLabel.substring(0, adjustedLabel.length() - 1));
            return SUPPLY_TICK_PERCENTAGES.indexOf(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private @NotNull String formatTimerText(long remainingMs) {
        if (PhaseOneConfig.pearlWaypointTimes == PhaseOneConfig.PearlWaypointType.TIMER_SECONDS) {
            return String.format(Locale.ROOT, "%.1fs", remainingMs / 1000.0);
        }
        return remainingMs + "ms";
    }

    private @NotNull RenderColor getPearlTimerColor(long remainingMs, int targetIndex) {
        long targetTimeMs = getTargetTimeMs(targetIndex);
        if (remainingMs <= 0 || targetTimeMs <= 0) {
            return RenderColor.white;
        }

        double elapsedRatio = 1.0 - ((double) remainingMs / (double) targetTimeMs);
        elapsedRatio = Math.clamp(elapsedRatio, 0.0, 1.0);

        if (elapsedRatio >= 0.75) return new RenderColor(85, 255, 85, 0xff);
        if (elapsedRatio >= 0.50) return new RenderColor(255, 255, 0, 0xff);
        if (elapsedRatio >= 0.25) return new RenderColor(255, 165, 0, 0xff);
        return new RenderColor(255, 85, 85, 0xff);
    }

    private long getRemainingTimerMs(int targetIndex) {
        if (targetIndex < 0 || lastSupplyProgressIndex < 0 || supplyProgressStartMs < 0) {
            return -1L;
        }

        // Timer must disappear exactly when the waypoint turns green/READY.
        if (lastSupplyProgressIndex >= targetIndex) {
            return -1L;
        }

        long elapsedMs = Math.max(0L, System.currentTimeMillis() - supplyProgressStartMs);
        long targetTimeMs = getTargetTimeMs(targetIndex);
        long remainingMs = targetTimeMs - elapsedMs;
        if (remainingMs <= 0) {
            return -1L;
        }

        long roundedMs = (remainingMs / TIMER_UPDATE_STEP_MS) * TIMER_UPDATE_STEP_MS;
        return roundedMs > 0 ? roundedMs : -1L;
    }

    private @NotNull RenderColor getAlertColor(@NotNull PearlWaypoint waypoint, int targetIndex) {
        if (!PhaseOneConfig.pearlThrowAlert || !waypoint.alert() || targetIndex < 0) {
            return RenderColor.white;
        }

        int currentIndex = lastSupplyProgressIndex;
        if (currentIndex < 0) {
            return RenderColor.white;
        }

        int ticksRemaining = targetIndex - currentIndex;
        if (ticksRemaining == 0) {
            return new RenderColor(144, 255, 144, 0xff);
        }
        if (ticksRemaining < 0) {
            return RenderColor.white;
        }
        if (ticksRemaining == 1) {
            return new RenderColor(255, 255, 0, 0xff);
        }
        if (ticksRemaining == 2) {
            return new RenderColor(255, 165, 0, 0xff);
        }

        return RenderColor.white;
    }

    private int getProgressIndex(int progress) {
        if (progress <= 0) return -1;

        for (int i = 0; i < SUPPLY_TICK_PERCENTAGES.size(); i++) {
            if (SUPPLY_TICK_PERCENTAGES.get(i) >= progress) {
                return i;
            }
        }

        return SUPPLY_TICK_PERCENTAGES.size() - 1;
    }

    private void tryPlayThrowAlert(int previousIndex, int currentIndex) {
        if (currentIndex <= previousIndex || mc.player == null || mc.world == null) return;

        WaypointArea area = areaDetection.getCurrentArea();
        if (area == null) return;

        int missingPre = supplyState.getMissingPre();
        for (PearlWaypoint waypoint : area.waypoints()) {
            if (!waypoint.alert() || !waypoint.shouldShow(missingPre) || waypoint.label().isEmpty()) continue;

            int targetIndex = getAdjustedTargetIndex(getAdjustedPercentage(waypoint.label()));
            if (targetIndex >= 0 && previousIndex < targetIndex && currentIndex == targetIndex) {
                mc.world.playSound(
                        mc.player,
                        mc.player.getBlockPos(),
                        SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                        SoundCategory.PLAYERS,
                        1.3f,
                        1.6f
                );
                return;
            }
        }
    }

    private long getTargetTimeMs(int targetIndex) {
        return (long) targetIndex * SUPPLY_STEP_TIME_MS;
    }

    private boolean isLocalPlayer(String playerName) {
        if (mc.player == null || playerName == null || playerName.isBlank()) {
            return false;
        }

        return playerName.equalsIgnoreCase(mc.player.getName().getString());
    }

    private void resetState() {
        lastSupplyProgress = 0;
        lastSupplyProgressIndex = -1;
        supplyProgressStartMs = -1L;
        lastSupplyProgressUpdateMs = -1L;
    }

    private double getReadyYOffset() {
        double baseOffset;
        if (shouldRenderTimer()) {
            baseOffset = Math.abs(shouldRenderText() ? TIMER_Y_OFFSET_WITH_TEXT : TIMER_Y_OFFSET_ONLY);
            return (baseOffset * READY_Y_OFFSET_MULTIPLIER) + READY_EXTRA_Y_OFFSET;
        }

        if (shouldRenderText()) {
            baseOffset = Math.abs(PEARL_LABEL_Y_OFFSET);
            return (baseOffset * READY_Y_OFFSET_MULTIPLIER) + READY_EXTRA_Y_OFFSET;
        }

        return (READY_FALLBACK_Y_OFFSET * READY_Y_OFFSET_MULTIPLIER) + READY_EXTRA_Y_OFFSET;
    }
}


