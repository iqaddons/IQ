package net.iqaddons.mod.mixin.feature;

import net.iqaddons.mod.IQConstants;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MuteTerminatorCooldownSoundMixin {

    @Inject(
            method = "onPlaySound",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$muteTerminatorCooldownSound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (Configuration.muteTerminatorCooldownSound
                && packet.getSound().matchesId(SoundEvents.ENTITY_ENDERMAN_TELEPORT.id())
                && isInSkyBlock()
                && isHoldingTerminator()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onGameMessage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$muteTerminatorCooldownMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }

        ItemStack stack = client.player.getMainHandStack();
        if (stack.isEmpty()) {
            return false;
        }

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        var nbt = customData.copyNbt();
        if (!nbt.contains("id")) {
            return false;
        }

        String itemId = nbt.getString("id").orElse("");
        return "skyblock:TERMINATOR".equalsIgnoreCase(itemId) || "TERMINATOR".equalsIgnoreCase(itemId);
    }
}


