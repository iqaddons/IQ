package net.iqaddons.mod.screen.nano

import net.iqaddons.mod.config.screen.KuudraWaypointEditorStore
import net.iqaddons.mod.manager.IQPersistentDataStore
import net.iqaddons.mod.nanovg.IqNanoVg
import net.iqaddons.mod.nanovg.IqNanoVgRenderable
import net.iqaddons.mod.nanovg.rendering.backend.Color
import net.iqaddons.mod.nanovg.rendering.backend.Paint
import net.iqaddons.mod.nanovg.rendering.backend.TextAlign
import net.iqaddons.mod.nanovg.rendering.renderer.Renderer2D
import net.iqaddons.mod.nanovg.util.CornerRadius
import net.iqaddons.mod.nanovg.util.Point
import net.iqaddons.mod.nanovg.util.Rect
import net.iqaddons.mod.utils.MessageUtil
import net.iqaddons.mod.utils.data.DataKey
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.util.Util
import org.lwjgl.glfw.GLFW
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import java.util.Locale

open class IqNanoKuudraWaypointsScreen(
    private val parent: Screen?,
) : Screen(Component.literal(IqNanoGlobalConfigScreen.configScreenTitle("Kuudra Waypoints"))), IqNanoVgRenderable {

    private data class Hit(val rect: Rect, val action: () -> Unit)
    private data class ItemHit(val index: Int, val rect: Rect)
    private data class DelaySliderHit(val item: KuudraWaypointEditorStore.Item, val rect: Rect)
    private data class CoordinateEdit(val main: Int, val sub: Int, val item: Int, val axis: Int)
    private data class ValueEdit(val main: Int, val sub: Int, val item: Int, val field: String)
    private data class RenameEdit(val main: Int, val sub: Int, val item: Int?)

    private val store = KuudraWaypointEditorStore()
    private val sessionStore = IQPersistentDataStore.get()
    private val hits = mutableListOf<Hit>()
    private val itemHits = mutableListOf<ItemHit>()
    private val delaySliderHits = mutableListOf<DelaySliderHit>()
    private var selectedMain = 0
    private var selectedSub = 0
    private var selectedItem = 0
    private var itemScroll = 0f
    private var draggingDelaySlider: DelaySliderHit? = null
    private var mouseXf = 0f
    private var mouseYf = 0f
    private var panel = Rect.ZERO
    private var time = 0f
    private var openProgress = 0f
    private var hoverTooltip: Pair<Rect, String>? = null
    private var editingCoordinate: CoordinateEdit? = null
    private val coordinateText = StringBuilder()
    private var editingValue: ValueEdit? = null
    private val valueText = StringBuilder()
    private var editingRename: RenameEdit? = null
    private val renameText = StringBuilder()
    private var pearlExtraEditor = ""
    private var saveFlash = 0f
    private var updateWaypointsConfirmUntil = 0f

    override fun init() {
        IqNanoGlobalConfigScreen.load()
        store.load()
        restoreUiSession()
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
        openProgress = approach(openProgress, 1f, 0.20f)
        saveFlash = approach(saveFlash, 0f, 0.12f)
        hits.clear()
        itemHits.clear()
        delaySliderHits.clear()
        hoverTooltip = null

        val sw = width.toFloat()
        val sh = height.toFloat()
        val compactViewport = sw < 980f || sh < 620f
        val margin = if (compactViewport) 10f else 18f
        val scale = IqNanoGlobalConfigScreen.getSharedUiScale().toFloat().coerceIn(0.72f, 1.35f)
        val panelW = min(max(max(760f, sw * 0.78f) * scale, min(620f, sw - margin * 2f)), sw - margin * 2f)
        val panelH = if (compactViewport) {
            min(max(360f, sh * 0.82f), sh - margin * 2f)
        } else {
            min(max(500f, sh * 0.74f), sh - margin * 2f)
        }
        panel = Rect((sw - panelW) / 2f, (sh - panelH) / 2f, panelW, panelH)

        renderer.fillRect(Rect(0f, 0f, sw, sh), Color.of(0f, 0f, 0f, 0.62f))
        renderer.withOpacity(openProgress.coerceIn(0f, 1f)) {
            renderer.withTransform(
                dx = panel.centerX * (1f - (0.965f + 0.035f * openProgress)),
                dy = panel.centerY * (1f - (0.965f + 0.035f * openProgress)) + (1f - openProgress) * 10f,
                sx = 0.965f + 0.035f * openProgress,
                sy = 0.965f + 0.035f * openProgress,
            ) {
                drawChrome(renderer)
                drawHeader(renderer)
                drawBody(renderer)
                drawBorder(renderer)
                hoverTooltip?.let { drawTooltip(renderer, it.first, it.second) }
            }
        }
    }

    private fun drawChrome(r: Renderer2D) {
        val opacity = IqNanoGlobalConfigScreen.getSharedGuiOpacity().toFloat().coerceIn(0.35f, 1f)
        r.backend.fillRect(
            panel,
            Paint.LinearGradient(
                Point(panel.x, panel.y),
                Point(panel.right, panel.bottom),
                panelStart().withAlpha((opacity * 0.92f).coerceIn(0.35f, 0.98f)),
                panelEnd().withAlpha((opacity * 0.98f).coerceIn(0.42f, 1f)),
            ),
            CornerRadius.uniform(10f),
        )
        IqNanoGlobalConfigScreen.brandBackgroundPath()?.let { path ->
            IqNanoVg.textures.textureFor(path)?.let { tex ->
                r.withClip(panel) {
                    r.withOpacity(0.16f) { r.backend.drawImage(tex, panel, null, CornerRadius.uniform(10f)) }
                    r.fillRect(panel, panelEnd().withAlpha(0.22f), CornerRadius.uniform(10f))
                }
            }
        }
        r.fillRect(Rect(panel.x, panel.y + 45f, panel.width, 1f), accent().withAlpha(0.58f))
    }

    private fun drawHeader(r: Renderer2D) {
        val logo = Rect(panel.x + 16f, panel.y + 12f, 24f, 24f)
        IqNanoVg.textures.textureFor(IqNanoGlobalConfigScreen.brandIconPath())?.let { tex ->
            r.backend.drawImage(tex, logo, null, CornerRadius.uniform(7f))
        }
        r.strokeRect(logo.inset(-1.6f), accent().withAlpha(0.38f), 1.2f, CornerRadius.uniform(8f))
        r.text(IqNanoGlobalConfigScreen.brandName(), logo.right + 9f, panel.y + 10f, 15.5f, text(), fontBold())
        r.text("KUUDRA WAYPOINTS (W.I.P)", logo.right + 9f, panel.y + 27f, 8.8f, muted(), fontSemi())
        val buttonGap = 7f
        val sideW = 76f
        val updateW = 148f
        val actionY = panel.y + 15f
        val actionH = 18f
        val totalW = sideW * 2f + updateW + buttonGap * 2f
        val actionX = panel.centerX - totalW / 2f
        headerButton(
            r,
            Rect(actionX, actionY, sideW, actionH),
            "IMPORT",
            "Import waypoint preset from clipboard\nReplaces current waypoint setup"
        ) { importWaypointPresetFromClipboard() }
        updateWaypointsButton(
            r,
            Rect(actionX + sideW + buttonGap, actionY, updateW, actionH),
        ) {
            val armed = updateWaypointsConfirmUntil > time
            if (!armed) {
                updateWaypointsConfirmUntil = time + 3f
            } else {
                updateWaypointsConfirmUntil = 0f
                store.updateWaypointsFromBundledDefaults()
                store.load()
                selectedMain = 0
                selectedSub = 0
                selectedItem = 0
                itemScroll = 0f
                pearlExtraEditor = ""
                saveFlash = 1f
                saveUiSession()
            }
        }
        headerButton(
            r,
            Rect(actionX + sideW + buttonGap + updateW + buttonGap, actionY, sideW, actionH),
            "EXPORT",
            "Copy your waypoint preset\nShare it with another player"
        ) { exportWaypointPresetToClipboard() }
        iconButton(r, Rect(panel.right - 54f, panel.y + 14f, 18f, 18f), "chevron-left", "Back") {
            updateWaypointsConfirmUntil = 0f
            saveUiSession()
            minecraft?.setScreen(parent)
        }
        iconButton(r, Rect(panel.right - 30f, panel.y + 14f, 18f, 18f), "close", "Close") { onClose() }
    }

    private fun drawBody(r: Renderer2D) {
        val compact = panel.width < 900f || panel.height < 430f
        val pad = if (compact) 10f else 14f
        val gap = if (compact) 6f else 8f
        val body = Rect(panel.x + pad, panel.y + 57f, panel.width - pad * 2f, panel.height - 67f)
        val columnW = (body.width - gap * 3f) / 4f
        val mainW = columnW
        val subW = columnW
        val listW = columnW
        val detailW = columnW
        val main = Rect(body.x, body.y, mainW, body.height)
        val sub = Rect(main.right + gap, body.y, subW, body.height)
        val list = Rect(sub.right + gap, body.y, listW, body.height)
        val detail = Rect(list.right + gap, body.y, detailW, body.height)
        drawColumn(r, main, "Categories")
        drawColumn(r, sub, "Subcategories")
        drawColumn(r, list, "Waypoints")
        drawColumn(r, detail, "Editor")
        drawMainCategories(r, main)
        drawSubcategories(r, sub)
        drawItems(r, list)
        drawDetails(r, detail)
    }

    private fun drawColumn(r: Renderer2D, rect: Rect, title: String) {
        r.fillRect(rect, surface().withAlpha(0.56f), CornerRadius.uniform(6f))
        r.strokeRect(rect, border().withAlpha(0.22f), 1f, CornerRadius.uniform(6f))
        r.text(title.uppercase(), rect.x + 10f, rect.y + 8f, 8.4f, muted(), fontSemi())
        r.fillRect(Rect(rect.x + 8f, rect.y + 24f, rect.width - 16f, 1f), Color.of(1f, 1f, 1f, 0.08f))
    }

    private fun drawMainCategories(r: Renderer2D, rect: Rect) {
        val compact = rect.height < 330f
        var y = rect.y + 34f
        val rowH = if (compact) 21f else 24f
        val rowStep = if (compact) 25f else 29f
        store.mainCategories().forEachIndexed { index, label ->
            val row = Rect(rect.x + 8f, y, rect.width - 16f, rowH)
            navRow(r, row, label, index == selectedMain, store.isDefaultMainCategory(index), visible = store.isMainVisible(index)) {
                selectedMain = index
                selectedSub = 0
                selectedItem = 0
                pearlExtraEditor = ""
                itemScroll = 0f
            }
            visibilityButton(r, row, store.isMainVisible(index), "Show or hide this waypoint category") {
                store.toggleMainVisibility(index)
                store.load()
            }
            y += rowStep
        }
        textButton(r, Rect(rect.x + 8f, rect.bottom - 28f, rect.width - 16f, 18f), "RESET CATEGORY", "Restore every default entry\nCustom entries in this category can be removed separately") {
            store.resetMainCategory(selectedMain)
            store.load()
            selectedSub = 0
            selectedItem = 0
            pearlExtraEditor = ""
        }
    }

    private fun drawSubcategories(r: Renderer2D, rect: Rect) {
        val subs = store.subcategories(selectedMain)
        selectedSub = selectedSub.coerceIn(0, (subs.size - 1).coerceAtLeast(0))
        val compact = rect.height < 330f
        var y = rect.y + 34f
        val rowH = if (compact) 20f else 22f
        val rowStep = if (compact) 23f else 26f
        subs.forEachIndexed { index, label ->
            val row = Rect(rect.x + 8f, y, rect.width - 16f, rowH)
            val display = if (editingRename?.let { it.main == selectedMain && it.sub == index && it.item == null } == true) renameText.toString() else label
            val renaming = editingRename?.let { it.main == selectedMain && it.sub == index && it.item == null } == true
            navRow(r, row, display, index == selectedSub, store.isDefaultSubcategory(selectedMain, label), renaming, store.isSubcategoryVisible(selectedMain, label)) {
                selectedSub = index
                selectedItem = 0
                pearlExtraEditor = ""
                itemScroll = 0f
            }
            visibilityButton(r, row, store.isSubcategoryVisible(selectedMain, label), "Show or hide this waypoint group") {
                store.toggleSubcategoryVisibility(selectedMain, label)
                store.load()
            }
            y += rowStep
        }
        val deleteCustom = Rect(rect.x + 8f, rect.bottom - 52f, rect.width - 16f, 17f)
        val currentSub = subs.getOrNull(selectedSub).orEmpty()
        if (!store.isDefaultSubcategory(selectedMain, currentSub)) {
            val gap = 7f
            val half = (rect.width - 16f - gap) / 2f
            textButton(r, Rect(rect.x + 8f, deleteCustom.y, half, deleteCustom.height), "Rename Custom", "Rename this custom subcategory\nDefault subcategories keep their fixed names") {
                beginSubcategoryRename(currentSub)
            }
            textButton(r, Rect(rect.x + 8f + half + gap, deleteCustom.y, half, deleteCustom.height), "Delete Custom", "Delete this custom subcategory\nAll waypoints inside it are removed") {
                store.removeCustomSubcategory(selectedMain, currentSub)
                store.load()
                selectedSub = selectedSub.coerceAtMost((store.subcategories(selectedMain).size - 1).coerceAtLeast(0))
                selectedItem = 0
            }
        }
        footerButtons(r, rect, "Reset", "New Sub", "Restore defaults for this subcategory\nOnly affects the selected group", "Create a new custom subcategory\nUse it to organize custom waypoints",
            {
            val sub = store.subcategories(selectedMain).getOrNull(selectedSub).orEmpty()
            store.resetSubcategory(selectedMain, sub)
            store.load()
            selectedItem = 0
            },
            {
            store.addSubcategory(selectedMain, defaultSubName())
            store.load()
            selectedSub = (store.subcategories(selectedMain).size - 1).coerceAtLeast(0)
            selectedItem = 0
            }
        )
    }

    private fun drawItems(r: Renderer2D, rect: Rect) {
        val subs = store.subcategories(selectedMain)
        val sub = subs.getOrNull(selectedSub).orEmpty()
        val items = store.items(selectedMain, sub)
        selectedItem = selectedItem.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        val compact = rect.height < 330f
        val rowH = if (compact) 22f else 25f
        val rowStep = if (compact) 25f else 29f
        val clip = Rect(rect.x + 6f, rect.y + 30f, rect.width - 12f, rect.height - if (compact) 58f else 64f)
        var y = clip.y - itemScroll
        r.withClip(clip) {
            items.forEachIndexed { index, item ->
                val row = Rect(clip.x, y, clip.width, rowH)
                if (row.bottom >= clip.y && row.y <= clip.bottom) {
                    val active = index == selectedItem
                    val hover = row.contains(mouseXf, mouseYf)
                    r.fillRect(row, (if (active) accent() else button()).withAlpha(if (active) 0.30f else if (hover) 0.48f else 0.30f), CornerRadius.uniform(4f))
                    r.strokeRect(row, accent().withAlpha(if (active) 0.54f else 0.18f), 1f, CornerRadius.uniform(4f))
                    val defaultItem = store.isDefaultItem(item)
                    val renaming = editingRename?.let { it.main == selectedMain && it.sub == selectedSub && it.item == index } == true
                    val title = if (renaming) renameText.toString() else item.title()
                    if (renaming) r.strokeRect(row.inset(-1f), accent().withAlpha(0.35f + editPulse(true) * 0.32f), 1.2f, CornerRadius.uniform(5f))
                    val visible = store.isItemVisible(item)
                    val titleMax = row.width - if (defaultItem) 78f else 34f
                    val displayTitle = fit(r, title, titleMax, 9.2f)
                    r.text(displayTitle, row.x + 25f, row.y + 7.4f, 9.2f, (if (active) text() else muted()).withAlpha(if (visible) 1f else 0.45f), fontSemi())
                    if (renaming) editPill(r, editPillRect(row, defaultItem))
                    if (renaming) {
                        val cursorX = (row.x + 27f + r.textWidth(displayTitle, 9.2f, fontSemi())).coerceAtMost(editPillRect(row, defaultItem).x - 5f)
                        textCursor(r, cursorX, row.centerY - 5.4f, 10.8f)
                    }
                    badge(r, row, if (defaultItem) "Default" else "Custom", defaultItem)
                    visibilityButton(r, row, visible, "Show or hide this waypoint") {
                        store.toggleItemVisibility(item)
                        store.load()
                    }
                    itemHits += ItemHit(index, row)
                    if (hover) hoverTooltip = row to item.info()
                }
                y += rowStep
            }
        }
        val maxScroll = (y + itemScroll - clip.bottom).coerceAtLeast(0f)
        itemScroll = itemScroll.coerceIn(0f, maxScroll)
        val add = Rect(rect.x + 8f, rect.bottom - 28f, rect.width - 16f, 18f)
        textButton(r, add, if (sub == "Areas") "New Area" else "New Waypoint", if (sub == "Areas") "Create a custom Pearl area\nBounds can be edited in the editor" else "Create a waypoint in this subcategory\nIt appears immediately in the selected group") {
            if ((selectedMain == 0 || selectedMain == 1) && sub == "Areas") {
                store.addArea(selectedMain, "custom area")
            } else {
                store.addWaypoint(selectedMain, sub)
            }
            store.load()
            selectedItem = 0
        }
    }

    private fun drawDetails(r: Renderer2D, rect: Rect) {
        val subs = store.subcategories(selectedMain)
        val items = store.items(selectedMain, subs.getOrNull(selectedSub).orEmpty())
        val item = items.getOrNull(selectedItem)
        if (item == null) {
            r.text("No waypoint selected", rect.x + 14f, rect.y + 42f, 11f, muted(), fontSemi())
            return
        }
        val compact = rect.height < 350f || rect.width < 190f
        val pad = if (compact) 9f else 14f
        val gap = if (compact) 5f else 8f
        val fieldH = if (compact) 16f else 18f
        val buttonH = if (compact) 15f else 17f
        val saveH = if (compact) 18f else 19f
        val buttonLabelSize = if (compact) 7.7f else 8.2f
        val title = if (editingRename?.let { it.main == selectedMain && it.sub == selectedSub && it.item == selectedItem } == true) renameText.toString() else item.title()
        val titleSize = if (compact) {
            if (r.textWidth(title, 11.5f, fontBold()) > rect.width - pad * 2f) 10.2f else 11.5f
        } else if (r.textWidth(title, 13f, fontBold()) > rect.width - pad * 2f) 11.2f else 13f
        var cursorY = rect.y + if (compact) 31f else 36f
        r.text(fit(r, title, rect.width - pad * 2f, titleSize), rect.x + pad, cursorY, titleSize, text(), fontBold())
        cursorY += if (compact) 17f else 22f
        val infoLines = item.info().split('\n')
        val infoCount = if (compact) 4 else 5
        val infoStep = if (compact) 9f else 11f
        infoLines.take(infoCount).forEachIndexed { index, line ->
            r.text(line, rect.x + pad, cursorY + index * infoStep, if (compact) 7.8f else 8.8f, muted(), fontRegular())
        }
        cursorY += infoLines.take(infoCount).size * infoStep
        cursorY += if (compact) 7f else 118f

        val saveRect = Rect(rect.x + pad, rect.bottom - saveH - 7f, rect.width - pad * 2f, saveH)
        val maxControlBottom = saveRect.y - if (compact) 5f else 7f
        val controlsMinY = rect.y + if (compact) 116f else 150f
        val coordinateRows = if (store.editableCoordinates(item)) {
            if (store.isPearlArea(item)) 4 else 3
        } else 0
        val extraBlockH = if (store.isCustomPearlWaypoint(item) && pearlExtraEditor.isNotBlank()) {
            if (pearlExtraEditor == "DELAY") 37f else 35f
        } else 0f
        val coordinateBlockH = if (coordinateRows > 0) 12f + coordinateRows * fieldH + (coordinateRows - 1) * 5f + 5f else 0f
        val buttons = detailButtons(item)
        val buttonRows = ((buttons.size + 1) / 2).coerceAtLeast(1)
        val buttonBlockH = 12f + buttonRows * buttonH + (buttonRows - 1) * gap
        val neededControlsH = extraBlockH + coordinateBlockH + buttonBlockH
        val controlsTopMax = (maxControlBottom - neededControlsH).coerceAtLeast(rect.y + 82f)
        cursorY = if (controlsTopMax >= controlsMinY) {
            cursorY.coerceIn(controlsMinY, controlsTopMax)
        } else {
            controlsTopMax
        }

        if (store.isCustomPearlWaypoint(item) && pearlExtraEditor.isNotBlank()) {
            if (pearlExtraEditor == "SIZE") {
                r.text("Size", rect.x + pad, cursorY, 8.3f, muted(), fontSemi())
                cursorY += 12f
                pearlValueField(r, Rect(rect.x + pad, cursorY, rect.width - pad * 2f, fieldH), item, "SIZE")
                cursorY += fieldH + 8f
            } else if (pearlExtraEditor == "DELAY") {
                r.text("Delay", rect.x + pad, cursorY, 8.3f, muted(), fontSemi())
                cursorY += 12f
                delaySlider(r, Rect(rect.x + pad, cursorY, rect.width - pad * 2f, 20f), item)
                cursorY += 25f
            }
        }

        if (store.editableCoordinates(item)) {
            r.text(store.coordinateLabel(item), rect.x + pad, cursorY, 8.3f, muted(), fontSemi())
            cursorY += 12f
            val fieldW = rect.width - pad * 2f
            for (axis in 0 until coordinateRows) {
                coordinateField(r, Rect(rect.x + pad, cursorY + (fieldH + 5f) * axis, fieldW, fieldH), item, axis, coordinateAxisLabel(item, axis))
            }
            cursorY += fieldH * coordinateRows + 5f * coordinateRows
        }

        r.text("Buttons", rect.x + pad, cursorY, 8.3f, muted(), fontSemi())
        cursorY += 12f
        val actionW = ((rect.width - pad * 2f - gap) / 2f).coerceAtLeast(44f)
        buttons.forEachIndexed { index, button ->
            val bx = rect.x + pad + (index % 2) * (actionW + gap)
            val by = cursorY + (index / 2) * (buttonH + gap)
            textButton(r, Rect(bx, by, actionW, buttonH), button.first, button.second, buttonLabelSize, button.third)
        }
        val dirty = store.hasUnsavedChanges()
        val saveLabel = if (dirty) "Save Changes *" else "Saved"
        primaryButton(r, saveRect, saveLabel, "Apply pending edits to JSON\nChanges only affect waypoints after saving", dirty) {
            store.saveChanges()
            store.load()
            saveFlash = 1f
        }
    }

    private fun detailButtons(item: KuudraWaypointEditorStore.Item): List<Triple<String, String, () -> Unit>> {
        val buttons = mutableListOf<Triple<String, String, () -> Unit>>()
        if (store.isPearlArea(item)) {
            buttons += Triple("Rename", "Rename this area\nUsed by Pearl/Etherwarp area matching") { beginItemRename(item) }
            buttons += Triple("Reset", "Restore default area bounds\nOnly available for built-in areas") { store.resetItem(item); store.load() }
            if (store.canRemove(item)) {
                buttons += Triple("Delete", "Delete this custom area\nAll waypoints inside it are removed") {
                    store.remove(item)
                    store.load()
                    selectedItem = (selectedItem - 1).coerceAtLeast(0)
                }
            } else {
                buttons += Triple("Hide", "Hide this default area\nDefaults can be shown again with the eye toggle") {
                    store.toggleItemVisibility(item)
                    store.load()
                }
            }
            return buttons
        }
        if (store.isPearlWaypoint(item)) {
            if (store.isCustomPearlWaypoint(item)) {
                buttons += Triple("Size", "Edit this custom pearl waypoint size\nPress ENTER to confirm the value, then Save Changes") { togglePearlExtra("SIZE") }
                buttons += Triple("Delay", "Adjust this custom pearl timer\nNegative = slower/later\nPositive = faster/earlier") { togglePearlExtra("DELAY") }
            }
            buttons += Triple("Area", "Move this pearl waypoint to the next area\nUseful when reorganizing default spots") {
                val sub = store.subcategories(selectedMain).getOrNull(selectedSub).orEmpty()
                store.moveToNextPearlArea(item)
                store.load()
                val movedIndex = store.indexOfItem(selectedMain, sub, item)
                selectedItem = if (movedIndex >= 0) {
                    movedIndex
                } else {
                    selectedItem.coerceAtMost((store.items(selectedMain, sub).size - 1).coerceAtLeast(0))
                }
            }
            buttons += Triple("Marker", "Switch marker behavior\nTarget is aim-focused, Solid is block-focused") { store.cycleMarkerStyle(item); store.load() }
        } else if (store.isStandBlock(item)) {
            buttons += Triple("Marker", "Switch stand block marker behavior\nUse Solid for physical block highlights") { store.cycleMarkerStyle(item); store.load() }
        } else {
            buttons += Triple("Phase", "Cycle the phase where this waypoint renders\nUsed by DPS, SKIP and custom etherwarp entries") { store.cyclePhase(item); store.load() }
            buttons += Triple("Render", "Cycle the 3D render style\nOutline is cleaner, Solid is more visible") { store.cycleRenderStyle(item); store.load() }
            buttons += Triple("Marker", "Switch marker behavior\nTarget is aim-focused, Solid is block-focused") { store.cycleMarkerStyle(item); store.load() }
            buttons += Triple("Color", "Cycle through preset waypoint colors\nUseful for separating multiple spots quickly") { store.cycleColor(item); store.load() }
        }
        buttons += Triple("Rename", "Rename the selected waypoint\nChanges the list label and rendered text when supported") { beginItemRename(item) }
        if (store.isPearlWaypoint(item)) {
            buttons += Triple("Style", "Cycle pearl type\nFlat, Sky or Double") {
                val currentSub = store.subcategories(selectedMain).getOrNull(selectedSub).orEmpty()
                store.cyclePearlStyle(item)
                val nextSub = store.pearlStyleSubcategory(item)
                store.load()
                if (currentSub == "Flat Pearls" || currentSub == "Sky Pearls" || currentSub == "Double Pearls") {
                    selectedSub = store.subcategories(selectedMain).indexOf(nextSub).takeIf { it >= 0 } ?: selectedSub
                }
                val sub = store.subcategories(selectedMain).getOrNull(selectedSub).orEmpty()
                val styledIndex = store.indexOfItem(selectedMain, sub, item)
                if (styledIndex >= 0) selectedItem = styledIndex
            }
        } else {
            buttons += Triple("Copy", "Duplicate this waypoint\nCreates a custom copy in the same group") { store.duplicate(item); store.load(); selectedItem++; Unit }
        }
        buttons += Triple("Reset", "Restore this waypoint from defaults\nCustom waypoints without defaults are unchanged") { store.resetItem(item); store.load() }
        if (store.canRemove(item)) {
            buttons += Triple("Delete", "Delete this custom waypoint") {
                store.remove(item)
                store.load()
                selectedItem = (selectedItem - 1).coerceAtLeast(0)
            }
        } else {
            buttons += Triple("Hide", "Hide this default waypoint\nDefaults can be shown again with the eye toggle") {
                store.toggleItemVisibility(item)
                store.load()
            }
        }
        return buttons
    }

    private fun coordinateAxisLabel(item: KuudraWaypointEditorStore.Item, axis: Int): String {
        if (!store.isPearlArea(item)) {
            return when (axis) {
                0 -> "X"
                1 -> "Y"
                else -> "Z"
            }
        }
        return when (axis) {
            0 -> "X1"
            1 -> "Z1"
            2 -> "X2"
            else -> "Z2"
        }
    }

    private fun navRow(r: Renderer2D, rect: Rect, label: String, active: Boolean, default: Boolean, editing: Boolean = false, visible: Boolean = true, action: () -> Unit) {
        val hover = rect.contains(mouseXf, mouseYf)
        if (active || hover) {
            r.fillRect(rect, accent().withAlpha((if (active) 0.32f else 0.13f) * if (visible) 1f else 0.62f), CornerRadius.uniform(4f))
            r.strokeRect(rect, accent().withAlpha((if (active) 0.48f else 0.18f) * if (visible) 1f else 0.70f), 1f, CornerRadius.uniform(4f))
        }
        if (editing) r.strokeRect(rect.inset(-1f), accent().withAlpha(0.35f + editPulse(true) * 0.32f), 1.2f, CornerRadius.uniform(5f))
        r.text(fit(r, label, rect.width - if (default) 80f else 36f, 9.2f), rect.x + 25f, rect.y + 7f, 9.2f, (if (active) text() else muted()).withAlpha(if (visible) 1f else 0.45f), fontSemi())
        if (editing) editPill(r, editPillRect(rect, default))
        badge(r, rect, if (default) "Default" else "Custom", default)
        hits += Hit(rect, action)
    }

    private fun visibilityButton(r: Renderer2D, row: Rect, visible: Boolean, tooltip: String, action: () -> Unit) {
        val rect = Rect(row.x + 5f, row.centerY - 6.25f, 12.5f, 12.5f)
        val hover = rect.inset(-3f).contains(mouseXf, mouseYf)
        if (hover) r.fillRect(rect.inset(-2f), button().withAlpha(0.74f), CornerRadius.uniform(3f))
        val color = when {
            hover -> text()
            visible -> text()
            else -> muted()
        }
        r.icon(if (visible) "eye" else "eye-off", rect, color.withAlpha(if (visible) 0.96f else 0.62f))
        if (hover) hoverTooltip = rect to "$tooltip\nHidden entries stay editable here."
        hits += Hit(rect.inset(-4f), action)
    }

    private fun iconButton(r: Renderer2D, rect: Rect, icon: String, tooltip: String, action: () -> Unit) {
        val hover = rect.contains(mouseXf, mouseYf)
        r.fillRect(rect, button().withAlpha(if (hover) 0.82f else 0.46f), CornerRadius.uniform(4f))
        r.strokeRect(rect, accent().withAlpha(if (hover) 0.78f else 0.36f), 1f, CornerRadius.uniform(4f))
        r.icon(icon, rect.inset(rect.width * 0.27f), if (hover) text() else muted())
        if (hover) hoverTooltip = rect to tooltip
        hits += Hit(rect, action)
    }

    private fun coordinateField(r: Renderer2D, rect: Rect, item: KuudraWaypointEditorStore.Item, axis: Int, label: String) {
        val editing = editingCoordinate?.let { it.main == selectedMain && it.sub == selectedSub && it.item == selectedItem && it.axis == axis } == true
        val hover = rect.contains(mouseXf, mouseYf)
        val pulse = editPulse(editing)
        r.fillRect(rect, button().withAlpha(if (editing) 0.66f + pulse * 0.14f else if (hover) 0.62f else 0.42f), CornerRadius.uniform(4f))
        r.strokeRect(rect, accent().withAlpha(if (editing) 0.62f + pulse * 0.25f else if (hover) 0.62f else 0.30f), 1f, CornerRadius.uniform(4f))
        val value = if (editing) coordinateText.toString() else String.format(Locale.US, "%.1f", store.coordinate(item, axis))
        r.text(label, rect.x + 6f, rect.y + 5.7f, 7.5f, muted(), fontSemi())
        val display = fit(r, value, rect.width - if (editing) 72f else 38f, 8f)
        r.text(display, rect.right - 5f, rect.y + 5.7f, 8f, text(), fontSemi(), TextAlign.RIGHT)
        if (editing) {
            editPill(r, Rect(rect.x + 20f, rect.centerY - 6f, 34f, 12f))
            textCursor(r, rect.right - 3.4f, rect.centerY - 5.2f, 10.4f)
        }
        if (hover) hoverTooltip = rect to "Edit exact $label coordinate\nPress ENTER to confirm"
        hits += Hit(rect) { beginCoordinateEdit(item, axis) }
    }

    private fun pearlValueField(r: Renderer2D, rect: Rect, item: KuudraWaypointEditorStore.Item, field: String) {
        val editing = editingValue?.let { it.main == selectedMain && it.sub == selectedSub && it.item == selectedItem && it.field == field } == true
        val hover = rect.contains(mouseXf, mouseYf)
        val pulse = editPulse(editing)
        r.fillRect(rect, button().withAlpha(if (editing) 0.66f + pulse * 0.14f else if (hover) 0.62f else 0.42f), CornerRadius.uniform(4f))
        r.strokeRect(rect, accent().withAlpha(if (editing) 0.62f + pulse * 0.25f else if (hover) 0.62f else 0.30f), 1f, CornerRadius.uniform(4f))
        val value = if (editing) valueText.toString() else String.format(Locale.US, "%.3f", store.pearlSize(item))
        r.text("Value", rect.x + 6f, rect.y + 5.7f, 7.5f, muted(), fontSemi())
        val display = fit(r, value, rect.width - if (editing) 82f else 42f, 8f)
        r.text(display, rect.right - 5f, rect.y + 5.7f, 8f, text(), fontSemi(), TextAlign.RIGHT)
        if (editing) {
            editPill(r, Rect(rect.x + 42f, rect.centerY - 6f, 34f, 12f))
            textCursor(r, rect.right - 3.4f, rect.centerY - 5.2f, 10.4f)
        }
        if (hover) hoverTooltip = rect to "Edit custom pearl size\nPress ENTER to confirm"
        hits += Hit(rect) { beginValueEdit(item, field) }
    }

    private fun delaySlider(r: Renderer2D, rect: Rect, item: KuudraWaypointEditorStore.Item) {
        val value = store.pearlDelay(item).toFloat().coerceIn(-100f, 100f)
        val hover = rect.contains(mouseXf, mouseYf)
        val track = Rect(rect.x, rect.centerY - 2f, rect.width, 4f)
        val zeroX = track.x + track.width * 0.5f
        val knobX = track.x + ((value + 100f) / 200f) * track.width
        r.fillRect(track, button().withAlpha(if (hover) 0.72f else 0.50f), CornerRadius.uniform(3f))
        r.fillRect(Rect(zeroX - 0.7f, track.y - 4f, 1.4f, 12f), muted().withAlpha(0.55f), CornerRadius.uniform(1f))
        val fillX = min(zeroX, knobX)
        val fillW = max(1f, kotlin.math.abs(knobX - zeroX))
        r.fillRect(Rect(fillX, track.y, fillW, track.height), accent().withAlpha(0.72f), CornerRadius.uniform(3f))
        r.fillRect(Rect(knobX - 5f, rect.centerY - 5f, 10f, 10f), accent().withAlpha(0.86f), CornerRadius.uniform(5f))
        r.strokeRect(Rect(knobX - 5f, rect.centerY - 5f, 10f, 10f), text().withAlpha(0.85f), 0.8f, CornerRadius.uniform(5f))
        r.text("-100", rect.x, rect.y, 6.8f, muted(), fontSemi())
        r.text("0", zeroX, rect.y, 6.8f, muted(), fontSemi(), TextAlign.CENTER)
        r.text("+100", rect.right, rect.y, 6.8f, muted(), fontSemi(), TextAlign.RIGHT)
        r.text("${value.toInt()} ticks", rect.centerX, rect.bottom - 2f, 7.5f, text(), fontSemi(), TextAlign.CENTER)
        if (hover) hoverTooltip = rect to "Custom pearl delay offset\nNegative = slower/later\nPositive = faster/earlier\nDrag, then Save Changes"
        delaySliderHits += DelaySliderHit(item, rect.inset(-5f))
    }

    private fun headerButton(r: Renderer2D, rect: Rect, label: String, tooltip: String, action: () -> Unit) {
        val hover = rect.contains(mouseXf, mouseYf)
        r.fillRect(rect, button().withAlpha(if (hover) 0.66f else 0.38f), CornerRadius.uniform(4f))
        r.strokeRect(rect, accent().withAlpha(if (hover) 0.70f else 0.34f), 1f, CornerRadius.uniform(4f))
        r.text(fit(r, label, rect.width - 8f, 7.6f), rect.centerX, rect.centerY - 3.1f, 7.6f, if (hover) text() else muted(), fontBold(), TextAlign.CENTER)
        if (hover) hoverTooltip = rect to tooltip
        hits += Hit(rect, action)
    }

    private fun textButton(r: Renderer2D, rect: Rect, label: String, tooltip: String, fontSize: Float = 8.2f, action: () -> Unit) {
        val hover = rect.contains(mouseXf, mouseYf)
        r.fillRect(rect, button().withAlpha(if (hover) 0.78f else 0.46f), CornerRadius.uniform(4f))
        r.strokeRect(rect, accent().withAlpha(if (hover) 0.72f else 0.32f), 1f, CornerRadius.uniform(4f))
        r.text(fit(r, label, rect.width - 8f, fontSize), rect.centerX, rect.centerY - fontSize * 0.42f, fontSize, if (hover) text() else muted(), fontSemi(), TextAlign.CENTER)
        if (hover) hoverTooltip = rect to tooltip
        hits += Hit(rect, action)
    }

    private fun updateWaypointsButton(r: Renderer2D, rect: Rect, action: () -> Unit) {
        val hover = rect.contains(mouseXf, mouseYf)
        val armed = updateWaypointsConfirmUntil > time
        val remaining = (updateWaypointsConfirmUntil - time).coerceAtLeast(0f)
        val pulse = ((sin(time * 7.5f) + 1f) * 0.5f)
        val label = if (armed) "CONFIRM UPDATE ${ceil(remaining).toInt().coerceIn(1, 3)}" else "UPDATE WAYPOINTS"
        val fillAlpha = when {
            armed -> 0.42f + pulse * 0.18f
            hover -> 0.78f
            else -> 0.46f
        }
        val strokeAlpha = when {
            armed -> 0.74f + pulse * 0.18f
            hover -> 0.72f
            else -> 0.32f
        }
        r.fillRect(rect, (if (armed) accent() else button()).withAlpha(fillAlpha), CornerRadius.uniform(4f))
        r.strokeRect(rect, accent().withAlpha(strokeAlpha), if (armed) 1.15f else 1f, CornerRadius.uniform(4f))
        if (armed) r.strokeRect(rect.inset(-2f), accent().withAlpha(0.10f + pulse * 0.12f), 1f, CornerRadius.uniform(6f))
        r.text(fit(r, label, rect.width - 8f, 8.0f), rect.centerX, rect.centerY - 3.35f, 8.0f, if (armed || hover) text() else muted(), fontBold(), TextAlign.CENTER)
        if (hover) {
            hoverTooltip = rect to if (armed) {
                "Confirm default waypoint refresh\nCustom waypoints stay untouched\nMove away or wait to cancel"
            } else {
                "Refresh bundled default waypoints\nKeeps custom waypoints and groups\nRequires a second click"
            }
        }
        hits += Hit(rect, action)
    }

    private fun primaryButton(r: Renderer2D, rect: Rect, label: String, tooltip: String, dirty: Boolean = false, action: () -> Unit) {
        val hover = rect.contains(mouseXf, mouseYf)
        val pulse = if (dirty) ((sin(time * 5.2f) + 1f) * 0.5f) else saveFlash
        r.fillRect(rect, accent().withAlpha(if (hover) 0.64f else if (dirty) 0.56f + pulse * 0.10f else 0.34f + pulse * 0.18f), CornerRadius.uniform(4f))
        r.strokeRect(rect, accent().withAlpha(if (hover) 0.95f else if (dirty) 0.78f + pulse * 0.18f else 0.45f), 1.1f, CornerRadius.uniform(4f))
        if (dirty) r.icon("save", Rect(rect.x + 9f, rect.centerY - 4.5f, 9f, 9f), text())
        r.text(label, rect.centerX, rect.centerY - 3.8f, 8.8f, if (dirty) text() else muted(), fontBold(), TextAlign.CENTER)
        if (hover) hoverTooltip = rect to tooltip
        hits += Hit(rect, action)
    }

    private fun footerButtons(
        r: Renderer2D,
        rect: Rect,
        left: String,
        right: String,
        leftTip: String,
        rightTip: String,
        leftAction: () -> Unit,
        rightAction: () -> Unit,
    ) {
        val gap = 7f
        val w = (rect.width - 16f - gap) / 2f
        val y = rect.bottom - 28f
        textButton(r, Rect(rect.x + 8f, y, w, 18f), left, leftTip, action = leftAction)
        textButton(r, Rect(rect.x + 8f + w + gap, y, w, 18f), right, rightTip, action = rightAction)
    }

    private fun badge(r: Renderer2D, row: Rect, label: String, default: Boolean) {
        val w = if (label == "Default") 38f else 34f
        val rect = Rect(row.right - w - 7f, row.centerY - 6f, w, 12f)
        val color = if (default) accent() else muted()
        r.fillRect(rect, color.withAlpha(if (default) 0.18f else 0.10f), CornerRadius.uniform(3f))
        r.strokeRect(rect, color.withAlpha(if (default) 0.34f else 0.22f), 0.8f, CornerRadius.uniform(3f))
        r.text(label, rect.centerX, rect.y + 3.1f, 6.2f, color, fontSemi(), TextAlign.CENTER)
        if (rect.contains(mouseXf, mouseYf)) hoverTooltip = rect to if (default) "Default waypoint" else "Custom waypoint"
    }

    private fun editPillRect(row: Rect, default: Boolean): Rect {
        val badgeW = if (default) 38f else 34f
        val badgeLeft = row.right - badgeW - 7f
        return Rect(badgeLeft - 40f, row.centerY - 6f, 34f, 12f)
    }

    private fun drawTooltip(r: Renderer2D, source: Rect, tooltip: String) {
        val lines = tooltip.lines().filter { it.isNotBlank() }.take(7)
        val w = lines.maxOfOrNull { r.textWidth(it, 8f, fontRegular()) }?.plus(18f)?.coerceIn(112f, 280f) ?: 128f
        val h = 12f + lines.size * 10f
        val x = (mouseXf + 12f).coerceAtMost(width.toFloat() - w - 8f)
        val y = (mouseYf + 12f).coerceAtMost(height.toFloat() - h - 8f)
        val box = Rect(x, y, w, h)
        r.fillRect(box, modal().withAlpha(0.97f), CornerRadius.uniform(5f))
        r.strokeRect(box, accent().withAlpha(0.48f), 1f, CornerRadius.uniform(5f))
        lines.forEachIndexed { index, line -> r.text(line, box.x + 9f, box.y + 8f + index * 10f, 8f, text(), fontRegular()) }
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        mouseXf = click.x().toFloat()
        mouseYf = click.y().toFloat()
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(click, doubled)
        delaySliderHits.lastOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            draggingDelaySlider = it
            updateDelaySlider(it, mouseXf)
            return true
        }
        hits.lastOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            it.action()
            return true
        }
        itemHits.lastOrNull { it.rect.contains(mouseXf, mouseYf) }?.let {
            selectedItem = it.index.coerceAtLeast(0)
            editingCoordinate = null
            editingValue = null
            pearlExtraEditor = ""
            return true
        }
        editingCoordinate = null
        editingValue = null
        return super.mouseClicked(click, doubled)
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        editingRename?.let { edit ->
            return when (input.key()) {
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    commitRenameEdit(edit)
                    true
                }
                GLFW.GLFW_KEY_ESCAPE -> {
                    editingRename = null
                    renameText.clear()
                    true
                }
                GLFW.GLFW_KEY_BACKSPACE -> {
                    if (renameText.isNotEmpty()) renameText.deleteCharAt(renameText.length - 1)
                    true
                }
                else -> true
            }
        }
        editingValue?.let { edit ->
            return when (input.key()) {
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    commitValueEdit(edit)
                    true
                }
                GLFW.GLFW_KEY_ESCAPE -> {
                    editingValue = null
                    valueText.clear()
                    true
                }
                GLFW.GLFW_KEY_BACKSPACE -> {
                    if (valueText.isNotEmpty()) valueText.deleteCharAt(valueText.length - 1)
                    true
                }
                else -> true
            }
        }
        editingCoordinate?.let { edit ->
            return when (input.key()) {
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    commitCoordinateEdit(edit)
                    true
                }
                GLFW.GLFW_KEY_ESCAPE -> {
                    editingCoordinate = null
                    coordinateText.clear()
                    true
                }
                GLFW.GLFW_KEY_BACKSPACE -> {
                    if (coordinateText.isNotEmpty()) coordinateText.deleteCharAt(coordinateText.length - 1)
                    true
                }
                else -> true
            }
        }
        return super.keyPressed(input)
    }

    override fun charTyped(input: CharacterEvent): Boolean {
        val c = input.codepoint().toChar()
        editingRename?.let {
            if (!Character.isISOControl(c)) renameText.append(c)
            return true
        }
        editingValue?.let {
            if (c.isDigit() || c == '.' || c == '-' || c == ',') {
                valueText.append(if (c == ',') '.' else c)
            }
            return true
        }
        editingCoordinate ?: return super.charTyped(input)
        if (c.isDigit() || c == '.' || c == '-' || c == ',') {
            coordinateText.append(if (c == ',') '.' else c)
        }
        return true
    }

    override fun mouseDragged(click: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        draggingDelaySlider?.let {
            updateDelaySlider(it, click.x().toFloat())
            return true
        }
        return super.mouseDragged(click, offsetX, offsetY)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        draggingDelaySlider = null
        return super.mouseReleased(click)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        itemScroll = (itemScroll - vertical.toFloat() * 18f).coerceAtLeast(0f)
        return true
    }

    override fun onClose() {
        updateWaypointsConfirmUntil = 0f
        saveUiSession()
        minecraft?.setScreen(null)
    }

    override fun removed() {
        saveUiSession()
        super.removed()
    }

    override fun isPauseScreen(): Boolean = false

    private fun drawBorder(r: Renderer2D) {
        r.strokeRect(panel, border().withAlpha(0.82f), 1.45f, CornerRadius.uniform(10f))
        val pulse = ((sin(time * 1.5f * PI.toFloat()) + 1f) * 0.5f)
        r.strokeRect(panel.inset(1f), accent().withAlpha(0.12f + pulse * 0.08f), 0.85f, CornerRadius.uniform(9f))
    }

    private fun approach(current: Float, target: Float, amount: Float): Float = current + (target - current) * amount

    private fun restoreUiSession() {
        if (IqNanoGlobalConfigScreen.isSharedUiStatePersistenceEnabled()) {
            selectedMain = sessionStore.getOrDefault(UI_SESSION_MAIN, selectedMain)
            selectedSub = sessionStore.getOrDefault(UI_SESSION_SUB, selectedSub)
            selectedItem = sessionStore.getOrDefault(UI_SESSION_ITEM, selectedItem)
            itemScroll = sessionStore.getOrDefault(UI_SESSION_ITEM_SCROLL, java.lang.Double.valueOf(0.0)).toFloat()
            pearlExtraEditor = sessionStore.getOrDefault(UI_SESSION_EXTRA_EDITOR, pearlExtraEditor)
        } else {
            selectedMain = 0
            selectedSub = 0
            selectedItem = 0
            itemScroll = 0f
            pearlExtraEditor = ""
        }
        clampSelection()
    }

    private fun saveUiSession() {
        if (!IqNanoGlobalConfigScreen.isSharedUiStatePersistenceEnabled()) return
        clampSelection()
        sessionStore.set(UI_SESSION_MAIN, selectedMain)
        sessionStore.set(UI_SESSION_SUB, selectedSub)
        sessionStore.set(UI_SESSION_ITEM, selectedItem)
        sessionStore.set(UI_SESSION_ITEM_SCROLL, itemScroll.toDouble())
        sessionStore.set(UI_SESSION_EXTRA_EDITOR, pearlExtraEditor)
    }

    private fun clampSelection() {
        selectedMain = selectedMain.coerceIn(0, (store.mainCategories().size - 1).coerceAtLeast(0))
        val subs = store.subcategories(selectedMain)
        selectedSub = selectedSub.coerceIn(0, (subs.size - 1).coerceAtLeast(0))
        val items = store.items(selectedMain, subs.getOrNull(selectedSub).orEmpty())
        selectedItem = selectedItem.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        itemScroll = itemScroll.coerceAtLeast(0f)
    }

    private fun iconForMain(index: Int): String = when (index) {
        0 -> "crosshair"
        1 -> "zap"
        else -> "map-pin"
    }
    private fun defaultSubName(): String = when (selectedMain) {
        0 -> "Custom Pearls"
        1 -> "Custom Etherwarps"
        else -> "Custom Waypoints"
    }

    private fun exportWaypointPresetToClipboard() {
        val mc = minecraft ?: return
        runCatching {
            mc.keyboardHandler.clipboard = store.exportWaypointPreset()
        }.onSuccess {
            MessageUtil.SUCCESS.sendMessage("Waypoint preset copied to clipboard.")
        }.onFailure {
            MessageUtil.ERROR.sendMessage("Failed to export waypoint preset.")
        }
    }

    private fun importWaypointPresetFromClipboard() {
        val mc = minecraft ?: return
        val text = mc.keyboardHandler.clipboard
        if (text.isNullOrBlank()) {
            MessageUtil.ERROR.sendMessage("Clipboard is empty.")
            return
        }
        runCatching {
            store.importWaypointPreset(text)
        }.onSuccess {
            store.load()
            selectedMain = 0
            selectedSub = 0
            selectedItem = 0
            itemScroll = 0f
            pearlExtraEditor = ""
            saveFlash = 1f
            saveUiSession()
            MessageUtil.SUCCESS.sendMessage("Waypoint preset imported.")
        }.onFailure {
            MessageUtil.ERROR.sendMessage("Invalid waypoint preset.")
        }
    }

    private fun beginCoordinateEdit(item: KuudraWaypointEditorStore.Item, axis: Int) {
        editingRename = null
        editingValue = null
        editingCoordinate = CoordinateEdit(selectedMain, selectedSub, selectedItem, axis)
        coordinateText.clear()
        coordinateText.append(String.format(Locale.US, "%.1f", store.coordinate(item, axis)))
    }

    private fun beginValueEdit(item: KuudraWaypointEditorStore.Item, field: String) {
        editingRename = null
        editingCoordinate = null
        editingValue = ValueEdit(selectedMain, selectedSub, selectedItem, field)
        valueText.clear()
        if (field == "SIZE") valueText.append(String.format(Locale.US, "%.3f", store.pearlSize(item)))
    }

    private fun beginItemRename(item: KuudraWaypointEditorStore.Item) {
        editingCoordinate = null
        editingValue = null
        editingRename = RenameEdit(selectedMain, selectedSub, selectedItem)
        renameText.clear()
        renameText.append(item.title())
    }

    private fun beginSubcategoryRename(current: String) {
        editingCoordinate = null
        editingValue = null
        editingRename = RenameEdit(selectedMain, selectedSub, null)
        renameText.clear()
        renameText.append(current)
    }

    private fun commitRenameEdit(edit: RenameEdit) {
        if (edit.item == null) {
            val sub = store.subcategories(edit.main).getOrNull(edit.sub).orEmpty()
            store.renameSubcategory(edit.main, sub, renameText.toString())
        } else {
            val sub = store.subcategories(edit.main).getOrNull(edit.sub).orEmpty()
            val item = store.items(edit.main, sub).getOrNull(edit.item)
            if (item != null) store.renameItem(item, renameText.toString())
        }
        store.load()
        editingRename = null
        renameText.clear()
    }

    private fun commitCoordinateEdit(edit: CoordinateEdit) {
        coordinateText.toString().trim().replace(',', '.').toDoubleOrNull()?.let {
            val sub = store.subcategories(edit.main).getOrNull(edit.sub).orEmpty()
            val item = store.items(edit.main, sub).getOrNull(edit.item)
            if (item != null) {
                store.setCoordinate(item, edit.axis, it)
                store.load()
            }
        }
        editingCoordinate = null
        coordinateText.clear()
    }

    private fun commitValueEdit(edit: ValueEdit) {
        valueText.toString().trim().replace(',', '.').toDoubleOrNull()?.let {
            val sub = store.subcategories(edit.main).getOrNull(edit.sub).orEmpty()
            val item = store.items(edit.main, sub).getOrNull(edit.item)
            if (item != null && edit.field == "SIZE") {
                store.setPearlSize(item, it)
                store.load()
            }
        }
        editingValue = null
        valueText.clear()
    }

    private fun togglePearlExtra(field: String) {
        pearlExtraEditor = if (pearlExtraEditor == field) "" else field
        editingCoordinate = null
        editingValue = null
        draggingDelaySlider = null
    }

    private fun updateDelaySlider(hit: DelaySliderHit, x: Float) {
        val value = (((x - hit.rect.x) / hit.rect.width).coerceIn(0f, 1f) * 200f - 100f).toInt()
        store.setPearlDelay(hit.item, value.toDouble())
    }

    private fun editPill(r: Renderer2D, rect: Rect) {
        val pulse = editPulse(true)
        r.fillRect(rect, accent().withAlpha(0.22f + pulse * 0.30f), CornerRadius.uniform(3f))
        r.strokeRect(rect, accent().withAlpha(0.55f + pulse * 0.35f), 0.8f, CornerRadius.uniform(3f))
        r.text("EDIT", rect.centerX, rect.y + 3.0f, 6.2f, text().withAlpha(0.78f + pulse * 0.22f), fontBold(), TextAlign.CENTER)
        if (rect.inset(-4f).contains(mouseXf, mouseYf)) hoverTooltip = rect to "Press ENTER to confirm"
    }

    private fun textCursor(r: Renderer2D, x: Float, y: Float, height: Float) {
        val alpha = 0.34f + editPulse(true) * 0.56f
        r.backend.line(Point(x, y), Point(x, y + height), Paint.solid(text().withAlpha(alpha)), 0.9f)
    }

    private fun editPulse(active: Boolean): Float {
        if (!active) return 0f
        return ((sin(time * 6.0f) + 1f) * 0.5f)
    }

    private fun fit(r: Renderer2D, text: String, maxWidth: Float, size: Float): String {
        if (r.textWidth(text, size, fontSemi()) <= maxWidth) return text
        var out = text
        while (out.length > 4 && r.textWidth("$out...", size, fontSemi()) > maxWidth) out = out.dropLast(1)
        return "$out..."
    }

    private fun accent() = IqNanoGlobalConfigScreen.themeAccent()
    private fun border() = IqNanoGlobalConfigScreen.themeBorder()
    private fun panelStart() = IqNanoGlobalConfigScreen.themePanelStart()
    private fun panelEnd() = IqNanoGlobalConfigScreen.themePanelEnd()
    private fun surface() = IqNanoGlobalConfigScreen.themeSurface()
    private fun button() = IqNanoGlobalConfigScreen.themeButtonSurface()
    private fun modal() = IqNanoGlobalConfigScreen.themeModalSurface()
    private fun text() = IqNanoGlobalConfigScreen.themeText()
    private fun muted() = IqNanoGlobalConfigScreen.themeMuted()
    private fun fontRegular() = IqNanoGlobalConfigScreen.fontRegular()
    private fun fontSemi() = IqNanoGlobalConfigScreen.fontSemiBold()
    private fun fontBold() = IqNanoGlobalConfigScreen.fontBold()

    companion object {
        private val UI_SESSION_MAIN = DataKey.of("kuudrawp.session.main", Int::class.javaObjectType)
        private val UI_SESSION_SUB = DataKey.of("kuudrawp.session.sub", Int::class.javaObjectType)
        private val UI_SESSION_ITEM = DataKey.of("kuudrawp.session.item", Int::class.javaObjectType)
        private val UI_SESSION_ITEM_SCROLL = DataKey.of("kuudrawp.session.itemScroll", Double::class.javaObjectType)
        private val UI_SESSION_EXTRA_EDITOR = DataKey.of("kuudrawp.session.extraEditor", String::class.java)
    }
}
