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
import net.iqaddons.mod.utils.TextColor;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class KuudraNotificationsFeature extends Feature {

    private static final List<KuudraNotificationRule> NOTIFICATION_RULES = List.of(
            new KuudraNotificationRule(
                    Pattern.compile(".*It's time to build the Ballista again! Cover me!"),
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.buildStartedText,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationToggles.buildStarted,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.buildStartedColor,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.buildStartedDuration
            ),
            new KuudraNotificationRule(
                    Pattern.compile("Casting Spell: Ichor Pool!"),
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.ichorUsedText,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationToggles.ichorUsed,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.ichorUsedColor,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.ichorUsedDuration
            ),
            new KuudraNotificationRule(
                    Pattern.compile(".*Starting in 4 seconds\\.{1,3}.*"),
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.sosReminderText,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationToggles.sosReminder,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.sosReminderColor,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.sosReminderDuration
            ),
            new KuudraNotificationRule(
                    Pattern.compile("You purchased Human Cannonball!"),
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.cannonBallText,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationToggles.cannonBall,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.cannonBallColor,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.cannonBallDuration
            ),
            new KuudraNotificationRule(
                    Pattern.compile("Someone else is currently trying to pick up these supplies!"),
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.supplyPickingAlertText,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationToggles.supplyPickingAlert,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.supplyPickingAlertColor,
                    () -> KuudraGeneralConfig.KuudraNotifications.NotificationStyles.supplyPickingAlertDuration,
                    SoundEvents.ENTITY_VILLAGER_NO
            )
    );

    public KuudraNotificationsFeature() {
        super(
                "kuudraNotifications",
                "Kuudra Notifications",
                () -> KuudraGeneralConfig.kuudraNotifications
                        && isAnyNotificationEnabled() &&
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
        if (KuudraGeneralConfig.KuudraNotifications.NotificationToggles.noPre) {
            NoPreMessageParser.ParsedNoPreCall parsed = NoPreMessageParser.parse(message);
            if (parsed != null) {
                String noPreText = resolveTemplate(
                        KuudraGeneralConfig.KuudraNotifications.NotificationStyles.noPreText,
                        "NO {PILE}!"
                )
                        .replace("{pile}", parsed.canonicalPileName())
                        .replace("{PILE}", parsed.canonicalPileName().toUpperCase(java.util.Locale.ROOT));
                showNotification(
                        noPreText,
                        KuudraGeneralConfig.KuudraNotifications.NotificationStyles.noPreColor,
                        KuudraGeneralConfig.KuudraNotifications.NotificationStyles.noPreDuration,
                        null
                );
                return;
            }
        }

        for (KuudraNotificationRule rule : NOTIFICATION_RULES) {
            if (!rule.isEnabled()) continue;

            var matcher = rule.pattern().matcher(message);
            if (!matcher.matches()) continue;

            showNotification(rule.text(), rule.color(), rule.duration(), rule.soundEvent());
            return;
        }
    }

    private void onKuudraPhaseChange(@NotNull KuudraPhaseChangeEvent event) {
        if (!KuudraGeneralConfig.KuudraNotifications.NotificationToggles.buildDone) return;
        if (event.currentPhase() != KuudraPhase.EATEN) return;

        showNotification(
                resolveText(
                        KuudraGeneralConfig.KuudraNotifications.NotificationStyles.buildDoneText,
                        "Build Done"
                ),
                KuudraGeneralConfig.KuudraNotifications.NotificationStyles.buildDoneColor,
                KuudraGeneralConfig.KuudraNotifications.NotificationStyles.buildDoneDuration,
                null
        );
    }

    private void onSupplyPlace(@NotNull SupplyPlaceEvent event) {
        if (KuudraGeneralConfig.KuudraNotifications.NotificationToggles.placedSupply && isLocalPlayer(event.playerName())) {
            showNotification(
                    resolveText(
                            KuudraGeneralConfig.KuudraNotifications.NotificationStyles.placedSupplyText,
                            "PLACED"
                    ),
                    KuudraGeneralConfig.KuudraNotifications.NotificationStyles.placedSupplyColor,
                    KuudraGeneralConfig.KuudraNotifications.NotificationStyles.placedSupplyDuration,
                    SoundEvents.BLOCK_NOTE_BLOCK_PLING.value()
            );
        }

        if (!KuudraGeneralConfig.KuudraNotifications.NotificationToggles.suppliesDone) return;
        if (event.currentSupply() != 6) return;

        showNotification(
                resolveText(
                        KuudraGeneralConfig.KuudraNotifications.NotificationStyles.suppliesDoneText,
                        "6/6"
                ),
                KuudraGeneralConfig.KuudraNotifications.NotificationStyles.suppliesDoneColor,
                KuudraGeneralConfig.KuudraNotifications.NotificationStyles.suppliesDoneDuration,
                null
        );
    }

    private void onSupplyPickup(@NotNull SupplyPickupEvent event) {
        if (!KuudraGeneralConfig.KuudraNotifications.NotificationToggles.supplyPickedUp) return;

        showNotification(
                resolveText(
                        KuudraGeneralConfig.KuudraNotifications.NotificationStyles.supplyPickedUpText,
                        "PICKED UP"
                ),
                KuudraGeneralConfig.KuudraNotifications.NotificationStyles.supplyPickedUpColor,
                KuudraGeneralConfig.KuudraNotifications.NotificationStyles.supplyPickedUpDuration,
                null
        );
    }

    private static boolean isAnyNotificationEnabled() {
        return NOTIFICATION_RULES.stream().anyMatch(KuudraNotificationRule::isEnabled)
                || KuudraGeneralConfig.KuudraNotifications.NotificationToggles.noPre
                || KuudraGeneralConfig.KuudraNotifications.NotificationToggles.buildDone
                || KuudraGeneralConfig.KuudraNotifications.NotificationToggles.suppliesDone
                || KuudraGeneralConfig.KuudraNotifications.NotificationToggles.placedSupply
                || KuudraGeneralConfig.KuudraNotifications.NotificationToggles.supplyPickedUp;
    }

    private boolean isLocalPlayer(@Nullable String playerName) {
        if (mc.player == null || playerName == null || playerName.isBlank()) {
            return false;
        }
        return StringUtils.stripFormatting(playerName).equalsIgnoreCase(mc.player.getName().getString());
    }

    private static void showNotification(
            @NotNull String text,
            @NotNull TextColor color,
            int durationTicks,
            @Nullable SoundEvent soundEvent
    ) {
        String bold = KuudraGeneralConfig.KuudraNotifications.bold ? "§l" : "";
        String alertText = color.code() + bold + text;
        if (soundEvent != null && KuudraGeneralConfig.KuudraNotifications.kuudraNotificationsSound) {
            MessageUtil.showAlert(alertText, durationTicks, soundEvent);
        } else {
            MessageUtil.showAlert(alertText, durationTicks);
        }
    }

    private static @NotNull String resolveTemplate(@Nullable String configured, @NotNull String fallback) {
        String resolved = configured == null ? "" : configured.trim();
        return resolved.isEmpty() ? fallback : resolved;
    }

    private static @NotNull String resolveText(@Nullable String configured, @NotNull String fallback) {
        return resolveTemplate(configured, fallback);
    }

    private record KuudraNotificationRule(
            Pattern pattern,
            Supplier<String> messageSupplier,
            BooleanSupplier enabledSupplier,
            Supplier<TextColor> colorSupplier,
            IntSupplier durationSupplier,
            @Nullable SoundEvent soundEvent
    ) {
        private KuudraNotificationRule(
                Pattern pattern,
                Supplier<String> messageSupplier,
                BooleanSupplier enabledSupplier,
                Supplier<TextColor> colorSupplier,
                IntSupplier durationSupplier
        ) {
            this(pattern, messageSupplier, enabledSupplier, colorSupplier, durationSupplier, null);
        }

        private boolean isEnabled() {
            return enabledSupplier.getAsBoolean();
        }

        private TextColor color() {
            return colorSupplier.get();
        }

        private int duration() {
            return durationSupplier.getAsInt();
        }

        private String text() {
            return resolveText(messageSupplier.get(), "Notification");
        }
    }
}
