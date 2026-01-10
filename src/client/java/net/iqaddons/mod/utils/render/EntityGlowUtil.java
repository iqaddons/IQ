package net.iqaddons.mod.utils.render;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class EntityGlowUtil {

    private static final Int2ObjectMap<RenderColor> GLOWING_ENTITIES = new Int2ObjectOpenHashMap<>();

    public static void setGlowing(int entityId, @NotNull RenderColor color) {
        GLOWING_ENTITIES.put(entityId, color);
    }

    public static void removeGlowing(int entityId) {
        GLOWING_ENTITIES.remove(entityId);
    }

    public static boolean isGlowing(int entityId) {
        return GLOWING_ENTITIES.containsKey(entityId);
    }

    public static @Nullable RenderColor getGlowColor(int entityId) {
        return GLOWING_ENTITIES.get(entityId);
    }

    public static int getGlowColorInt(int entityId) {
        RenderColor color = GLOWING_ENTITIES.get(entityId);
        return color != null ? color.argb : -1;
    }

    public static void clearAll() {
        GLOWING_ENTITIES.clear();
    }

    public static int size() {
        return GLOWING_ENTITIES.size();
    }
}