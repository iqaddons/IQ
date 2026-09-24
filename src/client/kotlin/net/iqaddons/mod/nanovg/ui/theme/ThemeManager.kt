package net.iqaddons.mod.nanovg.ui.theme

import net.iqaddons.mod.nanovg.Lifecycle
import net.iqaddons.mod.nanovg.event.UiEventBus

/**
 * Holds the active [Theme] and notifies listeners when it changes.
 *
 * Multiple themes can be registered by id ahead of time (built-in + future
 * user-defined ones); switching is a single [setActive] call, and every
 * widget picks the new values up on its next `draw()` because they read
 * [active] live rather than caching values at construction time.
 */
class ThemeManager(private val events: UiEventBus) : Lifecycle {

    private val registered = LinkedHashMap<String, Theme>()

    var active: Theme = DefaultTheme.theme
        private set

    override fun start() {
        register(DefaultTheme.theme)
        setActive(DefaultTheme.theme.name)
    }

    override fun stop() {
        registered.clear()
    }

    fun register(theme: Theme) {
        registered[theme.name] = theme
    }

    fun availableThemes(): List<String> = registered.keys.toList()

    fun setActive(name: String) {
        val theme = registered[name] ?: return
        active = theme
        events.emit(UiEventBus.Event.ThemeChanged(theme))
    }
}
