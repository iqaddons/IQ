package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.EntityGlowUtil;
import net.iqaddons.mod.utils.MessageUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.iqaddons.mod.utils.EntityDetectorUtil.findPlayerById;
import static net.iqaddons.mod.utils.EntityGlowUtil.PRIORITY_FRESH;

@Slf4j
public class FreshAlertFeature extends KuudraFeature {

    private static final long FRESH_DURATION_MS = 10_000;

    private final Map<Integer, Long> freshPlayers = new ConcurrentHashMap<>();

    public FreshAlertFeature() {
        super(
                "freshMessage",
                "Fresh Message",
                () -> PhaseTwoConfig.freshMessage,
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
        freshPlayers.keySet().forEach(id -> EntityGlowUtil.removeGlowing(id, PRIORITY_FRESH));
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
                EntityGlowUtil.removeGlowing(entry.getKey(), PRIORITY_FRESH);
                iterator.remove();
                log.debug("Fresh expired for entity {}", entry.getKey());
            }
        }
    }

    private void onWorldRender(@NotNull WorldRenderEvent event) {
        if (freshPlayers.isEmpty()) return;
        if (mc.world == null) return;
        if (!PhaseTwoConfig.freshTimers) return;

        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> entry : freshPlayers.entrySet()) {
            int entityId = entry.getKey();
            long startTime = entry.getValue();

            var player = findPlayerById(entityId);
            if (player == null) continue;

            long elapsed = now - startTime;
            long remaining = FRESH_DURATION_MS - elapsed;
            if (remaining <= 0) continue;

            float tickDelta = event.tickCounter().getTickProgress(true);
            double x = player.lastX + (player.getX() - player.lastX) * tickDelta;
            double y = player.lastY + (player.getY() - player.lastY) * tickDelta + 2.5;
            double z = player.lastZ + (player.getZ() - player.lastZ) * tickDelta;
            Vec3d hologramPos = new Vec3d(x, y, z);

            double remainingSeconds = remaining / 1000.0;
            var textColor = getTextRenderColor(remainingSeconds);
            event.drawText(
                    hologramPos,
                    Text.literal(String.format("%.1fs", remainingSeconds)),
                    0.05f, true, textColor
            );
        }
    }

    private void onPlayerFresh(@NotNull PlayerFreshEvent event) {
        if (event.selfFresh()) {
            MessageUtil.showTitle("§a§lFRESH!", "", 0, 20, 5);
            MessageUtil.PARTY.sendMessage("FRESH! (%d%%)".formatted(event.buildingProgress()));

            mc.world.playSound(
                    mc.player, mc.player.getBlockPos(),
                    SoundEvents.ENTITY_PLAYER_SPLASH,
                    SoundCategory.PLAYERS, 2.0f, 1.0f
            );
        }

        applyFreshEffect(event.playerEntityId(), event.playerName());
    }

    private void applyFreshEffect(int entityId, String playerName) {
        freshPlayers.put(entityId, System.currentTimeMillis());

        RenderColor freshColor = RenderColor.fromArgb(PhaseTwoConfig.freshHightlightColor);
        EntityGlowUtil.setGlowing(entityId, freshColor, PRIORITY_FRESH);

        log.debug("Applied Fresh to {} (id: {}), total: {}", playerName, entityId, freshPlayers.size());
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