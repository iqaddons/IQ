package net.iqaddons.mod.manager.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.model.kuudra.KuudraContext;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public final class KuudraStateValidator {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final String KUUDRA_AREA_PREFIX = "Kuudra";
    private static final String SKYBLOCK_TITLE = "SKYBLOCK";

    private static final Pattern SUPPLIES_PATTERN = Pattern.compile("Rescue supplies");
    private static final Pattern BUILD_PATTERN = Pattern.compile("Protect Elle\\s*\\((\\d+)%\\)");

    public sealed interface ValidationResult permits
            ValidationResult.Valid,
            ValidationResult.AreaMismatch,
            ValidationResult.NotOnSkyBlock,
            ValidationResult.PlayerNotInWorld,
            ValidationResult.PhaseMismatch {

        boolean isValid();

        String reason();

        record Valid() implements ValidationResult {

            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public String reason() {
                return "State is valid";
            }
        }

        record AreaMismatch(String expectedArea, String actualArea) implements ValidationResult {

            @Override
            public boolean isValid() {
                return false;
            }

            @Override
            public String reason() { 
                return "Area mismatch: expected '%s', got '%s'".formatted(expectedArea, actualArea); 
            }
        }

        record NotOnSkyBlock() implements ValidationResult {

            @Override
            public boolean isValid() {
                return false;
            }

            @Override
            public String reason() {
                return "Player not on SkyBlock";
            }
        }

        record PlayerNotInWorld() implements ValidationResult {

            @Override
            public boolean isValid() {
                return false;
            }

            @Override
            public String reason() {
                return "Player not in world";
            }
        }

        record PhaseMismatch(KuudraPhase expected, KuudraPhase detected) implements ValidationResult {

            @Override
            public boolean isValid() {
                return false;
            }

            @Contract(pure = true)
            @Override
            public @NotNull String reason() {
                return "Phase mismatch: expected '%s', detected '%s'".formatted(expected, detected); 
            }
        }
    }

    public @NotNull ValidationResult validate(@NotNull KuudraContext context) {
        if (mc.player == null || mc.world == null) {
            return new ValidationResult.PlayerNotInWorld();
        }

        if (!isOnSkyBlock()) {
            return new ValidationResult.NotOnSkyBlock();
        }

        if (context.isInRun()) {
            String currentArea = getCurrentArea();
            if (!isInKuudraArea(currentArea)) {
                return new ValidationResult.AreaMismatch(KUUDRA_AREA_PREFIX, currentArea);
            }

            Optional<KuudraPhase> detectedPhase = detectPhaseFromScoreboard();
            if (detectedPhase.isPresent() && !isPhasePlausible(context.phase(), detectedPhase.get())) {
                return new ValidationResult.PhaseMismatch(context.phase(), detectedPhase.get());
            }
        }

        return new ValidationResult.Valid();
    }

    public @NotNull AreaInfo detectAreaInfo() {
        boolean onSkyBlock = isOnSkyBlock();
        String area = getCurrentArea();
        boolean inKuudra = isInKuudraArea(area);

        return new AreaInfo(onSkyBlock, inKuudra, area);
    }

    private boolean isOnSkyBlock() {
        return ScoreboardUtils.hasTitle(SKYBLOCK_TITLE);
    }

    private @NotNull String getCurrentArea() {
        return ScoreboardUtils.getArea();
    }

    private boolean isInKuudraArea(@NotNull String area) {
        return area.toLowerCase().contains(KUUDRA_AREA_PREFIX.toLowerCase());
    }

    private @NotNull Optional<KuudraPhase> detectPhaseFromScoreboard() {
        for (String line : ScoreboardUtils.getLines()) {
            String stripped = ScoreboardUtils.stripFormatting(line);
            if (SUPPLIES_PATTERN.matcher(stripped).find()) {
                return Optional.of(KuudraPhase.SUPPLIES);
            }

            Matcher buildMatcher = BUILD_PATTERN.matcher(stripped);
            if (buildMatcher.find()) {
                return Optional.of(KuudraPhase.BUILD);
            }
        }

        return Optional.empty();
    }

    private boolean isPhasePlausible(@NotNull KuudraPhase expected, @NotNull KuudraPhase detected) {
        if (expected == detected) return true;
        if (expected.previous() == detected) return true;
        if (expected.next() == detected) return true;

        return expected.isCombatPhase() && detected.isCombatPhase();
    }

    public record AreaInfo(
            boolean onSkyBlock,
            boolean inKuudra,
            @NotNull String areaName
    ) {
        public boolean canBeInRun() {
            return onSkyBlock && inKuudra;
        }
    }
}