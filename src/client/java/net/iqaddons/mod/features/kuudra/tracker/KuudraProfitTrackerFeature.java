package net.iqaddons.mod.features.kuudra.tracker;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.skyblock.KuudraChestOpenEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraChestRerollEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.KuudraPriceCacheManager;
import net.iqaddons.mod.manager.KuudraProfitTrackerManager;
import net.iqaddons.mod.model.profit.ChestData;
import net.iqaddons.mod.model.profit.ChestType;
import net.iqaddons.mod.utils.ChestProfitUtil;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class KuudraProfitTrackerFeature extends Feature {

    private final KuudraProfitTrackerManager manager = KuudraProfitTrackerManager.get();
    private final KuudraPriceCacheManager priceCache = KuudraPriceCacheManager.get();

    public KuudraProfitTrackerFeature() {
        super("kuudraProfitTracker", "Kuudra Profit Tracker",
                () -> KuudraGeneralConfig.kuudraProfitTracker
        );
    }

    @Override
    protected void onActivate() {
        priceCache.refreshAsyncIfStale();

        subscribe(KuudraRunEndEvent.class, this::onKuudraRunEnd);
        subscribe(KuudraChestOpenEvent.class, this::onKuudraChestOpen);
        subscribe(KuudraChestRerollEvent.class, this::onKuudraChestReroll);
    }

    private void onKuudraRunEnd(@NotNull KuudraRunEndEvent event) {
        manager.onRunEnd(event.totalDuration().toMillis(), !event.isCompleted());
    }

    private void onKuudraChestOpen(@NotNull KuudraChestOpenEvent event) {
        if (event.chestType() == ChestType.UNKNOWN) return;

        priceCache.refreshAsyncIfStale();

        ChestData parsed = ChestProfitUtil.parseChest(event.slots(), priceCache, event.chestType());
        manager.onChestBought(parsed);
    }

    private void onKuudraChestReroll(@NotNull KuudraChestRerollEvent event) {
        priceCache.refreshAsyncIfStale();

        if (event.rerollType() == KuudraChestRerollEvent.RerollType.ITEMS) {
            manager.onReroll(false, priceCache.getKismetPrice());
        } else if (event.rerollType() == KuudraChestRerollEvent.RerollType.SHARD) {
            manager.onReroll(true, priceCache.getWheelOfFatePrice());
        }
    }
}
