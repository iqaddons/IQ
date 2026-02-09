package net.iqaddons.mod.hud;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.iqaddons.mod.events.EventBus;
import net.iqaddons.mod.events.impl.HudRenderEvent;
import net.iqaddons.mod.hud.config.HudConfigManager;
import net.iqaddons.mod.hud.element.HudWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
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

    private static final HudManager INSTANCE = new HudManager();
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final HudConfigManager configManager;

    private final List<HudWidget> widgets = new CopyOnWriteArrayList<>();
    private final Map<String, HudWidget> widgetById = new LinkedHashMap<>();

    private boolean editorOpen = false;
    private boolean initialized = false;

    private HudManager() {
        this.configManager = new HudConfigManager();
    }

    public void initialize() {
        if (initialized) {
            log.warn("HudManager already initialized!");
            return;
        }

        configManager.load();

        EventBus.subscribe(HudRenderEvent.class, this::onHudRender);

        initialized = true;
        log.info("HudManager initialized");
    }

    public void shutdown() {
        widgets.forEach(HudWidget::deactivate);
        configManager.shutdown();
        log.info("HudManager shutdown");
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
        return (T) widgets.stream()
                .filter(type::isInstance)
                .findFirst()
                .orElse(null);
    }

    @Contract(pure = true)
    public @NotNull @UnmodifiableView List<HudWidget> getWidgets() {
        return Collections.unmodifiableList(widgets);
    }

    private void onHudRender(@NotNull HudRenderEvent event) {
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;
        if (mc.currentScreen instanceof HudEditScreen) return;
        if (mc.options.playerListKey.isPressed()) return;

        double[] mousePos = getScaledMousePosition();
        for (HudWidget widget : widgets) {
            updateWidgetActivation(widget);

            if (widget.isActive()) {
                widget.render(event.drawContext(), mousePos[0], mousePos[1], event.tickDelta());
            }
        }
    }

    public void renderAll(@NotNull DrawContext context, int mouseX, int mouseY, float delta) {
        for (HudWidget widget : widgets) {
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

    public void openEditor() {
        if (mc.currentScreen instanceof HudEditScreen) {
            return;
        }
        mc.setScreen(new HudEditScreen());
    }

    public void saveConfig() {
        for (HudWidget widget : widgets) {
            configManager.saveFromWidget(widget);
        }
    }

    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (editorOpen) return false;
        if (mc.currentScreen instanceof ChatScreen) return false;

        for (HudWidget widget : widgets) {
            if (widget.isActive() && widget.shouldRender()) {
                if (widget.onClick(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static double @NotNull [] getScaledMousePosition() {
        double scaleFactor = mc.getWindow().getScaleFactor();
        return new double[]{
                mc.mouse.getX() / scaleFactor,
                mc.mouse.getY() / scaleFactor
        };
    }

    public static HudManager get() {
        return INSTANCE;
    }
}