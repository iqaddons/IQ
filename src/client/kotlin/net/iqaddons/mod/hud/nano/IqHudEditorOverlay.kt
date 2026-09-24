package net.iqaddons.mod.hud.nano

import net.iqaddons.mod.nanovg.rendering.backend.Paint
import net.iqaddons.mod.nanovg.rendering.backend.TextAlign
import net.iqaddons.mod.nanovg.rendering.renderer.Renderer2D
import net.iqaddons.mod.nanovg.util.CornerRadius
import net.iqaddons.mod.nanovg.util.Point
import net.iqaddons.mod.nanovg.util.Rect
import net.iqaddons.mod.screen.nano.IqNanoGlobalConfigScreen
import net.iqaddons.mod.hud.HudEditScreen.AlignmentGuide

object IqHudEditorOverlay {
    private data class HelpLine(val label: String, val value: String)

    @JvmStatic
    fun draw(
        renderer: Renderer2D,
        width: Int,
        height: Int,
        hasParent: Boolean,
        snapToGrid: Boolean,
        showGrid: Boolean,
        showGuides: Boolean,
        alignmentHelper: Boolean,
        mouseX: Float,
        mouseY: Float,
    ) {
        if (showGrid) drawGrid(renderer, width, height)
        if (showGuides) drawGuides(renderer, width, height)
        drawHudStyleButton(renderer, width, height, mouseX, mouseY)
        drawHelp(renderer, height, hasParent)
        drawStatus(renderer, width, height, snapToGrid, showGrid, showGuides, alignmentHelper)
    }

    @JvmStatic
    fun drawAlignmentGuides(renderer: Renderer2D, guides: List<AlignmentGuide>, width: Int, height: Int) {
        if (guides.isEmpty()) return

        val paint = Paint.solid(IqNanoGlobalConfigScreen.themeAccentHot().withAlpha(overlayAlpha(0.54f)))

        guides.forEach { guide ->
            if (guide.vertical()) {
                val x = guide.position()
                renderer.backend.line(Point(x, 0f), Point(x, height.toFloat()), paint, 0.7f)
            } else {
                val y = guide.position()
                renderer.backend.line(Point(0f, y), Point(width.toFloat(), y), paint, 0.7f)
            }
        }
    }

    private fun drawGrid(renderer: Renderer2D, width: Int, height: Int) {
        val paint = Paint.solid(IqNanoGlobalConfigScreen.themeText().withAlpha(0.085f))
        val majorPaint = Paint.solid(IqNanoGlobalConfigScreen.themeAccent().withAlpha(0.105f))
        val spacing = 20f
        val majorEvery = 4

        var index = 0
        var x = 0f
        while (x <= width) {
            renderer.backend.line(Point(x, 0f), Point(x, height.toFloat()), if (index % majorEvery == 0) majorPaint else paint, 0.55f)
            x += spacing
            index++
        }

        index = 0
        var y = 0f
        while (y <= height) {
            renderer.backend.line(Point(0f, y), Point(width.toFloat(), y), if (index % majorEvery == 0) majorPaint else paint, 0.55f)
            y += spacing
            index++
        }
    }

    private fun drawGuides(renderer: Renderer2D, width: Int, height: Int) {
        val cx = width / 2f
        val cy = height / 2f
        val guide = Paint.solid(IqNanoGlobalConfigScreen.themeText().withAlpha(0.18f))
        val center = Paint.solid(IqNanoGlobalConfigScreen.themeText().withAlpha(0.30f))
        val gap = 10f

        renderer.backend.line(Point(cx, 0f), Point(cx, cy - gap), guide, 0.65f)
        renderer.backend.line(Point(cx, cy + gap), Point(cx, height.toFloat()), guide, 0.65f)
        renderer.backend.line(Point(0f, cy), Point(cx - gap, cy), guide, 0.65f)
        renderer.backend.line(Point(cx + gap, cy), Point(width.toFloat(), cy), guide, 0.65f)
        renderer.backend.strokeCircle(Point(cx, cy), 4.2f, guide, 1.0f)
        renderer.backend.strokeCircle(Point(cx, cy), 2.8f, center, 0.75f)
        renderer.backend.fillCircle(Point(cx, cy), 1.05f, center)
    }

    private fun drawHelp(renderer: Renderer2D, height: Int, hasParent: Boolean) {
        val helpLines = arrayOf(
            HelpLine("Mouse", "Drag to move widget"),
            HelpLine("Scale", "Scroll / + / -"),
            HelpLine("Position", "Arrow keys (Shift x5)"),
            HelpLine("Toggles", "[G] Grid | [S] Snap | [Y] Guides | [A] Align"),
            HelpLine("Actions", "[C] Center | [R] Reset | [Ctrl+R] Defaults"),
            HelpLine("Layout", "[T] Top-left all"),
            HelpLine("Exit", if (hasParent) "[ESC] Save and back" else "[ESC] Save and close"),
        )
        val accent = IqNanoGlobalConfigScreen.themeAccent()
        val accentHot = IqNanoGlobalConfigScreen.themeAccentHot()
        val text = IqNanoGlobalConfigScreen.themeText()
        val muted = IqNanoGlobalConfigScreen.themeMuted()
        val x = 12f
        val lineHeight = 10.8f
        val labelX = 10f
        val valueX = 57f
        val rightPadding = 10f
        val maxValueWidth = helpLines.maxOf { renderer.textWidth(it.value, 8.2f, IqNanoGlobalConfigScreen.fontSemiBold()) }
        val panelW = valueX + maxValueWidth + rightPadding
        val panelH = 14f + helpLines.size * lineHeight
        val y = height - panelH - 12f
        val panel = Rect(x, y, panelW, panelH)
        renderer.fillRect(panel, IqNanoGlobalConfigScreen.themeModalSurface().withAlpha(overlayAlpha(0.72f)), CornerRadius.uniform(4.5f))
        renderer.strokeRect(panel, accent.withAlpha(overlayAlpha(0.82f)), 1.1f, CornerRadius.uniform(5.5f))
        var textY = y + 8f
        helpLines.forEach {
            renderer.text(it.label, x + labelX, textY, 8.2f, accentHot, IqNanoGlobalConfigScreen.fontBold())
            renderer.text(it.value, x + valueX, textY, 8.2f, if (it.label == "Exit") text.withAlpha(0.86f) else muted, IqNanoGlobalConfigScreen.fontSemiBold())
            textY += lineHeight
        }
    }

    private fun drawHudStyleButton(renderer: Renderer2D, width: Int, height: Int, mouseX: Float, mouseY: Float) {
        val panel = hudStyleButtonRect(width, height)
        val hovered = panel.contains(mouseX, mouseY)
        val pulse = if (hovered) {
            ((kotlin.math.sin((System.currentTimeMillis() % 10_000L) / 120.0) + 1.0) * 0.5).toFloat()
        } else {
            0f
        }
        val animatedPanel = if (hovered) panel.inset(-1f) else panel
        val accent = IqNanoGlobalConfigScreen.themeAccentHot()
        val modern = IqNanoGlobalConfigScreen.isSharedModernHudStyle()
        val vanilla = "VANILLA"
        val separator = " / "
        val modernText = "MODERN"
        val titleSize = 8.2f
        val valueSize = 8.5f
        val titleFont = IqNanoGlobalConfigScreen.fontBold()
        val valueFont = IqNanoGlobalConfigScreen.fontBold()
        val totalValueWidth = renderer.textWidth(vanilla, valueSize, valueFont) +
            renderer.textWidth(separator, valueSize, titleFont) +
            renderer.textWidth(modernText, valueSize, valueFont)
        var valueX = animatedPanel.centerX - totalValueWidth / 2f
        val valueY = animatedPanel.y + 19.4f

        renderer.fillRect(
            animatedPanel,
            IqNanoGlobalConfigScreen.themeModalSurface().withAlpha(overlayAlpha(if (hovered) 0.76f else 0.54f)),
            CornerRadius.uniform(4f)
        )
        renderer.strokeRect(
            animatedPanel,
            IqNanoGlobalConfigScreen.themeAccent().withAlpha(overlayAlpha(if (hovered) 0.70f + pulse * 0.16f else 0.36f)),
            if (hovered) 1.1f else 0.8f,
            CornerRadius.uniform(4.5f)
        )
        if (hovered) {
            renderer.strokeRect(animatedPanel.inset(-2f), accent.withAlpha(overlayAlpha(0.12f + pulse * 0.08f)), 0.8f, CornerRadius.uniform(6f))
        }
        renderer.text("HUD STYLE", animatedPanel.centerX, animatedPanel.y + 6.2f, titleSize, accent, titleFont, TextAlign.CENTER)
        renderer.text(vanilla, valueX, valueY, valueSize, if (modern) IqNanoGlobalConfigScreen.themeMuted() else IqNanoGlobalConfigScreen.themeText(), valueFont)
        valueX += renderer.textWidth(vanilla, valueSize, valueFont)
        renderer.text(separator, valueX, valueY, valueSize, IqNanoGlobalConfigScreen.themeMuted().withAlpha(0.82f), titleFont)
        valueX += renderer.textWidth(separator, valueSize, titleFont)
        renderer.text(modernText, valueX, valueY, valueSize, if (modern) IqNanoGlobalConfigScreen.themeText() else IqNanoGlobalConfigScreen.themeMuted(), valueFont)
    }

    private fun hudStyleButtonRect(width: Int, height: Int): Rect {
        val panelW = 122f
        val panelH = 32f
        return Rect((width - panelW) / 2f, height - panelH - 24f, panelW, panelH)
    }

    private fun drawStatus(renderer: Renderer2D, width: Int, height: Int, snap: Boolean, grid: Boolean, guides: Boolean, align: Boolean) {
        val labels = arrayOf("Snap" to snap, "Grid" to grid, "Guides" to guides, "Align" to align)
        val accent = IqNanoGlobalConfigScreen.themeAccentHot()
        val muted = IqNanoGlobalConfigScreen.themeMuted()
        val disabled = IqNanoGlobalConfigScreen.themeDisabled()
        val panelW = 88f
        val panelH = 57f
        val x = width - panelW - 12f
        val y = height - panelH - 12f
        val panel = Rect(x, y, panelW, panelH)
        renderer.fillRect(panel, IqNanoGlobalConfigScreen.themeModalSurface().withAlpha(overlayAlpha(0.70f)), CornerRadius.uniform(4.5f))
        renderer.strokeRect(panel, IqNanoGlobalConfigScreen.themeAccent().withAlpha(overlayAlpha(0.74f)), 1.05f, CornerRadius.uniform(5f))
        var textY = y + 7f
        labels.forEach { (label, value) ->
            renderer.text(label, x + 11f, textY, 8.4f, muted, IqNanoGlobalConfigScreen.fontSemiBold())
            renderer.text(onOff(value), x + panelW - 11f, textY, 8.4f, if (value) accent else disabled, IqNanoGlobalConfigScreen.fontBold(), TextAlign.RIGHT)
            textY += 12.2f
        }
    }

    private fun onOff(value: Boolean): String = if (value) "ON" else "OFF"

    private fun overlayAlpha(base: Float): Float {
        val opacity = IqNanoGlobalConfigScreen.getSharedOverlayOpacity().toFloat().coerceIn(0.25f, 1f)
        return (base * opacity).coerceIn(0.04f, 1f)
    }
}
