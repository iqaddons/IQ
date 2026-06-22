package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.pricing.KuudraProfitTrackerManager;
import net.iqaddons.mod.model.profit.ProfitData;
import net.iqaddons.mod.model.profit.ProfitScope;
import net.iqaddons.mod.utils.HudRenderer;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.TimeUtils;
import net.minecraft.client.gui.screens.ChatScreen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
public class KuudraProfitTrackerWidget extends HudWidget {

    private static final long RESET_CONFIRM_WINDOW_MS = 3500L;

    private static final Set<String> ALLOWED_AREAS = Set.of(
            "Dungeon Hub",
            "Forgotten Skull",
            "Kuudra's Hollow"
    );

    private final KuudraProfitTrackerManager tracker = KuudraProfitTrackerManager.get();

    private final HudLine title = HudLine.of("§b§lProfit Tracker");
    private final HudLine netProfit = HudLine.of("§fProfit: §a+0");
    private final HudLine runs = HudLine.of("§fRuns: §70 §8(§a0C§8/§c0F§8)");
    private final HudLine chests = HudLine.of("§fChests: §f0 §8(§e0P§8/§a0F§8)");
    private final HudLine rerolls = HudLine.of("§fRerolls: §b0/0 §8(§c-0§8)");
    private final HudLine avg = HudLine.of("§fAvg Time: §b0.00s");
    private final HudLine totalTime = HudLine.of("§fTime: §b0s");
    private final HudLine rate = HudLine.of("§fRate: §70/h");

    // Normal mode (no chat open)
    private final HudLine trackingSummary = HudLine.of("§fTracking: §aSession");

    // Chat mode lines
    private final HudLine trackingChatHeader = HudLine.of("§7Tracking: §8(Choose)");
    private final HudLine sessionOption      = HudLine.inline("§a[Session]");
    private final HudLine trackingSpacer     = HudLine.inline(" ");
    private final HudLine lifetimeOption     = HudLine.of("§7[Lifetime]");
    private final HudLine resetButton        = HudLine.of("§eReset Session!");

    private boolean hoveringSessionOption  = false;
    private boolean hoveringLifetimeOption = false;
    private boolean hoveringResetButton    = false;

    private @Nullable ProfitScope pendingResetScope = null;
    private long pendingResetExpiresAt = 0L;

    public KuudraProfitTrackerWidget() {
        super("kuudraProfitTrackerWidget",
                "Profit Tracker",
                815.0f, 30.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> KuudraGeneralConfig.kuudraProfitTracker);
        setVisibilityCondition(() -> {
            if (KuudraGeneralConfig.ProfitTrackerConfig.profitTrackerVisibility == KuudraGeneralConfig.ProfitTrackerVisibility.ALWAYS) {
                return true;
            }
            String area = ScoreboardUtils.getArea();
            return ALLOWED_AREAS.stream().anyMatch(area::startsWith);
        });

        // [Session] button
        sessionOption
                .onClick(() -> selectScope(ProfitScope.SESSION))
                .onHover((context, textRenderer) -> {
                    double[] mouse = HudRenderer.getScaledMousePosition();
                    HudRenderer.drawSimpleTooltip(context,
                            "Tracks stats for the current session.\nResets after 5 min of inactivity.",
                            mouse[0], mouse[1]);
                })
                .onMouseEnter(() -> { hoveringSessionOption = true;  updateTrackingSelector(); })
                .onMouseLeave(() -> { hoveringSessionOption = false; updateTrackingSelector(); })
                .showWhen(this::isChatSelectionOpen);

        // [Lifetime] button
        lifetimeOption
                .onClick(() -> selectScope(ProfitScope.LIFETIME))
                .onHover((context, textRenderer) -> {
                    double[] mouse = HudRenderer.getScaledMousePosition();
                    HudRenderer.drawSimpleTooltip(context,
                            "Tracks cumulative stats across all sessions.",
                            mouse[0], mouse[1]);
                })
                .onMouseEnter(() -> { hoveringLifetimeOption = true;  updateTrackingSelector(); })
                .onMouseLeave(() -> { hoveringLifetimeOption = false; updateTrackingSelector(); })
                .showWhen(this::isChatSelectionOpen);

        // Reset button
        resetButton
                .onClick(this::handleResetClick)
                .onHover((context, textRenderer) -> {
                    double[] mouse = HudRenderer.getScaledMousePosition();
                    ProfitScope currentScope = tracker.scope();
                    String tip;
                    if (isResetConfirmationPending(currentScope)) {
                        tip = "Click again to confirm reset.";
                    } else {
                        tip = currentScope == ProfitScope.SESSION
                                ? "Clears all session profit data."
                                : "Clears all lifetime profit data.\nThis cannot be undone!";
                    }
                    HudRenderer.drawSimpleTooltip(context, tip, mouse[0], mouse[1]);
                })
                .onMouseEnter(() -> { hoveringResetButton = true;  updateTrackingSelector(); })
                .onMouseLeave(() -> { hoveringResetButton = false; updateTrackingSelector(); })
                .showWhen(this::isChatSelectionOpen);

        trackingChatHeader.showWhen(this::isChatSelectionOpen);
        trackingSpacer.showWhen(this::isChatSelectionOpen);
        trackingSummary.showWhen(() -> !isChatSelectionOpen());

        setExampleLines(List.of(
                HudLine.of("§b§lProfit Tracker"),
                HudLine.of("§fProfit: §a+12.5m"),
                HudLine.of("§fRuns: §715 §8(§a14C§8/§c1F§8)"),
                HudLine.of("§fChests: §f9 §8(§e9P§8/§a0F§8)"),
                HudLine.of("§fRerolls: §b4/1 §8(§c-500k§8)"),
                HudLine.of("§fAvg Time: §a58.33s"),
                HudLine.of("§fTime: §b14m35s"),
                HudLine.of("§fRate: §712.8m/h"),
                HudLine.of("§fTracking: §aSession")
        ));
    }

    @Override
    protected void onActivate() {
        clearLines();
        addLines(title, netProfit, runs, chests, rerolls, avg, totalTime, rate,
                trackingSummary,
                trackingChatHeader,
                sessionOption, trackingSpacer, lifetimeOption,
                resetButton);

        subscribe(ClientTickEvent.class, event -> {
            if (event.isNthTick(5)) {
                updateLines();
            }
        });

        updateLines();
    }

    private void updateLines() {
        ProfitScope scope = tracker.scope();
        ProfitData data = scope == ProfitScope.LIFETIME
                ? tracker.lifetime()
                : tracker.session();

        String titleColor = KuudraGeneralConfig.ProfitTrackerColorConfig.titleColor.code();
        String profitLabelColor = KuudraGeneralConfig.ProfitTrackerColorConfig.profitLabelColor.code();
        String runsLabelColor = KuudraGeneralConfig.ProfitTrackerColorConfig.runsLabelColor.code();
        String chestsLabelColor = KuudraGeneralConfig.ProfitTrackerColorConfig.chestsLabelColor.code();
        String rerollsLabelColor = KuudraGeneralConfig.ProfitTrackerColorConfig.rerollsLabelColor.code();
        String avgTimeLabelColor = KuudraGeneralConfig.ProfitTrackerColorConfig.avgTimeLabelColor.code();
        String timeLabelColor = KuudraGeneralConfig.ProfitTrackerColorConfig.timeLabelColor.code();
        String rateLabelColor = KuudraGeneralConfig.ProfitTrackerColorConfig.rateLabelColor.code();
        String trackingLabelColor = KuudraGeneralConfig.ProfitTrackerColorConfig.trackingLabelColor.code();

        title.text(titleColor + "§lProfit Tracker");

        String sign = data.profit >= 0 ? "§a+" : "§c-";
        netProfit.text(profitLabelColor + "Profit: " + sign + formatCoins(Math.abs(data.profit)));

        runs.text(String.format("%sRuns: §7%s §8(§a%sC§8/§c%sF§8)",
                runsLabelColor,
                data.runs, data.completionRuns(), data.failedRuns));

        chests.text(String.format("%sChests: §f%s §8(§e%sP§8/§a%sF§8)",
                chestsLabelColor,
                data.chestsOpened, data.paidChests, data.freeChests));

        rerolls.text(String.format("%sRerolls: §b%sC/%sS §8(§c-%s§8)",
                rerollsLabelColor,
                data.rerolls, data.shardRerolls, formatCoins(data.rerollCostCoins)));

        var avgTimeSeconds = data.averageRunMillis() / 1000.0;
        avg.text(String.format("%sAvg Time: %s%s",
                avgTimeLabelColor,
                getAverageTimeColor(avgTimeSeconds), TimeUtils.formatTime(avgTimeSeconds)));

        totalTime.text(timeLabelColor + "Time: §b" + TimeUtils.formatTime(data.totalRunMillis));
        long hourlyRateCoins = Math.max(0, data.hourlyRateCoins());
        rate.text(rateLabelColor + "Rate: " + getRateColor(hourlyRateCoins) + formatCoins(hourlyRateCoins) + "/h");
        trackingSummary.text(trackingLabelColor + "Tracking: §a" + formatScopeLabel(scope));
        updateTrackingSelector();

        markDimensionsDirty();
    }

    private boolean isChatSelectionOpen() {
        return mc.screen instanceof ChatScreen;
    }

    private void selectScope(@NotNull ProfitScope scope) {
        if (tracker.scope() == scope) {
            updateTrackingSelector();
            return;
        }
        clearPendingReset();
        tracker.setScope(scope);
        updateLines();
    }

    private void handleResetClick() {
        ProfitScope currentScope = tracker.scope();

        if (!isResetConfirmationPending(currentScope)) {
            pendingResetScope = currentScope;
            pendingResetExpiresAt = System.currentTimeMillis() + RESET_CONFIRM_WINDOW_MS;
            updateTrackingSelector();
            return;
        }

        if (currentScope == ProfitScope.SESSION) {
            tracker.resetSession();
        } else {
            tracker.resetLifetime();
        }

        clearPendingReset();
        updateLines();
    }

    private boolean isResetConfirmationPending(@NotNull ProfitScope currentScope) {
        if (pendingResetScope != currentScope) {
            return false;
        }

        if (System.currentTimeMillis() > pendingResetExpiresAt) {
            clearPendingReset();
            return false;
        }

        return true;
    }

    private void clearPendingReset() {
        pendingResetScope = null;
        pendingResetExpiresAt = 0L;
    }

    private void updateTrackingSelector() {
        ProfitScope currentScope = tracker.scope();
        String trackingLabelColor = KuudraGeneralConfig.ProfitTrackerColorConfig.trackingLabelColor.code();

        trackingChatHeader.text(trackingLabelColor + "Tracking: §8(Choose)");

        sessionOption.text(formatScopeOption(ProfitScope.SESSION, currentScope, hoveringSessionOption, "Session"));
        lifetimeOption.text(formatScopeOption(ProfitScope.LIFETIME, currentScope, hoveringLifetimeOption, "Lifetime"));

        boolean confirmPending = isResetConfirmationPending(currentScope);
        String resetLabel = currentScope == ProfitScope.SESSION ? "Reset Session" : "Reset Lifetime";
        if (confirmPending) {
            resetLabel = "Confirm " + resetLabel;
        }

        String resetColor = confirmPending ? "§c" : (hoveringResetButton ? "§6" : "§e");
        String resetEmphasis = hoveringResetButton ? "§n" : "";
        resetButton.text(resetColor + resetEmphasis + resetLabel);
    }

    private @NotNull String formatScopeLabel(@NotNull ProfitScope scope) {
        return scope == ProfitScope.LIFETIME ? "Lifetime" : "Session";
    }

    private @NotNull String formatScopeOption(
            @NotNull ProfitScope option,
            @NotNull ProfitScope currentScope,
            boolean hovered,
            @NotNull String label
    ) {
        boolean selected = option == currentScope;
        String color = selected ? "§a" : (hovered ? "§f" : "§7");
        String emphasis = hovered ? "§n" : "";
        return color + emphasis + "[" + label + "]";
    }

    private String formatCoins(long coins) {
        if (coins >= 1_000_000_000L) return String.format(Locale.ROOT, "%.2fb", coins / 1_000_000_000d);
        if (coins >= 1_000_000L) return String.format(Locale.ROOT, "%.2fm", coins / 1_000_000d);
        if (coins >= 1_000L) return String.format(Locale.ROOT, "%.1fk", coins / 1_000d);
        return String.valueOf(coins);
    }

    public String getAverageTimeColor(double avgTimeSeconds) {
        if (avgTimeSeconds <= 50.0) return "§f";
        if (avgTimeSeconds <= 59.9) return "§5";
        if (avgTimeSeconds <= 65.0) return "§9";
        if (avgTimeSeconds <= 70.0) return "§a";
        if (avgTimeSeconds <= 75.0) return "§6";
        if (avgTimeSeconds <= 80.0) return "§c";
        return "§4";
    }

    public String getRateColor(long hourlyRateCoins) {
        if (hourlyRateCoins > 150_000_000L) return "§5";
        if (hourlyRateCoins > 100_000_000L) return "§9";
        if (hourlyRateCoins >= 80_000_000L) return "§a";
        if (hourlyRateCoins >= 60_000_000L) return "§6";
        if (hourlyRateCoins >= 40_000_000L) return "§c";
        if (hourlyRateCoins >= 10_000_000L) return "§7";
        return "§4";
    }
}
