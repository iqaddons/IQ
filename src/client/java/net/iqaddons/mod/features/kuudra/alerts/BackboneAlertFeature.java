package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.ItemUseEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.manager.BackboneAlertManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;

public class BackboneAlertFeature extends KuudraFeature {

    private static final int BACKBONE_TICKS = 22;
    private static final int BACKBONE_COOLDOWN_TICKS = 32;

    private final BackboneAlertManager manager = BackboneAlertManager.get();

    public BackboneAlertFeature() {
        super(
                "backboneAlert",
                "Backbone Alert",
                () -> PhaseFourConfig.backboneAlert,
                KuudraPhase.BOSS
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(ItemUseEvent.class, this::onItemUse);
        subscribe(ClientTickEvent.class, this::onTick);

        manager.reset();
    }

    @Override
    protected void onKuudraDeactivate() {
        manager.reset();
    }

    private void onItemUse(@NotNull ItemUseEvent event) {
        if (mc.player == null || event.getHand() != Hand.MAIN_HAND) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        String itemName = StringUtils.stripFormatting(stack.getName().getString()).toLowerCase();
        if (!itemName.contains("bonemerang") || manager.isOnCooldown()) {
            return;
        }

        manager.setCooldownTicks(BACKBONE_COOLDOWN_TICKS);
        manager.startBackboneTimer(BACKBONE_TICKS);
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || !event.isNthTick(2)) return;

        BackboneAlertManager.BoneResult result = manager.tick();
        if (!result.triggerRendNow() || mc.player == null) {
            return;
        }

        mc.getSoundManager().play(PositionedSoundInstance.master(
                SoundEvents.GOAT_HORN_SOUNDS.getFirst().value(),
                0.8f,
                1.0f
        ));
    }
}
