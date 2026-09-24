package net.iqaddons.mod.config.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.iqaddons.mod.IQModClient;
import net.iqaddons.mod.config.loader.EtherwarpConfigLoader;
import net.iqaddons.mod.features.kuudra.waypoints.AbstractEtherwarpWaypointFeature;
import net.iqaddons.mod.features.kuudra.waypoints.DpsWaypointFeature;
import net.iqaddons.mod.features.kuudra.waypoints.EtherwarpWaypointsFeature;
import net.iqaddons.mod.features.kuudra.waypoints.SkipWaypointFeature;
import net.iqaddons.mod.model.etherwarp.EtherwarpCategory;
import net.iqaddons.mod.model.etherwarp.EtherwarpWaypoint;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class KuudraWaypointConfigEditor {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonObject root = new JsonObject();

    void load() {
        Path path = EtherwarpConfigLoader.get().getConfigPath();
        try {
            if (!Files.exists(path)) {
                EtherwarpConfigLoader.get().reload();
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            if (!root.has("categories")) {
                root.add("categories", new JsonArray());
            }
        } catch (Exception e) {
            root = new JsonObject();
            root.add("categories", new JsonArray());
        }
    }

    @NotNull List<String> categoryNames() {
        List<String> names = new ArrayList<>();
        for (JsonElement element : categories()) {
            if (element.isJsonObject()) {
                names.add(getString(element.getAsJsonObject(), "name", "Unnamed"));
            }
        }
        return names;
    }

    @NotNull List<String> waypointNames(int categoryIndex) {
        List<String> names = new ArrayList<>();
        JsonObject category = category(categoryIndex);
        if (category == null) return names;
        for (JsonElement element : waypoints(category)) {
            if (element.isJsonObject()) {
                names.add(getString(element.getAsJsonObject(), "name", "Waypoint"));
            }
        }
        return names;
    }

    @Nullable JsonObject waypoint(int categoryIndex, int waypointIndex) {
        JsonObject category = category(categoryIndex);
        if (category == null) return null;
        JsonArray waypoints = waypoints(category);
        if (waypointIndex < 0 || waypointIndex >= waypoints.size()) return null;
        JsonElement element = waypoints.get(waypointIndex);
        return element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    void toggleCategory(int categoryIndex) {
        JsonObject category = category(categoryIndex);
        if (category == null) return;
        boolean enabled = !getBoolean(category, "enabled", true);
        category.addProperty("enabled", enabled);
        saveAndReload();
    }

    void cycleWaypointPhase(int categoryIndex, int waypointIndex) {
        JsonObject waypoint = waypoint(categoryIndex, waypointIndex);
        if (waypoint == null) return;
        JsonArray phases = waypoint.has("showInPhases") && waypoint.get("showInPhases").isJsonArray()
                ? waypoint.getAsJsonArray("showInPhases")
                : new JsonArray();
        KuudraPhase current = phases.size() > 0 ? parsePhase(phases.get(0).getAsString()) : KuudraPhase.SUPPLIES;
        KuudraPhase next = switch (current) {
            case SUPPLIES -> KuudraPhase.EATEN;
            case EATEN -> KuudraPhase.DPS;
            case DPS -> KuudraPhase.SKIP;
            case SKIP -> KuudraPhase.SUPPLIES;
            default -> KuudraPhase.SUPPLIES;
        };
        JsonArray nextPhases = new JsonArray();
        nextPhases.add(next.name());
        waypoint.add("showInPhases", nextPhases);
        saveAndReload();
    }

    void cycleRenderStyle(int categoryIndex, int waypointIndex) {
        cycleEnum(categoryIndex, waypointIndex, "renderStyle", WorldRenderUtils.RenderStyle.values(), WorldRenderUtils.RenderStyle.OUTLINE.name());
    }

    void cycleMarkerStyle(int categoryIndex, int waypointIndex) {
        cycleEnum(categoryIndex, waypointIndex, "markerStyle", EtherwarpWaypoint.WaypointMarkerStyle.values(), EtherwarpWaypoint.WaypointMarkerStyle.SOLID.name());
    }

    void cycleTextPosition(int categoryIndex, int waypointIndex) {
        cycleEnum(categoryIndex, waypointIndex, "textPosition", EtherwarpWaypoint.TextPosition.values(), EtherwarpWaypoint.TextPosition.ABOVE.name());
    }

    void bumpTextScale(int categoryIndex, int waypointIndex) {
        JsonObject waypoint = waypoint(categoryIndex, waypointIndex);
        if (waypoint == null) return;
        float value = getFloat(waypoint, "textScale", 0.05f) + 0.01f;
        if (value > 0.16f) value = 0.02f;
        waypoint.addProperty("textScale", value);
        saveAndReload();
    }

    void cycleColor(int categoryIndex, int waypointIndex) {
        JsonObject waypoint = waypoint(categoryIndex, waypointIndex);
        if (waypoint == null) return;
        String current = getString(waypoint, "color", "#00FFFF").toUpperCase();
        String next = switch (current) {
            case "#00FFFF" -> "#55FF55";
            case "#55FF55" -> "#FFFF55";
            case "#FFFF55" -> "#FFAA00";
            case "#FFAA00" -> "#FF55AA";
            case "#FF55AA" -> "#AA55FF";
            case "#AA55FF" -> "#FF5555";
            default -> "#00FFFF";
        };
        waypoint.addProperty("color", next);
        waypoint.remove("colors");
        saveAndReload();
    }

    void cycleText(int categoryIndex, int waypointIndex) {
        JsonObject waypoint = waypoint(categoryIndex, waypointIndex);
        if (waypoint == null) return;
        String current = getString(waypoint, "text", getString(waypoint, "name", "WP"));
        String next = switch (current.toUpperCase()) {
            case "DPS" -> "SKIP";
            case "SKIP" -> "X";
            case "X" -> "=";
            case "=" -> "TRI";
            case "TRI" -> "/";
            case "/" -> "WP";
            default -> "DPS";
        };
        waypoint.addProperty("text", next);
        saveAndReload();
    }

    void duplicateWaypoint(int categoryIndex, int waypointIndex) {
        JsonObject category = category(categoryIndex);
        JsonObject waypoint = waypoint(categoryIndex, waypointIndex);
        if (category == null || waypoint == null) return;
        JsonObject copy = waypoint.deepCopy();
        copy.addProperty("name", getString(waypoint, "name", "Waypoint") + " Copy");
        waypoints(category).add(copy);
        saveAndReload();
    }

    void removeWaypoint(int categoryIndex, int waypointIndex) {
        JsonObject category = category(categoryIndex);
        if (category == null) return;
        JsonArray waypoints = waypoints(category);
        if (waypointIndex < 0 || waypointIndex >= waypoints.size()) return;
        waypoints.remove(waypointIndex);
        saveAndReload();
    }

    @NotNull String summary(int categoryIndex, int waypointIndex) {
        JsonObject waypoint = waypoint(categoryIndex, waypointIndex);
        if (waypoint == null) return "Select a waypoint";
        return "Phase: " + firstPhase(waypoint)
                + "  Style: " + getString(waypoint, "renderStyle", "OUTLINE")
                + "  Marker: " + getString(waypoint, "markerStyle", "SOLID")
                + "  Text: " + getString(waypoint, "textPosition", "ABOVE")
                + "  Scale: " + getFloat(waypoint, "textScale", 0.05f);
    }

    private void cycleEnum(int categoryIndex, int waypointIndex, @NotNull String field, @NotNull Enum<?>[] values, @NotNull String fallback) {
        JsonObject waypoint = waypoint(categoryIndex, waypointIndex);
        if (waypoint == null || values.length == 0) return;
        String current = getString(waypoint, field, fallback);
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].name().equalsIgnoreCase(current)) {
                index = i;
                break;
            }
        }
        waypoint.addProperty(field, values[(index + 1) % values.length].name());
        saveAndReload();
    }

    private void saveAndReload() {
        try {
            Path path = EtherwarpConfigLoader.get().getConfigPath();
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception ignored) {
        }

        EtherwarpConfigLoader.get().reload();
        IQModClient client = IQModClient.get();
        if (client == null || client.getFeatureManager() == null) return;
        reload(client.getFeatureManager().get(EtherwarpWaypointsFeature.class));
        reload(client.getFeatureManager().get(DpsWaypointFeature.class));
        reload(client.getFeatureManager().get(SkipWaypointFeature.class));
    }

    private void reload(@Nullable AbstractEtherwarpWaypointFeature feature) {
        if (feature != null) {
            feature.reloadConfig();
        }
    }

    private JsonArray categories() {
        return root.getAsJsonArray("categories");
    }

    private @Nullable JsonObject category(int index) {
        JsonArray categories = categories();
        if (index < 0 || index >= categories.size()) return null;
        JsonElement element = categories.get(index);
        return element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private JsonArray waypoints(@NotNull JsonObject category) {
        if (!category.has("waypoints") || !category.get("waypoints").isJsonArray()) {
            category.add("waypoints", new JsonArray());
        }
        return category.getAsJsonArray("waypoints");
    }

    private @NotNull String firstPhase(@NotNull JsonObject waypoint) {
        if (!waypoint.has("showInPhases") || !waypoint.get("showInPhases").isJsonArray()) {
            return "SUPPLIES";
        }
        JsonArray phases = waypoint.getAsJsonArray("showInPhases");
        return phases.size() > 0 ? phases.get(0).getAsString() : "SUPPLIES";
    }

    private KuudraPhase parsePhase(@NotNull String value) {
        try {
            return KuudraPhase.valueOf(value);
        } catch (Exception e) {
            return KuudraPhase.SUPPLIES;
        }
    }

    private boolean getBoolean(@NotNull JsonObject object, @NotNull String field, boolean fallback) {
        try {
            return object.has(field) ? object.get(field).getAsBoolean() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private float getFloat(@NotNull JsonObject object, @NotNull String field, float fallback) {
        try {
            return object.has(field) ? object.get(field).getAsFloat() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private @NotNull String getString(@NotNull JsonObject object, @NotNull String field, @NotNull String fallback) {
        try {
            return object.has(field) ? object.get(field).getAsString() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

}
