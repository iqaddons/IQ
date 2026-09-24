package net.iqaddons.mod.features.kuudra.waypoints;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseThreeConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.KuudraStunTracker;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class StunWaypointsFeature extends KuudraFeature {

    private static final float STOMACH_OUTLINE_WIDTH = 3.0f;

    private final KuudraStunTracker stunTracker = new KuudraStunTracker();

    public StunWaypointsFeature() {
        super(
                "stunWaypoints",
                "Stun Waypoints",
                () -> PhaseThreeConfig.stunWaypoints,
                KuudraPhase.BUILD, KuudraPhase.STUN,
                KuudraPhase.EATEN, KuudraPhase.DPS
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(ChatReceivedEvent.class, this::onChat);
        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(WorldRenderEvent.class, this::onRender);
    }

    @Override
    protected void onKuudraDeactivate() {
        stunTracker.resetRun();
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        stunTracker.onChat(event.getStrippedMessage());
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || mc.player == null) return;
        stunTracker.onTick();
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        boolean inStomach = stunTracker.isInStomach();
        if (!stunTracker.isEaten() && !stunTracker.isStunPhase() && !inStomach) return;
        if (mc.player == null) return;

        Vec3 selected = PhaseThreeConfig.stunWaypointBlock.getPos();
        Vec3 renderPos = inStomach
                ? selected
                : stunTracker.isStunPhase()
                ? selected.add(getInterpolatedPlayerPos(event).subtract(KuudraStunTracker.ENTER_POS))
                : selected;

        renderWaypoint(event, renderPos, inStomach || stunTracker.isStunPhase());
    }

    private @NotNull Vec3 getInterpolatedPlayerPos(@NotNull WorldRenderEvent event) {
        if (mc.player == null) return Vec3.ZERO;

        float partialTicks = event.tickCounter().getGameTimeDeltaPartialTick(true);
        double x = mc.player.xo + (mc.player.getX() - mc.player.xo) * partialTicks;
        double y = mc.player.yo + (mc.player.getY() - mc.player.yo) * partialTicks;
        double z = mc.player.zo + (mc.player.getZ() - mc.player.zo) * partialTicks;
        return new Vec3(x, y, z);
    }

    private void renderWaypoint(@NotNull WorldRenderEvent event, @NotNull Vec3 pos, boolean highVisibility) {
        float half = 1.0f / 2f;
        AABB waypointBox = new AABB(
                pos.x() - half, pos.y(), pos.z() - half,
                pos.x() + half, pos.y() + 1.0, pos.z() + half
        );

        if (highVisibility) {
            RenderColor cyan = RenderColor.fromHex(0x00ffff);
            RenderColor white = RenderColor.white;
            Vec3 center = waypointBox.getCenter();
            event.drawFilled(waypointBox, true, cyan.withOpacity(0.18f));
            event.drawOutline(waypointBox, true, cyan.withOpacity(0.85f), STOMACH_OUTLINE_WIDTH);
            event.drawBillboardSquareOutline(center, 1.05f, true, white.withOpacity(0.70f));
            return;
        }

        event.drawStyledBox(waypointBox, true,
                RenderColor.fromArgb(PhaseThreeConfig.stunWaypointColor),
                PhaseThreeConfig.stunWaypointStyle
        );
    }

    @Getter
    @RequiredArgsConstructor
    public enum StunWaypoint {
        RIGHT_POD(new Vec3(-167.5, 28, -167.5)),
        LEFT_POD(new Vec3(-152.5, 27, -172.5)),
        BACK_POD(new Vec3(-154.5, 29, -156.5)),;

        private final Vec3 pos;
    }
}
