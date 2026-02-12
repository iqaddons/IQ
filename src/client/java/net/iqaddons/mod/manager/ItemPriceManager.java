package net.iqaddons.mod.manager;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.manager.pricing.AuctionPriceProvider;
import net.iqaddons.mod.manager.pricing.BazaarPriceProvider;
import net.iqaddons.mod.manager.pricing.PriceProvider;
import net.iqaddons.mod.model.profit.chest.ChestKeyType;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Getter
@Slf4j
public final class ItemPriceManager {

    private static final ItemPriceManager INSTANCE = new ItemPriceManager();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "iq-price-cache");
        t.setDaemon(true);
        return t;
    });

    private final List<PriceProvider> providers;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ItemPriceManager() {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();

        this.providers = List.of(
                new BazaarPriceProvider(httpClient),
                new AuctionPriceProvider(httpClient)
        );

        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateAll();
            } catch (Exception e) {
                log.warn("Failed to update prices", e);
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    public long getItemPrice(@NotNull String itemId) {
        return Math.max(0L, Math.round(getPrice(itemId).orElse(0D)));
    }

    public long getKeyPrice(@NotNull String keyTier) {

        ChestKeyType keyType = ChestKeyType.parseKeyType(keyTier);
        return Math.max(0L, Math.round(calculateKeyPrice(keyType)));
    }

    public @NotNull Optional<Double> getPrice(String itemId) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            for (PriceProvider provider : providers) {
                Optional<Double> price = provider.getPrice(itemId);
                if (price.isPresent()) {
                    return price;
                }
            }

            return Optional.empty();
        } finally {
            readLock.unlock();
        }
    }

    private void updateAll() {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            providers.forEach(PriceProvider::update);
        } finally {
            writeLock.unlock();
        }
    }

    private double calculateKeyPrice(ChestKeyType key) {
        if (key == ChestKeyType.FREE || key == ChestKeyType.UNKNOWN) {
            return 0D;
        }

        double netherStar = getPrice("NETHER_STAR").orElse(0D);
        double factionMaterial = getPrice(KuudraGeneralConfig.crimsonIsleFaction.getMaterialId()).orElse(0D);

        return key.getBaseCoinsCost() + (2 * netherStar) + (key.getMaterialAmount() * factionMaterial);
    }

    public static @NotNull ItemPriceManager get() {
        return INSTANCE;
    }
}
