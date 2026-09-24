package net.iqaddons.mod.screen.nano

import net.iqaddons.mod.IQModClient
import net.iqaddons.mod.config.Configuration
import net.iqaddons.mod.manager.IQPersistentDataStore
import net.iqaddons.mod.nanovg.IqNanoVg
import net.iqaddons.mod.nanovg.IqNanoVgRenderable
import net.iqaddons.mod.nanovg.rendering.backend.Color
import net.iqaddons.mod.nanovg.rendering.backend.Paint
import net.iqaddons.mod.nanovg.rendering.backend.TextAlign
import net.iqaddons.mod.nanovg.rendering.backend.VectorPath
import net.iqaddons.mod.nanovg.rendering.renderer.Renderer2D
import net.iqaddons.mod.nanovg.util.CornerRadius
import net.iqaddons.mod.nanovg.util.Point
import net.iqaddons.mod.nanovg.util.Rect
import net.iqaddons.mod.screen.model.ConfigEntryModel
import net.iqaddons.mod.screen.model.ConfigEntryModel.EntryType
import net.iqaddons.mod.utils.MessageUtil
import net.iqaddons.mod.utils.data.DataKey
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW
import java.lang.reflect.Field
import java.util.Locale
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

open class IqNanoGlobalConfigScreen(
    private val parent: Screen?,
    private vararg val configClasses: Class<*>,
) : Screen(Component.literal(configScreenTitle("Configuration Hub"))), IqNanoVgRenderable {

    private data class Hit(val rect: Rect, val action: () -> Unit)
    private data class SliderHit(val rect: Rect, val min: Double, val max: Double, val set: (Double) -> Unit)
    private data class ChoiceHit(val row: Row.Choice, val rect: Rect)
    private data class ChoiceOptionHit(val row: Row.Choice, val index: Int, val rect: Rect)
    private data class ScrollbarHit(val track: Rect, val thumb: Rect)
    private enum class Section(val title: String, val subtitle: String) {
        GUI("GUI & Visual", "Theme, colors and animation behavior."),
        HUD("HUD & Overlay", "HUD visibility and overlay options. (W.I.P)"),
        FEATURES("Manage Features", "Bulk actions and config import/export."),
        SYSTEM("System & Reload", "Reload and system-level actions."),
        DEBUG("Debug", "Logs and diagnostic debugging options. (W.I.P)"),
    }

    private val hits = mutableListOf<Hit>()
    private val sliderHits = mutableListOf<SliderHit>()
    private val choiceHits = mutableListOf<ChoiceHit>()
    private val choiceOptionHits = mutableListOf<ChoiceOptionHit>()
    private var section = Section.GUI
    private var scroll = 0f
    private var maxScroll = 0f
    private var draggingSlider: SliderHit? = null
    private var draggingScrollbar = false
    private var scrollbarDragOffset = 0f
    private var scrollbarHit: ScrollbarHit? = null
    private var openChoice: Row.Choice? = null
    private var choiceDropdownRect: Rect? = null
    private var mouseXf = 0f
    private var mouseYf = 0f
    private var panel = Rect.ZERO
    private var sectionContent = Rect.ZERO
    private var time = 0f
    private var openProgress = 0f
    private var closeProgress = 0f
    private var closing = false
    private var closeTarget: Screen? = null
    private var resetDefaultsConfirmUntil = 0f
    private var hoverTooltip: Pair<Rect, String>? = null
    private val choiceAnim = HashMap<String, Float>()
    private val choiceScroll = HashMap<String, Int>()

    override fun init() {
        IqNanoVg.prepare()
        load()
    }

    override fun extractRenderState(ctx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        mouseXf = mouseX.toFloat()
        mouseYf = mouseY.toFloat()
        super.extractRenderState(ctx, mouseX, mouseY, delta)
    }

    override fun renderNanoVg(renderer: Renderer2D, mouseX: Float, mouseY: Float) {
        mouseXf = mouseX
        mouseYf = mouseY
        time = (Util.getMillis() % 1_000_000L) / 1000f
        updateCloseAnimation()
        if (closing && closeProgress >= 0.96f) {
            val target = closeTarget
            closing = false
            closeTarget = null
            save()
            minecraft?.gui?.setScreen(target)
            return
        }
        hits.clear()
        sliderHits.clear()
        choiceHits.clear()
        choiceOptionHits.clear()
        scrollbarHit = null
        hoverTooltip = null
        val sw = width.toFloat()
        val sh = height.toFloat()
        val scale = uiScale.toFloat().coerceIn(0.75f, 1.35f)
        val panelMinW = min(430f, sw - 56f)
        val panelMinH = min(260f, sh - 46f)
        val panelW = min(max(max(575f, sw * 0.60f) * scale, panelMinW), sw - 56f)
        val panelH = min(max(max(320f, sh * 0.62f) * scale, panelMinH), sh - 46f)
        panel = Rect((sw - panelW) / 2f, (sh - panelH) / 2f, panelW, panelH)
        val sidebarW = 158f.coerceAtMost(panelW * 0.255f).coerceAtLeast(132f)
        val exit = closeProgress.coerceIn(0f, 1f)
        renderer.withOpacity(1f - exit * 0.35f) {
            renderer.fillRect(Rect(0f, 0f, sw, sh), Color.of(0f, 0f, 0f, 0.62f))
        }
        val scaleIn = 0.965f + 0.035f * openProgress
        val closeScale = 1f - 0.025f * exit
        renderer.withOpacity((openProgress * (1f - exit)).coerceIn(0f, 1f)) {
            renderer.withTransform(
                dx = panel.centerX * (1f - scaleIn * closeScale),
                dy = panel.centerY * (1f - scaleIn * closeScale) + (1f - openProgress) * 12f - exit * 5f,
                sx = scaleIn * closeScale,
                sy = scaleIn * closeScale,
            ) {
                renderer.backend.fillRect(
                    panel,
                    Paint.LinearGradient(
                        Point(panel.x, panel.y),
                        Point(panel.right, panel.bottom),
                        panelStart().withAlpha((guiOpacity * 0.92).toFloat().coerceIn(0.35f, 0.98f)),
                        panelEnd().withAlpha((guiOpacity * 0.98).toFloat().coerceIn(0.42f, 1.0f)),
                    ),
                    CornerRadius.uniform(10f),
                )
                drawPanelBackground(renderer, panel)
                renderer.fillRect(Rect(panel.x + sidebarW, panel.y + 1f, 1f, panel.height - 2f), Color.of(1f, 1f, 1f, 0.10f))
                renderer.fillRect(Rect(panel.x + sidebarW, panel.y + 45f, panel.width - sidebarW, 1f), accent().withAlpha(0.60f))
                renderer.fillRect(Rect(panel.x + 1f, panel.y + 45f, sidebarW - 1f, 1f), Color.of(1f, 1f, 1f, 0.10f))
                drawSidebar(renderer, Rect(panel.x, panel.y, sidebarW, panel.height))
                drawHeader(renderer, Rect(panel.x + sidebarW, panel.y, panel.width - sidebarW, 46f))
                sectionContent = Rect(panel.x + sidebarW + 14f, panel.y + 54f, panel.width - sidebarW - 22f, panel.height - 55f)
                drawSection(renderer, sectionContent)
                drawAnimatedBorder(renderer, panel)
                hoverTooltip?.let { drawTooltip(renderer, it.first, it.second) }
            }
        }
    }

    private fun drawSidebar(r: Renderer2D, rect: Rect) {
        val tex = IqNanoVg.textures.textureFor(brandIconPath())
        val headerCenterY = rect.y + 24f
        val logo = Rect(rect.x + 16f, headerCenterY - 13f, 26f, 26f)
        if (tex != null) r.backend.drawImage(tex, logo, null, CornerRadius.uniform(7f))
        val logoPulse = if (isSharedAnimationsEnabled()) pulse(0.10f, 1.0f) else 0f
        r.strokeRect(logo.inset(-2.0f), brandDetailAccent().withAlpha(0.22f + logoPulse), 1.45f, CornerRadius.uniform(8.7f))
        r.strokeRect(logo.inset(-0.8f), Color.fromHex("#FFFFFF").withAlpha(0.10f), 0.75f, CornerRadius.uniform(7.6f))
        if (logo.inset(-2f).contains(mouseXf, mouseYf)) hoverTooltip = logo to brandTooltip()
        val brandX = logo.right + 8f
        drawBrandTitle(r, brandName(), brandX, headerCenterY - 12.8f, 15.6f)
        r.text("CONFIG HUB", brandX + 0.4f, headerCenterY + 3.7f, 8.4f, brandSubtitleColor(), semi())
        var y = rect.y + 58f
        Section.entries.forEach {
            val row = Rect(rect.x + 11f, y, rect.width - 22f, 19f)
            val active = it == section
            if (active || row.contains(mouseXf, mouseYf)) {
                r.fillRect(row, accent().withAlpha(if (active) 0.36f else 0.14f), CornerRadius.uniform(4f))
                r.fillRect(Rect(row.x, row.y, 4f, row.height), accent(), CornerRadius.uniform(2f))
                r.strokeRect(row, accent().withAlpha(if (active) 0.30f else 0.12f), 1f, CornerRadius.uniform(4f))
            }
            r.text(it.title, row.x + 10f, row.y + 6.0f, 9.2f, if (active) text() else muted(), semi())
            hits += Hit(row) { section = it; scroll = 0f }
            y += 25f
        }

        val reset = Rect(rect.x + 18f, rect.bottom - 48f, rect.width - 36f, 22f)
        resetDefaultsButton(r, reset) { resetHubConfigDefaults() }
    }

    private fun drawPanelBackground(r: Renderer2D, rect: Rect) {
        val path = brandBackgroundPath() ?: return
        val tex = IqNanoVg.textures.textureFor(path) ?: return
        val radius = CornerRadius.uniform(10f)
        r.withClip(rect) {
            r.withOpacity(0.18f) {
                r.backend.drawImage(tex, rect, null, radius)
            }
            r.fillRect(rect, panelEnd().withAlpha(0.22f), radius)
        }
    }

    private fun drawHeader(r: Renderer2D, rect: Rect) {
        r.text(section.title, rect.x + 22f, rect.y + 9f, 13.5f, text(), bold())
        r.text(section.subtitle, rect.x + 22f, rect.y + 26f, 9.5f, muted(), regular())
        iconButton(r, Rect(rect.right - 52f, rect.y + 14f, 18f, 18f), "chevron-left", "Back to ${brandName()} Config") {
            resetDefaultsConfirmUntil = 0f
            (parent as? IqNanoConfigScreen)?.animateReturnFromHub()
            minecraft?.gui?.setScreen(parent)
        }
        iconButton(r, Rect(rect.right - 29f, rect.y + 14f, 18f, 18f), "close", "Close Config Hub") { onClose() }
        r.fillRect(Rect(rect.x, rect.bottom - 1f, rect.width, 1f), accent().withAlpha(0.60f))
    }

    private fun drawSection(r: Renderer2D, rect: Rect) {
        var y = rect.y - scroll
        val rows = when (section) {
            Section.GUI -> listOf(
                Row.Choice("GUI Theme", "Choose the IQ config color palette", THEMES, themeIndex) { themeIndex = it },
                Row.Slider("GUI Opacity", "Controls config panel transparency", guiOpacity, 0.35, 1.0) { guiOpacity = it },
                Row.Slider("UI Scale", "Scales both IQ config screens", uiScale, 0.75, 1.35) { uiScale = it },
                Row.Choice("GUI Animations", "Motion level\nAdvanced is vivid but may lower FPS while the GUI is open", ANIMATION_MODES, animationModeIndex) {
                    animationModeIndex = it
                    animationsEnabled = it != ANIM_OFF
                },
                Row.Slider("Animation Speed", "Controls transition responsiveness", animationSpeed, 0.1, 1.0) { animationSpeed = it },
                Row.Toggle("Outline / Shadow", "Adds IQ glow around panels", outlineShadow) { outlineShadow = it },
                Row.Toggle("Persist UI Session", "Remember search, selected category and scroll briefly", uiStatePersistenceEnabled) { uiStatePersistenceEnabled = it },
            )
            Section.FEATURES -> listOf(
                Row.Button("Disable All Features", "Turns off all boolean feature toggles", "Disable") { disableAllFeatures() },
                Row.Button("Reset Features to Default", "Restores IQ main config defaults", "Reset") { resetAllFeaturesDefault() },
                Row.Button("Export Config", "Copies IQ config values to clipboard", "Export") { exportConfigToClipboard() },
                Row.Button("Import Config", "Reads IQ config values from clipboard", "Import") { importConfigFromClipboard() },
            )
            Section.SYSTEM -> listOf(
                Row.Button("Update Config Files", "Refreshes generated IQ JSON files", "Update") { updateConfigFiles() },
                Row.Button("Reload IQ", "Runs /iq reload", "Reload") { runIqReload() },
                Row.Button("Soft Restart Features", "Refreshes feature runtime state", "Restart") { softRestartFeatures() },
                Row.Toggle("Auto Reload Config", "Automatically refresh when config changes are detected", autoReloadConfig) { autoReloadConfig = it },
            )
            Section.HUD -> listOf(
                Row.Button("Open HUD", "Open the HUD editor", "Open") {
                    net.iqaddons.mod.hud.HudManager.get().openEditor(this)
                },
                Row.Choice(
                    "Hud Font Style",
                    "Choose the HUD text renderer style\nVanilla and Custom Fonts use separate configs\nEach style can be edited independently",
                    HUD_STYLES,
                    hudStyleIndex
                ) {
                    net.iqaddons.mod.hud.HudManager.getIfInitialized()?.saveConfig()
                    hudStyleIndex = it
                    net.iqaddons.mod.hud.HudManager.getIfInitialized()?.reloadCurrentStyleConfigs()
                },
                Row.Slider("Overlay Opacity", "Controls editor overlay strength", overlayOpacity, 0.25, 1.0) { overlayOpacity = it },
                Row.Button("Reset HUD Positions", "Restore all HUD widgets to their default positions", "Reset") { resetHudPositions() },
            )
            Section.DEBUG -> listOf(
                Row.Toggle("Feature Logs", "Log feature lifecycle events", showFeatureLogs) { showFeatureLogs = it },
                Row.Toggle("Debug Overlay", "Render extra overlay diagnostics", debugOverlay) { debugOverlay = it },
                Row.Toggle("Debug Triggers", "Log internal event dispatch", debugEvents) { debugEvents = it },
                Row.Button("Copy Debug Info", "Copies a concise debug report", "Copy") { copyDebugInfo() },
            )
        }
        r.withClip(rect) {
            rows.forEach { row ->
                if (row is Row.Choice) {
                    choiceAnim[row.title] = approach(choiceAnim[row.title] ?: if (openChoice?.title == row.title) 1f else 0f, if (openChoice?.title == row.title) 1f else 0f, animationAmount())
                }
                val rowH = rowHeight(row)
                val rr = Rect(rect.x, y, rect.width - 18f, rowH)
                if (rr.bottom >= rect.y && rr.y <= rect.bottom) drawRow(r, rr, row)
                y += rowH + 7f
            }
        }
        drawOpenChoice(r, rect)
        maxScroll = (y + scroll + 4f - rect.y - rect.height).coerceAtLeast(0f)
        scroll = scroll.coerceIn(0f, maxScroll)
        if (maxScroll > 0f) {
            val track = Rect(rect.right - 5f, rect.y, 3f, rect.height)
            val thumbH = max(34f, rect.height * (rect.height / (rect.height + maxScroll)))
            val thumbY = rect.y + (rect.height - thumbH) * (scroll / maxScroll)
            r.fillRect(track, Color.of(1f, 1f, 1f, 0.06f), CornerRadius.uniform(2f))
            val thumb = Rect(track.x, thumbY, track.width, thumbH)
            r.fillRect(thumb, accent(), CornerRadius.uniform(2f))
            scrollbarHit = ScrollbarHit(track.inset(-5f), thumb.inset(-5f))
        }
    }

    private fun drawRow(r: Renderer2D, rect: Rect, row: Row) {
        val hover = isMouseInSection() && rect.contains(mouseXf, mouseYf) && !isMouseOverChoiceDropdown()
        r.fillRect(rect, surface().withAlpha(if (hover) 0.78f else 0.62f), CornerRadius.uniform(4f))
        r.strokeRect(rect, border().withAlpha(if (hover) 0.50f else 0.18f), 1f, CornerRadius.uniform(4f))
        r.text(row.title, rect.x + 18f, rect.y + 7f, 10.5f, text(), semi())
        val textX = rect.x + 18f
        val descriptionMaxW = (controlLeft(r, rect, row) - textX - 14f).coerceAtLeast(80f)
        rowDescriptionLines(row).forEachIndexed { index, line ->
            r.text(fitText(r, line, descriptionMaxW, 8.4f, regular()), textX, rect.y + 22f + index * 9.7f, 8.4f, muted(), regular())
        }
        when (row) {
            is Row.Toggle -> {
                val tr = Rect(rect.right - 54f, rect.centerY - 7.5f, 31f, 15f)
                drawToggle(r, tr, row.value)
                hits += Hit(tr) { row.set(!row.value); save() }
            }
            is Row.Slider -> {
                val bar = Rect(rect.right - 160f, rect.centerY - 5.5f, 106f, 11f)
                val t = ((row.value - row.min) / (row.max - row.min)).toFloat().coerceIn(0f, 1f)
                r.fillRect(bar, sliderTrack(), CornerRadius.uniform(5f))
                r.fillRect(Rect(bar.x, bar.y, bar.width * t, bar.height), accent(), CornerRadius.uniform(5f))
                r.fillRect(Rect(bar.x + bar.width * t - 3f, bar.y - 2f, 6f, bar.height + 4f), knob(), CornerRadius.uniform(3f))
                r.text("%.2f".format(row.value), rect.right - 28f, rect.centerY - 4.5f, 9f, text(), semi(), TextAlign.CENTER)
                val hit = SliderHit(bar.inset(-5f), row.min, row.max, row.set)
                sliderHits += hit
                hits += Hit(hit.rect) { updateSlider(hit, mouseXf) }
            }
            is Row.Button -> {
                val btn = Rect(rect.right - 78f, rect.centerY - 8f, 50f, 16f)
                smallButton(r, btn, row.buttonLabel)
                hits += Hit(btn) { row.action(); save() }
            }
            is Row.Choice -> {
                val value = row.options.getOrNull(row.index)?.orEmpty() ?: ""
                val btnW = (r.textWidth(value.uppercase(), 7.6f, semi()) + 30f).coerceIn(76f, 156f)
                val btn = Rect(rect.right - btnW - 28f, rect.centerY - 9f, btnW, 18f)
                choiceButton(r, btn, row, value)
                choiceHits += ChoiceHit(row, btn)
            }
        }
    }

    private fun controlLeft(r: Renderer2D, rect: Rect, row: Row): Float =
        when (row) {
            is Row.Toggle -> rect.right - 54f
            is Row.Slider -> rect.right - 160f
            is Row.Button -> rect.right - 78f
            is Row.Choice -> {
                val value = row.options.getOrNull(row.index)?.orEmpty() ?: ""
                val btnW = (r.textWidth(value.uppercase(), 7.6f, semi()) + 30f).coerceIn(76f, 156f)
                rect.right - btnW - 28f
            }
        }

    private fun fitText(r: Renderer2D, text: String, maxWidth: Float, size: Float, font: String): String {
        if (r.textWidth(text, size, font) <= maxWidth) return text
        val ellipsis = "..."
        var lo = 0
        var hi = text.length
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            val candidate = text.take(mid).trimEnd() + ellipsis
            if (r.textWidth(candidate, size, font) <= maxWidth) lo = mid else hi = mid - 1
        }
        return text.take(lo).trimEnd() + ellipsis
    }

    private fun rowDescriptionLines(row: Row): List<String> =
        row.description.split('\n').map { it.trim() }.filter { it.isNotBlank() }

    private fun rowHeight(row: Row): Float =
        39f + (rowDescriptionLines(row).size - 1).coerceAtLeast(0) * 10f

    private fun iconButton(r: Renderer2D, rect: Rect, icon: String, tooltip: String? = null, action: () -> Unit) {
        val hover = rect.contains(mouseXf, mouseYf)
        r.fillRect(rect, buttonSurface().withAlpha(if (hover) 0.82f else 0.46f), CornerRadius.uniform(4f))
        r.strokeRect(rect, accent().withAlpha(if (hover) 0.78f else 0.42f), 1f, CornerRadius.uniform(4f))
        r.icon(icon, rect.inset(rect.width * 0.27f), text())
        if (hover && !tooltip.isNullOrBlank()) hoverTooltip = rect to tooltip
        hits += Hit(rect, action)
    }

    private fun drawTooltip(r: Renderer2D, source: Rect, text: String) {
        val flag = text == BRAZIL_TOOLTIP_TEXT
        val lines = text.lines().filter { it.isNotBlank() }.take(5)
        val textW = lines.maxOfOrNull { r.textWidth(it, 7.8f, regular()) } ?: 0f
        val w = (textW + 18f + if (flag) 19f else 0f).coerceIn(80f, 230f)
        val h = (12f + lines.size * 10f).coerceAtLeast(22f)
        val x = (mouseXf + 12f).coerceAtMost(width.toFloat() - w - 8f)
        val y = if (mouseYf + h + 12f < height) mouseYf + 10f else mouseYf - h - 10f
        val rect = Rect(x, y.coerceAtLeast(8f), w, h)
        r.backend.boxShadow(rect, CornerRadius.uniform(5f), 14f, -3f, Color.of(0f, 0f, 0f, 0.72f))
        r.fillRect(rect, modalSurface().withAlpha(0.96f), CornerRadius.uniform(5f))
        r.strokeRect(rect, accent().withAlpha(0.42f), 1f, CornerRadius.uniform(5f))
        lines.forEachIndexed { index, line ->
            r.text(line, rect.x + 9f, rect.y + 7f + index * 10f, 7.8f, muted(), regular())
        }
        if (flag) drawBrazilFlag(r, Rect(rect.x + 9f + textW + 6f, rect.y + 6.5f, 13f, 9f))
    }

    private fun drawBrazilFlag(r: Renderer2D, rect: Rect) {
        r.fillRect(rect, Color.fromHex("#009B3A"), CornerRadius.uniform(1.2f))
        r.backend.fillPath(
            VectorPath()
                .moveTo(rect.centerX, rect.y + 1.1f)
                .lineTo(rect.right - 1.4f, rect.centerY)
                .lineTo(rect.centerX, rect.bottom - 1.1f)
                .lineTo(rect.x + 1.4f, rect.centerY)
                .close(),
            Paint.solid(Color.fromHex("#FFDF00")),
        )
        r.backend.fillCircle(Point(rect.centerX, rect.centerY), 2.05f, Paint.solid(Color.fromHex("#002776")))
    }

    private fun smallButton(r: Renderer2D, rect: Rect, label: String) {
        val hover = rect.contains(mouseXf, mouseYf)
        r.fillRect(rect, buttonSurface().withAlpha(if (hover) 0.78f else 0.58f), CornerRadius.uniform(4f))
        r.strokeRect(rect, accent().withAlpha(if (hover) 0.78f else 0.56f), 1f, CornerRadius.uniform(4f))
        r.text(label.uppercase(), rect.centerX, rect.y + 5.2f, 7.8f, text(), bold(), TextAlign.CENTER)
    }

    private fun resetDefaultsButton(r: Renderer2D, rect: Rect, action: () -> Unit) {
        val hover = rect.contains(mouseXf, mouseYf)
        val armed = resetDefaultsConfirmUntil > time
        val remaining = (resetDefaultsConfirmUntil - time).coerceAtLeast(0f)
        val pulse = ((sin(time * 7.5f) + 1f) * 0.5f)
        val label = if (armed) "CONFIRM RESET ${ceil(remaining).toInt().coerceIn(1, 3)}" else "RESET DEFAULTS"
        val fillAlpha = when {
            armed -> 0.42f + pulse * 0.18f
            hover -> 0.76f
            else -> 0.50f
        }
        val strokeAlpha = when {
            armed -> 0.74f + pulse * 0.18f
            hover -> 0.78f
            else -> 0.58f
        }
        r.fillRect(rect, (if (armed) accent() else buttonSurface()).withAlpha(fillAlpha), CornerRadius.uniform(7f))
        r.strokeRect(rect, accent().withAlpha(strokeAlpha), if (armed) 1.15f else 1f, CornerRadius.uniform(7f))
        if (armed) r.strokeRect(rect.inset(-2f), accent().withAlpha(0.10f + pulse * 0.12f), 1f, CornerRadius.uniform(9f))
        r.text(fitText(r, label, rect.width - 12f, 8.2f, bold()), rect.centerX, rect.y + 7.4f, 8.2f, text(), bold(), TextAlign.CENTER)
        if (hover) {
            hoverTooltip = rect to if (armed) {
                "Click again within 3 seconds to restore all Config Hub settings to defaults"
            } else {
                "Restore all Config Hub settings to defaults\nRequires a second click to confirm"
            }
        }
        hits += Hit(rect) {
            if (resetDefaultsConfirmUntil > time) {
                resetDefaultsConfirmUntil = 0f
                action()
            } else {
                resetDefaultsConfirmUntil = time + 3f
            }
        }
    }

    private fun choiceButton(r: Renderer2D, rect: Rect, row: Row.Choice, value: String) {
        val hover = rect.contains(mouseXf, mouseYf)
        val active = openChoice?.title == row.title
        r.fillRect(rect, buttonSurface().withAlpha(if (hover || active) 0.94f else 0.78f), CornerRadius.uniform(4f))
        r.strokeRect(rect, accent().withAlpha(if (active) 0.58f else if (hover) 0.42f else 0.18f), 1f, CornerRadius.uniform(4f))
        r.text(value.uppercase(), rect.centerX - 4f, rect.y + 4.8f, 7.6f, text(), semi(), TextAlign.CENTER)
        val progress = choiceAnim[row.title] ?: if (active) 1f else 0f
        r.withTransform(rect.right - 9.5f, rect.centerY, 1f, 1f) {
            r.backend.rotate(progress * (PI.toFloat() / 2f))
            r.icon("chevron-right", Rect(-4.5f, -4.5f, 9f, 9f), muted())
        }
    }

    private fun drawOpenChoice(r: Renderer2D, content: Rect) {
        val row = openChoice ?: run {
            choiceDropdownRect = null
            return
        }
        val anchor = choiceHits.lastOrNull { it.row.title == row.title }?.rect ?: return
        val values = row.options.toList()
        if (values.isEmpty()) return
        val progress = (choiceAnim[row.title] ?: 0f).coerceIn(0f, 1f)
        if (progress <= 0.02f) return

        val optionH = 18f
        val visibleCount = values.size.coerceAtMost(6)
        val maxStart = (values.size - visibleCount).coerceAtLeast(0)
        val startIndex = (choiceScroll[row.title] ?: 0).coerceIn(0, maxStart).also { choiceScroll[row.title] = it }
        val visibleValues = values.drop(startIndex).take(visibleCount)
        val scrollable = maxStart > 0
        val indicatorH = if (scrollable) 10f else 0f
        val dropdownH = 8f + indicatorH * 2f + optionH * visibleCount
        val width = values.maxOf { r.textWidth(it.uppercase(), 7.4f, semi()) + 28f }.coerceAtLeast(anchor.width).coerceAtMost(180f)
        val x = (anchor.right - width).coerceIn(content.x, content.right - width - 12f)
        val openDown = anchor.bottom + dropdownH + 8f <= content.bottom
        val fullY = if (openDown) anchor.bottom + 4f else anchor.y - dropdownH - 4f
        val animatedY = fullY + if (openDown) (1f - progress) * -6f else (1f - progress) * 6f
        val rect = Rect(x, animatedY, width, dropdownH * progress)
        choiceDropdownRect = Rect(x, fullY, width, dropdownH)

        r.withOpacity(progress) {
            r.backend.boxShadow(Rect(x, fullY, width, dropdownH), CornerRadius.uniform(5f), 14f, -3f, Color.of(0f, 0f, 0f, 0.70f))
            r.fillRect(rect, modalSurface().withAlpha(0.98f), CornerRadius.uniform(5f))
            r.strokeRect(rect, accent().withAlpha(0.46f), 1f, CornerRadius.uniform(5f))
            r.withClip(rect.inset(1f)) {
                if (startIndex > 0) r.icon("chevron-up", Rect(rect.centerX - 4f, fullY + 3f, 8f, 8f), muted())
                visibleValues.forEachIndexed { index, value ->
                    val optionIndex = startIndex + index
                    val option = Rect(x + 4f, fullY + 4f + indicatorH + index * optionH, width - 8f, optionH - 2f)
                    val selected = optionIndex == row.index
                    val hover = option.contains(mouseXf, mouseYf)
                    if (selected || hover) {
                        r.fillRect(option, accent().withAlpha(if (selected) 0.26f else 0.14f), CornerRadius.uniform(4f))
                    }
                    r.text(value.uppercase(), option.x + 7f, option.y + 4.2f, 7.4f, if (selected) text() else muted(), semi())
                    if (selected) r.icon("check", Rect(option.right - 14f, option.centerY - 4.5f, 9f, 9f), text())
                    choiceOptionHits += ChoiceOptionHit(row, optionIndex, option)
                }
                if (startIndex < maxStart) r.icon("chevron-down", Rect(rect.centerX - 4f, fullY + dropdownH - 11f, 8f, 8f), muted())
            }
        }
    }

    private fun isMouseOverChoiceDropdown(): Boolean =
        choiceDropdownRect?.contains(mouseXf, mouseYf) == true

    private fun isMouseInSection(): Boolean =
        sectionContent.contains(mouseXf, mouseYf)

    private fun isSectionHit(rect: Rect): Boolean =
        rect.intersects(sectionContent)

    private fun drawToggle(r: Renderer2D, rect: Rect, on: Boolean) {
        r.fillRect(rect, if (on) accent() else toggleOff(), CornerRadius.uniform(rect.height / 2f))
        val knobSize = rect.height - 4f
        r.fillRect(Rect(if (on) rect.right - knobSize - 2f else rect.x + 2f, rect.y + 2f, knobSize, knobSize), knob(), CornerRadius.uniform(knobSize / 2f))
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        if (closing) return true
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(click, doubled)
        mouseXf = click.x().toFloat()
        mouseYf = click.y().toFloat()
        choiceOptionHits.lastOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            if (!isMouseInSection() && !isMouseOverChoiceDropdown()) return@let
            it.row.set(it.index)
            openChoice = null
            save()
            return true
        }
        if (isMouseOverChoiceDropdown()) return true
        choiceHits.lastOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            if (!isMouseInSection()) return@let
            openChoice = if (openChoice?.title == it.row.title) null else it.row
            openChoice?.let { row -> choiceScroll[row.title] = 0 }
            return true
        }
        if (isMouseInSection()) scrollbarHit?.let { bar ->
            if (bar.thumb.contains(mouseXf, mouseYf)) {
                draggingScrollbar = true
                scrollbarDragOffset = mouseYf - (bar.thumb.y + 5f)
                return true
            }
            if (bar.track.contains(mouseXf, mouseYf)) {
                val thumbH = (bar.thumb.height - 10f).coerceAtLeast(1f)
                val trackH = bar.track.height
                val thumbY = (mouseYf - bar.track.y - thumbH / 2f).coerceIn(0f, trackH - thumbH)
                scroll = if (trackH <= thumbH) 0f else (thumbY / (trackH - thumbH) * maxScroll).coerceIn(0f, maxScroll)
                draggingScrollbar = true
                scrollbarDragOffset = thumbH / 2f
                return true
            }
        }
        sliderHits.firstOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            if (!isMouseInSection()) return@let
            draggingSlider = it
            updateSlider(it, mouseXf)
            return true
        }
        hits.lastOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            if (!isMouseInSection() && isSectionHit(it.rect)) return@let
            openChoice = null
            it.action()
            return true
        }
        openChoice = null
        return true
    }

    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        if (closing) return true
        if (draggingScrollbar) {
            scrollbarHit?.let { bar ->
                val thumbH = (bar.thumb.height - 10f).coerceAtLeast(1f)
                val trackH = bar.track.height
                val thumbY = (click.y().toFloat() - bar.track.y - scrollbarDragOffset).coerceIn(0f, trackH - thumbH)
                scroll = if (trackH <= thumbH) 0f else (thumbY / (trackH - thumbH) * maxScroll).coerceIn(0f, maxScroll)
                return true
            }
        }
        draggingSlider?.let {
            updateSlider(it, click.x().toFloat())
            return true
        }
        return super.mouseDragged(click, offsetX, offsetY)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        draggingSlider = null
        draggingScrollbar = false
        return super.mouseReleased(click)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (closing) return true
        openChoice?.let { row ->
            if (isMouseOverChoiceDropdown()) {
                val maxStart = (row.options.size - 6).coerceAtLeast(0)
                val step = if (verticalAmount > 0.0) -1 else if (verticalAmount < 0.0) 1 else 0
                choiceScroll[row.title] = ((choiceScroll[row.title] ?: 0) + step).coerceIn(0, maxStart)
                return true
            }
            openChoice = null
        }
        scroll = (scroll - verticalAmount.toFloat() * 34f).coerceIn(0f, maxScroll)
        return true
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        if (closing) return true
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (openChoice != null) {
                openChoice = null
                return true
            }
            onClose()
            return true
        }
        return super.keyPressed(input)
    }

    override fun removed() {
        save()
        super.removed()
    }

    override fun onClose() {
        beginClose(parent)
    }

    override fun isPauseScreen(): Boolean = false

    private fun updateSlider(hit: SliderHit, x: Float) {
        val nt = ((x - hit.rect.x) / hit.rect.width).coerceIn(0f, 1f)
        hit.set(hit.min + (hit.max - hit.min) * nt)
        save()
    }

    private fun updateCloseAnimation() {
        val amount = animationAmount()
        openProgress = approach(openProgress, if (closing) 0.82f else 1f, amount)
        closeProgress = approach(closeProgress, if (closing) 1f else 0f, if (isSharedAnimationsEnabled()) (amount * 1.65f).coerceIn(0.28f, 0.72f) else 1f)
    }

    private fun beginClose(target: Screen?) {
        if (closing) return
        resetDefaultsConfirmUntil = 0f
        save()
        (target as? IqNanoConfigScreen)?.animateReturnFromHub()
        closing = true
        closeTarget = target
        closeProgress = 0f
        draggingSlider = null
        draggingScrollbar = false
        openChoice = null
    }

    private fun animationAmount(): Float {
        if (!isSharedAnimationsEnabled()) return 1f
        return (0.10f + animationSpeed.toFloat().coerceIn(0.1f, 1f) * 0.26f).coerceIn(0.12f, 0.42f)
    }

    private fun approach(current: Float, target: Float, amount: Float): Float =
        current + (target - current) * amount.coerceIn(0f, 1f)

    private fun drawAnimatedBorder(r: Renderer2D, rect: Rect) {
        val radius = CornerRadius.uniform(10f)
        r.strokeRect(rect, border().withAlpha(0.82f), 1.45f, radius)
        r.strokeRect(rect.inset(1f), (if (isCyberpunkTheme()) accent() else Color.fromHex("#FFFFFF")).withAlpha(0.045f), 0.85f, CornerRadius.uniform(9f))
        if (!isSharedAnimationsEnabled()) return
        if (isSharedAdvancedAnimationsEnabled()) {
            drawAdvancedBorder(r, rect)
            return
        }

        val speed = getSharedAnimationSpeed().toFloat().coerceIn(0.1f, 1f)
        val pulse = ((sin(time * (0.65f + speed * 1.15f) * PI.toFloat()) + 1f) * 0.5f).coerceIn(0f, 1f)
        val hot = 0.24f + pulse * 0.20f
        if (isCyberpunkTheme()) {
            r.strokeRect(rect.inset(0.25f), accent().withAlpha(hot), 1.05f, CornerRadius.uniform(9.75f))
        } else {
            r.strokeRect(rect.inset(0.25f), accentHot().withAlpha(0.14f + pulse * 0.10f), 1.15f, CornerRadius.uniform(9.75f))
            r.strokeRect(rect.inset(1.2f), accentGlow().withAlpha(hot), 0.80f, CornerRadius.uniform(8.8f))
        }
    }

    private fun drawAdvancedBorder(r: Renderer2D, rect: Rect) {
        val perimeter = roundedRectPerimeter(rect, 10f)
        val waveSize = min(265f, max(170f, perimeter * 0.20f))
        val speed = getSharedAnimationSpeed().toFloat().coerceIn(0.1f, 1f)
        val animTime = time * (0.35f + speed * 1.35f)
        val travel = (animTime * 92f) % perimeter
        repeat(5) { i ->
            val start = (travel + i * perimeter / 5f + sin(animTime * 0.55f + i * 2.1f) * 14f) % perimeter
            drawBorderWave(r, rect, perimeter, start, waveSize, i * 1.73f, animTime)
        }
    }

    private fun drawBorderWave(r: Renderer2D, rect: Rect, perimeter: Float, start: Float, length: Float, seed: Float, animTime: Float) {
        val steps = (length / 1.45f).toInt().coerceIn(124, 182)
        val stepLength = length / steps
        var from = pointOnRoundedRect(rect, 10f, start, perimeter)
        for (i in 0 until steps) {
            val to = pointOnRoundedRect(rect, 10f, start + stepLength * (i + 1), perimeter)
            val t = (i + 0.5f) / steps.toFloat()
            val gradient = sin(t * PI.toFloat()).coerceAtLeast(0f)
            val softTail = gradient * gradient * (1f - 0.18f * t)
            val organicPulse = 0.70f + 0.30f * ((sin(animTime * (1.65f + seed * 0.18f) + seed + t * 7.1f) + 1f) * 0.5f)
            val shimmer = 0.90f + 0.10f * ((sin(animTime * 3.15f + seed * 3.4f + t * 13.0f) + 1f) * 0.5f)
            val edgeFade = borderWaveEdgeFade(t)
            val midX = (from.x + to.x) * 0.5f
            val midY = (from.y + to.y) * 0.5f
            val intensity = (0.006f + softTail * 0.235f) * edgeFade * organicPulse * shimmer * (1f + cornerGlowBoost(rect, midX, midY) * 0.22f)
            val width = 0.52f + softTail * 1.45f * (0.70f + edgeFade * 0.30f)
            if (isCyberpunkTheme()) {
                r.backend.line(from, to, Paint.solid(accentGlow().withAlpha(intensity * 0.40f)), width + 3.45f)
                r.backend.line(from, to, Paint.solid(accent().withAlpha(intensity * 0.66f)), width + 1.55f)
            } else {
                r.backend.line(from, to, Paint.solid(accent().withAlpha(intensity * 0.17f)), width + 4.15f)
                r.backend.line(from, to, Paint.solid(accentHot().withAlpha(intensity * 0.42f)), width + 1.85f)
                r.backend.line(from, to, Paint.solid(accentGlow().withAlpha(intensity * 0.70f)), width + 0.85f)
            }
            from = to
        }
    }

    private fun borderWaveEdgeFade(t: Float): Float {
        val head = (t / 0.18f).coerceIn(0f, 1f)
        val tail = ((1f - t) / 0.16f).coerceIn(0f, 1f)
        val fadeIn = head * head * (3f - 2f * head)
        val fadeOut = tail * tail * (3f - 2f * tail)
        return fadeIn * fadeOut
    }

    private fun drawBrandTitle(r: Renderer2D, value: String, x: Float, y: Float, size: Float) {
        val speed = getSharedAnimationSpeed().toFloat().coerceIn(0.1f, 1f)
        val anim = if (isSharedAnimationsEnabled()) ((sin(time * (0.75f + speed * 0.65f)) + 1f) * 0.5f) else 0.35f
        val glowPulse = 0.86f + anim * 0.44f
        val face = text().lerp(Color.fromHex("#FFFFFF"), 0.16f + anim * 0.18f)
        r.text(value, x + 1.35f, y + 0.55f, size, brandDetailAccent().withAlpha(0.34f * glowPulse), bold())
        r.text(value, x - 1.05f, y + 0.25f, size, accent().withAlpha(0.20f + 0.12f * anim), bold())
        r.text(value, x + 0.35f, y - 0.85f, size, accentGlow().withAlpha(0.12f + 0.16f * anim), bold())
        r.text(value, x, y - 0.55f, size, Color.fromHex("#FFFFFF").withAlpha(0.12f + 0.12f * anim), bold())
        r.text(value, x, y, size, face, bold())
    }

    private fun drawLogoChromaOutline(r: Renderer2D, logo: Rect) {
        val animEnabled = isSharedAnimationsEnabled()
        val speed = getSharedAnimationSpeed().toFloat().coerceIn(0.1f, 1f)
        val t = if (animEnabled) time * (0.55f + speed * 0.9f) else 0f
        val a = ((sin(t) + 1f) * 0.5f).coerceIn(0f, 1f)
        val b = ((sin(t + 2.09f) + 1f) * 0.5f).coerceIn(0f, 1f)
        val primary = accent().lerp(accentHot(), a)
        val secondary = accentHot().lerp(accentGlow(), b)
        val pulseAlpha = 0.56f + if (animEnabled) pulse(0.16f, 1.1f) else 0f

        r.strokeRect(logo.inset(-2.4f), primary.withAlpha(0.18f * pulseAlpha), 2.1f, CornerRadius.uniform(9f))
        r.strokeRect(logo.inset(-1.5f), secondary.withAlpha(0.34f * pulseAlpha), 1.15f, CornerRadius.uniform(8f))
        r.strokeRect(logo.inset(-0.7f), Color.fromHex("#FFFFFF").withAlpha(0.08f + 0.06f * pulseAlpha), 0.75f, CornerRadius.uniform(7.5f))
    }

    private fun pulse(amount: Float, speed: Float): Float =
        ((sin(time * speed * PI.toFloat()) + 1f) * 0.5f) * amount

    private fun cornerGlowBoost(rect: Rect, x: Float, y: Float): Float {
        val radius = 10f
        val leftDx = x - (rect.x + radius)
        val rightDx = x - (rect.right - radius)
        val topDy = y - (rect.y + radius)
        val bottomDy = y - (rect.bottom - radius)
        val topLeft = leftDx * leftDx + topDy * topDy
        val topRight = rightDx * rightDx + topDy * topDy
        val bottomRight = rightDx * rightDx + bottomDy * bottomDy
        val bottomLeft = leftDx * leftDx + bottomDy * bottomDy
        val nearest = sqrt(min(min(topLeft, topRight), min(bottomRight, bottomLeft)))
        val normalized = (1f - nearest / 42f).coerceIn(0f, 1f)
        return normalized * normalized
    }

    private fun roundedRectPerimeter(rect: Rect, radius: Float): Float =
        2f * ((rect.width - 2f * radius) + (rect.height - 2f * radius)) + 2f * PI.toFloat() * radius

    private fun pointOnRoundedRect(rect: Rect, radius: Float, distance: Float, perimeter: Float = roundedRectPerimeter(rect, radius)): Point {
        val straightW = rect.width - 2f * radius
        val straightH = rect.height - 2f * radius
        val arc = PI.toFloat() * radius / 2f
        var p = distance % perimeter
        if (p < straightW) return Point(rect.x + radius + p, rect.y)
        p -= straightW
        if (p < arc) return arcPoint(rect.right - radius, rect.y + radius, radius, -90f, p / arc)
        p -= arc
        if (p < straightH) return Point(rect.right, rect.y + radius + p)
        p -= straightH
        if (p < arc) return arcPoint(rect.right - radius, rect.bottom - radius, radius, 0f, p / arc)
        p -= arc
        if (p < straightW) return Point(rect.right - radius - p, rect.bottom)
        p -= straightW
        if (p < arc) return arcPoint(rect.x + radius, rect.bottom - radius, radius, 90f, p / arc)
        p -= arc
        if (p < straightH) return Point(rect.x, rect.bottom - radius - p)
        p -= straightH
        return arcPoint(rect.x + radius, rect.y + radius, radius, 180f, p / arc)
    }

    private fun arcPoint(cx: Float, cy: Float, radius: Float, startDegrees: Float, t: Float): Point {
        val angle = (startDegrees + 90f * t) * PI.toFloat() / 180f
        return Point(cx + cos(angle) * radius, cy + sin(angle) * radius)
    }

    private fun disableAllFeatures() {
        val updated = applyBooleanFeatures(false)
        saveMainConfigs()
        MessageUtil.WARNING.sendMessage("Disabled $updated feature toggles.")
    }

    private fun resetAllFeaturesDefault() {
        try {
            val iq = IQModClient.get()
            if (iq == null) {
                MessageUtil.ERROR.sendMessage("IQ client is not initialized yet.")
                return
            }
            iq.resetMainConfigToDefaults()
            runIqReload()
            MessageUtil.SUCCESS.sendMessage("Features reset to default values.")
        } catch (t: Throwable) {
            MessageUtil.ERROR.sendMessage("Failed to reset features: ${t.message}")
            IqNanoVg.logger.warn("Failed to reset IQ features to defaults", t)
        }
    }

    private fun exportConfigToClipboard() {
        val mc = minecraft ?: return
        val fields = collectMainConfigFields()
        if (fields.isEmpty()) {
            MessageUtil.ERROR.sendMessage("No main config fields available to export.")
            return
        }
        val text = fields.keys.sorted().joinToString(";") { key ->
            val value = try {
                val v = fields[key]?.get(null)
                if (v is Enum<*>) v.name else v.toString()
            } catch (_: Throwable) {
                ""
            }
            "$key=$value"
        }
        mc.keyboardHandler.clipboard = text
        MessageUtil.SUCCESS.sendMessage("Main config copied to clipboard.")
    }

    private fun importConfigFromClipboard() {
        val mc = minecraft ?: return
        val text = mc.keyboardHandler.clipboard
        if (text.isNullOrBlank()) {
            MessageUtil.ERROR.sendMessage("Clipboard is empty.")
            return
        }
        val fields = collectMainConfigFields()
        var applied = 0
        text.split(';').forEach { token ->
            val idx = token.indexOf('=')
            if (idx <= 0) return@forEach
            val key = token.substring(0, idx).trim()
            val raw = token.substring(idx + 1).trim()
            val field = fields[key] ?: return@forEach
            try {
                val parsed = parseFieldValue(field, raw) ?: return@forEach
                field.set(null, parsed)
                applied++
            } catch (_: Throwable) {
            }
        }
        saveMainConfigs()
        MessageUtil.SUCCESS.sendMessage("Imported $applied main config values.")
    }

    private fun updateConfigFiles() {
        runIqReload()
        MessageUtil.SUCCESS.sendMessage("Requested IQ config file refresh.")
    }

    private fun runIqReload() {
        val mc = minecraft ?: return
        val player = mc.player ?: return
        player.connection.sendCommand("iq reload")
    }

    private fun softRestartFeatures() {
        runIqReload()
        MessageUtil.INFO.sendMessage("Soft restart requested for feature systems.")
    }

    private fun resetHudPositions() {
        try {
            net.iqaddons.mod.hud.HudManager.get().resetAllConfigs()
            MessageUtil.SUCCESS.sendMessage("HUD positions reset.")
        } catch (t: Throwable) {
            MessageUtil.ERROR.sendMessage("Failed to reset HUD positions: ${t.message}")
            IqNanoVg.logger.warn("Failed to reset IQ HUD positions", t)
        }
    }

    private fun resetHubConfigDefaults() {
        themeIndex = DEFAULT_THEME_INDEX
        guiOpacity = DEFAULT_GUI_OPACITY
        uiScale = DEFAULT_UI_SCALE
        animationModeIndex = DEFAULT_ANIMATION_MODE
        animationsEnabled = animationModeIndex != ANIM_OFF
        animationSpeed = DEFAULT_ANIMATION_SPEED
        outlineShadow = DEFAULT_OUTLINE_SHADOW
        uiStatePersistenceEnabled = DEFAULT_UI_STATE_PERSISTENCE
        autoReloadConfig = DEFAULT_AUTO_RELOAD_CONFIG
        globalHudToggle = DEFAULT_GLOBAL_HUD_TOGGLE
        overlayOpacity = DEFAULT_OVERLAY_OPACITY
        snapToGrid = DEFAULT_SNAP_TO_GRID
        showBoundingBoxes = DEFAULT_SHOW_BOUNDING_BOXES
        densityIndex = DEFAULT_DENSITY_INDEX
        toggleStyleIndex = DEFAULT_TOGGLE_STYLE_INDEX
        hudStyleIndex = DEFAULT_HUD_STYLE_INDEX
        showFeatureLogs = DEFAULT_FEATURE_LOGS
        debugOverlay = DEFAULT_DEBUG_OVERLAY
        debugEvents = DEFAULT_DEBUG_EVENTS
        openChoice = null
        choiceScroll.clear()
        scroll = 0f
        save()
        net.iqaddons.mod.hud.HudManager.getIfInitialized()?.reloadCurrentStyleConfigs()
        MessageUtil.SUCCESS.sendMessage("Config Hub settings reset to defaults.")
    }

    private fun copyDebugInfo() {
        val mc = minecraft ?: return
        mc.keyboardHandler.clipboard = "IQ Debug | debugOverlay=$debugOverlay | debugEvents=$debugEvents | featureLogs=$showFeatureLogs"
        MessageUtil.INFO.sendMessage("Debug info copied.")
    }

    private fun applyBooleanFeatures(value: Boolean): Int {
        var updated = 0
        collectMainConfigFields().values.forEach { field ->
            val type = field.type
            if (type == java.lang.Boolean.TYPE || type == java.lang.Boolean::class.java) {
                try {
                    field.set(null, value)
                    updated++
                } catch (_: Throwable) {
                }
            }
        }
        return updated
    }

    private fun collectMainConfigFields(): Map<String, Field> {
        val out = linkedMapOf<String, Field>()
        configClasses.forEach { configClass ->
            val category = buildCategory(configClass) ?: return@forEach
            collectFieldEntries(category.entries(), out)
        }
        return out
    }

    private fun collectFieldEntries(entries: List<ConfigEntryModel>, out: MutableMap<String, Field>) {
        entries.forEach { entry ->
            if (entryType(entry) == EntryType.SECTION_HEADER) {
                children(entry)?.let { collectFieldEntries(it, out) }
                return@forEach
            }
            val field = entryField(entry) ?: return@forEach
            val type = field.type
            if (type == java.lang.Boolean.TYPE || type == java.lang.Boolean::class.java ||
                type == Integer.TYPE || type == java.lang.Integer::class.java ||
                type == java.lang.Float.TYPE || type == java.lang.Float::class.java ||
                type == java.lang.Double.TYPE || type == java.lang.Double::class.java ||
                type == java.lang.Long.TYPE || type == java.lang.Long::class.java ||
                type == String::class.java || type.isEnum
            ) {
                out["${field.declaringClass.simpleName}.${field.name}"] = field
            }
        }
    }

    private fun parseFieldValue(field: Field, raw: String): Any? {
        val type = field.type
        return try {
            when {
                type == java.lang.Boolean.TYPE || type == java.lang.Boolean::class.java -> raw.toBoolean()
                type == Integer.TYPE || type == java.lang.Integer::class.java -> raw.toInt()
                type == java.lang.Float.TYPE || type == java.lang.Float::class.java -> raw.toFloat()
                type == java.lang.Double.TYPE || type == java.lang.Double::class.java -> raw.toDouble()
                type == java.lang.Long.TYPE || type == java.lang.Long::class.java -> raw.toLong()
                type == String::class.java -> raw
                type.isEnum -> {
                    @Suppress("UNCHECKED_CAST")
                    java.lang.Enum.valueOf(type as Class<out Enum<*>>, raw.uppercase(Locale.ROOT))
                }
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun saveMainConfigs() {
        saveResourcefulConfig()
    }

    private sealed class Row(open val title: String, open val description: String) {
        data class Toggle(override val title: String, override val description: String, val value: Boolean, val set: (Boolean) -> Unit) : Row(title, description)
        data class Slider(override val title: String, override val description: String, val value: Double, val min: Double, val max: Double, val set: (Double) -> Unit) : Row(title, description)
        data class Button(override val title: String, override val description: String, val buttonLabel: String, val action: () -> Unit) : Row(title, description)
        data class Choice(
            override val title: String,
            override val description: String,
            val options: Array<String>,
            val index: Int,
            val set: (Int) -> Unit,
        ) : Row(title, description)
    }

    companion object {
        private data class Palette(
            val accent: Color,
            val accentHot: Color,
            val accentGlow: Color,
            val border: Color,
            val panelStart: Color,
            val panelEnd: Color,
            val surface: Color,
            val buttonSurface: Color,
            val modalSurface: Color,
            val toggleOff: Color,
            val knob: Color,
            val text: Color,
            val muted: Color,
            val disabled: Color,
        )

        private val THEMES = arrayOf("Default (IQ)", "Night", "Dark", "Thunder", "Ruby", "RAZER", "Cyberpunk 2077", "JOTA")
        private const val THEME_CYBERPUNK = 6
        private const val THEME_JOTA = 7
        private val PALETTES = arrayOf(
            Palette(
                Color.fromHex("#EC4BAF"), Color.fromHex("#F25DC0"), Color.fromHex("#FF7CD1"), Color.fromHex("#7A3D75"),
                Color.fromHex("#120917"), Color.fromHex("#030207"), Color.fromHex("#120E1C"), Color.fromHex("#160A16"),
                Color.fromHex("#07040B"), Color.fromHex("#261B2D"), Color.fromHex("#FAF1FB"), Color.fromHex("#F8F1FB"), Color.fromHex("#C9B4D5"), Color.fromHex("#756579"),
            ),
            Palette(
                Color.fromHex("#A7B0BE"), Color.fromHex("#D8DEE9"), Color.fromHex("#FFFFFF"), Color.fromHex("#475569"),
                Color.fromHex("#080B10"), Color.fromHex("#020304"), Color.fromHex("#0D1117"), Color.fromHex("#111722"),
                Color.fromHex("#05070A"), Color.fromHex("#222832"), Color.fromHex("#F4F7FB"), Color.fromHex("#F4F7FB"), Color.fromHex("#B8C0CC"), Color.fromHex("#5B6470"),
            ),
            Palette(
                Color.fromHex("#E5E7EB"), Color.fromHex("#FFFFFF"), Color.fromHex("#9CA3AF"), Color.fromHex("#343A46"),
                Color.fromHex("#030303"), Color.fromHex("#000000"), Color.fromHex("#060607"), Color.fromHex("#08090B"),
                Color.fromHex("#010101"), Color.fromHex("#111318"), Color.fromHex("#08090B"), Color.fromHex("#FAFAFA"), Color.fromHex("#AEB4BE"), Color.fromHex("#555B66"),
            ),
            Palette(
                Color.fromHex("#4FC3FF"), Color.fromHex("#BDEBFF"), Color.fromHex("#FFFFFF"), Color.fromHex("#344A63"),
                Color.fromHex("#050A12"), Color.fromHex("#010307"), Color.fromHex("#09111C"), Color.fromHex("#0B1018"),
                Color.fromHex("#02050A"), Color.fromHex("#162233"), Color.fromHex("#F4FAFF"), Color.fromHex("#F4FAFF"), Color.fromHex("#B8C8D8"), Color.fromHex("#667586"),
            ),
            Palette(
                Color.fromHex("#F43F5E"), Color.fromHex("#FB7185"), Color.fromHex("#FFE4E6"), Color.fromHex("#7F1D1D"),
                Color.fromHex("#130506"), Color.fromHex("#020101"), Color.fromHex("#15080B"), Color.fromHex("#18090C"),
                Color.fromHex("#060203"), Color.fromHex("#2B1115"), Color.fromHex("#FFF1F2"), Color.fromHex("#FFF1F2"), Color.fromHex("#F2B4BF"), Color.fromHex("#76545C"),
            ),
            Palette(
                Color.fromHex("#44D62C"), Color.fromHex("#7CFF5B"), Color.fromHex("#B6FF8A"), Color.fromHex("#1D6B2B"),
                Color.fromHex("#061009"), Color.fromHex("#010402"), Color.fromHex("#07140A"), Color.fromHex("#0A1A0D"),
                Color.fromHex("#020703"), Color.fromHex("#15301B"), Color.fromHex("#F2FFF0"), Color.fromHex("#F4FFF2"), Color.fromHex("#B7D4B2"), Color.fromHex("#58725A"),
            ),
            Palette(
                Color.fromHex("#F7FF00"), Color.fromHex("#00E5FF"), Color.fromHex("#FF2AA3"), Color.fromHex("#FF2AA3"),
                Color.fromHex("#06101A"), Color.fromHex("#01040A"), Color.fromHex("#07101A"), Color.fromHex("#08111B"),
                Color.fromHex("#02070E"), Color.fromHex("#08222B"), Color.fromHex("#121417"), Color.fromHex("#F4F7FF"), Color.fromHex("#00DDEB"), Color.fromHex("#62717A"),
            ),
            Palette(
                Color.fromHex("#FF2A2A"), Color.fromHex("#FF3030"), Color.fromHex("#FF4A4A"), Color.fromHex("#7F1515"),
                Color.fromHex("#0B0B0D"), Color.fromHex("#020202"), Color.fromHex("#12080C"), Color.fromHex("#170809"),
                Color.fromHex("#07090D"), Color.fromHex("#2A1114"), Color.fromHex("#EEE8E5"), Color.fromHex("#F3EFED"), Color.fromHex("#A9A19D"), Color.fromHex("#706A68"),
            ),
        )
        private val DENSITY = arrayOf("Compact", "Normal", "Spaced")
        private val TOGGLE_STYLES = arrayOf("Switch", "Checkbox", "Minimal")
        private val HUD_STYLES = arrayOf("VANILLA", "MODERN")
        private val ANIMATION_MODES = arrayOf("OFF", "Simple", "Advanced")
        private const val BRAZIL_TOOLTIP_TEXT = "made in brazil"
        private const val ANIM_OFF = 0
        private const val ANIM_SIMPLE = 1
        private const val ANIM_ADVANCED = 2

        private val K_THEME = DataKey.of("globalcfg.theme", Int::class.javaObjectType)
        private val K_GUI_OPACITY = DataKey.of("globalcfg.guiOpacity", Double::class.javaObjectType)
        private val K_UI_SCALE = DataKey.of("globalcfg.uiScale", Double::class.javaObjectType)
        private val K_ANIM_MODE = DataKey.of("globalcfg.animMode", Int::class.javaObjectType)
        private val K_ANIM_ENABLED = DataKey.of("globalcfg.animEnabled", Boolean::class.javaObjectType)
        private val K_ANIM_SPEED = DataKey.of("globalcfg.animSpeed", Double::class.javaObjectType)
        private val K_OUTLINE = DataKey.of("globalcfg.outline", Boolean::class.javaObjectType)
        private val K_AUTO_RELOAD = DataKey.of("globalcfg.autoReload", Boolean::class.javaObjectType)
        private val K_HUD = DataKey.of("globalcfg.hud", Boolean::class.javaObjectType)
        private val K_OVERLAY_OPACITY = DataKey.of("globalcfg.overlayOpacity", Double::class.javaObjectType)
        private val K_SNAP = DataKey.of("globalcfg.snap", Boolean::class.javaObjectType)
        private val K_BOUNDS = DataKey.of("globalcfg.bounds", Boolean::class.javaObjectType)
        private val K_DENSITY = DataKey.of("globalcfg.density", Int::class.javaObjectType)
        private val K_TOGGLE_STYLE = DataKey.of("globalcfg.toggleStyle", Int::class.javaObjectType)
        private val K_HUD_STYLE = DataKey.of("globalcfg.hudStyle", Int::class.javaObjectType)
        private val K_HUD_STYLE_OPTIONS_V2 = DataKey.of("globalcfg.hudStyleOptionsV2", Boolean::class.javaObjectType)
        private val K_DEBUG_LOGS = DataKey.of("globalcfg.debugLogs", Boolean::class.javaObjectType)
        private val K_DEBUG_OVERLAY = DataKey.of("globalcfg.debugOverlay", Boolean::class.javaObjectType)
        private val K_DEBUG_EVENTS = DataKey.of("globalcfg.debugEvents", Boolean::class.javaObjectType)
        private val K_UI_STATE_PERSIST = DataKey.of("globalcfg.uiStatePersist", Boolean::class.javaObjectType)

        private const val DEFAULT_THEME_INDEX = 0
        private const val DEFAULT_GUI_OPACITY = 0.75
        private const val DEFAULT_UI_SCALE = 1.0
        private const val DEFAULT_ANIMATION_MODE = ANIM_SIMPLE
        private const val DEFAULT_ANIMATIONS_ENABLED = true
        private const val DEFAULT_ANIMATION_SPEED = 0.7
        private const val DEFAULT_OUTLINE_SHADOW = true
        private const val DEFAULT_UI_STATE_PERSISTENCE = true
        private const val DEFAULT_AUTO_RELOAD_CONFIG = false
        private const val DEFAULT_GLOBAL_HUD_TOGGLE = true
        private const val DEFAULT_OVERLAY_OPACITY = 0.9
        private const val DEFAULT_SNAP_TO_GRID = true
        private const val DEFAULT_SHOW_BOUNDING_BOXES = false
        private const val DEFAULT_DENSITY_INDEX = 1
        private const val DEFAULT_TOGGLE_STYLE_INDEX = 0
        private const val DEFAULT_HUD_STYLE_INDEX = 0
        private const val DEFAULT_FEATURE_LOGS = false
        private const val DEFAULT_DEBUG_OVERLAY = false
        private const val DEFAULT_DEBUG_EVENTS = false

        @JvmStatic var themeIndex = DEFAULT_THEME_INDEX
        @JvmStatic var guiOpacity = DEFAULT_GUI_OPACITY
        @JvmStatic var uiScale = DEFAULT_UI_SCALE
        @JvmStatic var animationModeIndex = DEFAULT_ANIMATION_MODE
        @JvmStatic var animationsEnabled = DEFAULT_ANIMATIONS_ENABLED
        @JvmStatic var animationSpeed = DEFAULT_ANIMATION_SPEED
        @JvmStatic var outlineShadow = DEFAULT_OUTLINE_SHADOW
        @JvmStatic var uiStatePersistenceEnabled = DEFAULT_UI_STATE_PERSISTENCE
        @JvmStatic var autoReloadConfig = DEFAULT_AUTO_RELOAD_CONFIG
        @JvmStatic var globalHudToggle = DEFAULT_GLOBAL_HUD_TOGGLE
        @JvmStatic var overlayOpacity = DEFAULT_OVERLAY_OPACITY
        @JvmStatic var snapToGrid = DEFAULT_SNAP_TO_GRID
        @JvmStatic var showBoundingBoxes = DEFAULT_SHOW_BOUNDING_BOXES
        @JvmStatic var densityIndex = DEFAULT_DENSITY_INDEX
        @JvmStatic var toggleStyleIndex = DEFAULT_TOGGLE_STYLE_INDEX
        @JvmStatic var hudStyleIndex = DEFAULT_HUD_STYLE_INDEX
        @JvmStatic var showFeatureLogs = DEFAULT_FEATURE_LOGS
        @JvmStatic var debugOverlay = DEFAULT_DEBUG_OVERLAY
        @JvmStatic var debugEvents = DEFAULT_DEBUG_EVENTS
        private var loadedSharedState = false

        @JvmStatic fun load() {
            val s = IQPersistentDataStore.get()
            themeIndex = s.getOrDefault(K_THEME, themeIndex).coerceIn(0, THEMES.lastIndex)
            guiOpacity = s.getOrDefault(K_GUI_OPACITY, guiOpacity)
            uiScale = s.getOrDefault(K_UI_SCALE, uiScale)
            animationModeIndex = loadAnimationModeIndex(s)
            animationsEnabled = animationModeIndex != ANIM_OFF
            animationSpeed = s.getOrDefault(K_ANIM_SPEED, animationSpeed)
            outlineShadow = s.getOrDefault(K_OUTLINE, outlineShadow)
            autoReloadConfig = s.getOrDefault(K_AUTO_RELOAD, autoReloadConfig)
            globalHudToggle = s.getOrDefault(K_HUD, globalHudToggle)
            overlayOpacity = s.getOrDefault(K_OVERLAY_OPACITY, overlayOpacity)
            snapToGrid = s.getOrDefault(K_SNAP, snapToGrid)
            showBoundingBoxes = s.getOrDefault(K_BOUNDS, showBoundingBoxes)
            densityIndex = s.getOrDefault(K_DENSITY, densityIndex)
            toggleStyleIndex = s.getOrDefault(K_TOGGLE_STYLE, toggleStyleIndex)
            hudStyleIndex = loadHudStyleIndex(s)
            showFeatureLogs = s.getOrDefault(K_DEBUG_LOGS, showFeatureLogs)
            debugOverlay = s.getOrDefault(K_DEBUG_OVERLAY, debugOverlay)
            debugEvents = s.getOrDefault(K_DEBUG_EVENTS, debugEvents)
            uiStatePersistenceEnabled = s.getOrDefault(K_UI_STATE_PERSIST, uiStatePersistenceEnabled)
            loadedSharedState = true
        }

        @JvmStatic fun save() {
            val s = IQPersistentDataStore.get()
            s.set(K_THEME, themeIndex.coerceIn(0, THEMES.lastIndex))
            s.set(K_GUI_OPACITY, guiOpacity)
            s.set(K_UI_SCALE, uiScale)
            animationModeIndex = animationModeIndex.coerceIn(0, ANIMATION_MODES.lastIndex)
            animationsEnabled = animationModeIndex != ANIM_OFF
            s.set(K_ANIM_MODE, animationModeIndex)
            s.set(K_ANIM_ENABLED, animationsEnabled)
            s.set(K_ANIM_SPEED, animationSpeed)
            s.set(K_OUTLINE, outlineShadow)
            s.set(K_AUTO_RELOAD, autoReloadConfig)
            s.set(K_HUD, globalHudToggle)
            s.set(K_OVERLAY_OPACITY, overlayOpacity)
            s.set(K_SNAP, snapToGrid)
            s.set(K_BOUNDS, showBoundingBoxes)
            s.set(K_DENSITY, densityIndex)
            s.set(K_TOGGLE_STYLE, toggleStyleIndex)
            s.set(K_HUD_STYLE, hudStyleIndex)
            s.set(K_HUD_STYLE_OPTIONS_V2, true)
            s.set(K_DEBUG_LOGS, showFeatureLogs)
            s.set(K_DEBUG_OVERLAY, debugOverlay)
            s.set(K_DEBUG_EVENTS, debugEvents)
            s.set(K_UI_STATE_PERSIST, uiStatePersistenceEnabled)
            saveResourcefulConfig()
        }

        private fun ensureLoaded() {
            if (!loadedSharedState) load()
        }

        private fun loadHudStyleIndex(s: IQPersistentDataStore): Int {
            if (s.getOrDefault(K_HUD_STYLE_OPTIONS_V2, false)) {
                return s.getOrDefault(K_HUD_STYLE, hudStyleIndex).coerceIn(0, HUD_STYLES.lastIndex)
            }
            val migrated = when (s.getOrDefault(K_HUD_STYLE, -1)) {
                -1 -> hudStyleIndex
                0 -> 1
                1 -> 0
                else -> hudStyleIndex
            }.coerceIn(0, HUD_STYLES.lastIndex)
            s.set(K_HUD_STYLE, migrated)
            s.set(K_HUD_STYLE_OPTIONS_V2, true)
            return migrated
        }

        private fun loadAnimationModeIndex(s: IQPersistentDataStore): Int {
            if (s.has(K_ANIM_MODE)) {
                return s.getOrDefault(K_ANIM_MODE, DEFAULT_ANIMATION_MODE).coerceIn(0, ANIMATION_MODES.lastIndex)
            }
            return if (s.getOrDefault(K_ANIM_ENABLED, DEFAULT_ANIMATIONS_ENABLED)) ANIM_SIMPLE else ANIM_OFF
        }

        fun getSharedThemeIndex(): Int { ensureLoaded(); return themeIndex }
        @JvmStatic fun isCyberpunkTheme(): Boolean {
            ensureLoaded()
            return themeIndex.coerceIn(0, THEMES.lastIndex) == THEME_CYBERPUNK
        }
        @JvmStatic fun brandName(): String {
            ensureLoaded()
            return when (themeIndex.coerceIn(0, THEMES.lastIndex)) {
                THEME_CYBERPUNK -> "LUCY"
                THEME_JOTA -> "JOTA"
                else -> "IQ"
            }
        }
        @JvmStatic fun brandIconPath(): String {
            ensureLoaded()
            return when (themeIndex.coerceIn(0, THEMES.lastIndex)) {
                THEME_CYBERPUNK -> "/assets/iq/textures/icon_lucy.png"
                THEME_JOTA -> "/assets/iq/textures/icon_jota.png"
                else -> "/assets/iq/textures/icon.png"
            }
        }
        @JvmStatic fun brandBackgroundPath(): String? {
            ensureLoaded()
            return when (themeIndex.coerceIn(0, THEMES.lastIndex)) {
                THEME_CYBERPUNK -> "/assets/iq/textures/bg_lucy.jpg"
                THEME_JOTA -> "/assets/iq/textures/bg_jota.jpg"
                else -> null
            }
        }
        @JvmStatic fun brandTooltip(): String {
            ensureLoaded()
            return when (themeIndex.coerceIn(0, THEMES.lastIndex)) {
                THEME_CYBERPUNK -> "theme for those sad ppl"
                THEME_JOTA -> "my own personal theme hehe"
                else -> BRAZIL_TOOLTIP_TEXT
            }
        }
        @JvmStatic fun configScreenTitle(suffix: String): String = "${brandName()} $suffix"
        fun getSharedGuiOpacity(): Double { ensureLoaded(); return guiOpacity }
        @JvmStatic fun getSharedOverlayOpacity(): Double { ensureLoaded(); return overlayOpacity }
        fun getSharedUiScale(): Double { ensureLoaded(); return uiScale }
        fun isSharedAnimationsEnabled(): Boolean {
            ensureLoaded()
            animationsEnabled = animationModeIndex != ANIM_OFF
            return animationsEnabled
        }
        fun isSharedAdvancedAnimationsEnabled(): Boolean { ensureLoaded(); return animationModeIndex == ANIM_ADVANCED }
        fun getSharedAnimationSpeed(): Double { ensureLoaded(); return animationSpeed }
        fun isSharedOutlineShadowEnabled(): Boolean { ensureLoaded(); return outlineShadow }
        fun isSharedUiStatePersistenceEnabled(): Boolean { ensureLoaded(); return uiStatePersistenceEnabled }
        @JvmStatic fun isSharedGlobalHudEnabled(): Boolean { ensureLoaded(); return globalHudToggle }
        @JvmStatic fun getSharedHudStyleIndex(): Int {
            ensureLoaded()
            return hudStyleIndex.coerceIn(0, HUD_STYLES.lastIndex)
        }
        @JvmStatic fun sharedHudStyleLabel(): String {
            ensureLoaded()
            return if (hudStyleIndex == 1) "MODERN" else "VANILLA"
        }
        @JvmStatic fun toggleSharedHudStyle(): Int {
            ensureLoaded()
            hudStyleIndex = if (hudStyleIndex == 1) 0 else 1
            save()
            return hudStyleIndex
        }
        @JvmStatic fun isSharedModernHudStyle(): Boolean { ensureLoaded(); return hudStyleIndex == 1 }
        @JvmStatic fun fontRegular(): String = "Rajdhani-Regular"
        @JvmStatic fun fontMedium(): String = "Rajdhani-Medium"
        @JvmStatic fun fontSemiBold(): String = "Rajdhani-SemiBold"
        @JvmStatic fun fontBold(): String = "Rajdhani-Bold"

        @JvmStatic fun themeAccent(): Color { ensureLoaded(); return palette().accent }
        @JvmStatic fun themeAccentHot(): Color { ensureLoaded(); return palette().accentHot }
        @JvmStatic fun themeAccentGlow(): Color { ensureLoaded(); return palette().accentGlow }
        @JvmStatic fun themeBorder(): Color { ensureLoaded(); return palette().border }
        @JvmStatic fun themePanelStart(): Color { ensureLoaded(); return palette().panelStart }
        @JvmStatic fun themePanelEnd(): Color { ensureLoaded(); return palette().panelEnd }
        @JvmStatic fun themeSurface(): Color { ensureLoaded(); return palette().surface }
        @JvmStatic fun themeButtonSurface(): Color { ensureLoaded(); return palette().buttonSurface }
        @JvmStatic fun themeModalSurface(): Color { ensureLoaded(); return palette().modalSurface }
        @JvmStatic fun themeToggleOff(): Color { ensureLoaded(); return palette().toggleOff }
        @JvmStatic fun themeKnob(): Color { ensureLoaded(); return palette().knob }
        @JvmStatic fun themeText(): Color { ensureLoaded(); return palette().text }
        @JvmStatic fun themeMuted(): Color { ensureLoaded(); return palette().muted }
        @JvmStatic fun themeDisabled(): Color { ensureLoaded(); return palette().disabled }

        private fun palette(): Palette = PALETTES[themeIndex.coerceIn(0, PALETTES.lastIndex)]
        private fun accent() = palette().accent
        private fun accentHot() = palette().accentHot
        private fun accentGlow() = palette().accentGlow
        private fun border() = palette().border
        private fun panelStart() = palette().panelStart
        private fun panelEnd() = palette().panelEnd
        private fun surface() = palette().surface
        private fun buttonSurface() = palette().buttonSurface
        private fun modalSurface() = palette().modalSurface
        private fun toggleOff() = palette().toggleOff
        private fun knob() = palette().knob
        private fun text() = palette().text
        private fun muted() = palette().muted
        private fun sliderTrack() = toggleOff().lerp(muted(), 0.12f).withAlpha(0.92f)
        private fun brandDetailAccent() = if (isCyberpunkTheme()) accentHot() else accent()
        private fun brandSubtitleColor() = if (isCyberpunkTheme()) Color.fromHex("#00C8D8") else muted()
        private fun regular() = fontRegular()
        private fun semi() = fontSemiBold()
        private fun bold() = fontBold()

        private fun buildCategory(type: Class<*>): net.iqaddons.mod.screen.model.ConfigCategory? = try {
            val category = Class.forName("net.iqaddons.mod.utils.ConfigReflectionUtil")
                .getMethod("buildCategory", Class::class.java)
                .invoke(null, type) as? net.iqaddons.mod.screen.model.ConfigCategory
            if (category != null && category.entries().isNotEmpty()) category else null
        } catch (t: Throwable) {
            IqNanoVg.logger.warn("Failed to build IQ config category for ${type.name}", t)
            null
        }

        private fun modelField(name: String): Field =
            ConfigEntryModel::class.java.getDeclaredField(name).apply { isAccessible = true }

        private val F_TYPE = modelField("type")
        private val F_FIELD = modelField("field")
        private val F_CHILDREN = modelField("children")

        private fun entryType(e: ConfigEntryModel): EntryType = F_TYPE.get(e) as EntryType
        private fun entryField(e: ConfigEntryModel): Field? = F_FIELD.get(e) as? Field
        @Suppress("UNCHECKED_CAST")
        private fun children(e: ConfigEntryModel): List<ConfigEntryModel>? = F_CHILDREN.get(e) as? List<ConfigEntryModel>

        private fun saveResourcefulConfig() {
            try {
                IQModClient.get()?.saveMainConfig()
            } catch (t: Throwable) {
                IqNanoVg.logger.warn("Failed to save IQ main config from Nano global config screen", t)
            }
        }
    }
}
