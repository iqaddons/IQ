package net.iqaddons.mod.nanovg

/**
 * A subsystem that needs explicit setup and teardown around the game's own
 * lifecycle (resource reloads, client shutdown, GL context loss, etc).
 *
 * Every long-lived manager in IQ (theme, assets, windows, input) implements
 * this instead of doing work in an `init {}` block, so start-up order is
 * explicit and deterministic and teardown never leaks GPU/native resources.
 */
interface Lifecycle {
    /** Called once, in dependency order, from [IQClientInitializer]. */
    fun start()

    /** Called on client shutdown and before a full GL context recreation. */
    fun stop()
}

/**
 * Owns an ordered list of [Lifecycle] participants and starts/stops them as
 * a unit, in registration order forward and reverse order back.
 */
class LifecycleRegistry {
    private val participants = mutableListOf<Lifecycle>()

    fun register(participant: Lifecycle): LifecycleRegistry {
        participants += participant
        return this
    }

    fun startAll() {
        participants.forEach { it.start() }
    }

    fun stopAll() {
        participants.asReversed().forEach { it.stop() }
    }
}
