package net.iqaddons.mod.nanovg.rendering.nanovg

import net.iqaddons.mod.nanovg.IqNanoVg
import net.iqaddons.mod.nanovg.rendering.backend.*
import net.iqaddons.mod.nanovg.rendering.blur.BlurPass
import net.iqaddons.mod.nanovg.util.CornerRadius
import net.iqaddons.mod.nanovg.util.Point
import net.iqaddons.mod.nanovg.util.Rect
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NVGPaint
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL3.NVG_IMAGE_NODELETE
import org.lwjgl.nanovg.NanoVGGL3.nvglCreateImageFromHandle
import org.lwjgl.system.MemoryStack
import org.lwjgl.opengl.GL33.*

/**
 * Translates the backend-agnostic [RenderBackend] contract into NanoVG calls.
 * This is the *only* class in the codebase allowed to import `org.lwjgl.nanovg.*`
 * for drawing purposes (font/texture loading live behind their own managers).
 *
 * @param fontHandle resolves a theme font family name to the NanoVG font
 *   handle registered for it (see `assets.FontManager`).
 */
class NanoVGBackend(
    private val context: NanoVGContext,
    private val fontHandle: (family: String) -> Int,
    private val blurEnabled: Boolean = true,
) : RenderBackend {

    private val blurPassDelegate = lazy { BlurPass() }
    private val blurPass by blurPassDelegate
    private data class BlurImage(val generation: Long, val handle: Int)
    private var blurImage: BlurImage? = null
    private var textDiagnosticsLogged = false

    private val ctx: Long get() = context.handle

    override fun beginFrame(width: Float, height: Float, devicePixelRatio: Float) {
        if (blurPassDelegate.isInitialized()) blurPass.beginFrame()
        nvgBeginFrame(ctx, width, height, devicePixelRatio)
    }

    override fun endFrame() = nvgEndFrame(ctx)

    override fun save() = nvgSave(ctx)
    override fun restore() = nvgRestore(ctx)
    override fun translate(dx: Float, dy: Float) = nvgTranslate(ctx, dx, dy)
    override fun scale(sx: Float, sy: Float) = nvgScale(ctx, sx, sy)
    override fun rotate(radians: Float) = nvgRotate(ctx, radians)

    override fun scissor(rect: Rect) = nvgScissor(ctx, rect.x, rect.y, rect.width, rect.height)
    override fun intersectScissor(rect: Rect) = nvgIntersectScissor(ctx, rect.x, rect.y, rect.width, rect.height)
    override fun resetScissor() = nvgResetScissor(ctx)

    override fun globalAlpha(alpha: Float) = nvgGlobalAlpha(ctx, alpha.coerceIn(0f, 1f))

    override fun fillRect(rect: Rect, paint: Paint, radius: CornerRadius) {
        nvgBeginPath(ctx)
        roundedRectPath(rect, radius)
        applyFill(paint)
    }

    override fun strokeRect(rect: Rect, paint: Paint, strokeWidth: Float, radius: CornerRadius) {
        nvgBeginPath(ctx)
        roundedRectPath(rect, radius)
        applyStroke(paint, strokeWidth)
    }

    override fun fillCircle(center: Point, radius: Float, paint: Paint) {
        nvgBeginPath(ctx)
        nvgCircle(ctx, center.x, center.y, radius)
        applyFill(paint)
    }

    override fun strokeCircle(center: Point, radius: Float, paint: Paint, strokeWidth: Float) {
        nvgBeginPath(ctx)
        nvgCircle(ctx, center.x, center.y, radius)
        applyStroke(paint, strokeWidth)
    }

    override fun line(from: Point, to: Point, paint: Paint, strokeWidth: Float) {
        nvgBeginPath(ctx)
        nvgMoveTo(ctx, from.x, from.y)
        nvgLineTo(ctx, to.x, to.y)
        nvgLineCap(ctx, NVG_ROUND)
        nvgLineJoin(ctx, NVG_ROUND)
        applyStroke(paint, strokeWidth)
    }

    override fun fillPath(path: VectorPath, paint: Paint) {
        nvgBeginPath(ctx)
        emitPath(path)
        applyFill(paint)
    }

    override fun strokePath(path: VectorPath, paint: Paint, strokeWidth: Float) {
        nvgBeginPath(ctx)
        emitPath(path)
        applyStroke(paint, strokeWidth)
    }

    override fun boxShadow(rect: Rect, radius: CornerRadius, blur: Float, spread: Float, color: Color) {
        MemoryStack.stackPush().use { stack ->
            val paint = NVGPaint.malloc(stack)
            val avgRadius = (radius.topLeft + radius.topRight + radius.bottomRight + radius.bottomLeft) / 4f
            nvgBoxGradient(
                ctx,
                rect.x - spread, rect.y - spread, rect.width + spread * 2f, rect.height + spread * 2f,
                avgRadius + spread, blur,
                color.toNvg(stack), color.withAlpha(0f).toNvg(stack),
                paint,
            )
            nvgBeginPath(ctx)
            nvgRect(ctx, rect.x - blur * 2, rect.y - blur * 2, rect.width + blur * 4, rect.height + blur * 4)
            roundedRectPath(rect, radius)
            nvgPathWinding(ctx, NVG_HOLE)
            nvgFillPaint(ctx, paint)
            nvgFill(ctx)
        }
    }

    override fun backdropBlur(rect: Rect, radius: CornerRadius, strength: Float) {
        if (!blurEnabled) return
        val capture = blurPass.capture(rect, strength)
        val current = blurImage
        val image = if (current == null || current.generation != capture.generation) {
            current?.let { nvgDeleteImage(ctx, it.handle) }
            nvglCreateImageFromHandle(
                ctx,
                capture.texture,
                capture.width,
                capture.height,
                NVG_IMAGE_NODELETE,
            ).also { blurImage = BlurImage(capture.generation, it) }
        } else {
            current.handle
        }
        drawImage(image, rect, tint = null, cornerRadius = radius)
    }

    override fun drawText(text: String, position: Point, fontFamily: String, size: Float, color: Color, align: TextAlign) {
        val handle = fontHandle(fontFamily)
        if (handle == -1) {
            logTextDiagnostics(text, fontFamily, handle, size, -1f, FloatArray(4), "missing-font")
            return
        }

        nvgFontFaceId(ctx, handle)
        nvgFontSize(ctx, size)
        nvgFontBlur(ctx, 0f)
        nvgTextLetterSpacing(ctx, 0f)
        nvgTextLineHeight(ctx, 1f)

        // Text bounds are only useful for the one-shot diagnostic. Calling
        // nvgTextBounds for every glyph run duplicates NanoVG layout work
        // before nvgText() performs the actual draw.
        if (!textDiagnosticsLogged) {
            val bounds = FloatArray(4)
            val measuredWidth = nvgTextBounds(ctx, 0f, 0f, text, bounds)
            logTextDiagnostics(text, fontFamily, handle, size, measuredWidth, bounds, "draw")
        }

        MemoryStack.stackPush().use { stack ->
            nvgFontFaceId(ctx, handle)
            nvgFontSize(ctx, size)
            nvgTextAlign(ctx, align.toNvgHorizontal() or NVG_ALIGN_TOP)
            nvgFillColor(ctx, color.toNvg(stack))
            nvgText(ctx, position.x, position.y, text)
        }
    }

    override fun measureText(text: String, fontFamily: String, size: Float): TextMetrics {
        nvgFontFaceId(ctx, fontHandle(fontFamily))
        nvgFontSize(ctx, size)

        val bounds = FloatArray(4)
        val width = nvgTextBounds(ctx, 0f, 0f, text, bounds)

        val ascent = FloatArray(1)
        val descent = FloatArray(1)
        val lineHeight = FloatArray(1)
        nvgTextMetrics(ctx, ascent, descent, lineHeight)

        return TextMetrics(width = width, ascent = ascent[0], descent = descent[0], lineHeight = lineHeight[0])
    }

    override fun drawImage(textureHandle: Int, rect: Rect, tint: Color?, cornerRadius: CornerRadius) {
        MemoryStack.stackPush().use { stack ->
            val paint = NVGPaint.malloc(stack)
            val alpha = tint?.a ?: 1f
            nvgImagePattern(ctx, rect.x, rect.y, rect.width, rect.height, 0f, textureHandle, alpha, paint)
            nvgBeginPath(ctx)
            roundedRectPath(rect, cornerRadius)
            nvgFillPaint(ctx, paint)
            nvgFill(ctx)
        }
    }

    fun dispose() {
        if (context.isReady) blurImage?.let { nvgDeleteImage(ctx, it.handle) }
        blurImage = null
        if (blurPassDelegate.isInitialized()) blurPass.close()
    }

    // --- helpers ---------------------------------------------------------

    private fun roundedRectPath(rect: Rect, radius: CornerRadius) {
        if (radius == CornerRadius.ZERO) {
            nvgRect(ctx, rect.x, rect.y, rect.width, rect.height)
        } else {
            nvgRoundedRectVarying(
                ctx, rect.x, rect.y, rect.width, rect.height,
                radius.topLeft, radius.topRight, radius.bottomRight, radius.bottomLeft,
            )
        }
    }

    private fun emitPath(path: VectorPath) {
        for (command in path.commands) when (command) {
            is VectorPath.Command.MoveTo -> nvgMoveTo(ctx, command.x, command.y)
            is VectorPath.Command.LineTo -> nvgLineTo(ctx, command.x, command.y)
            is VectorPath.Command.CubicTo -> nvgBezierTo(ctx, command.c1x, command.c1y, command.c2x, command.c2y, command.x, command.y)
            VectorPath.Command.Close -> nvgClosePath(ctx)
        }
    }

    private fun applyFill(paint: Paint) {
        MemoryStack.stackPush().use { stack ->
            when (paint) {
                is Paint.Solid -> nvgFillColor(ctx, paint.color.toNvg(stack))
                is Paint.LinearGradient -> {
                    val nvgPaint = NVGPaint.malloc(stack)
                    nvgLinearGradient(ctx, paint.from.x, paint.from.y, paint.to.x, paint.to.y, paint.startColor.toNvg(stack), paint.endColor.toNvg(stack), nvgPaint)
                    nvgFillPaint(ctx, nvgPaint)
                }
                is Paint.RadialGradient -> {
                    val nvgPaint = NVGPaint.malloc(stack)
                    nvgRadialGradient(ctx, paint.center.x, paint.center.y, paint.innerRadius, paint.outerRadius, paint.startColor.toNvg(stack), paint.endColor.toNvg(stack), nvgPaint)
                    nvgFillPaint(ctx, nvgPaint)
                }
            }
            nvgFill(ctx)
        }
    }

    private fun applyStroke(paint: Paint, strokeWidth: Float) {
        MemoryStack.stackPush().use { stack ->
            when (paint) {
                is Paint.Solid -> nvgStrokeColor(ctx, paint.color.toNvg(stack))
                is Paint.LinearGradient -> {
                    val nvgPaint = NVGPaint.malloc(stack)
                    nvgLinearGradient(ctx, paint.from.x, paint.from.y, paint.to.x, paint.to.y, paint.startColor.toNvg(stack), paint.endColor.toNvg(stack), nvgPaint)
                    nvgStrokePaint(ctx, nvgPaint)
                }
                is Paint.RadialGradient -> {
                    val nvgPaint = NVGPaint.malloc(stack)
                    nvgRadialGradient(ctx, paint.center.x, paint.center.y, paint.innerRadius, paint.outerRadius, paint.startColor.toNvg(stack), paint.endColor.toNvg(stack), nvgPaint)
                    nvgStrokePaint(ctx, nvgPaint)
                }
            }
            nvgStrokeWidth(ctx, strokeWidth)
            nvgStroke(ctx)
        }
    }

    private fun TextAlign.toNvgHorizontal(): Int = when (this) {
        TextAlign.LEFT -> NVG_ALIGN_LEFT
        TextAlign.CENTER -> NVG_ALIGN_CENTER
        TextAlign.RIGHT -> NVG_ALIGN_RIGHT
    }

    private fun Color.toNvg(stack: MemoryStack): NVGColor =
        NVGColor.malloc(stack).also { nvgRGBAf(r, g, b, a, it) }

    private fun logTextDiagnostics(
        text: String,
        fontFamily: String,
        handle: Int,
        size: Float,
        width: Float,
        bounds: FloatArray,
        phase: String,
    ) {
        if (textDiagnosticsLogged) return
        textDiagnosticsLogged = true
        val viewport = IntArray(4)
        glGetIntegerv(GL_VIEWPORT, viewport)
        IqNanoVg.logger.info(
            "NanoVG text diagnostics phase=$phase text='$text' family=$fontFamily handle=$handle size=$size " +
                "width=$width bounds=[${bounds.getOrElse(0) { 0f }},${bounds.getOrElse(1) { 0f }}," +
                "${bounds.getOrElse(2) { 0f }},${bounds.getOrElse(3) { 0f }}] " +
                "glError=${glGetError()} framebuffer=${glGetInteger(GL_FRAMEBUFFER_BINDING)} " +
                "program=${glGetInteger(GL_CURRENT_PROGRAM)} activeTexture=${glGetInteger(GL_ACTIVE_TEXTURE)} " +
                "texture2D=${glGetInteger(GL_TEXTURE_BINDING_2D)} sampler=${glGetInteger(GL_SAMPLER_BINDING)} " +
                "blend=${glIsEnabled(GL_BLEND)} scissor=${glIsEnabled(GL_SCISSOR_TEST)} stencil=${glIsEnabled(GL_STENCIL_TEST)} " +
                "unpackAlignment=${glGetInteger(GL_UNPACK_ALIGNMENT)} viewport=[${viewport.joinToString()}]",
        )
    }

}
