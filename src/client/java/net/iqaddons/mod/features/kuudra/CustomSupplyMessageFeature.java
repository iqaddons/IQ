package net.iqaddons.mod.features.kuudra;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.state.SupplyStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.ChatUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CustomSupplyMessageFeature extends KuudraFeature {

    private static final Pattern SUPPLY_PATTERN = Pattern.compile("(.+) recovered one of Elle's supplies! \\((\\d)/6\\)");

    private final SupplyStateManager supplyState = SupplyStateManager.get();

    public CustomSupplyMessageFeature() {
        super(
                "customSupplyMessage",
                "Custom Supply Message",
                () -> Configuration.PhaseOneConfig.supplyRecoverMessage,
                KuudraPhase.SUPPLIES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(EventBus.subscribe(ChatReceivedEvent.class, this::onChat));
        log.info("Custom Supply Drop Message activated");
    }

    @Override
    protected void onKuudraDeactivate() {
        log.info("Custom Supply Drop Message deactivated");
    }

    private void onChat(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        Matcher matcher = SUPPLY_PATTERN.matcher(message);
        if (!matcher.find()) return;

        event.setCancelled(true);

        String supplyCount = matcher.group(2);
        String formattedMessage = event.getStrippedMessage();
        String formattedPlayerName = extractFormattedPlayerName(formattedMessage);

        double timeSeconds = supplyState.getElapsedTimeSeconds();
        String timeColor = getTimeColor(supplyState.getTimeTier());
        String formattedTime = String.format("%.2f", timeSeconds);

        ChatUtil.sendFormattedMessage(String.format(
                "%s §arecovered a supply in %s%ss §r§8(%s/6)",
                formattedPlayerName,
                timeColor,
                formattedTime,
                supplyCount
        ));
    }

    private @NotNull String extractFormattedPlayerName(@NotNull String formattedMessage) {
        int recoverIndex = formattedMessage.indexOf("recovered");
        if (recoverIndex > 0) {
            return formattedMessage.substring(0, recoverIndex - 1);
        }

        return formattedMessage;
    }

    @Contract(pure = true)
    private @NotNull String getTimeColor(int tier) {
        return switch (tier) {
            case 0 -> "§f§l";
            case 1 -> "§9§l";
            case 2 -> "§a§l";
            case 3 -> "§2§l";
            case 4 -> "§e§l";
            default -> "§c§l";
        };
    }
}
