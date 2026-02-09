package net.iqaddons.mod.model.kuudra;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public enum KuudraPhase {

    NONE("None", -1, msg -> msg.contains("Sending to server") || msg.contains("Starting in 5 seconds...")),
    SUPPLIES("Supplies", 0, msg -> msg.contains("[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!")),
    BUILD("Build", 1, msg -> msg.contains("[NPC] Elle: OMG! Great work collecting my supplies!")),
    EATEN("Eaten", 2, msg -> msg.contains("[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!")),
    STUN("Stun", 3, msg -> msg.contains("has been eaten by Kuudra!") && !msg.contains("Elle")),
    DPS("DPS", 4, msg -> msg.contains("destroyed one of Kuudra's pods!")),
    BOSS("Boss", 5, msg -> msg.contains("[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!")),
    COMPLETED("Completed", 6, msg -> msg.contains("KUUDRA DOWN!") || msg.contains("DEFEAT"));

    public static final KuudraPhase[] RUN_PHASES = { SUPPLIES, BUILD, EATEN, STUN, DPS, BOSS };
    public static final Set<KuudraPhase> COMBAT_PHASES = EnumSet.of(STUN, DPS, BOSS);
    public static final Set<KuudraPhase> PRE_COMBAT_PHASES = EnumSet.of(SUPPLIES, BUILD, EATEN);

    private final String displayName;
    private final int order;
    private final Predicate<String> trigger;

    public boolean isActive() {
        return this != NONE && this != COMPLETED;
    }

    public boolean isInRun() {
        return order >= 0 && order < 6;
    }

    public boolean isCombatPhase() {
        return COMBAT_PHASES.contains(this);
    }

    public boolean isPreCombat() {
        return PRE_COMBAT_PHASES.contains(this);
    }

    public boolean canTransitionTo(@NotNull KuudraPhase target) {
        if (target == NONE) return true;
        if (target == COMPLETED) return true;
        if (this == NONE && target == SUPPLIES) return true;
        return target == next();
    }

    public @NotNull KuudraPhase next() {
        return switch (this) {
            case NONE -> SUPPLIES;
            case SUPPLIES -> BUILD;
            case BUILD -> EATEN;
            case EATEN -> STUN;
            case STUN -> DPS;
            case DPS -> BOSS;
            case BOSS, COMPLETED -> COMPLETED;
        };
    }

    public @NotNull KuudraPhase previous() {
        return switch (this) {
            case NONE, SUPPLIES -> NONE;
            case BUILD -> SUPPLIES;
            case EATEN -> BUILD;
            case STUN -> EATEN;
            case DPS -> STUN;
            case BOSS -> DPS;
            case COMPLETED -> BOSS;
        };
    }

    public static @Nullable KuudraPhase fromMessage(String message) {
        for (KuudraPhase phase : values()) {
            if (phase.trigger.test(message)) {
                return phase;
            }
        }

        return null;
    }

    public static @NotNull Predicate<KuudraPhase> isOneOf(KuudraPhase... phases) {
        var phaseSet = EnumSet.noneOf(KuudraPhase.class);
        phaseSet.addAll(Set.of(phases));

        return phaseSet::contains;
    }
}