package net.iqaddons.mod.manager.pricing;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.manager.pricing.impl.AuctionPriceProvider;
import net.iqaddons.mod.manager.pricing.impl.BazaarPriceProvider;
import net.iqaddons.mod.model.profit.chest.type.ChestKeyType;
import org.jetbrains.annotations.NotNull;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private @NotNull Optional<Double> getPrice(String itemId) {
        for (PriceProvider provider : providers) {
            Optional<Double> price = provider.getPrice(itemId);
            if (price.isPresent()) {
                return price;
            }
        }

        return Optional.empty();
    }

    public double calculateKeyPrice(ChestKeyType key) {
        if (key == ChestKeyType.FREE || key == ChestKeyType.UNKNOWN) {
            log.debug("Attempted to calculate price for key type {}. Returning 0.", key);
            return 0D;
        }

        double netherStar = getPrice("NETHER_STAR").orElse(0D);
        double factionMaterial = getPrice(KuudraGeneralConfig.ProfitTrackerConfig.crimsonIsleFaction.getMaterialId()).orElse(0D);

        return key.getBaseCoinsCost() + (2 * netherStar) + (key.getMaterialAmount() * factionMaterial);
    }

    private void updateAll() {
        providers.forEach(PriceProvider::update);
    }

    public static @NotNull ItemPriceManager get() {
        return INSTANCE;
    }

    public long getKismetPrice() {
        return getItemPrice("KISMET_FEATHER");
    }

    public long getWheelOfFatePrice() {
        return getItemPrice("WHEEL_OF_FATE");
    }
}
