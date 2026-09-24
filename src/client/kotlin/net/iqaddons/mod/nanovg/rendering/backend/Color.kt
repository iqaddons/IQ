package net.iqaddons.mod.nanovg.rendering.backend

/**
 * Straight (non-premultiplied) RGBA color, components in `[0f, 1f]`.
 *
 * A value class so passing colors around widgets/animations costs nothing
 * extra at runtime — it compiles down to a single packed [Long].
 */
@JvmInline
value class Color private constructor(private val packed: Long) {

    val r: Float get() = ((packed shr 48) and 0xFFFF) / 65535f
    val g: Float get() = ((packed shr 32) and 0xFFFF) / 65535f
    val b: Float get() = ((packed shr 16) and 0xFFFF) / 65535f
    val a: Float get() = (packed and 0xFFFF) / 65535f

    /** Returns a copy of this color with a different alpha. */
    fun withAlpha(alpha: Float): Color = of(r, g, b, alpha.coerceIn(0f, 1f))

    /** Linearly interpolates towards [target]; `t` is clamped to `[0, 1]`. */
    fun lerp(target: Color, t: Float): Color {
        val clamped = t.coerceIn(0f, 1f)
        return of(
            r + (target.r - r) * clamped,
            g + (target.g - g) * clamped,
            b + (target.b - b) * clamped,
            a + (target.a - a) * clamped,
        )
    }

    override fun toString(): String {
        fun channel(v: Float) = (v * 255f).toInt().coerceIn(0, 255)
        return "Color(r=${channel(r)}, g=${channel(g)}, b=${channel(b)}, a=%.2f)".format(a)
    }

    companion object {
        val TRANSPARENT = of(0f, 0f, 0f, 0f)

        fun of(r: Float, g: Float, b: Float, a: Float = 1f): Color {
            fun quantize(v: Float) = (v.coerceIn(0f, 1f) * 65535f).toLong() and 0xFFFF
            val bits = (quantize(r) shl 48) or (quantize(g) shl 32) or (quantize(b) shl 16) or quantize(a)
            return Color(bits)
        }

        /** Parses `#RRGGBB` or `#RRGGBBAA` (case-insensitive, `#` optional). */
        fun fromHex(hex: String): Color {
            val clean = hex.removePrefix("#")
            require(clean.length == 6 || clean.length == 8) { "Expected #RRGGBB or #RRGGBBAA, got '$hex'" }
            val r = clean.substring(0, 2).toInt(16) / 255f
            val g = clean.substring(2, 4).toInt(16) / 255f
            val b = clean.substring(4, 6).toInt(16) / 255f
            val a = if (clean.length == 8) clean.substring(6, 8).toInt(16) / 255f else 1f
            return of(r, g, b, a)
        }
    }
}
