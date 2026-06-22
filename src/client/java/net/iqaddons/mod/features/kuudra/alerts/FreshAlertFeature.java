package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.iqaddons.mod.utils.EntityDetectorUtil.findPlayerById;

@Slf4j
public class FreshAlertFeature extends KuudraFeature {

    private static final long FRESH_DURATION_MS = 10_000;

    private final Map<Integer, Long> freshPlayers = new ConcurrentHashMap<>();

    public FreshAlertFeature() {
        super(
                "freshCountdown",
                "Fresh Countdown",
                () -> PhaseTwoConfig.freshCountdown || PhaseTwoConfig.freshMessage,
                KuudraPhase.BUILD
        );
    }

    @Override
    protected void onKuudraActivate() {
        freshPlayers.clear();

        subscribe(PlayerFreshEvent.class, this::onPlayerFresh);
        subscribe(ClientTickEvent.class, this::onTick);
        subscribe(WorldRenderEvent.class, this::onWorldRender);
    }

    @Override
    protected void onKuudraDeactivate() {
        freshPlayers.clear();
    }

    private void onTick(@NotNull ClientTickEvent event) {
        if (!event.isInGame()) return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, Long>> iterator = freshPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Long> entry = iterator.next();
            long elapsed = now - entry.getValue();

            if (elapsed >= FRESH_DURATION_MS) {
                iterator.remove();
                log.debug("Fresh expired for entity {}", entry.getKey());
            }
        }
    }

    private void onWorldRender(@NotNull WorldRenderEvent event) {
        if (freshPlayers.isEmpty()) return;
        if (mc.level == null) return;
        if (!PhaseTwoConfig.freshCountdown) return;

        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> entry : freshPlayers.entrySet()) {
            int entityId = entry.getKey();
            long startTime = entry.getValue();

            var player = findPlayerById(entityId);
            if (player == null) continue;

            long elapsed = now - startTime;
            long remaining = FRESH_DURATION_MS - elapsed;
            if (remaining <= 0) continue;

            float tickDelta = event.tickCounter().getGameTimeDeltaPartialTick(true);
            double x = player.xo + (player.getX() - player.xo) * tickDelta;
            double y = player.yo + (player.getY() - player.yo) * tickDelta + 2.5;
            double z = player.zo + (player.getZ() - player.zo) * tickDelta;
            Vec3 hologramPos = new Vec3(x, y, z);

            double remainingSeconds = remaining / 1000.0;
            var textColor = getTextRenderColor(remainingSeconds);
            event.drawText(
                    hologramPos,
                    Component.literal(String.format("%.1fs", remainingSeconds)),
                    0.05f, true, textColor
            );
        }
    }

    private void onPlayerFresh(@NotNull PlayerFreshEvent event) {
        if (event.selfFresh() && PhaseTwoConfig.freshMessage) {
            MessageUtil.showTitle("§a§lFRESH!", "", 0, 20, 5);
            MessageUtil.PARTY.sendMessage("FRESH! (%d%%)".formatted(event.buildingProgress()));

            mc.level.playSound(
                    mc.player, mc.player.blockPosition(),
                    SoundEvents.PLAYER_SPLASH,
                    SoundSource.PLAYERS, 2.0f, 1.0f
            );
        }

        trackFreshPlayer(event.playerEntityId(), event.playerName());
    }

    private void trackFreshPlayer(int entityId, String playerName) {
        freshPlayers.put(entityId, System.currentTimeMillis());
        log.debug("Tracking fresh timer for {} (id: {}), total: {}", playerName, entityId, freshPlayers.size());
    }

    @NotNull
    private RenderColor getTextRenderColor(double remainingSeconds) {
        if (remainingSeconds > 6.0) {
            return RenderColor.fromHex(0x55FF55);
        } else if (remainingSeconds > 3.0) {
            return RenderColor.fromHex(0xFFFF55);
        } else {
            return RenderColor.fromHex(0xFF5555);
        }
    }
}