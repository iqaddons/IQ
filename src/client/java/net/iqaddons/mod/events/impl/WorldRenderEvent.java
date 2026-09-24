package net.iqaddons.mod.events.impl;

import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.utils.render.RenderColor;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public record WorldRenderEvent(
        OrderedSubmitNodeCollector consumer,
        SubmitNodeCollector commandQueue,
        PoseStack matrices,
        CameraRenderState cameraState,
        DeltaTracker tickCounter
) implements Event {

    public void drawFilled(AABB box, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawFilled(matrices, consumer, cameraState, box, throughWalls, color);
    }

    public void drawOutline(AABB box, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawOutline(matrices, consumer, cameraState, box, throughWalls, color);
    }

    public void drawOutline(AABB box, boolean throughWalls, RenderColor color, float lineWidth) {
        WorldRenderUtils.drawOutline(matrices, consumer, cameraState, box, throughWalls, color, lineWidth);
    }

    public void drawFilledCircle(Vec3 center, float radius, int segments, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawFilledCircle(matrices, consumer, cameraState, center, radius, segments, throughWalls, color);
    }

    public void drawCircleOutline(Vec3 center, float radius, int segments, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawCircleOutline(matrices, consumer, cameraState, center, radius, segments, throughWalls, color);
    }

    public void drawThickCircleOutline(Vec3 center, float radius, float thickness, int segments, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawThickCircleOutline(matrices, consumer, cameraState, center, radius, thickness, segments, throughWalls, color);
    }

    public void drawThickSquareOutline(Vec3 center, float size, float thickness, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawThickSquareOutline(matrices, consumer, cameraState, center, size, thickness, throughWalls, color);
    }

    public void drawCircleWall(Vec3 center, float radius, float height, int segments, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawCircleWall(matrices, consumer, cameraState, center, radius, height, segments, throughWalls, color);
    }

    public void drawCircleWireframeWall(Vec3 center, float radius, float height, int segments, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawCircleWireframeWall(matrices, consumer, cameraState, center, radius, height, segments, throughWalls, color);
    }

    public void drawBillboardSquareOutline(Vec3 center, float size, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawBillboardSquareOutline(matrices, consumer, cameraState, center, size, throughWalls, color);
    }

    public void drawBillboardCircleOutline(Vec3 center, float radius, int segments, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawBillboardCircleOutline(matrices, consumer, cameraState, center, radius, segments, throughWalls, color);
    }

    public void drawThickBillboardCircleOutline(Vec3 center, float radius, float thickness, int segments, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawThickBillboardCircleOutline(matrices, consumer, cameraState, center, radius, thickness, segments, throughWalls, color);
    }

    public void drawStyledBox(@NotNull AABB box, boolean throughWalls, @NotNull RenderColor color, WorldRenderUtils.RenderStyle style) {
        WorldRenderUtils.drawStyledBox(matrices, consumer, cameraState, box, throughWalls, color, style);
    }

    public void drawText(Vec3 pos, Component text, float scale, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawText(matrices, consumer, cameraState, pos, text, scale, throughWalls, color);
    }

    public void drawBeam(Vec3 pos, int height, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawBeam(matrices, consumer, cameraState, pos, height, throughWalls, color);
    }

    public void drawFilledWithBeam(AABB box, int height, boolean throughWalls, RenderColor color) {
        WorldRenderUtils.drawFilled(matrices, consumer, cameraState, box, throughWalls, color);

        Vec3 center = box.getCenter();
        WorldRenderUtils.drawBeam(
                matrices, consumer, cameraState,
                center.add(0, box.maxY - center.y(), 0),
                height, throughWalls, color
        );
    }

    public void drawStyledWithBeam(AABB box, int height, boolean throughWalls, RenderColor color, WorldRenderUtils.RenderStyle style) {
        WorldRenderUtils.drawStyledBox(matrices, consumer, cameraState, box, throughWalls, color, style);

        Vec3 center = box.getCenter();
        WorldRenderUtils.drawBeam(
                matrices, consumer, cameraState,
                center.add(0, box.maxY - center.y(), 0),
                height, throughWalls, color
        );
    }

    public void drawTracer(Vec3 pos, RenderColor color) {
        WorldRenderUtils.drawTracer(matrices, consumer, cameraState, pos, color);
    }

    public void drawHitbox(Entity entity, boolean troughWalls, RenderColor color) {
        WorldRenderUtils.drawHitBox(matrices, consumer, cameraState, entity, tickCounter, troughWalls, color);
    }

    public void drawStyledHitbox(@NotNull Entity entity, boolean throughWalls, @NotNull RenderColor color, WorldRenderUtils.RenderStyle style) {
        WorldRenderUtils.drawStyledHitBox(matrices, consumer, cameraState, entity, tickCounter, throughWalls, color, style);
    }
}


