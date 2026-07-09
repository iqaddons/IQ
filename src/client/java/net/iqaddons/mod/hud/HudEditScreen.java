package net.iqaddons.mod.hud;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.hud.config.HudElementConfig;
import net.iqaddons.mod.hud.element.HudAnchor;
import net.iqaddons.mod.hud.element.HudElement;
import net.iqaddons.mod.hud.element.HudWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.List;

@Slf4j
@Getter
@Setter
public class HudEditScreen extends Screen {

    private static final String TITLE = "IQ HUD Editor";

    private static final int GRID_SIZE = 5;
    private static final float SCALE_STEP = 0.1f;
    private static final float POSITION_STEP = 1.0f;
    private static final float POSITION_STEP_FAST = 5.0f;

    private @Nullable HudElement selectedElement = null;

    private boolean showGrid = false;
    private boolean snapToGrid = false;

    public HudEditScreen() {
        super(Component.literal(TITLE));
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float a) {
        extractMenuBackground(context);
        if (showGrid) {
            renderGrid(context);
        }
        if (HudManager.get().isCenterGuidesEnabled()) {
            renderCenterGuides(context);
        }

        HudManager.get().renderAll(context, mouseX, mouseY, a);

        super.extractRenderState(context, mouseX, mouseY, a);
        renderHelpText(context);
    }

    private void renderGrid(@NotNull GuiGraphicsExtractor context) {
        int gridColor = new Color(255, 255, 255, 30).getRGB();

        for (int x = 0; x < width; x += GRID_SIZE) {
            context.verticalLine(x, 0, height, gridColor);
        }

        for (int y = 0; y < height; y += GRID_SIZE) {
            context.horizontalLine(0, width, y, gridColor);
        }
    }

    private void renderCenterGuides(@NotNull GuiGraphicsExtractor context) {
        int guideColor = new Color(255, 255, 255, 30).getRGB();
        int centerX = width / 2;
        int centerY = height / 2;

        context.verticalLine(centerX, 0, height, guideColor);
        context.horizontalLine(0, width, centerY, guideColor);
    }

    private void renderHelpText(@NotNull GuiGraphicsExtractor context) {
        if (minecraft == null || minecraft.font == null) return;

        List<String> helpLines = List.of(
                "§dMouse§8 • §fDrag to move widget",
                "§dScale§8 • §fScroll / + / -",
                "§dPosition§8 • §fArrow keys §8(§fShift = x5§8)",
                "§dToggles§8 • §f[G] Grid §8| §f[S] Snap §8| §f[Y] Guides",
                "§dActions§8 • §f[C] Center §8| §f[R] Reset §8| §fCtrl+R Reset all",
                "§dExit§8 • §f[ESC] Save & close"
        );

        int lineHeight = minecraft.font.lineHeight + 1;
        int padding = 6;
        int x = 10;
        int y = height - (helpLines.size() * lineHeight) - (padding * 2) - 8;

        int maxWidth = 0;
        for (String line : helpLines) {
            maxWidth = Math.max(maxWidth, minecraft.font.width(line));
        }

        int panelX = x - padding;
        int panelY = y - padding;
        int panelWidth = maxWidth + (padding * 2);
        int panelHeight = (helpLines.size() * lineHeight) + (padding * 2) - 1;

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, new Color(0, 0, 0, 120).getRGB());
        int borderColor = new Color(255, 105, 216, 110).getRGB();
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, borderColor);
        context.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, borderColor);
        context.fill(panelX, panelY, panelX + 1, panelY + panelHeight, borderColor);
        context.fill(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, borderColor);

        for (String line : helpLines) {
            context.text(minecraft.font, line, x, y, 0xFFFFFFFF);
            y += lineHeight;
        }

        String snapStatus = "Snap: " + (snapToGrid ? "ON" : "OFF");
        String gridStatus = "Grid: " + (showGrid ? "ON" : "OFF");
        String guidesStatus = "Guides: " + (HudManager.get().isCenterGuidesEnabled() ? "ON" : "OFF");
        int statusX = width - 10;
        int statusY = height - (lineHeight * 3) - 8;

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
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0) {
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

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
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
        if (click.button() == 0 && isDragging()) {
            setDragging(false);
            if (selectedElement instanceof HudWidget widget) {
                HudManager.get().getConfigManager().saveFromWidget(widget);
            }

            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selectedElement != null) {
            adjustSelectedScale((float) (verticalAmount * SCALE_STEP));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.key();
        boolean shift = input.hasShiftDown();
        float step = shift ? POSITION_STEP_FAST : POSITION_STEP;

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
                    if (input.hasControlDown()) {
                        resetAllElementsToTopLeft();
                    } else {
                        resetSelectedElement();
                    }
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

        log.info("All widgets reset to top-left (Ctrl+R)");
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
        for (HudWidget widget : HudManager.get().getWidgets()) {
            widget.setSelected(false);
        }
    }

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
        return true;
    }
}