package net.iqaddons.mod.nanovg.assets

import net.iqaddons.mod.nanovg.rendering.backend.VectorPath

/**
 * Registry of icon outlines drawn in Lucide's 24x24, 2px-stroke, round-cap
 * style. Paths are hand-authored [VectorPath] data rather than parsed from
 * `.svg` files: a real SVG file parser (path data, transforms, `<use>`,
 * viewBoxes, etc) is a substantial subsystem of its own, and for a fixed,
 * known icon set authoring the handful of moveTo/lineTo/cubicTo commands
 * directly is both simpler and avoids shipping a parser for input IQ
 * fully controls. If/when user-supplied SVG icons are needed, that parser
 * becomes its own module behind the same [pathFor] contract — no widget
 * code would need to change.
 *
 * Because icons are stroked with [Renderer2D.icon] using whatever [Color]
 * the caller passes, they automatically track theme/text color with no
 * per-icon tinting step.
 */
class IconManager {

    private val icons: Map<String, VectorPath> = buildMap {
        put("close", path {
            moveTo(6f, 6f); lineTo(18f, 18f)
            moveTo(18f, 6f); lineTo(6f, 18f)
        })
        put("check", path {
            moveTo(4f, 12.5f); lineTo(9.5f, 18f); lineTo(20f, 6f)
        })
        put("chevron-down", path {
            moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
        })
        put("chevron-up", path {
            moveTo(6f, 15f); lineTo(12f, 9f); lineTo(18f, 15f)
        })
        put("chevron-right", path {
            moveTo(9f, 6f); lineTo(15f, 12f); lineTo(9f, 18f)
        })
        put("chevron-left", path {
            moveTo(15f, 6f); lineTo(9f, 12f); lineTo(15f, 18f)
        })
        put("search", path {
            // circle approximated with cubic beziers, then the handle
            moveTo(20.5f, 11f)
            cubicTo(20.5f, 15.14f, 17.14f, 18.5f, 13f, 18.5f)
            cubicTo(8.86f, 18.5f, 5.5f, 15.14f, 5.5f, 11f)
            cubicTo(5.5f, 6.86f, 8.86f, 3.5f, 13f, 3.5f)
            cubicTo(17.14f, 3.5f, 20.5f, 6.86f, 20.5f, 11f)
            moveTo(21f, 21f); lineTo(16.65f, 16.65f)
        })
        put("eye", path {
            moveTo(2.5f, 12f)
            cubicTo(4.7f, 7.7f, 8.1f, 5.5f, 12f, 5.5f)
            cubicTo(15.9f, 5.5f, 19.3f, 7.7f, 21.5f, 12f)
            cubicTo(19.3f, 16.3f, 15.9f, 18.5f, 12f, 18.5f)
            cubicTo(8.1f, 18.5f, 4.7f, 16.3f, 2.5f, 12f)
            moveTo(15.5f, 12f)
            cubicTo(15.5f, 13.93f, 13.93f, 15.5f, 12f, 15.5f)
            cubicTo(10.07f, 15.5f, 8.5f, 13.93f, 8.5f, 12f)
            cubicTo(8.5f, 10.07f, 10.07f, 8.5f, 12f, 8.5f)
            cubicTo(13.93f, 8.5f, 15.5f, 10.07f, 15.5f, 12f)
        })
        put("eye-off", path {
            moveTo(3f, 3f); lineTo(21f, 21f)
            moveTo(10.6f, 5.7f)
            cubicTo(11.06f, 5.57f, 11.53f, 5.5f, 12f, 5.5f)
            cubicTo(15.9f, 5.5f, 19.3f, 7.7f, 21.5f, 12f)
            cubicTo(20.78f, 13.42f, 19.92f, 14.65f, 18.95f, 15.65f)
            moveTo(15.1f, 18f)
            cubicTo(14.12f, 18.33f, 13.08f, 18.5f, 12f, 18.5f)
            cubicTo(8.1f, 18.5f, 4.7f, 16.3f, 2.5f, 12f)
            cubicTo(3.68f, 9.7f, 5.2f, 7.99f, 7f, 6.88f)
            moveTo(8.65f, 8.65f)
            cubicTo(8.1f, 9.55f, 8.03f, 10.65f, 8.45f, 11.6f)
            cubicTo(8.87f, 12.55f, 9.72f, 13.25f, 10.75f, 13.5f)
            moveTo(13.35f, 15.15f)
            cubicTo(14.06f, 14.85f, 14.62f, 14.29f, 14.92f, 13.58f)
        })
        put("settings", path {
            moveTo(12f, 15f)
            cubicTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
            cubicTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
            cubicTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
            cubicTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
        })
        put("discord", path {
            moveTo(7f, 8f); cubicTo(9f, 6.8f, 15f, 6.8f, 17f, 8f)
            moveTo(7f, 8f); cubicTo(5.8f, 10.4f, 5.4f, 14.4f, 6.2f, 17f)
            moveTo(17f, 8f); cubicTo(18.2f, 10.4f, 18.6f, 14.4f, 17.8f, 17f)
            moveTo(6.2f, 17f); cubicTo(8.8f, 18.6f, 10.2f, 16.9f, 10.2f, 16.9f)
            moveTo(17.8f, 17f); cubicTo(15.2f, 18.6f, 13.8f, 16.9f, 13.8f, 16.9f)
            moveTo(9.4f, 12.5f); lineTo(9.5f, 12.5f)
            moveTo(14.5f, 12.5f); lineTo(14.6f, 12.5f)
        })
        put("heart", path {
            moveTo(12f, 20f)
            cubicTo(8f, 16.5f, 4f, 13.2f, 4f, 8.8f)
            cubicTo(4f, 6.4f, 5.8f, 4.8f, 8f, 4.8f)
            cubicTo(9.5f, 4.8f, 10.7f, 5.6f, 12f, 7f)
            cubicTo(13.3f, 5.6f, 14.5f, 4.8f, 16f, 4.8f)
            cubicTo(18.2f, 4.8f, 20f, 6.4f, 20f, 8.8f)
            cubicTo(20f, 13.2f, 16f, 16.5f, 12f, 20f)
        })
        put("edit", path {
            moveTo(12f, 20f); lineTo(20f, 20f)
            moveTo(16.5f, 3.5f); lineTo(20.5f, 7.5f)
            moveTo(19f, 9f); lineTo(8f, 20f)
            lineTo(4f, 20f); lineTo(4f, 16f)
            lineTo(15f, 5f)
        })
        put("trash", path {
            moveTo(3f, 6f); lineTo(21f, 6f)
            moveTo(8f, 6f); lineTo(8f, 4f); lineTo(16f, 4f); lineTo(16f, 6f)
            moveTo(6f, 6f); lineTo(7f, 21f); lineTo(17f, 21f); lineTo(18f, 6f)
            moveTo(10f, 11f); lineTo(10f, 17f)
            moveTo(14f, 11f); lineTo(14f, 17f)
        })
        put("rotate-ccw", path {
            moveTo(3f, 12f)
            cubicTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
            cubicTo(15.04f, 3f, 17.73f, 4.51f, 19.36f, 6.82f)
            moveTo(21f, 3f); lineTo(21f, 8f); lineTo(16f, 8f)
            moveTo(21f, 12f)
            cubicTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
            cubicTo(8.96f, 21f, 6.27f, 19.49f, 4.64f, 17.18f)
        })
        put("plus", path {
            moveTo(12f, 5f); lineTo(12f, 19f)
            moveTo(5f, 12f); lineTo(19f, 12f)
        })
        put("bell", path {
            moveTo(18f, 8f)
            cubicTo(18f, 6.4f, 17.37f, 4.88f, 16.24f, 3.76f)
            cubicTo(15.12f, 2.63f, 13.6f, 2f, 12f, 2f)
            cubicTo(10.4f, 2f, 8.88f, 2.63f, 7.76f, 3.76f)
            cubicTo(6.63f, 4.88f, 6f, 6.4f, 6f, 8f)
            cubicTo(6f, 15f, 3f, 17f, 3f, 17f)
            lineTo(21f, 17f)
            cubicTo(21f, 17f, 18f, 15f, 18f, 8f)
            moveTo(13.73f, 21f)
            cubicTo(13.55f, 21.3f, 13.3f, 21.55f, 13f, 21.72f)
            cubicTo(12.7f, 21.9f, 12.35f, 22f, 12f, 22f)
            cubicTo(11.65f, 22f, 11.3f, 21.9f, 11f, 21.72f)
            cubicTo(10.7f, 21.55f, 10.45f, 21.3f, 10.27f, 21f)
        })
        put("zap", path {
            moveTo(13f, 2f); lineTo(4f, 14f); lineTo(11f, 14f)
            lineTo(10f, 22f); lineTo(20f, 10f); lineTo(13f, 10f)
            lineTo(13f, 2f)
        })
        put("timer", path {
            moveTo(10f, 2f); lineTo(14f, 2f)
            moveTo(12f, 14f); lineTo(12f, 9f)
            moveTo(9f, 2f); lineTo(15f, 2f)
            moveTo(19.5f, 5.5f); lineTo(17.6f, 7.4f)
            moveTo(12f, 5f)
            cubicTo(7.58f, 5f, 4f, 8.58f, 4f, 13f)
            cubicTo(4f, 17.42f, 7.58f, 21f, 12f, 21f)
            cubicTo(16.42f, 21f, 20f, 17.42f, 20f, 13f)
            cubicTo(20f, 8.58f, 16.42f, 5f, 12f, 5f)
        })
        put("apple", path {
            moveTo(12f, 6f)
            cubicTo(13.2f, 4.6f, 14.5f, 4f, 16f, 4f)
            moveTo(12f, 6f)
            cubicTo(11.6f, 4.4f, 10.8f, 3.4f, 9.5f, 2.7f)
            moveTo(12f, 6f)
            cubicTo(9.8f, 4.7f, 6.4f, 5.5f, 5.2f, 8.5f)
            cubicTo(3.5f, 12.8f, 6.5f, 21f, 9.5f, 21f)
            cubicTo(10.5f, 21f, 11.1f, 20.4f, 12f, 20.4f)
            cubicTo(12.9f, 20.4f, 13.5f, 21f, 14.5f, 21f)
            cubicTo(17.5f, 21f, 20.5f, 12.8f, 18.8f, 8.5f)
            cubicTo(17.6f, 5.5f, 14.2f, 4.7f, 12f, 6f)
        })
        put("sword", path {
            moveTo(14.5f, 17.5f); lineTo(3f, 6f); lineTo(6f, 3f); lineTo(17.5f, 14.5f)
            moveTo(13f, 19f); lineTo(19f, 13f)
            moveTo(16f, 16f); lineTo(20f, 20f)
            moveTo(19f, 5f); lineTo(21f, 3f)
            moveTo(14.5f, 9.5f); lineTo(21f, 3f)
        })
        put("shield-crack", path {
            moveTo(12f, 22f)
            cubicTo(12f, 22f, 20f, 18f, 20f, 11f)
            lineTo(20f, 5f)
            lineTo(12f, 2f)
            lineTo(4f, 5f)
            lineTo(4f, 11f)
            cubicTo(4f, 18f, 12f, 22f, 12f, 22f)
            moveTo(12f, 2f); lineTo(12f, 8f); lineTo(9.5f, 10.5f); lineTo(13.5f, 13.5f); lineTo(11f, 17f)
        })
    }

    fun pathFor(name: String): VectorPath? = icons[name]

    fun isRegistered(name: String): Boolean = icons.containsKey(name)

    private inline fun path(block: VectorPath.() -> Unit): VectorPath = VectorPath().apply(block)

    companion object {
        /** Icons are authored against a 24x24 viewBox, matching Lucide's source grid. */
        const val VIEWBOX_SIZE = 24f
        const val STROKE_WIDTH = 2f
    }
}
