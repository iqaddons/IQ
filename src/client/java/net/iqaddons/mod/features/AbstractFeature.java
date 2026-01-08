package net.iqaddons.mod.features;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;

@Getter
public abstract class AbstractFeature implements Feature {

    protected final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public final void onEnable() {
        onActivate();
    }

    @Override
    public final void onDisable() {
        onDeactivate();
    }

    protected void register() {}
    protected void onActivate() {}
    protected void onDeactivate() {}

}
