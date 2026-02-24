package net.iqaddons.mod.mixin.feature;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.manager.KuudraStateManager;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
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
    private static final Identifier BALLISTA_BUILD_SOUND = Identifier.of("iq", "ballista_build");

    @Unique
    private static final MinecraftClient client = MinecraftClient.getInstance();

    @Inject(
            method = "onPlaySound",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$replaceBallistaBuildSound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (KuudraStateManager.get().phase() == KuudraPhase.BUILD) {
            if (PhaseTwoConfig.cleanBallistaSounds) {
                ci.cancel();
            }

            if (PhaseTwoConfig.replaceBallistaBuildSound
                    && packet.getSound().matchesId(SoundEvents.BLOCK_ANVIL_LAND.id())
            ) {
                client.execute(() -> {
                    if (client.getSoundManager() == null) return;

                    client.getSoundManager().play(PositionedSoundInstance.ambient(
                            SoundEvent.of(BALLISTA_BUILD_SOUND),
                            packet.getPitch(),
                            0.8F)
                    );
                });

                ci.cancel();
            }
        }
    }
}
