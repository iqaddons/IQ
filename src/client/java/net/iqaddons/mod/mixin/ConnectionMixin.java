package net.iqaddons.mod.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ActionBarReceivedEvent;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.MobEffectReceivedEvent;
import net.iqaddons.mod.events.impl.TitleReceivedEvent;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.ServerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {

    @Unique
    private static final Minecraft client = Minecraft.getInstance();

    @Unique
    private static final KuudraStateManager kuudraStateManager = KuudraStateManager.get();

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void iq$onPacketReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundPingPacket pingPacket && pingPacket.getId() != 0) {
            client.execute(() -> EventBus.post(ClientTickEvent.create(client)));
        }

        if (packet instanceof ClientboundSystemChatPacket(Component content, boolean overlay)) {
            if (overlay) {
                EventBus.post(new ActionBarReceivedEvent(content));
            } else {
                var event = EventBus.post(new ChatReceivedEvent(content));
                if (event.isCancelled()) {
                    ci.cancel();
                    return;
                }
            }
        }

        if (packet instanceof ClientboundPlayerChatPacket chatPacket) {
            Component content = chatPacket.unsignedContent() != null
                    ? chatPacket.unsignedContent()
                    : Component.literal(chatPacket.body().content());
            if (iq$postChatEvent(chatPacket.chatType().decorate(content), ci)) {
                return;
            }
        }

        if (packet instanceof ClientboundDisguisedChatPacket chatPacket) {
            if (iq$postChatEvent(chatPacket.chatType().decorate(chatPacket.message()), ci)) {
                return;
            }
        }

        if (packet instanceof ClientboundSetActionBarTextPacket actionBarPacket) {
            EventBus.post(new ActionBarReceivedEvent(actionBarPacket.text()));
        }

        if (packet instanceof ClientboundSetTitleTextPacket titlePacket) {
            EventBus.post(new TitleReceivedEvent(titlePacket.text(), Component.empty()));
        }

        if (packet instanceof ClientboundSetSubtitleTextPacket subtitlePacket) {
            EventBus.post(new TitleReceivedEvent(Component.empty(), subtitlePacket.text()));
        }

        if (packet instanceof ClientboundUpdateMobEffectPacket effectPacket) {
            EventBus.post(new MobEffectReceivedEvent(
                    effectPacket.getEntityId(),
                    effectPacket.getEffect(),
                    effectPacket.getEffectDurationTicks()
            ));
        }

        if (packet instanceof ClientboundSetTimePacket) {
            ServerUtils.onWorldTimeUpdate();
        }
    }

    @Unique
    private boolean iq$postChatEvent(Component content, CallbackInfo ci) {
        var event = EventBus.post(new ChatReceivedEvent(content));
        if (!event.isCancelled()) {
            return false;
        }

        ci.cancel();
        return true;
    }

    @Unique
    private boolean iq$shouldHideEatenCountdown(Component text) {
        if (!PhaseThreeConfig.eatenTimer || !PhaseThreeConfig.eatenTimerConfig.hideDefaultCountdown || text == null) return false;

        KuudraPhase phase = kuudraStateManager.phase();
        if (phase != KuudraPhase.BUILD && phase != KuudraPhase.EATEN) return false;

        String message = text.getString()
                .replaceAll("(?i)§[0-9A-FK-OR]", "")
                .replaceAll("[\\p{Cntrl}\\u200B-\\u200F\\uFEFF]", "")
                .trim();
        return message.equals("1") || message.equals("2") || message.equals("3");
    }
}
