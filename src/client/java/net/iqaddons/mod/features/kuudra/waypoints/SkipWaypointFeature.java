package net.iqaddons.mod.features.kuudra.waypoints;

import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.model.etherwarp.EtherwarpCategory;
import net.iqaddons.mod.model.etherwarp.EtherwarpWaypoint;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.KuudraStunTracker;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class SkipWaypointFeature extends AbstractEtherwarpWaypointFeature {

    private static final float STOMACH_OUTLINE_WIDTH = 3.0f;

    private final KuudraStunTracker stunTracker = new KuudraStunTracker();

    public SkipWaypointFeature() {
        super(
                "skipWaypoint",
                "SKIP Waypoint",
                () -> PhaseThreeConfig.skipWaypoint,
                KuudraPhase.EATEN, KuudraPhase.STUN, KuudraPhase.DPS
        );
    }

    @Override
    protected void onKuudraActivate() {
        super.onKuudraActivate();
        subscribe(ChatReceivedEvent.class, this::onChat);
        subscribe(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onKuudraDeactivate() {
        super.onKuudraDeactivate();
        stunTracker.resetRun();
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        stunTracker.onChat(event.getStrippedMessage());
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || mc.player == null) return;
        stunTracker.onTick();
    }

    @Override
    protected boolean shouldRenderCategory(@NotNull EtherwarpCategory category, @NotNull KuudraPhase phase) {
        return categoryContains(category, "skip");
    }

    @Override
    protected boolean shouldRenderWaypointInPhase(
            @NotNull EtherwarpCategory category,
            @NotNull EtherwarpWaypoint waypoint,
            @NotNull KuudraPhase phase
    ) {
        return super.shouldRenderWaypointInPhase(category, waypoint, phase)
                || (phase == KuudraPhase.EATEN && categoryContains(category, "skip"));
    }

    @Override
    protected void renderWaypoint(
            @NotNull WorldRenderEvent event,
            @NotNull EtherwarpCategory category,
            @NotNull EtherwarpWaypoint waypoint,
            @NotNull Vec3 position,
            int colorIndex
    ) {
        if (!stunTracker.isStunner()) return;

        if (stunTracker.isInStomach()) {
            renderStomachWaypoint(event, waypoint, position);
            return;
        }

        KuudraWaypointRenderSupport.render(
                event,
                waypoint,
                position,
                RenderColor.fromArgb(PhaseThreeConfig.SkipWaypointConfig.waypointColor),
                PhaseThreeConfig.SkipWaypointConfig.showText,
                PhaseThreeConfig.SkipWaypointConfig.textSize
        );

        if (PhaseThreeConfig.SkipWaypointConfig.waypointTracer) {
            event.drawTracer(position, RenderColor.fromArgb(PhaseThreeConfig.SkipWaypointConfig.waypointTracerColor));
        }
    }

    private void renderStomachWaypoint(
            @NotNull WorldRenderEvent event,
            @NotNull EtherwarpWaypoint waypoint,
            @NotNull Vec3 position
    ) {
        AABB box = waypoint.getRenderBox(position);
        Vec3 center = box.getCenter();
        RenderColor cyan = RenderColor.fromHex(0x00ffff);
        RenderColor white = RenderColor.white;

        event.drawFilled(box, true, cyan.withOpacity(0.18f));
        event.drawOutline(box, true, cyan.withOpacity(0.85f), STOMACH_OUTLINE_WIDTH);
        event.drawBillboardSquareOutline(center, 1.05f, true, white.withOpacity(0.70f));
        event.drawTracer(center, white.withOpacity(0.80f));
    }
}
