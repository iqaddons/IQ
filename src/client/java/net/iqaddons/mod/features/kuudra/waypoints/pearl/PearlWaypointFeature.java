package net.iqaddons.mod.features.kuudra.waypoints.pearl;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.features.kuudra.waypoints.pearl.data.PearlWaypoint;
import net.iqaddons.mod.features.kuudra.waypoints.pearl.data.WaypointArea;
import net.iqaddons.mod.config.loader.WaypointConfigLoader;
import net.iqaddons.mod.state.SupplyStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.features.kuudra.waypoints.pearl.area.AreaDetection;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Slf4j
public class PearlWaypointFeature extends KuudraFeature {

    private static final int AREA_CHECK_INTERVAL = 2;

    private final WaypointConfigLoader configLoader;
    private final AreaDetection areaDetection;

    private final SupplyStateManager supplyState = SupplyStateManager.get();

    public PearlWaypointFeature() {
        super(
                "pearlWaypoints",
                "Pearl Waypoints",
                () -> PhaseOneConfig.pearlWaypoints,
                KuudraPhase.SUPPLIES
        );

        this.configLoader = new WaypointConfigLoader();
        this.configLoader.load();

        this.areaDetection = new AreaDetection();
    }

    @Override
    protected void onKuudraActivate() {
        List<WaypointArea> areas = configLoader.getCached();
        areaDetection.setAreas(areas);

        subscribe(EventBus.subscribe(ClientTickEvent.class, this::onTick));
        subscribe(EventBus.subscribe(WorldRenderEvent.class, this::onRender));

        log.info("Pearl waypoints activated with {} areas", areas.size());
    }

    @Override
    protected void onKuudraDeactivate() {
        areaDetection.reset();
        log.info("Pearl waypoints deactivated");
    }

    public void reload() {
        List<WaypointArea> areas = configLoader.reload();
        areaDetection.setAreas(areas);
        log.info("Reloaded pearl waypoints: {} areas", areas.size());
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (event.isNthTick(AREA_CHECK_INTERVAL)) {
            areaDetection.update();
        }
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
        Vec3d target = waypoint.target();
        if (target.x == 0 && target.y == 0 && target.z == 0) return;

        float size = waypoint.size();
        float half = size / 2f;

        Box targetBox = new Box(
                target.getX() - half - 0.5, target.getY(), target.getZ() - half - 0.5,
                target.getX() + half - 0.5, target.getY() + size, target.getZ() + half - 0.5
        );

        event.drawFilled(targetBox, true, waypoint.color());
        if (!waypoint.label().isEmpty()) {
            Vec3d textPos = new Vec3d(target.getX() - 0.5, target.getY() - 1.5, target.getZ() - 0.5);
            event.drawText(textPos, Text.literal(waypoint.label()), 0.25f, true, RenderColor.white);
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
}
