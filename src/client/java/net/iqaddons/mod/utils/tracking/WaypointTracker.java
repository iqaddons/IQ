package net.iqaddons.mod.utils.tracking;

import lombok.experimental.UtilityClass;
import net.iqaddons.mod.model.WaypointData;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class WaypointTracker {

    private static final Pattern WAYPOINT_PATTERN = Pattern.compile(
            "^(.+?):\\s*x:\\s*(-?\\d+),\\s*y:\\s*(-?\\d+),\\s*z:\\s*(-?\\d+)(.*)$",
            Pattern.CASE_INSENSITIVE
    );

    public Optional<WaypointData> parse(String rawMessage, Duration baseDuration) {
        Matcher matcher = WAYPOINT_PATTERN.matcher(rawMessage);
        if (!matcher.matches()) return Optional.empty();

        try {
            String playerRaw = matcher.group(1).trim();
            Text playerName = parsePlayerName(playerRaw);

            double x = Double.parseDouble(matcher.group(2));
            double y = Double.parseDouble(matcher.group(3));
            double z = Double.parseDouble(matcher.group(4));
            Vec3d position = new Vec3d(x, y, z);

            String suffix = matcher.group(5).trim();
            boolean isUrgent = suffix.contains("|");

            double multiplier = isUrgent ? 0.33 : 1.0;
            Duration duration = baseDuration.multipliedBy((long) (multiplier * 1000)).dividedBy(1000);
            Instant expiresAt = Instant.now().plus(duration);

            return Optional.of(new WaypointData(playerName, position, expiresAt, isUrgent));

        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private @NotNull Text parsePlayerName(@NotNull String raw) {
        String formatted = raw.replace("&", "§");
        int bracketIndex = formatted.indexOf("[");
        if (bracketIndex > 0) {
            formatted = formatted.substring(bracketIndex);
        }

        return Text.literal(formatted);
    }
}
