package net.iqaddons.mod.mixin.accessor;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftClientAccessor {

    @Invoker("startAttack")
    boolean invokeDoAttack();

    @Invoker("startUseItem")
    void invokeDoItemUse();
}