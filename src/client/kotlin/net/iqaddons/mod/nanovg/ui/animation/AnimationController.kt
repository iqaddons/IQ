package net.iqaddons.mod.nanovg.ui.animation

/**
 * Computes real frame-to-frame delta time once per render pass and hands it
 * out to whoever advances [Animatable]s that frame.
 *
 * Kept deliberately dumb: it does not "own" or track individual Animatables
 * (that would mean every widget registering/unregistering itself on
 * mount/unmount for no real benefit — Minecraft re-renders the active screen
 * every frame regardless). It exists purely so every part of the UI tree
 * agrees on the same delta for a given frame, computed exactly once.
 */
class AnimationController {

    private var lastFrameNanos: Long = -1L

    /** Longest delta ever handed out for a single frame, to avoid huge jumps after a stall (e.g. alt-tab, GC pause). */
    private val maxDeltaSeconds = 0.1f

    /** Call once per render pass, before ticking any widgets. Returns this frame's delta in seconds. */
    fun tick(): Float {
        val now = System.nanoTime()
        if (lastFrameNanos < 0) {
            lastFrameNanos = now
            return 0f
        }
        val delta = ((now - lastFrameNanos) / 1_000_000_000.0).toFloat()
        lastFrameNanos = now
        return delta.coerceIn(0f, maxDeltaSeconds)
    }

    fun reset() {
        lastFrameNanos = -1L
    }
}
