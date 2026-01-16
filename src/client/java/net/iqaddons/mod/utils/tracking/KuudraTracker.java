package net.iqaddons.mod.utils.tracking;

import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.config.categories.PhaseOneConfig;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.SkyBlockStatusEvent;
import net.iqaddons.mod.events.impl.SupplyPickupEvent;
import net.iqaddons.mod.state.KuudraStateManager;
import net.iqaddons.mod.state.SupplyStateManager;
import net.iqaddons.mod.state.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.StringUtils;
import net.iqaddons.mod.utils.TextFormatUtil;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class KuudraTracker {

    private static final String KUUDRA_AREA = "Kuudra";
    private static final Pattern SUPPLY_PATTERN = Pattern.compile("(.+) recovered one of Elle's supplies! \\((\\d)/6\\)");

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
        if (!isInKuudraArea()) return;

        String message = event.getStrippedMessage();
        KuudraPhase detected = KuudraPhase.fromMessage(message);
        if (detected != null && detected != KuudraPhase.NONE) {
            stateManager.setPhase(detected);
        }

        Matcher supplyMatcher = SUPPLY_PATTERN.matcher(message);
        if (supplyMatcher.find()) {
            event.setCancelled(PhaseOneConfig.supplyRecoverMessage);

            String supplyCount = supplyMatcher.group(2);
            String formattedMessage = TextFormatUtil.toLegacyString(event.getText());
            long timeSeconds = supplyState.getElapsedTimeSeconds();

            EventBus.post(new SupplyPickupEvent(
                    formattedMessage,
                    StringUtils.extractFormattedPlayerName(formattedMessage),
                    Integer.parseInt(supplyCount), timeSeconds
            ));
        }
    }

    private void onSkyBlockStatus(@NotNull SkyBlockStatusEvent event) {
        if (!event.onSkyBlock() || !isInKuudraArea()) {
            stateManager.reset();
        }
    }

    private boolean isInKuudraArea() {
        return skyBlockTracker.isInArea(KUUDRA_AREA) || stateManager.isInKuudra();
    }
}