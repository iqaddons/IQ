package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.EntityDetectorUtil;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BuildProgressWidget extends HudWidget {

    private static final Pattern PROGRESS_PATTERN = Pattern.compile("Building Progress:?\\s*(\\d+)%");
    private static final Pattern BUILDERS_PATTERN = Pattern.compile("\\((\\d+)\\)");

    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private int currentProgress = 0;
    private int freshCount = 0;
    private final HudLine buildersLine;

    private final HudLine titleLine;
    private final HudLine progressLine;
    private int builderCount = 0;
    private final HudLine freshLine;

    public BuildProgressWidget() {
        super(
                "buildProgress",
                "Build Progress",
                430.0f, 385.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        titleLine = HudLine.of("§6§lBuild Progress");
        progressLine = HudLine.of("§fProgress: §a0%");
        buildersLine = HudLine.of("§fBuilders: §e0").showWhen(() -> builderCount > 0);
        freshLine = HudLine.of("§fFresh: §b0").showWhen(() -> freshCount > 0);

        setEnabledSupplier(() -> PhaseTwoConfig.buildProgressOverlay);
        setVisibilityCondition(() -> stateManager.phase() == KuudraPhase.BUILD);

        setExampleLines(List.of(
                titleLine,
                HudLine.of("§fProgress: §a75%"),
                HudLine.of("§fBuilders: §b3"),
                HudLine.of("§fFresh: §b3")
        ));
    }

    @Override
    protected void onActivate() {
        currentProgress = 0;
        freshCount = 0;
        builderCount = 0;

        clearLines();
        addLines(
                titleLine, progressLine,
                buildersLine, freshLine
        );

        subscribe(PlayerFreshEvent.class, this::onPlayerFresh);
        subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onDeactivate() {
        currentProgress = 0;
        freshCount = 0;
        builderCount = 0;
    }

    private void onPlayerFresh(@NotNull PlayerFreshEvent event) {
        if (event.selfFresh()) return;

        freshCount++;
        updateDisplay();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        BuildProgressData data = getBuildProgressFromArmorStand();
        if (data == null) return;

        if (data.progress() != currentProgress || data.builders() != builderCount) {
            currentProgress = data.progress();
            builderCount = data.builders();
            updateDisplay();
        }
    }

    private @Nullable BuildProgressData getBuildProgressFromArmorStand() {
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

    private void updateDisplay() {
        String progressColor = getProgressColor(currentProgress);
        progressLine.text(String.format("§fProgress: %s%d%%", progressColor, currentProgress));
        buildersLine.text(String.format("§fBuilders: §b%d", builderCount));
        freshLine.text(String.format("§fFresh: §b%d", freshCount));

        markDimensionsDirty();
    }

    private @NotNull String getProgressColor(int progress) {
        if (progress >= 80) return "§a";
        if (progress >= 60) return "§2";
        if (progress >= 40) return "§e";
        if (progress >= 20) return "§6";
        return "§c";
    }

    private record BuildProgressData(int progress, int builders) {
    }
}