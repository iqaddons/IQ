package net.iqaddons.mod.hud.nano

import net.iqaddons.mod.hud.HudEditScreen
import net.iqaddons.mod.hud.HudManager
import net.iqaddons.mod.hud.component.HudLine
import net.iqaddons.mod.hud.element.HudAnchor
import net.iqaddons.mod.hud.element.HudWidget
import net.iqaddons.mod.features.widgets.ChestCounterWidget
import net.iqaddons.mod.features.widgets.CratePriorityWidget
import net.iqaddons.mod.features.widgets.KuudraNotificationsWidget
import net.iqaddons.mod.utils.TextColor
import net.iqaddons.mod.nanovg.IqNanoVg
import net.iqaddons.mod.nanovg.rendering.backend.Color
import net.iqaddons.mod.nanovg.rendering.backend.TextAlign
import net.iqaddons.mod.nanovg.rendering.renderer.Renderer2D
import net.iqaddons.mod.nanovg.util.CornerRadius
import net.iqaddons.mod.nanovg.util.Rect
import net.iqaddons.mod.screen.nano.IqNanoGlobalConfigScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

object IqHudNanoRenderer {
    private const val FONT_SIZE = 9.6f
    private const val HEART_SIZE = 8.8f
    private const val HEART_ADVANCE = 9.4f
    private const val LINE_GAP = 1f
    private const val BACKDROP_SOFT_ALPHA = 0.16f
    private const val CHEST_COUNTER_ICON_RESOURCE = "/assets/iq/textures/widgets/kuudra_icon.png"
    private const val CHEST_COUNTER_ICON_U = 49f
    private const val CHEST_COUNTER_ICON_V = 38f
    private const val CHEST_COUNTER_ICON_SOURCE_WIDTH = 186f
    private const val CHEST_COUNTER_ICON_SOURCE_HEIGHT = 208f
    private const val CHEST_COUNTER_ICON_GAP = 3f
    private const val CHEST_COUNTER_ICON_Y_OFFSET = -1f
    private const val EDITOR_PANEL_PADDING_X = 4.5f
    private const val EDITOR_PANEL_PADDING_Y = 3.5f
    private const val EDITOR_PANEL_STROKE = 1.25f
    private const val EDITOR_LABEL_FONT_SIZE = 8.4f
    private const val EDITOR_LABEL_PADDING_X = 5.5f
    private const val EDITOR_LABEL_PADDING_Y = 3.0f
    private data class Metrics(val width: Int, val height: Int)

    @JvmStatic
    fun renderCurrentHud(client: Minecraft) {
        if (!IqNanoGlobalConfigScreen.isSharedModernHudStyle()) return
        if (!IqNanoGlobalConfigScreen.isSharedGlobalHudEnabled()) return
        if (client.player == null || client.level == null || client.options.hideGui || client.options.keyPlayerList.isDown) return
        val screen = client.screen
        if (screen is HudEditScreen) return
        if (!isAllowedInGameScreen(screen)) return

        val manager = HudManager.getIfInitialized() ?: return
        val backdropMode = isBackdropBlurScreen(screen)

        IqNanoVg.renderOverlay(client) { renderer, mouseX, mouseY ->
            manager.widgets.forEach { widget ->
                if (widget.shouldRender() && !widget.isNanoActive()) widget.activate()
                if (!widget.shouldRender() && widget.isNanoActive()) widget.deactivate()
                if (widget.isNanoActive()) drawWidget(renderer, widget, mouseX.toDouble(), mouseY.toDouble(), backdropMode)
            }

            if (!backdropMode) {
                manager.widgets.forEach { widget ->
                    if (widget.isNanoActive()) drawHoveredTooltip(renderer, widget, mouseX.toFloat(), mouseY.toFloat())
                }
            }
        }
    }

    private fun isAllowedInGameScreen(screen: Screen?): Boolean {
        if (screen == null) return true
        if (screen is ChatScreen || screen is PauseScreen || screen is AbstractContainerScreen<*>) return true
        return screen.javaClass.name.startsWith("net.iqaddons.mod.screen.")
    }

    private fun isBackdropBlurScreen(screen: Screen?): Boolean {
        if (screen == null || screen is ChatScreen) return false
        return screen is PauseScreen || screen.javaClass.name.startsWith("net.iqaddons.mod.screen.")
    }

    @JvmStatic
    fun handleClick(manager: HudManager, mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!IqNanoGlobalConfigScreen.isSharedModernHudStyle()) return false
        if (button != 0) return false

        manager.widgets.forEach { widget ->
            if (widget.isNanoActive() && widget.shouldRender() && handleWidgetClick(widget)) {
                return true
            }
        }

        return false
    }

    @JvmStatic
    fun drawEditorWidgets(renderer: Renderer2D, mouseX: Float, mouseY: Float) {
        if (!IqNanoGlobalConfigScreen.isSharedModernHudStyle()) return
        val manager = HudManager.getIfInitialized() ?: return
        manager.widgets.forEach { widget ->
            if (widget.isNanoEnabled()) {
                drawWidget(renderer, widget, mouseX.toDouble(), mouseY.toDouble(), false)
            }
        }
        drawEditorSelectedLabels(renderer)
    }

    private fun drawWidget(renderer: Renderer2D, widget: HudWidget, mouseX: Double, mouseY: Double, backdropMode: Boolean) {
        if (widget is CratePriorityWidget) {
            drawCratePriority(renderer, widget, mouseX, mouseY, backdropMode)
            return
        }

        val textRenderer = Minecraft.getInstance().font ?: return
        val lines = widget.nanoRenderableLines.filter { it.shouldRender() }
        if (lines.isEmpty()) return
        val metrics = measureWidget(renderer, textRenderer, widget, lines)
        widget.setNanoDimensions(metrics.width, metrics.height)

        val scale = widget.scale.coerceAtLeast(0.01f)
        val absoluteX = nanoAbsoluteX(widget, metrics, scale)
        val absoluteY = nanoAbsoluteY(widget, metrics, scale)
        val scaledX = absoluteX / scale
        val scaledY = absoluteY / scale
        var currentX = scaledX
        var currentY = scaledY
        var atLineStart = true

        if (!backdropMode) drawEditorSelection(renderer, widget, metrics, scale, absoluteX, absoluteY, mouseX, mouseY)

        renderer.backend.save()
        renderer.backend.scale(scale, scale)

        if (widget is ChestCounterWidget) {
            val iconHeight = textRenderer.lineHeight.toFloat().coerceAtLeast(1f)
            val iconWidth = (iconHeight * CHEST_COUNTER_ICON_SOURCE_WIDTH / CHEST_COUNTER_ICON_SOURCE_HEIGHT).coerceAtLeast(1f)
            renderer.imageResourceRegion(
                CHEST_COUNTER_ICON_RESOURCE,
                Rect(scaledX, scaledY + CHEST_COUNTER_ICON_Y_OFFSET, iconWidth, iconHeight),
                CHEST_COUNTER_ICON_U,
                CHEST_COUNTER_ICON_V,
                CHEST_COUNTER_ICON_SOURCE_WIDTH,
                CHEST_COUNTER_ICON_SOURCE_HEIGHT,
            )
        }

        fun drawPass(opacity: Float, updateHover: Boolean, shadow: Boolean) {
            currentX = scaledX
            currentY = scaledY
            atLineStart = true

            renderer.withOpacity(opacity) {
                lines.forEach { line ->
                    if (atLineStart) {
                        val start = widget.getNanoLineStartX(textRenderer, line)
                        currentX = scaledX + if (widget.isNanoLineCentered(textRenderer, line)) {
                            ((metrics.width - formattedWidth(renderer, line.getNanoText())) / 2f).coerceAtLeast(0f)
                        } else {
                            start
                        }
                    }

                    if (updateHover) {
                        updateNanoHover(
                            line,
                            currentX * scale,
                            currentY * scale,
                            formattedWidth(renderer, line.getNanoText()) * scale,
                            textRenderer.lineHeight * scale,
                            mouseX,
                            mouseY
                        )
                    }
                    drawFormattedLine(renderer, line, currentX, currentY, shadow)

                    if (line.hasLineBreak()) {
                        currentY += textRenderer.lineHeight + LINE_GAP
                        atLineStart = true
                    } else {
                        currentX += formattedWidth(renderer, line.getNanoText())
                        atLineStart = false
                    }
                }
            }
        }

        if (backdropMode) {
            drawPass(BACKDROP_SOFT_ALPHA, false, false)
        } else {
            drawPass(1f, true, true)
        }
        renderer.backend.restore()
    }

    private fun handleWidgetClick(widget: HudWidget): Boolean {
        widget.nanoRenderableLines.forEach { line ->
            if (line.shouldRender() && line.isNanoHovered() && line.hasNanoClickAction()) {
                line.runNanoClickAction()
                return true
            }
        }

        return false
    }

    private fun drawHoveredTooltip(renderer: Renderer2D, widget: HudWidget, mouseX: Float, mouseY: Float) {
        widget.nanoRenderableLines
            .firstOrNull { it.shouldRender() && it.isNanoHovered() && !it.nanoTooltipText.isNullOrBlank() }
            ?.nanoTooltipText
            ?.let { drawTooltip(renderer, it, mouseX, mouseY) }
    }

    private fun updateNanoHover(line: HudLine, x: Float, y: Float, width: Float, height: Float, mouseX: Double, mouseY: Double) {
        if (!line.isInteractive || line.getNanoText().isEmpty()) return

        val wasHovered = line.isNanoHovered()
        val isNowHovered = isInside(mouseX.toFloat(), mouseY.toFloat(), x, y, width, height)
        line.setNanoHovered(isNowHovered)

        if (isNowHovered && !wasHovered) {
            line.runNanoMouseEnterAction()
        } else if (!isNowHovered && wasHovered) {
            line.runNanoMouseLeaveAction()
        }
    }

    private fun drawTooltip(renderer: Renderer2D, text: String, mouseX: Float, mouseY: Float) {
        val lines = text.split('\n').filter { it.isNotBlank() }
        if (lines.isEmpty()) return

        val size = FONT_SIZE
        val paddingX = 8f
        val paddingY = 5f
        val lineStep = size + 2.4f
        val maxWidth = lines.maxOf { formattedWidth(renderer, it) }
        val width = maxWidth + paddingX * 2f + 4f
        val height = lines.size * lineStep + paddingY * 2f - 1.5f
        val window = Minecraft.getInstance().window
        var x = mouseX + 10f
        var y = mouseY - 4f

        if (x + width > window.guiScaledWidth - 6f) x = mouseX - width - 10f
        if (y + height > window.guiScaledHeight - 6f) y = mouseY - height - 8f
        x = x.coerceAtLeast(6f)
        y = y.coerceAtLeast(6f)

        val rect = Rect(x, y, width, height)
        renderer.backend.boxShadow(rect, CornerRadius.uniform(3f), 10f, -2f, Color.of(0f, 0f, 0f, 0.62f))
        renderer.fillRect(rect, IqNanoGlobalConfigScreen.themeModalSurface().withAlpha(0.96f), CornerRadius.uniform(3f))
        renderer.strokeRect(rect, IqNanoGlobalConfigScreen.themeBorder().withAlpha(0.72f), 1f, CornerRadius.uniform(3f))

        var textY = rect.y + paddingY + 0.8f
        lines.forEach {
            drawFormattedLine(renderer, it, rect.x + paddingX, textY, 1f, TextAlign.LEFT, true, IqNanoGlobalConfigScreen.themeText())
            textY += lineStep
        }
    }

    private fun drawEditorSelection(
        renderer: Renderer2D,
        widget: HudWidget,
        metrics: Metrics,
        scale: Float,
        absoluteX: Float,
        absoluteY: Float,
        mouseX: Double,
        mouseY: Double,
    ) {
        if (Minecraft.getInstance().screen !is HudEditScreen) return
        val rect = Rect(
            absoluteX - EDITOR_PANEL_PADDING_X,
            absoluteY - EDITOR_PANEL_PADDING_Y,
            metrics.width * scale + EDITOR_PANEL_PADDING_X * 2f,
            metrics.height * scale + EDITOR_PANEL_PADDING_Y * 2f,
        )
        val radius = CornerRadius.uniform(4f)
        val hovered = widget.isMouseOver(mouseX, mouseY)
        if (hovered || widget.isSelected) {
            val fillAlpha = if (widget.isSelected) 0.24f else 0.10f
            renderer.fillRect(rect, IqNanoGlobalConfigScreen.themeModalSurface().withAlpha(editorOverlayAlpha(fillAlpha)), radius)
        }
        if (widget.isSelected) {
            val accent = IqNanoGlobalConfigScreen.themeAccentHot()
            renderer.strokeRect(rect, accent.withAlpha(editorOverlayAlpha(0.92f)), EDITOR_PANEL_STROKE, radius)
            renderer.strokeRect(rect.inset(1.25f), Color.fromHex("#FFFFFF").withAlpha(editorOverlayAlpha(0.08f)), 0.75f, CornerRadius.uniform(3f))
        } else if (hovered) {
            renderer.strokeRect(rect, IqNanoGlobalConfigScreen.themeAccent().withAlpha(editorOverlayAlpha(0.42f)), 1f, radius)
        }
    }

    private fun drawEditorSelectedLabels(renderer: Renderer2D) {
        if (Minecraft.getInstance().screen !is HudEditScreen) return
        val manager = HudManager.getIfInitialized() ?: return
        val textRenderer = Minecraft.getInstance().font ?: return

        manager.widgets.forEach { widget ->
            if (!widget.isNanoEnabled() || !widget.isSelected) return@forEach
            val metrics = when (widget) {
                is CratePriorityWidget -> {
                    val raw = widget.getNanoRenderText(true)
                    if (raw.isEmpty()) return@forEach
                    Metrics(
                        maxOf(
                            formattedWidth(renderer, raw),
                            formattedWidth(renderer, widget.nanoMinReferenceText),
                        ).toInt().coerceAtLeast(1),
                        textRenderer.lineHeight.coerceAtLeast(FONT_SIZE.toInt())
                    )
                }
                else -> {
                    val lines = widget.nanoRenderableLines.filter { it.shouldRender() }
                    if (lines.isEmpty()) return@forEach
                    measureWidget(renderer, textRenderer, widget, lines)
                }
            }

            val scale = widget.scale.coerceAtLeast(0.01f)
            val absoluteX = nanoAbsoluteX(widget, metrics, scale)
            val absoluteY = nanoAbsoluteY(widget, metrics, scale)
            val rect = Rect(
                absoluteX - EDITOR_PANEL_PADDING_X,
                absoluteY - EDITOR_PANEL_PADDING_Y,
                metrics.width * scale + EDITOR_PANEL_PADDING_X * 2f,
                metrics.height * scale + EDITOR_PANEL_PADDING_Y * 2f,
            )
            drawSelectedWidgetLabel(renderer, widget, rect, IqNanoGlobalConfigScreen.themeAccentHot())
        }
    }

    private fun drawSelectedWidgetLabel(renderer: Renderer2D, widget: HudWidget, rect: Rect, accent: Color) {
        val label = widget.displayName
        if (label.isBlank()) return

        val font = IqNanoGlobalConfigScreen.fontBold()
        val labelWidth = renderer.textWidth(label, EDITOR_LABEL_FONT_SIZE, font) + EDITOR_LABEL_PADDING_X * 2f
        val labelHeight = EDITOR_LABEL_FONT_SIZE + EDITOR_LABEL_PADDING_Y * 2f - 1f
        val windowWidth = Minecraft.getInstance().window.guiScaledWidth.toFloat()
        val x = (rect.x + (rect.width - labelWidth) / 2f).coerceIn(4f, windowWidth - labelWidth - 4f)
        val y = if (rect.y - labelHeight - 3f >= 4f) rect.y - labelHeight - 3f else rect.bottom + 3f
        val labelRect = Rect(x, y, labelWidth, labelHeight)
        val radius = CornerRadius.uniform(3.5f)

        renderer.fillRect(labelRect, IqNanoGlobalConfigScreen.themeModalSurface().withAlpha(editorOverlayAlpha(0.86f)), radius)
        renderer.strokeRect(labelRect, accent.withAlpha(editorOverlayAlpha(0.82f)), 1f, radius)
        renderer.text(
            label,
            labelRect.x + labelRect.width / 2f,
            labelRect.y + EDITOR_LABEL_PADDING_Y - 0.2f,
            EDITOR_LABEL_FONT_SIZE,
            IqNanoGlobalConfigScreen.themeText(),
            font,
            TextAlign.CENTER
        )
    }

    private fun drawCratePriority(renderer: Renderer2D, widget: CratePriorityWidget, mouseX: Double, mouseY: Double, backdropMode: Boolean) {
        val preview = Minecraft.getInstance().screen is HudEditScreen
        val raw = widget.getNanoRenderText(preview)
        if (raw.isEmpty()) return

        val scale = widget.scale.coerceAtLeast(0.01f)
        val width = maxOf(
            formattedWidth(renderer, raw),
            formattedWidth(renderer, widget.nanoMinReferenceText),
        ).toInt().coerceAtLeast(1)
        val height = (Minecraft.getInstance().font?.lineHeight ?: FONT_SIZE.toInt()).coerceAtLeast(FONT_SIZE.toInt())
        val metrics = Metrics(width, height)
        widget.setNanoDimensions(metrics.width, metrics.height)
        val absoluteX = nanoAbsoluteX(widget, metrics, scale)
        val absoluteY = nanoAbsoluteY(widget, metrics, scale)
        val scaledX = absoluteX / scale
        val scaledY = absoluteY / scale - widget.getNanoSlideOffset(preview)
        val centerX = scaledX + width / 2f
        val alpha = widget.getNanoAlpha(preview).coerceIn(0f, 1f)

        if (!backdropMode) drawEditorSelection(renderer, widget, metrics, scale, absoluteX, absoluteY, mouseX, mouseY)

        renderer.backend.save()
        renderer.backend.scale(scale, scale)
        if (backdropMode) {
            drawFormattedLine(renderer, raw, centerX, scaledY, alpha * BACKDROP_SOFT_ALPHA, TextAlign.CENTER, false, widget.nanoTextColor.toNanoColor())
        } else {
            drawFormattedLine(renderer, raw, centerX, scaledY, alpha, TextAlign.CENTER, true, widget.nanoTextColor.toNanoColor())
        }
        renderer.backend.restore()
    }

    private fun drawFormattedLine(renderer: Renderer2D, line: HudLine, x: Float, y: Float, shadow: Boolean = line.hasNanoShadow()) {
        val raw = line.getNanoText()
        if (raw.isEmpty()) return
        drawFormattedLine(renderer, raw, x, y, 1f, TextAlign.LEFT, shadow)
    }

    private fun drawFormattedLine(
        renderer: Renderer2D,
        raw: String,
        x: Float,
        y: Float,
        alpha: Float,
        align: TextAlign,
        shadow: Boolean,
        baseColor: Color = Color.fromHex("#FFFFFF"),
    ) {
        var cursor = x
        if (align == TextAlign.CENTER) cursor -= formattedWidth(renderer, raw) / 2f
        var color = baseColor
        var bold = false
        val segment = StringBuilder()

        fun flush() {
            if (segment.isEmpty()) return
            val text = segment.toString()
            val font = if (bold) fontBold() else fontRegular()
            if (shadow) {
                renderer.text(text, cursor + 0.43f, y + 0.43f, FONT_SIZE, Color.of(0f, 0f, 0f, 0.92f * alpha), font)
                renderer.text(text, cursor + 0.24f, y + 0.24f, FONT_SIZE, Color.of(0f, 0f, 0f, 0.46f * alpha), font)
            }
            val finalColor = color.withAlpha(color.a * alpha)
            renderer.text(text, cursor, y, FONT_SIZE, finalColor, font)
            renderer.text(text, cursor + 0.12f, y, FONT_SIZE, finalColor.withAlpha(finalColor.a * 0.22f), font)
            cursor += renderer.textWidth(text, FONT_SIZE, font)
            segment.clear()
        }

        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '§' && i + 1 < raw.length) {
                flush()
                when (val code = raw[i + 1].lowercaseChar()) {
                    'l' -> bold = true
                    'r' -> {
                        color = baseColor
                        bold = false
                    }
                    in minecraftColors.keys -> color = minecraftColors.getValue(code)
                }
                i += 2
                continue
            }
            if (isHeart(c)) {
                flush()
                drawHeart(renderer, cursor, y, color.withAlpha(color.a * alpha), shadow, alpha)
                cursor += HEART_ADVANCE
                i++
                continue
            }
            segment.append(c)
            i++
        }
        flush()
    }

    private fun drawHeart(renderer: Renderer2D, x: Float, y: Float, color: Color, shadow: Boolean, alpha: Float) {
        val rect = Rect(x, y + ((FONT_SIZE - HEART_SIZE) * 0.5f) - 0.05f, HEART_SIZE, HEART_SIZE)
        if (shadow) {
            renderer.icon("heart", rect.offset(0.42f, 0.42f), Color.of(0f, 0f, 0f, 0.88f * alpha))
            renderer.icon("heart", rect.offset(0.22f, 0.22f), Color.of(0f, 0f, 0f, 0.40f * alpha))
        }
        renderer.icon("heart", rect, color)
    }

    private fun measureWidget(renderer: Renderer2D, textRenderer: net.minecraft.client.gui.Font, widget: HudWidget, lines: List<HudLine>): Metrics {
        var maxWidth = 0f
        var currentWidth = 0f
        var rows = 0

        var atLineStart = true
        lines.forEach { line ->
            if (atLineStart) {
                currentWidth = widget.getNanoLineStartX(textRenderer, line)
            }
            currentWidth += formattedWidth(renderer, line.getNanoText())
            if (line.hasLineBreak()) {
                maxWidth = maxOf(maxWidth, currentWidth)
                currentWidth = 0f
                rows++
                atLineStart = true
            } else {
                atLineStart = false
            }
        }

        maxWidth = maxOf(maxWidth, currentWidth)
        maxWidth = maxOf(maxWidth, nanoMinReferenceWidth(renderer, widget))
        if (currentWidth > 0f || rows == 0) rows++
        val height = rows * textRenderer.lineHeight + (rows - 1) * LINE_GAP
        return Metrics(maxWidth.toInt().coerceAtLeast(20), height.toInt().coerceAtLeast(textRenderer.lineHeight))
    }

    private fun nanoAbsoluteX(widget: HudWidget, metrics: Metrics, scale: Float): Float {
        val screen = HudAnchor.getScreenDimensions()
        val scaledWidth = (metrics.width * scale).toInt().coerceAtLeast(1)
        return widget.anchor.calculateX(screen[0], scaledWidth, widget.x)
    }

    private fun nanoAbsoluteY(widget: HudWidget, metrics: Metrics, scale: Float): Float {
        val screen = HudAnchor.getScreenDimensions()
        val scaledHeight = (metrics.height * scale).toInt().coerceAtLeast(1)
        return widget.anchor.calculateY(screen[1], scaledHeight, widget.y)
    }

    private fun nanoMinReferenceWidth(renderer: Renderer2D, widget: HudWidget): Float {
        return when (widget) {
            is KuudraNotificationsWidget -> formattedWidth(renderer, widget.nanoMinReferenceText)
            else -> 0f
        }
    }

    private fun formattedWidth(renderer: Renderer2D, raw: String): Float {
        var bold = false
        var width = 0f
        val segment = StringBuilder()

        fun flush() {
            if (segment.isEmpty()) return
            width += renderer.textWidth(segment.toString(), FONT_SIZE, if (bold) fontBold() else fontRegular())
            segment.clear()
        }

        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '§' && i + 1 < raw.length) {
                flush()
                when (raw[i + 1].lowercaseChar()) {
                    'l' -> bold = true
                    'r' -> bold = false
                }
                i += 2
                continue
            }
            if (isHeart(c)) {
                flush()
                width += HEART_ADVANCE
                i++
                continue
            }
            segment.append(c)
            i++
        }
        flush()
        return width
    }

    private fun isInside(px: Float, py: Float, x: Float, y: Float, width: Float, height: Float): Boolean =
        px >= x && px <= x + width && py >= y && py <= y + height

    private fun editorOverlayAlpha(base: Float): Float {
        val opacity = IqNanoGlobalConfigScreen.getSharedOverlayOpacity().toFloat().coerceIn(0.25f, 1f)
        return (base * opacity).coerceIn(0.04f, 1f)
    }

    private fun isHeart(c: Char): Boolean = c == '\u2764' || c == '\u2665'

    private fun fontRegular(): String = IqNanoGlobalConfigScreen.fontBold()

    private fun fontBold(): String = IqNanoGlobalConfigScreen.fontBold()

    private fun TextColor.toNanoColor(): Color = when (this) {
        TextColor.BLACK -> Color.fromHex("#000000")
        TextColor.DARK_BLUE -> Color.fromHex("#0000AA")
        TextColor.DARK_GREEN -> Color.fromHex("#00AA00")
        TextColor.DARK_AQUA -> Color.fromHex("#00AAAA")
        TextColor.DARK_RED -> Color.fromHex("#AA0000")
        TextColor.DARK_PURPLE -> Color.fromHex("#AA00AA")
        TextColor.GOLD -> Color.fromHex("#FFAA00")
        TextColor.GRAY -> Color.fromHex("#AAAAAA")
        TextColor.DARK_GRAY -> Color.fromHex("#555555")
        TextColor.BLUE -> Color.fromHex("#5555FF")
        TextColor.GREEN -> Color.fromHex("#55FF55")
        TextColor.AQUA -> Color.fromHex("#55FFFF")
        TextColor.RED -> Color.fromHex("#FF5555")
        TextColor.LIGHT_PURPLE -> Color.fromHex("#FF55FF")
        TextColor.YELLOW -> Color.fromHex("#FFFF55")
        TextColor.WHITE -> Color.fromHex("#FFFFFF")
    }

    private val minecraftColors = mapOf(
        '0' to Color.fromHex("#000000"),
        '1' to Color.fromHex("#0000AA"),
        '2' to Color.fromHex("#00AA00"),
        '3' to Color.fromHex("#00AAAA"),
        '4' to Color.fromHex("#AA0000"),
        '5' to Color.fromHex("#AA00AA"),
        '6' to Color.fromHex("#FFAA00"),
        '7' to Color.fromHex("#AAAAAA"),
        '8' to Color.fromHex("#555555"),
        '9' to Color.fromHex("#5555FF"),
        'a' to Color.fromHex("#55FF55"),
        'b' to Color.fromHex("#55FFFF"),
        'c' to Color.fromHex("#FF5555"),
        'd' to Color.fromHex("#FF55FF"),
        'e' to Color.fromHex("#FFFF55"),
        'f' to Color.fromHex("#FFFFFF"),
    )
}
