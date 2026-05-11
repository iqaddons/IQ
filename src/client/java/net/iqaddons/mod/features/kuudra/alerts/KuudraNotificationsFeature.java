package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.IQConstants;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.events.impl.skyblock.KuudraPhaseChangeEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPickupEvent;
import net.iqaddons.mod.events.impl.skyblock.supply.SupplyPlaceEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.iqaddons.mod.utils.NoPreMessageParser;
import net.iqaddons.mod.utils.ScoreboardUtils;
import net.iqaddons.mod.utils.StringUtils;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

public class KuudraNotificationsFeature extends Feature {

    private static final List<KuudraNotificationRule> NOTIFICATION_RULES = List.of(
            new KuudraNotificationRule(
                    Pattern.compile(".*It's time to build the Ballista again! Cover me!"),
                    "§a§BUILD STARTED",
                    () -> KuudraGeneralConfig.KuudraNotifications.buildStarted
            ),
            new KuudraNotificationRule(
                    Pattern.compile("Casting Spell: Ichor Pool!"),
                    "§b§lICHOR",
                    () -> KuudraGeneralConfig.KuudraNotifications.ichorUsed
            ),
            new KuudraNotificationRule(
                    Pattern.compile(".*Starting in 4 seconds\\.{1,3}.*"),
                    "§b§lSOS REMINDER",
                    () -> KuudraGeneralConfig.KuudraNotifications.sosReminder
            ),
            new KuudraNotificationRule(
                    Pattern.compile("You purchased Human Cannonball!"),
                    "§e§lCANNONBALL",
                    () -> KuudraGeneralConfig.KuudraNotifications.cannonBall
            ),
            new KuudraNotificationRule(
                    Pattern.compile("Someone else is currently trying to pick up these supplies!"),
                    "§cALREADY PICKING",
                    () -> KuudraGeneralConfig.KuudraNotifications.supplyPickingAlert,
                    SoundEvents.ENTITY_VILLAGER_NO
            )
    );

    public KuudraNotificationsFeature() {
        super(
                "kuudraNotifications",
                "Kuudra Notifications",
                () -> isAnyNotificationEnabled() &&
                        ScoreboardUtils.isInArea(IQConstants.KUUDRA_AREA_ID)
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
        subscribe(KuudraPhaseChangeEvent.class, this::onKuudraPhaseChange);
        subscribe(SupplyPickupEvent.class, this::onSupplyPickup);
        subscribe(SupplyPlaceEvent.class, this::onSupplyPlace);
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();

        mc.execute(() -> handleChatMessage(message));
    }

    private void handleChatMessage(@NotNull String message) {
        if (KuudraGeneralConfig.KuudraNotifications.noPre) {
            NoPreMessageParser.ParsedNoPreCall parsed = NoPreMessageParser.parse(message);
            if (parsed != null) {
                showAlert("§4§lNO " + parsed.canonicalPileName().toUpperCase() + "!", null);
                return;
            }
        }

        for (KuudraNotificationRule rule : NOTIFICATION_RULES) {
            if (!rule.isEnabled()) continue;

            var matcher = rule.pattern().matcher(message);
            if (!matcher.matches()) continue;

            String alertText = matcher.replaceAll(rule.titleTemplate).toUpperCase(Locale.ROOT);
            showAlert(alertText, rule.soundEvent());
            return;
        }
    }

    private void onKuudraPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (!KuudraGeneralConfig.KuudraNotifications.buildDone) return;
        if (event.currentPhase() != KuudraPhase.EATEN) return;

        showAlert("§ABuild Completed%", null);
    }

    private void onSupplyPlace(@NotNull SupplyPlaceEvent event) {
        if (KuudraGeneralConfig.KuudraNotifications.placedSupply && isLocalPlayer(event.playerName())) {
            MessageUtil.showAlert("§a§lPLACED", 20, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value());
        }

        if (!KuudraGeneralConfig.KuudraNotifications.suppliesDone) return;
        if (event.currentSupply() != 6) return;

        showAlert("§B§L6/6", null);
    }

    private void onSupplyPickup(@NotNull SupplyPickupEvent event) {
        if (!KuudraGeneralConfig.KuudraNotifications.supplyPickedUp) return;

        MessageUtil.showAlert("§a§lPICKED UP", 20);
    }

    private static boolean isAnyNotificationEnabled() {
        return NOTIFICATION_RULES.stream().anyMatch(KuudraNotificationRule::isEnabled)
                || KuudraGeneralConfig.KuudraNotifications.noPre
                || KuudraGeneralConfig.KuudraNotifications.buildDone
                || KuudraGeneralConfig.KuudraNotifications.suppliesDone
                || KuudraGeneralConfig.KuudraNotifications.placedSupply
                || KuudraGeneralConfig.KuudraNotifications.supplyPickedUp;
    }

    private boolean isLocalPlayer(@Nullable String playerName) {
        if (mc.player == null || playerName == null || playerName.isBlank()) {
            return false;
        }

        return StringUtils.stripFormatting(playerName).equalsIgnoreCase(mc.player.getName().getString());
    }

    private static void showAlert(@NotNull String alertText, @Nullable SoundEvent soundEvent) {
        if (soundEvent != null) {
            MessageUtil.showAlert(alertText, 60, soundEvent);
        } else {
            MessageUtil.showAlert(alertText, 60);
        }
    }

    private record KuudraNotificationRule(
            Pattern pattern,
            String titleTemplate,
            BooleanSupplier enabledSupplier,
            @Nullable SoundEvent soundEvent
    ) {
        private KuudraNotificationRule(
                Pattern pattern,
                String titleTemplate,
                BooleanSupplier enabledSupplier
        ) {
            this(pattern, titleTemplate, enabledSupplier, null);
        }

        private boolean isEnabled() {
            return enabledSupplier.getAsBoolean();
        }
    }
}
