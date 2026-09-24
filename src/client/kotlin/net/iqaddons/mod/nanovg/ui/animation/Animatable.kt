package net.iqaddons.mod.nanovg.ui.animation

import net.iqaddons.mod.nanovg.ui.theme.MotionSpec

/**
 * A single animated [Float]. Widgets hold one per animated property (hover
 * amount, expansion progress, opacity, ...) and call [update] once per frame
 * with the real frame delta — never a fixed per-tick step — so motion is
 * identical at 60fps, 240fps, or after a frame hitch.
 *
 * Retargeting mid-flight (calling [animateTo] again before the previous
 * animation finished) starts the new animation from the *current* eased
 * value, so animations are naturally interruptible with no visible snap.
 */
class Animatable(initialValue: Float = 0f) {

    var value: Float = initialValue
        private set

    val isAnimating: Boolean get() = elapsedMs < durationMs

    private var startValue: Float = initialValue
    private var targetValue: Float = initialValue
    private var elapsedMs: Long = 0
    private var durationMs: Long = 0
    private var easing: Easing = Easing.Linear

    fun snapTo(newValue: Float) {
        value = newValue
        startValue = newValue
        targetValue = newValue
        elapsedMs = 0
        durationMs = 0
    }

    fun animateTo(target: Float, spec: MotionSpec) {
        if (target == targetValue && isAnimating) return
        startValue = value
        targetValue = target
        durationMs = spec.durationMs
        easing = spec.easing
        elapsedMs = 0
    }

    /** Advances the animation by [deltaSeconds] (real elapsed time, not a tick count). */
    fun update(deltaSeconds: Float): Float {
        if (!isAnimating) return value
        elapsedMs = (elapsedMs + (deltaSeconds * 1000).toLong()).coerceAtMost(durationMs)
        val linearT = if (durationMs == 0L) 1f else elapsedMs.toFloat() / durationMs.toFloat()
        value = startValue + (targetValue - startValue) * easing.ease(linearT)
        return value
    }
}
