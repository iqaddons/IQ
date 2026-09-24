package net.iqaddons.mod.nanovg.rendering.backend

import net.iqaddons.mod.nanovg.util.CornerRadius
import net.iqaddons.mod.nanovg.util.Point
import net.iqaddons.mod.nanovg.util.Rect

/** Horizontal text anchor, backend-agnostic. */
enum class TextAlign { LEFT, CENTER, RIGHT }

/** Result of measuring a run of text without drawing it. */
data class TextMetrics(val width: Float, val ascent: Float, val descent: Float, val lineHeight: Float)

/**
 * The single abstraction every widget and renderer draws through.
 *
 * No widget, layout, or animation code is allowed to reference NanoVG (or
 * any other native rendering library) directly — it only ever sees this
 * interface. [IQ.rendering.nanovg.NanoVGBackend] is the current (and only)
 * implementation; a future Vulkan/Blaze3D-native backend can be dropped in
 * without touching a single widget.
 *
 * All coordinates are logical pixels in the current GUI-scaled space; the
 * backend is responsible for translating that to physical framebuffer
 * pixels.
 */
interface RenderBackend {

    /** Begins a frame at the given logical size and backing device pixel ratio. */
    fun beginFrame(width: Float, height: Float, devicePixelRatio: Float)

    /** Flushes batched draw calls and ends the frame. */
    fun endFrame()

    // --- Transform stack -------------------------------------------------

    fun save()
    fun restore()
    fun translate(dx: Float, dy: Float)
    fun scale(sx: Float, sy: Float)
    fun rotate(radians: Float)

    // --- Clipping ----------------------------------------------------------

    fun scissor(rect: Rect)
    fun intersectScissor(rect: Rect)
    fun resetScissor()

    // --- Opacity -----------------------------------------------------------

    /** Multiplies all subsequent draws (until restore) by [alpha] in `[0, 1]`. */
    fun globalAlpha(alpha: Float)

    // --- Shapes --------------------------------------------------------------

    fun fillRect(rect: Rect, paint: Paint, radius: CornerRadius = CornerRadius.ZERO)
    fun strokeRect(rect: Rect, paint: Paint, strokeWidth: Float, radius: CornerRadius = CornerRadius.ZERO)
    fun fillCircle(center: Point, radius: Float, paint: Paint)
    fun strokeCircle(center: Point, radius: Float, paint: Paint, strokeWidth: Float)
    fun line(from: Point, to: Point, paint: Paint, strokeWidth: Float)

    /** Draws a filled shape from an arbitrary vector path (icons, SVG outlines). */
    fun fillPath(path: VectorPath, paint: Paint)
    fun strokePath(path: VectorPath, paint: Paint, strokeWidth: Float)

    /**
     * Soft drop/inner shadow behind a rounded rect, expressed as a box
     * gradient rather than a real blur pass (matches NanoVG's native
     * `boxGradient`, which is what makes this cheap enough to use per-widget
     * per-frame without a separate blur render target).
     */
    fun boxShadow(rect: Rect, radius: CornerRadius, blur: Float, spread: Float, color: Color)

    /**
     * Gaussian-blurs whatever has already been drawn within [rect] on the
     * current layer. Backed by an offscreen framebuffer ping-pong; used
     * sparingly (background/glass panels), never per-widget.
     */
    fun backdropBlur(rect: Rect, radius: CornerRadius, strength: Float)

    // --- Text ----------------------------------------------------------------

    fun drawText(text: String, position: Point, fontFamily: String, size: Float, color: Color, align: TextAlign = TextAlign.LEFT)
    fun measureText(text: String, fontFamily: String, size: Float): TextMetrics

    // --- Images / icons --------------------------------------------------------

    fun drawImage(textureHandle: Int, rect: Rect, tint: Color? = null, cornerRadius: CornerRadius = CornerRadius.ZERO)
}

/**
 * A minimal, backend-agnostic vector path: a sequence of subpaths made of
 * move/line/cubic-bezier commands. Used for icon glyphs and arbitrary SVG-ish
 * shapes without exposing NanoVG's own path API to callers.
 */
class VectorPath {
    sealed interface Command {
        data class MoveTo(val x: Float, val y: Float) : Command
        data class LineTo(val x: Float, val y: Float) : Command
        data class CubicTo(val c1x: Float, val c1y: Float, val c2x: Float, val c2y: Float, val x: Float, val y: Float) : Command
        data object Close : Command
    }

    private val _commands = mutableListOf<Command>()
    val commands: List<Command> get() = _commands

    fun moveTo(x: Float, y: Float) = apply { _commands += Command.MoveTo(x, y) }
    fun lineTo(x: Float, y: Float) = apply { _commands += Command.LineTo(x, y) }
    fun cubicTo(c1x: Float, c1y: Float, c2x: Float, c2y: Float, x: Float, y: Float) =
        apply { _commands += Command.CubicTo(c1x, c1y, c2x, c2y, x, y) }
    fun close() = apply { _commands += Command.Close }
}
