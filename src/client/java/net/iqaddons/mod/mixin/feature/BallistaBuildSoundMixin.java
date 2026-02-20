package net.iqaddons.mod.mixin.feature;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.IQConstants;
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

    @Inject(
            method = "onPlaySound",
            at = @At("HEAD"),
            cancellable = true
    )
    private void iq$replaceBallistaBuildSound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (KuudraStateManager.get().phase() != KuudraPhase.BUILD) return;
        if (PhaseTwoConfig.luckyBuild) {
            if (!packet.getSound().matchesId(IQConstants.LUCKY_BUILD_SOUND)) {
                ci.cancel();
            }
        }

        if (!PhaseTwoConfig.replaceBallistaBuildSound || !KuudraStateManager.isInitialized()) return;
        if (packet.getSound().matchesId(SoundEvents.BLOCK_ANVIL_LAND.id())) {
            MinecraftClient client = MinecraftClient.getInstance();

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
