package net.iqaddons.mod.mixin.feature;

import net.iqaddons.mod.IQConstants;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MuteTerminatorCooldownSoundMixin {

    @Inject(
            method = "handleSoundEvent",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$muteTerminatorCooldownSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (Configuration.muteTerminatorCooldownSound
                && packet.getSound().is(SoundEvents.ENDERMAN_TELEPORT.location())
                && isInSkyBlock()
                && isHoldingTerminator()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "handleSystemChat",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$muteTerminatorCooldownMessage(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (!Configuration.muteTerminatorCooldownSound || !isInSkyBlock() || !isHoldingTerminator()) {
            return;
        }

        String plainMessage = StringUtils.stripFormatting(packet.content().getString());
        if (plainMessage.startsWith("This ability is on cooldown for ")) {
            ci.cancel();
        }
    }

    private static boolean isInSkyBlock() {
        return ScoreboardUtils.hasTitle(IQConstants.SKYBLOCK_AREA_ID);
    }

    private static boolean isHoldingTerminator() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        }

        ItemStack stack = client.player.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        var nbt = customData.copyTag();
        if (!nbt.contains("id")) {
            return false;
        }

        String itemId = nbt.getString("id").orElse("");
        return "skyblock:TERMINATOR".equalsIgnoreCase(itemId) || "TERMINATOR".equalsIgnoreCase(itemId);
    }
}


