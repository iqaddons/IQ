package net.iqaddons.mod.nanovg.ui.animation

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.PI

/**
 * A pure function `[0, 1] -> [0, 1]` (may overshoot past 1 for "back"-style
 * curves) mapping linear progress to eased progress. Stateless by design so
 * a single instance can be shared across every [Animatable] in the theme's
 * [IQ.ui.theme.AnimationSpec].
 */
fun interface Easing {
    fun ease(t: Float): Float

    companion object {
        val Linear = Easing { t -> t }

        val EaseOutExpo = Easing { t ->
            if (t >= 1f) 1f else 1f - 2f.pow(-10f * t)
        }

        val EaseOutQuart = Easing { t ->
            val f = 1f - t
            1f - f * f * f * f
        }

        val EaseOutBack = Easing { t ->
            val c1 = 1.70158f
            val c3 = c1 + 1f
            val f = t - 1f
            1f + c3 * f * f * f + c1 * f * f
        }

        val EaseInOutSine = Easing { t ->
            -(cos(PI.toFloat() * t) - 1f) / 2f
        }

        val EaseInOutCubic = Easing { t ->
            if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
        }
    }
}
