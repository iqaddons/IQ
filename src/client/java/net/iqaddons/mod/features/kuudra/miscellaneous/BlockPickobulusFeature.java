package net.iqaddons.mod.features.kuudra.miscellaneous;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.impl.ItemUseEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@Slf4j
public class BlockPickobulusFeature extends KuudraFeature {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final String PICKOBULUS_ABILITY_LORE = "ability: pickobulus";
    private static final long BLOCK_MESSAGE_COOLDOWN_MS = 900L;

    private long lastBlockMessageMillis = 0L;

    public BlockPickobulusFeature() {
        super(
                "blockPickobulus",
                "Block Pickobulus",
                () -> PhaseThreeConfig.blockPickobulus,
                KuudraPhase.SUPPLIES, KuudraPhase.BUILD,
                KuudraPhase.DPS, KuudraPhase.SKIP, KuudraPhase.BOSS
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(ItemUseEvent.class, this::onItemUse);
    }

    private void onItemUse(@NotNull ItemUseEvent event) {
        if (hasPickobulusAbility(event.getItemStack())) {
            event.setCancelled(true);
            showBlockMessage();
        }
    }

    private void showBlockMessage() {
        long now = System.currentTimeMillis();
        if (now - lastBlockMessageMillis < BLOCK_MESSAGE_COOLDOWN_MS) return;

        lastBlockMessageMillis = now;
        MessageUtil.showTitle("", "&cPickobulus Blocked", 0, 14, 4);
        if (MC.player != null) {
            MC.player.playSound(SoundEvents.VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private boolean hasPickobulusAbility(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return false;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;

        return lore.lines().stream()
                .map(line -> StringUtils.stripFormatting(line.getString()).toLowerCase(Locale.ROOT))
                .anyMatch(line -> line.contains(PICKOBULUS_ABILITY_LORE));
    }
}
