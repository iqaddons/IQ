package net.iqaddons.mod.hud.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.screen.nano.IqNanoGlobalConfigScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Map.entry;

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
    private static final String MODERN_CONFIG_FILE = "hud_config_modern.json";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Type CONFIG_MAP_TYPE = new TypeToken<Map<String, HudElementConfig>>() {}.getType();

    private static final Map<String, HudElementConfig> MODERN_DEFAULT_CONFIGS = Map.ofEntries(
            entry("chestCounterWidget", modernDefault("chestCounterWidget", 595.5f, 518.3524f, 1.4000008f, "TOP_LEFT")),
            entry("kuudra_direction", modernDefault("kuudra_direction", 420.5f, 132.0099f, 4.7999983f, "TOP_LEFT")),
            entry("simpleBuildProgress", modernDefault("simpleBuildProgress", 435.0f, 327.3326f, 2.2f, "TOP_LEFT")),
            entry("kuudraHealth", modernDefault("kuudraHealth", 450.5f, 20.094406f, 1.1f, "TOP_LEFT")),
            entry("customSplits", modernDefault("customSplits", 5.0f, 4.851834f, 1.2f, "TOP_LEFT")),
            entry("chestValueWidget", modernDefault("chestValueWidget", 146.0f, 165.46536f, 1.2f, "TOP_LEFT")),
            entry("eatenTimer", modernDefault("eatenTimer", 439.5f, 209.30695f, 1.9000002f, "TOP_LEFT")),
            entry("supplyTimerCountdownWidget", modernDefault("supplyTimerCountdownWidget", 414.0f, 365.9712f, 1.6000001f, "TOP_LEFT")),
            entry("supplyProgress", modernDefault("supplyProgress", 306.5f, 233.1178f, 3.0f, "TOP_LEFT")),
            entry("kuudraProfitTrackerWidget", modernDefault("kuudraProfitTrackerWidget", -434.5f, 271.59003f, 1.1f, "TOP_CENTER")),
            entry("backbone_alert", modernDefault("backbone_alert", 307.0f, 285.47922f, 3.0f, "TOP_LEFT")),
            entry("crate_priority", modernDefault("crate_priority", 381.5f, 98.82178f, 3.7999992f, "TOP_LEFT")),
            entry("fireVeilOverlay", modernDefault("fireVeilOverlay", 438.0f, 384.11987f, 1.5000001f, "TOP_LEFT")),
            entry("kuudra_notifications", modernDefault("kuudra_notifications", 356.0f, 62.594406f, 4.2999988f, "TOP_LEFT")),
            entry("buildProgress", modernDefault("buildProgress", 27.5f, 194.29778f, 1.0f, "TOP_LEFT")),
            entry("arrowTrackerWidget", modernDefault("arrowTrackerWidget", 278.5f, 519.95636f, 1.1f, "TOP_LEFT")),
            entry("supplyTimer", modernDefault("supplyTimer", 5.0f, 135.0099f, 1.2f, "TOP_LEFT")),
            entry("freshers_timer", modernDefault("freshers_timer", 5.0f, 228.41483f, 1.2f, "TOP_LEFT"))
    );

    private static @NotNull HudElementConfig modernDefault(
            @NotNull String id,
            float x,
            float y,
            float scale,
            @NotNull String anchor
    ) {
        return new HudElementConfig(id, x, y, scale, anchor).validated();
    }

    private final Map<String, HudElementConfig> configCache = new ConcurrentHashMap<>();
    private final Map<String, HudElementConfig> modernConfigCache = new ConcurrentHashMap<>();
    private final Path configPath;
    private final Path modernConfigPath;

    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Config-Saver");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean dirty = false;

    public HudConfigManager() {
        Path dir = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(CONFIG_DIR);
        this.configPath = dir.resolve(CONFIG_FILE);
        this.modernConfigPath = dir.resolve(MODERN_CONFIG_FILE);
    }

    public void load() {
        loadFile(configPath, configCache, "HUD");
        loadFile(modernConfigPath, modernConfigCache, "modern HUD");
    }

    private void loadFile(
            @NotNull Path path,
            @NotNull Map<String, HudElementConfig> target,
            @NotNull String label
    ) {
        try {
            if (!Files.exists(path)) {
                log.info("{} config not found, will create on first save", label);
                return;
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Map<String, HudElementConfig> loaded = GSON.fromJson(reader, CONFIG_MAP_TYPE);
                if (loaded != null) {
                    target.putAll(loaded);
                    log.info("Loaded {} {} configurations", loaded.size(), label);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load {} config, using defaults", label, e);
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
        if (!dirty && Files.exists(configPath) && Files.exists(modernConfigPath)) {
            return;
        }

        writeFile(configPath, configCache, "HUD");
        writeFile(modernConfigPath, modernConfigCache, "modern HUD");
        dirty = false;
    }

    private void writeFile(
            @NotNull Path path,
            @NotNull Map<String, HudElementConfig> source,
            @NotNull String label
    ) {
        try {
            Path parentDir = path.getParent();
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            String json = GSON.toJson(source, CONFIG_MAP_TYPE);
            Files.writeString(path, json, StandardCharsets.UTF_8);

            log.debug("Saved {} {} configurations", source.size(), label);
        } catch (Exception e) {
            log.error("Failed to save {} config", label, e);
        }
    }

    public void shutdown() {
        saveSync();
        saveExecutor.shutdown();
    }

    public @Nullable HudElementConfig getConfig(@NotNull String elementId) {
        return currentCache().get(elementId);
    }

    public void setConfig(@NotNull HudElementConfig config) {
        putConfig(config);
        saveAsync();
    }

    private void putConfig(@NotNull HudElementConfig config) {
        currentCache().put(config.id(), config.validated());
        dirty = true;
    }

    public void updatePosition(@NotNull String elementId, float x, float y) {
        HudElementConfig existing = currentCache().get(elementId);
        if (existing != null) {
            setConfig(existing.withPosition(x, y));
        } else {
            setConfig(HudElementConfig.defaultConfig(elementId, x, y));
        }
    }

    public void updateScale(@NotNull String elementId, float scale) {
        HudElementConfig existing = currentCache().get(elementId);
        if (existing != null) {
            setConfig(existing.withScale(scale));
        }
    }

    public void loadIntoWidget(@NotNull HudWidget widget) {
        HudElementConfig config = currentCache().get(widget.getId());
        if (config == null && IqNanoGlobalConfigScreen.isSharedModernHudStyle()) {
            config = MODERN_DEFAULT_CONFIGS.get(widget.getId());
        }
        if (config != null) {
            config.applyTo(widget);
            widget.refreshDimensions();
            log.debug("Loaded config for widget: {}", widget.getId());
        } else {
            widget.resetToDefaults();
            log.debug("No saved config for widget: {}, using defaults", widget.getId());
        }
    }

    public void saveFromWidget(@NotNull HudWidget widget) {
        HudElementConfig config = HudElementConfig.fromWidget(widget);
        setConfig(config);
    }

    public void saveFromWidgetsSync(@NotNull Collection<HudWidget> widgets) {
        for (HudWidget widget : widgets) {
            putConfig(HudElementConfig.fromWidget(widget));
        }
        saveSync();
    }

    public boolean hasConfig(@NotNull String elementId) {
        return currentCache().containsKey(elementId);
    }

    public void removeConfig(@NotNull String elementId) {
        if (currentCache().remove(elementId) != null) {
            saveAsync();
            log.debug("Removed config for element: {}", elementId);
        }
    }

    public void resetAll() {
        Map<String, HudElementConfig> cache = currentCache();
        cache.clear();
        if (IqNanoGlobalConfigScreen.isSharedModernHudStyle()) {
            cache.putAll(MODERN_DEFAULT_CONFIGS);
        }
        saveAsync();
        log.info("Reset all HUD configurations");
    }

    private @NotNull Map<String, HudElementConfig> currentCache() {
        return IqNanoGlobalConfigScreen.isSharedModernHudStyle() ? modernConfigCache : configCache;
    }
}
