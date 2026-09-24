package net.iqaddons.mod.hud;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.HudRenderEvent;
import net.iqaddons.mod.hud.config.HudConfigManager;
import net.iqaddons.mod.hud.element.HudWidget;
import net.iqaddons.mod.hud.nano.IqHudNanoRenderer;
import net.iqaddons.mod.screen.nano.IqNanoGlobalConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Getter
@Setter
public final class HudManager {

    private static final Minecraft mc = Minecraft.getInstance();
    private static HudManager instance;

    private final HudConfigManager configManager;

    private final List<HudWidget> widgets = new CopyOnWriteArrayList<>();
    private final Map<String, HudWidget> widgetById = new LinkedHashMap<>();

    private boolean editorOpen = false;
    private boolean centerGuidesEnabled = true;
    private boolean initialized = false;

    public HudManager() {
        this.configManager = new HudConfigManager();

        instance = this;
    }

    public void initialize() {
        if (initialized) {
            log.warn("HudManager already initialized!");
            return;
        }

        configManager.load();
        EventBus.subscribe(HudRenderEvent.class, this::onHudRender);

        initialized = true;
        log.debug("HudManager initialized");
    }

    public void stop() {
        widgets.forEach(HudWidget::deactivate);
        configManager.shutdown();
        log.debug("HudManager shutdown");
    }

    public void register(@NotNull HudWidget widget) {
        if (widgetById.containsKey(widget.getId())) {
            log.warn("Widget already registered: {}", widget.getId());
            return;
        }

        configManager.loadIntoWidget(widget);

        widgets.add(widget);
        widgetById.put(widget.getId(), widget);

        log.debug("Registered HUD widget: {}", widget.getDisplayName());
    }

    public void register(@NotNull HudWidget @NotNull ... widgetsToRegister) {
        for (HudWidget widget : widgetsToRegister) {
            register(widget);
        }
    }

    public void unregister(@NotNull String widgetId) {
        HudWidget widget = widgetById.remove(widgetId);
        if (widget != null) {
            widget.deactivate();
            widgets.remove(widget);
            log.debug("Unregistered HUD widget: {}", widgetId);
        }
    }

    public @Nullable HudWidget getWidget(@NotNull String widgetId) {
        return widgetById.get(widgetId);
    }

    @SuppressWarnings("unchecked")
    public <T extends HudWidget> @Nullable T getWidget(@NotNull Class<T> type) {
        for (HudWidget widget : widgets) {
            if (type.isInstance(widget)) {
                return (T) widget;
            }
        }
        return null;
    }

    @Contract(pure = true)
    public @NotNull @UnmodifiableView List<HudWidget> getWidgets() {
        return Collections.unmodifiableList(widgets);
    }

    private void onHudRender(@NotNull HudRenderEvent event) {
        if (IqNanoGlobalConfigScreen.isSharedModernHudStyle()) return;
        if (mc.screen instanceof AbstractContainerScreen<?>) return;

        double scaleFactor = mc.getWindow().getGuiScale();
        renderWidgets(
                event.drawContext(),
                mc.mouseHandler.xpos() / scaleFactor,
                mc.mouseHandler.ypos() / scaleFactor,
                event.tickDelta()
        );
    }

    public void renderOnHandledScreen(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (IqNanoGlobalConfigScreen.isSharedModernHudStyle()) return;
        renderWidgets(context, mouseX, mouseY, delta);
    }

    public void renderAll(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        for (HudWidget widget : widgets) {
            if (!widget.getEnabledSupplier().getAsBoolean()) continue;

            widget.renderExample(context, mouseX, mouseY, delta);
        }
    }

    private void updateWidgetActivation(@NotNull HudWidget widget) {
        boolean shouldBeActive = widget.shouldRender();

        if (shouldBeActive && !widget.isActive()) {
            widget.activate();
        } else if (!shouldBeActive && widget.isActive()) {
            widget.deactivate();
        }
    }

    private void renderWidgets(@NotNull GuiGraphicsExtractor context, double mouseX, double mouseY, float delta) {
        if (mc.player == null) return;
        if (mc.options.hideGui) return;
        if (mc.screen instanceof HudEditScreen) return;
        if (mc.options.keyPlayerList.isDown()) return;
        if (!IqNanoGlobalConfigScreen.isSharedGlobalHudEnabled()) return;

        for (HudWidget widget : widgets) {
            updateWidgetActivation(widget);

            if (widget.isActive()) {
                widget.render(context, mouseX, mouseY, delta);
            }
        }
    }

    public void openEditor() {
        openEditor(null);
    }

    public void openEditor(@Nullable Screen parent) {
        if (mc.screen instanceof HudEditScreen) {
            return;
        }
        mc.setScreen(new HudEditScreen(parent));
    }

    public boolean toggleCenterGuides() {
        centerGuidesEnabled = !centerGuidesEnabled;
        return centerGuidesEnabled;
    }

    public void saveConfig() {
        configManager.saveFromWidgetsSync(widgets);
    }

    public void resetAllConfigs() {
        configManager.resetAll();
        for (HudWidget widget : widgets) {
            configManager.loadIntoWidget(widget);
        }
    }

    public void reloadCurrentStyleConfigs() {
        for (HudWidget widget : widgets) {
            configManager.loadIntoWidget(widget);
            widget.refreshDimensions();
        }
    }

    public void toggleHudStyle() {
        saveConfig();
        for (HudWidget widget : widgets) {
            widget.deactivate();
            widget.setSelected(false);
            widget.refreshDimensions();
        }
        IqNanoGlobalConfigScreen.toggleSharedHudStyle();
        reloadCurrentStyleConfigs();
    }

    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (editorOpen) return false;
        if (IqNanoGlobalConfigScreen.isSharedModernHudStyle() && IqHudNanoRenderer.handleClick(this, mouseX, mouseY, button)) {
            return true;
        }

        for (HudWidget widget : widgets) {
            if (widget.isActive() && widget.shouldRender()) {
                if (widget.onClick(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static HudManager get() {
        if (instance == null) {
            throw new IllegalStateException("HudManager not initialized yet!");
        }

        return instance;
    }

    public static @Nullable HudManager getIfInitialized() {
        return instance;
    }
}
