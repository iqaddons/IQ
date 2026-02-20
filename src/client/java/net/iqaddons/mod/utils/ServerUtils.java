package net.iqaddons.mod.utils;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ServerUtils {

    private static volatile long previousUpdateMillis;

    @Getter
    private static volatile float averageTps = 20.0f;

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

    public static void onWorldTimeUpdate() {
        long now = System.currentTimeMillis();
        long previous = previousUpdateMillis;

        if (previous != 0L) {
            float tps = 20_000f / (now - previous + 1L);
            averageTps = Math.clamp(tps, 0.0f, 20.0f);
        }

        previousUpdateMillis = now;
    }
}
