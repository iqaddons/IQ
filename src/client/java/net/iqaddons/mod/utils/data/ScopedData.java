package net.iqaddons.mod.utils.data;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ScopedData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, JsonElement> data = new ConcurrentHashMap<>();

    public <T> void set(@NotNull DataKey<T> key, T value) {
        setById(key.id(), value);
    }

    public <T> void setById(@NotNull String id, T value) {
        if (value == null) {
            data.remove(id);
            return;
        }

        data.put(id, GSON.toJsonTree(value));
    }

    public <T> @NotNull Optional<T> get(@NotNull DataKey<T> key) {
        return getById(key.id(), key);
    }

    private <T> @NotNull Optional<T> getById(@NotNull String id, @NotNull DataKey<T> key) {
        JsonElement value = data.get(id);
        if (value == null || value.isJsonNull()) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(GSON.fromJson(value, key.type()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public <T> T getOrDefault(@NotNull DataKey<T> key, T defaultValue) {
        return get(key).orElse(defaultValue);
    }

    public boolean has(@NotNull DataKey<?> key) {
        return data.containsKey(key.id());
    }

    public void remove(@NotNull DataKey<?> key) {
        data.remove(key.id());
    }

    public @NotNull String serialize() {
        JsonObject root = new JsonObject();
        data.forEach(root::add);
        return GSON.toJson(root);
    }

    public void deserialize(String json) {
        data.clear();
        if (json == null || json.trim().isEmpty()) return;

        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return;
            }

            JsonObject root = parsed.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isJsonNull()) {
                    data.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception ignored) {
        }
    }
}