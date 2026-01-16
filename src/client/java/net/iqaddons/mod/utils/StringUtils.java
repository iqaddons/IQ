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
}
