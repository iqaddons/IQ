package net.iqaddons.mod.nanovg.rendering.blur

import net.iqaddons.mod.nanovg.rendering.shaders.ShaderProgram
import net.iqaddons.mod.nanovg.rendering.shaders.Shaders
import net.iqaddons.mod.nanovg.util.Rect
import org.lwjgl.opengl.GL33.*

/**
 * Produces a Gaussian-blurred copy of whatever is currently in the default
 * framebuffer under [rect]. Used exclusively for "glass" panel backgrounds
 * (window chrome, popups) — never per-widget, per the performance budget in
 * the architecture brief.
 *
 * Downsampling before blurring (half resolution by default) keeps this cheap
 * enough to run once per glass panel per frame: fewer texels to sample, and
 * the softened result reads as more "glassy" than a full-res blur anyway.
 */
class BlurPass(private val downsampleFactor: Int = 2) : AutoCloseable {

    data class Capture(val texture: Int, val width: Int, val height: Int, val generation: Long)

    private var captureTexture = 0
    private var pingTexture = 0
    private var pongTexture = 0
    private var fbo = 0
    private var allocatedWidth = 0
    private var allocatedHeight = 0
    private var allocationGeneration = 0L
    private var capturedThisFrame = false

    private val shaderDelegate = lazy {
        ShaderProgram(Shaders.FULLSCREEN_TRIANGLE_VERTEX, Shaders.GAUSSIAN_BLUR_FRAGMENT)
    }
    private val shader by shaderDelegate
    private var vao = 0

    private fun ensureAllocated(width: Int, height: Int) {
        if (width == allocatedWidth && height == allocatedHeight && fbo != 0) return
        freeTextures()

        allocatedWidth = width
        allocatedHeight = height
        captureTexture = createTexture(width, height)
        pingTexture = createTexture(width, height)
        pongTexture = createTexture(width, height)
        if (fbo == 0) fbo = glGenFramebuffers()
        if (vao == 0) vao = glGenVertexArrays()
        allocationGeneration++
    }

    fun beginFrame() {
        capturedThisFrame = false
    }

    private fun createTexture(width: Int, height: Int): Int {
        val tex = glGenTextures()
        glBindTexture(GL_TEXTURE_2D, tex)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        return tex
    }

    /**
     * Captures [rect] (in framebuffer pixels, already accounting for GUI
     * scale/device pixel ratio) from the currently-bound read framebuffer,
     * blurs it by [radiusPixels], and returns the externally-owned GL texture
     * plus its allocation generation and actual size. A caller may reuse its
     * NanoVG wrapper only while [Capture.generation] remains unchanged.
     */
    fun capture(rect: Rect, radiusPixels: Float): Capture {
        val requestedWidth = (rect.width / downsampleFactor).toInt().coerceAtLeast(1)
        val requestedHeight = (rect.height / downsampleFactor).toInt().coerceAtLeast(1)
        // Keep one allocation stable for the entire NanoVG frame. NanoVG flushes
        // image-backed draw calls at nvgEndFrame, so reallocating sooner could
        // invalidate a texture already referenced by a queued draw.
        if (!capturedThisFrame) ensureAllocated(requestedWidth, requestedHeight)
        capturedThisFrame = true
        val width = allocatedWidth
        val height = allocatedHeight

        val previousFbo = glGetInteger(GL_FRAMEBUFFER_BINDING)

        // 1. Downsample-copy the region behind the panel into `captureTexture`.
        glBindFramebuffer(GL_FRAMEBUFFER, fbo)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, captureTexture, 0)
        glBindFramebuffer(GL_READ_FRAMEBUFFER, previousFbo)
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo)
        glBlitFramebuffer(
            rect.x.toInt(), rect.y.toInt(), rect.right.toInt(), rect.bottom.toInt(),
            0, 0, width, height,
            GL_COLOR_BUFFER_BIT, GL_LINEAR,
        )

        // 2. Horizontal pass: captureTexture -> pingTexture.
        blurPass(source = captureTexture, targetTexture = pingTexture, direction = 0f to (1f / height), radiusPixels)
        // 3. Vertical pass: pingTexture -> pongTexture.
        blurPass(source = pingTexture, targetTexture = pongTexture, direction = (1f / width) to 0f, radiusPixels)

        glBindFramebuffer(GL_FRAMEBUFFER, previousFbo)
        return Capture(pongTexture, width, height, allocationGeneration)
    }

    private fun blurPass(source: Int, targetTexture: Int, direction: Pair<Float, Float>, radiusPixels: Float) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, targetTexture, 0)
        glViewport(0, 0, allocatedWidth, allocatedHeight)

        shader.use()
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, source)
        shader.uniform1i("uSource", 0)
        shader.uniform2f("uTexelDirection", direction.first, direction.second)
        shader.uniform1f("uRadius", radiusPixels / downsampleFactor)

        glBindVertexArray(vao)
        glDrawArrays(GL_TRIANGLES, 0, 3)
        glBindVertexArray(0)
    }

    private fun freeTextures() {
        if (captureTexture != 0) glDeleteTextures(captureTexture)
        if (pingTexture != 0) glDeleteTextures(pingTexture)
        if (pongTexture != 0) glDeleteTextures(pongTexture)
        captureTexture = 0; pingTexture = 0; pongTexture = 0
    }

    override fun close() {
        freeTextures()
        if (fbo != 0) glDeleteFramebuffers(fbo)
        if (vao != 0) glDeleteVertexArrays(vao)
        if (shaderDelegate.isInitialized()) shader.close()
        fbo = 0
        vao = 0
    }
}
