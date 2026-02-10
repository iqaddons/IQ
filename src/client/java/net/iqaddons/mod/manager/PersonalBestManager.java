package net.iqaddons.mod.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
public final class PersonalBestManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PB_FILE = Path.of("config", "iq", "pb_tracker.json");

    private static final PersonalBestManager INSTANCE = new PersonalBestManager();

    private volatile PersonalBest current = new PersonalBest(-1L, new EnumMap<>(KuudraPhase.class));

    private PersonalBestManager() {
        load();
    }

    public synchronized void load() {
        if (!Files.exists(PB_FILE)) {
            return;
        }

        try {
            String json = Files.readString(PB_FILE, StandardCharsets.UTF_8);
            PersonalBest loaded = GSON.fromJson(json, PersonalBest.class);
            if (loaded == null) return;

            Map<KuudraPhase, Long> phaseMap = new EnumMap<>(KuudraPhase.class);
            if (loaded.phaseSplitsMillis != null) {
                phaseMap.putAll(loaded.phaseSplitsMillis);
            }

            current = new PersonalBest(loaded.bestTimeMillis, phaseMap);
        } catch (Exception e) {
            log.warn("Failed to load PB tracker file", e);
        }
    }

    public synchronized void updatePersonalBest(long totalMillis, @NotNull Map<KuudraPhase, Long> splitMillis) {
        Map<KuudraPhase, Long> nextSplits = new EnumMap<>(KuudraPhase.class);
        nextSplits.putAll(splitMillis);

        current = new PersonalBest(totalMillis, nextSplits);
        save();
    }

    private synchronized void save() {
        try {
            Files.createDirectories(PB_FILE.getParent());
            String json = GSON.toJson(current, PersonalBest.class);
            Files.writeString(PB_FILE, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to save PB tracker file", e);
        }
    }

    public long getBestTimeMillis() {
        return current.bestTimeMillis;
    }

    public @NotNull @Unmodifiable Map<KuudraPhase, Long> getSplitsMillis() {
        return Map.copyOf(current.phaseSplitsMillis);
    }

    public boolean hasPersonalBest() {
        return current.bestTimeMillis > 0;
    }

    public static @NotNull PersonalBestManager get() {
        return INSTANCE;
    }

    private record PersonalBest(
            long bestTimeMillis,
            Map<KuudraPhase, Long> phaseSplitsMillis
    ) { }
}