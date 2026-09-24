package net.iqaddons.mod.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.integration.ModMenuIntegration;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises the hooks and GPU pipelines that Java compilation cannot validate. */
public class MigrationSmokeTest implements FabricClientGameTest {
    public static volatile String surfaceCapture;

    private static void captureConfig(ClientGameTestContext context) {
        context.runOnClient(client -> {
            if (!client.gui.screen().getClass().getName().equals("net.iqaddons.mod.screen.IQConfigScreen")) {
                throw new AssertionError("IQ config screen did not open");
            }
            surfaceCapture = client.gameDirectory.toPath().resolve("iq-config-surface.png").toString();
        });
        context.waitFor(client -> surfaceCapture == null);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> MixinEnvironment.getCurrentEnvironment().audit());
        if (Boolean.getBoolean("iq.test.packaged")) {
            try (var world = context.worldBuilder().create()) {
                context.waitTicks(30);
                context.setScreen(() -> new ModMenuIntegration().getModConfigScreenFactory().create(null));
                context.waitTicks(30);
                captureConfig(context);
                context.setScreen(() -> null);
            }
            return;
        }
        AtomicInteger frames = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        var subscription = EventBus.subscribe(WorldRenderEvent.class, event -> {
            try {
                Vec3 center = event.cameraState().pos.add(0, 0, 4);
                for (boolean throughWalls : new boolean[]{false, true}) {
                    event.drawFilled(AABB.ofSize(center, 1, 1, 1), throughWalls, RenderColor.green);
                    event.drawOutline(AABB.ofSize(center, 2, 2, 2), throughWalls, RenderColor.red, 2);
                    event.drawFilledCircle(center, 2, 16, throughWalls, RenderColor.green);
                    event.drawCircleOutline(center, 2, 16, throughWalls, RenderColor.white);
                    event.drawThickCircleOutline(center, 2, 0.1f, 16, throughWalls, RenderColor.green);
                    event.drawCircleWall(center, 2, 1, 16, throughWalls, RenderColor.green);
                    event.drawBillboardSquareOutline(center, 1, throughWalls, RenderColor.white);
                    event.drawBillboardCircleOutline(center, 1, 16, throughWalls, RenderColor.white);
                    event.drawThickBillboardCircleOutline(center, 1, 0.1f, 16, throughWalls, RenderColor.green);
                    event.drawText(center, Component.literal("IQ 26.2"), 0.05f, throughWalls, RenderColor.white);
                }
                event.drawTracer(center, RenderColor.white);
                frames.incrementAndGet();
            } catch (Throwable error) {
                failure.compareAndSet(null, error);
            }
        });
        try (var world = context.worldBuilder().create()) {
            context.waitFor(client -> frames.get() >= 20 || failure.get() != null);
            if (failure.get() != null) throw new AssertionError("World overlays failed", failure.get());
            context.takeScreenshot("iq-26.2-world");
            context.setScreen(() -> new ModMenuIntegration().getModConfigScreenFactory().create(null));
            context.waitTicks(30);
            captureConfig(context);
            context.takeScreenshot("iq-26.2-config");
            context.setScreen(() -> null);
        } finally {
            subscription.unsubscribe();
        }
    }
}
