package net.iqaddons.mod.features.kuudra.waypoints;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.config.preset.BuiltInPearlWaypoints;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPickupEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyProgressEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.manager.SupplyStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.kuudra.KuudraTier;
import net.iqaddons.mod.model.pearl.PearlAimSolution;
import net.iqaddons.mod.model.pearl.PearlTalismanTier;
import net.iqaddons.mod.model.pearl.PearlTrajectoryType;
import net.iqaddons.mod.model.pearl.PearlWaypoint;
import net.iqaddons.mod.model.pearl.WaypointArea;
import net.iqaddons.mod.utils.AreaDetectionUtil;
import net.iqaddons.mod.utils.BoundingBox2D;
import net.iqaddons.mod.utils.pearl.PearlTrajectorySolver;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
public class PearlWaypointFeature extends KuudraFeature {

    private static final int AREA_CHECK_INTERVAL = 2;
    private static final int TIMER_UPDATE_STEP_MS = 5;
    private static final int CIRCLE_SEGMENTS = 48;
    private static final float CIRCLE_THICKNESS = 0.035f;
    private static final float MIN_WAYPOINT_SIZE = 0.05f;
    private static final double PEARL_LAUNCH_EYE_Y_OFFSET = -0.10000000149011612;
    private static final double FLAT_SERVER_LAUNCH_Y_OFFSET = 1.5;
    private static final double TRAJECTORY_RENDER_Y_OFFSET = 0.05;
    private static final double TRAJECTORY_RENDER_Y_SPREAD = 1.04;
    private static final double TIMER_TEXT_GAP = 0.18;
    private static final double TIMER_TEXT_ABOVE_EXTRA_GAP = 0.22;
    private static final Vec3 WORLD_UP = new Vec3(0.0, 1.0, 0.0);
    private static final double PILE_BEACON_CENTER_OFFSET = 0.0;
    private static final double PILE_BEACON_TARGET_Y_OFFSET = 0.0;
    private static final double FLAT_AREA_Y_OFFSET_SCALE = 0.01;
    private static final double TEXT_REFERENCE_DISTANCE = 24.0;
    private static final double MIN_TEXT_DISTANCE_SCALE = 0.75;
    private static final double MAX_TEXT_DISTANCE_SCALE = 4.0;
    private static final double AREA_DEBUG_FLOOR_Y = 76.05;
    private static final double AREA_DEBUG_FLOOR_HEIGHT = 0.12;
    private static final double AREA_DEBUG_WALL_MIN_Y = 76.0;
    private static final double AREA_DEBUG_WALL_MAX_Y = 82.0;
    private static final float AREA_DEBUG_TEXT_SCALE = 0.08f;
    private static final long PICKUP_PROGRESS_TIMEOUT_MS = 750L;
    private static final long READY_FALLBACK_TIMEOUT_MS = 5_000L;
    private static final List<Integer> SUPPLY_TICK_PERCENTAGES = List.of(
            5, 11, 17, 23, 29, 35, 41,
            47, 53, 59, 65, 71, 77, 83,
            89, 95, 100
    );

    private final AreaDetectionUtil areaDetection;
    private final SupplyStateManager supplyState = SupplyStateManager.get();
    private final Set<String> alertedWaypoints = new HashSet<>();
    private List<WaypointArea> loadedAreas = List.of();

    private int lastSupplyProgress = 0;
    private int lastSupplyProgressIndex = -1;
    private long supplyProgressStartMs = -1L;
    private long lastPickupProgressMs = -1L;
    private long readyStartedMs = -1L;
    private long lastTick = 0L;

    public PearlWaypointFeature() {
        super(
                "pearlWaypoints",
                "Pearl Waypoints",
                () -> PhaseOneConfig.pearlWaypoints,
                KuudraPhase.SUPPLIES
        );
        this.areaDetection = new AreaDetectionUtil();
    }

    @Override
    protected void onKuudraActivate() {
        List<WaypointArea> areas = BuiltInPearlWaypoints.getAreas();
        loadedAreas = areas;
        areaDetection.setAreas(areas);

        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(WorldRenderEvent.class, this::onRender);
        subscribe(SupplyProgressEvent.class, this::onSupplyProgress);
        subscribe(SupplyDropEvent.class, event -> {
            if (isLocalPlayer(event.playerName())) resetState();
        });
        subscribe(SupplyPlaceEvent.class, event -> {
            if (isLocalPlayer(event.playerName())) resetState();
        });
        subscribe(SupplyPickupEvent.class, event -> resetState());

        resetState();
        log.info("Pearl waypoints activated with {} areas", areas.size());
    }

    @Override
    protected void onKuudraDeactivate() {
        areaDetection.reset();
        loadedAreas = List.of();
        resetState();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        lastTick = event.tickCount();
        if (event.isNthTick(AREA_CHECK_INTERVAL)) {
            areaDetection.update();
        }

        long now = System.currentTimeMillis();
        if (lastPickupProgressMs > 0L && now - lastPickupProgressMs > PICKUP_PROGRESS_TIMEOUT_MS) {
            clearPickupState();
        }
        if (readyStartedMs > 0L && now - readyStartedMs > READY_FALLBACK_TIMEOUT_MS) {
            clearPickupState();
        }
    }

    private void onSupplyProgress(@NotNull SupplyProgressEvent event) {
        lastSupplyProgress = event.getCurrentProgress();
        int previousIndex = lastSupplyProgressIndex;
        lastSupplyProgressIndex = getProgressIndex(lastSupplyProgress);

        long now = System.currentTimeMillis();
        lastPickupProgressMs = now;
        if (lastSupplyProgress <= 0 || supplyProgressStartMs < 0L) {
            readyStartedMs = -1L;
            alertedWaypoints.clear();
        }

        if (lastSupplyProgress <= 0) {
            supplyProgressStartMs = now;
        } else if (lastSupplyProgressIndex >= 0
                && (supplyProgressStartMs < 0 || previousIndex != lastSupplyProgressIndex)
        ) {
            supplyProgressStartMs = now - getProgressElapsedMs(lastSupplyProgressIndex);
        }
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        WaypointArea area = areaDetection.getCurrentArea();
        if (mc.player == null) return;

        if (PhaseOneConfig.PearlWaypointsConfig.pearlWaypointAreaDebug) {
            renderAreaDebug(event, area);
        }

        if (area == null) return;

        Vec3 commonStandBlockCenter = getCommonStandBlockCenter(area);
        int missingPre = supplyState.getMissingPre();

        for (PearlWaypoint waypoint : area.waypoints()) {
            if (!waypoint.shouldShow(missingPre)) continue;

            RenderData renderData = buildRenderData(area, waypoint, commonStandBlockCenter);
            if (renderData == null) continue;

            renderWaypoint(event, area, waypoint, renderData);
        }

        renderAreaStandBlockOutlines(event, area);
    }

    private void renderAreaDebug(@NotNull WorldRenderEvent event, @Nullable WaypointArea currentArea) {
        for (int i = 0; i < loadedAreas.size(); i++) {
            WaypointArea area = loadedAreas.get(i);
            RenderColor color = getAreaDebugColor(i, area == currentArea);
            BoundingBox2D bounds = area.bounds();

            AABB floor = new AABB(
                    bounds.minX(),
                    AREA_DEBUG_FLOOR_Y,
                    bounds.minZ(),
                    bounds.maxX() + 1.0,
                    AREA_DEBUG_FLOOR_Y + AREA_DEBUG_FLOOR_HEIGHT,
                    bounds.maxZ() + 1.0
            );
            AABB wallOutline = new AABB(
                    bounds.minX(),
                    AREA_DEBUG_WALL_MIN_Y,
                    bounds.minZ(),
                    bounds.maxX() + 1.0,
                    AREA_DEBUG_WALL_MAX_Y,
                    bounds.maxZ() + 1.0
            );

            event.drawFilled(floor, true, color.withOpacity(area == currentArea ? 0.28f : 0.16f));
            event.drawOutline(wallOutline, true, color, area == currentArea ? 3.0f : 1.5f);
            event.drawText(
                    floor.getCenter().add(0.0, AREA_DEBUG_WALL_MAX_Y - AREA_DEBUG_FLOOR_Y + 0.35, 0.0),
                    Component.literal(area.name()),
                    AREA_DEBUG_TEXT_SCALE,
                    true,
                    color.withOpacity(1.0f)
            );
        }
    }

    private @NotNull RenderColor getAreaDebugColor(int index, boolean currentArea) {
        RenderColor color = switch (index % 7) {
            case 0 -> new RenderColor(255, 85, 85, 255);
            case 1 -> new RenderColor(85, 170, 255, 255);
            case 2 -> new RenderColor(85, 255, 135, 255);
            case 3 -> new RenderColor(255, 205, 85, 255);
            case 4 -> new RenderColor(255, 85, 255, 255);
            case 5 -> new RenderColor(85, 255, 255, 255);
            default -> new RenderColor(255, 255, 255, 255);
        };
        return currentArea ? color : color.withOpacity(0.78f);
    }

    private @Nullable RenderData buildRenderData(
            @NotNull WaypointArea area,
            @NotNull PearlWaypoint waypoint,
            @Nullable Vec3 commonStandBlockCenter
    ) {
        if (waypoint.usesTrajectoryProjection()) {
            Vec3 origin = getPearlSpawnPosition(waypoint.trajectoryType());
            Vec3 columnTarget = getPileBeaconColumnTarget(waypoint);
            PearlAimSolution solution = solveWaypointTrajectory(area, waypoint, origin, columnTarget);
            if (solution == null) {
                return null;
            }

            Vec3 renderTarget = getProjectedRenderTarget(waypoint, solution.direction().normalize());
            return new RenderData(renderTarget, solution.flightTicks(), true, solution.direction().normalize());
        }

        return new RenderData(getStaticRenderTarget(area, waypoint, commonStandBlockCenter), -1.0, false, null);
    }

    private @Nullable PearlAimSolution solveWaypointTrajectory(
            @NotNull WaypointArea area,
            @NotNull PearlWaypoint waypoint,
            @NotNull Vec3 origin,
            @NotNull Vec3 target
    ) {
        if (waypoint.trajectoryType() == PearlTrajectoryType.FLAT) {
            return PearlTrajectorySolver.solveFlatForDisplay(origin, target, mc.level, mc.player,
                    isSquareLongFlatWaypoint(area, waypoint));
        }

        return PearlTrajectorySolver.solve(
                origin,
                target,
                waypoint.trajectoryType(),
                mc.level,
                mc.player
        );
    }

    private @NotNull Vec3 applyFlatAreaYOffset(
            @NotNull WaypointArea area,
            @NotNull PearlWaypoint waypoint,
            @NotNull Vec3 target
    ) {
        // Projected FLAT markers must stay on the solved ray; legacy visual offsets change the throw.
        if (waypoint.trajectoryType() != PearlTrajectoryType.FLAT || waypoint.usesTrajectoryProjection()) {
            return target;
        }

        int offset = switch (area.name().toLowerCase(Locale.ROOT)) {
            case "x" -> PhaseOneConfig.PearlWaypointsConfig.pearlFlatXAreaYOffset;
            case "equals" -> PhaseOneConfig.PearlWaypointsConfig.pearlFlatEqualsAreaYOffset;
            case "slash" -> PhaseOneConfig.PearlWaypointsConfig.pearlFlatSlashAreaYOffset;
            case "triangle" -> PhaseOneConfig.PearlWaypointsConfig.pearlFlatTriangleAreaYOffset;
            case "square" -> PhaseOneConfig.PearlWaypointsConfig.pearlFlatSquareAreaYOffset;
            case "shop" -> PhaseOneConfig.PearlWaypointsConfig.pearlFlatShopAreaYOffset;
            default -> 0;
        };

        if (offset == 0) {
            return target;
        }
        return target.add(0.0, offset * FLAT_AREA_Y_OFFSET_SCALE, 0.0);
    }

    private @NotNull Vec3 getProjectedRenderTarget(
            @NotNull PearlWaypoint waypoint,
            @NotNull Vec3 direction
    ) {
        Vec3 eyePosition = getPlayerEyePosition();
        double renderDistance = waypoint.projectionDistance() > 0.0
                ? waypoint.projectionDistance()
                : 13.0;
        return eyePosition.add(direction.scale(renderDistance));
    }

    private boolean isSquareLongFlatWaypoint(@NotNull WaypointArea area, @NotNull PearlWaypoint waypoint) {
        String label = waypoint.label().toLowerCase(Locale.ROOT);
        return area.name().equalsIgnoreCase("square")
                && waypoint.trajectoryType() == PearlTrajectoryType.FLAT
                && (label.equals("square - triangle") || label.equals("square - shop"));
    }

    private double getAdjustedTrajectoryRenderY(@NotNull Vec3 columnTarget, double rawY) {
        return columnTarget.y
                + ((rawY - columnTarget.y) * TRAJECTORY_RENDER_Y_SPREAD)
                + TRAJECTORY_RENDER_Y_OFFSET;
    }

    private @NotNull Vec3 getPileBeaconColumnTarget(@NotNull PearlWaypoint waypoint) {
        return waypoint.target().add(PILE_BEACON_CENTER_OFFSET, PILE_BEACON_TARGET_Y_OFFSET, PILE_BEACON_CENTER_OFFSET);
    }

    private @NotNull Vec3 getPlayerEyePosition() {
        return mc.player.position().add(0.0, mc.player.getEyeHeight(), 0.0);
    }

    private @NotNull Vec3 getPearlSpawnPosition(@NotNull PearlTrajectoryType type) {
        if (type == PearlTrajectoryType.FLAT) {
            return mc.player.position().add(0.0, FLAT_SERVER_LAUNCH_Y_OFFSET, 0.0);
        }
        return mc.player.getEyePosition().add(0.0, PEARL_LAUNCH_EYE_Y_OFFSET, 0.0);
    }

    private void renderWaypoint(
            @NotNull WorldRenderEvent event,
            @NotNull WaypointArea area,
            @NotNull PearlWaypoint waypoint,
            @NotNull RenderData renderData
    ) {
        float size = getAdjustedSize(waypoint);
        Vec3 target = applyFlatAreaYOffset(area, waypoint, getMarkerRenderCenter(renderData, size));
        if (waypoint.trajectoryType() == PearlTrajectoryType.FLAT && renderData.direction() != null
                && mc.options.getCameraType().isFirstPerson()) {
            // Anchor to the rendered camera, including interpolation and crouch transitions.
            double distance = waypoint.projectionDistance() > 0.0 ? waypoint.projectionDistance() : 13.0;
            target = event.cameraState().pos.add(renderData.direction().scale(distance));
        }
        if (target.x == 0 && target.y == 0 && target.z == 0) return;

        AABB box = makeWaypointBox(target, size);
        RenderColor color = getWaypointColor(waypoint);

        TimerState timer = renderData.trajectory()
                ? getTrajectoryTimer(waypoint, renderData.flightTicks())
                : getProgressTimer(waypoint);

        if (timer.ready() && PhaseOneConfig.PearlWaypointsConfig.pearlThrowAlert && waypoint.alert()) {
            color = new RenderColor(0, 255, 0, 0xff);
            if (readyStartedMs < 0L) {
                readyStartedMs = System.currentTimeMillis();
            }
            playAlertOnce(area.name() + ":" + waypoint.label() + ":" + waypoint.target());
        }

        renderShape(event, box, target, size, color);

        if (!timer.text().isBlank()) {
            Vec3 textPosition = getTimerTextPosition(target, size);
            event.drawText(
                    textPosition,
                    Component.literal(timer.text()),
                    getDistanceCompensatedTextScale(textPosition),
                    true,
                    timer.timerColor()
            );
        }
    }

    private @NotNull Vec3 getMarkerRenderCenter(@NotNull RenderData renderData, float size) {
        if (!renderData.trajectory()) {
            return renderData.position();
        }
        return renderData.position();
    }

    private void renderShape(
            @NotNull WorldRenderEvent event,
            @NotNull AABB box,
            @NotNull Vec3 center,
            float size,
            @NotNull RenderColor color
    ) {
        PhaseOneConfig.PearlWaypointRenderStyle style = PhaseOneConfig.PearlWaypointsConfig.pearlWaypointRenderStyle;
        RenderColor fill = color.withOpacity(Math.min(color.a, 0.8f));
        RenderColor softFill = color.withOpacity(Math.min(color.a, 0.35f));
        float radius = Math.max(MIN_WAYPOINT_SIZE, size * 0.75f);

        switch (style) {
            case FULL_BLOCK -> event.drawFilled(box, true, fill);
            case FILLED_OUTLINE -> {
                event.drawFilled(box, true, softFill);
                event.drawOutline(box, true, color);
            }
            case BLOCK_OUTLINE -> event.drawOutline(box, true, color);
            case SQUARE -> event.drawBillboardSquareOutline(center, radius * 2.0f, true, color);
            case CIRCLE -> event.drawThickBillboardCircleOutline(center, radius, CIRCLE_THICKNESS, CIRCLE_SEGMENTS, true, color);
        }
    }

    private void renderAreaStandBlockOutlines(@NotNull WorldRenderEvent event, @NotNull WaypointArea area) {
        if (!PhaseOneConfig.PearlWaypointsConfig.pearlWaypointBlockOutlines) {
            return;
        }

        Set<String> renderedBlocks = new HashSet<>();
        for (PearlWaypoint waypoint : area.waypoints()) {
            if (!waypoint.hasStandBlock()) continue;

            Vec3 block = waypoint.standBlock();
            String blockKey = block.x() + ":" + block.y() + ":" + block.z();
            if (!renderedBlocks.add(blockKey)) continue;

            AABB blockBox = new AABB(
                    block.x(), block.y(), block.z(),
                    block.x() + 1, block.y() + 1, block.z() + 1
            );
            event.drawOutline(blockBox, true, getWaypointColor(waypoint));
        }
    }

    private @NotNull TimerState getTrajectoryTimer(@NotNull PearlWaypoint waypoint, double flightTicks) {
        if (!isPickingSupply()) {
            return TimerState.hidden();
        }

        double elapsedTicks = getServerSyncedElapsedMs() / 50.0;
        double landingTick = waypoint.landingTick() != null
                ? waypoint.landingTick()
                : getSupplyWindowTicks(KuudraStateManager.get().context().tier(), PhaseOneConfig.PearlWaypointsConfig.pearlWaypointTalismanTier)
                + getTrajectoryLandingOffset(waypoint);
        double pingTicks = PhaseOneConfig.PearlWaypointsConfig.pearlWaypointPingMs / 50.0;
        double throwTick = landingTick - flightTicks - pingTicks;
        double remainingTicks = throwTick - elapsedTicks;

        if (remainingTicks <= 0.0) {
            return TimerState.ready("READY");
        }

        long remainingMs = Math.round(remainingTicks * 50.0);
        return TimerState.countdown(formatTimerText(remainingMs), getPearlTimerColor(remainingMs, Math.round((float) landingTick * 50.0)));
    }

    private double getTrajectoryLandingOffset(@NotNull PearlWaypoint waypoint) {
        if (waypoint.trajectoryType() == PearlTrajectoryType.DOUBLE_HIGH) {
            return waypoint.landingOffsetTicks();
        }
        return -waypoint.landingOffsetTicks();
    }

    private @NotNull TimerState getProgressTimer(@NotNull PearlWaypoint waypoint) {
        if (!isPickingSupply()) {
            return TimerState.staticOnly();
        }

        int targetIndex = getTargetIndex(waypoint.label());
        if (targetIndex < 0) {
            return TimerState.staticOnly();
        }

        if (lastSupplyProgressIndex >= targetIndex && lastSupplyProgressIndex >= 0) {
            return TimerState.ready("READY");
        }

        long remainingMs = getRemainingTimerMs(targetIndex);
        if (remainingMs <= 0L) {
            return TimerState.staticOnly();
        }

        return TimerState.countdown(formatTimerText(remainingMs), getPearlTimerColor(remainingMs, getProgressElapsedMs(targetIndex)));
    }

    private @NotNull String formatTimerText(long remainingMs) {
        long roundedMs = Math.max(TIMER_UPDATE_STEP_MS, (remainingMs / TIMER_UPDATE_STEP_MS) * TIMER_UPDATE_STEP_MS);
        if (PhaseOneConfig.PearlWaypointsConfig.pearlWaypointTimes == PhaseOneConfig.PearlWaypointType.TIMER_SECONDS) {
            return String.format(Locale.ROOT, "%.2fs", roundedMs / 1000.0);
        }
        if (PhaseOneConfig.PearlWaypointsConfig.pearlWaypointTimes == PhaseOneConfig.PearlWaypointType.TIMER_TICKS) {
            return Math.max(1L, Math.round(roundedMs / 50.0)) + "t";
        }
        return roundedMs + "ms";
    }

    private @NotNull Vec3 getTimerTextPosition(@NotNull Vec3 target, float size) {
        double offset = getTimerTextOffset(size);
        Vec3 textAxis = getCameraRelativeTextAxis(target);
        if (PhaseOneConfig.PearlWaypointsConfig.pearlWaypointTextPosition == PhaseOneConfig.PearlWaypointTextPosition.ABOVE) {
            return target.add(textAxis.scale(offset + TIMER_TEXT_ABOVE_EXTRA_GAP));
        }
        return target.add(textAxis.scale(-offset));
    }

    private double getTimerTextOffset(float size) {
        return switch (PhaseOneConfig.PearlWaypointsConfig.pearlWaypointRenderStyle) {
            case FULL_BLOCK, FILLED_OUTLINE, BLOCK_OUTLINE -> (size / 2.0) + TIMER_TEXT_GAP;
            case SQUARE, CIRCLE -> Math.max(MIN_WAYPOINT_SIZE, size * 0.75f) + TIMER_TEXT_GAP;
        };
    }

    private @NotNull Vec3 getCameraRelativeTextAxis(@NotNull Vec3 target) {
        if (mc.player == null) {
            return WORLD_UP;
        }

        Vec3 toTarget = target.subtract(getPlayerEyePosition());
        if (toTarget.lengthSqr() < 1.0E-6) {
            return WORLD_UP;
        }

        Vec3 viewDirection = toTarget.normalize();
        Vec3 right = viewDirection.cross(WORLD_UP);
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        }

        Vec3 screenUp = right.normalize().cross(viewDirection);
        if (screenUp.lengthSqr() < 1.0E-6) {
            return WORLD_UP;
        }
        return screenUp.normalize();
    }

    private float getDistanceCompensatedTextScale(@NotNull Vec3 textPosition) {
        if (mc.player == null) {
            return PhaseOneConfig.PearlWaypointsConfig.pearlWaypointsScale;
        }

        double distance = getPlayerEyePosition().distanceTo(textPosition);
        double multiplier = Math.clamp(distance / TEXT_REFERENCE_DISTANCE, MIN_TEXT_DISTANCE_SCALE, MAX_TEXT_DISTANCE_SCALE);
        return (float) (PhaseOneConfig.PearlWaypointsConfig.pearlWaypointsScale * multiplier);
    }

    private @NotNull Vec3 getStaticRenderTarget(
            @NotNull WaypointArea area,
            @NotNull PearlWaypoint waypoint,
            @Nullable Vec3 commonStandBlockCenter
    ) {
        Vec3 target = waypoint.target();
        if (commonStandBlockCenter == null || mc.player == null) {
            return target;
        }

        return target;
    }

    private float getAdjustedSize(@NotNull PearlWaypoint waypoint) {
        int steps = Math.clamp(PhaseOneConfig.PearlWaypointsConfig.pearlWaypointSize, -5, 5);
        double multiplier = 1.0 + (steps * 0.1);
        return (float) Math.max(MIN_WAYPOINT_SIZE, waypoint.size() * multiplier);
    }

    private @NotNull AABB makeWaypointBox(@NotNull Vec3 center, float size) {
        float half = size / 2f;
        return new AABB(
                center.x - half, center.y - half, center.z - half,
                center.x + half, center.y + half, center.z + half
        );
    }

    private @NotNull RenderColor getWaypointColor(@NotNull PearlWaypoint waypoint) {
        RenderColor waypointColor = waypoint.color();
        if (waypointColor.a == 0.0f) {
            return RenderColor.fromArgb(PhaseOneConfig.PearlWaypointsConfig.pearlWaypointColor);
        }
        return waypointColor;
    }

    private @Nullable Vec3 getCommonStandBlockCenter(@NotNull WaypointArea area) {
        PearlWaypoint waypoint = getStandBlockWaypoint(area);
        return waypoint != null ? waypoint.standBlock().add(0.5, 0.5, 0.5) : null;
    }

    private @Nullable PearlWaypoint getStandBlockWaypoint(@NotNull WaypointArea area) {
        for (PearlWaypoint waypoint : area.waypoints()) {
            if (waypoint.hasStandBlock()) {
                return waypoint;
            }
        }
        return null;
    }

    private int getTargetIndex(@NotNull String label) {
        if (label.isBlank()) return -1;

        try {
            int value = Integer.parseInt(label.replace("%", "").trim());
            return SUPPLY_TICK_PERCENTAGES.indexOf(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private long getRemainingTimerMs(int targetIndex) {
        if (targetIndex < 0 || lastSupplyProgressIndex < 0 || supplyProgressStartMs < 0) {
            return -1L;
        }

        long targetTimeMs = getProgressElapsedMs(targetIndex);
        long remainingMs = targetTimeMs - getServerSyncedElapsedMs();
        return remainingMs > 0 ? remainingMs : -1L;
    }

    private long getServerSyncedElapsedMs() {
        if (supplyProgressStartMs < 0L || lastPickupProgressMs < 0L) {
            return 0L;
        }

        return Math.max(0L, System.currentTimeMillis() - supplyProgressStartMs);
    }

    private @NotNull RenderColor getPearlTimerColor(long remainingMs, long totalMs) {
        if (remainingMs <= 0 || totalMs <= 0) return RenderColor.white;

        double remainingRatio = Math.clamp((double) remainingMs / (double) totalMs, 0.0, 1.0);

        if (remainingRatio >= 0.75) return new RenderColor(85, 255, 85, 0xff);
        if (remainingRatio >= 0.50) return new RenderColor(255, 255, 0, 0xff);
        if (remainingRatio >= 0.25) return new RenderColor(255, 165, 0, 0xff);
        return new RenderColor(255, 85, 85, 0xff);
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

    private long getProgressElapsedMs(int targetIndex) {
        double totalPickupMs = getSupplyWindowTicks(
                KuudraStateManager.get().context().tier(),
                PhaseOneConfig.PearlWaypointsConfig.pearlWaypointTalismanTier
        ) * 50.0;
        double progressStep = (targetIndex + 1) / (double) SUPPLY_TICK_PERCENTAGES.size();
        return Math.round(totalPickupMs * progressStep);
    }

    private double getSupplyWindowTicks(@NotNull KuudraTier tier, @NotNull PearlTalismanTier talismanTier) {
        return switch (talismanTier) {
            case NONE -> switch (tier) {
                case BASIC -> 60;
                case HOT -> 80;
                case BURNING -> 100;
                case FIERY, INFERNAL, UNKNOWN -> 120;
            };
            case TIER_1 -> switch (tier) {
                case BASIC -> 55;
                case HOT -> 75;
                case BURNING -> 90;
                case FIERY, INFERNAL, UNKNOWN -> 110;
            };
            case TIER_2 -> switch (tier) {
                case BASIC -> 50;
                case HOT -> 65;
                case BURNING -> 80;
                case FIERY, INFERNAL, UNKNOWN -> 100;
            };
            case TIER_3 -> switch (tier) {
                case BASIC -> 45;
                case HOT -> 60;
                case BURNING -> 70;
                case FIERY, INFERNAL, UNKNOWN -> 85;
            };
        };
    }

    private void playAlertOnce(@NotNull String key) {
        if (mc.player == null || mc.level == null || !alertedWaypoints.add(key)) return;

        mc.level.playSound(
                mc.player,
                mc.player.blockPosition(),
                SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.PLAYERS,
                1.3f,
                1.6f
        );
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
        lastPickupProgressMs = -1L;
        readyStartedMs = -1L;
        alertedWaypoints.clear();
    }

    private boolean isPickingSupply() {
        return lastPickupProgressMs > 0L
                && System.currentTimeMillis() - lastPickupProgressMs <= PICKUP_PROGRESS_TIMEOUT_MS;
    }

    private void clearPickupState() {
        lastSupplyProgress = 0;
        lastSupplyProgressIndex = -1;
        supplyProgressStartMs = -1L;
        lastPickupProgressMs = -1L;
        readyStartedMs = -1L;
        alertedWaypoints.clear();
    }

    public void reloadConfig() {
        List<WaypointArea> areas = BuiltInPearlWaypoints.getAreas();
        areaDetection.setAreas(areas);
        areaDetection.update();
        log.info("Pearl waypoint presets refreshed: {} areas", areas.size());
    }

    private record RenderData(
            @NotNull Vec3 position,
            double flightTicks,
            boolean trajectory,
            @Nullable Vec3 direction
    ) {
    }

    private record TimerState(
            @NotNull String text,
            boolean ready,
            @NotNull RenderColor timerColor
    ) {
        static @NotNull TimerState hidden() {
            return new TimerState("", false, RenderColor.white);
        }

        static @NotNull TimerState ready(@NotNull String text) {
            return new TimerState(text, true, new RenderColor(0, 255, 0, 0xff));
        }

        static @NotNull TimerState countdown(@NotNull String text, @NotNull RenderColor color) {
            return new TimerState(text, false, color);
        }

        static @NotNull TimerState staticOnly() {
            return new TimerState("", false, RenderColor.white);
        }
    }
}
