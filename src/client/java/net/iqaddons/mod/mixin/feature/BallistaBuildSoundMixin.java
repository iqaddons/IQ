package net.iqaddons.mod.mixin.feature;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Mixin(ClientPacketListener.class)
public class BallistaBuildSoundMixin {

    @Unique
    private static final Identifier BALLISTA_BUILD_SOUND = Identifier.fromNamespaceAndPath("iq", "ballista_build");

    @Unique
    private static final Minecraft client = Minecraft.getInstance();

    @Unique
    private static final Queue<Float> iq$pendingBallistaBuildSoundPitches = new ConcurrentLinkedQueue<>();

    static {
        ClientTickEvents.END_CLIENT_TICK.register(BallistaBuildSoundMixin::iq$playPendingBallistaBuildSounds);
    }

    @Inject(
            method = "handleSoundEvent",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$replaceBallistaBuildSound(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (KuudraStateManager.get().phase() == KuudraPhase.BUILD) {
            if (PhaseTwoConfig.cleanBallistaSounds) {
                ci.cancel();
            }

            if (PhaseTwoConfig.replaceBallistaBuildSound
                    && packet.getSound().is(SoundEvents.ANVIL_LAND.location())
            ) {
                iq$pendingBallistaBuildSoundPitches.offer(packet.getPitch());
                ci.cancel();
            }
        }
    }

    @Unique
    private static void iq$playPendingBallistaBuildSounds(Minecraft client) {
        if (client.getSoundManager() == null) return;

        Float pitch;
        while ((pitch = iq$pendingBallistaBuildSoundPitches.poll()) != null) {
            client.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(
                    SoundEvent.createVariableRangeEvent(BALLISTA_BUILD_SOUND),
                    pitch,
                    0.8F)
            );
        }
    }
}
