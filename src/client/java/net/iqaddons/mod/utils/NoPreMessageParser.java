package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import net.iqaddons.mod.model.spot.PreSpot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class NoPreMessageParser {

    private static final Pattern NO_PRE_PATTERN = Pattern.compile(
            "\\b(?:no|missing)\\s+([a-z]+(?:\\s+[a-z]+)?)\\b",
            Pattern.CASE_INSENSITIVE
    );

    public @Nullable ParsedNoPreCall parse(@NotNull String message) {
        Matcher matcher = NO_PRE_PATTERN.matcher(message);
        while (matcher.find()) {
            String canonicalPileName = toCanonicalPileName(matcher.group(1));
            if (canonicalPileName == null) {
                continue;
            }

            int missingPreValue = PreSpot.getMissingPreValueFromPileName(canonicalPileName);
            if (missingPreValue <= 0) {
                continue;
            }

            return new ParsedNoPreCall(missingPreValue, canonicalPileName);
        }

        return null;
    }

    private @Nullable String toCanonicalPileName(@NotNull String pileToken) {
        String normalized = pileToken
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z]", "");

        return switch (normalized) {
            case "triangle", "tri" -> "TRIANGLE";
            case "equals", "eq" -> "EQUALS";
            case "slash" -> "SLASH";
            case "shop" -> "SHOP";
            case "square" -> "SQUARE";
            case "x" -> "X";
            case "xc", "xcannon" -> "X CANNON";
            default -> null;
        };
    }

    public record ParsedNoPreCall(int missingPreValue, @NotNull String canonicalPileName) {
    }
}

