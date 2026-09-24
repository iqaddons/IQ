package net.iqaddons.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.iqaddons.mod.commands.IQCommand;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.config.loader.CratePriorityConfigLoader;
import net.iqaddons.mod.config.loader.EtherwarpConfigLoader;
import net.iqaddons.mod.config.loader.PearlWaypointConfigLoader;
import net.iqaddons.mod.events.dispatcher.KuudraEventsDispatcher;
import net.iqaddons.mod.features.FeatureManager;
import net.iqaddons.mod.integration.DiscordRPCIntegration;
import net.iqaddons.mod.lifecycle.LifecycleComponent;
import net.iqaddons.mod.lifecycle.modules.FeatureModule;
import net.iqaddons.mod.lifecycle.modules.KuudraModule;
import net.iqaddons.mod.lifecycle.modules.WidgetModule;
import net.iqaddons.mod.nanovg.IqNanoVg;
import net.iqaddons.mod.nanovg.IqNanoVgConfiguration;
import net.iqaddons.mod.utils.update.ModrinthUpdateChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
public class IQModClient implements ClientModInitializer {

    private static final String MOD_ID = "iqaddons";
    private static final Gson MAIN_CONFIG_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static final Pattern LEGACY_HIDE_USELESS_ARMOR_STANDS_ARRAY_PATTERN = Pattern.compile(
            "(?ms)^(\\s*)\"hideUselessArmorStands\"\\s*:\\s*\\[(.*?)\\]\\s*,\\s*$"
    );
    private static final String SPLIT_COLOR_MIGRATION_FLAG = "_splitsDarkAquaMigrationV1";
    private static final String DEFAULT_MAIN_CONFIG_RESOURCE = "/default-config/iqaddons.jsonc";
    private static IQModClient instance;

    public static Minecraft mc = Minecraft.getInstance();

    private Configurator configurator;
    private @Nullable FeatureManager featureManager;
    private @Nullable JsonObject startupMainConfigSnapshot;
    private boolean mainConfigSafeToSave = true;

    private final List<LifecycleComponent> components = new ArrayList<>();
    private final Map<Field, Object> mainConfigDefaults = new LinkedHashMap<>();

    public static IQModClient getInstance() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        IqNanoVgConfiguration.logSelectedMode();

        // Snapshot class-declared defaults before any config file or migration can apply saved values.
        captureMainConfigDefaults();

        ensureDefaultMainConfigExists();
        migrateLegacyHideUselessArmorStandsConfig();
        migrateSplitColorsToDarkAquaOnFirstLaunch();
        captureStartupMainConfigSnapshot();

        configurator = new Configurator(MOD_ID);
        configurator.register(Configuration.class);
        CratePriorityConfigLoader.get().load();
        EtherwarpConfigLoader.get().load();
        PearlWaypointConfigLoader.get().load();

        FeatureModule featureModule = new FeatureModule();
        initializeModules(
                new KuudraModule(), new KuudraEventsDispatcher(),
                featureModule, new WidgetModule()
        );
        this.featureManager = featureModule.getFeatures();

        IQKeyBindings.register();
        registerCommands();
        registerResourceReloadHooks();
        ModrinthUpdateChecker.INSTANCE.register();

//        ChunkSectionLayerMap.putFluids(ChunkSectionLayer.TRANSLUCENT, Fluids.LAVA, Fluids.FLOWING_LAVA); 26.1 changed how this works based on textures now?
//        BlockRenderLayerMap.putFluids(BlockRenderLayer.TRANSLUCENT, Fluids.LAVA, Fluids.FLOWING_LAVA); // TODO: verify 26.1 API for fluid render layers

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            saveMainConfig();
            components.forEach(LifecycleComponent::stop);
            IqNanoVgConfiguration.mode().runPreparation(IqNanoVg::dispose);

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

    private void registerResourceReloadHooks() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "nanovg_resource_reload"),
                new SimpleReloadListener<Void>() {
                    @Override
                    protected Void prepare(PreparableReloadListener.SharedState state) {
                        return null;
                    }

                    @Override
                    protected void apply(Void prepared, PreparableReloadListener.SharedState state) {
                        IqNanoVgConfiguration.mode().runReset(() ->
                                Minecraft.getInstance().execute(() ->
                                        IqNanoVg.resetAfterRenderTransition("client resource reload")));
                    }
                }
        );
        ResourceLoader.get(PackType.CLIENT_RESOURCES).addListenerOrdering(
                ResourceReloaderKeys.AFTER_VANILLA,
                Identifier.fromNamespaceAndPath(MOD_ID, "nanovg_resource_reload")
        );
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
            Path configFile = FabricLoader.getInstance().getConfigDir().resolve("iqaddons.jsonc");
            if (Files.exists(configFile)) {
                String content = Files.readString(configFile, StandardCharsets.UTF_8);
                if (content.contains("\"" + SPLIT_COLOR_MIGRATION_FLAG + "\"")) {
                    return;
                }

                String migrated = replaceSplitColorConfig(content);
                if (migrated != null) {
                    Files.writeString(configFile, addRootBooleanFlag(migrated, SPLIT_COLOR_MIGRATION_FLAG), StandardCharsets.UTF_8);
                    log.info("Migrated Custom Splits colors to DARK_AQUA in iqaddons.jsonc");
                    return;
                }

                Files.writeString(configFile, addRootBooleanFlag(content, SPLIT_COLOR_MIGRATION_FLAG), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Failed to migrate Custom Splits colors to DARK_AQUA", e);
        }
    }

    private @NotNull String addRootBooleanFlag(@NotNull String content, @NotNull String key) {
        String trimmed = content.stripTrailing();
        int close = trimmed.lastIndexOf('}');
        if (close < 0 || trimmed.contains("\"" + key + "\"")) {
            return content;
        }

        String before = trimmed.substring(0, close).stripTrailing();
        String after = trimmed.substring(close);
        String separator = before.endsWith("{") ? "\n  " : ",\n  ";
        return before + separator + "\"" + key + "\": true\n" + after + "\n";
    }

    private void ensureDefaultMainConfigExists() {
        try {
            Path configFile = FabricLoader.getInstance().getConfigDir().resolve("iqaddons.jsonc");
            if (Files.exists(configFile)) {
                return;
            }

            try (var is = getClass().getResourceAsStream(DEFAULT_MAIN_CONFIG_RESOURCE)) {
                if (is == null) {
                    return;
                }
                Files.createDirectories(configFile.getParent());
                Files.copy(is, configFile);
                log.info("Created default iqaddons.jsonc from bundled template");
            }
        } catch (Exception e) {
            log.warn("Failed to create default iqaddons.jsonc from bundled template", e);
        }
    }

    private void migrateLegacyKuudraNotificationsConfig() {
        try {
            Path configFile = FabricLoader.getInstance().getConfigDir().resolve("iqaddons.jsonc");
            if (!Files.exists(configFile)) {
                return;
            }

            String content = Files.readString(configFile, StandardCharsets.UTF_8);
            if (content.contains("\"kuudraNotificationsConfig\"")) {
                return;
            }

            Pattern legacyPattern = Pattern.compile(
                    "(?ms)^(\\s*)\\\"kuudraNotifications\\\"\\s*:\\s*(\\{.*?^\\1\\})\\s*,\\s*\\R"
                            + "\\1\\\"kuudraNotificationsSound\\\"\\s*:\\s*(true|false)\\s*,\\s*\\R"
                            + "\\1\\\"abilityAnnounce\\\"\\s*:\\s*(\\{.*?^\\1\\})\\s*,"
            );

            Matcher matcher = legacyPattern.matcher(content);
            if (!matcher.find()) {
                return;
            }

            String indent = matcher.group(1);
            String notificationToggles = matcher.group(2);
            String notificationSound = matcher.group(3);
            String abilityAnnounce = matcher.group(4);

            String replacement = indent + "\"kuudraNotificationsEnabled\": true,\n"
                    + indent + "\"kuudraNotificationsConfig\": {\n"
                    + indent + "    \"kuudraNotificationsSound\": " + notificationSound + ",\n"
                    + indent + "    \"kuudraNotifications\": " + notificationToggles + ",\n"
                    + indent + "    \"abilityAnnounce\": " + abilityAnnounce + "\n"
                    + indent + "},";

            String migrated = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
            Files.writeString(configFile, migrated, StandardCharsets.UTF_8);
            log.info("Migrated legacy Kuudra notifications config keys: kuudraNotifications, kuudraNotificationsSound, abilityAnnounce");
        } catch (Exception e) {
            log.warn("Failed to migrate legacy Kuudra notifications config", e);
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
        Path legacyJsonc = configDir.resolve("iqmod.jsonc");
        Path legacyJson = configDir.resolve("iqmod.json");

        restoreMainConfigDefaultsInMemory();

        try {
            Files.deleteIfExists(jsonc);
            Files.deleteIfExists(json);
            Files.deleteIfExists(legacyJsonc);
            Files.deleteIfExists(legacyJson);
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
        captureStartupMainConfigSnapshot();
    }

    public synchronized int resetMainConfigFieldsToDefaults(@NotNull Collection<Field> fields) {
        if (mainConfigDefaults.isEmpty()) {
            captureMainConfigDefaults();
        }

        int updated = 0;
        for (Field field : fields) {
            Object defaultValue = mainConfigDefaults.get(field);
            if (defaultValue == null && !mainConfigDefaults.containsKey(field)) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(null, cloneConfigValue(defaultValue));
                updated++;
            } catch (Exception e) {
                log.warn("Failed to restore default for {}.{}", field.getDeclaringClass().getSimpleName(), field.getName(), e);
            }
        }

        if (updated > 0) {
            saveMainConfig();
        }
        return updated;
    }

    public synchronized void saveMainConfig() {
        if (configurator == null) {
            return;
        }
        if (!mainConfigSafeToSave) {
            log.warn("Skipped IQ main config save because the existing iqaddons.jsonc could not be read safely");
            return;
        }

        try {
            configurator.saveConfig(Configuration.class);
            preserveStartupMainConfigValues();
        } catch (Exception e) {
            log.warn("Failed to save IQ main config", e);
        }
    }

    private void captureStartupMainConfigSnapshot() {
        Path configFile = mainConfigPath();
        if (!Files.exists(configFile)) {
            startupMainConfigSnapshot = null;
            mainConfigSafeToSave = true;
            return;
        }

        try {
            JsonElement parsed = parseJsonc(Files.readString(configFile, StandardCharsets.UTF_8));
            if (parsed != null && parsed.isJsonObject()) {
                startupMainConfigSnapshot = parsed.getAsJsonObject().deepCopy();
                mainConfigSafeToSave = true;
            } else {
                startupMainConfigSnapshot = null;
                mainConfigSafeToSave = false;
                log.warn("Existing iqaddons.jsonc is not a JSON object; saves are disabled to avoid resetting it");
            }
        } catch (Exception e) {
            startupMainConfigSnapshot = null;
            mainConfigSafeToSave = false;
            log.warn("Failed to parse existing iqaddons.jsonc; saves are disabled to avoid overwriting it with defaults", e);
        }
    }

    private void preserveStartupMainConfigValues() {
        if (startupMainConfigSnapshot == null) {
            return;
        }

        Path configFile = mainConfigPath();
        if (!Files.exists(configFile)) {
            return;
        }

        try {
            JsonElement parsed = parseJsonc(Files.readString(configFile, StandardCharsets.UTF_8));
            if (parsed == null || !parsed.isJsonObject()) {
                log.warn("Skipped IQ main config preservation because saved iqaddons.jsonc is not a JSON object");
                return;
            }

            JsonObject saved = parsed.getAsJsonObject();
            if (mergeMissingConfigValues(saved, startupMainConfigSnapshot)) {
                Files.writeString(configFile, MAIN_CONFIG_GSON.toJson(saved), StandardCharsets.UTF_8);
                log.info("Preserved unknown IQ config values from the previous iqaddons.jsonc");
            }
        } catch (Exception e) {
            log.warn("Failed to preserve unknown IQ config values after save", e);
        }
    }

    private boolean mergeMissingConfigValues(@NotNull JsonObject target, @NotNull JsonObject source) {
        boolean changed = false;
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            String key = entry.getKey();
            JsonElement sourceValue = entry.getValue();
            JsonElement targetValue = target.get(key);

            if (targetValue == null) {
                target.add(key, sourceValue.deepCopy());
                changed = true;
                continue;
            }

            if (targetValue.isJsonObject() && sourceValue.isJsonObject()) {
                changed |= mergeMissingConfigValues(targetValue.getAsJsonObject(), sourceValue.getAsJsonObject());
            }
        }
        return changed;
    }

    private @Nullable JsonElement parseJsonc(@NotNull String content) {
        JsonReader reader = new JsonReader(new StringReader(content));
        reader.setLenient(true);
        return JsonParser.parseReader(reader);
    }

    private @NotNull Path mainConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("iqaddons.jsonc");
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
