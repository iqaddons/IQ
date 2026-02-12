package net.iqaddons.mod.model.profit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public final class ProfitData {

    public long runs;
    public long failedRuns;
    public long totalRunMillis;

    public long chestsOpened;
    public long paidChests;
    public long freeChests;

    public long rerolls;
    public long shardRerolls;

    public long grossCoins;
    public long profit;
    public long keyCostCoins;
    public long rerollCostCoins;

    public long pricedItems;
    public long essence;

    public long averageRunMillis() {
        if (runs <= 0) {
            return 0;
        }

        return totalRunMillis / runs;
    }

    public long completionRuns() {
        return Math.max(0L, runs - failedRuns);
    }

    public long hourlyRateCoins() {
        if (totalRunMillis <= 0) {
            return 0L;
        }

        return (long) (profit * (3600_000d / totalRunMillis));
    }

    @NotNull
    public ProfitData copy() {
        return new ProfitData(
                runs, failedRuns, totalRunMillis, chestsOpened, paidChests, freeChests,
                rerolls, shardRerolls, grossCoins, profit, keyCostCoins,
                rerollCostCoins, pricedItems, essence
        );
    }
}