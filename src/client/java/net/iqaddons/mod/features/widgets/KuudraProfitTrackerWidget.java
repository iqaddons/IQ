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
    private final HudLine runs = HudLine.of("§fRuns: §70 §8(§c0F§8)");
    private final HudLine chests = HudLine.of("§fChests: §f0 §8(§e0P§8/§a0F§8)");
    private final HudLine rerolls = HudLine.of("§fRerolls: §b0/0 §8(§c-0§8)");
    private final HudLine avg = HudLine.of("§fAvg Time: §b0.00s");
    private final HudLine totalTime = HudLine.of("§fTime: §b0s");
    private final HudLine rate = HudLine.of("§fRate: §70/h");


    public KuudraProfitTrackerWidget() {
        super("kuudraProfitTrackerWidget",
                "Profit Tracker",
                10.0f, 50.0f,
                1.0f,
                HudAnchor.TOP_LEFT
        );

        setEnabledSupplier(() -> KuudraGeneralConfig.kuudraProfitTracker);
        setVisibilityCondition(() -> {
            if (KuudraGeneralConfig.profitTrackerVisibility == KuudraGeneralConfig.ProfitTrackerVisibility.ALWAYS) {
                return true;
            }

            String area = ScoreboardUtils.getArea();
            return ALLOWED_AREAS.stream().anyMatch(area::startsWith);
        });

        setExampleLines(List.of(
                HudLine.of("§e§lProfit Tracker §7(SESSION)"),
                HudLine.of("§fProfit: §a+12.5m"),
                HudLine.of("§fRuns: §715 §8(§c1F§8)"),
                HudLine.of("§fChests: §f9 §8(§e9P§8/§a0F§8)"),
                HudLine.of("§fRerolls: §b4/1 §8(§c-500k§8)"),
                HudLine.of("§fAvg Time: §a58.33s"),
                HudLine.of("§fTime: §b14m35s"),
                HudLine.of("§fRate: §712.8m/h")
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

        title.text("§e§lProfit Tracker §7(" + scope.name() + ")");

        String sign = data.profit >= 0 ? "§a+" : "§c-";
        netProfit.text("§fProfit: " + sign + formatCoins(Math.abs(data.profit)));

        runs.text(String.format("§fRuns: §7%s §8(§c%sF§8)",
                data.completionRuns(), data.failedRuns)
        );

        chests.text(String.format("§fChests: §f%s §8(§e%sP§8/§a%sF§8)",
                data.chestsOpened, data.paidChests, data.freeChests)
        );

        rerolls.text(String.format("§fRerolls: §b%sC/%sS §8(§c-%s§8)",
                data.rerolls, data.shardRerolls, formatCoins(data.rerollCostCoins))
        );

        var avgTimeSeconds = data.averageRunMillis() / 1000.0;
        avg.text(String.format("§fAvg Time: %s%s",
                getAverageTimeColor(avgTimeSeconds), TimeUtils.formatTime(avgTimeSeconds))
        );

        totalTime.text("§fTime: §b" + TimeUtils.formatTime(data.totalRunMillis));
        rate.text("§fRate: §7" + formatCoins(Math.max(0, data.hourlyRateCoins())) + "/h");

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
