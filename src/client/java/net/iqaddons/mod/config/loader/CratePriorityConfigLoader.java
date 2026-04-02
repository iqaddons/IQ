package net.iqaddons.mod.config.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import net.iqaddons.mod.model.spot.PreSpot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
public final class CratePriorityConfigLoader {

    private static final CratePriorityConfigLoader INSTANCE = new CratePriorityConfigLoader();

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("iq");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("crate_priority.json");
    private static final String DEFAULT_RESOURCE = "/default-config/iq/crate_priority.json";

    private volatile Map<PreSpot, Map<Integer, String>> cachedOverrides = Map.of();

    public synchronized void load() {
        Path configPath = getConfigPath();

        try {
            if (Files.exists(configPath)) {
                log.info("Loading crate priority config from: {}", configPath);
                try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                    cachedOverrides = parse(reader);
                    return;
                }
            }

            log.info("Loading crate priority defaults from bundled resource");
            try (InputStream is = getClass().getResourceAsStream(DEFAULT_RESOURCE)) {
                if (is == null) {
                    log.error("Default crate priority resource not found: {}", DEFAULT_RESOURCE);
                    cachedOverrides = Map.of();
                    return;
                }

                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    cachedOverrides = parse(reader);
                    saveDefaultConfig(configPath);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load crate priority config", e);
            cachedOverrides = Map.of();
        }
    }

    public synchronized void reload() {
        load();
    }

    public @Nullable String getDestinationOverride(@NotNull PreSpot preSpot, int missingPre) {
        if (cachedOverrides.isEmpty()) {
            load();
        }

        Map<Integer, String> byMissing = cachedOverrides.get(preSpot);
        if (byMissing == null) {
            return null;
        }

        return byMissing.get(missingPre);
    }

    private @NotNull Map<PreSpot, Map<Integer, String>> parse(@NotNull Reader reader) {
        try {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject priorities = root.getAsJsonObject("priorities");
            if (priorities == null) {
                log.warn("No 'priorities' object found in crate priority config");
                return Map.of();
            }

            Map<PreSpot, Map<Integer, String>> result = new HashMap<>();

            for (Map.Entry<String, JsonElement> preEntry : priorities.entrySet()) {
                PreSpot preSpot = parsePreSpot(preEntry.getKey());
                if (preSpot == null || !preEntry.getValue().isJsonObject()) {
                    continue;
                }

                JsonObject missingMap = preEntry.getValue().getAsJsonObject();
                Map<Integer, String> resolvedMissing = new HashMap<>();

                for (Map.Entry<String, JsonElement> missingEntry : missingMap.entrySet()) {
                    int missingPreValue = parseMissingPre(missingEntry.getKey());
                    if (missingPreValue <= 0 || !missingEntry.getValue().isJsonPrimitive()) {
                        continue;
                    }

                    String destination = missingEntry.getValue().getAsString().trim();
                    if (destination.isEmpty()) {
                        continue;
                    }

                    resolvedMissing.put(missingPreValue, destination);
                }

                if (!resolvedMissing.isEmpty()) {
                    result.put(preSpot, Collections.unmodifiableMap(resolvedMissing));
                }
            }

            log.info("Loaded crate priority overrides for {} pre spots", result.size());
            return Collections.unmodifiableMap(result);
        } catch (Exception e) {
            log.error("Failed to parse crate priority config", e);
            return Map.of();
        }
    }

    private @Nullable PreSpot parsePreSpot(@NotNull String key) {
        return switch (normalizeToken(key)) {
            case "TRIANGLE", "TRI" -> PreSpot.TRIANGLE;
            case "X" -> PreSpot.X;
            case "EQUALS", "EQ" -> PreSpot.EQUALS;
            case "SLASH" -> PreSpot.SLASH;
            default -> null;
        };
    }

    private int parseMissingPre(@NotNull String key) {
        String normalized = normalizeToken(key);

        return switch (normalized) {
            case "X" -> 1;
            case "X_CANNON", "XC", "XCANNON" -> 2;
            case "SQUARE" -> 3;
            case "SLASH" -> 4;
            case "EQUALS", "EQ" -> 5;
            case "TRIANGLE", "TRI" -> 6;
            case "SHOP" -> 7;
            default -> 0;
        };
    }

    private @NotNull String normalizeToken(@NotNull String token) {
        return token
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private @NotNull Path getConfigPath() {
        return CONFIG_FILE;
    }

    private void saveDefaultConfig(@NotNull Path configPath) {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (Files.exists(configPath)) {
                return;
            }

            try (InputStream is = getClass().getResourceAsStream(DEFAULT_RESOURCE)) {
                if (is == null) {
                    log.error("Default crate priority resource missing: {}", DEFAULT_RESOURCE);
                    return;
                }

                Files.copy(is, configPath);
                log.info("Created default crate priority config at: {}", configPath);
            }
        } catch (Exception e) {
            log.warn("Failed to save default crate priority config", e);
        }
    }

    public static CratePriorityConfigLoader get() {
        return INSTANCE;
    }
}

