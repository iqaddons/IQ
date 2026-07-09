package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@UtilityClass
public final class ScoreboardUtils {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Pattern AREA_SYMBOL_PATTERN = Pattern.compile("[⏣ф]");
    private static final Pattern LEADING_DECORATION_PATTERN = Pattern.compile("^[^\\p{L}\\p{N}]+");

    private static final String KUUDRA_AREA_TEXT = "Kuudra's Hollow";
    private static final String KUUDRA_AREA_TEXT_ALT = "Kuudra’s Hollow";

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
                .or(ScoreboardUtils::findAreaLineByText)
                .map(ScoreboardUtils::normalizeAreaLine)
                .orElse("");
    }

    public static boolean isInArea(@NotNull String areaName) {
        String normalizedArea = normalizeAreaName(getArea());
        String normalizedExpected = normalizeAreaName(areaName);
        return normalizedArea.contains(normalizedExpected);
    }

    private static @NotNull Optional<String> findAreaLineByText() {
        return getLines().stream()
                .map(StringUtils::stripFormatting)
                .filter(line -> line.contains(KUUDRA_AREA_TEXT)
                        || line.contains(KUUDRA_AREA_TEXT_ALT)
                        || line.contains("Dungeon Hub")
                        || line.contains("Forgotten Skull"))
                .findFirst();
    }

    private static @NotNull String normalizeAreaLine(@NotNull String line) {
        String stripped = StringUtils.stripFormatting(line);
        String normalizedApostrophe = stripped.replace('’', '\'');
        String withoutLegacySymbols = AREA_SYMBOL_PATTERN.matcher(normalizedApostrophe).replaceAll("");
        return LEADING_DECORATION_PATTERN.matcher(withoutLegacySymbols).replaceFirst("").trim();
    }

    private static @NotNull String normalizeAreaName(@NotNull String areaName) {
        return areaName
                .replace('’', '\'')
                .toLowerCase(Locale.ROOT)
                .trim();
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