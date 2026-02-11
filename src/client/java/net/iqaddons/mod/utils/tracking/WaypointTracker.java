package net.iqaddons.mod.utils.tracking;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.model.WaypointData;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@UtilityClass
public class WaypointTracker {

    private static final Pattern COORDS_PATTERN = Pattern.compile(
            "x:\\s*(-?\\d+),\\s*y:\\s*(-?\\d+),\\s*z:\\s*(-?\\d+)(.*)$",
            Pattern.CASE_INSENSITIVE
    );

    public Optional<WaypointData> parse(String rawMessage, Duration baseDuration) {
        Matcher matcher = COORDS_PATTERN.matcher(rawMessage);
        if (!matcher.find()) return Optional.empty();

        try {
            String header = rawMessage.substring(0, matcher.start()).trim();
            int playerSeparator = header.lastIndexOf(':');
            if (playerSeparator < 0) {
                return Optional.empty();
            }

            String playerRaw = header.substring(0, playerSeparator).trim();
            if (playerRaw.toLowerCase(Locale.ROOT).startsWith("party >")) {
                playerRaw = playerRaw.substring("party >".length()).trim();
            }

            Text playerName = parsePlayerName(playerRaw);

            double x = Double.parseDouble(matcher.group(1));
            double y = Double.parseDouble(matcher.group(2));
            double z = Double.parseDouble(matcher.group(3));
            Vec3d position = new Vec3d(x, y, z);

            String suffix = matcher.group(4).trim();
            boolean isUrgent = suffix.contains("|");

            double multiplier = isUrgent ? 0.33 : 1.0;
            Duration duration = baseDuration.multipliedBy((long) (multiplier * 1000)).dividedBy(1000);
            Instant expiresAt = Instant.now().plus(duration);

            return Optional.of(new WaypointData(playerName, position, expiresAt, isUrgent));
        } catch (NumberFormatException e) {
            log.error("Failed to parse coordinates from message: {}", rawMessage, e);
            return Optional.empty();
        }
    }

    private @NotNull Text parsePlayerName(@NotNull String raw) {
        String formatted = raw.replaceFirst("\\[\\d+]\\s*", "");

        return Text.literal(StringUtils.formatPlayerNick(formatted));
    }
}
