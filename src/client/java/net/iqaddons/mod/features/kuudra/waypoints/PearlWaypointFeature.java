package net.iqaddons.mod.features.kuudra.waypoints;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.config.loader.WaypointConfigLoader;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
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

@Slf4j
public class PearlWaypointFeature extends KuudraFeature {

    private static final int AREA_CHECK_INTERVAL = 2;
    private static final int SUPPLY_STEP_TIME_MS = 300;
    private static final int TIMER_UPDATE_STEP_MS = 5;
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
        subscribe(SupplyDropEvent.class, event -> resetState());

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
    }

    private void onSupplyProgress(@NotNull SupplyProgressEvent event) {
        int previousIndex = lastSupplyProgressIndex;
        lastSupplyProgress = event.getCurrentProgress();
        lastSupplyProgressIndex = getProgressIndex(lastSupplyProgress);

        long now = System.currentTimeMillis();
        if (lastSupplyProgress <= 0) {
            supplyProgressStartMs = now;
        } else if (supplyProgressStartMs < 0 && lastSupplyProgressIndex >= 0) {
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

        int missingPre = supplyState.getMissingPre();
        for (PearlWaypoint waypoint : area.waypoints()) {
            if (!waypoint.shouldShow(missingPre)) continue;

            renderWaypoint(event, waypoint);
        }
    }

    private void renderWaypoint(@NotNull WorldRenderEvent event, @NotNull PearlWaypoint waypoint) {
        Vec3d target = getRenderTarget(waypoint);
        if (target.x == 0 && target.y == 0 && target.z == 0) return;

        float size = waypoint.size();
        float half = size / 2f;

        Box targetBox = new Box(
                target.getX() - half - 0.5, target.getY(), target.getZ() - half - 0.5,
                target.getX() + half - 0.5, target.getY() + size, target.getZ() + half - 0.5
        );

        event.drawFilled(targetBox, true, waypoint.color().withOpacity(80.0f));
        if (!waypoint.label().isEmpty()) {
            String adjustedLabel = getAdjustedPercentage(waypoint.label());
            int targetIndex = getAdjustedTargetIndex(adjustedLabel);
            RenderColor labelColor = getAlertColor(waypoint, targetIndex);

            if (shouldRenderText()) {
                Vec3d textPos = new Vec3d(target.getX() - 0.5, target.getY() - 1.5, target.getZ() - 0.5);
                event.drawText(textPos, Text.literal(adjustedLabel),
                        PhaseOneConfig.pearlWaypointsScale, true,
                        labelColor
                );
            }

            long remainingTimerMs = shouldRenderTimer() ? getRemainingTimerMs(targetIndex) : -1L;
            if (remainingTimerMs > 0) {
                double timerYOffset = shouldRenderText() ? -3.9 : -1.5;
                float scale = shouldRenderText()
                        ? PhaseOneConfig.pearlWaypointsScale * 0.85f
                        : PhaseOneConfig.pearlWaypointsScale;

                Vec3d timerPos = new Vec3d(target.getX() - 0.5, target.getY() + timerYOffset, target.getZ() - 0.5);
                event.drawText(timerPos, Text.literal(remainingTimerMs + "ms"),
                        scale, true,
                        getPearlTimerColor(remainingTimerMs)
                );
            }
        }

        if (waypoint.hasStandBlock()) {
            Vec3d block = waypoint.standBlock();
            Box blockBox = new Box(
                    block.getX(), block.getY(), block.getZ(),
                    block.getX() + 1, block.getY() + 1, block.getZ() + 1
            );

            event.drawOutline(blockBox, true, waypoint.color());
        }
    }

    private boolean shouldRenderText() {
        return PhaseOneConfig.pearlWaypointTimes == PhaseOneConfig.PearlWaypointType.TEXT
                || PhaseOneConfig.pearlWaypointTimes == PhaseOneConfig.PearlWaypointType.BOTH;
    }

    private boolean shouldRenderTimer() {
        return PhaseOneConfig.pearlWaypointTimes == PhaseOneConfig.PearlWaypointType.TIMER
                || PhaseOneConfig.pearlWaypointTimes == PhaseOneConfig.PearlWaypointType.BOTH;
    }

    private @NotNull Vec3d getRenderTarget(@NotNull PearlWaypoint waypoint) {
        Vec3d target = waypoint.target();
        if (!PhaseOneConfig.dynamicPearlWaypoints || !waypoint.hasStandBlock() || mc.player == null) {
            return target;
        }

        Vec3d standBlockCenter = waypoint.standBlock().add(0.5, 0.0, 0.5);
        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d offset = playerPos.subtract(standBlockCenter);

        return target.add(offset.x, 0.0, offset.z);
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

        int adjustedIndex = Math.clamp(
                currentIndex + PhaseOneConfig.pearlWaypointsTimerDelay,
                0,
                SUPPLY_TICK_PERCENTAGES.size() - 1
        );

        return SUPPLY_TICK_PERCENTAGES.get(adjustedIndex) + "%";
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

    private @NotNull RenderColor getPearlTimerColor(long remainingMs) {
        if (remainingMs <= 50) return new RenderColor(85, 255, 85, 0xff);
        if (remainingMs <= 1000) return new RenderColor(144, 255, 144, 0xff);
        if (remainingMs <= 2000) return new RenderColor(255, 255, 0, 0xff);
        if (remainingMs <= 3500) return new RenderColor(255, 165, 0, 0xff);
        return new RenderColor(255, 85, 85, 0xff);
    }

    private long getRemainingTimerMs(int targetIndex) {
        if (targetIndex < 0 || lastSupplyProgressIndex < 0 || supplyProgressStartMs < 0) {
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

    private void resetState() {
        lastSupplyProgress = 0;
        lastSupplyProgressIndex = -1;
        supplyProgressStartMs = -1L;
    }
}