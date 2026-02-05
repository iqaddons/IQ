package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class StringUtils {

    public @NotNull String extractFormattedPlayerName(@NotNull String formattedMessage) {
        int recoverIndex = formattedMessage.indexOf("recovered");
        if (recoverIndex > 0) {
            return formattedMessage.substring(0, recoverIndex - 1).trim();
        }

        return formattedMessage;
    }

    public @NotNull String getShortMessage(@NotNull String message) {
        if (message.length() <= 30) {
            return message;
        }

        return message.substring(0, 30) + "...";
    }
}
