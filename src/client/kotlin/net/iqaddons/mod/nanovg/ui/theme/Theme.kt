package net.iqaddons.mod.nanovg.ui.theme

import net.iqaddons.mod.nanovg.rendering.backend.Color
import net.iqaddons.mod.nanovg.ui.animation.Easing

/** Full color palette for one theme. See DefaultTheme for the IQ palette. */
data class ColorScheme(
    val primary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceHover: Color,
    val surfaceActive: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val shadow: Color,
)

/** Sizing/spacing/visual-strength tokens. Widgets must never hardcode these. */
data class Metrics(
    val cornerRadiusSmall: Float,
    val cornerRadiusMedium: Float,
    val cornerRadiusLarge: Float,
    val spacingXs: Float,
    val spacingSm: Float,
    val spacingMd: Float,
    val spacingLg: Float,
    val spacingXl: Float,
    val borderThickness: Float,
    val blurStrength: Float,
    val shadowBlur: Float,
    val shadowSpread: Float,
)

/** Font family names + sizes/weights. Actual glyph loading lives in `assets.FontManager`. */
data class Typography(
    val fontFamily: String,
    val fontFamilyMedium: String,
    val fontFamilySemiBold: String,
    val fontFamilyBold: String,
    val sizeCaption: Float,
    val sizeBody: Float,
    val sizeLabel: Float,
    val sizeTitle: Float,
    val sizeHeadline: Float,
)

/** Default duration + easing curve for a category of motion. */
data class MotionSpec(val durationMs: Long, val easing: Easing)

/** Named motion presets so widgets ask for "hover" or "expand", not a raw duration. */
data class AnimationSpec(
    val hover: MotionSpec,
    val press: MotionSpec,
    val focus: MotionSpec,
    val expand: MotionSpec,
    val collapse: MotionSpec,
    val windowTransition: MotionSpec,
    val popupTransition: MotionSpec,
    val notificationTransition: MotionSpec,
)

/** Opacity tokens for disabled/hover/scrim states. */
data class Opacities(
    val disabled: Float,
    val hoverOverlay: Float,
    val scrim: Float,
)

/**
 * A complete, self-contained visual identity. Widgets are handed a [Theme]
 * (via [ThemeManager]) and read every visual value from it — never a literal
 * color, radius, or duration in widget code.
 */
data class Theme(
    val name: String,
    val colors: ColorScheme,
    val metrics: Metrics,
    val typography: Typography,
    val animation: AnimationSpec,
    val opacities: Opacities,
)
