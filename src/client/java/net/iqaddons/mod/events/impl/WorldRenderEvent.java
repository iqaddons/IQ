package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public record WorldRenderEvent(
        MatrixStack matrices,
        Matrix4f projectionMatrix,
        Camera camera,
        float tickDelta
) implements Event {

    public double cameraX() {
        return camera.getPos().x;
    }

    public double cameraY() {
        return camera.getPos().y;
    }

    public double cameraZ() {
        return camera.getPos().z;
    }
}


