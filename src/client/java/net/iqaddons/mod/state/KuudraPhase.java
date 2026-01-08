package net.iqaddons.mod.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KuudraPhase {

    NONE("None", -1),
    QUEUE("Queue", 0),
    SUPPLIES("Supplies", 1),
    BUILD("Build", 2),
    STUN("Stun", 3),
    BOSS("Boss", 4),
    COMPLETED("Completed", 5);

    private final String displayName;
    private final int order;

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
            case BUILD -> STUN;
            case STUN -> BOSS;
            case BOSS -> COMPLETED;
            default -> NONE;
        };
    }
}