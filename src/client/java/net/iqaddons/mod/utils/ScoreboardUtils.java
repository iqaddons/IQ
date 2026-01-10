package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@UtilityClass
public final class ScoreboardUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static String getTitle() {
        if (mc.world == null) return "";

        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (sidebar == null) return "";
        return sidebar.getDisplayName().getString();
    }

    public static boolean hasTitle(String text) {
        return stripFormatting(getTitle()).contains(text);
    }

    public static @NotNull List<String> getLines() {
        if (mc.world == null) return Collections.emptyList();

        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (sidebar == null) return Collections.emptyList();

        List<String> lines = new ArrayList<>();
        for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(sidebar)) {
            Team team = scoreboard.getScoreHolderTeam(entry.owner());
            String line;
            if (team != null) {
                line = team.getPrefix().getString() + team.getSuffix().getString();
            } else {
                line = entry.owner();
            }
            lines.add(line);
        }

        return lines;
    }

    public static String getLine(int index) {
        List<String> lines = getLines();
        if (index < 0 || index >= lines.size()) return "";
        return lines.get(index);
    }

    public static String findLine(String containing) {
        return getLines().stream()
                .filter(line -> stripFormatting(line).contains(containing))
                .findFirst()
                .orElse("");
    }

    public static @NotNull String getArea() {
        String areaLine = findLine("⏣");
        if (areaLine.isEmpty()) {
            areaLine = findLine("ф");
        }
        return stripFormatting(areaLine).replace("⏣", "").replace("ф", "").trim();
    }

    @Contract(pure = true)
    public static @NotNull String stripFormatting(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
}


