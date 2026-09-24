package net.iqaddons.mod.model.profit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ProfitTrackerDisplayLine {
    PROFIT("profit", "Profit"),
    RUNS("runs", "Runs"),
    CHESTS("chests", "Chests"),
    REROLLS("rerolls", "Rerolls"),
    AVG_TIME("avgTime", "Avg Time"),
    BEST_TIME("bestTime", "Best Time"),
    TIME("time", "Time"),
    RATE("rate", "Rate"),
    TRACKING("tracking", "Tracking");

    private final String id;
    private final String displayName;

    ProfitTrackerDisplayLine(@NotNull String id, @NotNull String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull String displayName() {
        return displayName;
    }

    public static @Nullable ProfitTrackerDisplayLine fromId(@Nullable String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        for (ProfitTrackerDisplayLine line : values()) {
            if (line.id.equalsIgnoreCase(id) || line.name().equalsIgnoreCase(id)) {
                return line;
            }
        }

        return null;
    }
}
