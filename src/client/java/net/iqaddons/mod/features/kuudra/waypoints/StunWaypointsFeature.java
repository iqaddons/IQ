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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class StunWaypointsFeature extends KuudraFeature {

    private static final Vec3d ENTER_POS = new Vec3d(-161, 49, -186);
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

        Vec3d selected = PhaseThreeConfig.stunWaypointBlock.getPos();
        Vec3d renderPos = stunPhase
                ? selected.add(getInterpolatedPlayerPos(event).subtract(ENTER_POS))
                : selected;

        renderWaypoint(event, renderPos);
    }

    private @NotNull Vec3d getInterpolatedPlayerPos(@NotNull WorldRenderEvent event) {
        if (mc.player == null) return Vec3d.ZERO;

        float partialTicks = event.tickCounter().getTickProgress(true);
        double x = mc.player.lastX + (mc.player.getX() - mc.player.lastX) * partialTicks;
        double y = mc.player.lastY + (mc.player.getY() - mc.player.lastY) * partialTicks;
        double z = mc.player.lastZ + (mc.player.getZ() - mc.player.lastZ) * partialTicks;
        return new Vec3d(x, y, z);
    }

    private void renderWaypoint(@NotNull WorldRenderEvent event, @NotNull Vec3d pos) {
        float half = 1.0f / 2f;
        Box waypointBox = new Box(
                pos.getX() - half, pos.getY(), pos.getZ() - half,
                pos.getX() + half, pos.getY() + 1.0, pos.getZ() + half
        );

        event.drawStyledBox(waypointBox, true,
                RenderColor.fromArgb(PhaseThreeConfig.stunWaypointColor),
                PhaseThreeConfig.stunWaypointStyle
        );
    }

    @Getter
    @RequiredArgsConstructor
    public enum StunWaypoint {
        RIGHT_POD(new Vec3d(-168, 28, -168)),
        LEFT_POD(new Vec3d(-153, 27, -173)),
        BACK_POD(new Vec3d(-156, 28, -157)),;

        private final Vec3d pos;
    }
}
