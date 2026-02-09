package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.HudRenderer;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.TimeUtils;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BuildProgressWidget extends HudWidget {

    private static final Pattern PROGRESS_PATTERN = Pattern.compile("Protect Elle\\s*\\((\\d+)%\\)");
    private static final String FRESH_MESSAGE = "Your Fresh Tools Perk bonus doubles your building speed";

    private final KuudraStateManager stateManager = KuudraStateManager.get();

    private int currentProgress = 0;
    private long phaseStartTime = 0;
    private int freshCount = 0;

    private EventBus.Subscription<PlayerFreshEvent> playerFreshSubscription;
    private EventBus.Subscription<ClientTickEvent> tickSubscription;

    private final HudLine titleLine;
    private final HudLine progressLine;
    private final HudLine freshLine;
    private final HudLine etaLine;

    public BuildProgressWidget() {
        super(
                "buildProgress",
                "Build Progress",
                10.0f, 80.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        titleLine = HudLine.of("§6§lBuild Progress");
        progressLine = HudLine.of("§fProgress: §a0%");
        freshLine = HudLine.of("§fFresh: §b0").showWhen(() -> freshCount > 0);
        etaLine = HudLine.of("§fETA: §e--");

        setEnabledSupplier(() -> PhaseTwoConfig.buildProgressOverlay);
        setVisibilityCondition(() -> stateManager.phase() == KuudraPhase.BUILD);

        setExampleLines(List.of(
                titleLine,
                HudLine.of("§fProgress: §a75%"),
                HudLine.of("§fFresh: §b3"),
                HudLine.of("§fETA: §e12s")
        ));
    }

    @Override
    protected void onActivate() {
        currentProgress = 0;
        phaseStartTime = System.currentTimeMillis();
        freshCount = 0;

        clearLines();
        addLines(
                titleLine, progressLine,
                freshLine, etaLine
        );

        subscribe(PlayerFreshEvent.class, this::onPlayerFresh);
        subscribe(ClientTickEvent.class, this::onTick);
    }

    private void onPlayerFresh(@NotNull PlayerFreshEvent event) {
        freshCount++;
        updateDisplay();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;
        if (!event.isNthTick(10)) return;

        int newProgress = getBuildProgress();
        if (newProgress >= 0 && newProgress != currentProgress) {
            currentProgress = newProgress;
            updateDisplay();
            log.debug("Build progress updated: {}%", currentProgress);
        }
    }

    private int getBuildProgress() {
        for (String line : ScoreboardUtils.getLines()) {
            String stripped = ScoreboardUtils.stripFormatting(line);

            Matcher matcher = PROGRESS_PATTERN.matcher(stripped);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse build progress: {}", stripped);
                }
            }
        }
        return -1;
    }

    private void updateDisplay() {
        String progressColor = getProgressColor(currentProgress);
        progressLine.text(String.format("§fProgress: %s%d%%", progressColor, currentProgress));
        freshLine.text(String.format("§fFresh: §b%d", freshCount));
        etaLine.text(String.format("§fETA: §e%s", calculateETA()));

        markDimensionsDirty();
    }

    private @NotNull String getProgressColor(int progress) {
        if (progress >= 80) return "§a";
        if (progress >= 60) return "§2";
        if (progress >= 40) return "§e";
        if (progress >= 20) return "§6";
        return "§c";
    }

    private @NotNull String calculateETA() {
        if (currentProgress <= 0 || phaseStartTime <= 0) {
            return "--";
        }

        long elapsed = System.currentTimeMillis() - phaseStartTime;
        if (elapsed <= 0) return "--";

        double progressPerMs = currentProgress / (double) elapsed;
        if (progressPerMs <= 0) return "--";

        int remaining = 100 - currentProgress;
        long remainingMs = (long) (remaining / progressPerMs);

        return TimeUtils.formatTime(remainingMs);
    }

    @Override
    public void render(@NotNull DrawContext context, double mouseX, double mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (shouldRender() && isActive() && currentProgress > 0) {
            float absX = getAbsoluteX();
            float absY = getAbsoluteY();
            int height = getScaledHeight();

            int barY = (int) (absY + height + 2);
            int barWidth = Math.max(getScaledWidth(), 100);

            HudRenderer.drawProgressBarAuto(
                    context,
                    (int) absX, barY,
                    barWidth, 6,
                    currentProgress / 100.0f
            );
        }
    }
}