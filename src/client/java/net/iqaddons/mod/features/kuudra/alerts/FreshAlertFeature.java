package net.iqaddons.mod.features.kuudra.alerts;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.ClientTickEvent;
import net.iqaddons.mod.events.impl.WorldRenderEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.render.EntityGlowUtil;
import net.iqaddons.mod.utils.render.RenderColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.iqaddons.mod.utils.render.EntityGlowUtil.PRIORITY_FRESH;

@Slf4j
public class FreshAlertFeature extends KuudraFeature {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final String FRESH_TOOLS_MESSAGE = "Your Fresh Tools Perk bonus doubles your building speed for the next 10 seconds!";
    private static final Pattern PROTECT_ELLE_PATTERN = Pattern.compile("Protect Elle\\s*\\((\\d+)%\\)");
    private static final Pattern PARTY_FRESH_PATTERN = Pattern.compile("Party > (?:\\[[^]]+] )?(\\w+): (?:\\[IQ] )?FRESH!");
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
        subscribe(EventBus.subscribe(ChatReceivedEvent.class, this::onChat));
        subscribe(EventBus.subscribe(ClientTickEvent.class, this::onTick));
        subscribe(EventBus.subscribe(WorldRenderEvent.class, this::onWorldRender));
        log.info("Fresh Message activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        freshPlayers.keySet().forEach(id -> EntityGlowUtil.removeGlowing(id, PRIORITY_FRESH));
        freshPlayers.clear();
        log.info("Fresh Message deactivated");
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

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        if (message.contains(FRESH_TOOLS_MESSAGE)) {
            ClientPlayerEntity player = mc.player;
            if (player == null) return;

            int buildProgress = getBuildingProgress();
            MessageUtil.PARTY.sendMessage("FRESH! (%d%%)".formatted(buildProgress));

            applyFreshEffect(player.getId(), player.getName().getString());
            return;
        }

        Matcher partyMatcher = PARTY_FRESH_PATTERN.matcher(message);
        if (partyMatcher.find()) {
            String playerName = partyMatcher.group(1);
            log.debug("Detected FRESH from: {}", playerName);

            if (mc.player != null && playerName.equalsIgnoreCase(mc.player.getName().getString())) {
                return;
            }

            findPlayerByName(playerName).ifPresentOrElse(
                    player -> {
                        applyFreshEffect(player.getId(), playerName);
                    },
                    () -> log.warn("Player '{}' not found in world", playerName)
            );
        }
    }

    private void applyFreshEffect(int entityId, String playerName) {
        freshPlayers.put(entityId, System.currentTimeMillis());

        RenderColor freshColor = RenderColor.fromArgb(PhaseTwoConfig.freshHightlightColor);
        EntityGlowUtil.setGlowing(entityId, freshColor, PRIORITY_FRESH);

        log.debug("Applied Fresh to {} (id: {}), total: {}", playerName, entityId, freshPlayers.size());
    }

    private @Nullable AbstractClientPlayerEntity findPlayerById(int entityId) {
        if (mc.world == null) return null;
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player.getId() == entityId) {
                return player;
            }
        }
        return null;
    }

    private Optional<AbstractClientPlayerEntity> findPlayerByName(@NotNull String name) {
        if (mc.world == null) return Optional.empty();

        return mc.world.getPlayers().stream()
                .filter(player -> player.getName().getString().equalsIgnoreCase(name))
                .findFirst();
    }

    private int getBuildingProgress() {
        for (String line : ScoreboardUtils.getLines()) {
            String stripped = ScoreboardUtils.stripFormatting(line);

            Matcher matcher = PROTECT_ELLE_PATTERN.matcher(stripped);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse progress: {}", stripped);
                }
            }
        }
        return 0;
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