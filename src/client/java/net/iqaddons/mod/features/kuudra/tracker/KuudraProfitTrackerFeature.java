package net.iqaddons.mod.features.kuudra.tracker;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.KuudraPriceCacheManager;

import java.util.function.BooleanSupplier;

public class KuudraProfitTrackerFeature extends Feature {

    private final KuudraPriceCacheManager priceCache = KuudraPriceCacheManager.get();

    public KuudraProfitTrackerFeature() {
        super("kuudraProfitTracker", "Kuudra Profit Tracker",
                () -> KuudraGeneralConfig.kuudraProfitTracker
        );
    }

    @Override
    protected void onActivate() {
        priceCache.refreshAsyncIfStale();
    }
}
