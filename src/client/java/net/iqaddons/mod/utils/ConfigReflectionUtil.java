package net.iqaddons.mod.utils;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.screen.model.ConfigCategory;
import net.iqaddons.mod.screen.model.ConfigEntryModel;
import net.iqaddons.mod.screen.model.ConfigEntryModel.EntryType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reads ResourcefulConfig annotations reflectively and builds {@link ConfigCategory} instances.
 *
 * <p>Processing order per field:
 * <ol>
 *   <li>{@code @ConfigOption.Separator} → insert a separator before the entry</li>
 *   <li>{@code @ConfigButton} on Runnable → button row</li>
 *   <li>{@code @ConfigEntry} on a {@code @ConfigObject} class → collapsible SECTION_HEADER</li>
 *   <li>{@code @ConfigEntry} on a primitive/enum/color field → typed control</li>
 * </ol>
 */
@Slf4j
@UtilityClass
public class ConfigReflectionUtil {

    public @Nullable ConfigCategory buildCategory(Class<?> configClass) {
        Category ann = configClass.getAnnotation(Category.class);
        String name = ann != null ? ann.value() : "General";
        return new ConfigCategory(name, configClass.getSimpleName(), processClass(configClass));
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private List<ConfigEntryModel> processClass(Class<?> cls) {
        List<ConfigEntryModel> entries = new ArrayList<>();
        for (Field field : cls.getDeclaredFields()) {
            field.setAccessible(true);

            ConfigOption.Separator sep = field.getAnnotation(ConfigOption.Separator.class);

            // ── Button ────────────────────────────────────────────────────
            ConfigButton button = field.getAnnotation(ConfigButton.class);
            if (button != null && Runnable.class.isAssignableFrom(field.getType())) {
                if (sep != null) entries.add(ConfigEntryModel.separator(sep.value()));
                Comment cmt = field.getAnnotation(Comment.class);
                try {
                    Runnable action = (Runnable) field.get(null);
                    entries.add(ConfigEntryModel.button(
                            button.title(), button.text(),
                            normalizeComment(cmt != null ? cmt.value() : null), action));
                } catch (Exception e) {
                    log.warn("Failed to read button field '{}'", field.getName(), e);
                }
                continue;
            }

            ConfigEntry entry = field.getAnnotation(ConfigEntry.class);
            if (entry == null) continue;

            Comment comment = field.getAnnotation(Comment.class);
            String description = normalizeComment(comment != null ? comment.value() : null);
            String label = entry.translation().isBlank() ? entry.id() : entry.translation();

            // ── Array → unsupported ───────────────────────────────────────
            if (field.getType().isArray()) {
                if (sep != null) entries.add(ConfigEntryModel.separator(sep.value()));
                entries.add(ConfigEntryModel.unsupported(label));
                continue;
            }

            // ── ConfigObject → collapsible section ────────────────────────
            boolean isConfigObject = field.getType().isAnnotationPresent(ConfigObject.class);
            if (isConfigObject) {
                if (sep != null) entries.add(ConfigEntryModel.separator(sep.value()));
                List<ConfigEntryModel> children = processClass(field.getType());
                entries.add(ConfigEntryModel.sectionHeader(
                        label,
                        description,
                        resolveDescriptionSupplier(field),
                        resolveVisibilitySupplier(field),
                        children
                ));
                continue;
            }

            // ── Regular entry ─────────────────────────────────────────────
            if (sep != null) entries.add(ConfigEntryModel.separator(sep.value()));
            ConfigEntryModel model = buildEntry(field, label, description);
            if (model != null) entries.add(model);
        }

        return mergeSectionGroups(entries);
    }

    private @Nullable ConfigEntryModel buildEntry(Field field, String label, String description) {
        return buildEntry(field, label, description, null);
    }

    private @Nullable ConfigEntryModel buildEntry(
            Field field,
            String label,
            String description,
            @Nullable Supplier<Boolean> visibilityOverride
    ) {
        Class<?> type = field.getType();
        Supplier<Boolean> visibilitySupplier = visibilityOverride != null
                ? visibilityOverride
                : resolveVisibilitySupplier(field);

        // Color
        if (field.isAnnotationPresent(ConfigOption.Color.class)
                && (type == int.class || type == Integer.class)) {
            boolean alpha = field.getAnnotation(ConfigOption.Color.class).alpha();
            return ConfigEntryModel.builder()
                    .type(EntryType.COLOR).label(label).description(description)
                    .field(field).hasAlpha(alpha)
                    .visibilitySupplier(visibilitySupplier).build();
        }

        // Boolean toggle
        if (type == boolean.class || type == Boolean.class) {
            return ConfigEntryModel.builder()
                    .type(EntryType.BOOLEAN).label(label).description(description)
                    .field(field)
                    .visibilitySupplier(visibilitySupplier).build();
        }

        // Enum select
        if (type.isEnum()) {
            return ConfigEntryModel.builder()
                    .type(EntryType.SELECT).label(label).description(description)
                    .field(field).enumValues(type.getEnumConstants())
                    .enumDescriptionResolver(resolveEnumDescriptionResolver(type))
                    .visibilitySupplier(visibilitySupplier).build();
        }

        // Numeric slider
        if (field.isAnnotationPresent(ConfigOption.Slider.class)
                || type == int.class || type == Integer.class
                || type == float.class || type == Float.class
                || type == double.class || type == Double.class) {
            ConfigOption.Range range = field.getAnnotation(ConfigOption.Range.class);
            double min = range != null ? range.min() : 0.0;
            double max = range != null ? range.max() : defaultNumericMax(field, type);
            EntryType st = (type == int.class || type == Integer.class) ? EntryType.INT_SLIDER
                    : (type == float.class || type == Float.class) ? EntryType.FLOAT_SLIDER
                    : EntryType.DOUBLE_SLIDER;
            return ConfigEntryModel.builder()
                    .type(st).label(label).description(description)
                    .field(field).rangeMin(min).rangeMax(max)
                    .visibilitySupplier(visibilitySupplier).build();
        }

        return ConfigEntryModel.unsupported(label);
    }

    private @Nullable String normalizeComment(@Nullable String comment) {
        if (comment == null || comment.isBlank()) return comment;
        return comment.replace("\r\n", "\n").replace('\r', '\n');
    }

    private @Nullable Supplier<String> resolveDescriptionSupplier(Field field) {
        if (field.getDeclaringClass() == KuudraGeneralConfig.class
                && "customSplitsBenchmarks".equals(field.getName())) {
            return KuudraGeneralConfig.CustomSplitsBenchmarks::sectionDescription;
        }
        return null;
    }

    private @Nullable Function<Object, String> resolveEnumDescriptionResolver(Class<?> type) {
        try {
            Method method = type.getMethod("description");
            if (method.getReturnType() != String.class || method.getParameterCount() != 0) {
                return null;
            }
            return value -> {
                if (value == null || !type.isInstance(value)) return null;
                try {
                    return normalizeComment((String) method.invoke(value));
                } catch (Exception ignored) {
                    return null;
                }
            };
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private @Nullable Supplier<Boolean> resolveVisibilitySupplier(Field field) {
        if (field.getDeclaringClass() == PhaseOneConfig.PearlWaypointsConfig.class
                && isPearlWaypointOffsetField(field)) {
            return () -> PhaseOneConfig.PearlWaypointsConfig.pearlWaypointOffsets;
        }
        Supplier<Boolean> parentFeature = resolveParentFeatureVisibility(field);
        if (parentFeature != null) return parentFeature;
        return null;
    }

    private @Nullable Supplier<Boolean> resolveParentFeatureVisibility(Field field) {
        String name = field.getName();
        if ("splitColorConfig".equals(name) || "customSplitsBenchmarks".equals(name)) {
            return () -> KuudraGeneralConfig.customSplits;
        }
        if ("profitTrackerColorConfig".equals(name) || "profitTrackerConfig".equals(name)) {
            return () -> KuudraGeneralConfig.kuudraProfitTracker;
        }
        if (!name.endsWith("Config")) return null;
        String base = name.substring(0, name.length() - "Config".length());
        Field enabled = findBooleanField(field.getDeclaringClass(), base, base + "Enabled", "enable" + capitalize(base));
        if (enabled == null) return null;
        enabled.setAccessible(true);
        return () -> {
            try {
                return Boolean.TRUE.equals(enabled.get(null));
            } catch (Exception ignored) {
                return true;
            }
        };
    }

    private @Nullable Field findBooleanField(Class<?> owner, String... names) {
        for (String name : names) {
            try {
                Field field = owner.getDeclaredField(name);
                Class<?> type = field.getType();
                if (type == boolean.class || type == Boolean.class) return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private List<ConfigEntryModel> mergeSectionGroups(List<ConfigEntryModel> entries) {
        List<ConfigEntryModel> merged = new ArrayList<>();
        for (ConfigEntryModel entry : entries) {
            if (isSection(entry, "Pace Splits Benchmarks")
                    && !merged.isEmpty()
                    && isSection(merged.get(merged.size() - 1), "Split Time Colors")) {
                ConfigEntryModel previous = merged.remove(merged.size() - 1);
                List<ConfigEntryModel> children = new ArrayList<>();
                children.add(previous);
                children.add(entry);
                merged.add(ConfigEntryModel.sectionHeader(
                        "Kuudra Splits Config",
                        firstNonBlank(previous.getDescription(), entry.getDescription()),
                        null,
                        previous.getVisibilitySupplier(),
                        children
                ));
            } else if (isSection(entry, "Profit Tracker Config")
                    && !merged.isEmpty()
                    && isSection(merged.get(merged.size() - 1), "Profit Tracker Colors")) {
                ConfigEntryModel previous = merged.remove(merged.size() - 1);
                List<ConfigEntryModel> children = new ArrayList<>();
                children.add(previous);
                if (entry.getChildren() != null) children.addAll(entry.getChildren());
                merged.add(ConfigEntryModel.sectionHeader(
                        "Profit Tracker Config",
                        entry.getDescription(),
                        null,
                        previous.getVisibilitySupplier(),
                        children
                ));
            } else {
                merged.add(entry);
            }
        }
        return merged;
    }

    private boolean isSection(ConfigEntryModel entry, String label) {
        return entry.getType() == EntryType.SECTION_HEADER && label.equals(entry.getLabel());
    }

    private String firstNonBlank(@Nullable String first, @Nullable String second) {
        if (first != null && !first.isBlank()) return first;
        return second != null ? second : "";
    }

    private double defaultNumericMax(Field field, Class<?> type) {
        if (type == int.class || type == Integer.class) {
            try {
                Object value = field.get(null);
                if (value instanceof Number number) {
                    return Math.max(10.0, number.doubleValue() * 5.0);
                }
            } catch (Exception ignored) {
            }
            return 20.0;
        }
        return 10.0;
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private boolean isPearlWaypointOffsetField(@Nullable Field field) {
        if (field == null) return false;
        return switch (field.getName()) {
            case "pearlFlatXAreaYOffset",
                 "pearlFlatEqualsAreaYOffset",
                 "pearlFlatSlashAreaYOffset",
                 "pearlFlatTriangleAreaYOffset",
                 "pearlFlatSquareAreaYOffset",
                 "pearlFlatShopAreaYOffset" -> true;
            default -> false;
        };
    }
}
