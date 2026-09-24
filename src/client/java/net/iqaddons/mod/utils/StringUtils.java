package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class StringUtils {

    private static final Minecraft client = Minecraft.getInstance();

    private static final Pattern MINECRAFT_NAME_PATTERN = Pattern.compile("([A-Za-z0-9_]{3,16})(?!.*[A-Za-z0-9_]{3,16})");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§([0-9a-fA-F])");

    public @NotNull String normalizeItemName(@NotNull ItemStack stack) {
        return normalizeText(StringUtils.stripFormatting(stack.getHoverName().getString()));
    }

    public @NotNull String normalizeText(@NotNull String text) {
        return text.toLowerCase(Locale.ROOT);
    }

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

    @NotNull
    public String getPlayerNick(@NotNull LocalPlayer player) {
        if (client.getConnection() == null) {
            return "§7" + player.getName().getString();
        }

        PlayerInfo entry = client.getConnection().getPlayerInfo(player.getUUID());
        if (entry == null || entry.getTabListDisplayName() == null) {
            return "§7" + player.getName().getString();
        }

        String displayName = TextFormatUtil.toLegacyString(entry.getTabListDisplayName());
        return StringUtils.formatPlayerNick(displayName);
    }

    public @NotNull String formatPlayerNick(@NotNull String rawPlayerText) {
        String normalizedText = removeChatPrefix(rawPlayerText.trim());
        String plainText = stripFormatting(normalizedText);

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
        String strippedPrefix = rawPlayerText;
        int index = 0;
        while (index + 1 < strippedPrefix.length() && strippedPrefix.charAt(index) == '§') {
            index += 2;
        }
        if (strippedPrefix.regionMatches(true, index, "party", 0, 5)) {
            int afterParty = index + 5;
            while (afterParty < strippedPrefix.length() && Character.isWhitespace(strippedPrefix.charAt(afterParty))) {
                afterParty++;
            }
            if (afterParty < strippedPrefix.length() && strippedPrefix.charAt(afterParty) == '>') {
                afterParty++;
                while (afterParty < strippedPrefix.length() && Character.isWhitespace(strippedPrefix.charAt(afterParty))) {
                    afterParty++;
                }
                strippedPrefix = strippedPrefix.substring(afterParty);
            }
        }
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

    public static @NotNull String stripFormatting(@NotNull String text) {
        int colorCodeIndex = text.indexOf('§');
        if (colorCodeIndex < 0 || colorCodeIndex + 1 >= text.length()) {
            return text;
        }

        StringBuilder stripped = new StringBuilder(text.length());
        stripped.append(text, 0, colorCodeIndex);
        for (int index = colorCodeIndex; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '§' && index + 1 < text.length() && isFormattingCode(text.charAt(index + 1))) {
                index++;
                continue;
            }
            stripped.append(current);
        }
        return stripped.toString();
    }

    public static @NotNull String digitsOnly(@NotNull String value) {
        StringBuilder digits = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current >= '0' && current <= '9') {
                digits.append(current);
            }
        }
        return digits.toString();
    }

    private static boolean isFormattingCode(char code) {
        return (code >= '0' && code <= '9')
                || (code >= 'a' && code <= 'f')
                || (code >= 'A' && code <= 'F')
                || (code >= 'k' && code <= 'o')
                || (code >= 'K' && code <= 'O')
                || code == 'r'
                || code == 'R';
    }
}
