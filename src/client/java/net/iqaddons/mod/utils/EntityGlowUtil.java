package net.iqaddons.mod.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import net.iqaddons.mod.utils.render.RenderColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

@UtilityClass
public class EntityGlowUtil {

    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_TEAM_HIGHLIGHT = 10;
    public static final int PRIORITY_FRESH = 100;

    private static final Int2ObjectMap<GlowStack> GLOWING_ENTITIES = new Int2ObjectOpenHashMap<>();

    public static void setGlowing(int entityId, @NotNull RenderColor color) {
        setGlowing(entityId, color, PRIORITY_DEFAULT);
    }

    public static void setGlowing(int entityId, @NotNull RenderColor color, int priority) {
        GlowStack stack = GLOWING_ENTITIES.computeIfAbsent(entityId, k -> new GlowStack());
        stack.add(priority, color);
    }

    public static void removeGlowing(int entityId) {
        GLOWING_ENTITIES.remove(entityId);
    }

    public static void removeGlowing(int entityId, int priority) {
        GlowStack stack = GLOWING_ENTITIES.get(entityId);
        if (stack != null) {
            stack.remove(priority);
            if (stack.isEmpty()) {
                GLOWING_ENTITIES.remove(entityId);
            }
        }
    }

    public static boolean isGlowing(int entityId) {
        return GLOWING_ENTITIES.containsKey(entityId);
    }

    public static boolean isGlowingWithPriority(int entityId, int minPriority) {
        GlowStack stack = GLOWING_ENTITIES.get(entityId);
        return stack != null && stack.getMaxPriority() >= minPriority;
    }

    public static @Nullable RenderColor getGlowColor(int entityId) {
        GlowStack stack = GLOWING_ENTITIES.get(entityId);
        return stack != null ? stack.getHighestPriorityColor() : null;
    }

    public static int getGlowColorInt(int entityId) {
        RenderColor color = getGlowColor(entityId);
        return color != null ? color.argb : -1;
    }

    /**
     * A stack of glow entries for a single entity, sorted by priority.
     * The highest priority glow is always rendered.
     */
    private static class GlowStack {
        private final Map<Integer, RenderColor> entries = new TreeMap<>((a, b) -> Integer.compare(b, a)); // Reverse order for descending priority

        void add(int priority, @NotNull RenderColor color) {
            entries.put(priority, color);
        }

        void remove(int priority) {
            entries.remove(priority);
        }

        boolean isEmpty() {
            return entries.isEmpty();
        }

        int getMaxPriority() {
            if (entries.isEmpty()) return -1;
            return entries.keySet().iterator().next(); // First key (highest priority due to reverse order)
        }

        @Nullable RenderColor getHighestPriorityColor() {
            if (entries.isEmpty()) return null;
            return entries.values().iterator().next(); // First value (highest priority)
        }
    }
}