package net.iqaddons.mod.config.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.iqaddons.mod.IQModClient;
import net.iqaddons.mod.config.loader.EtherwarpConfigLoader;
import net.iqaddons.mod.config.loader.PearlWaypointConfigLoader;
import net.iqaddons.mod.config.preset.BuiltInPearlWaypoints;
import net.iqaddons.mod.features.kuudra.waypoints.DpsWaypointFeature;
import net.iqaddons.mod.features.kuudra.waypoints.EtherwarpWaypointsFeature;
import net.iqaddons.mod.features.kuudra.waypoints.PearlWaypointFeature;
import net.iqaddons.mod.features.kuudra.waypoints.SkipWaypointFeature;
import net.iqaddons.mod.model.etherwarp.EtherwarpWaypoint;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.model.pearl.PearlTrajectoryType;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class KuudraWaypointEditorStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String WAYPOINT_PRESET_PREFIX = "IQW1:";

    private JsonObject pearlRoot = new JsonObject();
    private JsonObject etherRoot = new JsonObject();
    private JsonObject customRoot = new JsonObject();
    private boolean dirty = false;

    public void load() {
        if (dirty) return;
        PearlWaypointConfigLoader.get().reload();
        EtherwarpConfigLoader.getNewEtherwarpWaypoints().reload();
        EtherwarpConfigLoader.get().reload();
        pearlRoot = readJson(PearlWaypointConfigLoader.get().getConfigPath(), "areas");
        etherRoot = readJson(EtherwarpConfigLoader.getNewEtherwarpWaypoints().getConfigPath(), "categories");
        customRoot = readJson(EtherwarpConfigLoader.get().getConfigPath(), "categories");
    }

    public @NotNull List<String> mainCategories() {
        return List.of("Pearl Waypoints", "Etherwarp Waypoints", "Custom Waypoints");
    }

    public @NotNull List<String> subcategories(int main) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        switch (main) {
            case 0 -> {
                out.add("Flat Pearls");
                out.add("Sky Pearls");
                out.add("Double Pearls");
                out.add("Areas");
                out.add("Stand Blocks");
                appendCustomPearlSubcategories(out);
            }
            case 1 -> {
                out.add("X");
                out.add("Equals");
                out.add("Triangle");
                out.add("Slash");
                out.add("Areas");
                appendCustomEtherCategories(out, main);
            }
            case 2 -> {
                out.add("DPS");
                out.add("SKIP");
                out.add("Other Custom");
                appendCustomEtherCategories(out, main);
            }
        }
        return List.copyOf(out);
    }

    public @NotNull List<Item> items(int main, @NotNull String subcategory) {
        List<Item> out = new ArrayList<>();
        if (main == 0) {
            collectPearlItems(out, subcategory);
        } else {
            collectEtherItems(out, main, subcategory);
        }
        return out;
    }

    public int indexOfItem(int main, @NotNull String subcategory, @NotNull Item target) {
        List<Item> currentItems = items(main, subcategory);
        for (int i = 0; i < currentItems.size(); i++) {
            if (currentItems.get(i).object == target.object) return i;
        }
        return -1;
    }

    public boolean isMainVisible(int main) {
        return bool(rootForMain(main), "enabled", true);
    }

    public void toggleMainVisibility(int main) {
        JsonObject root = rootForMain(main);
        root.addProperty("enabled", !bool(root, "enabled", true));
        saveAll();
    }

    public boolean isSubcategoryVisible(int main, @NotNull String subcategory) {
        if (main == 0) return !containsString(array(pearlRoot, "hiddenSubcategories"), subcategory);
        if (main == 1 && subcategory.equals("Areas")) return anyEnabled(array(etherRoot, "areas"));
        boolean found = false;
        for (JsonElement element : array(etherRootFor(main), "categories")) {
            if (!element.isJsonObject()) continue;
            JsonObject category = element.getAsJsonObject();
            if (!matchesEtherSubcategory(main, subcategory, string(category, "name", ""))) continue;
            found = true;
            if (bool(category, "enabled", true)) return true;
        }
        return !found || isDefaultSubcategory(main, subcategory);
    }

    public void toggleSubcategoryVisibility(int main, @NotNull String subcategory) {
        if (main == 0) {
            toggleString(array(pearlRoot, "hiddenSubcategories"), subcategory);
            saveAll();
            return;
        }
        if (main == 1 && subcategory.equals("Areas")) {
            boolean next = !anyEnabled(array(etherRoot, "areas"));
            for (JsonElement element : array(etherRoot, "areas")) {
                if (element.isJsonObject()) element.getAsJsonObject().addProperty("enabled", next);
            }
            saveAll();
            return;
        }
        boolean next = !isSubcategoryVisible(main, subcategory);
        for (JsonElement element : array(etherRootFor(main), "categories")) {
            if (!element.isJsonObject()) continue;
            JsonObject category = element.getAsJsonObject();
            if (matchesEtherSubcategory(main, subcategory, string(category, "name", ""))) {
                category.addProperty("enabled", next);
            }
        }
        saveAll();
    }

    public boolean isItemVisible(@NotNull Item item) {
        return bool(item.object, "enabled", true);
    }

    public void toggleItemVisibility(@NotNull Item item) {
        item.object.addProperty("enabled", !isItemVisible(item));
        saveAll();
    }

    public void cyclePhase(@NotNull Item item) {
        if (item.source == Source.PEARL) {
            setPearlField(item, "phase", nextPhase(string(item.object, "phase", "SUPPLIES")));
        } else {
            JsonArray phases = new JsonArray();
            phases.add(nextPhase(firstPhase(item.object)));
            item.object.add("showInPhases", phases);
        }
        saveAll();
    }

    public void cycleRenderStyle(@NotNull Item item) {
        if (item.source == Source.PEARL) {
            setPearlField(item, "renderStyle", nextEnum(string(item.object, "renderStyle", "SQUARE"), List.of("SQUARE", "CIRCLE", "BLOCK_OUTLINE", "FILLED_OUTLINE", "FULL_BLOCK")));
        } else {
            item.object.addProperty("renderStyle", nextEnum(string(item.object, "renderStyle", "OUTLINE"), enumNames(WorldRenderUtils.RenderStyle.values())));
        }
        saveAll();
    }

    public void cycleMarkerStyle(@NotNull Item item) {
        if (item.source == Source.PEARL) {
            setPearlField(item, "markerStyle", nextEnum(string(item.object, "markerStyle", "TARGET"), List.of("TARGET", "SOLID")));
        } else {
            item.object.addProperty("markerStyle", nextEnum(string(item.object, "markerStyle", "SOLID"), enumNames(EtherwarpWaypoint.WaypointMarkerStyle.values())));
        }
        saveAll();
    }

    public void cyclePearlStyle(@NotNull Item item) {
        if (item.source != Source.PEARL) return;
        String current = string(item.object, "trajectoryType", "FLAT");
        String next = switch (current) {
            case "FLAT" -> "SKY";
            case "SKY" -> "DOUBLE_HIGH";
            default -> "FLAT";
        };
        item.object.addProperty("trajectoryType", next);
        saveAll();
    }

    public @NotNull String pearlStyleSubcategory(@NotNull Item item) {
        String trajectory = string(item.object, "trajectoryType", "FLAT");
        if (trajectory.equals("SKY")) return "Sky Pearls";
        if (trajectory.startsWith("DOUBLE")) return "Double Pearls";
        return "Flat Pearls";
    }

    public void cycleColor(@NotNull Item item) {
        String current = string(item.object, "color", "#00FFFF").toUpperCase(Locale.ROOT);
        String next = switch (current) {
            case "#00FFFF" -> "#55FF55";
            case "#55FF55" -> "#FFFF55";
            case "#FFFF55" -> "#FFAA00";
            case "#FFAA00" -> "#FF55AA";
            case "#FF55AA" -> "#AA55FF";
            case "#AA55FF" -> "#FF5555";
            default -> "#00FFFF";
        };
        item.object.addProperty("color", next);
        item.object.remove("colors");
        saveAll();
    }

    public void renameItem(@NotNull Item item, @NotNull String name) {
        String clean = name.trim();
        if (clean.isBlank()) return;
        if (item.source == Source.PEARL_AREA) {
            item.object.addProperty("name", uniquePearlAreaName(clean));
        } else if (item.source == Source.ETHER_AREA) {
            item.object.addProperty("name", uniqueEtherAreaName(clean));
        } else if (item.source == Source.PEARL_STAND_BLOCK) {
            item.object.addProperty("standBlockLabel", clean);
        } else if (item.source == Source.PEARL) {
            item.object.addProperty("label", clean);
        } else {
            item.object.addProperty("name", clean);
        }
        saveAll();
    }

    public void renameSubcategory(int main, @NotNull String subcategory, @NotNull String name) {
        String clean = name.trim();
        if (clean.isBlank() || isDefaultSubcategory(main, subcategory)) return;
        if (main == 0) {
            String unique = uniquePearlSubcategoryName(clean);
            JsonArray subs = array(pearlRoot, "customSubcategories");
            for (int i = 0; i < subs.size(); i++) {
                if (subcategory.equalsIgnoreCase(subs.get(i).getAsString())) {
                    subs.set(i, new com.google.gson.JsonPrimitive(unique));
                    renamePearlWaypointSubcategory(subcategory, unique);
                    break;
                }
            }
            saveAll();
            return;
        }
        for (JsonElement element : array(etherRootFor(main), "categories")) {
            if (element.isJsonObject() && subcategory.equalsIgnoreCase(string(element.getAsJsonObject(), "name", ""))) {
                element.getAsJsonObject().addProperty("name", clean);
                break;
            }
        }
        saveAll();
    }

    public void nudgeTarget(@NotNull Item item, double dx, double dy, double dz) {
        if (item.source == Source.PEARL) {
            String field = pearlCoordinateField(item.object);
            item.object.add(field, shifted(item.object.getAsJsonArray(field), dx, dy, dz));
            if (field.equals("target")) item.object.add("aimTarget", shifted(item.object.getAsJsonArray("aimTarget"), dx, dy, dz));
        } else if (item.object.has("position") && item.object.get("position").isJsonArray()) {
            item.object.add("position", shifted(item.object.getAsJsonArray("position"), dx, dy, dz));
        } else if (item.object.has("positions") && item.object.get("positions").isJsonArray()) {
            JsonArray out = new JsonArray();
            for (JsonElement element : item.object.getAsJsonArray("positions")) {
                out.add(element.isJsonArray() ? shifted(element.getAsJsonArray(), dx, dy, dz) : element);
            }
            item.object.add("positions", out);
        }
        saveAll();
    }

    public double coordinate(@NotNull Item item, int axis) {
        JsonArray arr = coordinateArray(item);
        if (arr == null || arr.size() <= axis) return 0.0;
        return arr.get(axis).getAsDouble();
    }

    public void setCoordinate(@NotNull Item item, int axis, double value) {
        JsonArray arr = coordinateArray(item);
        if (arr == null || arr.size() <= axis) return;
        if (item.source == Source.PEARL_AREA || item.source == Source.ETHER_AREA) {
            JsonArray next = new JsonArray();
            for (int i = 0; i < arr.size(); i++) {
                next.add(i == axis ? value : arr.get(i).getAsDouble());
            }
            item.object.add("bounds", next);
            saveAll();
            return;
        }
        if (arr.size() < 3) return;
        JsonArray next = vec(
                axis == 0 ? value : arr.get(0).getAsDouble(),
                axis == 1 ? value : arr.get(1).getAsDouble(),
                axis == 2 ? value : arr.get(2).getAsDouble()
        );
        if (item.source == Source.PEARL_STAND_BLOCK) {
            item.object.add("standBlock", next);
        } else if (item.source == Source.PEARL) {
            String field = pearlCoordinateField(item.object);
            item.object.add(field, next);
            if (field.equals("target")) item.object.add("aimTarget", next.deepCopy());
        } else if (item.object.has("position") && item.object.get("position").isJsonArray()) {
            item.object.add("position", next);
        } else if (item.object.has("positions") && item.object.get("positions").isJsonArray() && !item.object.getAsJsonArray("positions").isEmpty()) {
            item.object.getAsJsonArray("positions").set(0, next);
        }
        saveAll();
    }

    public boolean editableCoordinates(@NotNull Item item) {
        return coordinateArray(item) != null;
    }

    public String coordinateLabel(@NotNull Item item) {
        if (item.source == Source.PEARL_AREA || item.source == Source.ETHER_AREA) return "Area Bounds";
        if (item.source == Source.PEARL_STAND_BLOCK) return "Stand Block";
        if (item.source == Source.PEARL) return pearlCoordinateField(item.object).equals("standBlock") ? "Stand Block" : "Target";
        if (item.source == Source.ETHER || item.source == Source.NEW_ETHER) return markerStyle(item).equals("TARGET") ? "Target" : "Position";
        return "Bounds";
    }

    public String markerStyle(@NotNull Item item) {
        if (item.source == Source.PEARL_AREA || item.source == Source.ETHER_AREA) return "AREA";
        return string(item.object, "markerStyle", item.source == Source.PEARL || item.source == Source.PEARL_STAND_BLOCK ? "TARGET" : "SOLID");
    }

    public void duplicate(@NotNull Item item) {
        JsonObject copy = item.object.deepCopy();
        if (item.source == Source.PEARL_AREA) {
            copy.addProperty("name", uniquePearlAreaName(item.title + " Copy"));
            array(pearlRoot, "areas").add(copy);
        } else if (item.source == Source.ETHER_AREA) {
            copy.addProperty("name", uniqueEtherAreaName(item.title + " Copy"));
            array(etherRoot, "areas").add(copy);
        } else if (item.source == Source.PEARL) {
            copy.addProperty("label", item.title + " Copy");
            parentPearlWaypoints(item).add(copy);
        } else if (item.source == Source.PEARL_STAND_BLOCK) {
            copy.addProperty("standBlockLabel", item.title + " Copy");
            parentPearlWaypoints(item).add(copy);
        } else {
            copy.addProperty("name", item.title + " Copy");
            parentEtherWaypoints(item).add(copy);
        }
        saveAll();
    }

    public void moveToNextPearlArea(@NotNull Item item) {
        if (item.source != Source.PEARL || item.parent == null) return;
        JsonArray areas = array(pearlRoot, "areas");
        if (areas.size() < 2) return;

        int currentArea = -1;
        for (int i = 0; i < areas.size(); i++) {
            if (areas.get(i) == item.parent) {
                currentArea = i;
                break;
            }
        }
        if (currentArea < 0) return;

        JsonObject nextArea = areas.get((currentArea + 1) % areas.size()).getAsJsonObject();
        JsonArray currentWaypoints = waypoints(item.parent);
        for (int i = 0; i < currentWaypoints.size(); i++) {
            if (currentWaypoints.get(i) == item.object) {
                currentWaypoints.remove(i);
                waypoints(nextArea).add(item.object);
                saveAll();
                return;
            }
        }
    }

    public boolean canRemove(@NotNull Item item) {
        return !isDefaultItem(item);
    }

    public void remove(@NotNull Item item) {
        if (!canRemove(item)) return;
        if (item.source == Source.PEARL_AREA || item.source == Source.ETHER_AREA) {
            JsonArray areas = array(item.source == Source.PEARL_AREA ? pearlRoot : etherRoot, "areas");
            for (int i = 0; i < areas.size(); i++) {
                if (areas.get(i) == item.object) {
                    areas.remove(i);
                    break;
                }
            }
            saveAll();
            return;
        }
        JsonArray parent = item.source == Source.PEARL || item.source == Source.PEARL_STAND_BLOCK ? parentPearlWaypoints(item) : parentEtherWaypoints(item);
        for (int i = 0; i < parent.size(); i++) {
            if (parent.get(i) == item.object) {
                parent.remove(i);
                break;
            }
        }
        saveAll();
    }

    public void resetMainCategory(int main) {
        if (main == 0) {
            pearlRoot = defaultPearlRoot();
        } else {
            if (main == 1) etherRoot = defaultEtherRoot(main);
            else customRoot = defaultEtherRoot(main);
        }
        saveAll();
    }

    public void updateWaypointsFromBundledDefaults() {
        backupExistingConfigFiles();
        pearlRoot = mergePearlDefaultsPreservingCustom();
        etherRoot = mergeEtherDefaultsPreservingCustom(1);
        customRoot = mergeEtherDefaultsPreservingCustom(2);
        dirty = true;
        persistAll();
    }

    public void resetSubcategory(int main, @NotNull String subcategory) {
        if (main == 0) {
            JsonObject defaults = defaultPearlRoot();
            if (subcategory.equals("Areas")) {
                resetPearlAreaBounds(defaults);
            } else {
                replacePearlSubcategory(subcategory, defaults);
            }
        } else {
            JsonObject defaults = defaultEtherRoot(main);
            if (main == 1 && subcategory.equals("Areas")) {
                etherRoot.add("areas", array(defaults, "areas").deepCopy());
            } else {
                replaceEtherSubcategory(main, subcategory, defaults);
            }
        }
        saveAll();
    }

    public void resetItem(@NotNull Item item) {
        if (item.source == Source.PEARL_AREA || item.source == Source.ETHER_AREA) {
            JsonObject replacement = item.source == Source.PEARL_AREA
                    ? findDefaultArea(string(item.object, "name", ""))
                    : findDefaultEtherArea(string(item.object, "name", ""));
            if (replacement != null) copyInto(item.object, replacement);
            saveAll();
            return;
        }
        JsonObject replacement = item.source == Source.PEARL || item.source == Source.PEARL_STAND_BLOCK
                ? findDefaultPearlItem(item)
                : findDefaultEtherItem(item);
        if (replacement != null) {
            copyInto(item.object, replacement);
            saveAll();
        }
    }

    public boolean canReset(@NotNull Item item) {
        if (item.source == Source.PEARL_AREA) return findDefaultArea(string(item.object, "name", "")) != null;
        if (item.source == Source.ETHER_AREA) return findDefaultEtherArea(string(item.object, "name", "")) != null;
        return item.source == Source.PEARL || item.source == Source.PEARL_STAND_BLOCK ? findDefaultPearlItem(item) != null : findDefaultEtherItem(item) != null;
    }

    public void saveChanges() {
        persistAll();
    }

    public boolean hasUnsavedChanges() {
        return dirty;
    }

    public @NotNull String exportWaypointPreset() {
        JsonObject preset = new JsonObject();
        preset.addProperty("version", 1);
        preset.add("pearl", pearlRoot.deepCopy());
        preset.add("etherwarp", etherRoot.deepCopy());
        preset.add("custom", customRoot.deepCopy());
        return WAYPOINT_PRESET_PREFIX + encodePreset(GSON.toJson(preset));
    }

    public void importWaypointPreset(@NotNull String text) {
        String clean = text.trim();
        if (!clean.startsWith(WAYPOINT_PRESET_PREFIX)) {
            throw new IllegalArgumentException("Invalid waypoint preset.");
        }
        JsonObject preset = JsonParser.parseString(decodePreset(clean.substring(WAYPOINT_PRESET_PREFIX.length()))).getAsJsonObject();
        JsonObject pearl = object(preset, "pearl");
        JsonObject etherwarp = object(preset, "etherwarp");
        JsonObject custom = object(preset, "custom");
        array(pearl, "areas");
        array(etherwarp, "areas");
        array(etherwarp, "categories");
        array(custom, "categories");
        pearlRoot = pearl;
        etherRoot = etherwarp;
        customRoot = custom;
        dirty = true;
        persistAll();
    }

    public boolean isCustomPearlWaypoint(@NotNull Item item) {
        return item.source == Source.PEARL && findDefaultPearlItem(item) == null;
    }

    public double pearlSize(@NotNull Item item) {
        return number(item.object, "size", 0.224);
    }

    public void setPearlSize(@NotNull Item item, double value) {
        if (item.source != Source.PEARL) return;
        item.object.addProperty("size", Math.clamp(value, 0.05, 3.0));
        saveAll();
    }

    public double pearlDelay(@NotNull Item item) {
        return number(item.object, "landingOffsetTicks", 0.0);
    }

    public void setPearlDelay(@NotNull Item item, double value) {
        if (item.source != Source.PEARL) return;
        item.object.addProperty("landingOffsetTicks", Math.clamp(value, -100.0, 100.0));
        saveAll();
    }

    public void addCategory(int main) {
        if (main == 0) {
            addSubcategory(0, "Custom Pearls");
            return;
        }
        JsonObject category = new JsonObject();
        category.addProperty("name", uniqueEtherCategoryName(main, main == 1 ? "Custom Etherwarps" : "Custom Waypoints"));
        category.addProperty("enabled", true);
        category.add("waypoints", new JsonArray());
        array(etherRootFor(main), "categories").add(category);
        saveAll();
    }

    public boolean isDefaultMainCategory(int main) {
        return main >= 0 && main < mainCategories().size();
    }

    public boolean isDefaultSubcategory(int main, @NotNull String subcategory) {
        if (main == 0) return List.of("Flat Pearls", "Sky Pearls", "Double Pearls", "Areas", "Stand Blocks").contains(subcategory);
        if (main == 1) return List.of("X", "Equals", "Triangle", "Slash", "Areas").contains(subcategory);
        if (main == 2) return List.of("DPS", "SKIP", "Other Custom").contains(subcategory);
        for (JsonElement element : array(defaultEtherRoot(main), "categories")) {
            if (!element.isJsonObject()) continue;
            if (matchesEtherSubcategory(main, subcategory, string(element.getAsJsonObject(), "name", ""))) return true;
        }
        return false;
    }

    public void removeCustomSubcategory(int main, @NotNull String subcategory) {
        if (isDefaultSubcategory(main, subcategory)) return;
        if (main == 0) {
            JsonArray subs = array(pearlRoot, "customSubcategories");
            for (int i = subs.size() - 1; i >= 0; i--) {
                if (subcategory.equalsIgnoreCase(subs.get(i).getAsString())) subs.remove(i);
            }
            removePearlWaypointSubcategory(subcategory);
            saveAll();
            return;
        }

        JsonArray categories = array(etherRootFor(main), "categories");
        for (int i = categories.size() - 1; i >= 0; i--) {
            JsonElement element = categories.get(i);
            if (element.isJsonObject() && subcategory.equalsIgnoreCase(string(element.getAsJsonObject(), "name", ""))) {
                categories.remove(i);
            }
        }
        saveAll();
    }

    public boolean isDefaultItem(@NotNull Item item) {
        if (item.source == Source.PEARL_AREA) return findDefaultArea(string(item.object, "name", "")) != null;
        if (item.source == Source.ETHER_AREA) return findDefaultEtherArea(string(item.object, "name", "")) != null;
        return item.source == Source.PEARL || item.source == Source.PEARL_STAND_BLOCK ? findDefaultPearlItem(item) != null : findDefaultEtherItem(item) != null;
    }

    public boolean isPearlWaypoint(@NotNull Item item) {
        return item.source == Source.PEARL;
    }

    public boolean isPearlArea(@NotNull Item item) {
        return item.source == Source.PEARL_AREA || item.source == Source.ETHER_AREA;
    }

    public boolean isPearlLike(@NotNull Item item) {
        return item.source == Source.PEARL || item.source == Source.PEARL_STAND_BLOCK || item.source == Source.PEARL_AREA || item.source == Source.ETHER_AREA;
    }

    public boolean isStandBlock(@NotNull Item item) {
        return item.source == Source.PEARL_STAND_BLOCK;
    }

    public void addSubcategory(int main, @NotNull String name) {
        if (main == 0) {
            array(pearlRoot, "customSubcategories").add(uniquePearlSubcategoryName(name));
        } else if (main == 1 && name.equalsIgnoreCase("custom area")) {
            JsonObject area = new JsonObject();
            area.addProperty("name", uniqueEtherAreaName(name));
            JsonArray bounds = new JsonArray();
            bounds.add(-150);
            bounds.add(-150);
            bounds.add(-125);
            bounds.add(-125);
            area.add("bounds", bounds);
            array(etherRoot, "areas").add(area);
        } else {
            JsonObject category = new JsonObject();
            category.addProperty("name", name);
            category.addProperty("enabled", true);
            category.add("waypoints", new JsonArray());
            array(etherRootFor(main), "categories").add(category);
        }
        saveAll();
    }

    public void addArea(int main, @NotNull String name) {
        if (main == 0) {
            addPearlArea(name);
        } else if (main == 1) {
            JsonObject area = new JsonObject();
            area.addProperty("name", uniqueEtherAreaName(name));
            JsonArray bounds = new JsonArray();
            bounds.add(-150);
            bounds.add(-150);
            bounds.add(-125);
            bounds.add(-125);
            area.add("bounds", bounds);
            array(etherRoot, "areas").add(area);
        }
        saveAll();
    }

    public void addWaypoint(int main, @NotNull String subcategory) {
        if (main == 0) {
            JsonObject area = selectedPearlArea(subcategory);
            JsonObject wp = new JsonObject();
            wp.addProperty("label", subcategory.equals("Stand Blocks") ? "" : "New Pearl");
            if (!isDefaultSubcategory(main, subcategory)) wp.addProperty("subcategory", subcategory);
            wp.add("target", vec(-106.0, 79.0, -113.0));
            wp.add("aimTarget", vec(-106.0, 79.0, -113.0));
            if (subcategory.equals("Stand Blocks")) wp.add("standBlock", vec(-106.0, 79.0, -113.0));
            wp.addProperty("trajectoryType", subcategory.contains("Sky") ? "SKY" : subcategory.contains("Double") ? "DOUBLE_HIGH" : "FLAT");
            wp.addProperty("projectionDistance", 13.0);
            wp.addProperty("landingOffsetTicks", 0.0);
            wp.addProperty("size", 0.224);
            wp.addProperty("alert", true);
            wp.addProperty("color", "#00FFFF");
            waypoints(area).add(wp);
        } else {
            JsonObject cat = firstEtherCategory(main, subcategory);
            JsonObject wp = new JsonObject();
            wp.addProperty("name", "New Waypoint");
            wp.add("position", vec(-106.5, 79.0, -113.5));
            wp.addProperty("color", "#00FFFF");
            wp.addProperty("alpha", 0.8);
            wp.addProperty("renderStyle", "OUTLINE");
            wp.addProperty("lineWidth", 2.5);
            wp.addProperty("shape", "FULL");
            wp.addProperty("markerStyle", main == 1 ? "TARGET" : "SOLID");
            JsonArray phases = new JsonArray();
            phases.add(main == 1 ? "SUPPLIES" : "DPS");
            wp.add("showInPhases", phases);
            wp.add("hideInPhases", new JsonArray());
            wp.addProperty("maxRenderDistance", -1);
            waypoints(cat).add(wp);
        }
        saveAll();
    }

    private void collectPearlItems(@NotNull List<Item> out, @NotNull String subcategory) {
        boolean customSubcategory = !isDefaultSubcategory(0, subcategory);
        List<Item> custom = new ArrayList<>();
        List<Item> defaults = new ArrayList<>();
        for (JsonElement areaElement : array(pearlRoot, "areas")) {
            if (!areaElement.isJsonObject()) continue;
            JsonObject area = areaElement.getAsJsonObject();
            String areaName = string(area, "name", "area");
            if (subcategory.equals("Areas")) {
                out.add(new Item(Source.PEARL_AREA, null, area, displayAreaName(areaName), areaInfo(area)));
                continue;
            }
            for (JsonElement waypointElement : waypoints(area)) {
                if (!waypointElement.isJsonObject()) continue;
                JsonObject waypoint = waypointElement.getAsJsonObject();
                if (subcategory.equals("Stand Blocks")) {
                    if (waypoint.has("standBlock")) {
                        out.add(new Item(Source.PEARL_STAND_BLOCK, area, waypoint, titleStandBlock(areaName, waypoint), pearlInfo(areaName, waypoint)));
                    }
                    continue;
                }
                if (isStandBlockOnly(waypoint)) continue;
                String waypointSubcategory = string(waypoint, "subcategory", "");
                if (customSubcategory) {
                    if (!subcategory.equalsIgnoreCase(waypointSubcategory)) continue;
                    out.add(new Item(Source.PEARL, area, waypoint, titlePearl(waypoint), pearlInfo(areaName, waypoint)));
                    continue;
                }
                if (!waypointSubcategory.isBlank()) continue;
                String trajectory = string(waypoint, "trajectoryType", "FLAT");
                if (!matchesPearlSubcategory(subcategory, areaName, trajectory)) continue;
                Item item = new Item(Source.PEARL, area, waypoint, titlePearl(waypoint), pearlInfo(areaName, waypoint));
                if (findDefaultPearlItem(item) == null) custom.add(0, item);
                else defaults.add(item);
            }
        }
        out.addAll(custom);
        out.addAll(defaults);
    }

    private void collectEtherItems(@NotNull List<Item> out, int main, @NotNull String subcategory) {
        if (main == 1 && subcategory.equals("Areas")) {
            for (JsonElement areaElement : array(etherRoot, "areas")) {
                if (!areaElement.isJsonObject()) continue;
                JsonObject area = areaElement.getAsJsonObject();
                String areaName = string(area, "name", "area");
                out.add(new Item(Source.ETHER_AREA, null, area, displayAreaName(areaName), areaInfo(area)));
            }
            return;
        }
        List<Item> custom = new ArrayList<>();
        List<Item> defaults = new ArrayList<>();
        for (JsonElement categoryElement : array(etherRootFor(main), "categories")) {
            if (!categoryElement.isJsonObject()) continue;
            JsonObject category = categoryElement.getAsJsonObject();
            String categoryName = string(category, "name", "Category");
            if (!matchesEtherSubcategory(main, subcategory, categoryName)) continue;
            for (JsonElement waypointElement : waypoints(category)) {
                if (!waypointElement.isJsonObject()) continue;
                JsonObject waypoint = waypointElement.getAsJsonObject();
                Item item = new Item(main == 1 ? Source.NEW_ETHER : Source.ETHER, category, waypoint, string(waypoint, "name", "Waypoint"), etherInfo(categoryName, waypoint));
                if (findDefaultEtherItem(item) == null) custom.add(0, item);
                else defaults.add(item);
            }
        }
        out.addAll(custom);
        out.addAll(defaults);
    }

    private boolean matchesPearlSubcategory(String subcategory, String areaName, String trajectory) {
        if (subcategory.contains("Sky")) return trajectory.equals("SKY");
        if (subcategory.contains("Double")) return trajectory.startsWith("DOUBLE");
        return trajectory.equals("FLAT");
    }

    private boolean matchesEtherSubcategory(int main, String subcategory, String categoryName) {
        if (subcategory.equalsIgnoreCase(categoryName)) return true;
        String name = normalize(categoryName);
        String sub = normalize(subcategory);
        boolean custom = name.contains("dps") || name.contains("skip");
        if (main == 2) {
            if (sub.contains("dps")) return name.contains("dps");
            if (sub.contains("skip")) return name.contains("skip");
            if (sub.contains("othercustom")) return !custom;
            return name.contains(sub);
        }
        if (custom) return false;
        if (sub.contains("other")) return !(name.contains("x") || name.contains("equal") || name.contains("tri") || name.contains("slash"));
        return name.contains(sub) || (sub.contains("triangle") && name.contains("tri"));
    }

    private void saveAll() {
        dirty = true;
    }

    private void persistAll() {
        try {
            writeJson(PearlWaypointConfigLoader.get().getConfigPath(), pearlRoot);
            writeJson(EtherwarpConfigLoader.getNewEtherwarpWaypoints().getConfigPath(), etherRoot);
            writeJson(EtherwarpConfigLoader.get().getConfigPath(), customRoot);
            dirty = false;
        } catch (Exception ignored) {
            return;
        }
        PearlWaypointConfigLoader.get().reload();
        EtherwarpConfigLoader.getNewEtherwarpWaypoints().reload();
        EtherwarpConfigLoader.get().reload();
        IQModClient client = IQModClient.get();
        if (client == null || client.getFeatureManager() == null) return;
        PearlWaypointFeature pearl = client.getFeatureManager().get(PearlWaypointFeature.class);
        if (pearl != null) pearl.reloadConfig();
        EtherwarpWaypointsFeature ether = client.getFeatureManager().get(EtherwarpWaypointsFeature.class);
        if (ether != null) ether.reloadConfig();
        DpsWaypointFeature dps = client.getFeatureManager().get(DpsWaypointFeature.class);
        if (dps != null) dps.reloadConfig();
        SkipWaypointFeature skip = client.getFeatureManager().get(SkipWaypointFeature.class);
        if (skip != null) skip.reloadConfig();
    }

    private void backupExistingConfigFiles() {
        backupConfigFile(PearlWaypointConfigLoader.get().getConfigPath());
        backupConfigFile(EtherwarpConfigLoader.getNewEtherwarpWaypoints().getConfigPath());
        backupConfigFile(EtherwarpConfigLoader.get().getConfigPath());
    }

    private void backupConfigFile(Path path) {
        try {
            if (!Files.exists(path)) return;
            Path backup = path.resolveSibling(path.getFileName() + ".before-updatewaypoints.bak");
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
        }
    }

    private JsonObject readJson(Path path, String arrayName) {
        try {
            if (!Files.exists(path)) {
                if (arrayName.equals("areas")) PearlWaypointConfigLoader.get().save(BuiltInPearlWaypoints.getDefaultAreas());
                else EtherwarpConfigLoader.get().reload();
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (!root.has(arrayName)) root.add(arrayName, new JsonArray());
                return root;
            }
        } catch (Exception e) {
            if (arrayName.equals("areas")) {
                try {
                    PearlWaypointConfigLoader.get().save(BuiltInPearlWaypoints.getDefaultAreas());
                    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                        if (!root.has(arrayName)) root.add(arrayName, new JsonArray());
                        return root;
                    }
                } catch (Exception ignored) {
                }
            }
            JsonObject root = new JsonObject();
            root.add(arrayName, new JsonArray());
            return root;
        }
    }

    private JsonObject defaultPearlRoot() {
        return PearlWaypointConfigLoader.get().defaultRootJson();
    }

    private JsonObject defaultEtherRoot(int main) {
        String resource = main == 1 ? "/default-config/iq/new_etherwarp_waypoints.json" : "/default-config/iq/custom_waypoints.json";
        try (Reader reader = new java.io.InputStreamReader(
                KuudraWaypointEditorStore.class.getResourceAsStream(resource),
                StandardCharsets.UTF_8
        )) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception ignored) {
            JsonObject root = new JsonObject();
            root.add("categories", new JsonArray());
            return root;
        }
    }

    private JsonObject etherRootFor(int main) {
        return main == 1 ? etherRoot : customRoot;
    }

    private JsonObject rootForMain(int main) {
        if (main == 0) return pearlRoot;
        return etherRootFor(main);
    }

    private JsonObject mergePearlDefaultsPreservingCustom() {
        JsonObject merged = defaultPearlRoot();
        copyPropertyIfPresent(pearlRoot, merged, "enabled");
        copyPropertyIfPresent(pearlRoot, merged, "hiddenSubcategories");
        copyPropertyIfPresent(pearlRoot, merged, "customSubcategories");

        for (JsonElement areaElement : array(pearlRoot, "areas")) {
            if (!areaElement.isJsonObject()) continue;
            JsonObject currentArea = areaElement.getAsJsonObject();
            String areaName = string(currentArea, "name", "");
            JsonObject mergedArea = findDefaultArea(merged, areaName);
            if (mergedArea == null) {
                array(merged, "areas").add(currentArea.deepCopy());
                continue;
            }
            copyPropertyIfPresent(currentArea, mergedArea, "enabled");
            preservePearlDefaultWaypointVisibility(currentArea, mergedArea);
            for (JsonElement waypointElement : waypoints(currentArea)) {
                if (!waypointElement.isJsonObject()) continue;
                JsonObject waypoint = waypointElement.getAsJsonObject();
                Source source = isStandBlockOnly(waypoint) ? Source.PEARL_STAND_BLOCK : Source.PEARL;
                Item item = new Item(source, currentArea, waypoint, titlePearl(waypoint), pearlInfo(areaName, waypoint));
                if (isCustomPearlWaypointObject(waypoint) || findDefaultPearlItem(item) == null) {
                    waypoints(mergedArea).add(waypoint.deepCopy());
                }
            }
        }
        return merged;
    }

    private JsonObject mergeEtherDefaultsPreservingCustom(int main) {
        JsonObject currentRoot = etherRootFor(main);
        JsonObject merged = defaultEtherRoot(main);
        copyPropertyIfPresent(currentRoot, merged, "enabled");

        if (main == 1) {
            for (JsonElement areaElement : array(currentRoot, "areas")) {
                if (!areaElement.isJsonObject()) continue;
                JsonObject currentArea = areaElement.getAsJsonObject();
                JsonObject mergedArea = findDefaultArea(merged, string(currentArea, "name", ""));
                if (mergedArea == null) {
                    array(merged, "areas").add(currentArea.deepCopy());
                } else {
                    copyPropertyIfPresent(currentArea, mergedArea, "enabled");
                }
            }
        }

        for (JsonElement categoryElement : array(currentRoot, "categories")) {
            if (!categoryElement.isJsonObject()) continue;
            JsonObject currentCategory = categoryElement.getAsJsonObject();
            String categoryName = string(currentCategory, "name", "");
            JsonObject mergedCategory = findEtherCategory(merged, categoryName);
            if (mergedCategory == null) {
                array(merged, "categories").add(currentCategory.deepCopy());
                continue;
            }
            copyPropertyIfPresent(currentCategory, mergedCategory, "enabled");
            preserveEtherDefaultWaypointVisibility(main, currentCategory, mergedCategory);
            for (JsonElement waypointElement : waypoints(currentCategory)) {
                if (!waypointElement.isJsonObject()) continue;
                JsonObject waypoint = waypointElement.getAsJsonObject();
                Source source = main == 1 ? Source.NEW_ETHER : Source.ETHER;
                Item item = new Item(source, currentCategory, waypoint, string(waypoint, "name", "Waypoint"), etherInfo(categoryName, waypoint));
                if (findDefaultEtherItem(item) == null) {
                    waypoints(mergedCategory).add(waypoint.deepCopy());
                }
            }
        }
        return merged;
    }

    private void preservePearlDefaultWaypointVisibility(JsonObject currentArea, JsonObject mergedArea) {
        for (JsonElement mergedElement : waypoints(mergedArea)) {
            if (!mergedElement.isJsonObject()) continue;
            JsonObject mergedWaypoint = mergedElement.getAsJsonObject();
            JsonObject currentWaypoint = findPearlWaypointByDefaultIdentity(currentArea, mergedWaypoint);
            if (currentWaypoint != null) copyPropertyIfPresent(currentWaypoint, mergedWaypoint, "enabled");
        }
    }

    private JsonObject findPearlWaypointByDefaultIdentity(JsonObject currentArea, JsonObject defaultWaypoint) {
        String title = titlePearl(defaultWaypoint);
        String trajectory = string(defaultWaypoint, "trajectoryType", "FLAT");
        boolean stand = defaultWaypoint.has("standBlock");
        for (JsonElement element : waypoints(currentArea)) {
            if (!element.isJsonObject()) continue;
            JsonObject candidate = element.getAsJsonObject();
            if (isCustomPearlWaypointObject(candidate)) continue;
            if (titlePearl(candidate).equals(title)
                    && string(candidate, "trajectoryType", "FLAT").equals(trajectory)
                    && candidate.has("standBlock") == stand) {
                return candidate;
            }
        }
        return null;
    }

    private void preserveEtherDefaultWaypointVisibility(int main, JsonObject currentCategory, JsonObject mergedCategory) {
        for (JsonElement mergedElement : waypoints(mergedCategory)) {
            if (!mergedElement.isJsonObject()) continue;
            JsonObject mergedWaypoint = mergedElement.getAsJsonObject();
            JsonObject currentWaypoint = findEtherWaypointByName(currentCategory, string(mergedWaypoint, "name", ""));
            if (currentWaypoint != null) copyPropertyIfPresent(currentWaypoint, mergedWaypoint, "enabled");
        }
    }

    private JsonObject findEtherCategory(JsonObject root, String name) {
        for (JsonElement element : array(root, "categories")) {
            if (element.isJsonObject() && string(element.getAsJsonObject(), "name", "").equals(name)) {
                return element.getAsJsonObject();
            }
        }
        return null;
    }

    private JsonObject findEtherWaypointByName(JsonObject category, String name) {
        for (JsonElement element : waypoints(category)) {
            if (element.isJsonObject() && string(element.getAsJsonObject(), "name", "").equals(name)) {
                return element.getAsJsonObject();
            }
        }
        return null;
    }

    private void copyPropertyIfPresent(JsonObject source, JsonObject target, String key) {
        if (source.has(key)) target.add(key, source.get(key).deepCopy());
    }

    private void resetPearlAreaBounds(JsonObject defaults) {
        JsonArray areas = array(pearlRoot, "areas");
        for (JsonElement areaElement : areas) {
            if (!areaElement.isJsonObject()) continue;
            JsonObject current = areaElement.getAsJsonObject();
            JsonObject replacement = findDefaultArea(defaults, string(current, "name", ""));
            if (replacement != null && replacement.has("bounds")) {
                current.add("bounds", replacement.getAsJsonArray("bounds").deepCopy());
            }
        }
    }

    private void replacePearlSubcategory(String subcategory, JsonObject defaults) {
        JsonArray areas = array(pearlRoot, "areas");
        for (JsonElement areaElement : areas) {
            if (!areaElement.isJsonObject()) continue;
            JsonObject area = areaElement.getAsJsonObject();
            JsonObject defaultArea = findDefaultArea(defaults, string(area, "name", ""));
            if (defaultArea == null) continue;
            JsonArray currentWaypoints = waypoints(area);
            removeMatchingPearlWaypoints(currentWaypoints, subcategory, string(area, "name", ""));
            for (JsonElement waypointElement : waypoints(defaultArea)) {
                if (!waypointElement.isJsonObject()) continue;
                JsonObject waypoint = waypointElement.getAsJsonObject();
                if (matchesPearlResetSubcategory(subcategory, string(area, "name", ""), waypoint)) {
                    currentWaypoints.add(waypoint.deepCopy());
                }
            }
        }
    }

    private void removeMatchingPearlWaypoints(JsonArray waypoints, String subcategory, String areaName) {
        for (int i = waypoints.size() - 1; i >= 0; i--) {
            JsonElement element = waypoints.get(i);
            if (element.isJsonObject() && matchesPearlResetSubcategory(subcategory, areaName, element.getAsJsonObject())) {
                waypoints.remove(i);
            }
        }
    }

    private boolean matchesPearlResetSubcategory(String subcategory, String areaName, JsonObject waypoint) {
        if (subcategory.equals("Stand Blocks")) return waypoint.has("standBlock");
        if (isStandBlockOnly(waypoint)) return false;
        return matchesPearlSubcategory(subcategory, areaName, string(waypoint, "trajectoryType", "FLAT"));
    }

    private void replaceEtherSubcategory(int main, String subcategory, JsonObject defaults) {
        JsonArray categories = array(etherRoot, "categories");
        for (int i = categories.size() - 1; i >= 0; i--) {
            JsonElement element = categories.get(i);
            if (element.isJsonObject() && matchesEtherSubcategory(main, subcategory, string(element.getAsJsonObject(), "name", ""))) {
                categories.remove(i);
            }
        }
        for (JsonElement element : array(defaults, "categories")) {
            if (element.isJsonObject() && matchesEtherSubcategory(main, subcategory, string(element.getAsJsonObject(), "name", ""))) {
                categories.add(element.deepCopy());
            }
        }
    }

    private JsonObject findDefaultPearlItem(Item item) {
        JsonObject defaults = defaultPearlRoot();
        String areaName = item.parent == null ? "" : string(item.parent, "name", "");
        JsonObject defaultArea = findDefaultArea(defaults, areaName);
        if (defaultArea == null) return null;
        String title = titlePearl(item.object);
        String trajectory = string(item.object, "trajectoryType", "FLAT");
        boolean stand = item.object.has("standBlock");
        for (JsonElement element : waypoints(defaultArea)) {
            if (!element.isJsonObject()) continue;
            JsonObject candidate = element.getAsJsonObject();
            if (titlePearl(candidate).equals(title)
                    && string(candidate, "trajectoryType", "FLAT").equals(trajectory)
                    && candidate.has("standBlock") == stand) {
                return candidate.deepCopy();
            }
        }
        return null;
    }

    private JsonObject findDefaultEtherItem(Item item) {
        JsonObject defaults = defaultEtherRoot(item.source == Source.NEW_ETHER ? 1 : 2);
        String categoryName = item.parent == null ? "" : string(item.parent, "name", "");
        String title = string(item.object, "name", "");
        for (JsonElement categoryElement : array(defaults, "categories")) {
            if (!categoryElement.isJsonObject()) continue;
            JsonObject category = categoryElement.getAsJsonObject();
            if (!string(category, "name", "").equals(categoryName)) continue;
            for (JsonElement waypointElement : waypoints(category)) {
                if (!waypointElement.isJsonObject()) continue;
                JsonObject waypoint = waypointElement.getAsJsonObject();
                if (string(waypoint, "name", "").equals(title)) return waypoint.deepCopy();
            }
        }
        return null;
    }

    private JsonObject findDefaultArea(String name) {
        return findDefaultArea(defaultPearlRoot(), name);
    }

    private JsonObject findDefaultEtherArea(String name) {
        return findDefaultArea(defaultEtherRoot(1), name);
    }

    private JsonObject findDefaultArea(JsonObject root, String name) {
        for (JsonElement element : array(root, "areas")) {
            if (element.isJsonObject() && string(element.getAsJsonObject(), "name", "").equals(name)) {
                return element.getAsJsonObject().deepCopy();
            }
        }
        return null;
    }

    private void copyInto(JsonObject target, JsonObject source) {
        List<String> keys = new ArrayList<>();
        for (String key : target.keySet()) keys.add(key);
        for (String key : keys) target.remove(key);
        for (String key : source.keySet()) target.add(key, source.get(key).deepCopy());
    }

    private void writeJson(Path path, JsonObject root) throws Exception {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private JsonArray array(JsonObject root, String name) {
        if (!root.has(name) || !root.get(name).isJsonArray()) root.add(name, new JsonArray());
        return root.getAsJsonArray(name);
    }

    private JsonObject object(JsonObject root, String name) {
        if (!root.has(name) || !root.get(name).isJsonObject()) {
            throw new IllegalArgumentException("Missing waypoint preset section: " + name);
        }
        return root.getAsJsonObject(name);
    }

    private JsonArray waypoints(JsonObject parent) {
        if (!parent.has("waypoints") || !parent.get("waypoints").isJsonArray()) parent.add("waypoints", new JsonArray());
        return parent.getAsJsonArray("waypoints");
    }

    private JsonArray parentPearlWaypoints(Item item) {
        if (item.parent == null) return new JsonArray();
        return waypoints(item.parent);
    }

    private JsonArray parentEtherWaypoints(Item item) {
        return waypoints(item.parent);
    }

    private JsonObject firstPearlArea() {
        JsonArray areas = array(pearlRoot, "areas");
        if (areas.isEmpty()) addPearlArea("custom area");
        return areas.get(0).getAsJsonObject();
    }

    private JsonObject selectedPearlArea(@NotNull String subcategory) {
        return firstPearlArea();
    }

    private void addPearlArea(@NotNull String name) {
        JsonObject area = new JsonObject();
        area.addProperty("name", uniquePearlAreaName(name));
        JsonArray bounds = new JsonArray();
        bounds.add(-150);
        bounds.add(-150);
        bounds.add(-125);
        bounds.add(-125);
        area.add("bounds", bounds);
        area.add("waypoints", new JsonArray());
        array(pearlRoot, "areas").add(area);
    }

    private JsonObject firstEtherCategory(int main, String subcategory) {
        for (JsonElement element : array(etherRootFor(main), "categories")) {
            if (element.isJsonObject() && matchesEtherSubcategory(main, subcategory, string(element.getAsJsonObject(), "name", ""))) {
                return element.getAsJsonObject();
            }
        }
        addSubcategory(main, subcategory);
        JsonArray categories = array(etherRootFor(main), "categories");
        return categories.get(categories.size() - 1).getAsJsonObject();
    }

    private void setPearlField(Item item, String field, String value) {
        item.object.addProperty(field, value);
    }

    private String titlePearl(JsonObject obj) {
        String label = string(obj, "label", "");
        return label.isBlank() ? "Stand Block" : label;
    }

    private String titleStandBlock(String area, JsonObject obj) {
        String custom = string(obj, "standBlockLabel", "");
        if (!custom.isBlank()) return custom;
        return displayAreaName(area) + " Block";
    }

    private String displayAreaName(String area) {
        String normalized = normalize(area);
        if (normalized.startsWith("customarea")) {
            String suffix = normalized.substring("customarea".length());
            return suffix.isBlank() ? "Custom Area" : "Custom Area " + suffix;
        }
        return switch (area.toLowerCase(Locale.ROOT)) {
            case "x" -> "X";
            case "x cannon" -> "X Cannon";
            case "square" -> "Square";
            case "slash" -> "Slash";
            case "equals" -> "Equals";
            case "triangle" -> "Triangle";
            case "shop" -> "Shop";
            default -> area;
        };
    }

    private boolean isStandBlockOnly(JsonObject obj) {
        return obj.has("standBlock") && string(obj, "label", "").isBlank();
    }

    private boolean isCustomPearlWaypointObject(JsonObject obj) {
        return !string(obj, "subcategory", "").isBlank();
    }

    private String pearlInfo(String area, JsonObject obj) {
        return "Area " + area + "\nType " + string(obj, "trajectoryType", "FLAT")
                + "\nTarget " + vecString(obj.getAsJsonArray("target"))
                + (obj.has("standBlock") ? "\nStand " + vecString(obj.getAsJsonArray("standBlock")) : "")
                + "\nSize " + number(obj, "size", 0.224);
    }

    private String areaInfo(JsonObject obj) {
        return "Area bounds\n" + boundsString(obj.getAsJsonArray("bounds")) + "\nWaypoints " + waypoints(obj).size();
    }

    private String etherInfo(String category, JsonObject obj) {
        return "Category " + category + "\nPhase " + firstPhase(obj)
                + "\nStyle " + string(obj, "renderStyle", "OUTLINE")
                + "\nMarker " + string(obj, "markerStyle", "SOLID");
    }

    private String firstPhase(JsonObject obj) {
        JsonArray phases = obj.getAsJsonArray("showInPhases");
        return phases != null && !phases.isEmpty() ? phases.get(0).getAsString() : "SUPPLIES";
    }

    private String nextPhase(String current) {
        KuudraPhase phase;
        try {
            phase = KuudraPhase.valueOf(current);
        } catch (Exception e) {
            phase = KuudraPhase.SUPPLIES;
        }
        return switch (phase) {
            case SUPPLIES -> "EATEN";
            case EATEN -> "DPS";
            case DPS -> "SKIP";
            case SKIP -> "SUPPLIES";
            default -> "SUPPLIES";
        };
    }

    private String nextEnum(String current, List<String> values) {
        int index = values.indexOf(current.toUpperCase(Locale.ROOT));
        return values.get((Math.max(0, index) + 1) % values.size());
    }

    private List<String> enumNames(Enum<?>[] values) {
        List<String> out = new ArrayList<>();
        for (Enum<?> value : values) out.add(value.name());
        return out;
    }

    private String uniquePearlAreaName(String base) {
        Set<String> names = new LinkedHashSet<>();
        for (JsonElement element : array(pearlRoot, "areas")) {
            if (element.isJsonObject()) names.add(string(element.getAsJsonObject(), "name", ""));
        }
        String clean = base.replace("Area: ", "").trim();
        if (clean.isBlank()) clean = "custom";
        if (normalize(clean).equals("customarea")) {
            int i = 1;
            String candidate;
            do {
                candidate = "custom area " + i++;
            } while (names.contains(candidate));
            return candidate;
        }
        String candidate = clean;
        int i = 2;
        while (names.contains(candidate)) candidate = clean + " " + i++;
        return candidate;
    }

    private void appendCustomPearlSubcategories(LinkedHashSet<String> out) {
        for (JsonElement element : array(pearlRoot, "customSubcategories")) {
            if (!element.isJsonPrimitive()) continue;
            String name = element.getAsString();
            if (!name.isBlank()) out.add(name);
        }
    }

    private String uniquePearlSubcategoryName(String base) {
        Set<String> names = new LinkedHashSet<>(List.of("Flat Pearls", "Sky Pearls", "Double Pearls", "Areas", "Stand Blocks"));
        for (JsonElement element : array(pearlRoot, "customSubcategories")) {
            if (element.isJsonPrimitive()) names.add(element.getAsString());
        }
        String clean = base.trim();
        if (clean.equalsIgnoreCase("custom area")) clean = "Custom Pearls";
        if (clean.isBlank()) clean = "Custom Pearls";
        String candidate = clean;
        int i = 2;
        while (names.contains(candidate)) candidate = clean + " " + i++;
        return candidate;
    }

    private void renamePearlWaypointSubcategory(String oldName, String newName) {
        for (JsonElement areaElement : array(pearlRoot, "areas")) {
            if (!areaElement.isJsonObject()) continue;
            for (JsonElement waypointElement : waypoints(areaElement.getAsJsonObject())) {
                if (!waypointElement.isJsonObject()) continue;
                JsonObject waypoint = waypointElement.getAsJsonObject();
                if (oldName.equalsIgnoreCase(string(waypoint, "subcategory", ""))) {
                    waypoint.addProperty("subcategory", newName);
                }
            }
        }
    }

    private void removePearlWaypointSubcategory(String subcategory) {
        for (JsonElement areaElement : array(pearlRoot, "areas")) {
            if (!areaElement.isJsonObject()) continue;
            JsonArray waypoints = waypoints(areaElement.getAsJsonObject());
            for (int i = waypoints.size() - 1; i >= 0; i--) {
                JsonElement waypointElement = waypoints.get(i);
                if (waypointElement.isJsonObject() && subcategory.equalsIgnoreCase(string(waypointElement.getAsJsonObject(), "subcategory", ""))) {
                    waypoints.remove(i);
                }
            }
        }
    }

    private void appendCustomEtherCategories(LinkedHashSet<String> out, int main) {
        Set<String> defaults = defaultEtherCategoryNames(main);
        for (JsonElement element : array(etherRootFor(main), "categories")) {
            if (!element.isJsonObject()) continue;
            String name = string(element.getAsJsonObject(), "name", "");
            if (name.isBlank() || defaults.contains(name)) continue;
            boolean customPhase = normalize(name).contains("dps") || normalize(name).contains("skip");
            if (main == 1 && customPhase) continue;
            out.add(name);
        }
    }

    private Set<String> defaultEtherCategoryNames(int main) {
        Set<String> names = new LinkedHashSet<>();
        for (JsonElement element : array(defaultEtherRoot(main), "categories")) {
            if (element.isJsonObject()) names.add(string(element.getAsJsonObject(), "name", ""));
        }
        return names;
    }

    private String uniqueEtherCategoryName(int main, String base) {
        Set<String> names = new LinkedHashSet<>();
        for (JsonElement element : array(etherRootFor(main), "categories")) {
            if (element.isJsonObject()) names.add(string(element.getAsJsonObject(), "name", ""));
        }
        String candidate = base;
        int i = 2;
        while (names.contains(candidate)) candidate = base + " " + i++;
        return candidate;
    }

    private String uniqueEtherAreaName(String base) {
        Set<String> names = new LinkedHashSet<>();
        for (JsonElement element : array(etherRoot, "areas")) {
            if (element.isJsonObject()) names.add(string(element.getAsJsonObject(), "name", ""));
        }
        String clean = base.replace("Area: ", "").trim();
        if (clean.isBlank()) clean = "custom";
        String candidate = clean;
        int i = 2;
        while (names.contains(candidate)) candidate = clean + " " + i++;
        return candidate;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }

    private String string(JsonObject obj, String key, String fallback) {
        return obj.has(key) ? obj.get(key).getAsString() : fallback;
    }

    private double number(JsonObject obj, String key, double fallback) {
        return obj.has(key) ? obj.get(key).getAsDouble() : fallback;
    }

    private JsonArray vec(double x, double y, double z) {
        JsonArray arr = new JsonArray();
        arr.add(x);
        arr.add(y);
        arr.add(z);
        return arr;
    }

    private JsonArray shifted(JsonArray arr, double dx, double dy, double dz) {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        if (arr != null && arr.size() == 3) {
            x = arr.get(0).getAsDouble();
            y = arr.get(1).getAsDouble();
            z = arr.get(2).getAsDouble();
        }
        return vec(x + dx, y + dy, z + dz);
    }

    private JsonArray coordinateArray(Item item) {
        if (item.source == Source.PEARL_AREA) return item.object.getAsJsonArray("bounds");
        if (item.source == Source.ETHER_AREA) return item.object.getAsJsonArray("bounds");
        if (item.source == Source.PEARL_STAND_BLOCK) return item.object.getAsJsonArray("standBlock");
        if (item.source == Source.PEARL) return item.object.getAsJsonArray(pearlCoordinateField(item.object));
        if (item.object.has("position") && item.object.get("position").isJsonArray()) return item.object.getAsJsonArray("position");
        if (item.object.has("positions") && item.object.get("positions").isJsonArray() && !item.object.getAsJsonArray("positions").isEmpty()) {
            JsonElement first = item.object.getAsJsonArray("positions").get(0);
            return first.isJsonArray() ? first.getAsJsonArray() : null;
        }
        return null;
    }

    private String pearlCoordinateField(JsonObject waypoint) {
        String label = string(waypoint, "label", "");
        if (waypoint.has("standBlock") && label.isBlank()) return "standBlock";
        return "target";
    }

    private String vecString(JsonArray arr) {
        if (arr == null || arr.size() != 3) return "-";
        return String.format(Locale.ROOT, "%.1f %.1f %.1f", arr.get(0).getAsDouble(), arr.get(1).getAsDouble(), arr.get(2).getAsDouble());
    }

    private String boundsString(JsonArray arr) {
        if (arr == null || arr.size() != 4) return "-";
        return String.format(Locale.ROOT, "X %.1f..%.1f  Z %.1f..%.1f", arr.get(0).getAsDouble(), arr.get(2).getAsDouble(), arr.get(1).getAsDouble(), arr.get(3).getAsDouble());
    }

    private boolean bool(JsonObject obj, String key, boolean fallback) {
        return obj.has(key) ? obj.get(key).getAsBoolean() : fallback;
    }

    private String encodePreset(String json) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
                gzip.write(json.getBytes(StandardCharsets.UTF_8));
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Could not export waypoint preset.", e);
        }
    }

    private String decodePreset(String encoded) {
        try {
            byte[] compressed = Base64.getUrlDecoder().decode(encoded);
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid waypoint preset.", e);
        }
    }

    private boolean anyEnabled(JsonArray array) {
        if (array.isEmpty()) return true;
        for (JsonElement element : array) {
            if (element.isJsonObject() && bool(element.getAsJsonObject(), "enabled", true)) return true;
        }
        return false;
    }

    private boolean containsString(JsonArray array, String value) {
        for (JsonElement element : array) {
            if (element.isJsonPrimitive() && value.equalsIgnoreCase(element.getAsString())) return true;
        }
        return false;
    }

    private void toggleString(JsonArray array, String value) {
        for (int i = array.size() - 1; i >= 0; i--) {
            JsonElement element = array.get(i);
            if (element.isJsonPrimitive() && value.equalsIgnoreCase(element.getAsString())) {
                array.remove(i);
                return;
            }
        }
        array.add(value);
    }

    private enum Source {
        PEARL,
        ETHER,
        NEW_ETHER,
        PEARL_AREA,
        ETHER_AREA,
        PEARL_STAND_BLOCK
    }

    public static final class Item {
        private final Source source;
        private final JsonObject parent;
        private final JsonObject object;
        public final String title;
        public final String info;

        private Item(Source source, JsonObject parent, JsonObject object, String title, String info) {
            this.source = source;
            this.parent = parent;
            this.object = object;
            this.title = title;
            this.info = info;
        }

        public String title() {
            return title;
        }

        public String info() {
            return info;
        }
    }
}
