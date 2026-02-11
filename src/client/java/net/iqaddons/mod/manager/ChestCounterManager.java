package net.iqaddons.mod.manager;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.utils.data.DataKey;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;

@Slf4j
public final class ChestCounterManager {

    public static final int MAX_CHESTS = 60;
    private static final ChestCounterManager INSTANCE = new ChestCounterManager();

    private static final DataKey<Integer> CHEST_COUNT_KEY = DataKey.of("chestCount", Integer.class);

    private final IQPersistentDataStore store = IQPersistentDataStore.get();
    private volatile int chests;

    private ChestCounterManager() {
        int stored = Math.max(0, Math.min(MAX_CHESTS, store.getOrDefault(CHEST_COUNT_KEY, 0)));
        if (stored > 0 || Files.exists(IQPersistentDataStore.DATA_FILE) && store.has(CHEST_COUNT_KEY)) {
            chests = stored;
        }
    }

    public static @NotNull ChestCounterManager get() {
        return INSTANCE;
    }

    public synchronized int getChests() {
        return chests;
    }

    public synchronized int increment() {
        if (chests < MAX_CHESTS) {
            chests++;
            store.set(CHEST_COUNT_KEY, chests);
        }

        return chests;
    }

    public synchronized void reset() {
        chests = 0;
        store.set(CHEST_COUNT_KEY, chests);
    }
}