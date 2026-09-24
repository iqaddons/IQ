package net.iqaddons.mod.gametest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyProgressEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyDropEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.kuudra.alerts.SupplyGiantHitboxAlertFeature;
import net.iqaddons.mod.utils.render.WorldOverlayBatch;
import net.iqaddons.mod.utils.render.WorldOverlayLayerPolicy;
import net.iqaddons.mod.utils.render.WorldOverlayPhaseSubmitter;
import net.iqaddons.mod.utils.render.WorldRenderUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.iqaddons.mod.features.kuudra.waypoints.SupplyWaypointsFeature;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Hitboxes track collision continuously; subtitles require active pickup progress. */
public final class SupplyHitboxTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (Boolean.getBoolean("iq.test.packaged")) return;
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> {
                var feature = new SupplyGiantHitboxAlertFeature();
                var oldStyle = PhaseOneConfig.supplyGiantHitboxStyle;
                var oldPos = client.player.position();
                var giant = new Giant(EntityTypes.GIANT, client.level);
                giant.setId(987654);
                giant.setPos(0, 60, 0);
                giant.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.PLAYER_HEAD));
                client.level.addEntity(giant);
                feature.activate();
                invoke(feature, "onKuudraActivate", new Class<?>[0]);
                try {
                    client.gui.hud.clearTitles();
                    client.player.setPos(0, 60, 0);
                    EventBus.post(ClientTickEvent.create(client));
                    check((int) field(feature, "highlightedGiantId") == giant.getId(),
                            "Entering a supply giant must highlight without a pickup event");
                    check(field(feature, "highlightedAlertLevel").toString().equals("PRIMARY"),
                            "Eyes inside must take priority over body intersection");
                    check(field(client.gui.hud, "subtitle") == null, "Entering a giant without collecting must not show subtitles");
                    EventBus.post(new SupplyProgressEvent(null, null, "10%", 10));
                    check(((Component) field(client.gui.hud, "subtitle")).getString().equals("§cNeed Double Pearl"), "Starting collection while already inside must show the eye subtitle");
                    check(((Component) field(client.gui.hud, "title")).getString().isEmpty(), "Alert must not occupy the title");
                    for (int tick = 0; tick < 80; tick++) {
                        client.gui.hud.tick(false);
                        EventBus.post(ClientTickEvent.create(client));
                        check((int) field(client.gui.hud, "titleTime") > 0, "Subtitle must stay visible beyond its old timeout while collecting inside");
                    }
                    var camera = new CameraRenderState();
                    camera.pos = Vec3.ZERO;
                    int[] outlines = {0};
                    int[] outlineColor = {0};
                    var collector = (OrderedSubmitNodeCollector) Proxy.newProxyInstance(
                            OrderedSubmitNodeCollector.class.getClassLoader(), new Class<?>[]{OrderedSubmitNodeCollector.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("submitShapeOutline")) {
                                    outlines[0]++;
                                    outlineColor[0] = (int) args[3];
                                }
                                return null;
                            });
                    var event = new WorldRenderEvent(collector, null, new PoseStack(), camera, client.getDeltaTracker());
                    for (var style : WorldRenderUtils.RenderStyle.values()) {
                        PhaseOneConfig.supplyGiantHitboxStyle = style;
                        var batch = WorldOverlayBatch.begin(Vec3.ZERO);
                        outlines[0] = 0;
                        try {
                            invoke(feature, "onRender", new Class<?>[]{WorldRenderEvent.class}, event);
                            check(batch.pendingCommandCount() + outlines[0] == (style == WorldRenderUtils.RenderStyle.BOTH ? 2 : 1),
                                    "Each style must render one box, without expanded duplicate outlines: " + style);
                            check(outlines[0] == 0,
                                    "Through-wall outlines must use the batched always-on-top phase: " + style);
                        } finally {
                            WorldOverlayBatch.end(batch);
                        }
                    }
                    client.player.setPos(giant.getBoundingBox().maxX + 0.1, 60, 0);
                    EventBus.post(ClientTickEvent.create(client));
                    check(field(feature, "highlightedAlertLevel").toString().equals("SECONDARY"),
                            "Body intersection with eyes outside must give the possible warning");
                    check(((Component) field(client.gui.hud, "subtitle")).getString().equals("§fPossible Double Pearl"), "Moving from eyes to body must update the persistent subtitle immediately");
                    EventBus.post(new SupplyProgressEvent(null, null, "20%", 20));
                    check(((Component) field(client.gui.hud, "subtitle")).getString().equals("§fPossible Double Pearl"), "Body warning belongs in subtitle during collection");
                    EventBus.post(new SupplyProgressEvent(null, null, "100%", 100));
                    EventBus.post(ClientTickEvent.create(client));
                    check(subtitleEmpty(client.gui.hud), "Finished collection must not issue a warning");
                    check((int) field(feature, "highlightedGiantId") == giant.getId(), "Finishing collection must keep the collision highlight");
                    EventBus.post(new SupplyProgressEvent(null, null, "0%", 0));
                    check(subtitleEmpty(client.gui.hud), "Inactive collection must not issue a warning");
                    EventBus.post(new SupplyProgressEvent(null, null, "5%", 5));
                    check(((Component) field(client.gui.hud, "subtitle")).getString().equals("§fPossible Double Pearl"), "A new collection must warn even with unchanged collision");
                    client.player.setPos(20, 60, 20);
                    EventBus.post(ClientTickEvent.create(client));
                    check((int) field(feature, "highlightedGiantId") == -1,
                            "Leaving the box must clear the highlight without a pickup event");
                    check(subtitleEmpty(client.gui.hud), "Leaving must remove the subtitle immediately");
                    EventBus.post(new SupplyDropEvent(client.player.getName().getString()));
                    client.player.setPos(0, 60, 0);
                    EventBus.post(ClientTickEvent.create(client));
                    check((int) field(feature, "highlightedGiantId") == giant.getId(), "Re-entry must trigger again");
                    check(subtitleEmpty(client.gui.hud), "Re-entry without collecting must remain silent");
                    EventBus.post(new SupplyProgressEvent(null, null, "10%", 10));
                    EventBus.post(new SupplyDropEvent("AnotherPlayer"));
                    EventBus.post(ClientTickEvent.create(client));
                    check(!subtitleEmpty(client.gui.hud), "Another player's drop must not end this collection");
                    EventBus.post(new SupplyDropEvent(client.player.getName().getString()));
                    check(subtitleEmpty(client.gui.hud), "Aborting collection must remove the subtitle immediately");
                    EventBus.post(new SupplyProgressEvent(null, null, "10%", 10));
                    client.gui.hud.setSubtitle(Component.literal("Another alert"));
                    EventBus.post(new SupplyDropEvent(client.player.getName().getString()));
                    check(((Component) field(client.gui.hud, "subtitle")).getString().equals("Another alert"), "Cleanup must preserve a subtitle that replaced ours");
                    giant.discard();
                    EventBus.post(ClientTickEvent.create(client));
                    check((int) field(feature, "highlightedGiantId") == -1, "Removed giants must not remain highlighted");
                    // Interaction range is geometric and must not depend on the current crosshair target.
                    var waypoint = new SupplyWaypointsFeature();
                    AABB geometricBox = new AABB(3.5, 0, -0.5, 4.5, 10, 0.5);
                    AABB geometricRange = (AABB) invokeResult(waypoint, "getInteractionRangeSection",
                            new Class<?>[]{AABB.class, Vec3.class, double.class}, geometricBox, new Vec3(0, 5, 0), 4.0);
                    double verticalReach = Math.sqrt(4.0 * 4.0 - 3.5 * 3.5);
                    check(close(geometricRange.minY, 5.0 - verticalReach)
                                    && close(geometricRange.maxY, 5.0 + verticalReach),
                            "Range section must be clipped from eye distance, not player hitbox distance");
                    check(invokeResult(waypoint, "getInteractionRangeSection",
                                    new Class<?>[]{AABB.class, Vec3.class, double.class}, geometricBox, new Vec3(-0.5, 5, 0), 4.0) == null,
                            "A box exactly at the strict vanilla reach boundary must remain out of range");
                    var zombie = new Zombie(EntityTypes.ZOMBIE, client.level);
                    zombie.setPos(0, 60, 1);
                    zombie.xo = zombie.getX();
                    zombie.yo = zombie.getY();
                    zombie.zo = zombie.getZ();
                    var oldHit = client.hitResult;
                    boolean oldRange = PhaseOneConfig.SupplyWaypointsConfig.supplyInteractionBoxRange;
                    try {
                        for (int scenario = 0; scenario < 4; scenario++) {
                            PhaseOneConfig.SupplyWaypointsConfig.supplyInteractionBoxRange = scenario != 1;
                            int expectedBoxes = 1;
                            if (scenario <= 1) {
                                zombie.setPos(0, 60, 1);
                            } else if (scenario == 2) {
                                zombie.setPos(client.player.entityInteractionRange() + zombie.getBbWidth(), 60, 0);
                            } else {
                                double targetVerticalReach = 0.5;
                                double reach = client.player.entityInteractionRange();
                                double nearestHorizontal = Math.sqrt(reach * reach - targetVerticalReach * targetVerticalReach);
                                zombie.setPos(
                                        nearestHorizontal + zombie.getBbWidth() / 2.0,
                                        client.player.getEyeY() - zombie.getBbHeight() / 2.0,
                                        0
                                );
                                expectedBoxes = 3;
                            }
                            zombie.xo = zombie.getX();
                            zombie.yo = zombie.getY();
                            zombie.zo = zombie.getZ();
                            client.hitResult = null;
                            outlines[0] = 0;
                            var phaseCapture = new GeometryPhaseCapture();
                            var batch = WorldOverlayBatch.begin(Vec3.ZERO, phaseCapture);
                            try {
                                invoke(waypoint, "renderInteractionHitbox", new Class<?>[]{WorldRenderEvent.class, Zombie.class, RenderColor.class, RenderColor.class},
                                        event, zombie, RenderColor.red, RenderColor.green);
                                check(outlines[0] == 0 && batch.pendingCommandCount() == expectedBoxes * 2,
                                        "Interaction box fill and outline must both use the through-wall batch");
                                batch.flush(collector);
                                check(phaseCapture.phases.size() == 2
                                                && phaseCapture.phases.stream().allMatch(phase -> phase == WorldOverlayLayerPolicy.RenderPhase.ALWAYS_ON_TOP),
                                        "Interaction box must remain visible behind walls");
                                check(phaseCapture.colors.contains(scenario == 0 || scenario == 3 ? RenderColor.green.argb : RenderColor.red.argb),
                                        "A nearby supply must use the range color without requiring aim: " + scenario);
                                if (scenario == 3) {
                                    check(phaseCapture.colors.contains(RenderColor.red.argb),
                                            "A partially reachable supply must retain normal-colored outer segments");
                                }
                            } finally { WorldOverlayBatch.end(batch); }
                        }
                    } finally {
                        client.hitResult = oldHit;
                        PhaseOneConfig.SupplyWaypointsConfig.supplyInteractionBoxRange = oldRange;
                    }
                } finally {
                    feature.deactivate();
                    giant.discard();
                    client.player.setPos(oldPos);
                    PhaseOneConfig.supplyGiantHitboxStyle = oldStyle;
                }
            });
        }
    }

    private static boolean subtitleEmpty(Object hud) {
        Component subtitle = (Component) field(hud, "subtitle");
        return subtitle == null || subtitle.getString().isEmpty();
    }

    private static final class GeometryPhaseCapture implements WorldOverlayPhaseSubmitter {
        private final List<WorldOverlayLayerPolicy.RenderPhase> phases = new ArrayList<>();
        private final List<Integer> colors = new ArrayList<>();
        private final VertexConsumer vertices = (VertexConsumer) Proxy.newProxyInstance(
                VertexConsumer.class.getClassLoader(), new Class<?>[]{VertexConsumer.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setColor")) {
                        if (args.length == 1) {
                            colors.add((int) args[0]);
                        } else if (args.length == 4) {
                            boolean normalized = args[0] instanceof Float;
                            int r = colorChannel((Number) args[0], normalized);
                            int g = colorChannel((Number) args[1], normalized);
                            int b = colorChannel((Number) args[2], normalized);
                            int a = colorChannel((Number) args[3], normalized);
                            colors.add((a << 24) | (r << 16) | (g << 8) | b);
                        }
                    }
                    return method.getReturnType() == VertexConsumer.class ? proxy : null;
                }
        );

        @Override
        public void iq$submitCustomGeometry(
                PoseStack matrices,
                RenderType renderType,
                SubmitNodeCollector.CustomGeometryRenderer renderer,
                WorldOverlayLayerPolicy.RenderPhase phase
        ) {
            phases.add(phase);
            renderer.render(matrices.last(), vertices);
        }

        @Override
        public void iq$submitText(
                PoseStack matrices, float x, float y, FormattedCharSequence text, boolean dropShadow,
                Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor,
                WorldOverlayLayerPolicy.RenderPhase phase
        ) {
            throw new AssertionError("Interaction box must not submit text");
        }

        private static int colorChannel(Number value, boolean normalized) {
            return normalized ? Math.round(value.floatValue() * 255.0f) : value.intValue();
        }
    }

    private static Object field(Object target, String name) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void invoke(Object target, String name, Class<?>[] types, Object... args) {
        invokeResult(target, name, types, args);
    }

    private static Object invokeResult(Object target, String name, Class<?>[] types, Object... args) {
        try {
            var method = target.getClass().getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 1.0E-9;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
