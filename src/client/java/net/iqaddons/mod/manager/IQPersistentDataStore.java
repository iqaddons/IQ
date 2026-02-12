package net.iqaddons.mod.manager;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.utils.data.DataKey;
import net.iqaddons.mod.utils.data.ScopedData;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public final class IQPersistentDataStore {

    public static final Path DATA_FILE = Path.of("config", "iq", "data.json");

    private static final IQPersistentDataStore INSTANCE = new IQPersistentDataStore();

    private ScopedData cache;

    public static @NotNull IQPersistentDataStore get() {
        return INSTANCE;
    }

    public synchronized <T> T getOrDefault(@NotNull DataKey<T> key, T defaultValue) {
        ensureLoaded();
        return cache.getOrDefault(key, defaultValue);
    }

    public synchronized <T> void set(@NotNull DataKey<T> key, T value) {
        ensureLoaded();
        cache.set(key, value);
        save();
    }

    public synchronized boolean has(@NotNull DataKey<?> key) {
        ensureLoaded();
        return cache.has(key);
    }

    private void ensureLoaded() {
        if (cache != null) {
            return;
        }

        cache = new ScopedData();
        if (!Files.exists(DATA_FILE)) {
            return;
        }

        try {
            cache.deserialize(Files.readString(DATA_FILE, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Failed to load persistent IQ data store", e);
            cache = new ScopedData();
        }
    }

    private void save() {
        try {
            Files.createDirectories(DATA_FILE.getParent());
            Files.writeString(DATA_FILE, cache.serialize(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to save persistent IQ data store", e);
        }
    }
}
