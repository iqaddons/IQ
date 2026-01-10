package net.iqaddons.mod.loader;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import net.iqaddons.mod.features.kuudra.waypoints.data.PearlWaypoint;
import net.iqaddons.mod.features.kuudra.waypoints.data.WaypointArea;
import net.iqaddons.mod.utils.BoundingBox2D;
import net.iqaddons.mod.utils.RenderColor;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class WaypointConfigLoader {

    private static final String CONFIG_DIR = "config/iq";
    private static final String DEFAULT_RESOURCE = "/data/iq/pearl_waypoints.json";
    private static final String CONFIG_FILE = "pearl_waypoints.json";

    private volatile List<WaypointArea> cachedAreas = Collections.emptyList();

    public @NotNull List<WaypointArea> load() {
        Path configPath = getConfigPath();

        try {
            if (Files.exists(configPath)) {
                log.info("Loading pearl waypoints from: {}", configPath);
                try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                    cachedAreas = parseJson(reader);
                    return cachedAreas;
                }
            }

            log.info("Loading pearl waypoints from bundled resource");
            try (InputStream is = getClass().getResourceAsStream(DEFAULT_RESOURCE)) {
                if (is == null) {
                    log.error("Default waypoint resource not found: {}", DEFAULT_RESOURCE);
                    cachedAreas = Collections.emptyList();
                    return cachedAreas;
                }

                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    cachedAreas = parseJson(reader);
                    saveDefaultConfig(configPath);

                    return cachedAreas;
                }
            }
        } catch (Exception e) {
            log.error("Failed to load pearl waypoints", e);
            cachedAreas = Collections.emptyList();
            return cachedAreas;
        }
    }

    public @NotNull List<WaypointArea> reload() {
        log.info("Reloading pearl waypoints configuration");
        return load();
    }

    public @NotNull List<WaypointArea> getCached() {
        return cachedAreas;
    }

    private @NotNull @UnmodifiableView List<WaypointArea> parseJson(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        JsonArray areasArray = root.getAsJsonArray("areas");

        List<WaypointArea> areas = new ArrayList<>(areasArray.size());

        for (JsonElement element : areasArray) {
            parseArea(element.getAsJsonObject()).ifPresent(areas::add);
        }

        log.info("Loaded {} waypoint areas", areas.size());
        return Collections.unmodifiableList(areas);
    }

    private Optional<WaypointArea> parseArea(JsonObject obj) {
        try {
            String name = obj.get("name").getAsString();

            JsonArray pos1 = obj.getAsJsonArray("pos1");
            JsonArray pos2 = obj.getAsJsonArray("pos2");

            BoundingBox2D bounds = BoundingBox2D.fromCorners(
                    pos1.get(0).getAsDouble(),
                    pos1.get(1).getAsDouble(),
                    pos2.get(0).getAsDouble(),
                    pos2.get(1).getAsDouble()
            );

            List<PearlWaypoint> waypoints = new ArrayList<>();
            JsonArray waypointsArray = obj.getAsJsonArray("waypoints");
            for (JsonElement wpElement : waypointsArray) {
                parseWaypoint(wpElement.getAsJsonObject()).ifPresent(waypoints::add);
            }

            return Optional.of(new WaypointArea(name, bounds, waypoints));
        } catch (Exception e) {
            log.warn("Failed to parse waypoint area", e);
            return Optional.empty();
        }
    }

    private Optional<PearlWaypoint> parseWaypoint(JsonObject obj) {
        try {
            JsonArray coords = obj.getAsJsonArray("coords");
            Vec3d target = new Vec3d(
                    coords.get(0).getAsDouble(),
                    coords.get(1).getAsDouble(),
                    coords.get(2).getAsDouble()
            );

            JsonArray rgb = obj.getAsJsonArray("rgb");
            RenderColor color = new RenderColor(
                    rgb.get(0).getAsInt(),
                    rgb.get(1).getAsInt(),
                    rgb.get(2).getAsInt(),
                    0xff
            );

            Vec3d standBlock = null;
            if (obj.has("block")) {
                JsonArray block = obj.getAsJsonArray("block");
                standBlock = new Vec3d(
                        block.get(0).getAsDouble(),
                        block.get(1).getAsDouble(),
                        block.get(2).getAsDouble()
                );
            }

            Integer preSupply = obj.has("pre") ? obj.get("pre").getAsInt() : null;
            Integer hideForPre = obj.has("hideForPre") ? obj.get("hideForPre").getAsInt() : null;
            float size = obj.has("size") ? obj.get("size").getAsFloat() : PearlWaypoint.DEFAULT_SIZE;
            String label = obj.has("text") ? obj.get("text").getAsString() : "";

            return Optional.of(new PearlWaypoint(target, color, standBlock, preSupply, hideForPre, size, label));
        } catch (Exception e) {
            log.warn("Failed to parse waypoint", e);
            return Optional.empty();
        }
    }

    private @NotNull Path getConfigPath() {
        return FabricLoader.getInstance()
                .getGameDir()
                .resolve(CONFIG_DIR)
                .resolve(CONFIG_FILE);
    }

    private void saveDefaultConfig(Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());

            try (InputStream is = getClass().getResourceAsStream(DEFAULT_RESOURCE)) {
                if (is != null) {
                    Files.copy(is, configPath);
                    log.info("Created default config at: {}", configPath);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to save default config", e);
        }
    }
}
