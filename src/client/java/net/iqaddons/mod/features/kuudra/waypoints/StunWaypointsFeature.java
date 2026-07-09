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
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class StunWaypointsFeature extends KuudraFeature {

    private static final Vec3 ENTER_POS = new Vec3(-161, 49, -186);
    private static final double EATEN_Y_THRESHOLD = 50.0;

    private volatile boolean stunPhase = false;
    private volatile boolean eaten = false;

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
        stunPhase = false;
        eaten = false;
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String msg = event.getStrippedMessage();
        if (msg.contains("You purchased Human Cannonball!")) {
            stunPhase = true;
            return;
        }

        if (msg.contains("destroyed one of Kuudra's pods!")) {
            stunPhase = false;
            eaten = false;
        }
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame() || mc.player == null) return;
        if (mc.player.getY() < EATEN_Y_THRESHOLD && stunPhase) {
            stunPhase = false;
            eaten = true;
        }
    }

    private void onRender(@NotNull WorldRenderEvent event) {
        if (!eaten && !stunPhase) return;
        if (mc.player == null) return;

        Vec3 selected = PhaseThreeConfig.stunWaypointBlock.getPos();
        Vec3 renderPos = stunPhase
                ? selected.add(getInterpolatedPlayerPos(event).subtract(ENTER_POS))
                : selected;

        renderWaypoint(event, renderPos);
    }

    private @NotNull Vec3 getInterpolatedPlayerPos(@NotNull WorldRenderEvent event) {
        if (mc.player == null) return Vec3.ZERO;

        float partialTicks = event.tickCounter().getGameTimeDeltaPartialTick(true);
        double x = mc.player.xo + (mc.player.getX() - mc.player.xo) * partialTicks;
        double y = mc.player.yo + (mc.player.getY() - mc.player.yo) * partialTicks;
        double z = mc.player.zo + (mc.player.getZ() - mc.player.zo) * partialTicks;
        return new Vec3(x, y, z);
    }

    private void renderWaypoint(@NotNull WorldRenderEvent event, @NotNull Vec3 pos) {
        float half = 1.0f / 2f;
        AABB waypointBox = new AABB(
                pos.x() - half, pos.y(), pos.z() - half,
                pos.x() + half, pos.y() + 1.0, pos.z() + half
        );

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
        BACK_POD(new Vec3(-154.5, 28, -156.5)),
        ;

        private final Vec3 pos;
    }
}
