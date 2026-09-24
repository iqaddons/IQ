package net.iqaddons.mod.manager.pricing.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.manager.pricing.PriceProvider;

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
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class AuctionPriceProvider implements PriceProvider {

    private static final String LOWEST_BIN_ENDPOINT = "https://whatyouth.ing/api/nofrills/v2/economy/get-item-pricing/";

    private final HttpClient httpClient;
    private final Map<String, Double> prices = new ConcurrentHashMap<>();
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final AtomicLong rateLimitedUntil = new AtomicLong(0);
    private volatile long backoffMs = 10 * 60 * 1000L; // starts at 10 minutes, doubles on each 429

    public AuctionPriceProvider(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Optional<Double> getPrice(String itemId) {
        return Optional.ofNullable(prices.get(itemId));
    }

    @Override
    public void update() {
        if (System.currentTimeMillis() < rateLimitedUntil.get()) {
            log.debug("Skipping BIN price update, still rate limited");
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LOWEST_BIN_ENDPOINT))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                long delay = backoffMs;
                backoffMs = Math.min(backoffMs * 2, 60 * 60 * 1000L); // cap at 1 hour
                rateLimitedUntil.set(System.currentTimeMillis() + delay);
                log.warn("Lowest BIN API rate limited (429), backing off for {} minutes", delay / 60000);
                return;
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Lowest BIN API returned status " + response.statusCode());
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject auctionData = root.getAsJsonObject("auction");
            for (Map.Entry<String, JsonElement> entry : auctionData.entrySet()) {
                double value = Math.max(0D, entry.getValue().getAsDouble());
                prices.put(entry.getKey(), value);
            }

            backoffMs = 10 * 60 * 1000L; // reset backoff on success
            ready.set(true);
        } catch (Exception e) {
            log.warn("Failed refreshing lowest BIN prices", e);
        }
    }

    @Override
    public boolean isReady() {
        return ready.get();
    }
}