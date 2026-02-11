package net.iqaddons.mod.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public final class ChestCounterManager {

    public static final int MAX_CHESTS = 60;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("config", "iq", "chest_counter.json");

    private static final ChestCounterManager INSTANCE = new ChestCounterManager();

    private volatile int chests;

    private ChestCounterManager() {
        load();
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
            save();
        }

        return chests;
    }

    public synchronized void reset() {
        chests = 0;
        save();
    }

    private synchronized void load() {
        if (!Files.exists(FILE)) {
            return;
        }

        try {
            String json = Files.readString(FILE, StandardCharsets.UTF_8);
            ChestCounterState state = GSON.fromJson(json, ChestCounterState.class);
            if (state != null) {
                chests = Math.max(0, Math.min(MAX_CHESTS, state.chests));
            }
        } catch (Exception e) {
            log.warn("Failed to load chest counter", e);
            chests = 0;
        }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(new ChestCounterState(chests)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to save chest counter", e);
        }
    }

    private record ChestCounterState(int chests) {
    }
}