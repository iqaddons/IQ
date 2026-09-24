package net.iqaddons.mod.nanovg.rendering.nanovg

import net.iqaddons.mod.nanovg.IqNanoVg
import net.iqaddons.mod.nanovg.Lifecycle
import org.lwjgl.nanovg.NanoVGGL3.*

/**
 * Owns the single native `NVGcontext*` for the mod. Created lazily on first
 * use (i.e. when the IQ UI is first opened) and recreated transparently if
 * the GL context is ever lost, since every other rendering type in this
 * package treats [handle] as ephemeral rather than caching it.
 */
class NanoVGContext(private val stencilStrokes: Boolean = true) : Lifecycle {

    var handle: Long = 0L
        private set

    val isReady: Boolean get() = handle != 0L

    override fun start() {
        if (handle != 0L) return
        val flags = NVG_ANTIALIAS or if (stencilStrokes) NVG_STENCIL_STROKES else 0
        handle = nvgCreate(flags)
        check(handle != 0L) { "Failed to create NanoVG context" }
        IqNanoVg.logger.info("NanoVG context created (handle=$handle, stencilStrokes=$stencilStrokes)")
    }

    override fun stop() {
        if (handle != 0L) {
            nvgDelete(handle)
            handle = 0L
        }
    }
}
