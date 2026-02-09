package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.utils.render.RenderColor;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public record WorldRenderEvent(
        VertexConsumerProvider.Immediate consumer,
        OrderedRenderCommandQueue commandQueue,
        MatrixStack matrices,
        Matrix4f projectionMatrix,
        Camera camera,
        RenderTickCounter tickCounter
) implements Event {

    public void drawFilled(Box box, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawFilled(matrices, consumer, camera, box, throughWalls, color);
    }

    public void drawOutline(Box box, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawOutline(matrices, consumer, camera, box, throughWalls, color);
    }

    public void drawText(Vec3d pos, Text text, float scale, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawText(matrices, consumer, camera, pos, text, scale, throughWalls, color);
    }

    public void drawBeam(Vec3d pos, int height, RenderColor color) {
        WorldRenderUtils.drawBeam(this, pos, height, color);
    }

    public void drawFilledWithBeam(Box box, int height, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawFilled(matrices, consumer, camera, box, throughWalls, color);
        Vec3d center = box.getCenter();
        WorldRenderUtils.drawBeam(
                this,
                center.add(0, box.maxY - center.getY(), 0),
                height, color
        );
    }

    public void drawTracer(Vec3d pos, RenderColor color) {
        WorldRenderUtils.drawTracer(matrices, consumer, camera, pos, color);
    }

    public void drawHitbox(Entity entity, boolean troughWalls, RenderColor color) {
        WorldRenderUtils.drawHitBox(matrices, consumer, camera, entity, tickCounter, troughWalls, color);
    }
}


