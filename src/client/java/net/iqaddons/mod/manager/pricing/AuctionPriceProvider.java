package net.iqaddons.mod.manager.pricing;

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
public class AuctionPriceProvider implements PriceProvider {

    private static final String LOWEST_BIN_ENDPOINT = "https://moulberry.codes/lowestbin.json";

    private final HttpClient httpClient;
    private final Map<String, Double> prices = new ConcurrentHashMap<>();
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public AuctionPriceProvider(HttpClient httpClient) {
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
                    .uri(URI.create(LOWEST_BIN_ENDPOINT))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Lowest BIN API returned status " + response.statusCode());
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                double value = Math.max(0D, entry.getValue().getAsDouble());
                prices.put(entry.getKey(), value);
            }

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