package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for converting Minecraft Text components to legacy format strings.
 * This is needed because Text.getString() strips all formatting information.
 */
@UtilityClass
public class TextFormatUtil {

    /**
     * Converts a Text component to a string with legacy formatting codes (§).
     * This preserves colors and formatting that would otherwise be lost.
     */
    public static @NotNull String toLegacyString(@NotNull Text text) {
        StringBuilder result = new StringBuilder();
        appendTextWithFormatting(text, result);
        return result.toString();
    }

    private static void appendTextWithFormatting(@NotNull Text text, @NotNull StringBuilder builder) {
        Style style = text.getStyle();
        String formatCodes = styleToLegacyCodes(style);
        if (!formatCodes.isEmpty()) {
            builder.append(formatCodes);
        }
        
        String content = text.copyContentOnly().getString();
        builder.append(content);
        
        for (Text sibling : text.getSiblings()) {
            appendTextWithFormatting(sibling, builder);
        }
    }

    private static @NotNull String styleToLegacyCodes(@NotNull Style style) {
        StringBuilder codes = new StringBuilder();
        if (style.getColor() != null) {
            String colorCode = colorToLegacyCode(style.getColor().getName());
            if (colorCode != null) {
                codes.append(colorCode);
            }
        }
        
        if (style.isBold()) codes.append("§l");
        if (style.isItalic()) codes.append("§o");
        if (style.isUnderlined()) codes.append("§n");
        if (style.isStrikethrough()) codes.append("§m");
        if (style.isObfuscated()) codes.append("§k");
        
        return codes.toString();
    }

    @Contract(pure = true)
    private static @Nullable String colorToLegacyCode(@NotNull String colorName) {
        return switch (colorName.toLowerCase()) {
            case "black" -> "§0";
            case "dark_blue" -> "§1";
            case "dark_green" -> "§2";
            case "dark_aqua" -> "§3";
            case "dark_red" -> "§4";
            case "dark_purple" -> "§5";
            case "gold" -> "§6";
            case "gray" -> "§7";
            case "dark_gray" -> "§8";
            case "blue" -> "§9";
            case "green" -> "§a";
            case "aqua" -> "§b";
            case "red" -> "§c";
            case "light_purple" -> "§d";
            case "yellow" -> "§e";
            case "white" -> "§f";
            default -> null;
        };
    }

    /**
     * Extracts a player name with formatting from a chat message.
     * Looks for the player name before "recovered" and preserves color codes.
     */
    public static @NotNull String extractPlayerNameFormatted(@NotNull Text text, @NotNull String plainPlayerName) {
        StringBuilder result = new StringBuilder();
        extractPlayerNameFromText(text, plainPlayerName, result, new StringBuilder());
        
        String extracted = result.toString().trim();
        return extracted.isEmpty() ? plainPlayerName : extracted;
    }

    private static boolean extractPlayerNameFromText(
            @NotNull Text text,
            @NotNull String targetName,
            @NotNull StringBuilder result,
            @NotNull StringBuilder currentPath
    ) {
        Style style = text.getStyle();
        String formatCodes = styleToLegacyCodes(style);
        String content = text.copyContentOnly().getString();
        
        currentPath.append(formatCodes).append(content);
        if (currentPath.toString().contains(targetName)) {
            String path = currentPath.toString();
            int nameEnd = path.indexOf(targetName) + targetName.length();

            result.append(path, 0, nameEnd);
            return true;
        }
        
        for (Text sibling : text.getSiblings()) {
            if (extractPlayerNameFromText(sibling, targetName, result, currentPath)) {
                return true;
            }
        }
        
        return false;
    }
}