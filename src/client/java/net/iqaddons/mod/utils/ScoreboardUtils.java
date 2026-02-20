package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@UtilityClass
public final class ScoreboardUtils {

    private static final MinecraftClient MC = MinecraftClient.getInstance();
    private static final Pattern AREA_SYMBOL_PATTERN = Pattern.compile("[⏣ф]");

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
        return findLine("⏣")
                .or(() -> findLine("ф"))
                .map(StringUtils::stripFormatting)
                .map(line -> AREA_SYMBOL_PATTERN.matcher(line).replaceAll(""))
                .map(String::trim)
                .orElse("");
    }

    public static boolean isInArea(@NotNull String areaName) {
        return getArea().startsWith(areaName);
    }

    private static Optional<ScoreboardObjective> getObjective() {
        if (MC.world == null) return Optional.empty();

        Scoreboard scoreboard = MC.world.getScoreboard();
        return Optional.ofNullable(scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR));
    }

    private static List<String> extractLines(@NotNull ScoreboardObjective objective) {
        Scoreboard scoreboard = MC.world.getScoreboard();

        return scoreboard.getScoreboardEntries(objective).stream()
                .map(entry -> buildLineText(scoreboard, entry))
                .collect(Collectors.toList());
    }

    private static String buildLineText(@NotNull Scoreboard scoreboard, @NotNull ScoreboardEntry entry) {
        Team team = scoreboard.getScoreHolderTeam(entry.owner());
        if (team == null) {
            return entry.owner();
        }
        return team.getPrefix().getString() + team.getSuffix().getString();
    }
}