package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.config.categories.PhaseFourConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.ItemUseEvent;
import net.iqaddons.mod.events.impl.skyblock.BackboneStateChangeEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.manager.BackboneAlertManager;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class BackboneAlertFeature extends Feature {

    private static final int BACKBONE_TICKS = 22;
    private static final int BACKBONE_COOLDOWN_TICKS = 32;

    private final BackboneAlertManager manager = BackboneAlertManager.get();

    public BackboneAlertFeature() {
        super(
                "backboneAlert",
                "Backbone Alert",
                () -> PhaseFourConfig.backboneAlert
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ItemUseEvent.class, this::onItemUse);
        subscribe(ClientTickEvent.class, this::onTick);

        manager.reset();
        postStateChange(false);
    }

    @Override
    protected void onDeactivate() {
        manager.reset();
        postStateChange(false);
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
        manager.setThrowOrigin(getPlayerEyePos(mc.player));
        manager.startBackboneTimer(Math.max(1, BACKBONE_TICKS - PhaseFourConfig.backboneAlertAdvanceTicks));

        postStateChange(false);
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || mc.player == null) return;

        boolean trackedBackHit = manager.trackBackHit(mc.player);
        BackboneAlertManager.BoneResult result = manager.tick();
        postStateChange(result.triggerRendNow());
        if (!result.triggerRendNow() && !trackedBackHit) return;

        if (PhaseFourConfig.backboneAlertSound) {
            mc.player.playSound(
                    SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(),
                    2.0f, 1.0f
            );
        }
    }

    private void postStateChange(boolean rendTriggered) {
        EventBus.post(new BackboneStateChangeEvent(
                manager.getTicksRemaining(),
                manager.getStartingTicks(),
                manager.isRendActive(),
                rendTriggered
        ));
    }

    private @NotNull Vec3d getPlayerEyePos(@NotNull ClientPlayerEntity player) {
        return player.getEntityPos().add(0.0, player.getStandingEyeHeight(), 0.0);
    }
}
