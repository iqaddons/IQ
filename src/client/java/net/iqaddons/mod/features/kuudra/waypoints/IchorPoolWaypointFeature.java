package net.iqaddons.mod.features.kuudra.waypoints;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IchorPoolWaypointFeature extends Feature {

    private static final Pattern ICHOR_LOCATION_PATTERN = Pattern.compile(
            "(?i).*\\bcasting(?:\\s+spell:)?\\s+ichor\\s+pool!?\\s*(?:at|@)?\\s*\\(?\\s*(-?\\d+)\\s*[, ]+\\s*(-?\\d+)\\s*[, ]+\\s*(-?\\d+)\\s*\\)?.*"
    );

    private static final float ICHOR_RADIUS = 8.0F;
    private static final long ICHOR_DURATION_MS = 20_000L;
    private static final int CIRCLE_SEGMENTS = 64;
    private static final RenderColor ICHOR_COLOR = new RenderColor(96, 238, 255, 210);

    private final List<IchorPoolArea> activePools = new ArrayList<>();

    public IchorPoolWaypointFeature() {
        super(
                "ichorPoolArea",
                "Ichor Pool Area",
                () -> KuudraGeneralConfig.AbilityAnnounce.ichorPool
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
        subscribe(WorldRenderEvent.class, this::onWorldRender);
    }

    @Override
    protected void onDeactivate() {
        activePools.clear();
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        Matcher matcher = ICHOR_LOCATION_PATTERN.matcher(event.getStrippedMessage());
        if (!matcher.matches()) return;

        double x = Double.parseDouble(matcher.group(1));
        double y = Double.parseDouble(matcher.group(2));
        double z = Double.parseDouble(matcher.group(3));

        activePools.add(new IchorPoolArea(new Vec3d(x + 0.5, y + 0.05, z + 0.5), System.currentTimeMillis() + ICHOR_DURATION_MS));
    }

    private void onWorldRender(@NotNull WorldRenderEvent event) {
        long now = System.currentTimeMillis();

        Iterator<IchorPoolArea> iterator = activePools.iterator();
        while (iterator.hasNext()) {
            IchorPoolArea pool = iterator.next();
            if (pool.expiresAtMs() <= now) {
                iterator.remove();
                continue;
            }

            event.drawCircleOutline(
                    pool.center(), ICHOR_RADIUS,
                    CIRCLE_SEGMENTS, true,
                    ICHOR_COLOR
            );
        }
    }

    private record IchorPoolArea(Vec3d center, long expiresAtMs) {
    }
}