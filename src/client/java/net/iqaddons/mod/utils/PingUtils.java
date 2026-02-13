package net.iqaddons.mod.utils;

import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PingUtils {

    public static Duration getAveragePing() {
        List<Long> previousPings = getPreviousPings();
        if (previousPings.isEmpty()) {
            return Duration.ZERO;
        }

        double average = previousPings.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        return Duration.ofMillis((long) average);
    }

    public static @NotNull List<Long> getPreviousPings() {
        var pingLogger = MinecraftClient.getInstance().getDebugHud().getPingLog();

        List<Long> list = new ArrayList<>();
        for (int i = 0; i < pingLogger.getLength(); i++) {
            list.add(pingLogger.get(i));
        }
        return list;
    }

}
