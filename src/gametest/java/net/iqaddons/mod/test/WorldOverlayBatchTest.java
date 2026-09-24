package net.iqaddons.mod.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.iqaddons.mod.utils.render.WorldOverlayBatch;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.world.phys.Vec3;

/** Verifies that overlay work is grouped by GPU render layer, not by primitive. */
public final class WorldOverlayBatchTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        WorldOverlayBatch batch = new WorldOverlayBatch(Vec3.ZERO);
        batch.submit(WorldRenderUtils.Layers.BoxFilled, (entry, vertices) -> { });
        batch.submit(WorldRenderUtils.Layers.BoxFilled, (entry, vertices) -> { });
        batch.submit(WorldRenderUtils.Layers.CircleOutline, (entry, vertices) -> { });

        if (batch.pendingLayerCount() != 2) {
            throw new AssertionError("Expected two GPU submissions, got " + batch.pendingLayerCount());
        }
        if (batch.pendingCommandCount() != 3) {
            throw new AssertionError("Expected three queued primitives, got " + batch.pendingCommandCount());
        }
    }
}
