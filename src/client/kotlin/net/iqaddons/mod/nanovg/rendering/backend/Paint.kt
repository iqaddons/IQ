package net.iqaddons.mod.nanovg.rendering.backend

import net.iqaddons.mod.nanovg.util.Point

/**
 * What to fill/stroke a shape with. Deliberately backend-agnostic — a
 * [RenderBackend] implementation translates this into whatever native paint
 * object it needs (e.g. `NVGPaint`) at draw time, so widget code never
 * touches NanoVG types directly.
 */
sealed interface Paint {
    data class Solid(val color: Color) : Paint

    data class LinearGradient(
        val from: Point,
        val to: Point,
        val startColor: Color,
        val endColor: Color,
    ) : Paint

    data class RadialGradient(
        val center: Point,
        val innerRadius: Float,
        val outerRadius: Float,
        val startColor: Color,
        val endColor: Color,
    ) : Paint

    companion object {
        fun solid(color: Color): Paint = Solid(color)
    }
}
