package net.iqaddons.mod.manager.pricing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class BazaarPriceProvider implements PriceProvider {

    private static final String BAZAAR_ENDPOINT = "https://api.hypixel.net/v2/skyblock/bazaar";

    private final HttpClient httpClient;
    private final Map<String, Double> prices = new ConcurrentHashMap<>();
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public BazaarPriceProvider(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Optional<Double> getPrice(String itemId) {
        return Optional.ofNullable(prices.get(itemId));
    }

    @Override
    public void update() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BAZAAR_ENDPOINT))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Bazaar API returned status " + response.statusCode());
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject products = root.getAsJsonObject("products");
            if (products == null) {
                return;
            }

            for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
                JsonObject product = entry.getValue().getAsJsonObject();
                JsonArray sellSummary = product.getAsJsonArray("sell_summary");
                if (sellSummary == null || sellSummary.isEmpty()) {
                    continue;
                }

                JsonObject firstOrder = sellSummary.get(0).getAsJsonObject();
                if (!firstOrder.has("pricePerUnit")) {
                    continue;
                }

                double sellPrice = Math.max(0D, firstOrder.get("pricePerUnit").getAsDouble());
                prices.put(entry.getKey(), sellPrice);
            }

            ready.set(true);
        } catch (Exception e) {
            log.warn("Failed refreshing bazaar prices", e);
        }
    }

    @Override
    public boolean isReady() {
        return ready.get();
    }
}