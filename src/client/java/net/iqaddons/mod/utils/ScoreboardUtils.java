package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@UtilityClass
public final class ScoreboardUtils {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Pattern AREA_SYMBOL_PATTERN = Pattern.compile("[⏣ф]");

    private static final long AREA_STICKY_MS = 3000L;

    private static volatile String lastSeenArea = "";
    private static volatile long lastSeenAreaAt = 0L;

    public static @NotNull String getTitle() {
        return getObjective()
                .map(obj -> StringUtils.stripFormatting(obj.getDisplayName().getString()))
                .orElse("");
    }

    public static boolean hasTitle(@NotNull String text) {
        return getTitle().contains(text);
    }

    public static @NotNull List<String> getLines() {
        return getObjective()
                .map(ScoreboardUtils::extractLines)
                .orElse(Collections.emptyList());
    }

    public static @NotNull Optional<String> findLine(@NotNull String containing) {
        return getLines().stream()
                .filter(line -> StringUtils.stripFormatting(line).contains(containing))
                .findFirst();
    }

    public static @NotNull String getArea() {
        Optional<String> found = findLine("⏣").or(() -> findLine("ф"));

        if (found.isPresent()) {
            String cleaned = StringUtils.stripFormatting(found.get());
            cleaned = AREA_SYMBOL_PATTERN.matcher(cleaned).replaceAll("").trim();
            // update cache
            lastSeenArea = cleaned;
            lastSeenAreaAt = System.currentTimeMillis();
            return cleaned;
        }

        if (!lastSeenArea.isEmpty() && (System.currentTimeMillis() - lastSeenAreaAt) <= AREA_STICKY_MS) {
            return lastSeenArea;
        }

        return "";
    }

    public static boolean isInArea(@NotNull String areaName) {
        return getArea().startsWith(areaName);
    }

    private static Optional<Objective> getObjective() {
        if (MC.level == null) return Optional.empty();

        Scoreboard scoreboard = MC.level.getScoreboard();
        return Optional.ofNullable(scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR));
    }

    private static List<String> extractLines(@NotNull Objective objective) {
        Scoreboard scoreboard = MC.level.getScoreboard();

        return scoreboard.listPlayerScores(objective).stream()
                .map(entry -> buildLineText(scoreboard, entry))
                .collect(Collectors.toList());
    }

    private static String buildLineText(@NotNull Scoreboard scoreboard, @NotNull PlayerScoreEntry entry) {
        PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
        if (team == null) {
            return entry.owner();
        }
        return team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();
    }
}