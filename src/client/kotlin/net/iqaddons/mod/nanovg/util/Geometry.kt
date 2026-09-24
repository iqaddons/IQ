package net.iqaddons.mod.nanovg.util

/** Immutable 2D size in logical (GUI-scaled) pixels. */
data class Size(val width: Float, val height: Float) {
    companion object {
        val ZERO = Size(0f, 0f)
    }
}

/** Immutable 2D point in logical pixels. */
data class Point(val x: Float, val y: Float) {
    companion object {
        val ZERO = Point(0f, 0f)
    }
}

/** Axis-aligned rectangle, logical pixels, origin top-left. */
data class Rect(val x: Float, val y: Float, val width: Float, val height: Float) {
    val left: Float get() = x
    val top: Float get() = y
    val right: Float get() = x + width
    val bottom: Float get() = y + height
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
    val size: Size get() = Size(width, height)

    fun contains(px: Float, py: Float): Boolean =
        px >= left && px < right && py >= top && py < bottom

    /** Shrinks the rect symmetrically on every side by [amount]. */
    fun inset(amount: Float): Rect = inset(amount, amount, amount, amount)

    fun inset(left: Float, top: Float, right: Float, bottom: Float): Rect = Rect(
        x = x + left,
        y = y + top,
        width = (width - left - right).coerceAtLeast(0f),
        height = (height - top - bottom).coerceAtLeast(0f),
    )

    fun offset(dx: Float, dy: Float): Rect = Rect(x + dx, y + dy, width, height)

    fun intersects(other: Rect): Boolean =
        left < other.right && right > other.left && top < other.bottom && bottom > other.top

    companion object {
        val ZERO = Rect(0f, 0f, 0f, 0f)
    }
}

/** Per-corner radius; a single value applies uniformly via [uniform]. */
data class CornerRadius(
    val topLeft: Float,
    val topRight: Float,
    val bottomRight: Float,
    val bottomLeft: Float,
) {
    companion object {
        fun uniform(radius: Float) = CornerRadius(radius, radius, radius, radius)
        val ZERO = uniform(0f)
    }
}

/** Uniform or per-edge spacing, used for both padding and margin. */
data class Edges(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val horizontal: Float get() = left + right
    val vertical: Float get() = top + bottom

    companion object {
        fun all(value: Float) = Edges(value, value, value, value)
        fun symmetric(horizontal: Float = 0f, vertical: Float = 0f) =
            Edges(horizontal, vertical, horizontal, vertical)
        val ZERO = all(0f)
    }
}
