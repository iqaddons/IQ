package net.iqaddons.mod.nanovg.ui.animation

import net.iqaddons.mod.nanovg.ui.theme.MotionSpec

/**
 * Bundles the small set of [Animatable]s that make up a show/hide
 * transition (opacity, scale, vertical offset) behind one `visible` flag, so
 * windows/popups/notifications don't each hand-roll the same three
 * Animatables. Any subset of the axes can be used — e.g. a tooltip fade only
 * sets [opacity], a popup uses all three.
 */
class Transition(
    private val enterSpec: MotionSpec,
    private val exitSpec: MotionSpec,
    startOpacity: Float = 0f,
    startScale: Float = 1f,
    startOffsetY: Float = 0f,
) {
    val opacity = Animatable(startOpacity)
    val scale = Animatable(startScale)
    val offsetY = Animatable(startOffsetY)

    var isVisible: Boolean = false
        private set

    /** True once a `show(false)` has finished animating out — safe point to actually remove the widget. */
    val isFullyHidden: Boolean get() = !isVisible && opacity.value <= 0.001f && !opacity.isAnimating

    fun show(
        visible: Boolean,
        targetOpacity: Float = 1f,
        targetScale: Float = 1f,
        targetOffsetY: Float = 0f,
        hiddenOffsetY: Float = 0f,
        hiddenScale: Float = 0.96f,
    ) {
        isVisible = visible
        val spec = if (visible) enterSpec else exitSpec
        opacity.animateTo(if (visible) targetOpacity else 0f, spec)
        scale.animateTo(if (visible) targetScale else hiddenScale, spec)
        offsetY.animateTo(if (visible) targetOffsetY else hiddenOffsetY, spec)
    }

    fun update(deltaSeconds: Float) {
        opacity.update(deltaSeconds)
        scale.update(deltaSeconds)
        offsetY.update(deltaSeconds)
    }

    companion object {
        fun fade(spec: MotionSpec) = Transition(spec, spec)
        fun popupStyle(spec: MotionSpec) = Transition(spec, spec, startOffsetY = -6f)
        fun windowStyle(spec: MotionSpec) = Transition(spec, spec, startScale = 0.97f, startOffsetY = 8f)
        fun notificationStyle(spec: MotionSpec) = Transition(spec, spec, startOffsetY = 24f)
    }
}
