package net.iqaddons.mod.manager.pricing.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.manager.pricing.PriceProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class AuctionPriceProvider implements PriceProvider {

    private static final String LOWEST_BIN_ENDPOINT = "https://sky.coflnet.com/api/prices/neu";
    private static final String ITEM_LOWEST_BIN_ENDPOINT = "https://sky.coflnet.com/api/auctions/tag/%s/active/bin";
    private static final long TARGETED_CACHE_MS = 60_000L;

    private final HttpClient httpClient;
    private final Map<String, Double> prices = new ConcurrentHashMap<>();
    private final Map<String, Long> targetedRefreshAt = new ConcurrentHashMap<>();
    private final java.util.Set<String> targetedRefreshesInFlight = ConcurrentHashMap.newKeySet();
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

    public void refreshItem(String itemId, Executor executor) {
        if (itemId == null || itemId.isBlank() || System.currentTimeMillis() < rateLimitedUntil.get()) {
            return;
        }

        long now = System.currentTimeMillis();
        long lastRefreshAt = targetedRefreshAt.getOrDefault(itemId, 0L);
        if (!isTargetedRefreshDue(lastRefreshAt, now) || !targetedRefreshesInFlight.add(itemId)) {
            return;
        }

        targetedRefreshAt.put(itemId, now);
        try {
            executor.execute(() -> refreshItemNow(itemId));
        } catch (RuntimeException e) {
            targetedRefreshesInFlight.remove(itemId);
            targetedRefreshAt.remove(itemId, now);
            throw e;
        }
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
                applyRateLimit(response);
                return;
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Lowest BIN API returned status " + response.statusCode());
            }

            prices.putAll(parsePrices(response.body()));

            backoffMs = 10 * 60 * 1000L; // reset backoff on success
            ready.set(true);
        } catch (Exception e) {
            log.warn("Failed refreshing lowest BIN prices", e);
        }
    }

    private void refreshItemNow(String itemId) {
        try {
            if (System.currentTimeMillis() < rateLimitedUntil.get()) {
                return;
            }

            String encodedItemId = URLEncoder.encode(itemId, StandardCharsets.UTF_8).replace("+", "%20");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ITEM_LOWEST_BIN_ENDPOINT.formatted(encodedItemId)))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                applyRateLimit(response);
                return;
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.debug("Targeted BIN lookup for {} returned status {}", itemId, response.statusCode());
                return;
            }

            parseLowestBinPrice(response.body()).ifPresent(price -> prices.put(itemId, price));
            backoffMs = 10 * 60 * 1000L;
        } catch (Exception e) {
            log.debug("Failed targeted BIN refresh for {}", itemId, e);
        } finally {
            targetedRefreshesInFlight.remove(itemId);
        }
    }

    private void applyRateLimit(HttpResponse<?> response) {
        long fallbackDelay = backoffMs;
        long retryAfterMs = response.headers()
                .firstValueAsLong("Retry-After")
                .stream()
                .map(seconds -> Math.max(1L, seconds) * 1000L)
                .findFirst()
                .orElse(fallbackDelay);
        backoffMs = Math.min(backoffMs * 2, 60 * 60 * 1000L);
        rateLimitedUntil.set(System.currentTimeMillis() + retryAfterMs);
        log.warn("Lowest BIN API rate limited (429), backing off for {} seconds", retryAfterMs / 1000L);
    }

    private static Map<String, Double> parsePrices(String payload) {
        JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
        Map<String, Double> parsedPrices = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            double value = Math.max(0D, entry.getValue().getAsDouble());
            parsedPrices.put(entry.getKey(), value);
        }
        return parsedPrices;
    }

    private static Optional<Double> parseLowestBinPrice(String payload) {
        JsonArray auctions = JsonParser.parseString(payload).getAsJsonArray();
        double lowestPrice = Double.POSITIVE_INFINITY;

        for (JsonElement element : auctions) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject auction = element.getAsJsonObject();
            if (auction.has("bin") && !auction.get("bin").getAsBoolean()) {
                continue;
            }
            if (!auction.has("startingBid")) {
                continue;
            }

            double totalPrice = auction.get("startingBid").getAsDouble();
            int count = auction.has("count") ? Math.max(1, auction.get("count").getAsInt()) : 1;
            double unitPrice = totalPrice / count;
            if (unitPrice > 0D && Double.isFinite(unitPrice)) {
                lowestPrice = Math.min(lowestPrice, unitPrice);
            }
        }

        return Double.isFinite(lowestPrice) ? Optional.of(lowestPrice) : Optional.empty();
    }

    private static boolean isTargetedRefreshDue(long lastRefreshAt, long now) {
        return lastRefreshAt <= 0L || now - lastRefreshAt >= TARGETED_CACHE_MS;
    }

    @Override
    public boolean isReady() {
        return ready.get();
    }
}
