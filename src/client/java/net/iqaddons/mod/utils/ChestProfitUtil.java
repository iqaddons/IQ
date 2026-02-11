package net.iqaddons.mod.utils;

import net.iqaddons.mod.manager.KuudraPriceCacheManager;
import net.iqaddons.mod.model.profit.ChestData;
import net.iqaddons.mod.model.profit.ChestType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChestProfitUtil {

    private static final Pattern ESSENCE_PATTERN = Pattern.compile("(?i)(\\d+)\\s*crimson essence");

    public static @NotNull ChestData parseChest(
            @NotNull List<Slot> slots,
            @NotNull KuudraPriceCacheManager priceCache,
            @NotNull ChestType chestType
    ) {
        long grossValue = 0L;
        long keyCost = parseKeyCost(slots, priceCache);

        int essence = 0;
        int teeth = 0;
        int pricedItems = 0;

        for (int slotId = 9; slotId <= 17; slotId++) {
            if (slotId >= slots.size()) {
                continue;
            }

            ItemStack stack = slots.get(slotId).getStack();
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            int count = Math.max(1, stack.getCount());
            String itemId = getSkyblockItemId(stack);
            if (itemId == null || itemId.isBlank()) {
                itemId = mapFromDisplay(stack.getName().getString());
            }

            if (itemId == null || itemId.isBlank()) {
                continue;
            }

            if ("ESSENCE_CRIMSON".equals(itemId)) {
                essence += parseEssenceAmount(stack, count);
            }

            if ("KUUDRA_TEETH".equals(itemId)) {
                teeth += count;
            }

            long itemPrice = priceCache.getItemPrice(itemId);
            if (itemPrice <= 0L) continue;

            long stackValue = itemPrice * count;
            grossValue += stackValue;
            pricedItems++;
        }

        return new ChestData(
                chestType,
                grossValue,
                keyCost,
                grossValue - keyCost,
                essence,
                teeth,
                pricedItems
        );
    }

    private static long parseKeyCost(@NotNull List<Slot> slots, @NotNull KuudraPriceCacheManager priceCache) {
        if (31 >= slots.size()) return 0L;

        ItemStack buyStack = slots.get(31).getStack();
        if (buyStack == null || buyStack.isEmpty()) {
            return 0L;
        }

        List<String> lore = getLoreLines(buyStack);
        for (int i = 0; i < lore.size(); i++) {
            String line = StringUtils.stripFormatting(lore.get(i));
            if (!line.equalsIgnoreCase("Cost")) {
                continue;
            }

            if (i + 1 >= lore.size()) {
                return 0L;
            }

            String costLine = StringUtils.stripFormatting(lore.get(i + 1));
            if (costLine.contains("FREE") || costLine.contains("This Chest is Free")) {
                return 0L;
            }

            String lower = costLine.toLowerCase(Locale.ROOT);
            if (lower.contains("infernal")) return priceCache.getKeyPrice("INFERNAL");
            if (lower.contains("fiery")) return priceCache.getKeyPrice("FIERY");
            if (lower.contains("burning")) return priceCache.getKeyPrice("BURNING");
            if (lower.contains("hot")) return priceCache.getKeyPrice("HOT");
            if (lower.contains("kuudra key")) return priceCache.getKeyPrice("KUUDRA_KEY");
            return 0L;
        }

        return 0L;
    }

    private static int parseEssenceAmount(@NotNull ItemStack stack, int fallbackCount) {
        Matcher matcher = ESSENCE_PATTERN.matcher(StringUtils.stripFormatting(stack.getName().getString()));
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return fallbackCount;
            }
        }

        return fallbackCount;
    }

    private static @Nullable String mapFromDisplay(@NotNull String displayName) {
        String stripped = StringUtils.stripFormatting(displayName).toUpperCase(Locale.ROOT);
        if (stripped.contains("CRIMSON ESSENCE")) return "ESSENCE_CRIMSON";
        if (stripped.contains("KUUDRA TEETH")) return "KUUDRA_TEETH";
        if (stripped.contains("KISMET FEATHER")) return "KISMET_FEATHER";
        if (stripped.contains("WHEEL OF FATE")) return "WHEEL_OF_FATE";
        return null;
    }

    public static @Nullable String getSkyblockItemId(@NotNull ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }

        var nbt = customData.copyNbt();
        if (!nbt.contains("ExtraAttributes")) {
            return null;
        }

        var extra = nbt.getCompound("ExtraAttributes");
        if (extra.isPresent() && extra.get().contains("id")) {
            return extra.get().getString("id", "UNDEFINED");
        }

        return null;
    }

    public static @NotNull List<String> getLoreLines(@NotNull ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        lore.lines().forEach(line -> lines.add(line.getString()));
        return lines;
    }

    public static boolean canUseReroll(ItemStack stack, @NotNull String blockedPhrase) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        String title = Formatting.strip(stack.getName().getString()).toLowerCase();
        if (!title.contains("reroll")) return false;

        String loreJoined = getLoreLines(stack).stream()
                .map(StringUtils::stripFormatting)
                .map(String::toLowerCase)
                .reduce("", (a, b) -> a + "\n" + b);

        return !loreJoined.contains(blockedPhrase.toLowerCase());
    }


}
