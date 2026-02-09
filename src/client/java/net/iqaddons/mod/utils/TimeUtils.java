package net.iqaddons.mod.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class TimeUtils {

    public Long convertToMillis(@NotNull String string) {
        long time = 0;

        for (String split : string.split(" ")) {
            char charAt = split.charAt(split.length() - 1);

            int timeInt;
            try {
                timeInt = Integer.parseInt(split.replace(Character.toString(charAt), ""));
            } catch (NumberFormatException exception) {
                return (long) -1;
            }

            for (TimeMultiplier timeMultiplier : TimeMultiplier.values()) {
                if (timeMultiplier.getDiminutive() != charAt) continue;

                time += timeMultiplier.getMultiplier() * timeInt;
            }
        }

        return time;
    }

    public String formatDate(long time) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.ROOT);
        return simpleDateFormat.format(new Date(time));
    }

    public String formatTime(long time) {
        return formatTime(time, false);
    }

    public String formatTime(@NotNull Duration duration) {
        return formatTime(duration.toMillis());
    }

    public String formatTime(double seconds) {
        if (seconds <= 0) return "0s";

        long totalSeconds = (long) seconds;
        long minutes = totalSeconds / 60;
        long secs = totalSeconds % 60;

        if (minutes > 0) {
            return secs > 0 ? minutes + "m" + secs + "s" : minutes + "m";
        } else {
            return String.format(Locale.ROOT, "%.2fs", seconds);
        }
    }

    public String formatTime(long time, boolean compareDifference) {
        if (time <= 0) return "n/a";

        long realTime = (compareDifference ? System.currentTimeMillis() - time : time);
        if (realTime <= 0L) return "nunca";

        long day = TimeUnit.MILLISECONDS.toDays(realTime);
        long hours = TimeUnit.MILLISECONDS.toHours(realTime) - day * 24L;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(realTime) - TimeUnit.MILLISECONDS.toHours(realTime) * 60L;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(realTime) - TimeUnit.MILLISECONDS.toMinutes(realTime) * 60L;

        StringBuilder builder = new StringBuilder();
        if (day > 0L) builder.append(day).append("d ");
        if (hours > 0L) builder.append(hours).append("h ");
        if (minutes > 0L) builder.append(minutes).append("m ");
        if (seconds > 0L) builder.append(seconds).append("s");

        String build = builder.toString();
        return (build.isEmpty() ? "agora" : build);
    }

    @Getter
    @AllArgsConstructor
    private enum TimeMultiplier {
        SECONDS(1000, 's'),
        MINUTES(60 * 1000, 'm'),
        HOURS(60 * 60 * 1000, 'h'),
        DAYS(24 * 60 * 60 * 1000, 'd'),
        WEEKS(7 * 24 * 60 * 60 * 1000, 'w'),
        YEARS((long) 365 * 24 * 60 * 60 * 1000, 'y');

        private final long multiplier;
        private final char diminutive;
    }
}