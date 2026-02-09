package net.iqaddons.mod.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import net.iqaddons.mod.utils.render.RenderColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class EntityGlowUtil {

    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_TEAM_HIGHLIGHT = 10;
    public static final int PRIORITY_FRESH = 100;

    private static final Int2ObjectMap<GlowEntry> GLOWING_ENTITIES = new Int2ObjectOpenHashMap<>();

    public static void setGlowing(int entityId, @NotNull RenderColor color) {
        setGlowing(entityId, color, PRIORITY_DEFAULT);
    }

    public static void setGlowing(int entityId, @NotNull RenderColor color, int priority) {
        GlowEntry existing = GLOWING_ENTITIES.get(entityId);

        if (existing == null || priority >= existing.priority()) {
            GLOWING_ENTITIES.put(entityId, new GlowEntry(color, priority));
        }
    }

    public static void removeGlowing(int entityId) {
        GLOWING_ENTITIES.remove(entityId);
    }

    public static void removeGlowing(int entityId, int priority) {
        GlowEntry existing = GLOWING_ENTITIES.get(entityId);
        if (existing != null && existing.priority() == priority) {
            GLOWING_ENTITIES.remove(entityId);
        }
    }

    public static boolean isGlowing(int entityId) {
        return GLOWING_ENTITIES.containsKey(entityId);
    }

    public static boolean isGlowingWithPriority(int entityId, int minPriority) {
        GlowEntry entry = GLOWING_ENTITIES.get(entityId);
        return entry != null && entry.priority() >= minPriority;
    }

    public static @Nullable RenderColor getGlowColor(int entityId) {
        GlowEntry entry = GLOWING_ENTITIES.get(entityId);
        return entry != null ? entry.color() : null;
    }

    public static int getGlowColorInt(int entityId) {
        GlowEntry entry = GLOWING_ENTITIES.get(entityId);
        return entry != null ? entry.color().argb : -1;
    }

    private record GlowEntry(
            @NotNull RenderColor color,
            int priority
    ) {}
}