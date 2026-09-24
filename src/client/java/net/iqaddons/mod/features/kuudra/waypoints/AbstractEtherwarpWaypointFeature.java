package net.iqaddons.mod.features.kuudra.waypoints;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.loader.EtherwarpConfigLoader;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.etherwarp.EtherwarpCategory;
import net.iqaddons.mod.model.etherwarp.EtherwarpWaypoint;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

@Slf4j
public abstract class AbstractEtherwarpWaypointFeature extends KuudraFeature {

    private volatile List<WaypointEntry> visibleWaypoints = List.of();
    private volatile List<EtherwarpCategory> cachedCategories = List.of();
    private volatile KuudraPhase cachedPhase = KuudraPhase.NONE;

    protected AbstractEtherwarpWaypointFeature(
            @NotNull String id,
            @NotNull String name,
            @NotNull BooleanSupplier enabledSupplier,
            @NotNull KuudraPhase @NotNull ... phases
    ) {
        super(id, name, enabledSupplier, phases);
    }

    @Override
    protected void onKuudraActivate() {
        reloadConfig();
        subscribe(WorldRenderEvent.class, this::onWorldRender);
    }

    @Override
    protected void onKuudraDeactivate() {
        visibleWaypoints = List.of();
        cachedPhase = KuudraPhase.NONE;
    }

    @Override
    protected void onPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        updateVisibleWaypoints(event.currentPhase());
    }

    private void onWorldRender(@NotNull WorldRenderEvent event) {
        if (mc.player == null || visibleWaypoints.isEmpty()) {
            return;
        }

        KuudraPhase phase = currentPhase();
        if (phase != cachedPhase) {
            updateVisibleWaypoints(phase);
        }

        Vec3 playerPos = mc.player.position();
        Set<String> renderedThisFrame = new HashSet<>();
        for (WaypointEntry entry : visibleWaypoints) {
            if (!shouldRenderCategory(entry.category(), phase) || !shouldRenderWaypointInPhase(entry.category(), entry.waypoint(), phase)) {
                continue;
            }

            EtherwarpWaypoint waypoint = entry.waypoint();
            for (int i = 0; i < waypoint.positions().size(); i++) {
                Vec3 pos = waypoint.positions().get(i);
                Vec3 renderPos = getRenderPosition(entry.category(), waypoint, pos, phase);
                if (waypoint.maxRenderDistance() > 0 && playerPos.distanceTo(renderPos) > waypoint.maxRenderDistance()) {
                    continue;
                }
                if (!renderedThisFrame.add(waypoint.getUniqueId(renderPos))) {
                    continue;
                }
                renderWaypoint(event, entry.category(), waypoint, renderPos, i);
            }
        }
    }

    private void updateVisibleWaypoints(@NotNull KuudraPhase phase) {
        List<WaypointEntry> next = new ArrayList<>();
        for (EtherwarpCategory category : cachedCategories) {
            if (!category.enabled()) {
                continue;
            }

            for (EtherwarpWaypoint waypoint : category.waypoints()) {
                if (shouldRenderWaypointInPhase(category, waypoint, phase)) {
                    next.add(new WaypointEntry(category, waypoint));
                }
            }
        }

        visibleWaypoints = List.copyOf(next);
        cachedPhase = phase;
    }

    public void reloadConfig() {
        List<EtherwarpCategory> categories = loadCategories();
        cachedCategories = categories;
        updateVisibleWaypoints(currentPhase());
    }

    protected @NotNull List<EtherwarpCategory> loadCategories() {
        return EtherwarpConfigLoader.get().reload();
    }

    protected abstract boolean shouldRenderCategory(@NotNull EtherwarpCategory category, @NotNull KuudraPhase phase);

    protected boolean shouldRenderWaypointInPhase(
            @NotNull EtherwarpCategory category,
            @NotNull EtherwarpWaypoint waypoint,
            @NotNull KuudraPhase phase
    ) {
        return KuudraWaypointRenderSupport.shouldRenderInPhase(waypoint, phase);
    }

    protected @NotNull Vec3 getRenderPosition(
            @NotNull EtherwarpCategory category,
            @NotNull EtherwarpWaypoint waypoint,
            @NotNull Vec3 position,
            @NotNull KuudraPhase phase
    ) {
        return position;
    }

    protected void renderWaypoint(
            @NotNull WorldRenderEvent event,
            @NotNull EtherwarpCategory category,
            @NotNull EtherwarpWaypoint waypoint,
            @NotNull Vec3 position,
            int colorIndex
    ) {
        KuudraWaypointRenderSupport.render(event, waypoint, position, colorIndex);
    }

    protected static boolean categoryContains(@NotNull EtherwarpCategory category, @NotNull String token) {
        return category.name().toLowerCase().contains(token.toLowerCase());
    }

    private record WaypointEntry(@NotNull EtherwarpCategory category, @NotNull EtherwarpWaypoint waypoint) {
    }
}
