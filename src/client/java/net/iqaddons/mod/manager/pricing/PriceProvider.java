package net.iqaddons.mod.manager.pricing;

import java.util.Optional;

public interface PriceProvider {

    Optional<Double> getPrice(String itemId);

    void update();

    boolean isReady();
}