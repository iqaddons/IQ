package net.iqaddons.mod.features.kuudra.tracker;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.skyblock.KuudraRunEndEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.KuudraPriceCacheManager;
import net.iqaddons.mod.manager.KuudraProfitTrackerManager;
import org.jetbrains.annotations.NotNull;


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
    }

    private void onKuudraRunEnd(@NotNull KuudraRunEndEvent event) {
        manager.onRunEnd(event.totalDuration().toMillis(), !event.completed());
    }
}
