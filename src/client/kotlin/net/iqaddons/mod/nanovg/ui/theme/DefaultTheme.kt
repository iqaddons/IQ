package net.iqaddons.mod.nanovg.ui.theme

import net.iqaddons.mod.nanovg.rendering.backend.Color
import net.iqaddons.mod.nanovg.ui.animation.Easing

/** Centralized IQ visual tokens for the NanoVG UI. */
object DefaultTheme {

    val colors = ColorScheme(
        primary = Color.fromHex("#EC4BAF"),
        accent = Color.fromHex("#F7A8DC"),
        background = Color.fromHex("#07040B"),
        surface = Color.fromHex("#120E1C"),
        surfaceHover = Color.fromHex("#1F1330"),
        surfaceActive = Color.fromHex("#2A1A39"),
        border = Color.fromHex("#7A3D75"),
        textPrimary = Color.fromHex("#F8F1FB"),
        textSecondary = Color.fromHex("#C9B4D5"),
        textDisabled = Color.fromHex("#756579"),
        success = Color.fromHex("#6EE7B7"),
        warning = Color.fromHex("#F7C948"),
        danger = Color.fromHex("#F871A6"),
        shadow = Color.of(0f, 0f, 0f, 0.58f),
    )

    val metrics = Metrics(
        cornerRadiusSmall = 3f,
        cornerRadiusMedium = 5f,
        cornerRadiusLarge = 7f,
        spacingXs = 4f,
        spacingSm = 8f,
        spacingMd = 12f,
        spacingLg = 20f,
        spacingXl = 32f,
        borderThickness = 1f,
        blurStrength = 16f,
        shadowBlur = 18f,
        shadowSpread = -3f,
    )

    val typography = Typography(
        fontFamily = "Rajdhani-Regular",
        fontFamilyMedium = "Rajdhani-Medium",
        fontFamilySemiBold = "Rajdhani-SemiBold",
        fontFamilyBold = "Rajdhani-Bold",
        sizeCaption = 10f,
        sizeBody = 13f,
        sizeLabel = 14f,
        sizeTitle = 18f,
        sizeHeadline = 26f,
    )

    val animation = AnimationSpec(
        hover = MotionSpec(150, Easing.EaseOutQuart),
        press = MotionSpec(100, Easing.EaseOutExpo),
        focus = MotionSpec(150, Easing.EaseOutQuart),
        expand = MotionSpec(220, Easing.EaseOutBack),
        collapse = MotionSpec(180, Easing.EaseInOutCubic),
        windowTransition = MotionSpec(260, Easing.EaseOutExpo),
        popupTransition = MotionSpec(180, Easing.EaseOutQuart),
        notificationTransition = MotionSpec(220, Easing.EaseOutBack),
    )

    val opacities = Opacities(
        disabled = 0.4f,
        hoverOverlay = 0.06f,
        scrim = 0.5f,
    )

    val theme = Theme(
        name = "IQ Rose",
        colors = colors,
        metrics = metrics,
        typography = typography,
        animation = animation,
        opacities = opacities,
    )
}
