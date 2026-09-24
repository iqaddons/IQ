package net.iqaddons.mod.screen.nano

import net.iqaddons.mod.IQModClient
import net.iqaddons.mod.config.Configuration
import net.iqaddons.mod.config.categories.KuudraGeneralConfig
import net.iqaddons.mod.config.categories.PhaseFourConfig
import net.iqaddons.mod.config.categories.PhaseOneConfig
import net.iqaddons.mod.config.categories.PhaseThreeConfig
import net.iqaddons.mod.config.categories.PhaseTwoConfig
import net.iqaddons.mod.manager.IQPersistentDataStore
import net.iqaddons.mod.manager.ProfitTrackerDisplayManager
import net.iqaddons.mod.nanovg.IqNanoVg
import net.iqaddons.mod.nanovg.IqNanoVgRenderable
import net.iqaddons.mod.nanovg.rendering.backend.Color
import net.iqaddons.mod.nanovg.rendering.backend.Paint
import net.iqaddons.mod.nanovg.rendering.backend.TextAlign
import net.iqaddons.mod.nanovg.rendering.renderer.Renderer2D
import net.iqaddons.mod.nanovg.util.CornerRadius
import net.iqaddons.mod.nanovg.util.Point
import net.iqaddons.mod.nanovg.util.Rect
import net.iqaddons.mod.screen.model.ConfigCategory
import net.iqaddons.mod.screen.model.ConfigEntryModel
import net.iqaddons.mod.screen.model.ConfigEntryModel.EntryType
import net.iqaddons.mod.utils.MessageUtil
import net.iqaddons.mod.utils.data.DataKey
import net.iqaddons.mod.model.profit.ProfitTrackerDisplayLine
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW
import java.lang.reflect.Field
import java.util.IdentityHashMap
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

open class IqNanoConfigScreen(
    private val parent: Screen?,
    private vararg val configClasses: Class<*>,
) : Screen(Component.literal(IqNanoGlobalConfigScreen.configScreenTitle("Config"))), IqNanoVgRenderable {

    private var initialCategoryId: String? = null
    private var initialEntryLabel: String? = null

    constructor(parent: Screen?, initialCategoryId: String?, vararg configClasses: Class<*>) : this(parent, *configClasses) {
        this.initialCategoryId = initialCategoryId
    }

    constructor(parent: Screen?, initialCategoryId: String?, initialEntryLabel: String?, vararg configClasses: Class<*>) : this(parent, *configClasses) {
        this.initialCategoryId = initialCategoryId
        this.initialEntryLabel = initialEntryLabel
    }

    private data class RowHit(val entry: ConfigEntryModel, val rect: Rect, val depth: Int)
    private data class VisibleEntry(val entry: ConfigEntryModel, val depth: Int, val expansion: Float = 1f)
    private data class SidebarHit(val index: Int, val rect: Rect)
    private data class ActionHit(val id: String, val rect: Rect, val action: () -> Unit)
    private data class SliderHit(val entry: ConfigEntryModel, val rect: Rect)
    private data class NumberInputHit(val entry: ConfigEntryModel, val rect: Rect)
    private data class SelectHit(val entry: ConfigEntryModel, val rect: Rect)
    private data class SelectOptionHit(val entry: ConfigEntryModel, val value: Any?, val rect: Rect)
    private data class ScrollbarHit(val track: Rect, val thumb: Rect)
    private data class ProfitLineHit(val line: ProfitTrackerDisplayLine, val active: Boolean, val rect: Rect)
    private data class ProfitAddHit(val line: ProfitTrackerDisplayLine, val rect: Rect)
    private data class ContextMenuHit(val rect: Rect, val entry: ConfigEntryModel)

    private val store = IQPersistentDataStore.get()
    private val categories = mutableListOf<ConfigCategory>()
    private val rows = mutableListOf<RowHit>()
    private val sidebarHits = mutableListOf<SidebarHit>()
    private val actions = mutableListOf<ActionHit>()
    private val sliderHits = mutableListOf<SliderHit>()
    private val numberInputHits = mutableListOf<NumberInputHit>()
    private val selectHits = mutableListOf<SelectHit>()
    private val selectOptionHits = mutableListOf<SelectOptionHit>()
    private val profitLineHits = mutableListOf<ProfitLineHit>()
    private val profitAddHits = mutableListOf<ProfitAddHit>()
    private val query = StringBuilder()
    private var selectedCategory = 0
    private var scroll = 0f
    private var maxScroll = 0f
    private var draggingSlider: ConfigEntryModel? = null
    private var editingNumber: ConfigEntryModel? = null
    private val editingNumberText = StringBuilder()
    private var openSelect: ConfigEntryModel? = null
    private var selectDropdownRect: Rect? = null
    private var draggingScrollbar = false
    private var scrollbarDragOffset = 0f
    private var scrollbarHit: ScrollbarHit? = null
    private var sidebarRect = Rect.ZERO
    private var sidebarScroll = 0f
    private var maxSidebarScroll = 0f
    private var layoutScale = 1f
    private var draggingColorTarget: String? = null
    private var colorEditing: ConfigEntryModel? = null
    private val colorHueMemory = HashMap<String, Float>()
    private var colorModalProgress = 0f
    private var hoverTooltip: Pair<Rect, String>? = null
    private var searchFocused = false
    private var panel = Rect.ZERO
    private var content = Rect.ZERO
    private var mouseXf = 0f
    private var mouseYf = 0f
    private var time = 0f
    private var openProgress = 0f
    private var closeProgress = 0f
    private var closing = false
    private var closeTarget: Screen? = null
    private var searchFocusAnim = 0f
    private val categoryAnim = HashMap<String, Float>()
    private val rowHoverAnim = HashMap<String, Float>()
    private val toggleAnim = HashMap<String, Float>()
    private val sectionAnim = HashMap<String, Float>()
    private val sectionPaths = IdentityHashMap<ConfigEntryModel, String>()
    private val selectAnim = HashMap<String, Float>()
    private val visibilityAnim = HashMap<String, Float>()
    private val selectScroll = HashMap<String, Int>()
    private val profitDisplayManager = ProfitTrackerDisplayManager.get()
    private val profitActiveLines = mutableListOf<ProfitTrackerDisplayLine>()
    private val profitInactiveLines = mutableListOf<ProfitTrackerDisplayLine>()
    private var profitDisplayExpanded = false
    private var profitDraggingLine: ProfitTrackerDisplayLine? = null
    private var profitTrashRect: Rect? = null
    private var profitPanelRect: Rect? = null
    private var profitActiveDropRect: Rect? = null
    private var profitInactiveDropRect: Rect? = null
    private var contextMenuEntry: ConfigEntryModel? = null
    private var contextMenuRect: Rect? = null
    private var contextMenuHit: ContextMenuHit? = null
    private var contextMenuOpenedAt = 0f

    fun animateReturnFromHub() {
        openProgress = 0.18f
    }

    override fun init() {
        IqNanoVg.prepare()
        IqNanoGlobalConfigScreen.load()
        categories.clear()
        effectiveConfigClasses().mapNotNullTo(categories) { buildCategory(it) }
        rebuildSectionPathIndex()
        reloadProfitDisplayLines()
        IqNanoVg.logger.info("IQ Nano config loaded ${categories.size} categories from ${effectiveConfigClasses().size} config classes")
        restoreUiSession()
        initialCategoryId?.let { id ->
            val index = categories.indexOfFirst { it.id().equals(id, ignoreCase = true) }
            if (index >= 0) {
                selectedCategory = index
                scroll = 0f
                query.clear()
            }
            initialCategoryId = null
        }
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
        updateAnimations()
        if (closing && closeProgress >= 0.96f) {
            val target = closeTarget
            closing = false
            closeTarget = null
            saveConfig()
            minecraft?.gui?.setScreen(target)
            return
        }
        rows.clear()
        sidebarHits.clear()
        actions.clear()
        sliderHits.clear()
        numberInputHits.clear()
        selectHits.clear()
        selectOptionHits.clear()
        profitLineHits.clear()
        profitAddHits.clear()
        scrollbarHit = null
        profitTrashRect = null
        profitPanelRect = null
        profitActiveDropRect = null
        profitInactiveDropRect = null
        contextMenuHit = null
        hoverTooltip = null

        val w = width.toFloat()
        val h = height.toFloat()
        val scale = IqNanoGlobalConfigScreen.getSharedUiScale().toFloat().coerceIn(0.75f, 1.35f)
        layoutScale = scale
        val panelMinW = min(430f, w - 56f)
        val panelMinH = min(260f, h - 46f)
        val panelW = min(max(max(575f, w * 0.60f) * scale, panelMinW), w - 56f)
        val panelH = min(max(max(320f, h * 0.62f) * scale, panelMinH), h - 46f)
        panel = Rect((w - panelW) / 2f, (h - panelH) / 2f, panelW, panelH)
        val sidebarW = 158f.coerceAtMost(panelW * 0.255f).coerceAtLeast(112f)
        val headerH = 46f
        val sidebar = Rect(panel.x, panel.y, sidebarW, panel.height)
        sidebarRect = sidebar
        val header = Rect(panel.x + sidebarW, panel.y, panel.width - sidebarW, headerH)
        content = Rect(header.x + 14f, panel.y + headerH + 8f, header.width - 22f, panel.height - headerH - 9f)

        val exit = closeProgress.coerceIn(0f, 1f)
        renderer.withOpacity(1f - exit * 0.35f) {
            scrim(renderer, w, h)
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
                panelChrome(renderer, panel, sidebarW)
                drawSidebar(renderer, sidebar)
                drawHeader(renderer, header)
                drawRows(renderer, content)
                drawPanelBorder(renderer, panel)
                drawOpenSelect(renderer)
                drawContextMenu(renderer)
                colorEditing?.let { drawColorModal(renderer, it) }
                if (contextMenuEntry == null) hoverTooltip?.let { drawTooltip(renderer, it.first, it.second) }
            }
        }
    }

    private fun scrim(r: Renderer2D, w: Float, h: Float) {
        r.fillRect(Rect(0f, 0f, w, h), Color.of(0f, 0f, 0f, 0.62f))
    }

    private fun panelChrome(r: Renderer2D, rect: Rect, sidebarW: Float) {
        val radius = CornerRadius.uniform(10f)
        val opacity = IqNanoGlobalConfigScreen.getSharedGuiOpacity().toFloat().coerceIn(0.35f, 1f)
        r.backend.fillRect(
            rect,
            Paint.LinearGradient(
                Point(rect.x, rect.y),
                Point(rect.right, rect.bottom),
                iqPanelStart().withAlpha((opacity * 0.92f).coerceIn(0.35f, 0.98f)),
                iqPanelEnd().withAlpha((opacity * 0.98f).coerceIn(0.42f, 1.0f)),
            ),
            radius,
        )
        drawPanelBackground(r, rect, radius)
        r.fillRect(Rect(rect.x + sidebarW, rect.y + 1f, 1f, rect.height - 2f), Color.of(1f, 1f, 1f, 0.10f))
        r.fillRect(Rect(rect.x + sidebarW, rect.y + 45f, rect.width - sidebarW, 1f), iqAccent().withAlpha(0.60f))
        r.fillRect(Rect(rect.x + 1f, rect.y + 45f, sidebarW - 1f, 1f), Color.of(1f, 1f, 1f, 0.10f))
    }

    private fun drawPanelBorder(r: Renderer2D, rect: Rect) {
        val radius = CornerRadius.uniform(10f)
        if (IqNanoGlobalConfigScreen.isSharedOutlineShadowEnabled()) {
            drawAnimatedBorder(r, rect)
        } else {
            r.strokeRect(rect, iqBorder().withAlpha(0.82f), 1.45f, radius)
        }
    }

    private fun drawPanelBackground(r: Renderer2D, rect: Rect, radius: CornerRadius) {
        val path = IqNanoGlobalConfigScreen.brandBackgroundPath() ?: return
        val tex = IqNanoVg.textures.textureFor(path) ?: return
        r.withClip(rect) {
            r.withOpacity(0.18f) {
                r.backend.drawImage(tex, rect, null, radius)
            }
            r.fillRect(rect, iqPanelEnd().withAlpha(0.22f), radius)
        }
    }

    private fun drawSidebar(r: Renderer2D, rect: Rect) {
        val tex = IqNanoVg.textures.textureFor(IqNanoGlobalConfigScreen.brandIconPath())
        val headerCenterY = rect.y + 24f
        val logo = Rect(rect.x + 16f, headerCenterY - 13f, 26f, 26f)
        if (tex != null) r.backend.drawImage(tex, logo, null, CornerRadius.uniform(7f))
        val logoPulse = if (IqNanoGlobalConfigScreen.isSharedAnimationsEnabled()) pulse(0.10f, 1.0f) else 0f
        r.strokeRect(logo.inset(-2.0f), brandDetailAccent().withAlpha(0.22f + logoPulse), 1.45f, CornerRadius.uniform(8.7f))
        r.strokeRect(logo.inset(-0.8f), Color.fromHex("#FFFFFF").withAlpha(0.10f), 0.75f, CornerRadius.uniform(7.6f))
        if (logo.inset(-2f).contains(mouseXf, mouseYf)) hoverTooltip = logo to IqNanoGlobalConfigScreen.brandTooltip()
        val brandX = logo.right + 8f
        drawBrandTitle(r, IqNanoGlobalConfigScreen.brandName(), brandX, headerCenterY - 12.8f, 15.6f)
        r.text("CONFIG", brandX + 0.4f, headerCenterY + 3.7f, 8.4f, brandSubtitleColor(), fontSemi())

        val iconSize = 15f
        val iconGap = 4f
        val iconY = headerCenterY - iconSize / 2f
        val groupW = iconSize * 3f + iconGap * 2f
        val groupX = rect.right - 7f - groupW
        socialTextureButton(r, Rect(groupX, iconY, iconSize, iconSize), "discord", "/assets/iq/textures/social/discord.png", "Discord: support, suggestions and development updates") { openUrl("https://discord.com/invite/25aaMJMGMc") }
        socialTextureButton(r, Rect(groupX + iconSize + iconGap, iconY, iconSize, iconSize), "modrinth", "/assets/iq/textures/social/modrinth.png", "Modrinth: download the latest ${IqNanoGlobalConfigScreen.brandName()} versions") { openUrl("https://modrinth.com/mod/iq-addons") }
        socialTextureButton(r, Rect(groupX + (iconSize + iconGap) * 2f, iconY, iconSize, iconSize), "patreon", "/assets/iq/textures/social/patreon.png", "Patreon: support ${IqNanoGlobalConfigScreen.brandName()} and get the exclusive version") { openUrl("https://patreon.com/IQAddons") }

        val listTop = rect.y + 54f
        val listBottom = (rect.bottom - 76f).coerceAtLeast(listTop + 24f).coerceAtMost(rect.bottom - 24f)
        val list = Rect(rect.x + 1f, listTop, rect.width - 2f, listBottom - listTop)
        var y = list.y - sidebarScroll
        r.withClip(list) {
        categories.forEachIndexed { index, category ->
            val row = Rect(rect.x + 11f, y, rect.width - 22f, 19f)
            if (row.bottom >= list.y && row.y <= list.bottom) sidebarHits += SidebarHit(index, row)
            val active = index == selectedCategory
            val hover = row.contains(mouseXf, mouseYf) && !isColorModalOpen()
            val anim = categoryAnim[category.id()] ?: if (active) 1f else 0f
            if (anim > 0.01f || hover) {
                val c = iqAccent().withAlpha(0.12f + 0.28f * anim + if (hover) 0.04f else 0f)
                r.fillRect(row, c, CornerRadius.uniform(4f))
                r.fillRect(Rect(row.x, row.y, 3.5f + 2f * anim, row.height), iqAccent().withAlpha(0.35f + 0.50f * anim), CornerRadius.uniform(2f))
                r.strokeRect(row, iqAccent().withAlpha(0.12f + 0.30f * anim), 1f, CornerRadius.uniform(4f))
            }
            r.text(displayLabel(category.name()), row.x + 10f + 2f * anim, row.y + 6.0f, 9.2f, if (anim > 0.5f) iqText() else iqMuted(), fontSemi())
            y += 21.5f
            if (shouldDrawCategoryDivider(index)) {
                r.fillRect(Rect(row.x + 1f, y + 1.5f, row.width - 2f, 1f), Color.of(1f, 1f, 1f, 0.14f))
                y += 7.5f
            }
        }
        }
        maxSidebarScroll = (y + sidebarScroll - list.bottom + 2f).coerceAtLeast(0f)
        sidebarScroll = sidebarScroll.coerceIn(0f, maxSidebarScroll)
        if (layoutScale < 0.999f && maxSidebarScroll > 0f) {
            val track = Rect(rect.right - 8f, list.y + 1f, 2f, list.height - 2f)
            val thumbH = max(18f, track.height * (track.height / (track.height + maxSidebarScroll)))
            val thumbY = track.y + (track.height - thumbH) * (sidebarScroll / maxSidebarScroll)
            r.fillRect(track, Color.of(1f, 1f, 1f, 0.06f), CornerRadius.uniform(2f))
            r.fillRect(Rect(track.x, thumbY, track.width, thumbH), iqAccent().withAlpha(0.92f), CornerRadius.uniform(2f))
        }
        val edit = Rect(rect.x + 18f, rect.bottom - 48f, rect.width - 36f, 22f)
        hudEditorButton(r, edit) { net.iqaddons.mod.hud.HudManager.get().openEditor(this) }
        r.text("MODRINTH VERSION v${modVersion()}", rect.centerX, rect.bottom - 14f, 8.5f, Color.of(1f, 1f, 1f, 0.14f), fontRegular(), TextAlign.CENTER)
    }

    private fun drawHeader(r: Renderer2D, rect: Rect) {
        val searchW = min(168f, rect.width * 0.36f)
        val search = Rect(rect.x + (rect.width - searchW) / 2f - 4f, rect.y + 14f, searchW, 18f)
        val searchHover = search.contains(mouseXf, mouseYf) || searchFocused
        r.fillRect(search, iqModalSurface().withAlpha(0.72f + 0.20f * searchFocusAnim + if (searchHover) 0.04f else 0f), CornerRadius.uniform(4f))
        r.strokeRect(search, iqBorder().withAlpha(0.22f + 0.50f * searchFocusAnim + if (searchHover) 0.08f else 0f), 1f + 0.4f * searchFocusAnim, CornerRadius.uniform(4f))
        if (searchFocusAnim > 0.02f) r.strokeRect(search.inset(-2f - pulse(1.5f, 1.5f)), iqAccent().withAlpha(0.08f * searchFocusAnim), 1f, CornerRadius.uniform(6f))
        r.icon("search", Rect(search.x + 8f, search.y + 5.2f, 7.2f, 7.2f), iqMuted().lerp(iqAccent(), searchFocusAnim))
        val q = query.toString()
        val placeholderAlpha = if (searchFocused && q.isBlank()) 0.42f + 0.34f * ((sin(time * 5.2f) + 1f) * 0.5f) else 1f
        val clearVisible = searchFocused
        val clearButton = Rect(search.right - 17f, search.y + 3f, 12f, 12f)
        val textMax = if (clearVisible) search.width - 43f else search.width - 29f
        r.text(fitText(r, if (q.isBlank()) "Search..." else q, textMax, 9.2f, 7.0f, fontMedium()), search.x + 21f, search.y + 4.4f, 9.2f, (if (q.isBlank()) iqDisabled() else iqText()).withAlpha(placeholderAlpha), fontMedium())
        actions += ActionHit("search", search) { searchFocused = true }
        if (clearVisible) {
            val clearHover = clearButton.contains(mouseXf, mouseYf)
            r.fillRect(clearButton.inset(-1f), iqButtonSurface().withAlpha(if (clearHover) 0.72f else 0.36f), CornerRadius.uniform(4f))
            r.strokeRect(clearButton.inset(-1f), iqAccent().withAlpha(if (clearHover) 0.48f else 0.18f), 1f, CornerRadius.uniform(4f))
            r.icon("trash", clearButton.inset(2.4f), iqMuted().lerp(iqText(), if (clearHover) 1f else 0f))
            actions += ActionHit("search-clear", clearButton.inset(-3f)) {
                query.clear()
                scroll = 0f
                openSelect = null
                searchFocused = true
            }
        }

        socialTextureButton(r, Rect(rect.right - 52f, rect.y + 14f, 18f, 18f), "settings", "/assets/iq/textures/social/settings.png", "Open Config Hub") {
            saveUiSession()
            minecraft?.gui?.setScreen(IqNanoGlobalConfigScreen(this, *effectiveConfigClasses()))
        }
        iconButton(r, Rect(rect.right - 29f, rect.y + 14f, 18f, 18f), "close", "Close ${IqNanoGlobalConfigScreen.brandName()} Config") { onClose() }
    }

    private fun drawRows(r: Renderer2D, rect: Rect) {
        val entries = if (query.toString().trim().isBlank()) {
            val category = categories.getOrNull(selectedCategory) ?: return
            visibleEntries(category.entries())
        } else {
            visibleGlobalEntries()
        }
        val rowGap = 5.5f
        var y = rect.y - scroll
        var pendingTargetOffset: Float? = null
        val clip = Rect(rect.x, rect.y, rect.width, rect.height)
        r.withClip(clip) {
            entries.forEach { pair ->
                val entry = pair.entry
                val depth = pair.depth
                val child = depth > 0
                val indent = (depth * 18f).coerceAtMost(42f)
                val baseH = when (entryType(entry)) {
                    EntryType.SEPARATOR -> 16f
                    EntryType.SECTION_HEADER -> 23f
                    else -> entryHeight(r, entry, rect.width - 26f - indent, child)
                }
                val h = (baseH * pair.expansion).coerceAtLeast(if (pair.expansion > 0.02f) 2f else 0f)
                if (h > 0f && pair.expansion > 0.03f && y + h >= rect.y && y <= rect.bottom) {
                    drawEntry(r, entry, Rect(rect.x + indent, y, rect.width - 18f - indent, h), depth, pair.expansion)
                }
                val target = initialEntryLabel
                if (target != null && pendingTargetOffset == null && displayLabel(entryLabel(entry)).equals(target, ignoreCase = true)) {
                    pendingTargetOffset = y + scroll - rect.y
                }
                y += h + rowGap * if (pair.expansion > 0.02f) 1f else 0f
            }
        }
        maxScroll = (y + scroll + 4f - rect.y - rect.height).coerceAtLeast(0f)
        pendingTargetOffset?.let { offset ->
            scroll = (offset - 8f).coerceIn(0f, maxScroll)
            initialEntryLabel = null
        }
        scroll = scroll.coerceIn(0f, maxScroll)
        if (maxScroll > 0f) {
            val track = Rect(rect.right - 5f, rect.y, 3f, rect.height)
            val thumbH = max(34f, rect.height * (rect.height / (rect.height + maxScroll)))
            val thumbY = rect.y + (rect.height - thumbH) * (scroll / maxScroll)
            r.fillRect(track, Color.of(1f, 1f, 1f, 0.06f), CornerRadius.uniform(2f))
            val thumb = Rect(track.x, thumbY, track.width, thumbH)
            r.fillRect(thumb, iqAccent(), CornerRadius.uniform(2f))
            scrollbarHit = ScrollbarHit(track.inset(-5f), thumb.inset(-5f))
        }
    }

    private fun drawEntry(r: Renderer2D, e: ConfigEntryModel, rect: Rect, depth: Int, visible: Float = 1f) {
        if (isProfitDisplayOptions(e)) {
            drawProfitDisplayOptions(r, e, rect, visible)
            return
        }
        if (entryType(e) == EntryType.SEPARATOR) {
            separatorLabel(e)?.takeIf { it.isNotBlank() }?.let {
                val label = displayLabel(it).uppercase()
                val labelSize = 6.9f
                val w = r.textWidth(label, labelSize, fontBold()) + 15f
                val lineY = rect.centerY
                val pill = Rect(rect.x + (rect.width - w) / 2f, rect.y + 2.5f, w, 10.5f)
                val sideW = ((rect.width - w) / 2f - 7f).coerceAtLeast(0f)
                val left = Rect(rect.x, lineY, sideW, 1f)
                val right = Rect(pill.right + 7f, lineY, sideW, 1f)
                if (sideW > 1f) {
                    r.backend.fillRect(left, Paint.LinearGradient(Point(left.x, left.y), Point(left.right, left.y), Color.of(1f, 1f, 1f, 0.02f), iqAccent().withAlpha(0.18f)), CornerRadius.ZERO)
                    r.backend.fillRect(right, Paint.LinearGradient(Point(right.x, right.y), Point(right.right, right.y), iqAccent().withAlpha(0.18f), Color.of(1f, 1f, 1f, 0.02f)), CornerRadius.ZERO)
                }
                r.fillRect(pill, iqButtonSurface().withAlpha(0.76f), CornerRadius.uniform(3f))
                r.strokeRect(pill, iqAccent().withAlpha(0.24f), 1f, CornerRadius.uniform(3f))
                r.text(label, pill.centerX, pill.y + 2.2f, labelSize, iqDisabled(), fontBold(), TextAlign.CENTER)
            }
            return
        }

        val hover = rect.contains(mouseXf, mouseYf) && !isMouseOverSelectDropdown() && !isColorModalOpen()
        rows += RowHit(e, rect, depth)
        val hoverAnim = rowHoverAnim[entryKey(e)] ?: if (hover) 1f else 0f
        val type = entryType(e)
        val child = depth > 0
        val compact = child || type == EntryType.SECTION_HEADER
        val bg = when {
            type == EntryType.SECTION_HEADER -> iqButtonSurface().withAlpha(0.56f + 0.08f * hoverAnim)
            child -> iqSurface().lerp(iqButtonSurface(), 0.55f).withAlpha(0.62f)
            else -> iqSurface().withAlpha(0.66f)
        }
        val radius = if (type == EntryType.SECTION_HEADER) 4f else 5f
        r.withOpacity(visible.coerceIn(0f, 1f)) {
        r.fillRect(rect, bg, CornerRadius.uniform(radius))
        if (hoverAnim > 0.01f) {
            val glow = hoverAnim * hoverAnim
            val sweep = Rect(rect.x, rect.y, min(rect.width, 210f), rect.height)
            r.backend.fillRect(
                sweep,
                Paint.LinearGradient(
                    Point(sweep.x, sweep.centerY),
                    Point(sweep.right, sweep.centerY),
                    iqAccent().withAlpha(0.16f * glow),
                    iqAccent().withAlpha(0.0f),
                ),
                CornerRadius.uniform(radius),
            )
            r.fillRect(Rect(rect.x, rect.y + 5f, 2.2f + 1.2f * glow, rect.height - 10f), iqAccent().withAlpha(0.36f + 0.42f * glow), CornerRadius.uniform(2f))
        }
        r.strokeRect(rect, iqBorder().withAlpha(0.13f + 0.22f * hoverAnim), 1f, CornerRadius.uniform(radius))
        if (hoverAnim > 0.01f) r.strokeRect(rect.inset(-0.8f), iqAccentGlow().withAlpha(0.055f * hoverAnim), 0.8f, CornerRadius.uniform(radius + 1f))
        if (type == EntryType.SECTION_HEADER) r.fillRect(Rect(rect.x, rect.y + 4f, 3f, rect.height - 8f), iqAccent().withAlpha(0.72f + 0.10f * hoverAnim), CornerRadius.uniform(2f))

        val titleX = rect.x + if (child) 13f else 16f
        val textMaxWidth = textColumnWidth(e, rect)
        val title = displayLabel(entryLabel(e))
        val rawDescription = e.getDescription()
        val descriptionSize = 7.7f
        val descriptionLineH = 8.3f
        val descriptionLines = rawDescription
            ?.takeIf { it.isNotBlank() }
            ?.takeUnless { compact }
            ?.let { wrapDescription(r, stripCodes(it), textMaxWidth, descriptionSize, fontRegular(), 4) }
            .orEmpty()
        if (hover && compact && !rawDescription.isNullOrBlank()) {
            hoverTooltip = rect to stripCodes(rawDescription)
        }
        val titleSize = if (compact) 8.1f else 9.0f
        val blockHeight = titleSize + if (descriptionLines.isNotEmpty()) 3.1f + descriptionLines.size * descriptionLineH else 0f
        var textY = rect.centerY - blockHeight / 2f + 0.4f
        r.text(title, titleX, textY, titleSize, iqText(), fontSemi())
        textY += if (compact) 10.0f else 12.4f
        descriptionLines.forEach {
            r.text(it, titleX, textY, descriptionSize, iqMuted(), fontRegular())
            textY += descriptionLineH
        }

        when (type) {
            EntryType.BOOLEAN -> {
                if (isBuildProgressStyle(e)) {
                    drawBuildProgressStyleSelector(r, e, centeredActionRect(rect, 101f, 16f))
                } else {
                    drawToggle(r, e, centeredActionRect(rect, 28f, 13.5f))
                }
            }
            EntryType.INT_SLIDER, EntryType.FLOAT_SLIDER, EntryType.DOUBLE_SLIDER -> {
                val (valueBox, bar) = sliderRects(rect, e)
                drawSlider(r, e, valueBox, bar)
                numberInputHits += NumberInputHit(e, valueBox)
                sliderHits += SliderHit(e, bar.inset(-5f))
            }
            EntryType.SELECT -> {
                val selectText = selectValueLabel(e, fieldValue(entryField(e)))
                val selectW = (r.textWidth(selectText, 6.9f, fontSemi()) + 27f).coerceIn(68f, 140f)
                drawSelect(r, e, centeredActionRect(rect, selectW, 16f))
            }
            EntryType.BUTTON -> drawSmallButton(r, centeredActionRect(rect, 45f, 14.5f), buttonText(e) ?: "Open")
            EntryType.COLOR -> drawColorSwatch(r, e, centeredActionRect(rect, 36f, 15f))
            EntryType.SECTION_HEADER -> {
                drawConfigBadge(r, centeredActionRect(rect, 53f, 13.5f, centerOffset = -16f), "${configChildCount(e)} configs")
                val progress = sectionAnim[sectionKey(e)] ?: if (e.isExpanded) 1f else 0f
                r.withTransform(rect.right - 21.5f, rect.centerY, 1f, 1f) {
                    r.backend.rotate(progress * (PI.toFloat() / 2f))
                    r.icon("chevron-right", Rect(-6f, -6f, 12f, 12f), iqText())
                }
            }
            EntryType.UNSUPPORTED -> r.text("Unsupported", rect.right - 84f, rect.centerY - 4f, 9f, iqDisabled(), fontRegular())
            else -> {}
        }
        }
    }

    private fun isMouseInContent(): Boolean =
        content.contains(mouseXf, mouseYf)

    private fun drawToggle(r: Renderer2D, e: ConfigEntryModel, rect: Rect) {
        val on = fieldValue(entryField(e)) as? Boolean == true
        val t = toggleAnim[entryKey(e)] ?: if (on) 1f else 0f
        r.fillRect(rect, iqToggleOff().lerp(iqAccent(), t), CornerRadius.uniform(rect.height / 2f))
        r.strokeRect(rect, iqBorder().withAlpha(0.55f), 1f, CornerRadius.uniform(rect.height / 2f))
        val knobSize = rect.height - 3f
        val knobInset = 1.5f
        val knobX = rect.x + knobInset + (rect.width - knobSize - knobInset * 2f) * t
        val knob = Rect(knobX, rect.y + knobInset, knobSize, knobSize)
        r.fillRect(knob, iqKnob(), CornerRadius.uniform(knobSize / 2f))
    }

    private fun drawBuildProgressStyleSelector(r: Renderer2D, e: ConfigEntryModel, rect: Rect) {
        val simple = fieldValue(entryField(e)) as? Boolean == true
        val hover = rect.contains(mouseXf, mouseYf)
        r.fillRect(rect, iqButtonSurface().withAlpha(if (hover) 0.92f else 0.76f), CornerRadius.uniform(6f))
        r.strokeRect(rect, iqAccent().withAlpha(if (hover) 0.46f else 0.24f), 1f, CornerRadius.uniform(6f))
        val selected = if (simple) Rect(rect.x + 1.6f, rect.y + 1.6f, rect.width / 2f - 2.6f, rect.height - 3.2f)
            else Rect(rect.centerX + 1f, rect.y + 1.6f, rect.width / 2f - 2.6f, rect.height - 3.2f)
        r.fillRect(selected, iqAccent().withAlpha(0.88f), CornerRadius.uniform(5f))
        r.text("SIMPLE", rect.x + rect.width * 0.25f, rect.y + 4.6f, 6.7f, if (simple) iqKnob() else iqMuted(), fontBold(), TextAlign.CENTER)
        r.text("NORMAL", rect.x + rect.width * 0.75f, rect.y + 4.6f, 6.7f, if (!simple) iqKnob() else iqMuted(), fontBold(), TextAlign.CENTER)
    }

    private fun drawProfitDisplayOptions(r: Renderer2D, e: ConfigEntryModel, rect: Rect, visible: Float) {
        rows += RowHit(e, Rect(rect.x, rect.y, rect.width, 25f), 1)
        val hover = Rect(rect.x, rect.y, rect.width, 25f).contains(mouseXf, mouseYf)
        val progress = approach(sectionAnim["profit-display-options"] ?: if (profitDisplayExpanded) 1f else 0f, if (profitDisplayExpanded) 1f else 0f, 0.22f)
        sectionAnim["profit-display-options"] = progress

        r.withOpacity(visible.coerceIn(0f, 1f)) {
            val header = Rect(rect.x, rect.y, rect.width, 25f)
            r.fillRect(header, iqButtonSurface().withAlpha(if (hover) 0.74f else 0.58f), CornerRadius.uniform(4f))
            r.strokeRect(header, iqAccent().withAlpha(if (hover) 0.34f else 0.20f), 1f, CornerRadius.uniform(4f))
            r.fillRect(Rect(header.x, header.y + 4f, 3f, header.height - 8f), iqAccent().withAlpha(0.82f), CornerRadius.uniform(2f))
            r.text("Display Options", header.x + 16f, header.y + 7.2f, 8.8f, iqText(), fontSemi())
            if (hover) {
                hoverTooltip = header to "Choose Profit Tracker lines, drag between Active and Inactive, and reorder the widget display."
            }
            drawConfigBadge(r, centeredActionRect(header, 58f, 15f, centerOffset = -16f), "${profitActiveLines.size} active")
            r.withTransform(header.right - 21.5f, header.centerY, 1f, 1f) {
                r.backend.rotate(progress * (PI.toFloat() / 2f))
                r.icon("chevron-right", Rect(-6.5f, -6.5f, 13f, 13f), iqText())
            }

            if (progress <= 0.02f) return@withOpacity
            val body = Rect(rect.x + 9f, rect.y + 31f, rect.width - 18f, rect.height - 35f)
            profitPanelRect = body
            r.withOpacity(progress) {
                val trash = Rect(body.right - 25f, body.y + 3f, 19f, 17f)
                profitTrashRect = trash.inset(-5f)
                val trashHover = trash.contains(mouseXf, mouseYf) || profitTrashRect?.contains(mouseXf, mouseYf) == true && profitDraggingLine != null
                r.text("Active", body.x + 2f, body.y + 6f, 8f, iqMuted(), fontSemi())
                r.text("drag down to hide", body.x + 45f, body.y + 6f, 7.2f, iqDisabled(), fontRegular())
                r.fillRect(trash, (if (trashHover) iqButtonSurface().lerp(iqAccent(), 0.16f) else iqButtonSurface()).withAlpha(0.78f), CornerRadius.uniform(4f))
                r.strokeRect(trash, iqAccent().withAlpha(if (trashHover) 0.70f else 0.24f), 1f, CornerRadius.uniform(4f))
                r.icon("trash", trash.inset(4f), if (trashHover) iqText() else iqMuted())

                var y = body.y + 25f
                profitActiveLines.forEach { line ->
                    if (line != profitDraggingLine) {
                        drawProfitDisplayLine(r, line, true, Rect(body.x, y, body.width, 17f))
                    } else {
                        r.fillRect(Rect(body.x, y + 7f, body.width, 1f), iqAccent().withAlpha(0.42f), CornerRadius.ZERO)
                    }
                    y += 19f
                }
                val inactiveStart = y + 8f
                profitActiveDropRect = Rect(body.x, body.y, body.width, inactiveStart - body.y - 2f)
                r.fillRect(Rect(body.x, inactiveStart - 4f, body.width, 1f), Color.of(1f, 1f, 1f, 0.08f))
                r.text("Inactive", body.x + 2f, inactiveStart + 7f, 8f, iqMuted(), fontSemi())
                r.text("drag up or click +", body.x + 51f, inactiveStart + 7f, 7.2f, iqDisabled(), fontRegular())
                y = inactiveStart + 25f
                profitInactiveDropRect = Rect(body.x, inactiveStart - 4f, body.width, body.bottom - inactiveStart + 4f)
                if (profitInactiveLines.isEmpty()) {
                    r.text("All lines are active", body.x + 10f, y + 4f, 7.8f, iqDisabled(), fontRegular())
                } else {
                    profitInactiveLines.forEach { line ->
                        drawProfitDisplayLine(r, line, false, Rect(body.x, y, body.width, 17f))
                        y += 19f
                    }
                }
                profitDraggingLine?.let { line ->
                    drawProfitDisplayLine(r, line, true, Rect(mouseXf - body.width / 2f, mouseYf - 8.5f, body.width, 17f), ghost = true)
                }
            }
        }
    }

    private fun drawProfitDisplayLine(r: Renderer2D, line: ProfitTrackerDisplayLine, active: Boolean, rect: Rect, ghost: Boolean = false) {
        val hover = rect.contains(mouseXf, mouseYf)
        val bg = when {
            ghost -> iqSurface().lerp(iqAccent(), 0.18f).withAlpha(0.84f)
            hover -> iqSurface().lerp(iqAccent(), 0.12f).withAlpha(0.80f)
            active -> iqSurface().withAlpha(0.76f)
            else -> iqModalSurface().withAlpha(0.66f)
        }
        r.fillRect(rect, bg, CornerRadius.uniform(4f))
        r.strokeRect(rect, iqBorder().withAlpha(if (hover || ghost) 0.46f else 0.20f), 1f, CornerRadius.uniform(4f))
        r.text("::", rect.x + 8f, rect.y + 4.1f, 7.8f, iqDisabled(), fontBold())
        r.text(line.displayName(), rect.x + 24f, rect.y + 4.1f, 8.2f, if (active) iqText() else iqMuted(), fontSemi())
        if (active) {
            r.text("ON", rect.right - 23f, rect.y + 4.1f, 7.8f, iqAccentHot(), fontBold())
        } else {
            val add = Rect(rect.right - 22f, rect.y + 2f, 13f, 13f)
            r.fillRect(add, iqButtonSurface().withAlpha(if (add.contains(mouseXf, mouseYf)) 0.92f else 0.72f), CornerRadius.uniform(4f))
            r.strokeRect(add, iqAccent().withAlpha(0.34f), 1f, CornerRadius.uniform(4f))
            r.icon("plus", add.inset(3f), iqText())
            if (!ghost) profitAddHits += ProfitAddHit(line, add.inset(-4f))
        }
        if (!ghost) profitLineHits += ProfitLineHit(line, active, rect)
    }

    private fun drawSlider(r: Renderer2D, e: ConfigEntryModel, valueRect: Rect, rect: Rect) {
        val v = (fieldValue(entryField(e)) as? Number)?.toDouble() ?: rangeMin(e)
        val t = ((v - rangeMin(e)) / (rangeMax(e) - rangeMin(e))).toFloat().coerceIn(0f, 1f)
        val editing = editingNumber == e
        val valueHover = valueRect.contains(mouseXf, mouseYf)
        val valueText = if (editing) {
            val caret = ((time * 2.2f).toInt() % 2 == 0)
            editingNumberText.toString() + if (caret) "|" else ""
        } else {
            formatNumericValue(e, v)
        }
        r.fillRect(valueRect, (if (editing) iqSurface().lerp(iqAccent(), 0.08f) else iqButtonSurface()).withAlpha(if (valueHover || editing) 0.92f else 0.76f), CornerRadius.uniform(4f))
        r.strokeRect(valueRect, iqAccent().withAlpha(if (editing) 0.58f else if (valueHover) 0.38f else 0.18f), 1f, CornerRadius.uniform(4f))
        r.text(fitText(r, valueText, valueRect.width - 7f, 6.9f, 5.8f, fontSemi()), valueRect.centerX, valueRect.y + 4.2f, 6.9f, iqText(), fontSemi(), TextAlign.CENTER)

        r.fillRect(rect, sliderTrack(), CornerRadius.uniform(5f))
        r.fillRect(Rect(rect.x, rect.y, rect.width * t, rect.height), iqAccent(), CornerRadius.uniform(5f))
        r.fillRect(Rect(rect.x + rect.width * t - 2.5f, rect.y - 1.5f, 5f, rect.height + 3f), iqKnob(), CornerRadius.uniform(2.5f))
    }

    private fun drawSelect(r: Renderer2D, e: ConfigEntryModel, rect: Rect) {
        val hover = rect.contains(mouseXf, mouseYf)
        val active = openSelect == e
        r.fillRect(rect, iqButtonSurface().withAlpha(if (hover || active) 0.94f else 0.78f), CornerRadius.uniform(4f))
        r.strokeRect(rect, iqAccent().withAlpha(if (active) 0.58f else if (hover) 0.42f else 0.18f), 1f, CornerRadius.uniform(4f))
        r.text(displayLabel(fieldValue(entryField(e))?.toString().orEmpty()), rect.centerX - 4f, rect.y + 4.2f, 6.9f, iqText(), fontSemi(), TextAlign.CENTER)
        val progress = selectAnim[entryKey(e)] ?: if (active) 1f else 0f
        r.withTransform(rect.right - 9.5f, rect.centerY, 1f, 1f) {
            r.backend.rotate(progress * (PI.toFloat() / 2f))
            r.icon("chevron-right", Rect(-4f, -4f, 8f, 8f), iqMuted())
        }
        selectHits += SelectHit(e, rect)
    }

    private fun drawOpenSelect(r: Renderer2D) {
        val entry = openSelect ?: run {
            selectDropdownRect = null
            return
        }
        val anchor = selectHits.lastOrNull { it.entry == entry }?.rect ?: return
        val values = enumValues(entry)?.toList().orEmpty()
        if (values.isEmpty()) return
        val progress = (selectAnim[entryKey(entry)] ?: 0f).coerceIn(0f, 1f)
        if (progress <= 0.02f) return

        val optionH = 18f
        val visibleCount = values.size.coerceAtMost(6)
        val key = entryKey(entry)
        val maxStart = (values.size - visibleCount).coerceAtLeast(0)
        val startIndex = (selectScroll[key] ?: 0).coerceIn(0, maxStart).also { selectScroll[key] = it }
        val visibleValues = values.drop(startIndex).take(visibleCount)
        val scrollable = maxStart > 0
        val indicatorH = if (scrollable) 10f else 0f
        val dropdownH = 8f + indicatorH * 2f + optionH * visibleCount
        val width = values.maxOf {
            r.textWidth(selectValueLabel(entry, it), 7.4f, fontSemi()) + 28f
        }.coerceAtLeast(anchor.width).coerceAtMost(180f)
        val x = (anchor.right - width).coerceIn(content.x, content.right - width - 12f)
        val openDown = anchor.bottom + dropdownH + 8f <= content.bottom
        val fullY = if (openDown) anchor.bottom + 4f else anchor.y - dropdownH - 4f
        val animatedY = fullY + if (openDown) (1f - progress) * -6f else (1f - progress) * 6f
        val rect = Rect(x, animatedY, width, dropdownH * progress)
        selectDropdownRect = Rect(x, fullY, width, dropdownH)

        r.withOpacity(progress) {
            r.backend.boxShadow(Rect(x, fullY, width, dropdownH), CornerRadius.uniform(5f), 14f, -3f, Color.of(0f, 0f, 0f, 0.70f))
            r.fillRect(rect, iqModalSurface().withAlpha(0.98f), CornerRadius.uniform(5f))
            r.strokeRect(rect, iqAccent().withAlpha(0.46f), 1f, CornerRadius.uniform(5f))
            r.withClip(rect.inset(1f)) {
                if (startIndex > 0) {
                    r.icon("chevron-up", Rect(rect.centerX - 4f, fullY + 3f, 8f, 8f), iqMuted())
                }
                visibleValues.forEachIndexed { index, value ->
                    val row = Rect(x + 4f, fullY + 4f + indicatorH + index * optionH, width - 8f, optionH - 2f)
                    val selected = fieldValue(entryField(entry)) == value
                    val hover = row.contains(mouseXf, mouseYf)
                    if (selected || hover) {
                        r.fillRect(row, iqAccent().withAlpha(if (selected) 0.26f else 0.14f), CornerRadius.uniform(4f))
                    }
                    r.text(selectValueLabel(entry, value), row.x + 7f, row.y + 4.2f, 7.4f, if (selected) iqText() else iqMuted(), fontSemi())
                    if (selected) r.icon("check", Rect(row.right - 14f, row.centerY - 4.5f, 9f, 9f), iqText())
                    if (hover) enumDescription(entry, value)?.takeIf { it.isNotBlank() }?.let {
                        hoverTooltip = row to stripCodes(it)
                    }
                    selectOptionHits += SelectOptionHit(entry, value, row)
                }
                if (startIndex < maxStart) {
                    r.icon("chevron-down", Rect(rect.centerX - 4f, fullY + dropdownH - 11f, 8f, 8f), iqMuted())
                }
            }
        }
    }

    private fun isMouseOverSelectDropdown(): Boolean =
        selectDropdownRect?.contains(mouseXf, mouseYf) == true

    private fun drawContextMenu(r: Renderer2D) {
        val entry = contextMenuEntry ?: run {
            contextMenuRect = null
            return
        }
        if (time - contextMenuOpenedAt > 3f || time < contextMenuOpenedAt) {
            contextMenuEntry = null
            contextMenuRect = null
            contextMenuHit = null
            return
        }
        if (isColorModalOpen()) {
            contextMenuRect = null
            contextMenuHit = null
            return
        }

        val label = if (entryType(entry) == EntryType.SECTION_HEADER) "Restore section" else "Restore default"
        val menuW = max(98f, r.textWidth(label, 7.8f, fontSemi()) + 30f)
        val menuH = 20f
        val anchor = contextMenuRect ?: Rect(mouseXf, mouseYf, 1f, 1f)
        val preferredX = anchor.right - menuW - 28f
        val preferredY = if (anchor.y - menuH - 4f >= content.y) anchor.y - menuH - 4f else anchor.bottom + 4f
        val x = preferredX.coerceIn(panel.x + 6f, panel.right - menuW - 6f)
        val y = preferredY.coerceIn(panel.y + 6f, panel.bottom - menuH - 6f)
        val rect = Rect(x, y, menuW, menuH)
        val hover = rect.contains(mouseXf, mouseYf)

        r.backend.boxShadow(rect, CornerRadius.uniform(4f), 10f, -3f, Color.of(0f, 0f, 0f, 0.68f))
        r.fillRect(rect, iqModalSurface().withAlpha(0.95f), CornerRadius.uniform(4f))
        r.strokeRect(rect, iqAccent().withAlpha(if (hover) 0.64f else 0.34f), 1f, CornerRadius.uniform(4f))
        if (hover) r.fillRect(rect.inset(2f), iqAccent().withAlpha(0.13f), CornerRadius.uniform(3f))
        r.icon("rotate-ccw", Rect(rect.x + 8f, rect.centerY - 4f, 8f, 8f), if (hover) iqText() else iqMuted())
        r.text(label, rect.x + 21f, rect.y + 6.4f, 7.8f, if (hover) iqText() else iqMuted(), fontSemi())

        contextMenuHit = ContextMenuHit(rect, entry)
    }

    private fun drawTooltip(r: Renderer2D, source: Rect, text: String) {
        val lines = wrapText(r, text, 210f, 7.8f, fontRegular()).take(3)
        if (lines.isEmpty()) return
        val w = (lines.maxOf { r.textWidth(it, 7.8f, fontRegular()) } + 18f).coerceIn(80f, 230f)
        val h = 13f + lines.size * 9f
        val x = (mouseXf + 12f).coerceAtMost(width.toFloat() - w - 8f)
        val y = if (mouseYf + h + 12f < height) mouseYf + 10f else mouseYf - h - 10f
        val rect = Rect(x, y.coerceAtLeast(8f), w, h)
        r.backend.boxShadow(rect, CornerRadius.uniform(5f), 14f, -3f, Color.of(0f, 0f, 0f, 0.72f))
        r.fillRect(rect, iqModalSurface().withAlpha(0.96f), CornerRadius.uniform(5f))
        r.strokeRect(rect, iqAccent().withAlpha(0.42f), 1f, CornerRadius.uniform(5f))
        var ty = rect.y + 7f
        lines.forEach {
            r.text(it, rect.x + 9f, ty, 7.8f, iqMuted(), fontRegular())
            ty += 9f
        }
    }

    private fun drawSmallButton(r: Renderer2D, rect: Rect, label: String) {
        val hover = rect.contains(mouseXf, mouseYf)
        r.fillRect(rect, iqButtonSurface().withAlpha(if (hover) 0.78f else 0.58f), CornerRadius.uniform(4f))
        r.strokeRect(rect, iqAccent().withAlpha(if (hover) 0.78f else 0.56f), 1f, CornerRadius.uniform(4f))
        r.text(label.uppercase(), rect.centerX, rect.y + 4.25f, 7.0f, iqText(), fontBold(), TextAlign.CENTER)
    }

    private fun drawColorSwatch(r: Renderer2D, e: ConfigEntryModel, rect: Rect) {
        val argb = (fieldValue(entryField(e)) as? Number)?.toInt() ?: 0xFFFFFFFF.toInt()
        r.fillRect(rect, colorFromArgb(argb), CornerRadius.uniform(4f))
        r.strokeRect(rect, Color.fromHex("#FAF1FB").withAlpha(0.68f), 1f, CornerRadius.uniform(4f))
    }

    private fun drawColorModal(r: Renderer2D, e: ConfigEntryModel) {
        val anim = colorModalProgress.coerceIn(0f, 1f)
        val modal = Rect(panel.centerX - 210f, panel.centerY - 155f + (1f - anim) * 10f, 420f, 310f)
        val argb = (fieldValue(entryField(e)) as? Number)?.toInt() ?: 0xFFFFFFFF.toInt()
        val a = ((argb ushr 24) and 255) / 255f
        val rr = ((argb ushr 16) and 255)
        val gg = ((argb ushr 8) and 255)
        val bb = (argb and 255)
        val hsb = java.awt.Color.RGBtoHSB(rr, gg, bb, null)
        val sat = hsb[1]
        val bri = hsb[2]
        val key = entryKey(e)
        val hue = if (sat <= 0.001f || bri <= 0.001f) colorHueMemory[key] ?: hsb[0] else hsb[0].also { colorHueMemory[key] = it }
        val hueColor = colorFromRgb(java.awt.Color.HSBtoRGB(hue, 1f, 1f))
        val current = colorFromArgb(argb)
        val hex = "#%02X%02X%02X".format(rr, gg, bb)

        r.fillRect(Rect(0f, 0f, width.toFloat(), height.toFloat()), Color.of(0f, 0f, 0f, 0.42f * anim))
        actions += ActionHit("color:blocker", Rect(0f, 0f, width.toFloat(), height.toFloat())) {}
        r.fillRect(modal, iqModalSurface().withAlpha(0.97f * anim), CornerRadius.uniform(8f))
        r.strokeRect(modal, iqAccent().withAlpha(0.82f * anim), 1.2f, CornerRadius.uniform(8f))
        r.text("Select Color", modal.x + 18f, modal.y + 16f, 13f, iqText(), fontBold())
        r.text(displayLabel(entryLabel(e)), modal.x + 18f, modal.y + 34f, 9f, iqMuted(), fontRegular())
        iconButton(r, Rect(modal.right - 32f, modal.y + 13f, 18f, 18f), "close", "Close Color Picker") {
            draggingColorTarget = null
            colorEditing = null
        }

        val preview = Rect(modal.right - 108f, modal.y + 42f, 78f, 23f)
        r.fillRect(preview, current, CornerRadius.uniform(5f))
        r.strokeRect(preview, iqAccent().withAlpha(0.42f), 1f, CornerRadius.uniform(5f))
        r.text(hex, preview.centerX, preview.y + 6f, 8.4f, iqText(), fontBold(), TextAlign.CENTER)

        val sv = Rect(modal.x + 18f, modal.y + 82f, modal.width - 36f, 118f)
        r.fillRect(sv, hueColor, CornerRadius.uniform(4f))
        r.backend.fillRect(sv, Paint.LinearGradient(Point(sv.x, sv.y), Point(sv.right, sv.y), Color.fromHex("#FFFFFF"), Color.fromHex("#FFFFFF").withAlpha(0f)), CornerRadius.uniform(4f))
        r.backend.fillRect(sv, Paint.LinearGradient(Point(sv.x, sv.y), Point(sv.x, sv.bottom), Color.fromHex("#000000").withAlpha(0f), Color.fromHex("#000000")), CornerRadius.uniform(4f))
        r.strokeRect(sv, iqBorder().withAlpha(0.62f), 1f, CornerRadius.uniform(4f))
        val svKnob = Point(sv.x + sv.width * sat, sv.y + sv.height * (1f - bri))
        r.strokeRect(Rect(svKnob.x - 5f, svKnob.y - 5f, 10f, 10f), Color.fromHex("#FFFFFF"), 1.6f, CornerRadius.uniform(5f))
        actions += ActionHit("color:sv", sv) {
            draggingColorTarget = "sv"
            updateColorFromPicker(e, "sv", mouseXf, mouseYf, sv)
        }

        val hueBar = Rect(sv.x, sv.bottom + 28f, sv.width, 9f)
        drawHueBar(r, hueBar)
        val hueX = hueBar.x + hueBar.width * hue
        r.strokeRect(Rect(hueX - 4f, hueBar.y - 4f, 8f, 17f), Color.fromHex("#FFFFFF"), 1.5f, CornerRadius.uniform(4f))
        r.text("Hue", hueBar.x, hueBar.y - 14f, 8.5f, iqMuted(), fontMedium())
        actions += ActionHit("color:hue", hueBar.inset(-5f)) {
            draggingColorTarget = "hue"
            updateColorFromPicker(e, "hue", mouseXf, mouseYf, hueBar)
        }

        val alphaBar = Rect(sv.x, hueBar.bottom + 30f, sv.width, 9f)
        r.backend.fillRect(alphaBar, Paint.LinearGradient(Point(alphaBar.x, alphaBar.y), Point(alphaBar.right, alphaBar.y), current.withAlpha(0f), current.withAlpha(1f)), CornerRadius.uniform(4f))
        r.strokeRect(alphaBar, iqBorder().withAlpha(0.52f), 1f, CornerRadius.uniform(4f))
        val alphaX = alphaBar.x + alphaBar.width * a
        r.strokeRect(Rect(alphaX - 4f, alphaBar.y - 4f, 8f, 17f), Color.fromHex("#FFFFFF"), 1.5f, CornerRadius.uniform(4f))
        r.text("Opacity", alphaBar.x, alphaBar.y - 14f, 8.5f, iqMuted(), fontMedium())
        r.text("${(a * 100f).roundToInt()}%", alphaBar.right, alphaBar.y - 14f, 8.5f, iqText(), fontSemi(), TextAlign.RIGHT)
        r.text("RGB $rr, $gg, $bb", modal.centerX, modal.bottom - 18f, 8.2f, iqMuted(), fontRegular(), TextAlign.CENTER)
        actions += ActionHit("color:alpha", alphaBar.inset(-5f)) {
            draggingColorTarget = "alpha"
            updateColorFromPicker(e, "alpha", mouseXf, mouseYf, alphaBar)
        }
    }

    private fun drawHueBar(r: Renderer2D, rect: Rect) {
        val colors = listOf(
            Color.fromHex("#FF0000"),
            Color.fromHex("#FFFF00"),
            Color.fromHex("#00FF00"),
            Color.fromHex("#00FFFF"),
            Color.fromHex("#0000FF"),
            Color.fromHex("#FF00FF"),
            Color.fromHex("#FF0000"),
        )
        val segmentW = rect.width / (colors.size - 1)
        for (i in 0 until colors.lastIndex) {
            val segment = Rect(rect.x + i * segmentW, rect.y, segmentW + 0.5f, rect.height)
            r.backend.fillRect(segment, Paint.LinearGradient(Point(segment.x, segment.y), Point(segment.right, segment.y), colors[i], colors[i + 1]), CornerRadius.ZERO)
        }
        r.strokeRect(rect, Color.fromHex("#FAF1FB").withAlpha(0.30f), 0.8f, CornerRadius.uniform(4f))
    }

    private fun iconButton(r: Renderer2D, rect: Rect, icon: String, tooltip: String? = null, action: () -> Unit) {
        val hover = rect.contains(mouseXf, mouseYf)
        r.fillRect(rect, iqButtonSurface().withAlpha(if (hover) 0.82f else 0.46f), CornerRadius.uniform(4f))
        r.strokeRect(rect, iqAccent().withAlpha(if (hover) 0.78f else 0.42f), 1f, CornerRadius.uniform(4f))
        r.icon(icon, rect.inset(rect.width * 0.27f), iqText())
        if (hover && !tooltip.isNullOrBlank()) hoverTooltip = rect to tooltip
        actions += ActionHit(icon, rect, action)
    }

    private fun socialTextureButton(r: Renderer2D, rect: Rect, id: String, texturePath: String, tooltip: String? = null, action: () -> Unit) {
        val hover = rect.contains(mouseXf, mouseYf)
        r.fillRect(rect, iqButtonSurface().withAlpha(if (hover) 0.76f else 0.42f), CornerRadius.uniform(4f))
        r.strokeRect(rect, iqAccent().withAlpha(if (hover) 0.68f else 0.34f), 1f, CornerRadius.uniform(4f))
        val tex = IqNanoVg.textures.textureFor(texturePath)
        if (tex != null) {
            r.backend.drawImage(tex, rect.inset(rect.width * 0.24f), Color.fromHex("#FFFFFF"), CornerRadius.ZERO)
        } else {
            r.icon(id, rect.inset(rect.width * 0.24f), iqText())
        }
        if (hover && !tooltip.isNullOrBlank()) hoverTooltip = rect to tooltip
        actions += ActionHit(id, rect, action)
    }

    private fun actionButton(r: Renderer2D, rect: Rect, label: String, action: () -> Unit) {
        drawSmallButton(r, rect, label)
        actions += ActionHit(label, rect, action)
    }

    private fun hudEditorButton(r: Renderer2D, rect: Rect, action: () -> Unit) {
        val hover = rect.contains(mouseXf, mouseYf)
        r.fillRect(rect, iqButtonSurface().withAlpha(if (hover) 0.76f else 0.50f), CornerRadius.uniform(7f))
        r.strokeRect(rect, iqAccent().withAlpha(if (hover) 0.78f else 0.58f), 1f, CornerRadius.uniform(7f))
        r.text("HUD EDITOR", rect.centerX, rect.y + 7.4f, 8.2f, iqText(), fontBold(), TextAlign.CENTER)
        actions += ActionHit("hud-editor", rect, action)
    }

    private fun visibleEntries(entries: List<ConfigEntryModel>): List<VisibleEntry> {
        val q = query.toString().trim().lowercase()
        val out = mutableListOf<VisibleEntry>()
        collectVisibleEntries(entries, q, 0, 1f, out)
        return out
    }

    private fun visibleGlobalEntries(): List<VisibleEntry> {
        val out = mutableListOf<VisibleEntry>()
        categories.forEach { category ->
            val matches = visibleEntries(category.entries())
            if (matches.isNotEmpty()) {
                out += VisibleEntry(ConfigEntryModel.separator(displayLabel(category.name())), 0)
                out += matches
            }
        }
        return out
    }

    private fun collectVisibleEntries(
        entries: List<ConfigEntryModel>,
        queryText: String,
        depth: Int,
        parentExpansion: Float,
        out: MutableList<VisibleEntry>,
    ): Boolean {
        var anyVisible = false
        entries.forEach { e ->
            val visibility = visibilityAnim[entryKey(e)] ?: if (e.isVisible) 1f else 0f
            if (!e.isVisible && visibility <= 0.02f) return@forEach
            val selfMatches = queryText.isBlank() ||
                    displayLabel(entryLabel(e)).lowercase().contains(queryText) ||
                    (e.getDescription() ?: "").lowercase().contains(queryText)
            if (entryType(e) == EntryType.SECTION_HEADER) {
                val matchingNested = mutableListOf<VisibleEntry>()
                val hasVisibleChildren = collectVisibleEntries(children(e).orEmpty(), queryText, depth + 1, 1f, matchingNested)
                if (selfMatches || hasVisibleChildren) {
                    out += VisibleEntry(e, depth, parentExpansion * visibility)
                    val progress = sectionAnim[sectionKey(e)] ?: if (e.isExpanded) 1f else 0f
                    val expansion = parentExpansion * visibility * progress
                    if (e.isExpanded || progress > 0.02f) {
                        val nested = mutableListOf<VisibleEntry>()
                        collectVisibleEntries(children(e).orEmpty(), "", depth + 1, 1f, nested)
                        nested.forEach { out += it.copy(expansion = it.expansion * expansion) }
                    }
                    anyVisible = true
                }
            } else if (selfMatches) {
                out += VisibleEntry(e, depth, parentExpansion * visibility)
                anyVisible = true
            }
        }
        return anyVisible
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        if (closing) return true
        mouseXf = click.x().toFloat()
        mouseYf = click.y().toFloat()
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            searchFocused = false
            colorEditing = null
            openSelect = null
            rows.firstOrNull { isMouseInContent() && it.rect.contains(mouseXf, mouseYf) }?.let { hit ->
                openContextMenu(hit.entry, hit.rect)
                return true
            }
            contextMenuEntry = null
            contextMenuRect = null
            return true
        }
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(click, doubled)
        contextMenuHit?.takeIf { it.rect.contains(mouseXf, mouseYf) }?.let {
            resetEntryToDefault(it.entry)
            contextMenuEntry = null
            contextMenuRect = null
            searchFocused = false
            return true
        }
        contextMenuEntry = null
        contextMenuRect = null
        actions.lastOrNull { it.rect.contains(mouseXf, mouseYf) }?.let { it.action(); return true }
        val inContent = isMouseInContent()
        profitAddHits.lastOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            if (!inContent) return@let
            moveProfitLine(it.line, true, profitActiveLines.size)
            searchFocused = false
            return true
        }
        profitLineHits.lastOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            if (!inContent) return@let
            profitDraggingLine = it.line
            profitActiveLines.remove(it.line)
            profitInactiveLines.remove(it.line)
            saveProfitDisplayLines()
            searchFocused = false
            return true
        }
        selectOptionHits.lastOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            if (!inContent && !isMouseOverSelectDropdown()) return@let
            setField(entryField(it.entry), it.value)
            openSelect = null
            searchFocused = false
            return true
        }
        if (isMouseOverSelectDropdown()) {
            searchFocused = false
            return true
        }
        selectHits.lastOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            if (!inContent) return@let
            openSelect = if (openSelect == it.entry) null else it.entry
            openSelect?.let { entry -> selectScroll[entryKey(entry)] = 0 }
            editingNumber = null
            searchFocused = false
            return true
        }
        numberInputHits.firstOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            if (!inContent) return@let
            searchFocused = false
            if (doubled) beginNumberEdit(it.entry)
            return true
        }
        if (inContent) scrollbarHit?.let { bar ->
            if (bar.thumb.contains(mouseXf, mouseYf)) {
                draggingScrollbar = true
                scrollbarDragOffset = mouseYf - (bar.thumb.y + 5f)
                searchFocused = false
                return true
            }
            if (bar.track.contains(mouseXf, mouseYf)) {
                val thumbH = (bar.thumb.height - 10f).coerceAtLeast(1f)
                val trackH = bar.track.height
                val thumbY = (mouseYf - bar.track.y - thumbH / 2f).coerceIn(0f, trackH - thumbH)
                scroll = if (trackH <= thumbH) 0f else (thumbY / (trackH - thumbH) * maxScroll).coerceIn(0f, maxScroll)
                saveUiSession()
                draggingScrollbar = true
                scrollbarDragOffset = thumbH / 2f
                searchFocused = false
                return true
            }
        }
        sliderHits.firstOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            if (!inContent) return@let
            draggingSlider = it.entry
            updateSlider(it.entry, mouseXf)
            searchFocused = false
            return true
        }
        sidebarHits.firstOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            selectedCategory = it.index
                scroll = 0f
                query.clear()
                openSelect = null
                searchFocused = false
                saveUiSession()
                return true
        }
        rows.firstOrNull { it.rect.contains(mouseXf, mouseYf) }?.let { hit ->
            if (!inContent) return@let
            searchFocused = false
            handleEntryClick(hit.entry, mouseXf)
            return true
        }
        searchFocused = false
        colorEditing = null
        openSelect = null
        return true
    }

    private fun handleEntryClick(e: ConfigEntryModel, x: Float) {
        if (isProfitDisplayOptions(e)) {
            profitDisplayExpanded = !profitDisplayExpanded
            return
        }
        when (entryType(e)) {
            EntryType.BOOLEAN -> {
                if (isBuildProgressStyle(e)) {
                    val rowRect = rows.firstOrNull { it.entry == e }?.rect
                    val selector = rowRect?.let { centeredActionRect(it, 112f, 18f) }
                    val current = fieldValue(entryField(e)) as? Boolean ?: false
                    val next = if (selector != null && selector.contains(x, mouseYf)) {
                        x < selector.centerX
                    } else {
                        !current
                    }
                    setField(entryField(e), next)
                } else {
                    setField(entryField(e), !(fieldValue(entryField(e)) as? Boolean ?: false))
                }
            }
            EntryType.SELECT -> openSelect = if (openSelect == e) null else e
            EntryType.BUTTON -> buttonAction(e)?.run()
            EntryType.SECTION_HEADER -> {
                e.toggleExpanded()
                saveUiSession()
            }
            EntryType.COLOR -> {
                rememberColorHue(e)
                colorModalProgress = 0f
                colorEditing = e
            }
            EntryType.INT_SLIDER, EntryType.FLOAT_SLIDER, EntryType.DOUBLE_SLIDER -> {
                editingNumber = null
                draggingSlider = e
                updateSlider(e, x)
            }
            else -> {}
        }
    }

    private fun openContextMenu(entry: ConfigEntryModel, rowRect: Rect) {
        if (entryType(entry) == EntryType.SEPARATOR || entryType(entry) == EntryType.UNSUPPORTED || isProfitDisplayOptions(entry)) {
            contextMenuEntry = null
            contextMenuRect = null
            return
        }
        if (collectResetFields(entry).isEmpty()) {
            contextMenuEntry = null
            contextMenuRect = null
            return
        }
        contextMenuEntry = entry
        contextMenuRect = rowRect
        contextMenuOpenedAt = time
    }

    private fun resetEntryToDefault(entry: ConfigEntryModel) {
        val fields = collectResetFields(entry)
        if (fields.isEmpty()) return
        try {
            val updated = IQModClient.get()?.resetMainConfigFieldsToDefaults(fields) ?: 0
            openSelect = null
            editingNumber = null
            draggingSlider = null
            colorEditing = null
            if (updated > 0) {
                MessageUtil.SUCCESS.sendMessage("Reset ${displayLabel(entryLabel(entry))} to default.")
            } else {
                MessageUtil.WARNING.sendMessage("No defaults found for ${displayLabel(entryLabel(entry))}.")
            }
        } catch (t: Throwable) {
            MessageUtil.ERROR.sendMessage("Failed to reset config: ${t.message}")
            IqNanoVg.logger.warn("Failed to reset IQ config entry ${entryLabel(entry)} to defaults", t)
        }
    }

    private fun collectResetFields(entry: ConfigEntryModel): List<Field> {
        val out = LinkedHashSet<Field>()
        collectResetFields(entry, out)
        return out.toList()
    }

    private fun collectResetFields(entry: ConfigEntryModel, out: MutableSet<Field>) {
        entryField(entry)?.let { out += it }
        if (entryType(entry) == EntryType.SECTION_HEADER) {
            children(entry).orEmpty().forEach { collectResetFields(it, out) }
        }
    }

    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        if (closing) return true
        val colorEntry = colorEditing
        val colorTarget = draggingColorTarget
        if (colorEntry != null && colorTarget != null) {
            val pickerRects = colorPickerRects()
            val rect = when (colorTarget) {
                "sv" -> pickerRects.first
                "hue" -> pickerRects.second
                "alpha" -> pickerRects.third
                else -> null
            }
            if (rect != null) {
                updateColorFromPicker(colorEntry, colorTarget, click.x().toFloat(), click.y().toFloat(), rect)
                return true
            }
        }
        if (draggingScrollbar) {
            scrollbarHit?.let { bar ->
                val thumbH = (bar.thumb.height - 10f).coerceAtLeast(1f)
                val trackH = bar.track.height
                val thumbY = (click.y().toFloat() - bar.track.y - scrollbarDragOffset).coerceIn(0f, trackH - thumbH)
                scroll = if (trackH <= thumbH) 0f else (thumbY / (trackH - thumbH) * maxScroll).coerceIn(0f, maxScroll)
                saveUiSession()
                return true
            }
        }
        draggingSlider?.let {
            updateSlider(it, click.x().toFloat())
            return true
        }
        profitDraggingLine?.let {
            updateProfitDrag(click.x().toFloat(), click.y().toFloat())
            return true
        }
        return super.mouseDragged(click, offsetX, offsetY)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        profitDraggingLine?.let { line ->
            if (profitTrashRect?.contains(click.x().toFloat(), click.y().toFloat()) == true) {
                moveProfitLine(line, false, profitInactiveLines.size)
            } else {
                updateProfitDrag(click.x().toFloat(), click.y().toFloat())
                saveProfitDisplayLines()
            }
            profitDraggingLine = null
            return true
        }
        draggingSlider = null
        if (draggingScrollbar) saveUiSession()
        draggingScrollbar = false
        draggingColorTarget = null
        return super.mouseReleased(click)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (closing) return true
        openSelect?.let { entry ->
            if (isMouseOverSelectDropdown()) {
                val values = enumValues(entry)?.size ?: 0
                val maxStart = (values - 6).coerceAtLeast(0)
                val key = entryKey(entry)
                val step = if (verticalAmount > 0.0) -1 else if (verticalAmount < 0.0) 1 else 0
                selectScroll[key] = ((selectScroll[key] ?: 0) + step).coerceIn(0, maxStart)
                return true
            }
            openSelect = null
        }
        if (sidebarRect.contains(mouseX.toFloat(), mouseY.toFloat()) && maxSidebarScroll > 0f) {
            sidebarScroll = (sidebarScroll - verticalAmount.toFloat() * 26f).coerceIn(0f, maxSidebarScroll)
            return true
        }
        scroll = (scroll - verticalAmount.toFloat() * 34f).coerceIn(0f, maxScroll)
        saveUiSession()
        return true
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        if (closing) return true
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && colorEditing != null) {
            draggingColorTarget = null
            colorEditing = null
            return true
        }
        editingNumber?.let { entry ->
            return when (input.key()) {
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    commitNumberEdit(entry)
                    true
                }
                GLFW.GLFW_KEY_ESCAPE -> {
                    editingNumber = null
                    editingNumberText.clear()
                    true
                }
                GLFW.GLFW_KEY_BACKSPACE -> {
                    if (editingNumberText.isNotEmpty()) editingNumberText.deleteCharAt(editingNumberText.length - 1)
                    true
                }
                else -> true
            }
        }
        if (!searchFocused) return super.keyPressed(input)
        return when (input.key()) {
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (query.isNotEmpty()) {
                    query.deleteCharAt(query.length - 1)
                    scroll = 0f
                    saveUiSession()
                }
                openSelect = null
                true
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                searchFocused = false
                openSelect = null
                true
            }
            else -> super.keyPressed(input)
        }
    }

    override fun charTyped(input: CharacterEvent): Boolean {
        if (closing) return true
        editingNumber?.let {
            val c = input.codepoint().toChar()
            if (c.isDigit() || c == '.' || c == '-' || c == ',') {
                editingNumberText.append(if (c == ',') '.' else c)
            }
            return true
        }
        if (!searchFocused) return super.charTyped(input)
        val c = input.codepoint().toChar()
        if (!Character.isISOControl(c)) {
            query.append(c)
            scroll = 0f
            openSelect = null
            saveUiSession()
        }
        return true
    }

    private fun updateSlider(e: ConfigEntryModel, x: Float) {
        val row = rows.firstOrNull { it.entry == e } ?: return
        val bar = sliderRects(row.rect, e).second
        val t = ((x - bar.x) / bar.width).coerceIn(0f, 1f)
        val value = rangeMin(e) + (rangeMax(e) - rangeMin(e)) * t
        applyNumericValue(e, value)
    }

    private fun sliderRects(row: Rect, e: ConfigEntryModel): Pair<Rect, Rect> {
        val hasDescription = !(e.getDescription().isNullOrBlank()) && row.height > 32f
        val y = if (hasDescription) row.y + 10f else row.centerY - 5f
        val barW = (row.width * 0.23f).coerceIn(68f, 96f)
        val bar = Rect(row.right - 20f - barW, y, barW, 10f)
        val value = Rect(bar.x - 39f, y - 2.5f, 31f, 15f)
        return value to bar
    }

    private fun formatNumericValue(e: ConfigEntryModel, value: Double): String =
        if (entryType(e) == EntryType.INT_SLIDER) value.roundToInt().toString() else "%.2f".format(Locale.US, value)

    private fun beginNumberEdit(e: ConfigEntryModel) {
        editingNumber = e
        editingNumberText.clear()
        val value = (fieldValue(entryField(e)) as? Number)?.toDouble() ?: rangeMin(e)
        editingNumberText.append(formatNumericValue(e, value))
    }

    private fun commitNumberEdit(e: ConfigEntryModel) {
        val parsed = editingNumberText.toString().trim().replace(',', '.').toDoubleOrNull()
        if (parsed != null) applyNumericValue(e, parsed)
        editingNumber = null
        editingNumberText.clear()
    }

    private fun applyNumericValue(e: ConfigEntryModel, raw: Double) {
        val value = raw.coerceIn(rangeMin(e), rangeMax(e))
        val field = entryField(e)
        when (entryType(e)) {
            EntryType.INT_SLIDER -> setField(field, value.roundToInt())
            EntryType.FLOAT_SLIDER -> setField(field, value.toFloat())
            EntryType.DOUBLE_SLIDER -> setField(field, value)
            else -> {}
        }
    }

    private fun colorPickerRects(): Triple<Rect, Rect, Rect> {
        val modal = Rect(panel.centerX - 210f, panel.centerY - 155f, 420f, 310f)
        val sv = Rect(modal.x + 18f, modal.y + 82f, modal.width - 36f, 118f)
        val hueBar = Rect(sv.x, sv.bottom + 28f, sv.width, 9f)
        val alphaBar = Rect(sv.x, hueBar.bottom + 30f, sv.width, 9f)
        return Triple(sv, hueBar, alphaBar)
    }

    private fun isColorModalOpen(): Boolean = colorEditing != null

    private fun rememberColorHue(e: ConfigEntryModel) {
        val argb = (fieldValue(entryField(e)) as? Number)?.toInt() ?: return
        val r = (argb ushr 16) and 255
        val g = (argb ushr 8) and 255
        val b = argb and 255
        val hsb = java.awt.Color.RGBtoHSB(r, g, b, null)
        if (hsb[1] > 0.001f && hsb[2] > 0.001f) {
            colorHueMemory[entryKey(e)] = hsb[0]
        }
    }

    private fun updateColorFromPicker(e: ConfigEntryModel, target: String, x: Float, y: Float, rect: Rect) {
        val old = (fieldValue(entryField(e)) as? Number)?.toInt() ?: 0xFFFFFFFF.toInt()
        val oldA = ((old ushr 24) and 255) / 255f
        val oldR = (old ushr 16) and 255
        val oldG = (old ushr 8) and 255
        val oldB = old and 255
        val hsb = java.awt.Color.RGBtoHSB(oldR, oldG, oldB, null)
        val key = entryKey(e)
        var hue = if (hsb[1] <= 0.001f || hsb[2] <= 0.001f) colorHueMemory[key] ?: hsb[0] else hsb[0]
        var sat = hsb[1]
        var bri = hsb[2]
        var alpha = oldA
        val clampedX = x.coerceIn(rect.x, rect.right)
        val clampedY = y.coerceIn(rect.y, rect.bottom)

        when (target) {
            "sv" -> {
                sat = ((clampedX - rect.x) / rect.width).coerceIn(0f, 1f)
                bri = (1f - ((clampedY - rect.y) / rect.height)).coerceIn(0f, 1f)
            }
            "hue" -> hue = ((clampedX - rect.x) / rect.width).coerceIn(0f, 1f).also { colorHueMemory[key] = it }
            "alpha" -> alpha = ((clampedX - rect.x) / rect.width).coerceIn(0f, 1f)
        }

        val rgb = java.awt.Color.HSBtoRGB(hue, sat, bri)
        val next = argb(
            (alpha * 255f).roundToInt().coerceIn(0, 255),
            (rgb ushr 16) and 255,
            (rgb ushr 8) and 255,
            rgb and 255,
        )
        setField(entryField(e), next)
    }

    private fun setColorChannel(e: ConfigEntryModel, channel: Int, value: Int) {
        val old = (fieldValue(entryField(e)) as? Number)?.toInt() ?: 0xFFFFFFFF.toInt()
        val a = (old ushr 24) and 255
        val r = (old ushr 16) and 255
        val g = (old ushr 8) and 255
        val b = old and 255
        val next = when (channel) {
            0 -> argb(a, value, g, b)
            1 -> argb(a, r, value, b)
            2 -> argb(a, r, g, value)
            else -> argb(value, r, g, b)
        }
        setField(entryField(e), next)
    }

    private fun fieldValue(field: Field?): Any? = try {
        field?.get(null)
    } catch (_: Throwable) {
        null
    }

    private fun setField(field: Field?, value: Any?) {
        try {
            field?.set(null, value)
        } catch (t: Throwable) {
            IqNanoVg.logger.warn("Failed to update IQ config field ${field?.name}", t)
        }
    }

    private fun configChildCount(e: ConfigEntryModel): Int =
        children(e).orEmpty().count { child ->
            child.isVisible && entryType(child) != EntryType.SEPARATOR
        }

    private fun selectValueLabel(entry: ConfigEntryModel, value: Any?): String {
        if (entryField(entry)?.name == "etherwarpWaypointRenderStyle" && value?.toString() == "TOP") {
            return "TOP BLOCK"
        }
        return displayLabel(value?.toString().orEmpty())
    }

    private fun saveConfig() {
        saveUiSession()
        saveResourcefulConfig()
    }

    override fun removed() {
        saveConfig()
        super.removed()
    }

    override fun onClose() {
        beginClose(parent)
    }

    override fun isPauseScreen(): Boolean = false

    private fun updateAnimations() {
        val enabled = IqNanoGlobalConfigScreen.isSharedAnimationsEnabled()
        val speed = IqNanoGlobalConfigScreen.getSharedAnimationSpeed().toFloat().coerceIn(0.1f, 1f)
        val amount = if (enabled) (0.10f + speed * 0.26f).coerceIn(0.12f, 0.42f) else 1f
        val hoveredEntry = if (isMouseInContent() && !isMouseOverSelectDropdown() && !isColorModalOpen()) {
            rows.firstOrNull { it.rect.contains(mouseXf, mouseYf) }?.entry
        } else {
            null
        }
        openProgress = approach(openProgress, if (closing) 0.82f else 1f, amount)
        closeProgress = approach(closeProgress, if (closing) 1f else 0f, if (enabled) (amount * 1.65f).coerceIn(0.28f, 0.72f) else 1f)
        searchFocusAnim = approach(searchFocusAnim, if (searchFocused) 1f else 0f, amount)
        colorModalProgress = approach(colorModalProgress, if (colorEditing != null) 1f else 0f, amount)

        categories.forEachIndexed { index, category ->
            categoryAnim[category.id()] = approach(categoryAnim[category.id()] ?: 0f, if (index == selectedCategory) 1f else 0f, amount)
            val shouldAnimateEntries = query.isNotBlank() || index == selectedCategory
            if (shouldAnimateEntries) category.entries().forEach { updateEntryAnimation(it, amount, hoveredEntry) }
        }
    }

    private fun updateEntryAnimation(entry: ConfigEntryModel, amount: Float, hoveredEntry: ConfigEntryModel?) {
        val key = entryKey(entry)
        val visibleTarget = if (entry.isVisible) 1f else 0f
        visibilityAnim[key] = approach(visibilityAnim[key] ?: visibleTarget, visibleTarget, amount)
        val hoverTarget = if (hoveredEntry == entry) 1f else 0f
        val hoverAmount = if (hoverTarget > 0f) 0.52f else 0.78f
        rowHoverAnim[key] = approach(rowHoverAnim[key] ?: 0f, hoverTarget, hoverAmount)
        if (entryType(entry) == EntryType.BOOLEAN) {
            val on = fieldValue(entryField(entry)) as? Boolean == true
            toggleAnim[key] = approach(toggleAnim[key] ?: if (on) 1f else 0f, if (on) 1f else 0f, amount)
        }
        if (entryType(entry) == EntryType.SECTION_HEADER) {
            if (!entry.isVisible && entry.isExpanded) {
                entry.toggleExpanded()
                sectionAnim[sectionKey(entry)] = 0f
            }
            sectionAnim[sectionKey(entry)] = approach(sectionAnim[sectionKey(entry)] ?: if (entry.isExpanded) 1f else 0f, if (entry.isExpanded) 1f else 0f, amount)
            if (entry.isExpanded || (sectionAnim[sectionKey(entry)] ?: 0f) > 0.02f || query.isNotBlank()) {
                children(entry).orEmpty().forEach { updateEntryAnimation(it, amount, hoveredEntry) }
            }
        }
        if (entryType(entry) == EntryType.SELECT) {
            selectAnim[key] = approach(selectAnim[key] ?: if (openSelect == entry) 1f else 0f, if (openSelect == entry) 1f else 0f, amount)
        }
    }

    private fun approach(current: Float, target: Float, amount: Float): Float =
        current + (target - current) * amount.coerceIn(0f, 1f)

    private fun beginClose(target: Screen?) {
        if (closing) return
        saveUiSession()
        closing = true
        closeTarget = target
        closeProgress = 0f
        draggingSlider = null
        draggingScrollbar = false
        draggingColorTarget = null
        profitDraggingLine = null
        searchFocused = false
        openSelect = null
        colorEditing = null
    }

    private fun restoreUiSession() {
        if (IqNanoGlobalConfigScreen.isSharedUiStatePersistenceEnabled()) {
            selectedCategory = store.getOrDefault(UI_SESSION_CATEGORY, selectedCategory)
            val savedQuery = store.getOrDefault(UI_SESSION_QUERY, "")
            query.clear()
            if (savedQuery.isNotBlank()) query.append(savedQuery)
            scroll = store.getOrDefault(UI_SESSION_SCROLL, java.lang.Double.valueOf(0.0)).toFloat()
            restoreExpandedSections(store.getOrDefault(UI_SESSION_EXPANDED_SECTIONS, ""))
        } else {
            selectedCategory = 0
            query.clear()
            scroll = 0f
            restoreExpandedSections("")
        }
        selectedCategory = selectedCategory.coerceIn(0, (categories.size - 1).coerceAtLeast(0))
        scroll = scroll.coerceAtLeast(0f)
    }

    private fun saveUiSession() {
        if (!IqNanoGlobalConfigScreen.isSharedUiStatePersistenceEnabled()) return
        store.set(UI_SESSION_CATEGORY, selectedCategory)
        store.set(UI_SESSION_QUERY, query.toString())
        store.set(UI_SESSION_SCROLL, scroll.toDouble())
        store.set(UI_SESSION_EXPANDED_SECTIONS, expandedSectionKeys().joinToString("\n"))
    }

    private fun rebuildSectionPathIndex() {
        sectionPaths.clear()
        categories.forEach { category ->
            indexSectionPaths(category.entries(), category.id())
        }
    }

    private fun indexSectionPaths(entries: List<ConfigEntryModel>, parentPath: String) {
        val labelCounts = HashMap<String, Int>()
        entries.forEach { entry ->
            if (entryType(entry) == EntryType.SECTION_HEADER) {
                val label = entryLabel(entry)
                val count = labelCounts.merge(label, 1, Int::plus) ?: 1
                val key = "$parentPath/${label.replace("/", "\\/")}#$count"
                sectionPaths[entry] = key
                indexSectionPaths(children(entry).orEmpty(), key)
            }
        }
    }

    private fun restoreExpandedSections(saved: String) {
        val expanded = saved.lineSequence().filter { it.isNotBlank() }.toHashSet()
        sectionPaths.forEach { (entry, key) ->
            if (entry.isExpanded != (key in expanded)) entry.toggleExpanded()
            sectionAnim[sectionKey(entry)] = if (entry.isExpanded) 1f else 0f
        }
    }

    private fun expandedSectionKeys(): List<String> =
        sectionPaths.entries.asSequence()
            .filter { it.key.isExpanded }
            .map { it.value }
            .sorted()
            .toList()

    private fun entryKey(e: ConfigEntryModel): String =
        entryField(e)?.let { "${it.declaringClass.name}#${it.name}" } ?: "label#${entryLabel(e)}"

    private fun sectionKey(e: ConfigEntryModel): String = "section#${sectionPaths[e] ?: entryLabel(e)}"

    private fun effectiveConfigClasses(): Array<out Class<*>> =
        configClasses.takeIf { it.isNotEmpty() } ?: DEFAULT_CONFIG_CLASSES

    private fun shouldDrawCategoryDivider(index: Int): Boolean {
        val categoryName = categories.getOrNull(index)?.name()?.let { displayLabel(it) } ?: return false
        return categoryName == "General" || categoryName.startsWith("Phase 4")
    }

    private fun drawConfigBadge(r: Renderer2D, rect: Rect, label: String) {
        r.fillRect(rect, iqButtonSurface().withAlpha(0.72f), CornerRadius.uniform(4f))
        r.strokeRect(rect, iqAccent().withAlpha(0.30f), 1f, CornerRadius.uniform(4f))
        r.text(fitText(r, label, rect.width - 9f, 7.0f, 5.8f, fontMedium()), rect.centerX, rect.y + 3.7f, 7.0f, iqMuted(), fontMedium(), TextAlign.CENTER)
    }

    private fun actionCenterX(rect: Rect): Float = rect.right - 43f

    private fun centeredActionRect(rect: Rect, width: Float, height: Float, centerOffset: Float = 0f): Rect {
        val rightPad = if (centerOffset < 0f) 42f else 24f
        val desiredX = actionCenterX(rect) + centerOffset - width / 2f
        val x = desiredX
            .coerceAtMost(rect.right - rightPad - width)
            .coerceAtLeast(rect.x + 8f)
        return Rect(x, rect.centerY - height / 2f, width, height)
    }

    private fun entryHeight(r: Renderer2D, entry: ConfigEntryModel, width: Float, child: Boolean = false): Float {
        if (isProfitDisplayOptions(entry)) {
            val totalRows = profitActiveLines.size + max(1, profitInactiveLines.size)
            val expandedH = 90f + totalRows * 19f
            return if (profitDisplayExpanded) expandedH.coerceIn(105f, 285f) else 25f
        }
        if (child) return 22f
        val desc = entry.getDescription()?.takeIf { it.isNotBlank() } ?: return 34f
        val lines = wrapDescription(r, stripCodes(desc), textColumnWidth(entry, Rect(0f, 0f, width, 34f)), 7.7f, fontRegular(), 4).size
        return (28f + lines * 8.3f).coerceAtLeast(34f).coerceAtMost(52f)
    }

    private fun isProfitDisplayOptions(entry: ConfigEntryModel): Boolean =
        entryType(entry) == EntryType.BUTTON && entryLabel(entry) == "Display Options" && buttonText(entry)?.equals("OPEN", true) == true

    private fun isBuildProgressStyle(entry: ConfigEntryModel): Boolean =
        entryField(entry)?.name == "simpleBuildProgressOverlay" ||
            displayLabel(entryLabel(entry)).equals("Build Progress Style", ignoreCase = true)

    private fun reloadProfitDisplayLines() {
        profitActiveLines.clear()
        profitActiveLines += profitDisplayManager.activeLines()
        profitInactiveLines.clear()
        profitInactiveLines += profitDisplayManager.inactiveLines()
    }

    private fun saveProfitDisplayLines() {
        profitDisplayManager.setLayout(profitActiveLines.toList(), profitInactiveLines.toList())
    }

    private fun moveProfitLine(line: ProfitTrackerDisplayLine, active: Boolean, index: Int) {
        profitActiveLines.remove(line)
        profitInactiveLines.remove(line)
        val target = if (active) profitActiveLines else profitInactiveLines
        target.add(index.coerceIn(0, target.size), line)
        saveProfitDisplayLines()
    }

    private fun updateProfitDrag(mouseX: Float, mouseY: Float) {
        val line = profitDraggingLine ?: return
        val active = profitDragTargetActive(mouseX, mouseY)
        val targetLines = if (active) profitActiveLines else profitInactiveLines
        val index = profitLineHits
            .filter { it.active == active && it.line != line }
            .count { mouseY > it.rect.centerY }
            .coerceIn(0, targetLines.size)
        profitActiveLines.remove(line)
        profitInactiveLines.remove(line)
        targetLines.add(index, line)
        saveProfitDisplayLines()
    }

    private fun profitDragTargetActive(mouseX: Float, mouseY: Float): Boolean {
        val inactive = profitInactiveDropRect
        val active = profitActiveDropRect
        if (inactive?.contains(mouseX, mouseY) == true) return false
        if (active?.contains(mouseX, mouseY) == true) return true
        if (inactive != null && mouseY >= inactive.y) return false
        return true
    }

    private fun textColumnWidth(entry: ConfigEntryModel, rect: Rect): Float {
        val reserved = when (entryType(entry)) {
            EntryType.BOOLEAN -> if (isBuildProgressStyle(entry)) 176f else 150f
            EntryType.BUTTON -> 150f
            EntryType.SELECT -> 214f
            EntryType.INT_SLIDER, EntryType.FLOAT_SLIDER, EntryType.DOUBLE_SLIDER -> 198f
            EntryType.COLOR -> 130f
            EntryType.SECTION_HEADER -> 150f
            else -> 24f
        }.coerceAtMost(rect.width * 0.52f)
        return (rect.width - reserved).coerceAtLeast(90f)
    }

    private fun wrapText(r: Renderer2D, text: String, maxWidth: Float, size: Float, font: String): List<String> {
        if (text.isBlank()) return emptyList()
        if (r.textWidth(text, size, font) <= maxWidth) return listOf(text)
        val lines = mutableListOf<String>()
        var current = ""
        text.split(Regex("\\s+")).forEach { word ->
            val next = if (current.isBlank()) word else "$current $word"
            if (r.textWidth(next, size, font) <= maxWidth || current.isBlank()) {
                current = next
            } else {
                lines += current
                current = word
            }
        }
        if (current.isNotBlank()) lines += current
        return lines
    }

    private fun wrapDescription(r: Renderer2D, text: String, maxWidth: Float, size: Float, font: String, maxLines: Int): List<String> =
        text.lineSequence()
            .flatMap { line -> wrapText(r, line.trim(), maxWidth, size, font).asSequence() }
            .filter { it.isNotBlank() }
            .take(maxLines)
            .toList()

    private fun fitText(r: Renderer2D, text: String, maxWidth: Float, size: Float, minSize: Float, font: String): String {
        if (maxWidth <= 8f || r.textWidth(text, size, font) <= maxWidth) return text
        val ellipsis = "..."
        var out = text
        while (out.length > 4 && r.textWidth(out + ellipsis, minSize, font) > maxWidth) {
            out = out.dropLast(1)
        }
        return out.trimEnd() + ellipsis
    }

    private fun drawAnimatedBorder(r: Renderer2D, rect: Rect) {
        val radius = CornerRadius.uniform(10f)
        r.strokeRect(rect, iqBorder().withAlpha(0.82f), 1.45f, radius)
        r.strokeRect(rect.inset(1f), (if (IqNanoGlobalConfigScreen.isCyberpunkTheme()) iqAccent() else Color.fromHex("#FFFFFF")).withAlpha(0.045f), 0.85f, CornerRadius.uniform(9f))
        if (!IqNanoGlobalConfigScreen.isSharedAnimationsEnabled()) return
        if (IqNanoGlobalConfigScreen.isSharedAdvancedAnimationsEnabled()) {
            drawAdvancedBorder(r, rect)
            return
        }

        val speed = IqNanoGlobalConfigScreen.getSharedAnimationSpeed().toFloat().coerceIn(0.1f, 1f)
        val pulse = ((sin(time * (0.65f + speed * 1.15f) * PI.toFloat()) + 1f) * 0.5f).coerceIn(0f, 1f)
        val hot = 0.24f + pulse * 0.20f
        if (IqNanoGlobalConfigScreen.isCyberpunkTheme()) {
            r.strokeRect(rect.inset(0.25f), iqAccent().withAlpha(hot), 1.05f, CornerRadius.uniform(9.75f))
        } else {
            r.strokeRect(rect.inset(0.25f), iqAccentHot().withAlpha(0.14f + pulse * 0.10f), 1.15f, CornerRadius.uniform(9.75f))
            r.strokeRect(rect.inset(1.2f), iqAccentGlow().withAlpha(hot), 0.80f, CornerRadius.uniform(8.8f))
        }
    }

    private fun drawAdvancedBorder(r: Renderer2D, rect: Rect) {
        val perimeter = roundedRectPerimeter(rect, 10f)
        val waveSize = min(265f, max(170f, perimeter * 0.20f))
        val speed = IqNanoGlobalConfigScreen.getSharedAnimationSpeed().toFloat().coerceIn(0.1f, 1f)
        val animTime = time * (0.35f + speed * 1.35f)
        val travel = (animTime * 92f) % perimeter
        repeat(5) { i ->
            val start = (travel + i * perimeter / 5f + sin(animTime * 0.55f + i * 2.1f) * 14f) % perimeter
            drawBorderWave(r, rect, start, waveSize, i * 1.73f, animTime)
        }
    }

    private fun drawBorderWave(r: Renderer2D, rect: Rect, start: Float, length: Float, seed: Float, animTime: Float) {
        val points = mutableListOf<Point>()
        val steps = 220
        for (i in 0..steps) {
            points += pointOnRoundedRect(rect, 10f, start + length * i / steps)
        }
        for (i in 0 until points.lastIndex) {
            val t = (i + 0.5f) / steps.toFloat()
            val gradient = sin(t * PI.toFloat()).coerceAtLeast(0f)
            val softTail = gradient * gradient * (1f - 0.18f * t)
            val organicPulse = 0.70f + 0.30f * ((sin(animTime * (1.65f + seed * 0.18f) + seed + t * 7.1f) + 1f) * 0.5f)
            val shimmer = 0.90f + 0.10f * ((sin(animTime * 3.15f + seed * 3.4f + t * 13.0f) + 1f) * 0.5f)
            val edgeFade = borderWaveEdgeFade(t)
            val midPoint = Point((points[i].x + points[i + 1].x) * 0.5f, (points[i].y + points[i + 1].y) * 0.5f)
            val cornerBoost = cornerGlowBoost(rect, midPoint)
            val intensity = (0.006f + softTail * 0.235f) * edgeFade * organicPulse * shimmer * (1f + cornerBoost * 0.22f)
            val width = 0.52f + softTail * 1.45f * (0.70f + edgeFade * 0.30f)

            if (IqNanoGlobalConfigScreen.isCyberpunkTheme()) {
                r.backend.line(points[i], points[i + 1], Paint.solid(iqAccentGlow().withAlpha(intensity * 0.58f)), width + 3.8f)
                r.backend.line(points[i], points[i + 1], Paint.solid(iqAccent().withAlpha(intensity * 0.62f)), width + 1.75f)
            } else {
                r.backend.line(points[i], points[i + 1], Paint.solid(iqAccent().withAlpha(intensity * 0.24f)), width + 4.7f)
                r.backend.line(points[i], points[i + 1], Paint.solid(iqAccentHot().withAlpha(intensity * 0.42f)), width + 2.05f)
                r.backend.line(points[i], points[i + 1], Paint.solid(iqAccentGlow().withAlpha(intensity * 0.68f)), width + 0.95f)
                r.backend.line(points[i], points[i + 1], Paint.solid(Color.fromHex("#FFE2F6").withAlpha(intensity * 0.10f)), max(0.40f, width * 0.28f))
            }
        }
    }

    private fun borderWaveEdgeFade(t: Float): Float {
        val head = (t / 0.18f).coerceIn(0f, 1f)
        val tail = ((1f - t) / 0.16f).coerceIn(0f, 1f)
        val fadeIn = head * head * (3f - 2f * head)
        val fadeOut = tail * tail * (3f - 2f * tail)
        return fadeIn * fadeOut
    }

    private fun drawBrandTitle(r: Renderer2D, text: String, x: Float, y: Float, size: Float) {
        val anim = if (IqNanoGlobalConfigScreen.isSharedAnimationsEnabled()) {
            ((sin(time * (0.75f + IqNanoGlobalConfigScreen.getSharedAnimationSpeed().toFloat().coerceIn(0.1f, 1f) * 0.65f)) + 1f) * 0.5f)
        } else {
            0.35f
        }
        val glowPulse = 0.86f + anim * 0.44f
        val face = iqText().lerp(Color.fromHex("#FFFFFF"), 0.16f + anim * 0.18f)
        r.text(text, x + 1.35f, y + 0.55f, size, brandDetailAccent().withAlpha(0.34f * glowPulse), fontBold())
        r.text(text, x - 1.05f, y + 0.25f, size, iqAccent().withAlpha(0.20f + 0.12f * anim), fontBold())
        r.text(text, x + 0.35f, y - 0.85f, size, iqAccentGlow().withAlpha(0.12f + 0.16f * anim), fontBold())
        r.text(text, x, y - 0.55f, size, Color.fromHex("#FFFFFF").withAlpha(0.12f + 0.12f * anim), fontBold())
        r.text(text, x, y, size, face, fontBold())
    }

    private fun drawLogoChromaOutline(r: Renderer2D, logo: Rect) {
        val animEnabled = IqNanoGlobalConfigScreen.isSharedAnimationsEnabled()
        val speed = IqNanoGlobalConfigScreen.getSharedAnimationSpeed().toFloat().coerceIn(0.1f, 1f)
        val t = if (animEnabled) time * (0.55f + speed * 0.9f) else 0f
        val a = ((sin(t) + 1f) * 0.5f).coerceIn(0f, 1f)
        val b = ((sin(t + 2.09f) + 1f) * 0.5f).coerceIn(0f, 1f)
        val primary = iqAccent().lerp(iqAccentHot(), a)
        val secondary = iqAccentHot().lerp(iqAccentGlow(), b)
        val pulseAlpha = 0.56f + if (animEnabled) pulse(0.16f, 1.1f) else 0f

        r.strokeRect(logo.inset(-2.4f), primary.withAlpha(0.18f * pulseAlpha), 2.1f, CornerRadius.uniform(9f))
        r.strokeRect(logo.inset(-1.5f), secondary.withAlpha(0.34f * pulseAlpha), 1.15f, CornerRadius.uniform(8f))
        r.strokeRect(logo.inset(-0.7f), Color.fromHex("#FFFFFF").withAlpha(0.08f + 0.06f * pulseAlpha), 0.75f, CornerRadius.uniform(7.5f))
    }

    private fun pulse(amount: Float, speed: Float): Float =
        ((sin(time * speed * PI.toFloat()) + 1f) * 0.5f) * amount

    private fun cornerGlowBoost(rect: Rect, point: Point): Float {
        val radius = 10f
        val corners = arrayOf(
            Point(rect.x + radius, rect.y + radius),
            Point(rect.right - radius, rect.y + radius),
            Point(rect.right - radius, rect.bottom - radius),
            Point(rect.x + radius, rect.bottom - radius),
        )
        val nearest = corners.minOf { corner ->
            val dx = point.x - corner.x
            val dy = point.y - corner.y
            sqrt(dx * dx + dy * dy)
        }
        val normalized = (1f - nearest / 42f).coerceIn(0f, 1f)
        return normalized * normalized
    }

    private fun roundedRectPerimeter(rect: Rect, radius: Float): Float =
        2f * ((rect.width - 2f * radius) + (rect.height - 2f * radius)) + 2f * PI.toFloat() * radius

    private fun pointOnRoundedRect(rect: Rect, radius: Float, distance: Float): Point {
        val straightW = rect.width - 2f * radius
        val straightH = rect.height - 2f * radius
        val arc = PI.toFloat() * radius / 2f
        var p = distance % roundedRectPerimeter(rect, radius)

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

    companion object {
        private val UI_SCALE = DataKey.of("globalcfg.uiScale", Double::class.javaObjectType)
        private val UI_SESSION_CATEGORY = DataKey.of("globalcfg.session.category", Int::class.javaObjectType)
        private val UI_SESSION_QUERY = DataKey.of("globalcfg.session.query", String::class.java)
        private val UI_SESSION_SCROLL = DataKey.of("globalcfg.session.scroll", Double::class.javaObjectType)
        private val UI_SESSION_EXPANDED_SECTIONS = DataKey.of("globalcfg.session.expandedSections", String::class.java)
        fun savedUiScale(): Double = IQPersistentDataStore.get().getOrDefault(UI_SCALE, java.lang.Double.valueOf(1.0)).toDouble()

        private val DEFAULT_CONFIG_CLASSES = arrayOf(
            Configuration::class.java,
            KuudraGeneralConfig::class.java,
            PhaseOneConfig::class.java,
            PhaseTwoConfig::class.java,
            PhaseThreeConfig::class.java,
            PhaseFourConfig::class.java,
        )

        private fun buildCategory(type: Class<*>): ConfigCategory? = try {
            val category = Class.forName("net.iqaddons.mod.utils.ConfigReflectionUtil")
                .getMethod("buildCategory", Class::class.java)
                .invoke(null, type) as? ConfigCategory
            if (category != null && category.entries().isNotEmpty()) category else null
        } catch (t: Throwable) {
            IqNanoVg.logger.warn("Failed to build IQ config category for ${type.name}", t)
            null
        }

        private fun modVersion(): String =
            net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer("iqaddons")
                .map { it.metadata.version.friendlyString }
                .orElse("unknown")

        private fun modelField(name: String): Field =
            ConfigEntryModel::class.java.getDeclaredField(name).apply { isAccessible = true }

        private val F_TYPE = modelField("type")
        private val F_LABEL = modelField("label")
        private val F_SEPARATOR = modelField("separatorLabel")
        private val F_FIELD = modelField("field")
        private val F_RANGE_MIN = modelField("rangeMin")
        private val F_RANGE_MAX = modelField("rangeMax")
        private val F_BUTTON_TEXT = modelField("buttonText")
        private val F_BUTTON_ACTION = modelField("buttonAction")
        private val F_ENUM_VALUES = modelField("enumValues")
        private val F_ENUM_DESCRIPTION_RESOLVER = modelField("enumDescriptionResolver")
        private val F_CHILDREN = modelField("children")

        @Suppress("UNCHECKED_CAST")
        private fun entryType(e: ConfigEntryModel): EntryType = F_TYPE.get(e) as EntryType
        private fun entryLabel(e: ConfigEntryModel): String = F_LABEL.get(e) as? String ?: ""
        private fun separatorLabel(e: ConfigEntryModel): String? = F_SEPARATOR.get(e) as? String
        private fun entryField(e: ConfigEntryModel): Field? = F_FIELD.get(e) as? Field
        private fun rangeMin(e: ConfigEntryModel): Double = (F_RANGE_MIN.get(e) as? Number)?.toDouble() ?: 0.0
        private fun rangeMax(e: ConfigEntryModel): Double = (F_RANGE_MAX.get(e) as? Number)?.toDouble() ?: 1.0
        private fun buttonText(e: ConfigEntryModel): String? = F_BUTTON_TEXT.get(e) as? String
        private fun buttonAction(e: ConfigEntryModel): Runnable? = F_BUTTON_ACTION.get(e) as? Runnable
        private fun enumValues(e: ConfigEntryModel): Array<Any?>? = F_ENUM_VALUES.get(e) as? Array<Any?>
        @Suppress("UNCHECKED_CAST")
        private fun enumDescription(e: ConfigEntryModel, value: Any?): String? =
            (F_ENUM_DESCRIPTION_RESOLVER.get(e) as? java.util.function.Function<Any?, String?>)?.apply(value)
        private fun children(e: ConfigEntryModel): List<ConfigEntryModel>? = F_CHILDREN.get(e) as? List<ConfigEntryModel>

        private fun saveResourcefulConfig() {
            try {
                IQModClient.get()?.saveMainConfig()
            } catch (t: Throwable) {
                IqNanoVg.logger.warn("Failed to save IQ main config from Nano config screen", t)
            }
        }

        private fun openUrl(url: String) {
            Util.getPlatform().openUri(url)
        }

        private fun displayLabel(raw: String): String {
            val stripped = stripCodes(raw)
            val base = if (' ' !in stripped && stripped.count { it == '.' } == 1 && stripped.substringBefore('.').all { it.isLowerCase() }) {
                stripped.substringAfterLast('.')
            } else {
                stripped
            }
            val clean = base.replace('_', ' ').replace('-', ' ').replace(Regex("\\s+"), " ")
            if (clean.equals("Phase 4 Boss Fight", ignoreCase = true)) return "Phase 4 - Boss"
            Regex("^Phase\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE).matchEntire(clean)?.let {
                return "Phase ${it.groupValues[1]} - ${it.groupValues[2].trim()}"
            }
            return clean.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ").trim().replaceFirstChar { it.uppercase() }
        }

        private fun stripCodes(text: String): String =
            normalizeUnsupportedGlyphs(text.replace(Regex("(?:§|§)."), ""))

        private fun normalizeUnsupportedGlyphs(text: String): String = text
            .replace("→", "->")
            .replace("←", "<-")
            .replace("↔", "<->")
            .replace("⇒", "=>")
            .replace("➜", "->")
            .replace("➔", "->")
            .replace("›", ">")
            .replace("»", ">")
            .replace("â†’", "->")
            .replace("â†�", "<-")
            .replace("â†”", "<->")
            .replace("â‡’", "=>")
            .replace("â€º", ">")
            .replace("Â»", ">")
        private fun iqAccent() = IqNanoGlobalConfigScreen.themeAccent()
        private fun iqAccentHot() = IqNanoGlobalConfigScreen.themeAccentHot()
        private fun iqAccentGlow() = IqNanoGlobalConfigScreen.themeAccentGlow()
        private fun brandDetailAccent() = if (IqNanoGlobalConfigScreen.isCyberpunkTheme()) iqAccentHot() else iqAccent()
        private fun brandSubtitleColor() = if (IqNanoGlobalConfigScreen.isCyberpunkTheme()) Color.fromHex("#00C8D8") else iqMuted()
        private fun iqBorder() = IqNanoGlobalConfigScreen.themeBorder()
        private fun iqPanelStart() = IqNanoGlobalConfigScreen.themePanelStart()
        private fun iqPanelEnd() = IqNanoGlobalConfigScreen.themePanelEnd()
        private fun iqSurface() = IqNanoGlobalConfigScreen.themeSurface()
        private fun iqButtonSurface() = IqNanoGlobalConfigScreen.themeButtonSurface()
        private fun iqModalSurface() = IqNanoGlobalConfigScreen.themeModalSurface()
        private fun iqToggleOff() = IqNanoGlobalConfigScreen.themeToggleOff()
        private fun iqKnob() = IqNanoGlobalConfigScreen.themeKnob()
        private fun iqText() = IqNanoGlobalConfigScreen.themeText()
        private fun iqMuted() = IqNanoGlobalConfigScreen.themeMuted()
        private fun iqDisabled() = IqNanoGlobalConfigScreen.themeDisabled()
        private fun sliderTrack() = iqToggleOff().lerp(iqMuted(), 0.12f).withAlpha(0.92f)
        private fun fontRegular() = IqNanoGlobalConfigScreen.fontRegular()
        private fun fontMedium() = IqNanoGlobalConfigScreen.fontMedium()
        private fun fontSemi() = IqNanoGlobalConfigScreen.fontSemiBold()
        private fun fontBold() = IqNanoGlobalConfigScreen.fontBold()
        private fun colorFromArgb(argb: Int): Color {
            val a = ((argb ushr 24) and 255) / 255f
            val r = ((argb ushr 16) and 255) / 255f
            val g = ((argb ushr 8) and 255) / 255f
            val b = (argb and 255) / 255f
            return Color.of(r, g, b, a)
        }

        private fun colorFromRgb(rgb: Int): Color {
            val r = ((rgb ushr 16) and 255) / 255f
            val g = ((rgb ushr 8) and 255) / 255f
            val b = (rgb and 255) / 255f
            return Color.of(r, g, b, 1f)
        }

        private fun argb(a: Int, r: Int, g: Int, b: Int): Int =
            ((a and 255) shl 24) or ((r and 255) shl 16) or ((g and 255) shl 8) or (b and 255)
    }
}
