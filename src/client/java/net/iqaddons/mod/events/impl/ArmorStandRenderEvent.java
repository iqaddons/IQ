package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.state.level.CameraRenderState;

@Getter
@RequiredArgsConstructor
public class ArmorStandRenderEvent implements Event, Cancellable {

    private final ArmorStandRenderState renderState;
    private final PoseStack matrices;
    private final SubmitNodeCollector queue;
    private final CameraRenderState camera;

    @Setter
    private boolean cancelled = false;

}
