package net.iqaddons.mod.nanovg.rendering.renderer

import net.iqaddons.mod.nanovg.assets.IconManager
import net.iqaddons.mod.nanovg.assets.TextureManager
import net.iqaddons.mod.nanovg.rendering.backend.*
import net.iqaddons.mod.nanovg.ui.theme.Theme
import net.iqaddons.mod.nanovg.util.CornerRadius
import net.iqaddons.mod.nanovg.util.Rect

/**
 * The object widgets actually hold a reference to during `draw()`.
 *
 * [RenderBackend] stays a low-level, stateless-per-call contract; this class
 * adds the handful of conveniences every widget needs (icon lookup, a
 * `withClip`/`withOpacity` scoping helper) without growing the backend
 * interface itself — keeping the backend swap-cost low is more important
 * than saving a few call sites a couple of lines.
 */
class Renderer2D(
    val backend: RenderBackend,
    private val icons: IconManager,
    private val textures: TextureManager,
) {
    fun fillRect(rect: Rect, color: Color, radius: CornerRadius = CornerRadius.ZERO) =
        backend.fillRect(rect, Paint.solid(color), radius)

    fun strokeRect(rect: Rect, color: Color, width: Float, radius: CornerRadius = CornerRadius.ZERO) =
        backend.strokeRect(rect, Paint.solid(color), width, radius)

    fun shadow(rect: Rect, radius: CornerRadius, theme: Theme) =
        backend.boxShadow(rect, radius, theme.metrics.shadowBlur, theme.metrics.shadowSpread, theme.colors.shadow)

    fun glassPanel(rect: Rect, radius: CornerRadius, theme: Theme) {
        backend.fillRect(rect, Paint.solid(theme.colors.surface.withAlpha(0.92f)), radius)
        backend.strokeRect(rect, Paint.solid(theme.colors.border), theme.metrics.borderThickness, radius)
    }

    fun text(text: String, x: Float, y: Float, size: Float, color: Color, fontFamily: String, align: TextAlign = TextAlign.LEFT) =
        backend.drawText(text, net.iqaddons.mod.nanovg.util.Point(x, y), fontFamily, size, color, align)

    fun textWidth(text: String, size: Float, fontFamily: String): Float =
        backend.measureText(text, fontFamily, size).width

    /** Draws a themed Lucide icon, tinted to [color], inside [rect]. */
    fun icon(name: String, rect: Rect, color: Color) {
        val path = icons.pathFor(name) ?: return
        backend.save()
        backend.translate(rect.x, rect.y)
        backend.scale(rect.width / IconManager.VIEWBOX_SIZE, rect.height / IconManager.VIEWBOX_SIZE)
        backend.strokePath(path, Paint.solid(color), IconManager.STROKE_WIDTH)
        backend.restore()
    }

    fun imageResourceRegion(resourcePath: String, rect: Rect, sourceX: Float, sourceY: Float, sourceWidth: Float, sourceHeight: Float) {
        val texture = textures.textureFor(resourcePath) ?: return
        val size = textures.textureSize(resourcePath) ?: return
        val scaleX = rect.width / sourceWidth
        val scaleY = rect.height / sourceHeight
        val imageRect = Rect(
            rect.x - sourceX * scaleX,
            rect.y - sourceY * scaleY,
            size.width * scaleX,
            size.height * scaleY,
        )

        withClip(rect) {
            backend.drawImage(texture, imageRect)
        }
    }

    inline fun withClip(rect: Rect, block: () -> Unit) {
        backend.save()
        backend.intersectScissor(rect)
        block()
        backend.restore()
    }

    inline fun withOpacity(alpha: Float, block: () -> Unit) {
        backend.save()
        backend.globalAlpha(alpha)
        block()
        backend.restore()
    }

    inline fun withTransform(dx: Float = 0f, dy: Float = 0f, sx: Float = 1f, sy: Float = 1f, block: () -> Unit) {
        backend.save()
        backend.translate(dx, dy)
        if (sx != 1f || sy != 1f) backend.scale(sx, sy)
        block()
        backend.restore()
    }
}
