package net.iqaddons.mod.mixin.feature;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Slf4j
@Mixin(ClientPlayNetworkHandler.class)
public class BallistaBuildSoundMixin {

    @Unique
    private static final SoundEvent BALLISTA_BUILD_SOUND = Registry.register(
            Registries.SOUND_EVENT,
            Identifier.of("iq", "ballista_build"),
            SoundEvent.of(Identifier.of("iq", "ballista_build"))
    );

    @Inject(
            method = "onPlaySound",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$replaceBallistaBuildSound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (!PhaseTwoConfig.replaceBallistaBuildSound) return;
        if (!KuudraStateManager.isInitialized()) return;
        if (KuudraStateManager.get().phase() != KuudraPhase.BUILD) return;
        if (!packet.getSound().matchesId(SoundEvents.BLOCK_ANVIL_LAND.id())) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        log.info("Replacing ballista build sound at ({}, {}, {})", packet.getX(), packet.getY(), packet.getZ());
        client.world.playSound(
                null,
                packet.getX(), packet.getY(), packet.getZ(),
                BALLISTA_BUILD_SOUND,
                packet.getCategory(),
                packet.getVolume(),
                packet.getPitch(),
                packet.getSeed()
        );

        ci.cancel();
    }
}
