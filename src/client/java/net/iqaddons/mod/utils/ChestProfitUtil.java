package net.iqaddons.mod.utils;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.IQConstants;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.manager.pricing.ItemPriceManager;
import net.iqaddons.mod.manager.calculator.ChestProfitCalculator;
import net.iqaddons.mod.manager.calculator.impl.EnchantedBookValueCalculator;
import net.iqaddons.mod.manager.calculator.impl.EssenceValueCalculator;
import net.iqaddons.mod.manager.calculator.ItemValueCalculator;
import net.iqaddons.mod.manager.calculator.impl.GenericValueCalculator;
import net.iqaddons.mod.manager.calculator.impl.SalvageValueCalculator;
import net.iqaddons.mod.model.profit.chest.ChestItemValue;
import net.iqaddons.mod.model.profit.chest.ChestValueBreakdown;
import net.iqaddons.mod.model.profit.chest.data.ChestContents;
import net.iqaddons.mod.model.profit.chest.data.ChestData;
import net.iqaddons.mod.model.profit.chest.type.ChestKeyType;
import net.iqaddons.mod.model.profit.chest.type.ChestType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.iqaddons.mod.IQConstants.CRIMSON_ESSENCE_ID;

@Slf4j
public final class ChestProfitUtil {

    private static final ChestProfitCalculator CHEST_PROFIT_CALCULATOR;
    private static final int BUY_SLOT = 31;
    private static final int INFO_SLOT = 49;

    public static final Map<String, String> KUUDRA_DROPS_NAME_TO_API_ID = Map.of(
            "CRIMSON ESSENCE", CRIMSON_ESSENCE_ID,
            "KUUDRA TEETH", "KUUDRA_TEETH",
            "KISMET FEATHER", "KISMET_FEATHER",
            "WHEEL OF FATE", "WHEEL_OF_FATE"
    );

    static {
        Map<String, ItemValueCalculator> calculators = new HashMap<>();
        calculators.put(CRIMSON_ESSENCE_ID, new EssenceValueCalculator());
        calculators.put("ENCHANTED_BOOK", new EnchantedBookValueCalculator());

        var salvageCalculator = new SalvageValueCalculator();
        for (String armor : new String[]{"AURORA", "CRIMSON", "TERROR", "FERVOR", "HOLLOW"}) {
            for (String piece : new String[]{"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"}) {
                calculators.put(armor + "_" + piece, salvageCalculator);
            }
        }

        CHEST_PROFIT_CALCULATOR = new ChestProfitCalculator(
                new GenericValueCalculator(),
                calculators,
                ChestProfitUtil::resolveItemId
        );
    }

    public static @NotNull ChestValueBreakdown analyzeChest(@NotNull List<Slot> slots) {
        List<ItemStack> chestItems = new ArrayList<>();
        List<ChestItemValue> items = new ArrayList<>();

        for (int slotId = 9; slotId <= 17; slotId++) {
            if (slotId >= slots.size()) {
                continue;
            }

            ItemStack stack = slots.get(slotId).getStack();
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            chestItems.add(stack);

            String itemId = resolveItemId(stack);
            int quantity = Math.max(1, resolveItemQuantity(stack));
            double itemValue = CHEST_PROFIT_CALCULATOR.calculateItemValue(stack);

            if (CRIMSON_ESSENCE_ID.equals(itemId)) {
                int bonusQuantity = (int) Math.round(quantity * (KuudraGeneralConfig.kuudraPetBonus / 100.0));
                if (bonusQuantity > 0) {
                    double unitPrice = ItemPriceManager.get().getItemPrice(CRIMSON_ESSENCE_ID);
                    double bonusValue = unitPrice * bonusQuantity;
                    itemValue -= bonusValue;

                    items.add(new ChestItemValue(
                            Text.literal("§dCrimson Essence Bonus"),
                            bonusQuantity,
                            bonusValue
                    ));
                }
            }

            items.add(new ChestItemValue(
                    stack.getName(),
                    Math.max(1, stack.getCount()),
                    itemValue
            ));
        }

        ChestContents contents = new ChestContents(chestItems, parseKeyType(slots));
        double totalValue = CHEST_PROFIT_CALCULATOR.calculateTotalValue(contents);
        double keyCost = ItemPriceManager.get().calculateKeyPrice(contents.keyType());

        return new ChestValueBreakdown(
                totalValue, keyCost,
                totalValue - keyCost,
                items.stream()
                        .filter(chestItemValue -> chestItemValue.value() > 0)
                        .sorted(Comparator.comparingDouble(ChestItemValue::value).reversed())
                        .toList()
        );
    }

    public static @NotNull ChestData parseChest(
            @NotNull List<Slot> slots,
            @NotNull ItemPriceManager priceManager,
            @NotNull ChestType chestType
    ) {
        List<ItemStack> chestItems = new ArrayList<>();
        int essence = 0;
        int pricedItems = 0;

        for (int slotId = 9; slotId <= 17; slotId++) {
            if (slotId >= slots.size()) {
                continue;
            }

            ItemStack stack = slots.get(slotId).getStack();
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            chestItems.add(stack);
            String itemId = resolveItemId(stack);
            if (CRIMSON_ESSENCE_ID.equals(itemId)) {
                essence += resolveItemQuantity(stack);
            }

            if (itemId != null && priceManager.getItemPrice(itemId) > 0L) {
                pricedItems++;
            }
        }

        ChestContents contents = new ChestContents(chestItems, parseKeyType(slots));
        long grossValue = Math.max(0L, Math.round(CHEST_PROFIT_CALCULATOR.calculateTotalValue(contents)));
        long keyCost = Math.max(0L, chestType == ChestType.PAID
                ? Math.round(priceManager.calculateKeyPrice(contents.keyType()))
                : 0L
        );

        return new ChestData(
                chestType,
                grossValue,
                keyCost,
                grossValue - keyCost,
                essence,
                pricedItems
        );
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

    public static int resolveItemQuantity(@NotNull ItemStack stack) {
        String name = StringUtils.stripFormatting(stack.getName().getString());
        int index = name.lastIndexOf(" x");
        if (index == -1) {
            return stack.getCount();
        }

        String numberPart = name.substring(index + 2).replace(",", "");
        try {
            return Integer.parseInt(numberPart);
        } catch (NumberFormatException ignored) {
            return stack.getCount();
        }
    }

    private static ChestKeyType parseKeyType(@NotNull List<Slot> slots) {
        if (isFreeChest(slots)) return ChestKeyType.FREE;
        if (INFO_SLOT >= slots.size()) return ChestKeyType.UNKNOWN;

        ItemStack infoStack = slots.get(49).getStack();
        if (infoStack == null || infoStack.isEmpty()) {
            return ChestKeyType.UNKNOWN;
        }

        List<String> lore = getLoreLines(infoStack);
        for (String rawLine : lore) {
            String line = StringUtils.stripFormatting(rawLine);
            String lower = line.toLowerCase();
            if (!lower.contains("kuudra")) continue;

            if (lower.contains("infernal")) return ChestKeyType.INFERNAL;
            if (lower.contains("fiery")) return ChestKeyType.FIERY;
            if (lower.contains("burning")) return ChestKeyType.BURNING;
            if (lower.contains("hot")) return ChestKeyType.HOT;
            if (lower.contains("basic")) return ChestKeyType.BASIC;
        }

        return ChestKeyType.UNKNOWN;
    }

    private static boolean isFreeChest(@NotNull List<Slot> slots) {
        if (BUY_SLOT >= slots.size()) return false;
        ItemStack buyStack = slots.get(BUY_SLOT).getStack();
        if (buyStack == null || buyStack.isEmpty()) {
            return false;
        }

        return getLoreLines(buyStack)
                .stream()
                .map(StringUtils::stripFormatting)
                .map(String::toLowerCase)
                .anyMatch(line -> line.contains("free")
                        || line.contains("free reward chest"));
    }

    private static @Nullable String resolveItemId(@NotNull ItemStack stack) {
        String itemId = getSkyblockItemId(stack);
        if (itemId != null && !itemId.isBlank()) {
            return itemId;
        }

        String shardId = resolveShardId(stack);
        if (shardId != null) {
            return shardId;
        }

        String displayName = StringUtils
                .stripFormatting(stack.getName().getString())
                .toUpperCase()
                .trim();

        displayName = stripTrailingQuantity(displayName);
        return findMappedItemId(displayName, KUUDRA_DROPS_NAME_TO_API_ID);
    }

    private static @Nullable String resolveShardId(@NotNull ItemStack stack) {
        String name = StringUtils
                .stripFormatting(stack.getName().getString())
                .trim()
                .toUpperCase();

        name = stripTrailingQuantity(name);
        if (!name.endsWith("SHARD")) {
            return null;
        }

        String base = name.substring(0, name.length() - " SHARD".length()).trim();
        return "SHARD_" + base.replace(" ", "_");
    }


    private static @NotNull String stripTrailingQuantity(@NotNull String name) {
        int index = name.lastIndexOf(" X");
        if (index == -1) return name;

        String possibleNumber = name.substring(index + 2).replace(",", "");
        if (possibleNumber.matches("\\d+")) {
            return name.substring(0, index).trim();
        }

        return name;
    }

    private static @Nullable String findMappedItemId(String displayName, @NotNull Map<String, String> mapping) {
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (displayName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private static @Nullable String getSkyblockItemId(@NotNull ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return null;

        var nbt = customData.copyNbt();
        if (!nbt.contains("id")) {
            return null;
        }

        return nbt.getString("id").orElse("UNKNOWN");
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

}
