package net.iqaddons.mod;

import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.iqaddons.mod.commands.IQCommand;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.config.loader.CratePriorityConfigLoader;
import net.iqaddons.mod.events.dispatcher.KuudraEventsDispatcher;
import net.iqaddons.mod.features.FeatureManager;
import net.iqaddons.mod.integration.DiscordRPCIntegration;
import net.iqaddons.mod.lifecycle.LifecycleComponent;
import net.iqaddons.mod.lifecycle.modules.FeatureModule;
import net.iqaddons.mod.lifecycle.modules.KuudraModule;
import net.iqaddons.mod.lifecycle.modules.WidgetModule;
import net.iqaddons.mod.utils.update.ModrinthUpdateChecker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.fluid.Fluids;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
public class IQModClient implements ClientModInitializer {

    private static final String MOD_ID = "iqmod";
    private static final Pattern LEGACY_HIDE_USELESS_ARMOR_STANDS_ARRAY_PATTERN = Pattern.compile(
            "(?ms)^(\\s*)\"hideUselessArmorStands\"\\s*:\\s*\\[(.*?)\\]\\s*,\\s*$"
    );
    private static final String SPLIT_COLOR_MIGRATION_MARKER_RELATIVE_PATH = "iq/migrations/splits-dark-aqua-v1.marker";
    private static IQModClient instance;

    public static MinecraftClient mc = MinecraftClient.getInstance();

    private Configurator configurator;
    private @Nullable FeatureManager featureManager;

    private final List<LifecycleComponent> components = new ArrayList<>();
    private final Map<Field, Object> mainConfigDefaults = new LinkedHashMap<>();

    @Override
    public void onInitializeClient() {
        instance = this;

        migrateLegacyHideUselessArmorStandsConfig();
        migrateSplitColorsToDarkAquaOnFirstLaunch();

        // Snapshot class-declared defaults before any config file is loaded/applied.
        captureMainConfigDefaults();

        configurator = new Configurator(MOD_ID);
        configurator.register(Configuration.class);
        CratePriorityConfigLoader.get().load();

        FeatureModule featureModule = new FeatureModule();
        initializeModules(
                new KuudraModule(), new KuudraEventsDispatcher(),
                featureModule, new WidgetModule()
        );
        this.featureManager = featureModule.getFeatures();

        IQKeyBindings.register();
        registerCommands();
        ModrinthUpdateChecker.INSTANCE.register();

        BlockRenderLayerMap.putFluids(BlockRenderLayer.TRANSLUCENT, Fluids.LAVA, Fluids.FLOWING_LAVA);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            components.forEach(LifecycleComponent::stop);

            DiscordRPCIntegration.INSTANCE.shutdown();
            ModrinthUpdateChecker.INSTANCE.shutdown();
        });

        log.info("IQ Mod has been initialized!");
    }

    private void initializeModules(LifecycleComponent @NotNull ... components) {
        for (LifecycleComponent component : components) {
            this.components.add(component);
            component.start();
        }
    }

    private void migrateLegacyHideUselessArmorStandsConfig() {
        try {
            Path configFile = FabricLoader.getInstance().getConfigDir().resolve("iqaddons.jsonc");
            if (!Files.exists(configFile)) {
                return;
            }

            String content = Files.readString(configFile, StandardCharsets.UTF_8);
            Matcher matcher = LEGACY_HIDE_USELESS_ARMOR_STANDS_ARRAY_PATTERN.matcher(content);
            if (!matcher.find()) {
                return;
            }

            String indent = matcher.group(1);
            String values = matcher.group(2);

            boolean build = values.contains("\"BUILD\"");
            boolean rightCannon = values.contains("\"RIGHT_CANNON\"");
            boolean leftCannon = values.contains("\"LEFT_CANNON\"");
            boolean shop = values.contains("\"SHOP\"");
            boolean others = values.contains("\"OTHERS\"");
            boolean enabled = build || rightCannon || leftCannon || shop || others;

            String replacement = indent + "\"hideUselessArmorStands\": " + enabled + ",\n"
                    + indent + "\"hideUselessArmorStandsConfig\": {\n"
                    + indent + "    \"build\": " + build + ",\n"
                    + indent + "    \"rightCannon\": " + rightCannon + ",\n"
                    + indent + "    \"leftCannon\": " + leftCannon + ",\n"
                    + indent + "    \"shop\": " + shop + ",\n"
                    + indent + "    \"others\": " + others + "\n"
                    + indent + "},";

            String migrated = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
            Files.writeString(configFile, migrated, StandardCharsets.UTF_8);
            log.info("Migrated legacy hideUselessArmorStands array config in iqaddons.jsonc");
        } catch (Exception e) {
            log.warn("Failed to migrate legacy hideUselessArmorStands config", e);
        }
    }

    private void migrateSplitColorsToDarkAquaOnFirstLaunch() {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            Path markerFile = configDir.resolve(SPLIT_COLOR_MIGRATION_MARKER_RELATIVE_PATH);
            if (Files.exists(markerFile)) {
                return;
            }

            Path configFile = configDir.resolve("iqaddons.jsonc");
            if (Files.exists(configFile)) {
                String content = Files.readString(configFile, StandardCharsets.UTF_8);
                String migrated = replaceSplitColorConfig(content);
                if (migrated != null) {
                    Files.writeString(configFile, migrated, StandardCharsets.UTF_8);
                    log.info("Migrated Custom Splits colors to DARK_AQUA in iqaddons.jsonc");
                }
            }

            Files.createDirectories(markerFile.getParent());
            Files.writeString(markerFile, "applied", StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to migrate Custom Splits colors to DARK_AQUA", e);
        }
    }

    private @Nullable String replaceSplitColorConfig(@NotNull String content) {
        int keyIndex = content.indexOf("\"splitColorConfig\"");
        if (keyIndex < 0) {
            return null;
        }

        int lineStart = content.lastIndexOf('\n', keyIndex);
        lineStart = lineStart < 0 ? 0 : lineStart + 1;

        int objectStart = content.indexOf('{', keyIndex);
        if (objectStart < 0) {
            return null;
        }

        int objectEnd = findObjectEnd(content, objectStart);
        if (objectEnd < 0) {
            return null;
        }

        String indent = content.substring(lineStart, keyIndex).replaceAll("[^\\s]", "");
        String replacement = indent + "\"splitColorConfig\": {\n"
                + indent + "    \"supplies\": \"DARK_AQUA\",\n"
                + indent + "    \"build\": \"DARK_AQUA\",\n"
                + indent + "    \"eaten\": \"DARK_AQUA\",\n"
                + indent + "    \"stun\": \"DARK_AQUA\",\n"
                + indent + "    \"dps\": \"DARK_AQUA\",\n"
                + indent + "    \"skip\": \"DARK_AQUA\",\n"
                + indent + "    \"boss\": \"DARK_AQUA\",\n"
                + indent + "    \"overall\": \"DARK_AQUA\",\n"
                + indent + "    \"pace\": \"DARK_AQUA\"\n"
                + indent + "}";

        return content.substring(0, lineStart) + replacement + content.substring(objectEnd + 1);
    }

    private int findObjectEnd(@NotNull String content, int objectStart) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = objectStart; i < content.length(); i++) {
            char c = content.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
                continue;
            }

            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Resets the main ResourcefulConfig file to class defaults and applies it immediately.
     *
     * <p>This deletes both legacy and current file names, recreates the configurator,
     * re-registers {@link Configuration}, and saves right away so the file exists instantly.
     */
    public synchronized void resetMainConfigToDefaults() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path jsonc = configDir.resolve("iqaddons.jsonc");
        Path json = configDir.resolve("iqaddons.json");

        restoreMainConfigDefaultsInMemory();

        try {
            Files.deleteIfExists(jsonc);
            Files.deleteIfExists(json);
        } catch (Exception e) {
            log.warn("Failed to delete existing IQ main config file before reset", e);
        }

        Configurator next = new Configurator(MOD_ID);
        next.register(Configuration.class);
        try {
            next.saveConfig(Configuration.class);
        } catch (Exception e) {
            log.warn("Failed to persist regenerated default IQ config file", e);
        }
        this.configurator = next;
    }

    private synchronized void captureMainConfigDefaults() {
        if (!mainConfigDefaults.isEmpty()) return;
        captureConfigClassDefaults(Configuration.class);

        Config configAnn = Configuration.class.getAnnotation(Config.class);
        if (configAnn != null) {
            for (Class<?> category : configAnn.categories()) {
                captureConfigClassDefaults(category);
            }
        }
    }

    private void captureConfigClassDefaults(@NotNull Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(null);
                mainConfigDefaults.put(field, cloneConfigValue(value));
            } catch (Exception e) {
                log.warn("Failed to snapshot default for {}.{}", type.getSimpleName(), field.getName(), e);
            }
        }

        for (Class<?> nested : type.getDeclaredClasses()) {
            captureConfigClassDefaults(nested);
        }
    }

    private synchronized void restoreMainConfigDefaultsInMemory() {
        if (mainConfigDefaults.isEmpty()) {
            captureMainConfigDefaults();
        }
        for (Map.Entry<Field, Object> entry : mainConfigDefaults.entrySet()) {
            try {
                entry.getKey().set(null, cloneConfigValue(entry.getValue()));
            } catch (Exception e) {
                log.warn("Failed to restore default for {}", entry.getKey().getName(), e);
            }
        }
    }

    private @Nullable Object cloneConfigValue(@Nullable Object value) {
        if (value == null) return null;
        if (value instanceof List<?> list) return new ArrayList<>(list);
        if (value instanceof Map<?, ?> map) return new HashMap<>(map);
        if (value instanceof java.util.Set<?> set) return new HashSet<>(set);
        if (value.getClass().isArray()) {
            if (value instanceof Object[] arr) return arr.clone();
            if (value instanceof int[] arr) return arr.clone();
            if (value instanceof long[] arr) return arr.clone();
            if (value instanceof float[] arr) return arr.clone();
            if (value instanceof double[] arr) return arr.clone();
            if (value instanceof boolean[] arr) return arr.clone();
            if (value instanceof byte[] arr) return arr.clone();
            if (value instanceof short[] arr) return arr.clone();
            if (value instanceof char[] arr) return arr.clone();
        }
        // Primitive wrappers, enums, strings and most config scalars are immutable.
        return value;
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                IQCommand.register(dispatcher)
        );
    }

    public static IQModClient get() {
        return instance;
    }
}
