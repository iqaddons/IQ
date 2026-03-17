package net.iqaddons.mod.features.kuudra.alerts;

import net.iqaddons.mod.config.categories.KuudraGeneralConfig;
import net.iqaddons.mod.events.impl.ChatReceivedEvent;
import net.iqaddons.mod.features.KuudraFeature;
import net.iqaddons.mod.model.kuudra.KuudraPhase;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

public class AbilityAnnounceFeature extends KuudraFeature {

    private static final List<AbilityAnnounceRule> SPELL_RULES = List.of(
            new AbilityAnnounceRule(
                    Pattern.compile("Casting Spell: Spirit Spark!"),
                    () -> KuudraGeneralConfig.AbilityAnnounce.spiritSpark
            ),
            new AbilityAnnounceRule(
                    Pattern.compile("Casting Spell: Hollowed Rush!"),
                    () -> KuudraGeneralConfig.AbilityAnnounce.hollowedRush
            ),
            new AbilityAnnounceRule(
                    Pattern.compile("Casting Spell: Raging Wind!"),
                    () -> KuudraGeneralConfig.AbilityAnnounce.ragingWind
            ),
            new AbilityAnnounceRule(
                    Pattern.compile("Casting Spell: Ichor Pool!"),
                    () -> KuudraGeneralConfig.AbilityAnnounce.ichorPool
            )
    );

    public AbilityAnnounceFeature() {
        super(
                "abilityAnnounce",
                "Ability Announce",
                KuudraGeneralConfig.AbilityAnnounce::hasSpellEnabled,
                KuudraPhase.RUN_PHASES
        );
    }

    @Override
    protected void onKuudraActivate() {
        subscribe(ChatReceivedEvent.class, this::onChatReceived);
    }

    private void onChatReceived(@NotNull ChatReceivedEvent event) {
        String message = event.getStrippedMessage();

        for (AbilityAnnounceRule rule : SPELL_RULES) {
            if (!rule.isEnabled()) continue;

            if (rule.pattern().matcher(message).matches()) {
                MessageUtil.PARTY.sendMessage("[IQ] %s @%s".formatted(message, formatPlayerPosition()));
                return;
            }
        }
    }

    private @NotNull String formatPlayerPosition() {
        if (mc.player == null) return "(?, ?, ?)";

        BlockPos pos = mc.player.getBlockPos();
        return "(%d, %d, %d)".formatted(pos.getX(), pos.getY(), pos.getZ());
    }

    private record AbilityAnnounceRule(
            Pattern pattern,
            BooleanSupplier enabledSupplier
    ) {
        private boolean isEnabled() {
            return enabledSupplier.getAsBoolean();
        }
    }
}
