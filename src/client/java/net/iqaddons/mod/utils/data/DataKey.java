package net.iqaddons.mod.utils.data;

import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public record DataKey<T>(
        @NotNull String id,
        @NotNull Type type
) {

    public static <T> @NotNull DataKey<T> of(@NotNull String id, @NotNull Class<T> type) {
        return new DataKey<>(id, type);
    }

    public static <T> @NotNull DataKey<T> of(@NotNull String id, @NotNull TypeToken<T> typeToken) {
        return new DataKey<>(id, typeToken.getType());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DataKey<?> other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}