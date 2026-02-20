package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.utils.MessageUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

public class KuudraNotificationsFeature extends KuudraFeature {

    private static final List<KuudraNotificationRule> NOTIFICATION_RULES = List.of(
            new KuudraNotificationRule(
                    Pattern.compile(".*It's time to build the Ballista again! Cover me!"),
                    "§a§lBUILD STARTED",
                    () -> KuudraGeneralConfig.KuudraNotifications.buildStarted
            ),
            new KuudraNotificationRule(
                    Pattern.compile("Casting Spell: Ichor Pool!"),
                    "§b§lICHOR USED",
                    () -> KuudraGeneralConfig.KuudraNotifications.ichorUsed
            ),
            new KuudraNotificationRule(
                    Pattern.compile(".* No (X|Equals|Triangle|Slash|X Cannon|Shop|Square)!"),
                    "§4§lNO $1!",
                    () -> KuudraGeneralConfig.KuudraNotifications.noPre
            ),
            new KuudraNotificationRule(
                    Pattern.compile("Starting in 4 seconds\\."),
                    "§e§lSOS REMINDER",
                    () -> KuudraGeneralConfig.KuudraNotifications.sosReminder
            ),
            new KuudraNotificationRule(
                    Pattern.compile(".*Phew! The Ballista is finally ready!"),
                    "§a§lBUILD 100%",
                    () -> KuudraGeneralConfig.KuudraNotifications.buildDone
            ),
            new KuudraNotificationRule(
                    Pattern.compile(".*\\(6/6\\)"),
                    "§a§lSUPPLIES DONE",
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
                    () -> KuudraGeneralConfig.KuudraNotifications.supplyPickingAlert
            )
    );

    public KuudraNotificationsFeature() {
        super(
                "kuudraNotifications",
                "Kuudra Notifications",
                () -> NOTIFICATION_RULES.stream().anyMatch(KuudraNotificationRule::isEnabled)
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();
        for (KuudraNotificationRule rule : NOTIFICATION_RULES) {
            if (!rule.isEnabled()) continue;

            var matcher = rule.pattern().matcher(message);
            if (!matcher.matches()) continue;

            MessageUtil.showAlert(matcher.replaceAll(rule.titleTemplate), 40);
            return;
        }
    }

    private record KuudraNotificationRule(
            Pattern pattern,
            String titleTemplate,
            BooleanSupplier enabledSupplier
    ) {
        private boolean isEnabled() {
            return enabledSupplier.getAsBoolean();
        }
    }
}
