package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.IQConstants;
import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.features.Feature;
import net.iqaddons.mod.utils.MessageUtil;
import net.iqaddons.mod.utils.ScoreboardUtils;
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
                    "§a§lBUILD STARTED",
                    () -> KuudraGeneralConfig.KuudraNotifications.buildStarted
            ),
            new KuudraNotificationRule(
                    Pattern.compile("Casting Spell: Ichor Pool!"),
                    "§b§lICHOR",
                    () -> KuudraGeneralConfig.KuudraNotifications.ichorUsed
            ),
            new KuudraNotificationRule(
                    Pattern.compile(".* No (X|Equals|Triangle|Slash|X Cannon|Shop|Square)!"),
                    "§4§lNO $1!",
                    () -> KuudraGeneralConfig.KuudraNotifications.noPre
            ),
            new KuudraNotificationRule(
                    Pattern.compile(".*Starting in 4 seconds\\.{1,3}.*"),
                    "§b§lSOS REMINDER",
                    () -> KuudraGeneralConfig.KuudraNotifications.sosReminder
            ),
            new KuudraNotificationRule(
                    Pattern.compile(".*Phew! The Ballista is finally ready!"),
                    "§a§lBUILD 100%",
                    () -> KuudraGeneralConfig.KuudraNotifications.buildDone
            ),
            new KuudraNotificationRule(
                    Pattern.compile(".*\\(6/6\\).*"),
                    "§b§lSUPPLIES 6/6",
                    () -> KuudraGeneralConfig.KuudraNotifications.suppliesDone
            ),
            new KuudraNotificationRule(
                    Pattern.compile("You purchased Human Cannonball!"),
                    "§e§lCANNONBALL",
                    () -> KuudraGeneralConfig.KuudraNotifications.cannonBall
            ),
            new KuudraNotificationRule(
                    Pattern.compile("Someone else is currently trying to pick up these supplies!"),
                    "§c§lALREADY PICKING!",
                    () -> KuudraGeneralConfig.KuudraNotifications.supplyPickingAlert,
                    SoundEvents.ENTITY_VILLAGER_NO
            )
    );

    public KuudraNotificationsFeature() {
        super(
                "kuudraNotifications",
                "Kuudra Notifications",
                () -> NOTIFICATION_RULES.stream().anyMatch(KuudraNotificationRule::isEnabled) &&
                        ScoreboardUtils.isInArea(IQConstants.KUUDRA_AREA_ID)
        );
    }

    @Override
    protected void onActivate() {
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        for (KuudraNotificationRule rule : NOTIFICATION_RULES) {
            if (!rule.isEnabled()) continue;

            var matcher = rule.pattern().matcher(message);
            if (!matcher.matches()) continue;

            String alertText = matcher.replaceAll(rule.titleTemplate).toUpperCase(Locale.ROOT);
            if (rule.soundEvent() != null) {
                MessageUtil.showAlert(alertText, 60, rule.soundEvent());
            } else {
                MessageUtil.showAlert(alertText, 60);
            }
            return;
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
