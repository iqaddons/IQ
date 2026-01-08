package net.iqaddons.mod.features;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FeatureManager {

    private static FeatureManager instance;
    private final Map<Class<? extends Feature>, Feature> features = new HashMap<>();

    private FeatureManager() {
    }

    public void register(@NotNull AbstractFeature @NotNull ... features) {
        for (var feature : features) {
            register(feature);
        }
    }

    public void register(@NotNull AbstractFeature feature) {
        feature.register();
        features.put(feature.getClass(), feature);
    }

    @SuppressWarnings("unchecked")
    public <T extends Feature> T get(Class<T> type) {
        return (T) features.get(type);
    }

    public Collection<Feature> getAll() {
        return Collections.unmodifiableCollection(features.values());
    }

    public static FeatureManager get() {
        if (instance == null) {
            instance = new FeatureManager();
        }

        return instance;
    }
}
