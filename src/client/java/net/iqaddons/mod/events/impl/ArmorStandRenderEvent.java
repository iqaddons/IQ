package net.iqaddons.mod.events.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.iqaddons.mod.events.Cancellable;
import net.iqaddons.mod.events.Event;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;

@Getter
@RequiredArgsConstructor
public class ArmorStandRenderEvent implements Event, Cancellable {

    private final ArmorStandEntityRenderState renderState;
    private final MatrixStack matrices;
    private final OrderedRenderCommandQueue queue;
    private final CameraRenderState camera;

    @Setter
    private boolean cancelled = false;

}
