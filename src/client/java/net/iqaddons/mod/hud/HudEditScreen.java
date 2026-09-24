package net.iqaddons.mod.hud;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.hud.config.HudElementConfig;
import net.iqaddons.mod.hud.nano.IqHudEditorOverlay;
import net.iqaddons.mod.hud.nano.IqHudNanoRenderer;
import net.iqaddons.mod.hud.element.HudElement;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.nanovg.IqNanoVgRenderable;
import net.iqaddons.mod.nanovg.rendering.renderer.Renderer2D;
import net.iqaddons.mod.screen.nano.IqNanoGlobalConfigScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Getter
@Setter
public class HudEditScreen extends Screen implements IqNanoVgRenderable {

    private static final String TITLE = "IQ HUD Editor";

    private static final int GRID_SIZE = 5;
    private static final float SCALE_STEP = 0.1f;
    private static final float POSITION_STEP = 1.0f;
    private static final float POSITION_STEP_FAST = 5.0f;
    private static final float ALIGNMENT_GUIDE_THRESHOLD = 3.25f;
    private static final float ALIGNMENT_SNAP_THRESHOLD = 0.75f;
    private static final float OPEN_ANIMATION_SPEED = 0.12f;
    private static final float CLOSE_ANIMATION_SPEED = 0.16f;
    private static final int HUD_STYLE_SWAP_DELAY_FRAMES = 3;
    private static final int HUD_STYLE_BUTTON_WIDTH = 122;
    private static final int HUD_STYLE_BUTTON_HEIGHT = 32;
    private static final int HUD_STYLE_BUTTON_BOTTOM_MARGIN = 24;

    private @Nullable HudElement selectedElement = null;
    private final @Nullable Screen parent;

    private boolean showGrid = false;
    private boolean snapToGrid = false;
    private boolean alignmentHelperEnabled = true;
    private boolean closing = false;
    private float openProgress = 0.0f;
    private float closeProgress = 0.0f;
    private int pendingHudStyleSwapFrames = 0;
    private final List<AlignmentGuide> activeAlignmentGuides = new ArrayList<>();

    public record AlignmentGuide(float position, boolean vertical) {}
    private record HelpLine(String label, String value) {}

    public HudEditScreen() {
        this(null);
    }

    public HudEditScreen(@Nullable Screen parent) {
        super(Component.literal(TITLE));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float a) {
        updateAnimation();
        if (closing && closeProgress >= 0.985f) {
            finishClose();
            return;
        }
        updatePendingHudStyleSwap();

        boolean modernHud = IqNanoGlobalConfigScreen.isSharedModernHudStyle();
        if (modernHud) {
            renderModernEditorBackground(context);
        } else {
            extractMenuBackground(context);
        }
        if (showGrid && !modernHud) {
            renderGrid(context);
        }
        if (HudManager.get().isCenterGuidesEnabled() && !modernHud) {
            renderCenterGuides(context);
        }

        HudManager.get().renderAll(context, mouseX, mouseY, a);
        if (!modernHud) {
            renderHudStyleButton(context, mouseX, mouseY);
            renderHelpText(context);
        }

        super.extractRenderState(context, mouseX, mouseY, a);
    }

    private void renderModernEditorBackground(@NotNull GuiGraphicsExtractor context) {
        double opacity = Math.clamp(IqNanoGlobalConfigScreen.getSharedOverlayOpacity(), 0.25, 1.0);
        int alpha = (int) Math.round(160.0 * opacity * animationVisibility());
        context.fill(0, 0, width, height, new Color(0, 0, 0, alpha).getRGB());
    }

    private void renderGrid(@NotNull GuiGraphicsExtractor context) {
        boolean modernHud = IqNanoGlobalConfigScreen.isSharedModernHudStyle();
        int gridColor = modernHud
                ? new Color(255, 255, 255, 9).getRGB()
                : new Color(255, 255, 255, 30).getRGB();
        int spacing = modernHud ? GRID_SIZE * 2 : GRID_SIZE;

        for (int x = 0; x < width; x += spacing) {
            context.verticalLine(x, 0, height, gridColor);
        }

        for (int y = 0; y < height; y += spacing) {
            context.horizontalLine(0, width, y, gridColor);
        }
    }

    private void renderCenterGuides(@NotNull GuiGraphicsExtractor context) {
        int guideColor = IqNanoGlobalConfigScreen.isSharedModernHudStyle()
                ? new Color(255, 255, 255, 36).getRGB()
                : new Color(255, 255, 255, 30).getRGB();
        int centerX = width / 2;
        int centerY = height / 2;

        context.verticalLine(centerX, 0, height, guideColor);
        context.horizontalLine(0, width, centerY, guideColor);
    }

    private void renderHelpText(@NotNull GuiGraphicsExtractor context) {
        if (minecraft == null || minecraft.font == null) return;

        List<HelpLine> helpLines = List.of(
                new HelpLine("Mouse", "Drag to move widget"),
                new HelpLine("Scale", "Scroll / + / -"),
                new HelpLine("Position", "Arrow keys (Shift x5)"),
                new HelpLine("Toggles", "[G] Grid | [S] Snap | [Y] Guides | [A] Align"),
                new HelpLine("Actions", "[C] Center | [R] Reset | [Ctrl+R] Defaults"),
                new HelpLine("Layout", "[T] Top-left all"),
                new HelpLine("Exit", parent != null ? "[ESC] Save and back" : "[ESC] Save and close")
        );

        int lineHeight = minecraft.font.lineHeight + 1;
        int paddingX = 10;
        int paddingY = 7;
        int labelX = 10;
        int valueX = 74;
        int x = 12;
        int y = height - (helpLines.size() * lineHeight) - paddingY - 12;

        int maxValueWidth = 0;
        for (HelpLine line : helpLines) {
            maxValueWidth = Math.max(maxValueWidth, minecraft.font.width(line.value()));
        }

        int panelX = x;
        int panelY = y - paddingY;
        int panelWidth = valueX + maxValueWidth + paddingX;
        int panelHeight = (helpLines.size() * lineHeight) + (paddingY * 2) - 1;
        int textX = panelX + labelX;

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, new Color(7, 10, 13, 165).getRGB());
        int borderColor = new Color(255, 105, 216, 150).getRGB();
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, borderColor);
        context.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, borderColor);
        context.fill(panelX, panelY, panelX + 1, panelY + panelHeight, borderColor);
        context.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, borderColor);

        int labelColor = 0xFFFF69D8;
        int valueColor = 0xFFE8E8E8;
        int mutedValueColor = 0xFFD0D0D0;
        for (HelpLine line : helpLines) {
            context.text(minecraft.font, line.label(), textX, y, labelColor);
            context.text(minecraft.font, line.value(), panelX + valueX, y, "Exit".equals(line.label()) ? valueColor : mutedValueColor);
            y += lineHeight;
        }

        String snapStatus = "Snap: " + (snapToGrid ? "ON" : "OFF");
        String gridStatus = "Grid: " + (showGrid ? "ON" : "OFF");
        String guidesStatus = "Guides: " + (HudManager.get().isCenterGuidesEnabled() ? "ON" : "OFF");
        String alignStatus = "Align: " + (alignmentHelperEnabled ? "ON" : "OFF");
        int statusX = width - 10;
        int statusY = height - (lineHeight * 4) - 8;

        int statusOn = 0xFFFF69D8;
        int statusOff = 0xFF8A7B84;
        context.text(
                minecraft.font,
                snapStatus,
                statusX - minecraft.font.width(snapStatus),
                statusY,
                snapToGrid ? statusOn : statusOff
        );
        statusY += lineHeight;
        context.text(
                minecraft.font,
                gridStatus,
                statusX - minecraft.font.width(gridStatus),
                statusY,
                showGrid ? statusOn : statusOff
        );
        statusY += lineHeight;
        context.text(
                minecraft.font,
                guidesStatus,
                statusX - minecraft.font.width(guidesStatus),
                statusY,
                HudManager.get().isCenterGuidesEnabled() ? statusOn : statusOff
        );
        statusY += lineHeight;
        context.text(
                minecraft.font,
                alignStatus,
                statusX - minecraft.font.width(alignStatus),
                statusY,
                alignmentHelperEnabled ? statusOn : statusOff
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (closing) return true;
        if (isHudStyleSwapPending()) return true;

        if (click.button() == 0) {
            if (isMouseOverHudStyleButton(click.x(), click.y())) {
                requestHudStyleToggle();
                return true;
            }

            selectedElement = findElementAt(click.x(), click.y());
            if (selectedElement != null) {
                updateSelectionState();
                setDragging(true);

                log.debug("Selected element: {}", selectedElement.getId());
            } else {
                clearSelection();
            }

            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    private void renderHudStyleButton(@NotNull GuiGraphicsExtractor context, double mouseX, double mouseY) {
        if (minecraft == null || minecraft.font == null) return;

        int x = hudStyleButtonX();
        int y = hudStyleButtonY();
        boolean hovered = isMouseOverHudStyleButton(mouseX, mouseY);
        int fill = hovered
                ? new Color(14, 18, 22, 215).getRGB()
                : new Color(7, 10, 13, 145).getRGB();
        int border = hovered
                ? new Color(255, 105, 216, 210).getRGB()
                : new Color(255, 105, 216, 78).getRGB();
        int labelColor = 0xFF00F5FF;
        int activeColor = 0xFFFFFFFF;
        int inactiveColor = 0xFF8A8A8A;
        int separatorColor = 0xFFB5B5B5;
        boolean modern = IqNanoGlobalConfigScreen.isSharedModernHudStyle();

        int hoverInset = hovered ? -1 : 0;
        int left = x + hoverInset;
        int top = y + hoverInset;
        int right = x + HUD_STYLE_BUTTON_WIDTH - hoverInset;
        int bottom = y + HUD_STYLE_BUTTON_HEIGHT - hoverInset;
        context.fill(left, top, right, bottom, fill);
        context.fill(left, top, right, top + 1, border);
        context.fill(left, bottom - 1, right, bottom, border);
        context.fill(left, top, left + 1, bottom, border);
        context.fill(right - 1, top, right, bottom, border);

        String title = "HUD STYLE";
        context.text(minecraft.font, title, x + (HUD_STYLE_BUTTON_WIDTH - minecraft.font.width(title)) / 2, y + 4, labelColor);

        String vanilla = "VANILLA";
        String separator = " / ";
        String modernText = "MODERN";
        int totalWidth = minecraft.font.width(vanilla) + minecraft.font.width(separator) + minecraft.font.width(modernText);
        int textX = x + (HUD_STYLE_BUTTON_WIDTH - totalWidth) / 2;
        int textY = y + 17;
        context.text(minecraft.font, vanilla, textX, textY, modern ? inactiveColor : activeColor);
        textX += minecraft.font.width(vanilla);
        context.text(minecraft.font, separator, textX, textY, separatorColor);
        textX += minecraft.font.width(separator);
        context.text(minecraft.font, modernText, textX, textY, modern ? activeColor : inactiveColor);
    }

    private boolean isMouseOverHudStyleButton(double mouseX, double mouseY) {
        int x = hudStyleButtonX();
        int y = hudStyleButtonY();
        return mouseX >= x && mouseX < x + HUD_STYLE_BUTTON_WIDTH
                && mouseY >= y && mouseY < y + HUD_STYLE_BUTTON_HEIGHT;
    }

    private int hudStyleButtonX() {
        return (width - HUD_STYLE_BUTTON_WIDTH) / 2;
    }

    private int hudStyleButtonY() {
        return height - HUD_STYLE_BUTTON_HEIGHT - HUD_STYLE_BUTTON_BOTTOM_MARGIN;
    }

    private void requestHudStyleToggle() {
        if (isHudStyleSwapPending()) return;
        setDragging(false);
        clearSelection();
        activeAlignmentGuides.clear();
        HudManager.get().saveConfig();
        pendingHudStyleSwapFrames = HUD_STYLE_SWAP_DELAY_FRAMES;
    }

    private boolean isHudStyleSwapPending() {
        return pendingHudStyleSwapFrames > 0;
    }

    private void updatePendingHudStyleSwap() {
        if (pendingHudStyleSwapFrames <= 0) return;
        pendingHudStyleSwapFrames--;
        if (pendingHudStyleSwapFrames > 0) return;

        setDragging(false);
        clearSelection();
        activeAlignmentGuides.clear();
        HudManager.get().toggleHudStyle();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (closing) return true;
        if (isHudStyleSwapPending()) return true;

        if (isDragging() && selectedElement != null && click.button() == 0) {
            int[] screen = HudAnchor.getScreenDimensions();
            float dx = HudAnchor.scaleDeltaX((float) offsetX, screen[0]);
            float dy = HudAnchor.scaleDeltaY((float) offsetY, screen[1]);

            float newX = selectedElement.getX() + dx;
            float newY = selectedElement.getY() + dy;

            if (snapToGrid) {
                newX = snapToGrid(newX);
                newY = snapToGrid(newY);
            }

            selectedElement.setPosition(newX, newY);
            if (alignmentHelperEnabled) {
                applyAlignmentSnap(selectedElement);
            } else {
                activeAlignmentGuides.clear();
            }

            // Clamp position so widget stays within visible game window
            clampDragPosition(selectedElement);

            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    private void clampDragPosition(@NotNull HudElement element) {
        int[] screen = net.iqaddons.mod.hud.element.HudAnchor.getScreenDimensions();
        int screenWidth = screen[0];
        int screenHeight = screen[1];

        if (screenWidth <= 0 || screenHeight <= 0) return;

        int scaledWidth = element.getScaledWidth();
        int scaledHeight = element.getScaledHeight();

        float absX = element.getAnchor().calculateX(screenWidth, scaledWidth, element.getX());
        float absY = element.getAnchor().calculateY(screenHeight, scaledHeight, element.getY());

        // Clamp absolute position within screen bounds
        float clampedAbsX = Math.clamp(absX, 0.0f, Math.max(0, screenWidth - scaledWidth));
        float clampedAbsY = Math.clamp(absY, 0.0f, Math.max(0, screenHeight - scaledHeight));

        // Convert back to anchor offsets
        float newOffsetX = element.getAnchor().toOffsetX(clampedAbsX, screenWidth, scaledWidth);
        float newOffsetY = element.getAnchor().toOffsetY(clampedAbsY, screenHeight, scaledHeight);

        // Only update if position actually needs clamping
        if (Math.abs(newOffsetX - element.getX()) > 0.01f || Math.abs(newOffsetY - element.getY()) > 0.01f) {
            log.debug("Clamping widget {}: ({}, {}) -> ({}, {})", 
                    element.getId(), element.getX(), element.getY(), newOffsetX, newOffsetY);
            element.setPosition(newOffsetX, newOffsetY);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (closing) return true;

        if (click.button() == 0 && isDragging()) {
            setDragging(false);
            activeAlignmentGuides.clear();
            if (selectedElement instanceof HudWidget widget) {
                HudManager.get().getConfigManager().saveFromWidget(widget);
            }

            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (closing) return true;
        if (isHudStyleSwapPending()) return true;

        if (selectedElement != null) {
            adjustSelectedScale((float) (verticalAmount * SCALE_STEP));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (closing) return true;
        if (isHudStyleSwapPending()) return true;

        int keyCode = input.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        boolean shift = input.hasShiftDown();
        float step = shift ? POSITION_STEP_FAST : POSITION_STEP;

        if (input.hasControlDown() && keyCode == GLFW.GLFW_KEY_R) {
            resetAllElementsToDefaults();
            return true;
        }

        if (selectedElement != null) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_UP -> {
                    moveElement(0, -step);
                    return true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    moveElement(0, step);
                    return true;
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    moveElement(-step, 0);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    moveElement(step, 0);
                    return true;
                }
                case GLFW.GLFW_KEY_R -> {
                    resetSelectedElement();
                    return true;
                }
                case GLFW.GLFW_KEY_C -> {
                    centerSelectedElement();
                    return true;
                }
                case GLFW.GLFW_KEY_KP_ADD, GLFW.GLFW_KEY_EQUAL -> {
                    adjustSelectedScale(SCALE_STEP);
                    return true;
                }
                case GLFW.GLFW_KEY_KP_SUBTRACT, GLFW.GLFW_KEY_MINUS -> {
                    adjustSelectedScale(-SCALE_STEP);
                    return true;
                }
            }
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_G -> {
                showGrid = !showGrid;
                return true;
            }
            case GLFW.GLFW_KEY_S -> {
                snapToGrid = !snapToGrid;
                return true;
            }
            case GLFW.GLFW_KEY_Y -> {
                HudManager.get().toggleCenterGuides();
                return true;
            }
            case GLFW.GLFW_KEY_A -> {
                alignmentHelperEnabled = !alignmentHelperEnabled;
                if (!alignmentHelperEnabled) {
                    activeAlignmentGuides.clear();
                }
                return true;
            }
            case GLFW.GLFW_KEY_T -> {
                resetAllElementsToTopLeft();
                return true;
            }
        }

        return super.keyPressed(input);
    }

    private void resetAllElementsToTopLeft() {
        for (HudWidget widget : HudManager.get().getWidgets()) {
            try {
                widget.setAnchor(HudAnchor.TOP_LEFT);

                float offsetX = 0.0f;
                float offsetY = 0.0f;

                if (snapToGrid) {
                    offsetX = snapToGrid(offsetX);
                    offsetY = snapToGrid(offsetY);
                }

                widget.setPosition(offsetX, offsetY);

                HudManager.get().getConfigManager().saveFromWidget(widget);
            } catch (Exception e) {
                log.warn("Failed to reset widget to top-left: {}", widget.getId(), e);
            }
        }

        log.info("All widgets moved to top-left (Ctrl+Shift+R)");
    }

    private void resetAllElementsToDefaults() {
        for (HudWidget widget : HudManager.get().getWidgets()) {
            try {
                widget.resetToDefaults();
                HudManager.get().getConfigManager().saveFromWidget(widget);
            } catch (Exception e) {
                log.warn("Failed to reset widget to defaults: {}", widget.getId(), e);
            }
        }

        activeAlignmentGuides.clear();
        log.info("All widgets reset to defaults (Ctrl+R)");
    }

    private void moveElement(float dx, float dy) {
        if (selectedElement == null) return;

        float newX = selectedElement.getX() + dx;
        float newY = selectedElement.getY() + dy;

        if (snapToGrid) {
            newX = snapToGrid(newX);
            newY = snapToGrid(newY);
        }

        selectedElement.setPosition(newX, newY);
        activeAlignmentGuides.clear();
        
        // Clamp position so widget stays within visible game window when using arrow keys
        clampDragPosition(selectedElement);
        
        if (selectedElement instanceof HudWidget widget) {
            HudManager.get().getConfigManager().saveFromWidget(widget);
        }
    }

    private void resetSelectedElement() {
        if (selectedElement == null) return;

        if (selectedElement instanceof HudWidget widget) {
            widget.resetToDefaults();
            HudManager.get().getConfigManager().saveFromWidget(widget);
            log.info("Reset element to defaults: {}", widget.getId());
        }
    }

    private void centerSelectedElement() {
        if (selectedElement == null) return;

        int[] screen = net.iqaddons.mod.hud.element.HudAnchor.getScreenDimensions();
        int screenWidth = screen[0];
        int screenHeight = screen[1];
        int widgetWidth = selectedElement.getScaledWidth();
        int widgetHeight = selectedElement.getScaledHeight();

        float absoluteX = (screenWidth - widgetWidth) / 2.0f;
        float absoluteY = (screenHeight - widgetHeight) / 2.0f;

        float offsetX = selectedElement.getAnchor().toOffsetX(absoluteX, screenWidth, widgetWidth);
        float offsetY = selectedElement.getAnchor().toOffsetY(absoluteY, screenHeight, widgetHeight);

        if (snapToGrid) {
            offsetX = snapToGrid(offsetX);
            offsetY = snapToGrid(offsetY);
        }

        selectedElement.setPosition(offsetX, offsetY);
        if (selectedElement instanceof HudWidget widget) {
            HudManager.get().getConfigManager().saveFromWidget(widget);
        }
    }

    private void adjustSelectedScale(float scaleDelta) {
        if (selectedElement == null) return;

        float newScale = selectedElement.getScale() + scaleDelta;
        newScale = Math.clamp(newScale, HudElementConfig.MIN_SCALE, HudElementConfig.MAX_SCALE);

        selectedElement.setScale(newScale);
        if (selectedElement instanceof HudWidget widget) {
            HudManager.get().getConfigManager().saveFromWidget(widget);
        }
    }

    private @Nullable HudElement findElementAt(double mouseX, double mouseY) {
        List<HudWidget> widgets = HudManager.get().getWidgets();
        for (int i = widgets.size() - 1; i >= 0; i--) {
            HudWidget widget = widgets.get(i);
            if (widget.isMouseOver(mouseX, mouseY)) {
                return widget;
            }
        }
        return null;
    }

    private void updateSelectionState() {
        for (HudWidget widget : HudManager.get().getWidgets()) {
            widget.setSelected(widget == selectedElement);
        }
    }

    private void clearSelection() {
        selectedElement = null;
        activeAlignmentGuides.clear();
        for (HudWidget widget : HudManager.get().getWidgets()) {
            widget.setSelected(false);
        }
    }

    public @NotNull List<AlignmentGuide> getActiveAlignmentGuides() {
        return Collections.unmodifiableList(activeAlignmentGuides);
    }

    private void applyAlignmentSnap(@NotNull HudElement element) {
        activeAlignmentGuides.clear();
        if (!alignmentHelperEnabled) return;

        int[] screen = HudAnchor.getScreenDimensions();
        int screenWidth = screen[0];
        int screenHeight = screen[1];
        if (screenWidth <= 0 || screenHeight <= 0) return;

        int elementWidth = element.getScaledWidth();
        int elementHeight = element.getScaledHeight();
        float absX = element.getAnchor().calculateX(screenWidth, elementWidth, element.getX());
        float absY = element.getAnchor().calculateY(screenHeight, elementHeight, element.getY());

        AlignmentSnap xSnap = findBestXSnap(element, absX, elementWidth, screenWidth, screenHeight);
        AlignmentSnap ySnap = findBestYSnap(element, absY, elementHeight, screenWidth, screenHeight);

        if (xSnap != null) {
            absX += xSnap.delta();
            activeAlignmentGuides.add(new AlignmentGuide(xSnap.guidePosition(), true));
        }

        if (ySnap != null) {
            absY += ySnap.delta();
            activeAlignmentGuides.add(new AlignmentGuide(ySnap.guidePosition(), false));
        }

        if (xSnap != null || ySnap != null) {
            float offsetX = element.getAnchor().toOffsetX(absX, screenWidth, elementWidth);
            float offsetY = element.getAnchor().toOffsetY(absY, screenHeight, elementHeight);
            element.setPosition(offsetX, offsetY);
        }
    }

    private @Nullable AlignmentSnap findBestXSnap(
            @NotNull HudElement selected,
            float selectedAbsX,
            int selectedWidth,
            int screenWidth,
            int screenHeight
    ) {
        float[] selectedPoints = new float[]{
                selectedAbsX,
                selectedAbsX + selectedWidth / 2.0f,
                selectedAbsX + selectedWidth
        };

        AlignmentSnap best = null;
        for (HudWidget widget : HudManager.get().getWidgets()) {
            if (widget == selected || !widget.isNanoEnabled()) continue;
            int otherWidth = widget.getScaledWidth();
            float otherAbsX = widget.getAnchor().calculateX(screenWidth, otherWidth, widget.getX());
            float[] otherPoints = new float[]{
                    otherAbsX,
                    otherAbsX + otherWidth / 2.0f,
                    otherAbsX + otherWidth
            };

            best = findCloserSnap(selectedPoints, otherPoints, best);
        }

        return best;
    }

    private @Nullable AlignmentSnap findBestYSnap(
            @NotNull HudElement selected,
            float selectedAbsY,
            int selectedHeight,
            int screenWidth,
            int screenHeight
    ) {
        float[] selectedPoints = new float[]{
                selectedAbsY,
                selectedAbsY + selectedHeight / 2.0f,
                selectedAbsY + selectedHeight
        };

        AlignmentSnap best = null;
        for (HudWidget widget : HudManager.get().getWidgets()) {
            if (widget == selected || !widget.isNanoEnabled()) continue;
            int otherHeight = widget.getScaledHeight();
            float otherAbsY = widget.getAnchor().calculateY(screenHeight, otherHeight, widget.getY());
            float[] otherPoints = new float[]{
                    otherAbsY,
                    otherAbsY + otherHeight / 2.0f,
                    otherAbsY + otherHeight
            };

            best = findCloserSnap(selectedPoints, otherPoints, best);
        }

        return best;
    }

    private @Nullable AlignmentSnap findCloserSnap(float[] selectedPoints, float[] otherPoints, @Nullable AlignmentSnap best) {
        AlignmentSnap closest = best;
        for (float selectedPoint : selectedPoints) {
            for (float otherPoint : otherPoints) {
                float delta = otherPoint - selectedPoint;
                float distance = Math.abs(delta);
                if (distance > ALIGNMENT_GUIDE_THRESHOLD) continue;
                if (closest == null || distance < closest.distance()) {
                    float snapDelta = distance <= ALIGNMENT_SNAP_THRESHOLD ? delta : 0.0f;
                    closest = new AlignmentSnap(snapDelta, otherPoint, distance);
                }
            }
        }

        return closest;
    }

    private record AlignmentSnap(float delta, float guidePosition, float distance) {}

    private float snapToGrid(float value) {
        return Math.round(value / GRID_SIZE) * GRID_SIZE;
    }

    @Override
    public void removed() {
        super.removed();
        clearSelection();

        HudManager.get().saveConfig();
        HudManager.get().setEditorOpen(false);
        log.debug("HUD Editor closed, configurations saved");
    }

    @Override
    public void init() {
        super.init();
        HudManager.get().setEditorOpen(true);
        log.debug("HUD Editor opened");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderNanoVg(@NotNull Renderer2D renderer, float mouseX, float mouseY) {
        if (!IqNanoGlobalConfigScreen.isSharedModernHudStyle()) return;

        float visible = animationVisibility();
        float scale = 0.975f + 0.025f * openProgress - 0.018f * closeProgress;
        float dx = width * 0.5f * (1.0f - scale);
        float dy = height * 0.5f * (1.0f - scale) + (1.0f - openProgress) * 8.0f - closeProgress * 4.0f;

        renderer.getBackend().save();
        renderer.getBackend().globalAlpha(visible);
        renderer.getBackend().translate(dx, dy);
        renderer.getBackend().scale(scale, scale);
        IqHudEditorOverlay.drawAlignmentGuides(renderer, getActiveAlignmentGuides(), width, height);
        IqHudNanoRenderer.drawEditorWidgets(renderer, mouseX, mouseY);
        IqHudEditorOverlay.draw(renderer, width, height, parent != null, snapToGrid, showGrid, HudManager.get().isCenterGuidesEnabled(), alignmentHelperEnabled, mouseX, mouseY);
        renderer.getBackend().restore();
    }

    @Override
    public void onClose() {
        if (closing) return;

        closing = true;
        closeProgress = 0.0f;
        setDragging(false);
        activeAlignmentGuides.clear();
        clearSelection();
        HudManager.get().saveConfig();
    }

    private void updateAnimation() {
        openProgress = approach(openProgress, 1.0f, OPEN_ANIMATION_SPEED);
        closeProgress = approach(closeProgress, closing ? 1.0f : 0.0f, CLOSE_ANIMATION_SPEED);
    }

    private float animationVisibility() {
        return Math.clamp(openProgress * (1.0f - closeProgress), 0.0f, 1.0f);
    }

    private float approach(float current, float target, float amount) {
        return current + (target - current) * Math.clamp(amount, 0.0f, 1.0f);
    }

    private void finishClose() {
        if (minecraft == null) return;

        closing = false;
        Screen target = parent;
        minecraft.setScreen(target);
    }
}
