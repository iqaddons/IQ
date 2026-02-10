package net.iqaddons.mod.features;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@NoArgsConstructor
public final class FeatureManager {

    private static final int SYNC_INTERVAL_TICKS = 20;

    private final Map<String, Feature> features = new LinkedHashMap<>();
    private EventBus.Subscription<ClientTickEvent> tickSubscription;

    public void register(@NotNull Feature @NotNull ... features) {
        for (Feature feature : features) {
            this.features.put(feature.getId(), feature);
            log.debug("Registered feature: {}", feature.getName());
        }
    }

    public void start() {
        tickSubscription = EventBus.subscribe(ClientTickEvent.class, this::onTick);
        syncAllFeatures();
        log.info("FeatureManager started with {} features", features.size());
    }

    public void stop() {
        EventBus.unsubscribe(tickSubscription);

        features.values().forEach(Feature::deactivate);
    }

    public @Nullable Feature get(@NotNull String id) {
        return features.get(id);
    }

    @SuppressWarnings("unchecked")
    public <T extends Feature> @Nullable T get(@NotNull Class<T> type) {
        return (T) features.values().stream()
                .filter(type::isInstance)
                .findFirst()
                .orElse(null);
    }

    public @NotNull @UnmodifiableView Collection<Feature> all() {
        return Collections.unmodifiableCollection(features.values());
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (event.isNthTick(SYNC_INTERVAL_TICKS)) {
            syncAllFeatures();
        }
    }

    private void syncAllFeatures() {
        for (Feature feature : features.values()) {
            boolean shouldBeActive = feature.isEnabled();

            if (shouldBeActive && !feature.isActive()) {
                feature.activate();
                log.debug("Activated: {}", feature.getName());
            } else if (!shouldBeActive && feature.isActive()) {
                feature.deactivate();
                log.debug("Deactivated: {}", feature.getName());
            }
        }
    }
}