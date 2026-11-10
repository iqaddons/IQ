package net.iqaddons.mod.gametest;

import net.iqaddons.mod.manager.pricing.impl.AuctionPriceProvider;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/** Deterministic regression for the direct item-to-price payload returned by Coflnet. */
public final class AuctionPriceProviderTest {
    public static void main(String[] args) {
        parsesDirectCoflnetPriceMap();
        parsesLowestActiveBinPerUnit();
        targetedRefreshUsesSixtySecondCache();
        System.out.println("PASS: auction price provider regressions");
    }

    private static void parsesDirectCoflnetPriceMap() {
        String payload = """
                {
                  "BURNING_KUUDRA_CORE": 32000000,
                  "ANANKE_FEATHER": 33500000,
                  "TORMENTOR": 25000000,
                  "HELLSTORM_STAFF": 15990000
                }
                """;

        Map<String, Double> prices = invokeParser(payload);
        check(prices.get("BURNING_KUUDRA_CORE") == 32_000_000D, "Burning Kuudra Core price is missing");
        check(prices.get("ANANKE_FEATHER") == 33_500_000D, "Ananke Feather price is missing");
        check(prices.get("TORMENTOR") == 25_000_000D, "Tormentor price is missing");
        check(prices.get("HELLSTORM_STAFF") == 15_990_000D, "Hellstorm Wand price is missing");
    }

    private static void parsesLowestActiveBinPerUnit() {
        String payload = """
                [
                  {"startingBid": 640000, "count": 64, "bin": true},
                  {"startingBid": 12000, "count": 1, "bin": true}
                ]
                """;

        Optional<Double> price = invokeLowestBinParser(payload);
        check(price.isPresent(), "Lowest active BIN price is missing");
        check(price.get() == 10_000D, "Lowest active BIN must be normalized per item");
        check(invokeLowestBinParser("[]").isEmpty(), "Empty auctions must not overwrite the cached price");
    }

    private static void targetedRefreshUsesSixtySecondCache() {
        long now = 1_000_000L;
        check(invokeRefreshDue(0L, now), "An item without a targeted refresh must be refreshed");
        check(!invokeRefreshDue(now - 59_999L, now), "An item inside the 60 second cache must not refresh");
        check(invokeRefreshDue(now - 60_000L, now), "An item at the 60 second boundary must refresh");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Double> invokeParser(String payload) {
        try {
            Method method = AuctionPriceProvider.class.getDeclaredMethod("parsePrices", String.class);
            method.setAccessible(true);
            return (Map<String, Double>) method.invoke(null, payload);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Coflnet direct price payload parser is missing", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<Double> invokeLowestBinParser(String payload) {
        try {
            Method method = AuctionPriceProvider.class.getDeclaredMethod("parseLowestBinPrice", String.class);
            method.setAccessible(true);
            return (Optional<Double>) method.invoke(null, payload);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Coflnet lowest BIN parser is missing", e);
        }
    }

    private static boolean invokeRefreshDue(long lastRefreshAt, long now) {
        try {
            Method method = AuctionPriceProvider.class.getDeclaredMethod(
                    "isTargetedRefreshDue", long.class, long.class
            );
            method.setAccessible(true);
            return (boolean) method.invoke(null, lastRefreshAt, now);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Targeted auction refresh cache is missing", e);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
