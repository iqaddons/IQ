package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class BuildProgressOverlayUtil {

	private static final Pattern PROGRESS_PATTERN = Pattern.compile("Building Progress:?\\s*(\\d+)%");
	private static final Pattern BUILDERS_PATTERN = Pattern.compile("\\((\\d+)\\)");

	private static boolean lastClassicEnabled = PhaseTwoConfig.buildProgressOverlay;
	private static boolean lastSimpleEnabled = PhaseTwoConfig.simpleBuildProgressOverlay;

	private BuildProgressOverlayUtil() {
	}

	public static boolean isClassicOverlayEnabled() {
		syncOverlayModes();
		return PhaseTwoConfig.buildProgressOverlay;
	}

	public static boolean isSimpleOverlayEnabled() {
		syncOverlayModes();
		return PhaseTwoConfig.simpleBuildProgressOverlay;
	}

	public static void syncOverlayModes() {
		boolean classicEnabled = PhaseTwoConfig.buildProgressOverlay;
		boolean simpleEnabled = PhaseTwoConfig.simpleBuildProgressOverlay;

		if (classicEnabled && simpleEnabled) {
			boolean classicChanged = classicEnabled != lastClassicEnabled;
			boolean simpleChanged = simpleEnabled != lastSimpleEnabled;

			if (classicChanged && !simpleChanged) {
				PhaseTwoConfig.simpleBuildProgressOverlay = false;
			} else if (simpleChanged && !classicChanged) {
				PhaseTwoConfig.buildProgressOverlay = false;
			} else {
				// Fallback for stale config states: keep the original widget enabled.
				PhaseTwoConfig.simpleBuildProgressOverlay = false;
			}
		}

		lastClassicEnabled = PhaseTwoConfig.buildProgressOverlay;
		lastSimpleEnabled = PhaseTwoConfig.simpleBuildProgressOverlay;
	}

	public static @Nullable BuildProgressData getBuildProgressFromArmorStand() {
		for (ArmorStandEntity stand : EntityDetectorUtil.getAllArmorStands()) {
			if (!stand.hasCustomName() || stand.getCustomName() == null) continue;

			String stripped = Objects.requireNonNull(stand.getCustomName()).getString().replaceAll("§.", "");
			if (!stripped.contains("Building Progress")) continue;

			Matcher progressMatcher = PROGRESS_PATTERN.matcher(stripped);
			Matcher buildersMatcher = BUILDERS_PATTERN.matcher(stripped);

			if (!progressMatcher.find()) continue;

			try {
				int progress = Integer.parseInt(progressMatcher.group(1));
				int builders = buildersMatcher.find() ? Integer.parseInt(buildersMatcher.group(1)) : 0;
				return new BuildProgressData(progress, builders);
			} catch (NumberFormatException e) {
				log.warn("Failed to parse build progress armor stand: {}", stripped);
			}
		}

		return null;
	}

	public record BuildProgressData(int progress, int builders) {
	}
}

