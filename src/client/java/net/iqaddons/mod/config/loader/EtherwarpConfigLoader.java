package net.iqaddons.mod.config.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import net.iqaddons.mod.model.etherwarp.EtherwarpCategory;
import net.iqaddons.mod.model.etherwarp.EtherwarpWaypoint;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class EtherwarpConfigLoader {

    private static final EtherwarpConfigLoader INSTANCE = new EtherwarpConfigLoader();

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("iq");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("etherwarp_config.json");
    private static final String DEFAULT_RESOURCE = "/default-config/iq/etherwarp_config.json";

    private volatile List<EtherwarpCategory> cachedCategories = Collections.emptyList();

    /**
     * Carrega a configuração de waypoints do Etherwarp.
     * Tenta carregar do arquivo de config, com fallback para recurso bundled.
     */
    public @NotNull @UnmodifiableView List<EtherwarpCategory> load() {
        Path configPath = getConfigPath();

        try {
            if (Files.exists(configPath)) {
                log.info("Loading Etherwarp config from: {}", configPath);
                try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                    cachedCategories = parseJson(reader);
                    return cachedCategories;
                }
            }

            log.info("Loading Etherwarp config from bundled resource");
            try (InputStream is = getClass().getResourceAsStream(DEFAULT_RESOURCE)) {
                if (is == null) {
                    log.error("Default Etherwarp resource not found: {}", DEFAULT_RESOURCE);
                    cachedCategories = Collections.emptyList();
                    return cachedCategories;
                }

                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    cachedCategories = parseJson(reader);
                    saveDefaultConfig(configPath);
                    return cachedCategories;
                }
            }
        } catch (Exception e) {
            log.error("Failed to load Etherwarp config", e);
            cachedCategories = Collections.emptyList();
            return cachedCategories;
        }
    }

    /**
     * Recarrega a configuração de waypoints.
     */
    public @NotNull @UnmodifiableView List<EtherwarpCategory> reload() {
        log.info("Reloading Etherwarp config");
        return load();
    }

    /**
     * Retorna a configuração em cache.
     */
    public @NotNull @UnmodifiableView List<EtherwarpCategory> getCached() {
        return cachedCategories;
    }

    private @NotNull @UnmodifiableView List<EtherwarpCategory> parseJson(@NotNull Reader reader) {
        try {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray categoriesArray = root.getAsJsonArray("categories");
            if (categoriesArray == null) {
                log.warn("No 'categories' array found in Etherwarp config");
                return Collections.emptyList();
            }

            List<EtherwarpCategory> categories = new ArrayList<>();
            for (JsonElement element : categoriesArray) {
                if (element.isJsonObject()) {
                    parseCategory(element.getAsJsonObject()).ifPresent(categories::add);
                }
            }

            log.info("Loaded {} Etherwarp categories with {} total waypoints",
                    categories.size(),
                    categories.stream().mapToInt(c -> c.waypoints().size()).sum());

            return Collections.unmodifiableList(categories);
        } catch (Exception e) {
            log.error("Failed to parse Etherwarp JSON", e);
            return Collections.emptyList();
        }
    }

    private Optional<EtherwarpCategory> parseCategory(@NotNull JsonObject obj) {
        try {
            String name = obj.get("name").getAsString();
            boolean enabled = obj.has("enabled") && obj.get("enabled").getAsBoolean();
            if (!obj.has("enabled")) {
                enabled = true;
            }

            JsonArray waypointsArray = obj.getAsJsonArray("waypoints");
            if (waypointsArray == null) {
                return Optional.of(new EtherwarpCategory(name, enabled, List.of()));
            }

            List<EtherwarpWaypoint> waypoints = new ArrayList<>();
            for (JsonElement element : waypointsArray) {
                if (element.isJsonObject()) {
                    parseWaypoint(element.getAsJsonObject()).ifPresent(waypoints::add);
                }
            }

            return Optional.of(new EtherwarpCategory(name, enabled, waypoints));
        } catch (Exception e) {
            log.warn("Failed to parse Etherwarp category", e);
            return Optional.empty();
        }
    }

    private Optional<EtherwarpWaypoint> parseWaypoint(@NotNull JsonObject obj) {
        try {
            String name = obj.get("name").getAsString();

            List<Vec3d> positions = parsePositions(obj);

            // Parse color
            int colorRgb = parseColor(obj.get("color"));
            List<Integer> colorsRgb = parseColors(obj);

            // Parse alpha
            float alpha = obj.has("alpha") ? obj.get("alpha").getAsFloat() : 0.8f;
            alpha = Math.clamp(alpha, 0.0f, 1.0f);

            // Parse render style
            String styleStr = obj.has("renderStyle") ? obj.get("renderStyle").getAsString() : "OUTLINE";
            WorldRenderUtils.RenderStyle renderStyle = WorldRenderUtils.RenderStyle.valueOf(styleStr);

            // Parse line width
            float lineWidth = obj.has("lineWidth") ? obj.get("lineWidth").getAsFloat() : 2.0f;
            lineWidth = Math.clamp(lineWidth, 0.5f, 4.0f);

            // Parse phases
            Set<KuudraPhase> showInPhases = parsePhaseArray(obj, "showInPhases");
            Set<KuudraPhase> hideInPhases = parsePhaseArray(obj, "hideInPhases");

            // Parse max render distance
            float maxRenderDistance = obj.has("maxRenderDistance") ? obj.get("maxRenderDistance").getAsFloat() : -1f;

            EtherwarpWaypoint.HighlightShape shape = parseShape(obj);
            EtherwarpWaypoint.BoxSpec boxSpec = parseBoxSpec(obj);

            EtherwarpWaypoint waypoint = new EtherwarpWaypoint(
                    name,
                    positions,
                    colorRgb,
                    colorsRgb,
                    alpha,
                    renderStyle,
                    lineWidth,
                    showInPhases,
                    hideInPhases,
                    maxRenderDistance,
                    shape,
                    boxSpec
            );

            if (!waypoint.isValid()) {
                log.warn("Skipping invalid Etherwarp waypoint: {}", name);
                return Optional.empty();
            }

            return Optional.of(waypoint);
        } catch (Exception e) {
            log.warn("Failed to parse Etherwarp waypoint", e);
            return Optional.empty();
        }
    }

    private @NotNull List<Vec3d> parsePositions(@NotNull JsonObject obj) {
        Set<Vec3d> unique = new LinkedHashSet<>();

        if (obj.has("positions") && obj.get("positions").isJsonArray()) {
            JsonArray positionsArray = obj.getAsJsonArray("positions");
            for (JsonElement element : positionsArray) {
                if (!element.isJsonArray()) continue;
                parseVec3d(element.getAsJsonArray()).ifPresent(unique::add);
            }
        }

        // Compatibilidade com configs legadas que possuem apenas `position`.
        if (obj.has("position") && obj.get("position").isJsonArray()) {
            parseVec3d(obj.getAsJsonArray("position")).ifPresent(unique::add);
        }

        return List.copyOf(unique);
    }

    private Optional<Vec3d> parseVec3d(@NotNull JsonArray arr) {
        if (arr.size() != 3) {
            return Optional.empty();
        }

        try {
            return Optional.of(new Vec3d(
                    arr.get(0).getAsDouble(),
                    arr.get(1).getAsDouble(),
                    arr.get(2).getAsDouble()
            ));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private EtherwarpWaypoint.HighlightShape parseShape(@NotNull JsonObject obj) {
        if (!obj.has("shape")) {
            return EtherwarpWaypoint.HighlightShape.FULL;
        }

        try {
            String raw = obj.get("shape").getAsString().trim().toUpperCase().replace('-', '_').replace(' ', '_');
            return switch (raw) {
                case "UPPER_SLAB" -> EtherwarpWaypoint.HighlightShape.SLAB_UPPER;
                case "LOWER_SLAB" -> EtherwarpWaypoint.HighlightShape.SLAB_LOWER;
                case "UPPER_HALF", "TOP_HALF" -> EtherwarpWaypoint.HighlightShape.HALF_UPPER;
                case "LOWER_HALF", "BOTTOM_HALF" -> EtherwarpWaypoint.HighlightShape.HALF_LOWER;
                case "PLATE", "CENTER" -> EtherwarpWaypoint.HighlightShape.CENTER_PLATE;
                case "TOP_EDGE" -> EtherwarpWaypoint.HighlightShape.EDGE_TOP;
                case "BOTTOM_EDGE" -> EtherwarpWaypoint.HighlightShape.EDGE_BOTTOM;
                default -> EtherwarpWaypoint.HighlightShape.valueOf(raw);
            };
        } catch (Exception e) {
            log.warn("Unknown highlight shape '{}', using FULL", obj.get("shape"));
            return EtherwarpWaypoint.HighlightShape.FULL;
        }
    }

    private EtherwarpWaypoint.BoxSpec parseBoxSpec(@NotNull JsonObject obj) {
        if (!obj.has("box") || !obj.get("box").isJsonObject()) {
            return EtherwarpWaypoint.BoxSpec.DEFAULT;
        }

        try {
            JsonObject boxObj = obj.getAsJsonObject("box");
            JsonArray minArr = boxObj.getAsJsonArray("min");
            JsonArray maxArr = boxObj.getAsJsonArray("max");

            Optional<Vec3d> min = minArr != null ? parseVec3d(minArr) : Optional.empty();
            Optional<Vec3d> max = maxArr != null ? parseVec3d(maxArr) : Optional.empty();
            if (min.isEmpty() || max.isEmpty()) {
                return EtherwarpWaypoint.BoxSpec.DEFAULT;
            }

            Vec3d minVec = min.get();
            Vec3d maxVec = max.get();
            if (maxVec.x <= minVec.x || maxVec.y <= minVec.y || maxVec.z <= minVec.z) {
                log.warn("Invalid custom box extents, using default box");
                return EtherwarpWaypoint.BoxSpec.DEFAULT;
            }

            return new EtherwarpWaypoint.BoxSpec(minVec, maxVec);
        } catch (Exception e) {
            log.warn("Failed to parse custom box, using default", e);
            return EtherwarpWaypoint.BoxSpec.DEFAULT;
        }
    }

    private int parseColor(JsonElement colorElement) {
        if (colorElement == null || colorElement.isJsonNull()) {
            return 0xFFFFFF;
        }

        try {
            if (colorElement.isJsonArray()) {
                JsonArray rgb = colorElement.getAsJsonArray();
                if (rgb.size() == 3) {
                    int r = Math.clamp(rgb.get(0).getAsInt(), 0, 255);
                    int g = Math.clamp(rgb.get(1).getAsInt(), 0, 255);
                    int b = Math.clamp(rgb.get(2).getAsInt(), 0, 255);
                    return (r << 16) | (g << 8) | b;
                }
            }

            if (colorElement.isJsonPrimitive()) {
                String value = colorElement.getAsString().trim();
                if (value.startsWith("#")) {
                    value = value.substring(1);
                }

                if (value.length() == 6) {
                    return Integer.parseInt(value, 16);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse color value '{}', using white", colorElement);
        }

        return 0xFFFFFF;
    }

    private @NotNull List<Integer> parseColors(@NotNull JsonObject obj) {
        if (!obj.has("colors") || !obj.get("colors").isJsonArray()) {
            return List.of();
        }

        JsonArray colorsArray = obj.getAsJsonArray("colors");
        List<Integer> parsed = new ArrayList<>(colorsArray.size());

        for (JsonElement element : colorsArray) {
            parsed.add(parseColor(element));
        }

        return List.copyOf(parsed);
    }

    private Set<KuudraPhase> parsePhaseArray(@NotNull JsonObject obj, @NotNull String fieldName) {
        Set<KuudraPhase> phases = EnumSet.noneOf(KuudraPhase.class);

        if (!obj.has(fieldName)) {
            return phases;
        }

        JsonArray phaseArray = obj.getAsJsonArray(fieldName);
        for (JsonElement element : phaseArray) {
            try {
                phases.add(KuudraPhase.valueOf(element.getAsString()));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown phase in Etherwarp config: {}", element.getAsString());
            }
        }

        return phases;
    }

    private Path getConfigPath() {
        return CONFIG_FILE;
    }

    private void saveDefaultConfig(@NotNull Path configPath) {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(configPath)) {
                try (InputStream is = getClass().getResourceAsStream(DEFAULT_RESOURCE)) {
                    if (is != null) {
                        Files.copy(is, configPath);
                        log.info("Copied default Etherwarp config to: {}", configPath);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to save default Etherwarp config", e);
        }
    }

    public static EtherwarpConfigLoader get() {
        return INSTANCE;
    }
}



