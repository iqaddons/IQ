package net.iqaddons.mod.screen.model;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a single renderable config entry.
 * ConfigObject sections use {@link EntryType#SECTION_HEADER} and collapse/expand in-place.
 */
@Builder
@Getter
public class ConfigEntryModel {

    public enum EntryType {
        BOOLEAN,
        INT_SLIDER,
        FLOAT_SLIDER,
        DOUBLE_SLIDER,
        COLOR,
        SELECT,
        STRING,
        BUTTON,
        SEPARATOR,
        SECTION_HEADER,
        UNSUPPORTED
    }

    private final EntryType type;
    private final String label;

    @Nullable
    private final String description;
    @Nullable
    private final Field field;

    // SEPARATOR
    @Nullable
    private final String separatorLabel;

    // SLIDER
    @Builder.Default
    private final double rangeMin = 0.0;
    @Builder.Default
    private final double rangeMax = 1.0;

    // COLOR
    @Builder.Default
    private final boolean hasAlpha = false;

    // BUTTON
    @Nullable
    private final String buttonText;
    @Nullable
    private final Runnable buttonAction;

    // SELECT
    @Nullable
    private final Object[] enumValues;

    // SECTION_HEADER
    @Nullable
    private final List<ConfigEntryModel> children;
    @Builder.Default
    private final AtomicBoolean expanded = new AtomicBoolean(false);

    // ── Factories ──────────────────────────────────────────────────────────

    public static ConfigEntryModel separator(@Nullable String label) {
        return ConfigEntryModel.builder().type(EntryType.SEPARATOR).label("").separatorLabel(label).build();
    }

    public static ConfigEntryModel sectionHeader(String label, @Nullable String description,
                                                 List<ConfigEntryModel> children) {
        return ConfigEntryModel.builder()
                .type(EntryType.SECTION_HEADER).label(label).description(description)
                .children(children).expanded(new AtomicBoolean(false)).build();
    }

    public static ConfigEntryModel button(String label, String text,
                                          @Nullable String desc, Runnable action) {
        return ConfigEntryModel.builder().type(EntryType.BUTTON).label(label)
                .buttonText(text).description(desc).buttonAction(action).build();
    }

    public static ConfigEntryModel unsupported(String label) {
        return ConfigEntryModel.builder().type(EntryType.UNSUPPORTED).label(label).build();
    }

    public boolean isExpanded() {
        return expanded != null && expanded.get();
    }

    public void toggleExpanded() {
        if (expanded != null) expanded.set(!expanded.get());
    }
}