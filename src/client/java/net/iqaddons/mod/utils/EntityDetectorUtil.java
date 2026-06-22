package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@UtilityClass
public class EntityDetectorUtil {

    private static final Minecraft mc = Minecraft.getInstance();

    public static <T extends Entity> @NotNull List<T> getEntitiesOfType(@NotNull Class<T> entityClass) {
        ClientLevel world = mc.level;
        if (world == null) {
            return Collections.emptyList();
        }

        return StreamSupport.stream(world.entitiesForRendering().spliterator(), false)
                .filter(entityClass::isInstance)
                .map(entityClass::cast)
                .collect(Collectors.toList());
    }

    public static <T extends Entity> @NotNull List<T> getEntitiesOfType(
            @NotNull Class<T> entityClass,
            @NotNull Predicate<T> predicate
    ) {
        return getEntitiesOfType(entityClass).stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    public static @NotNull List<Giant> getSupplyCarriers() {
        return getEntitiesOfType(Giant.class, giant ->
                giant.getY() < 67.0 && isHoldingSkull(giant)
        );
    }

    public static @NotNull List<ArmorStand> getArmorStandsByName(@NotNull String nameContains) {
        return getEntitiesOfType(ArmorStand.class, stand ->
                stand.hasCustomName() &&
                        stand.getCustomName() != null &&
                        stand.getCustomName().getString().contains(nameContains)
        );
    }

    public static @NotNull List<ArmorStand> getCompletedPileStands() {
        return getArmorStandsByName("SUPPLIES RECEIVED");
    }

    public static @NotNull Optional<ArmorStand> findElle() {
        return getEntitiesOfType(ArmorStand.class).stream()
                .filter(stand ->
                        stand.hasCustomName() &&
                                stand.getCustomName() != null &&
                                stand.getCustomName().getString().toLowerCase().contains("elle")
                )
                .findFirst();
    }

    private static boolean isHoldingSkull(@NotNull Giant giant) {
        ItemStack heldItem = giant.getMainHandItem();
        if (heldItem.isEmpty()) return false;

        return heldItem.is(Items.PLAYER_HEAD) || heldItem.is(Items.SKELETON_SKULL) ||
                heldItem.is(Items.WITHER_SKELETON_SKULL) || heldItem.is(Items.ZOMBIE_HEAD) ||
                heldItem.is(Items.CREEPER_HEAD) || heldItem.is(Items.PIGLIN_HEAD) ||
                heldItem.is(Items.DRAGON_HEAD);
    }

    public static @NotNull List<ArmorStand> getAllArmorStands() {
        return getEntitiesOfType(ArmorStand.class);
    }

    public static Optional<AbstractClientPlayer> findPlayerByName(@NotNull String name) {
        if (mc.level == null) return Optional.empty();

        Optional<AbstractClientPlayer> result = mc.level.players().stream()
                .filter(player -> player.getName().getString().equalsIgnoreCase(name))
                .findFirst();

        if (result.isEmpty()) {
            // Log all available players for debugging
            System.out.println("DEBUG: Could not find player '" + name + "'. Available players:");
            mc.level.players().forEach(p ->
                System.out.println("  - '" + p.getName().getString() + "' (matches: " +
                    p.getName().getString().equalsIgnoreCase(name) + ")")
            );
        }

        return result;
    }

    public static @Nullable AbstractClientPlayer findPlayerById(int entityId) {
        if (mc.level == null) return null;
        for (AbstractClientPlayer player : mc.level.players()) {
            if (player.getId() == entityId) {
                return player;
            }
        }
        return null;
    }
}


