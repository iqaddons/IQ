package net.iqaddons.mod.hud;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.hud.config.HudElementConfig;
import net.iqaddons.mod.hud.element.HudElement;
import net.iqaddons.mod.hud.element.HudWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
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
        super(Text.literal(TITLE));
    }

    @Override
    public void render(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        renderDarkening(context);
        if (showGrid) {
            renderGrid(context);
        }

        HudManager.get().renderAll(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
        renderHelpText(context);
    }

    private void renderGrid(@NotNull DrawContext context) {
        int gridColor = new Color(255, 255, 255, 30).getRGB();

        for (int x = 0; x < width; x += GRID_SIZE) {
            context.drawVerticalLine(x, 0, height, gridColor);
        }

        for (int y = 0; y < height; y += GRID_SIZE) {
            context.drawHorizontalLine(0, width, y, gridColor);
        }
    }

    private void renderHelpText(@NotNull DrawContext context) {
        if (client == null || client.textRenderer == null) return;

        List<String> helpLines = List.of(
                "§e[Drag]§7 Move element",
                "§e[Scroll / + / -]§7 Scale",
                "§e[Arrows]§7 Fine-tune position",
                "§e[G]§7 Toggle grid | §e[S]§7 Toggle snap",
                "§e[R]§7 Reset selected | §e[ESC]§7 Save & close"
        );

        int lineHeight = client.textRenderer.fontHeight + 1;
        int padding = 5;
        int x = 5;
        int y = height - (helpLines.size() * lineHeight) - padding;

        int maxWidth = 0;
        for (String line : helpLines) {
            maxWidth = Math.max(maxWidth, client.textRenderer.getWidth(line));
        }

        for (String line : helpLines) {
            context.drawTextWithShadow(client.textRenderer, line, x, y, 0xFFFFFFFF);
            y += lineHeight;
        }

        String snapStatus = snapToGrid ? "Snap: ON" : "Snap: OFF";
        int snapColor = snapToGrid ? 0xFF55FF55 : 0xFFFF5555;
        context.drawTextWithShadow(
                client.textRenderer,
                snapStatus,
                width - client.textRenderer.getWidth(snapStatus) - 5,
                5,
                snapColor
        );;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
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
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (isDragging() && selectedElement != null && click.button() == 0) {
            float newX = selectedElement.getX() + (float) offsetX;
            float newY = selectedElement.getY() + (float) offsetY;

            if (snapToGrid) {
                newX = snapToGrid(newX);
                newY = snapToGrid(newY);
            }

            selectedElement.setPosition(newX, newY);
            return true;
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
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
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        boolean shift = input.hasShift();
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
                    resetSelectedElement();
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
        }

        return super.keyPressed(input);
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
        if (selectedElement instanceof HudWidget widget) {
            HudManager.get().getConfigManager().saveFromWidget(widget);
        }
    }

    private void resetSelectedElement() {
        if (selectedElement == null) return;

        String id = selectedElement.getId();
        HudManager.get().getConfigManager().removeConfig(id);

        log.info("Reset element to defaults: {}", id);
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
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}