package net.iqaddons.mod.utils.tracking;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.events.impl.skyblock.SkyBlockStatusEvent;
import net.iqaddons.mod.events.impl.skyblock.SupplyPickupEvent;
import net.iqaddons.mod.state.KuudraStateManager;
import net.iqaddons.mod.state.SupplyStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.StringUtils;
import net.iqaddons.mod.utils.TextFormatUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.iqaddons.mod.utils.EntityDetectorUtil.findPlayerByName;

@Slf4j
public final class KuudraTracker {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final String KUUDRA_AREA = "Kuudra";

    private static final Pattern SUPPLY_PATTERN = Pattern.compile("(.+) recovered one of Elle's supplies! \\((\\d)/6\\)");

    private static final String FRESH_TOOLS_MESSAGE = "Your Fresh Tools Perk bonus doubles your building speed for the next 10 seconds!";
    private static final Pattern PROTECT_ELLE_PATTERN = Pattern.compile("Protect Elle\\s*\\((\\d+)%\\)");
    private static final Pattern PARTY_FRESH_PATTERN = Pattern.compile("Party > (?:\\[[^]]+] )?(\\w+): (?:\\[IQ] )?FRESH!");

    private final SupplyStateManager supplyState = SupplyStateManager.get();
    private final KuudraStateManager stateManager;
    private final SkyBlockTracker skyBlockTracker;

    public KuudraTracker(@NotNull SkyBlockTracker skyBlockTracker) {
        this.skyBlockTracker = skyBlockTracker;
        this.stateManager = KuudraStateManager.get();
    }

    public void start() {
        EventBus.subscribe(
                ChatReceivedEvent.class,
                this::onChat
        );

        EventBus.subscribe(
                SkyBlockStatusEvent.class,
                this::onSkyBlockStatus
        );
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        if (!skyBlockTracker.isOnSkyBlock()) return;
        if (!skyBlockTracker.isInArea(KUUDRA_AREA)) return;

        String message = event.getStrippedMessage();
        KuudraPhase detected = KuudraPhase.fromMessage(message);
        if (detected != null && detected != KuudraPhase.NONE) {
            stateManager.setPhase(detected);
        }

        handleSupplyDetect(event, message);
        handleFreshDetect(message);

        if (message.contains("Sending to server") || message.contains("Starting in 5 seconds...")) {
            stateManager.reset();
        }
    }

    private void handleSupplyDetect(ChatReceivedEvent event, String message) {
        Matcher supplyMatcher = SUPPLY_PATTERN.matcher(message);
        if (supplyMatcher.find()) {
            event.setCancelled(PhaseOneConfig.supplyRecoverMessage);

            String supplyCount = supplyMatcher.group(2);
            String formattedMessage = TextFormatUtil.toLegacyString(event.getText());
            double timeSeconds = supplyState.getElapsedTimeMillis() / 1000.0;

            EventBus.post(new SupplyPickupEvent(
                    formattedMessage,
                    StringUtils.extractFormattedPlayerName(formattedMessage),
                    Integer.parseInt(supplyCount), timeSeconds
            ));
        }
    }

    private void handleFreshDetect(@NotNull String message) {
        int buildProgress = getBuildingProgress();
        if (message.contains(FRESH_TOOLS_MESSAGE)) {
            ClientPlayerEntity player = mc.player;
            if (player == null) return;

            EventBus.post(new PlayerFreshEvent(
                    true, player.getName().getString(),
                    player.getId(), buildProgress,
                    System.currentTimeMillis()
            ));
            return;
        }

        Matcher partyMatcher = PARTY_FRESH_PATTERN.matcher(message);
        if (partyMatcher.find()) {
            String playerName = partyMatcher.group(1);
            if (mc.player != null && playerName.equalsIgnoreCase(mc.player.getName().getString())) {
                return;
            }

            findPlayerByName(playerName).ifPresentOrElse(
                    player ->
                            EventBus.post(new PlayerFreshEvent(
                                    false, playerName, player.getId(),
                                    buildProgress, System.currentTimeMillis()
                            )),
                    () -> log.warn("Player '{}' not found in world", playerName)
            );
        }
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

    private void onSkyBlockStatus(@NotNull SkyBlockStatusEvent event) {
        if (!event.onSkyBlock() || !skyBlockTracker.isInArea(KUUDRA_AREA)) {
            stateManager.reset();
        }
    }
}