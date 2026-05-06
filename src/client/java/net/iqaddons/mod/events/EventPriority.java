package net.iqaddons.mod.events;

/**
 * Defines the execution priority for event subscriptions.
 * <p>
 * Subscriptions with <b>higher</b> priority (lower ordinal) run <b>first</b>;
 * subscriptions with <b>lower</b> priority (higher ordinal) run <b>last</b>.
 * <p>
 * Execution order: {@code HIGHEST → HIGH → NORMAL → LOW → LOWEST}
 */
public enum EventPriority {

    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST
}

