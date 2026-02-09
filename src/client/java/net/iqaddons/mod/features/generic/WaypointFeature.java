package net.iqaddons.mod.features.generic;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.model.WaypointData;
import net.iqaddons.mod.utils.render.RenderColor;
import net.iqaddons.mod.utils.tracking.WaypointTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class WaypointFeature extends Feature {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final List<WaypointData> waypoints = new CopyOnWriteArrayList<>();

    private static final int EXPIRATION_CHECK_INTERVAL = 20;

    public WaypointFeature() {
        super("waypoints", "Waypoints",
                () -> Configuration.Waypoints.activated
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
        subscribe(WorldRenderEvent.class, this::onWorldRender);
        subscribe(ClientTickEvent.class, this::onClientTick);
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        WaypointTracker.parse(event.getStrippedMessage(), Duration.ofSeconds(Configuration.Waypoints.duration))
                .ifPresent(waypoints::add);
    }

    private void onWorldRender(WorldRenderEvent event) {
        waypoints.forEach(waypoint -> {
            var player = mc.player;
            if (player == null) return;

            var distance = waypoint.distanceFrom(player.getEntityPos());
            var distanceColor = getDistanceColor(distance);

            event.drawStyledWithBeam(Box.from(waypoint.position()), 100, true, distanceColor, Configuration.Waypoints.style);
            event.drawText(waypoint.position(),
                    waypoint.playerName(),
                    0.5f, true,
                    distanceColor
            );

            event.drawText(waypoint.position().subtract(0, -1, 0),
                    Text.of(String.format("§f%.2fm", distance)),
                    0.5f, true,
                    distanceColor
            );
        });
    }

    private void onClientTick(@NotNull ClientTickEvent event) {
        if (!event.isNthTick(EXPIRATION_CHECK_INTERVAL)) return;

        waypoints.removeIf(WaypointData::isExpired);
    }

    @Contract(pure = true)
    private @NotNull RenderColor getDistanceColor(double distance) {
        if (distance < 10) return RenderColor.fromHex(0x55FF55, 0.30f);
        if (distance < 25) return RenderColor.fromHex(0x00AA00, 0.30f);
        if (distance < 50) return RenderColor.fromHex(0xFFFF55, 0.30f);
        if (distance < 100) return RenderColor.fromHex(0xFFAA00, 0.30f);
        if (distance < 200) return RenderColor.fromHex(0xFF5555, 0.30f);
        return RenderColor.fromHex(0xAA0000, 0.30f);
    }

}
