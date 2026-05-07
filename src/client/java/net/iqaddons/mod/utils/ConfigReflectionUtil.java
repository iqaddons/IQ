package net.iqaddons.mod.utils;

import com.teamresourceful.resourcefulconfig.api.annotations.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.screen.model.ConfigCategory;
import net.iqaddons.mod.screen.model.ConfigEntryModel;
import net.iqaddons.mod.screen.model.ConfigEntryModel.EntryType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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
                entries.add(ConfigEntryModel.sectionHeader(label, description, children));
                continue;
            }

            // ── Regular entry ─────────────────────────────────────────────
            if (sep != null) entries.add(ConfigEntryModel.separator(sep.value()));
            ConfigEntryModel model = buildEntry(field, label, description);
            if (model != null) entries.add(model);
        }

        return entries;
    }

    private @Nullable ConfigEntryModel buildEntry(Field field, String label, String description) {
        Class<?> type = field.getType();

        // Color
        if (field.isAnnotationPresent(ConfigOption.Color.class)
                && (type == int.class || type == Integer.class)) {
            boolean alpha = field.getAnnotation(ConfigOption.Color.class).alpha();
            return ConfigEntryModel.builder()
                    .type(EntryType.COLOR).label(label).description(description)
                    .field(field).hasAlpha(alpha).build();
        }

        // Boolean toggle
        if (type == boolean.class || type == Boolean.class) {
            return ConfigEntryModel.builder()
                    .type(EntryType.BOOLEAN).label(label).description(description)
                    .field(field).build();
        }

        // Enum select
        if (type.isEnum()) {
            return ConfigEntryModel.builder()
                    .type(EntryType.SELECT).label(label).description(description)
                    .field(field).enumValues(type.getEnumConstants()).build();
        }

        // String input
        if (type == String.class) {
            return ConfigEntryModel.builder()
                    .type(EntryType.STRING).label(label).description(description)
                    .field(field).build();
        }

        // Numeric slider
        if (field.isAnnotationPresent(ConfigOption.Slider.class)) {
            ConfigOption.Range range = field.getAnnotation(ConfigOption.Range.class);
            double min = range != null ? range.min() : 0.0;
            double max = range != null ? range.max() : 1.0;
            EntryType st = (type == int.class || type == Integer.class) ? EntryType.INT_SLIDER
                    : (type == float.class || type == Float.class) ? EntryType.FLOAT_SLIDER
                    : EntryType.DOUBLE_SLIDER;
            return ConfigEntryModel.builder()
                    .type(st).label(label).description(description)
                    .field(field).rangeMin(min).rangeMax(max).build();
        }

        return ConfigEntryModel.unsupported(label);
    }

    private @Nullable String normalizeComment(@Nullable String comment) {
        if (comment == null || comment.isBlank()) return comment;
        return comment.replace("\r\n", "\n").replace('\r', '\n');
    }
}