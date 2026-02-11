package net.iqaddons.mod.manager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public final class KuudraPriceCacheManager {

    private static final KuudraPriceCacheManager INSTANCE = new KuudraPriceCacheManager();
    private static final long TTL_MS = 15 * 60 * 1000L;

    private static final Set<String> BAZAAR_IDS = Set.of(
            "KUUDRA_TEETH", "MANDRAA", "KUUDRA_MANDIBLE", "ESSENCE_CRIMSON",
            "KISMET_FEATHER", "HEAVY_PEARL", "TOXIC_ARROW_POISON", "TWILIGHT_ARROW_POISON",
            "KUUDRA_TENTACLE"
    );

    private static final Map<String, String> KEY_ITEM_IDS = Map.of(
            "KUUDRA_KEY", "KUUDRA_KEY",
            "HOT", "HOT_KUUDRA_KEY",
            "BURNING", "BURNING_KUUDRA_KEY",
            "FIERY", "FIERY_KUUDRA_KEY",
            "INFERNAL", "INFERNAL_KUUDRA_KEY"
    );


    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "iq-kuudra-price-cache");
        t.setDaemon(true);
        return t;
    });

    private final Map<String, Long> bazaarSellPrices = new ConcurrentHashMap<>();
    private final Map<String, Long> lowestBinPrices = new ConcurrentHashMap<>();

    private final AtomicBoolean refreshInFlight = new AtomicBoolean(false);
    private volatile long lastRefreshMs;

    public static @NotNull KuudraPriceCacheManager get() {
        return INSTANCE;
    }

    public void refreshAsyncIfStale() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshMs <= TTL_MS) return;
        if (!refreshInFlight.compareAndSet(false, true)) {
            return;
        }

        executor.execute(() -> {
            try {
                refreshBazaar();
                refreshLowestBin();
                lastRefreshMs = System.currentTimeMillis();
            } catch (Exception e) {
                log.warn("Failed refreshing Kuudra price cache", e);
            } finally {
                refreshInFlight.set(false);
            }
        });
    }

    public long getItemPrice(@NotNull String itemId) {
        if (BAZAAR_IDS.contains(itemId)) {
            return bazaarSellPrices.getOrDefault(itemId, 0L);
        }

        return lowestBinPrices.getOrDefault(itemId, 0L);
    }

    public long getKismetPrice() {
        return getItemPrice("KISMET_FEATHER");
    }

    public long getWheelOfFatePrice() {
        return getItemPrice("WHEEL_OF_FATE");
    }

    public long getKeyPrice(@NotNull String keyTier) {
        String itemId = KEY_ITEM_IDS.getOrDefault(keyTier.toUpperCase(), keyTier.toUpperCase());

        long bz = bazaarSellPrices.getOrDefault(itemId, 0L);
        if (bz > 0) {
            return bz;
        }

        return lowestBinPrices.getOrDefault(itemId, 0L);
    }

    private void refreshBazaar() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.hypixel.net/v2/skyblock/bazaar"))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Bazaar API returned status " + response.statusCode());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonObject products = root.getAsJsonObject("products");
        if (products == null) return;

        for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
            JsonObject product = entry.getValue().getAsJsonObject();
            JsonObject quickStatus = product.getAsJsonObject("quick_status");
            if (quickStatus == null || !quickStatus.has("sellPrice")) {
                continue;
            }

            long sellPrice = Math.round(quickStatus.get("sellPrice").getAsDouble());
            bazaarSellPrices.put(entry.getKey(), Math.max(0L, sellPrice));
        }
    }

    private void refreshLowestBin() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://moulberry.codes/lowestbin.json"))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Lowest BIN API returned status " + response.statusCode());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            long value = Math.max(0L, Math.round(entry.getValue().getAsDouble()));
            lowestBinPrices.put(entry.getKey(), value);
        }
    }
}
