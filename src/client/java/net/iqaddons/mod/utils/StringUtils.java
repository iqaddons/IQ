package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class StringUtils {

    private static final Pattern MINECRAFT_NAME_PATTERN = Pattern.compile("([A-Za-z0-9_]{3,16})(?!.*[A-Za-z0-9_]{3,16})");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§([0-9a-fA-F])");

    public @NotNull String extractFormattedPlayerName(@NotNull String formattedMessage) {
        int endIndex = findMessageSeparator(formattedMessage);
        String playerSection = endIndex > 0
                ? formattedMessage.substring(0, endIndex).trim()
                : formattedMessage;

        return formatPlayerNick(playerSection);
    }

    private @NotNull String findLastColorBefore(@NotNull String formatted, int endExclusive) {
        Matcher colorMatcher = COLOR_CODE_PATTERN.matcher(formatted.substring(0, endExclusive));
        String lastColor = "§7";
        while (colorMatcher.find()) {
            lastColor = "§" + colorMatcher.group(1).toLowerCase();
        }

        return lastColor;
    }

    public @NotNull String getShortMessage(@NotNull String message) {
        if (message.length() <= 30) {
            return message;
        }

        return message.substring(0, 30) + "...";
    }

    public @NotNull String formatPlayerNick(@NotNull String rawPlayerText) {
        String normalizedText = removeChatPrefix(rawPlayerText.trim());
        String plainText = normalizedText.replaceAll("§.", "");

        Matcher nameMatcher = MINECRAFT_NAME_PATTERN.matcher(plainText);
        if (!nameMatcher.find()) {
            return normalizedText;
        }

        String playerName = nameMatcher.group(1);
        int tokenStartInFormatted = findTokenStartInFormatted(normalizedText, plainText, playerName);
        String rankColor = tokenStartInFormatted >= 0
                ? findLastColorBefore(normalizedText, tokenStartInFormatted)
                : "§f";

        return rankColor + playerName;
    }

    private int findMessageSeparator(@NotNull String formattedMessage) {
        int recoveredIndex = formattedMessage.indexOf("recovered");
        if (recoveredIndex > 0) {
            return recoveredIndex;
        }

        int droppedIndex = formattedMessage.indexOf("dropped");
        if (droppedIndex > 0) {
            return droppedIndex;
        }

        return formattedMessage.indexOf(':');
    }

    private @NotNull String removeChatPrefix(@NotNull String rawPlayerText) {
        String strippedPrefix = rawPlayerText.replaceFirst("(?i)^(§.)*party\\s*>\\s*", "");
        int separator = strippedPrefix.indexOf(':');
        return separator > 0 ? strippedPrefix.substring(0, separator).trim() : strippedPrefix;
    }

    private int findTokenStartInFormatted(@NotNull String formatted, @NotNull String plain, @NotNull String token) {
        int tokenStartInPlain = plain.lastIndexOf(token);
        if (tokenStartInPlain < 0) {
            return -1;
        }

        int plainIndex = 0;
        for (int formattedIndex = 0; formattedIndex < formatted.length(); formattedIndex++) {
            char current = formatted.charAt(formattedIndex);
            if (current == '§' && formattedIndex + 1 < formatted.length()) {
                formattedIndex++;
                continue;
            }

            if (plainIndex == tokenStartInPlain) {
                return formattedIndex;
            }

            plainIndex++;
        }

        return -1;
    }
}
