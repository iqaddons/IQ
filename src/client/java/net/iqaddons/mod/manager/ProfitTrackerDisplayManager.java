package net.iqaddons.mod.manager;

import net.iqaddons.mod.model.profit.ProfitTrackerDisplayLine;
import net.iqaddons.mod.utils.data.DataKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ProfitTrackerDisplayManager {

    private static final DataKey<PersistentDisplayOptions> DISPLAY_OPTIONS_KEY =
            DataKey.of("kuudraProfitTrackerDisplayOptions", PersistentDisplayOptions.class);
    private static final ProfitTrackerDisplayManager INSTANCE = new ProfitTrackerDisplayManager();

    private final IQPersistentDataStore store = IQPersistentDataStore.get();
    private volatile List<ProfitTrackerDisplayLine> activeLines = defaultOrder();
    private volatile List<ProfitTrackerDisplayLine> inactiveLines = List.of();

    private ProfitTrackerDisplayManager() {
        PersistentDisplayOptions persisted = store.getOrDefault(DISPLAY_OPTIONS_KEY, new PersistentDisplayOptions());
        applyLayout(parseLines(persisted.activeLineIds), parseLines(persisted.inactiveLineIds), false);
    }

    public static @NotNull ProfitTrackerDisplayManager get() {
        return INSTANCE;
    }

    public @NotNull List<ProfitTrackerDisplayLine> activeLines() {
        return List.copyOf(activeLines);
    }

    public @NotNull List<ProfitTrackerDisplayLine> inactiveLines() {
        return List.copyOf(inactiveLines);
    }

    public boolean isActive(@NotNull ProfitTrackerDisplayLine line) {
        return activeLines.contains(line);
    }

    public synchronized void setLayout(
            @NotNull List<ProfitTrackerDisplayLine> active,
            @NotNull List<ProfitTrackerDisplayLine> inactive
    ) {
        applyLayout(active, inactive, true);
    }

    public synchronized void resetDefaults() {
        applyLayout(defaultOrder(), List.of(), true);
    }

    private synchronized void applyLayout(
            @NotNull List<ProfitTrackerDisplayLine> active,
            @NotNull List<ProfitTrackerDisplayLine> inactive,
            boolean save
    ) {
        Set<ProfitTrackerDisplayLine> seen = EnumSet.noneOf(ProfitTrackerDisplayLine.class);
        List<ProfitTrackerDisplayLine> normalizedActive = new ArrayList<>();
        List<ProfitTrackerDisplayLine> normalizedInactive = new ArrayList<>();

        appendUnique(active, normalizedActive, seen);
        appendUnique(inactive, normalizedInactive, seen);

        for (ProfitTrackerDisplayLine line : defaultOrder()) {
            if (seen.add(line)) {
                normalizedActive.add(line);
            }
        }

        activeLines = List.copyOf(normalizedActive);
        inactiveLines = List.copyOf(normalizedInactive);

        if (save) {
            save();
        }
    }

    private void appendUnique(
            @NotNull List<ProfitTrackerDisplayLine> source,
            @NotNull List<ProfitTrackerDisplayLine> target,
            @NotNull Set<ProfitTrackerDisplayLine> seen
    ) {
        for (ProfitTrackerDisplayLine line : source) {
            if (line != null && seen.add(line)) {
                target.add(line);
            }
        }
    }

    private @NotNull List<ProfitTrackerDisplayLine> parseLines(@NotNull List<String> ids) {
        List<ProfitTrackerDisplayLine> lines = new ArrayList<>();
        for (String id : ids) {
            ProfitTrackerDisplayLine line = ProfitTrackerDisplayLine.fromId(id);
            if (line != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private void save() {
        store.set(DISPLAY_OPTIONS_KEY, new PersistentDisplayOptions(activeLines, inactiveLines));
    }

    private static @NotNull List<ProfitTrackerDisplayLine> defaultOrder() {
        return List.of(ProfitTrackerDisplayLine.values());
    }

    public static final class PersistentDisplayOptions {
        public List<String> activeLineIds;
        public List<String> inactiveLineIds;

        public PersistentDisplayOptions() {
            this.activeLineIds = defaultOrder().stream().map(ProfitTrackerDisplayLine::id).toList();
            this.inactiveLineIds = List.of();
        }

        public PersistentDisplayOptions(
                @NotNull List<ProfitTrackerDisplayLine> activeLines,
                @NotNull List<ProfitTrackerDisplayLine> inactiveLines
        ) {
            this.activeLineIds = activeLines.stream().map(ProfitTrackerDisplayLine::id).toList();
            this.inactiveLineIds = inactiveLines.stream().map(ProfitTrackerDisplayLine::id).toList();
        }
    }
}
