package net.iqaddons.mod.features.widgets;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.hud.component.HudLine;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.manager.KuudraProfitTrackerManager;
import net.iqaddons.mod.model.profit.ProfitData;
import net.iqaddons.mod.model.profit.ProfitScope;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.TimeUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
public class KuudraProfitTrackerWidget extends HudWidget {

    private static final Set<String> ALLOWED_AREAS = Set.of(
            "Dungeon Hub",
            "Forgotten Skull",
            "Kuudra's Hollow"
    );

    private final KuudraProfitTrackerManager tracker = KuudraProfitTrackerManager.get();

    private final HudLine title = HudLine.of("§e§lProfit Tracker §7(SESSION)");
    private final HudLine netProfit = HudLine.of("§fProfit: §a+0");
    private final HudLine runs = HudLine.of("§fRuns: §a0");
    private final HudLine chests = HudLine.of("§fChests: §e0");
    private final HudLine rerolls = HudLine.of("§fRerolls: §b0§7/§d0");
    private final HudLine avg = HudLine.of("§fAvg Time: §b0.00s");
    private final HudLine totalTime = HudLine.of("§fTotal Time: §b0.00s");
    private final HudLine rate = HudLine.of("§fRate: §a0/hr");

    public KuudraProfitTrackerWidget() {
        super("kuudraProfitTrackerWidget",
                "Profit Tracker",
                10.0f, 50.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> KuudraGeneralConfig.kuudraProfitTracker);
        setVisibilityCondition(() -> ALLOWED_AREAS.contains(ScoreboardUtils.getArea()));

        setExampleLines(List.of(
                HudLine.of("§e§lProfit Tracker §7(SESSION)"),
                HudLine.of("§fProfit: §a+12.5m"),
                HudLine.of("§fRuns: §a15 §7(§c1 failed§7)"),
                HudLine.of("§fChests: §e9 §7(§69 paid§7/§a0 free§7)"),
                HudLine.of("§fRerolls: §b4§7/§d1"),
                HudLine.of("§fAvg Time: §258.33s"),
                HudLine.of("§fTotal Time: §b14m35s"),
                HudLine.of("§fRate: §a12.8m/hr")
        ));
    }

    @Override
    protected void onActivate() {
        clearLines();
        addLines(title, netProfit, runs, chests, rerolls, avg, totalTime, rate);

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
        title.text("§6§lProfit Tracker §7(" + scope.name() + ")");

        String sign = data.profit >= 0 ? "§a+" : "§c-";
        netProfit.text("§fProfit: " + sign + formatCoins(Math.abs(data.profit)));

        runs.text("§fRuns: §a" + data.runs + " §7(§c" + data.failedRuns + " failed§7)");
        chests.text("§fChests: §e" + data.chestsOpened + " §7(§6" + data.paidChests + " paid§7/§a" + data.freeChests + " free§7)");
        rerolls.text("§fRerolls: §b" + data.rerolls + "§7/§d" + data.shardRerolls + " §7(§c-" + formatCoins(data.rerollCostCoins) + "§7)");
        var avgTimeSeconds = data.averageRunMillis() / 1000.0;
        avg.text("§fAvg Time: §b" + getAverageTimeColor(avgTimeSeconds) + TimeUtils.formatTime(avgTimeSeconds));
        totalTime.text("§fTime: §b" + TimeUtils.formatTime(data.totalRunMillis / 1000.0));
        rate.text("§fRate: §a" + formatCoins(Math.max(0, data.hourlyRateCoins())) + "/hr");

        markDimensionsDirty();
    }

    private String formatCoins(long coins) {
        if (coins >= 1_000_000_000L) return String.format(Locale.ROOT, "%.2fb", coins / 1_000_000_000d);
        if (coins >= 1_000_000L) return String.format(Locale.ROOT, "%.2fm", coins / 1_000_000d);
        if (coins >= 1_000L) return String.format(Locale.ROOT, "%.1fk", coins / 1_000d);
        return String.valueOf(coins);
    }

    public String getAverageTimeColor(double avgTimeSeconds) {
        if (avgTimeSeconds <= 70) return "§2";
        if (avgTimeSeconds <= 75) return "§a";
        if (avgTimeSeconds <= 100) return "§c";
        return "§4";
    }
}
