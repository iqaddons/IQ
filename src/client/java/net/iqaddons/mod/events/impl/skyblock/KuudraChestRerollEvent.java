package net.iqaddons.mod.events.impl.skyblock;

import net.iqaddons.mod.events.Event;

public record KuudraChestRerollEvent(
        int windowId,
        RerollType rerollType
) implements Event {

    public enum RerollType {
        ITEMS,
        SHARD
    }
}
