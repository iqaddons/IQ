package net.iqaddons.mod.nanovg.event

import net.iqaddons.mod.nanovg.ui.theme.Theme

/**
 * A small typed pub/sub bus so subsystems (theme, window manager, input,
 * notifications) can react to each other without holding direct references
 * — the notification center, for instance, subscribes to
 * [Event.NotificationRequested] without the caller needing to know it
 * exists.
 *
 * Deliberately not a generic global event bus for arbitrary payloads: the
 * sealed [Event] hierarchy keeps every event IQ can emit discoverable in
 * one file instead of scattered string-keyed topics.
 */
class UiEventBus {

    sealed interface Event {
        data class ThemeChanged(val theme: Theme) : Event
        data class WindowOpened(val windowId: String) : Event
        data class WindowClosed(val windowId: String) : Event
        data class NotificationRequested(val title: String, val message: String, val level: Level) : Event {
            enum class Level { INFO, SUCCESS, WARNING, DANGER }
        }
    }

    fun interface Listener<T : Event> {
        fun onEvent(event: T)
    }

    private val listeners = mutableMapOf<Class<out Event>, MutableList<Listener<Event>>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Event> subscribe(type: Class<T>, listener: Listener<T>) {
        listeners.getOrPut(type) { mutableListOf() }.add(listener as Listener<Event>)
    }

    fun emit(event: Event) {
        listeners[event::class.java]?.forEach { it.onEvent(event) }
    }
}
