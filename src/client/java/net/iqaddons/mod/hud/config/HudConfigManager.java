package net.iqaddons.mod.hud.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import net.iqaddons.mod.hud.element.HudWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages persistence of HUD element configurations.
 * Handles loading, saving, and caching of HUD positions and scales.
 *
 * <p>Configurations are stored in JSON format under:
 * {@code .minecraft/config/iq/hud_config.json}
 */
@Slf4j
public class HudConfigManager {

    private static final String CONFIG_DIR = "iq";
    private static final String CONFIG_FILE = "hud_config.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Type CONFIG_MAP_TYPE = new TypeToken<Map<String, HudElementConfig>>() {}.getType();

    private final Map<String, HudElementConfig> configCache = new ConcurrentHashMap<>();
    private final Path configPath;

    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Config-Saver");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean dirty = false;

    public HudConfigManager() {
        this.configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(CONFIG_DIR)
                .resolve(CONFIG_FILE);
    }

    public void load() {
        try {
            if (!Files.exists(configPath)) {
                log.info("HUD config not found, will create on first save");
                return;
            }

            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                Map<String, HudElementConfig> loaded = GSON.fromJson(reader, CONFIG_MAP_TYPE);
                if (loaded != null) {
                    configCache.putAll(loaded);
                    log.info("Loaded {} HUD configurations", loaded.size());
                }
            }
        } catch (Exception e) {
            log.error("Failed to load HUD config, using defaults", e);
        }
    }

    public void saveAsync() {
        dirty = true;
        saveExecutor.execute(this::saveInternal);
    }

    public void saveSync() {
        saveInternal();
    }

    private void saveInternal() {
        if (!dirty && Files.exists(configPath)) {
            return;
        }

        try {
            Path parentDir = configPath.getParent();
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            String json = GSON.toJson(configCache, CONFIG_MAP_TYPE);
            Files.writeString(configPath, json, StandardCharsets.UTF_8);

            dirty = false;
            log.debug("Saved {} HUD configurations", configCache.size());
        } catch (Exception e) {
            log.error("Failed to save HUD config", e);
        }
    }

    public void shutdown() {
        saveSync();
        saveExecutor.shutdown();
    }

    public @Nullable HudElementConfig getConfig(@NotNull String elementId) {
        return configCache.get(elementId);
    }

    public void setConfig(@NotNull HudElementConfig config) {
        configCache.put(config.id(), config.validated());
        saveAsync();
    }

    public void updatePosition(@NotNull String elementId, float x, float y) {
        HudElementConfig existing = configCache.get(elementId);
        if (existing != null) {
            setConfig(existing.withPosition(x, y));
        } else {
            setConfig(HudElementConfig.defaultConfig(elementId, x, y));
        }
    }

    public void updateScale(@NotNull String elementId, float scale) {
        HudElementConfig existing = configCache.get(elementId);
        if (existing != null) {
            setConfig(existing.withScale(scale));
        }
    }

    public void loadIntoWidget(@NotNull HudWidget widget) {
        HudElementConfig config = configCache.get(widget.getId());
        if (config != null) {
            config.applyTo(widget);
            log.debug("Loaded config for widget: {}", widget.getId());
        } else {
            log.debug("No saved config for widget: {}, using defaults", widget.getId());
        }
    }

    public void saveFromWidget(@NotNull HudWidget widget) {
        HudElementConfig config = HudElementConfig.fromWidget(widget);
        setConfig(config);
    }

    public boolean hasConfig(@NotNull String elementId) {
        return configCache.containsKey(elementId);
    }

    public void removeConfig(@NotNull String elementId) {
        if (configCache.remove(elementId) != null) {
            saveAsync();
            log.debug("Removed config for element: {}", elementId);
        }
    }

    public void resetAll() {
        configCache.clear();
        saveAsync();
        log.info("Reset all HUD configurations");
    }
}