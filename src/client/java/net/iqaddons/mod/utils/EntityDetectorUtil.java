package net.iqaddons.mod.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@UtilityClass
public class EntityDetectorUtil {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    public static <T extends Entity> @NotNull List<T> getEntitiesOfType(@NotNull Class<T> entityClass) {
        ClientWorld world = MC.world;
        if (world == null) {
            return Collections.emptyList();
        }

        return StreamSupport.stream(world.getEntities().spliterator(), false)
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

    public static @NotNull List<GiantEntity> getSupplyCarriers() {
        return getEntitiesOfType(GiantEntity.class, giant ->
                giant.getY() < 67.0 && isHoldingSkull(giant)
        );
    }

    public static @NotNull List<ArmorStandEntity> getArmorStandsByName(@NotNull String nameContains) {
        return getEntitiesOfType(ArmorStandEntity.class, stand ->
                stand.hasCustomName() &&
                        stand.getCustomName() != null &&
                        stand.getCustomName().getString().contains(nameContains)
        );
    }

    public static @NotNull List<ArmorStandEntity> getCompletedPileStands() {
        return getArmorStandsByName("SUPPLIES RECEIVED");
    }

    public static @NotNull Optional<ArmorStandEntity> findElle() {
        return getEntitiesOfType(ArmorStandEntity.class).stream()
                .filter(stand ->
                        stand.hasCustomName() &&
                                stand.getCustomName() != null &&
                                stand.getCustomName().getString().toLowerCase().contains("elle")
                )
                .findFirst();
    }

    private static boolean isHoldingSkull(@NotNull GiantEntity giant) {
        ItemStack heldItem = giant.getMainHandStack();
        if (heldItem.isEmpty()) return false;

        return heldItem.isOf(Items.PLAYER_HEAD) || heldItem.isOf(Items.SKELETON_SKULL) ||
                heldItem.isOf(Items.WITHER_SKELETON_SKULL) || heldItem.isOf(Items.ZOMBIE_HEAD) ||
                heldItem.isOf(Items.CREEPER_HEAD) || heldItem.isOf(Items.PIGLIN_HEAD) ||
                heldItem.isOf(Items.DRAGON_HEAD);
    }

    public static @NotNull List<ArmorStandEntity> getAllArmorStands() {
        return getEntitiesOfType(ArmorStandEntity.class);
    }
}


