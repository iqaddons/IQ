package net.iqaddons.mod.manager;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.model.profit.ChestData;
import net.iqaddons.mod.model.profit.ChestType;
import net.iqaddons.mod.model.profit.ProfitData;
import net.iqaddons.mod.model.profit.ProfitScope;
import net.iqaddons.mod.utils.data.DataKey;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;

@Slf4j
public final class KuudraProfitTrackerManager {

    private static final DataKey<PersistentKuudraProfit> PROFIT_KEY = DataKey.of("kuudraProfit", PersistentKuudraProfit.class);
    private static final KuudraProfitTrackerManager INSTANCE = new KuudraProfitTrackerManager();

    private final IQPersistentDataStore store = IQPersistentDataStore.get();

    private volatile ProfitData lifetime = new ProfitData();
    private volatile ProfitData session = new ProfitData();
    private volatile ProfitScope currentScope = ProfitScope.SESSION;

    private KuudraProfitTrackerManager() {
        PersistentKuudraProfit persisted = store.getOrDefault(PROFIT_KEY, new PersistentKuudraProfit());
        if (hasData(persisted) || Files.exists(IQPersistentDataStore.DATA_FILE) && store.has(PROFIT_KEY)) {
            lifetime = persisted.lifetime;
            session = persisted.session;
            currentScope = persisted.scope == null
                    ? ProfitScope.SESSION
                    : persisted.scope;
        }
    }

    public static @NotNull KuudraProfitTrackerManager get() {
        return INSTANCE;
    }

    public synchronized void onRunEnd(long runMillis, boolean failed) {
        long safeRunMillis = Math.max(0L, runMillis);

        updateRun(lifetime, safeRunMillis, failed);
        updateRun(session, safeRunMillis, failed);

        save();
    }

    public synchronized void onChestBought(ChestData chest) {
        updateChest(lifetime, chest);
        updateChest(session, chest);
        save();
    }

    public synchronized void onReroll(boolean shard, long rerollCost) {
        updateReroll(lifetime, shard, rerollCost);
        updateReroll(session, shard, rerollCost);
        save();
    }

    public synchronized void resetSession() {
        session = new ProfitData();
        save();
    }

    public synchronized void resetLifetime() {
        lifetime = new ProfitData();
        save();
    }

    public synchronized void resetAll() {
        lifetime = new ProfitData();
        session = new ProfitData();
        save();
    }

    public @NotNull ProfitData lifetime() {
        return lifetime.copy();
    }

    public @NotNull ProfitData session() {
        return session.copy();
    }

    public @NotNull ProfitScope scope() {
        return currentScope;
    }

    public synchronized void setScope(@NotNull ProfitScope scope) {
        this.currentScope = scope;
        save();
    }

    public synchronized @NotNull ProfitScope toggleScope() {
        currentScope = (currentScope == ProfitScope.SESSION) ? ProfitScope.LIFETIME : ProfitScope.SESSION;
        save();
        return currentScope;
    }

    private void updateRun(@NotNull ProfitData data, long runMillis, boolean failed) {
        data.runs++;
        if (failed) data.failedRuns++;
        if (runMillis > 0) data.totalRunMillis += runMillis;

    }

    private void updateChest(@NotNull ProfitData data, @NotNull ChestData record) {
        data.chestsOpened++;
        if (record.type() == ChestType.PAID) {
            data.paidChests++;
        } else {
            data.freeChests++;
        }

        data.grossCoins += record.grossValue();
        data.profit += record.netProfit();
        data.keyCostCoins += record.keyCost();
        data.pricedItems += Math.max(0, record.pricedItems());
        data.essence += Math.max(0, record.essence());
        data.teeth += Math.max(0, record.teeth());
    }

    private void updateReroll(@NotNull ProfitData data, boolean shard, long rerollCost) {
        if (shard) {
            data.shardRerolls++;
        } else {
            data.rerolls++;
        }

        data.rerollCostCoins += Math.max(0L, rerollCost);
        data.profit -= Math.max(0L, rerollCost);
    }

    private boolean hasData(@NotNull PersistentKuudraProfit persisted) {
        return persisted.lifetime.runs > 0 || persisted.session.runs > 0
                || persisted.lifetime.profit != 0 || persisted.session.profit != 0;
    }

    private synchronized void save() {
        store.set(PROFIT_KEY, new PersistentKuudraProfit(lifetime, session, currentScope));
    }

    @AllArgsConstructor
    private static final class PersistentKuudraProfit {

        public ProfitData lifetime;
        public ProfitData session;
        public ProfitScope scope;

        public PersistentKuudraProfit() {
            this.lifetime = new ProfitData();
            this.session = new ProfitData();
            this.scope = ProfitScope.SESSION;
        }
    }
}
