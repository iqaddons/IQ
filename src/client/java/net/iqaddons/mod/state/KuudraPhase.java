package net.iqaddons.mod.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public enum KuudraPhase {

    NONE("None", -1, _ -> false),
    QUEUE("Queue", 0, msg -> msg.contains("[NPC] Elle: Talk with me to begin!")),
    SUPPLIES("Supplies", 1, msg -> msg.contains("[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!")),
    BUILD("Build", 2, msg -> msg.contains("[NPC] Elle: OMG! Great work collecting my supplies!")),
    EATEN("Eaten", 3, msg -> msg.contains("[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!")),
    STUN("Stun", 4, msg -> msg.contains("has been eaten by Kuudra!") && !msg.contains("Elle")),
    HIT("Hit", 5, msg -> msg.contains("destroyed one of Kuudra's pods!")),
    BOSS("Boss", 7, msg -> msg.contains("[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!")),
    COMPLETED("Completed", 8, msg -> msg.contains("KUUDRA DOWN!") || msg.contains("DEFEAT"));

    private final String displayName;
    private final int order;
    private final Predicate<String> trigger;

    public boolean isActive() {
        return this != NONE && this != COMPLETED;
    }

    public boolean isInRun() {
        return order >= 1 && order <= 4;
    }

    public KuudraPhase next() {
        return switch (this) {
            case QUEUE -> SUPPLIES;
            case SUPPLIES -> BUILD;
            case BUILD -> EATEN;
            case EATEN -> STUN;
            case STUN -> HIT;
            case HIT -> BOSS;
            case BOSS -> COMPLETED;
            default -> NONE;
        };
    }

    public static KuudraPhase fromMessage(String message) {
        for (KuudraPhase phase : values()) {
            if (phase.trigger.test(message)) {
                return phase;
            }
        }

        return NONE;
    }
}