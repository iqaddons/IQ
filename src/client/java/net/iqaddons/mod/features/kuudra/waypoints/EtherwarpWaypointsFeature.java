package net.iqaddons.mod.features.kuudra.waypoints;

import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.config.loader.EtherwarpConfigLoader;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.CratePriorityHudEvent;
import net.iqaddons.mod.events.impl.EntitySpawnPacketEvent;
import net.iqaddons.mod.events.impl.EntityTrackingUpdateEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.kuudra.alerts.CratePriorityFeature;
import net.iqaddons.mod.model.etherwarp.EtherwarpCategory;
import net.iqaddons.mod.model.etherwarp.EtherwarpWaypoint;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.pearl.WaypointArea;
import net.iqaddons.mod.utils.AreaDetectionUtil;
import net.iqaddons.mod.utils.StringUtils;
import net.iqaddons.mod.utils.pearl.PearlImpactProjector;
import net.iqaddons.mod.utils.render.RenderColor;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class EtherwarpWaypointsFeature extends AbstractEtherwarpWaypointFeature {

    private static final int AREA_CHECK_INTERVAL = 2;
    private static final int PEARL_TRACK_TIMEOUT_TICKS = 80;
    private static final int TARGET_ACTIVE_TICKS = 120;
    private static final int SIMULATION_TICKS = 90;
    private static final double SPAWN_DISTANCE = 8.0;
    private static final double DEBUG_DISTANCE = 32.0;
    private static final int LANDING_CONFIRM_UPDATES = 2;
    private static final int OUTSIDE_CANCEL_UPDATES = 2;
    private static final float TOP_OUTLINE_WIDTH = 1.8f;
    private static final float TARGET_RADIUS = 0.42f;
    private static final float TARGET_THICKNESS = 0.045f;
    private static final int TARGET_SEGMENTS = 48;
    private static final double TEXT_GAP = 0.72;
    private static final double TEXT_BELOW_GAP = 0.48;
    private static final Map<String, Bounds2D> PREVIEW_FALLBACKS = Map.of(
            "x", new Bounds2D(-130.5, -134.5, -139.5, -143.5),
            "slash", new Bounds2D(-104.5, -83.5, -122.5, -64.5),
            "equals", new Bounds2D(-74.5, -94.5, -59.5, -81.5),
            "triangle", new Bounds2D(-76.5, -115.5, -63.5, -127.5)
    );
    private static final Map<String, Bounds2D> LANDING_BOUNDS = Map.of(
            "x", new Bounds2D(-108.5, -115.5, -103.5, -110.5),
            "slash", new Bounds2D(-100.5, -101.5, -95.5, -96.5),
            "equals", new Bounds2D(-103.5, -101.5, -108.5, -96.5),
            "triangle", new Bounds2D(-96.5, -103.5, -91.5, -108.5)
    );

    private final AreaDetectionUtil areaDetection = new AreaDetectionUtil();
    private final Set<Integer> seenPearls = new HashSet<>();
    private List<WaypointArea> previewAreas = List.of();
    private int trackedPearlId = -1;
    private long trackedPearlStartedTick = 0L;
    private long activeAreaUntilTick = 0L;
    private long lastTick = 0L;
    private String activeAreaName = "";
    private @NotNull String activeCratePriorityDestination = "";
    private long activeCratePriorityUntilTick = 0L;
    private @Nullable Vec3 predictedPlayerLanding = null;
    private @Nullable Vec3 predictedRawPlayerLanding = null;
    private @Nullable Vec3 pendingPlayerLanding = null;
    private @Nullable Vec3 pendingRawPlayerLanding = null;
    private int pendingLandingUpdates = 0;
    private int outsidePredictionUpdates = 0;
    private boolean hasConfirmedLanding = false;

    public EtherwarpWaypointsFeature() {
        super(
                "etherwarpWaypoints",
                "Etherwarp Waypoints",
                () -> PhaseOneConfig.etherwarpWaypoints,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        reloadAreas();
        super.onKuudraActivate();
        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(CratePriorityHudEvent.class, this::onCratePriorityHud);
        subscribe(EntitySpawnPacketEvent.class, this::onEntitySpawn);
        subscribe(EntityTrackingUpdateEvent.class, this::onEntityTrackingUpdate);
    }

    @Override
    protected void onKuudraDeactivate() {
        areaDetection.reset();
        seenPearls.clear();
        trackedPearlId = -1;
        activeAreaName = "";
        activeAreaUntilTick = 0L;
        activeCratePriorityDestination = "";
        activeCratePriorityUntilTick = 0L;
        resetPearlPredictionState();
        super.onKuudraDeactivate();
    }

    @Override
    public void reloadConfig() {
        reloadAreas();
        super.reloadConfig();
    }

    private void reloadAreas() {
        previewAreas = EtherwarpConfigLoader.getNewEtherwarpWaypoints().loadAreas();
        areaDetection.setAreas(previewAreas);
    }

    private void onTick(@NotNull ClientTickEvent event) {
        lastTick = event.tickCount();
        if (event.isInGame() && event.isNthTick(AREA_CHECK_INTERVAL)) {
            areaDetection.update();
        }
        if (event.isInGame()) {
            scanPearls();
        }

        if (trackedPearlId > 0 && event.tickCount() - trackedPearlStartedTick > PEARL_TRACK_TIMEOUT_TICKS) {
            log.info("Etherwarp Waypoints: pearl tracking timed out id={}", trackedPearlId);
            trackedPearlId = -1;
            clearPendingPrediction();
        }

        if (!activeAreaName.isBlank() && event.tickCount() > activeAreaUntilTick) {
            log.info("Etherwarp Waypoints: expired active target group area={}", activeAreaName);
            activeAreaName = "";
            resetPearlPredictionState();
        }

        if (!activeCratePriorityDestination.isBlank() && event.tickCount() > activeCratePriorityUntilTick) {
            activeCratePriorityDestination = "";
            activeCratePriorityUntilTick = 0L;
        }
    }

    private void onCratePriorityHud(@NotNull CratePriorityHudEvent event) {
        String destination = parseDynamicDestination(event.text());
        if (destination.isBlank()) {
            return;
        }

        activeCratePriorityDestination = destination;
        activeCratePriorityUntilTick = lastTick + Math.max(20, event.durationTicks());
        log.info("Etherwarp Waypoints: dynamic target from crate priority = {}", activeCratePriorityDestination);
    }

    private void onEntitySpawn(@NotNull EntitySpawnPacketEvent event) {
        if (event.entityType() != net.minecraft.world.entity.EntityTypes.ENDER_PEARL) {
            return;
        }
        considerPearl(event.entityId(), event.position(), "packet");
    }

    private void scanPearls() {
        if (mc.level == null) return;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.getType() == net.minecraft.world.entity.EntityTypes.ENDER_PEARL) {
                if (entity.getId() == trackedPearlId && !hasConfirmedLanding) {
                    refineTrackedPearl(entity, "scan");
                }
                considerPearl(entity.getId(), entity.position(), "scan");
            }
        }
    }

    private void considerPearl(int entityId, @NotNull Vec3 spawn, @NotNull String source) {
        if (!seenPearls.add(entityId)) {
            return;
        }

        if (mc.player == null) {
            return;
        }

        double distance = spawn.distanceTo(mc.player.position());
        String previewArea = findPreviewArea(mc.player.position());
        KuudraPhase phase = currentPhase();
        if (previewArea.isBlank() || distance > SPAWN_DISTANCE) {
            if (distance <= DEBUG_DISTANCE) {
                log.info(
                        "Etherwarp Waypoints: ignored pearl id={} source={} phase={} player={} spawn={} previewArea={} distance={}",
                        entityId,
                        source,
                        phase,
                        mc.player.position(),
                        spawn,
                        previewArea,
                        String.format(Locale.ROOT, "%.2f", distance)
                );
            }
            return;
        }

        trackedPearlId = entityId;
        trackedPearlStartedTick = lastTick;
        activeAreaName = previewArea;
        activeAreaUntilTick = lastTick + TARGET_ACTIVE_TICKS;
        predictedPlayerLanding = landingBoundsFor(previewArea).center(spawn.y());
        pendingPlayerLanding = null;
        pendingLandingUpdates = 0;
        outsidePredictionUpdates = 0;
        hasConfirmedLanding = false;
        log.info("Etherwarp Waypoints: tracking {} pearl id={} source={} phase={} spawn={}, fallbackLanding={}, waiting for trajectory confirmation", previewArea, trackedPearlId, source, phase, spawn, predictedPlayerLanding);
    }

    private void onEntityTrackingUpdate(@NotNull EntityTrackingUpdateEvent event) {
        Entity entity = event.entity();
        if (trackedPearlId <= 0 || entity.getId() != trackedPearlId || mc.level == null) {
            return;
        }

        refineTrackedPearl(entity, "packet");
    }

    private void refineTrackedPearl(@NotNull Entity entity, @NotNull String source) {
        if (mc.level == null) {
            return;
        }

        LandingPrediction prediction = predictLanding(entity.position(), entity.getDeltaMovement());
        if (prediction == null) {
            return;
        }

        Bounds2D landingBounds = landingBoundsFor(activeAreaName);
        if (!landingBounds.contains(prediction.snappedPlayerLanding().x(), prediction.snappedPlayerLanding().z())) {
            if (hasConfirmedLanding) {
                return;
            }

            outsidePredictionUpdates++;
            if (outsidePredictionUpdates >= OUTSIDE_CANCEL_UPDATES) {
                log.info("Etherwarp Waypoints: {} pearl landing outside pile area raw={} blockCenter={} from {}, cancelling targets", activeAreaName, prediction.rawHit(), prediction.snappedPlayerLanding(), source);
                activeAreaName = "";
                activeAreaUntilTick = 0L;
                trackedPearlId = -1;
                resetPearlPredictionState();
            }
            return;
        }

        outsidePredictionUpdates = 0;
        activeAreaUntilTick = lastTick + TARGET_ACTIVE_TICKS;
        updatePredictedLanding(prediction.snappedPlayerLanding(), prediction.rawHit(), source);
    }

    @Override
    protected boolean shouldRenderCategory(@NotNull EtherwarpCategory category, @NotNull KuudraPhase phase) {
        String areaName = activeAreaName;
        if (areaName.isBlank()) {
            return false;
        }

        String categoryName = normalize(category.name());
        if (categoryName.contains("dps") || categoryName.contains("skip")) {
            return false;
        }

        return categoryName.contains(areaName)
                || (areaName.equals("triangle") && categoryName.contains("tri"))
                || (areaName.equals("slash") && categoryName.contains("slash"))
                || (areaName.equals("equals") && categoryName.contains("equal"));
    }

    @Override
    protected @NotNull Vec3 getRenderPosition(
            @NotNull EtherwarpCategory category,
            @NotNull EtherwarpWaypoint waypoint,
            @NotNull Vec3 position,
            @NotNull KuudraPhase phase
    ) {
        if (activeAreaName.isBlank() || mc.player == null || !isInsidePreview(activeAreaName, mc.player.position())) {
            return position;
        }

        Vec3 landing = selectRenderLanding(position);
        if (landing == null) {
            return position;
        }

        Vec3 playerPos = mc.player.position();
        Vec3 offsetFromFuturePlayer = position.subtract(landing);
        return playerPos.add(offsetFromFuturePlayer);
    }

    private @Nullable Vec3 selectRenderLanding(@NotNull Vec3 targetPosition) {
        Vec3 snappedLanding = predictedPlayerLanding;
        if (snappedLanding == null) {
            return null;
        }

        Vec3 rawLanding = predictedRawPlayerLanding;
        if (rawLanding != null && futureAimHitsTarget(rawLanding, targetPosition)) {
            return rawLanding;
        }

        return snappedLanding;
    }

    private boolean futureAimHitsTarget(@NotNull Vec3 landing, @NotNull Vec3 targetPosition) {
        if (mc.level == null || mc.player == null) {
            return false;
        }

        Vec3 futureEye = landing.add(0.0, mc.player.getEyeHeight(), 0.0);
        Vec3 targetCenter = getVisualCenter(getEtherwarpRenderBox(targetPosition));
        BlockHitResult hit = mc.level.clip(new ClipContext(
                futureEye,
                targetCenter,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        ));
        return hit.getType() == HitResult.Type.BLOCK
                && hit.getBlockPos().equals(BlockPos.containing(targetPosition));
    }

    @Override
    protected void renderWaypoint(
            @NotNull WorldRenderEvent event,
            @NotNull EtherwarpCategory category,
            @NotNull EtherwarpWaypoint waypoint,
            @NotNull Vec3 position,
            int colorIndex
    ) {
        RenderColor color = getWaypointColor(waypoint);
        AABB box = getEtherwarpRenderBox(position);
        Vec3 center = getVisualCenter(box);

        renderEtherwarpShape(event, box, center, color);

        if (PhaseOneConfig.EtherwarpWaypointsConfig.etherwarpWaypointTracer) {
            event.drawTracer(center, color.withOpacity(Math.min(1.0f, color.a * 1.15f)));
        }

        if (PhaseOneConfig.EtherwarpWaypointsConfig.etherwarpWaypointText && !waypoint.text().isBlank()) {
            event.drawText(
                    getTextPosition(box, center),
                    Component.literal(waypoint.text()),
                    PhaseOneConfig.EtherwarpWaypointsConfig.etherwarpWaypointTextScale,
                    true,
                    RenderColor.fromArgb(PhaseOneConfig.EtherwarpWaypointsConfig.etherwarpWaypointTextColor)
            );
        }
    }

    private void renderEtherwarpShape(
            @NotNull WorldRenderEvent event,
            @NotNull AABB box,
            @NotNull Vec3 center,
            @NotNull RenderColor color
    ) {
        switch (PhaseOneConfig.EtherwarpWaypointsConfig.etherwarpWaypointRenderStyle) {
            case TOP -> {
                AABB top = new AABB(box.minX, box.maxY - 0.045, box.minZ, box.maxX, box.maxY, box.maxZ);
                event.drawFilled(top, true, color.withOpacity(Math.min(color.a, 0.18f)));
                event.drawOutline(top, true, color.withOpacity(Math.min(1.0f, color.a * 1.25f)), TOP_OUTLINE_WIDTH);
            }
            case FULL_BLOCK -> event.drawFilled(box, true, color.withOpacity(Math.min(color.a, 0.75f)));
            case FILLED_OUTLINE -> {
                event.drawFilled(box, true, color.withOpacity(Math.min(color.a, 0.28f)));
                event.drawOutline(box, true, color, TOP_OUTLINE_WIDTH);
            }
            case BLOCK_OUTLINE -> event.drawOutline(box, true, color, TOP_OUTLINE_WIDTH);
            case SQUARE -> event.drawBillboardSquareOutline(center, TARGET_RADIUS * 2.0f, true, color);
            case CIRCLE -> event.drawThickBillboardCircleOutline(center, TARGET_RADIUS, TARGET_THICKNESS, TARGET_SEGMENTS, true, color);
        }
    }

    private @NotNull Vec3 getTextPosition(@NotNull AABB box, @NotNull Vec3 center) {
        if (PhaseOneConfig.EtherwarpWaypointsConfig.etherwarpWaypointTextPosition == PhaseOneConfig.EtherwarpWaypointTextPosition.ABOVE) {
            return center.add(0.0, (box.maxY - box.minY) * 0.5 + TEXT_GAP, 0.0);
        }

        PhaseOneConfig.EtherwarpWaypointRenderStyle style = PhaseOneConfig.EtherwarpWaypointsConfig.etherwarpWaypointRenderStyle;
        double y = switch (style) {
            case TOP -> center.y() - TEXT_BELOW_GAP;
            case FULL_BLOCK, FILLED_OUTLINE, BLOCK_OUTLINE -> box.minY - TEXT_BELOW_GAP;
            case SQUARE, CIRCLE -> center.y() - TARGET_RADIUS - TEXT_BELOW_GAP;
        };
        return new Vec3(center.x(), y, center.z());
    }

    private @NotNull RenderColor getWaypointColor(@NotNull EtherwarpWaypoint waypoint) {
        String destination = !activeCratePriorityDestination.isBlank()
                ? activeCratePriorityDestination
                : CratePriorityFeature.getCurrentDestination();
        boolean dynamic = PhaseOneConfig.EtherwarpWaypointsConfig.etherwarpWaypointDynamicRender && !destination.isBlank();
        if (!dynamic) {
            return RenderColor.fromArgb(PhaseOneConfig.EtherwarpWaypointsConfig.etherwarpWaypointColor);
        }

        String normalizedDestination = normalizeDynamicToken(destination);
        boolean active = matchesDynamicTarget(waypoint.name(), normalizedDestination)
                || matchesDynamicTarget(waypoint.text(), normalizedDestination);
        int argb = active
                ? PhaseOneConfig.EtherwarpWaypointsConfig.etherwarpWaypointDynamicActiveColor
                : PhaseOneConfig.EtherwarpWaypointsConfig.etherwarpWaypointDynamicInactiveColor;
        return RenderColor.fromArgb(argb);
    }

    private boolean matchesDynamicTarget(@NotNull String waypointName, @NotNull String normalizedDestination) {
        if (normalizedDestination.isBlank()) {
            return false;
        }

        String waypoint = normalizeDynamicToken(waypointName);
        String destinationAlias = normalizeAlias(normalizedDestination);
        String waypointAlias = normalizeAlias(waypoint);
        if (waypointAlias.equals(destinationAlias)) {
            return true;
        }

        if (destinationAlias.equals("x") || destinationAlias.equals("xcannon")) {
            return waypointAlias.equals("xcannon") || waypointAlias.equals("xcoal");
        }

        if (destinationAlias.equals("shop")) {
            return waypointAlias.startsWith("shop");
        }

        return waypointAlias.equals(destinationAlias)
                || waypointAlias.contains(destinationAlias)
                || destinationAlias.contains(waypointAlias);
    }

    private @NotNull String normalizeAlias(@NotNull String value) {
        return switch (value) {
            case "xc", "xcannon" -> "xcannon";
            case "shopborder" -> "shopborder";
            case "shopsafe", "safeshop" -> "shopsafe";
            case "shopclose", "closeshop" -> "shopclose";
            case "tri" -> "triangle";
            case "eq", "equal" -> "equals";
            default -> value;
        };
    }

    private @NotNull String parseDynamicDestination(@NotNull String text) {
        String clean = stripMinecraftFormatting(text).replace("!", "").trim();
        String upper = clean.toUpperCase(Locale.ROOT);
        if (upper.startsWith("GO ")) {
            return clean.substring(3).trim();
        }
        return clean;
    }

    private @NotNull String normalizeDynamicToken(@NotNull String value) {
        String normalized = stripMinecraftFormatting(value).replace("!", "").trim();
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.startsWith("GO ")) {
            normalized = normalized.substring(3).trim();
        }
        return normalize(normalized);
    }

    private @NotNull String stripMinecraftFormatting(@NotNull String value) {
        return StringUtils.stripFormatting(value.replace("\u00C2\u00A7", "\u00A7"));
    }

    private @NotNull String parseCratePriorityDestination(@NotNull String text) {
        String clean = StringUtils.stripFormatting(text).trim();
        if (clean.toUpperCase(Locale.ROOT).startsWith("GO ")) {
            return clean.substring(3).trim();
        }
        return clean;
    }

    private @NotNull String normalizeDynamicTarget(@NotNull String value) {
        String normalized = StringUtils.stripFormatting(value).trim();
        if (normalized.toUpperCase(Locale.ROOT).startsWith("GO ")) {
            normalized = normalized.substring(3).trim();
        }
        return normalize(normalized);
    }

    private @NotNull Vec3 getVisualCenter(@NotNull AABB box) {
        if (PhaseOneConfig.EtherwarpWaypointsConfig.etherwarpWaypointRenderStyle == PhaseOneConfig.EtherwarpWaypointRenderStyle.TOP) {
            return new Vec3((box.minX + box.maxX) / 2.0, box.maxY - 0.0225, (box.minZ + box.maxZ) / 2.0);
        }
        return box.getCenter();
    }

    private @NotNull AABB getEtherwarpRenderBox(@NotNull Vec3 position) {
        double yOffset = 0.3;
        if (PhaseOneConfig.EtherwarpWaypointsConfig.etherwarpWaypointRenderStyle == PhaseOneConfig.EtherwarpWaypointRenderStyle.TOP) {
            yOffset -= 0.3;
        }
        return getFullBlockBox(position.add(0.0, yOffset, 0.0));
    }

    private @NotNull AABB getFullBlockBox(@NotNull Vec3 position) {
        return new AABB(
                position.x() - 0.5, position.y(), position.z() - 0.5,
                position.x() + 0.5, position.y() + 1.0, position.z() + 0.5
        );
    }

    private void updatePredictedLanding(@NotNull Vec3 snapped, @NotNull Vec3 rawLanding, @NotNull String source) {
        if (!hasConfirmedLanding || predictedPlayerLanding == null) {
            predictedPlayerLanding = snapped;
            predictedRawPlayerLanding = rawLanding;
            hasConfirmedLanding = true;
            clearPendingPrediction();
            log.info("Etherwarp Waypoints: {} pearl first confirmed landing source={} raw={}, snappedPlayer={}", activeAreaName, source, rawLanding, snapped);
            return;
        }

        if (sameBlock(predictedPlayerLanding, snapped)) {
            predictedRawPlayerLanding = rawLanding;
            clearPendingPrediction();
            return;
        }

        if (pendingPlayerLanding != null && sameBlock(pendingPlayerLanding, snapped)) {
            pendingLandingUpdates++;
            pendingRawPlayerLanding = rawLanding;
        } else {
            pendingPlayerLanding = snapped;
            pendingRawPlayerLanding = rawLanding;
            pendingLandingUpdates = 1;
        }

        if (pendingLandingUpdates >= LANDING_CONFIRM_UPDATES) {
            Vec3 previous = predictedPlayerLanding;
            predictedPlayerLanding = snapped;
            predictedRawPlayerLanding = pendingRawPlayerLanding != null ? pendingRawPlayerLanding : rawLanding;
            clearPendingPrediction();
            log.info("Etherwarp Waypoints: {} pearl refined landing source={} {} -> {} from raw={}", activeAreaName, source, previous, snapped, rawLanding);
        }
    }

    private void resetPearlPredictionState() {
        predictedPlayerLanding = null;
        predictedRawPlayerLanding = null;
        clearPendingPrediction();
        outsidePredictionUpdates = 0;
        hasConfirmedLanding = false;
    }

    private void clearPendingPrediction() {
        pendingPlayerLanding = null;
        pendingRawPlayerLanding = null;
        pendingLandingUpdates = 0;
    }

    private boolean sameBlock(@NotNull Vec3 first, @NotNull Vec3 second) {
        return Math.floor(first.x()) == Math.floor(second.x())
                && Math.floor(first.z()) == Math.floor(second.z());
    }

    private @Nullable LandingPrediction predictLanding(@NotNull Vec3 start, @NotNull Vec3 velocity) {
        if (mc.level == null) return null;

        PearlImpactProjector.PearlTrajectoryHit hit = PearlImpactProjector.projectPearlImpact(
                mc.level,
                mc.player,
                start,
                velocity,
                SIMULATION_TICKS
        );
        if (hit == null) {
            return null;
        }

        Vec3 rawHit = hit.location();
        BlockPos blockPos = hit.blockPos();
        Vec3 blockCenter = new Vec3(
                blockPos.getX() + 0.5,
                rawHit.y(),
                blockPos.getZ() + 0.5
        );
        return new LandingPrediction(rawHit, blockCenter, blockPos);
    }

    private @NotNull String findPreviewArea(@NotNull Vec3 pos) {
        for (WaypointArea area : previewAreas) {
            String areaName = normalizeAreaName(area);
            if (!areaName.isBlank() && area.containsPlayer(pos.x(), pos.z())) {
                return areaName;
            }
        }

        for (Map.Entry<String, Bounds2D> entry : PREVIEW_FALLBACKS.entrySet()) {
            if (entry.getValue().contains(pos.x(), pos.z())) {
                return entry.getKey();
            }
        }

        return "";
    }

    private boolean isInsidePreview(@NotNull String areaName, @NotNull Vec3 pos) {
        String normalized = normalize(areaName);
        for (WaypointArea area : previewAreas) {
            if (normalizeAreaName(area).equals(normalized) && area.containsPlayer(pos.x(), pos.z())) {
                return true;
            }
        }

        Bounds2D fallback = PREVIEW_FALLBACKS.get(normalized);
        return fallback != null && fallback.contains(pos.x(), pos.z());
    }

    private @NotNull Bounds2D landingBoundsFor(@NotNull String areaName) {
        return LANDING_BOUNDS.getOrDefault(normalize(areaName), LANDING_BOUNDS.get("x"));
    }

    @Override
    protected @NotNull List<EtherwarpCategory> loadCategories() {
        return EtherwarpConfigLoader.getNewEtherwarpWaypoints().reload();
    }

    private @NotNull String normalizeAreaName(@Nullable WaypointArea area) {
        if (area == null) return "";
        String name = normalize(area.name());
        return switch (name) {
            case "tri", "triangle" -> "triangle";
            case "=", "equals", "equal" -> "equals";
            case "/", "slash" -> "slash";
            default -> name;
        };
    }

    private @NotNull String normalize(@NotNull String value) {
        return value.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }

    private record Bounds2D(double x1, double z1, double x2, double z2) {
        private boolean contains(double x, double z) {
            return x >= Math.min(x1, x2)
                    && x <= Math.max(x1, x2)
                    && z >= Math.min(z1, z2)
                    && z <= Math.max(z1, z2);
        }

        private @NotNull Vec3 center(double y) {
            return new Vec3((x1 + x2) / 2.0, y, (z1 + z2) / 2.0);
        }
    }

    private record LandingPrediction(@NotNull Vec3 rawHit, @NotNull Vec3 snappedPlayerLanding, @NotNull BlockPos blockPos) {
    }
}
