package net.iqaddons.mod.mixin.feature;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.skyblock.RagnarockCastEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class RagnarockCastMixin {

    private static final float RAGNAROCK_CAST_PITCH = 1.4920635f;
    private static final String RAGNAROCK_AXE_ID = "RAGNAROCK_AXE";

    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void iq$onPlaySound(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (!isWolfDeathSound(packet)) {
            return;
        }

        if (Math.abs(packet.getPitch() - RAGNAROCK_CAST_PITCH) > 0.00001f) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (!RAGNAROCK_AXE_ID.equals(getSkyblockItemId(mc.player.getMainHandItem()))) {
            return;
        }

        EventBus.post(new RagnarockCastEvent());
    }

    private boolean isWolfDeathSound(ClientboundSoundPacket packet) {
        String path = packet.getSound().value().location().getPath();
        return path.startsWith("entity.wolf.") && path.endsWith(".death");
    }

    private String getSkyblockItemId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }

        var nbt = customData.copyTag();
        if (!nbt.contains("id")) {
            return null;
        }

        return nbt.getString("id").orElse(null);
    }
}
