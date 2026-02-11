package net.iqaddons.mod.events.dispatcher.detector;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.Event;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.skyblock.PlayerFreshEvent;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.function.Consumer;
import java.util.regex.Matcher;

import static net.iqaddons.mod.IQConstants.*;
import static net.iqaddons.mod.utils.EntityDetectorUtil.findPlayerByName;

@Slf4j
public class FreshDetector {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public void detect(ChatReceivedEvent event, String message, Consumer<Event> postEvent) {
        int buildProgress = getBuildingProgress();
        if (message.contains(FRESH_TOOLS_MESSAGE)) {
            ClientPlayerEntity player = client.player;
            if (player == null) return;

            postEvent.accept(new PlayerFreshEvent(
                    true, StringUtils.getPlayerNick(player),
                    player.getId(), buildProgress,
                    System.currentTimeMillis()
            ));
            return;
        }

        Matcher partyMatcher = PARTY_FRESH_PATTERN.matcher(message);
        if (partyMatcher.find()) {
            if (client.world == null) return;

            String playerName = StringUtils.extractFormattedPlayerName(event.getMessage());
            String plainPlayerName = StringUtils.stripFormatting(playerName);
            if (plainPlayerName.isBlank()) {
                plainPlayerName = partyMatcher.group(1);
            }

            findPlayerByName(plainPlayerName).ifPresentOrElse(
                    player ->
                            postEvent.accept(new PlayerFreshEvent(
                                    false, playerName, player.getId(),
                                    buildProgress, System.currentTimeMillis()
                            )),
                    () -> log.warn("Player '{}' not found in world", playerName)
            );
        }
    }

    private int getBuildingProgress() {
        for (String line : ScoreboardUtils.getLines()) {
            String stripped = StringUtils.stripFormatting(line);
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
}
