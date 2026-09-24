package net.iqaddons.mod.features.kuudra.waypoints;

import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.model.etherwarp.EtherwarpCategory;
import net.iqaddons.mod.model.etherwarp.EtherwarpWaypoint;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class DpsWaypointFeature extends AbstractEtherwarpWaypointFeature {

    public DpsWaypointFeature() {
        super(
                "dpsWaypoint",
                "DPS Waypoint",
                () -> PhaseThreeConfig.dpsWaypoint,
                KuudraPhase.EATEN, KuudraPhase.DPS
        );
    }

    @Override
    protected boolean shouldRenderCategory(@NotNull EtherwarpCategory category, @NotNull KuudraPhase phase) {
        return categoryContains(category, "dps");
    }

    @Override
    protected void renderWaypoint(
            @NotNull WorldRenderEvent event,
            @NotNull EtherwarpCategory category,
            @NotNull EtherwarpWaypoint waypoint,
            @NotNull Vec3 position,
            int colorIndex
    ) {
        KuudraWaypointRenderSupport.render(
                event,
                waypoint,
                position,
                RenderColor.fromArgb(PhaseThreeConfig.DpsWaypointConfig.waypointColor),
                PhaseThreeConfig.DpsWaypointConfig.showText,
                PhaseThreeConfig.DpsWaypointConfig.textSize
        );
    }
}
