package net.iqaddons.mod.mixin;

import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.EntitySpawnPacketEvent;
import net.iqaddons.mod.events.impl.EntityTrackingUpdateEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class EntityTrackingPacketMixin {

    @Unique
    private static final Minecraft iq$client = Minecraft.getInstance();

    @Inject(method = "handleAddEntity", at = @At("TAIL"))
    private void iq$onAddEntity(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        EventBus.post(new EntitySpawnPacketEvent(
                packet.getId(),
                packet.getType(),
                new Vec3(packet.getX(), packet.getY(), packet.getZ())
        ));
        iq$postEntityUpdate(packet.getId());
    }

    @Inject(method = "handleMoveEntity", at = @At("TAIL"))
    private void iq$onMoveEntity(ClientboundMoveEntityPacket packet, CallbackInfo ci) {
        iq$postEntityUpdate(packet.getEntity(iq$client.level));
    }

    @Inject(method = "handleTeleportEntity", at = @At("TAIL"))
    private void iq$onTeleportEntity(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
        iq$postEntityUpdate(packet.id());
    }

    @Inject(method = "handleSetEntityData", at = @At("TAIL"))
    private void iq$onSetEntityData(ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        iq$postEntityUpdate(packet.id());
    }

    @Unique
    private static void iq$postEntityUpdate(int entityId) {
        if (iq$client.level == null) return;
        iq$postEntityUpdate(iq$client.level.getEntity(entityId));
    }

    @Unique
    private static void iq$postEntityUpdate(Entity entity) {
        if (entity == null) return;
        EventBus.post(new EntityTrackingUpdateEvent(entity));
    }
}
