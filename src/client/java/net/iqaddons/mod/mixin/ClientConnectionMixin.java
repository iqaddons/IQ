package net.iqaddons.mod.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"))
    private void iq$onPacketReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof CommonPingS2CPacket pingPacket && pingPacket.getParameter() != 0) {
            EventBus.post(ClientTickEvent.create(MinecraftClient.getInstance()));
        }
    }
}
