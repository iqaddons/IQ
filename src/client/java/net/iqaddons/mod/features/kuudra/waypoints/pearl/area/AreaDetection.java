package net.iqaddons.mod.features.kuudra.waypoints.pearl.area;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.features.kuudra.waypoints.pearl.data.WaypointArea;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@Slf4j
public class AreaDetection {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private volatile List<WaypointArea> areas = List.of();
    private volatile @Nullable WaypointArea currentArea = null;

    public void setAreas(@NotNull List<WaypointArea> areas) {
        this.areas = areas;
        this.currentArea = null;
    }

    public void update() {
        if (mc.player == null) {
            currentArea = null;
            return;
        }

        double x = mc.player.getX();
        double z = mc.player.getZ();
        if (currentArea != null && Objects.requireNonNull(currentArea).containsPlayer(x, z)) {
            return;
        }

        WaypointArea previous = currentArea;
        currentArea = findAreaContaining(x, z);
        if (previous != currentArea) {
            String from = previous != null ? previous.name() : "none";
            String to = currentArea != null ? Objects.requireNonNull(currentArea).name() : "none";
            log.debug("Pearl area: {} -> {}", from, to);
        }
    }

    public @Nullable WaypointArea getCurrentArea() {
        return currentArea;
    }

    public boolean isInArea() {
        return currentArea != null;
    }

    public void reset() {
        currentArea = null;
    }

    private @Nullable WaypointArea findAreaContaining(double x, double z) {
        for (WaypointArea area : areas) {
            if (area.containsPlayer(x, z)) {
                return area;
            }
        }
        return null;
    }
}
