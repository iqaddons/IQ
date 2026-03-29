package net.iqaddons.mod.config.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import net.iqaddons.mod.model.spot.PileLocation;
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
import java.util.List;
import java.util.Optional;

@Slf4j
public class PileConfigLoader {

    private static final PileConfigLoader INSTANCE = new PileConfigLoader();

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("iq");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("pile_locations.json");
    private static final String DEFAULT_RESOURCE = "/default-config/iq/pile_locations.json";

    private volatile List<PileLocation> cachedPiles = Collections.emptyList();

    public @NotNull @UnmodifiableView List<PileLocation> load() {
        Path configPath = getConfigPath();

        try {
            if (Files.exists(configPath)) {
                log.info("Loading pile locations from: {}", configPath);
                try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                    cachedPiles = parseJson(reader);
                    return cachedPiles;
                }
            }

            log.info("Loading pile locations from bundled resource");
            try (InputStream is = getClass().getResourceAsStream(DEFAULT_RESOURCE)) {
                if (is == null) {
                    log.error("Default pile resource not found: {}", DEFAULT_RESOURCE);
                    cachedPiles = Collections.emptyList();
                    return cachedPiles;
                }

                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    cachedPiles = parseJson(reader);
                    saveDefaultConfig(configPath);
                    return cachedPiles;
                }
            }
        } catch (Exception e) {
            log.error("Failed to load pile locations", e);
            cachedPiles = Collections.emptyList();
            return cachedPiles;
        }
    }

    public @NotNull @UnmodifiableView List<PileLocation> reload() {
        log.info("Reloading pile locations config");
        return load();
    }

    public @NotNull @UnmodifiableView List<PileLocation> getCached() {
        return cachedPiles;
    }

    private @NotNull @UnmodifiableView List<PileLocation> parseJson(@NotNull Reader reader) {
        try {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray pilesArray = root.getAsJsonArray("piles");
            if (pilesArray == null) {
                log.warn("No 'piles' array found in pile config");
                return Collections.emptyList();
            }

            List<PileLocation> piles = new ArrayList<>(pilesArray.size());
            for (JsonElement element : pilesArray) {
                if (element.isJsonObject()) {
                    parsePile(element.getAsJsonObject()).ifPresent(piles::add);
                }
            }

            log.info("Loaded {} pile locations", piles.size());
            return Collections.unmodifiableList(piles);
        } catch (Exception e) {
            log.error("Failed to parse pile locations JSON", e);
            return Collections.emptyList();
        }
    }

    private Optional<PileLocation> parsePile(@NotNull JsonObject obj) {
        try {
            String name = obj.get("name").getAsString();
            JsonArray positionArray = obj.has("position")
                    ? obj.getAsJsonArray("position")
                    : obj.getAsJsonArray("coords");

            if (positionArray == null || positionArray.size() != 3) {
                log.warn("Skipping pile '{}' because it does not have a valid [x, y, z] position", name);
                return Optional.empty();
            }

            Vec3d position = new Vec3d(
                    positionArray.get(0).getAsDouble(),
                    positionArray.get(1).getAsDouble(),
                    positionArray.get(2).getAsDouble()
            );

            int noPreValue = obj.has("noPreValue") ? obj.get("noPreValue").getAsInt() : 0;
            return Optional.of(new PileLocation(name, position, noPreValue));
        } catch (Exception e) {
            log.warn("Failed to parse pile location", e);
            return Optional.empty();
        }
    }

    private @NotNull Path getConfigPath() {
        return FabricLoader.getInstance()
                .getGameDir()
                .resolve(CONFIG_DIR)
                .resolve(CONFIG_FILE);
    }

    private void saveDefaultConfig(@NotNull Path configPath) {
        try {
            Files.createDirectories(configPath.getParent());

            try (InputStream is = getClass().getResourceAsStream(DEFAULT_RESOURCE)) {
                if (is == null) {
                    log.error("Default resource missing: {}", DEFAULT_RESOURCE);
                    return;
                }

                Files.copy(is, configPath);
                log.info("Created default pile config at: {}", configPath);
            }
        } catch (Exception e) {
            log.warn("Failed to save default pile config", e);
        }
    }

    public static PileConfigLoader get() {
        return INSTANCE;
    }
}

