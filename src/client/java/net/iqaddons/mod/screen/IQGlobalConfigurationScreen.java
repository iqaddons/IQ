package net.iqaddons.mod.screen;

import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.manager.IQPersistentDataStore;
import net.iqaddons.mod.utils.data.DataKey;
import net.iqaddons.mod.screen.model.ConfigCategory;
import net.iqaddons.mod.screen.model.ConfigEntryModel;
import net.iqaddons.mod.screen.model.ConfigEntryModel.EntryType;
import net.iqaddons.mod.utils.ConfigReflectionUtil;
import net.iqaddons.mod.utils.MessageUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class IQGlobalConfigurationScreen extends Screen {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IQGlobalConfigurationScreen.class);

    private static final boolean TEMP_HIDDEN = false;

    private static final Identifier BACK_ICON_TEXTURE  = Identifier.fromNamespaceAndPath("iq", "textures/social/back.png");
    private static final Identifier CLOSE_ICON_TEXTURE = Identifier.fromNamespaceAndPath("iq", "textures/social/close.png");
    private static final int ICON_RENDER_SIZE = 12;

    private static final int BG_OUTER = 0x7A000000;
    private static final int BG_HEADER = 0xD50D0A16;
    private static final int BG_ROW = 0xA3120E1C;
    private static final int BG_ROW_HOV = 0xC51F1330;
    private static final int BG_CONTROL_TOP = 0xCC2A1A39;
    private static final int BG_CONTROL_TOP_HOV = 0xD63C234D;
    private static final int BG_CONTROL_BOTTOM = 0xCC1A1027;
    private static final int BG_CONTROL_BOTTOM_HOV = 0xD3261635;
    private static final int BORDER_DIM = 0x3034253D;
    private static final int BORDER_MID = 0x6A7A3D75;
    private static final int T_MAIN = 0xFFF8F1FB;
    private static final int T_MUTED = 0xFFC9B4D5;
    private static final int T_HUB_TITLE = 0xFFD6A0FF;
    private static final int T_HUB_SUBTITLE = 0xFFA07AC8;
    private static final int T_ACTION_TEXT = 0xFFFFE7F4;
    private static final int T_ACTION_TEXT_HOV = 0xFFFFC2E7;
    private static final int T_FEATURE_TITLE = 0xFFFFF3FC;
    private static final int ACCENT = 0xFFD650AB;
    private static final int TOGGLE_ON = 0xFFEC4BAF;
    private static final int TOGGLE_OFF = 0xC2261B2D;
    private static final int TOGGLE_OUTLINE_IDLE = 0x664C335A;
    private static final int TOGGLE_OUTLINE_HOV = 0x8A7A4B90;

    private static final float GUI_W_RATIO = 0.49f;
    private static final float GUI_H_RATIO = 0.47f;
    private static final int GUI_MIN_W = 575;
    private static final int GUI_MIN_H = 320;
    private static final float SCREEN_OPEN_SPEED = 7.5f;
    private static final float SCREEN_CLOSE_SPEED = 11.0f;
    private static final int SCREEN_TRANSITION_Y = 10;

    private static final int SIDEBAR_W = 118;
    private static final int HEADER_H = 42;
    private static final int SIDEBAR_ROW_H = 24;
    private static final int SIDEBAR_ROW_GAP = 2;
    private static final int SIDEBAR_PILL_H = 16;
    private static final int SIDEBAR_PILL_RADIUS = 5;
    private static final float SIDEBAR_PILL_GLOW = 4f;
    private static final int ROW_H = 22;
    private static final int PAD = 10;
    private static final int CONTROL_H = 15;
    private static final int HEADER_ACTION_BTN_SIZE = 19;
    private static final int HEADER_ACTION_BTN_GAP = 6;
    private static final int HEADER_ACTION_BTN_RIGHT_PAD = 8;

    private static final String[] THEMES = {"Default (IQ)", "Dark", "Light", "Ocean (Blue)", "Crimson (Red)", "Emerald (Green)"};
    private static final boolean HIDE_LIGHT_THEME = true;
    private static final int LIGHT_THEME_INDEX = 2;
    private static final String[] DENSITY = {"Compact", "Normal", "Spaced"};
    private static final String[] TOGGLE_STYLES = {"Switch", "Checkbox", "Minimal"};
    private static final String[] FONTS = {"Minecraft", "Rounded", "Mono"};

    private final Screen parent;
    private final Class<?>[] configClasses;

    private final List<ActionHit> actionHits = new ArrayList<>();
    private final List<SliderHit> sliderHits = new ArrayList<>();
    private final List<SidebarSlot> sidebarSlots = new ArrayList<>();
    private final Map<String, Float> hoverAnims = new java.util.HashMap<>();
    private final Map<String, Float> toggleAnims = new java.util.HashMap<>();
    private final Map<String, Float> selectSlideAnims = new java.util.HashMap<>();
    private final Map<String, Integer> selectSlideDirs = new java.util.HashMap<>();

    private @Nullable SliderHit draggingSlider;
    private double contentScroll = 0;
    private double maxScroll = 0;
    private float panelAnim = 0f;
    private ConfigSection selectedSection = ConfigSection.GUI_VISUAL;
    private ConfigSection lastRenderedSection = ConfigSection.GUI_VISUAL;
    private double scrollVelocity = 0;
    private float catPillY = -1f;
    private float contentFadeAnim = 1f;
    private float screenTransition = 0f;
    private boolean closingScreen = false;
    private boolean closeHandled = false;
    private @Nullable Screen closeTarget;
    private long lastTransitionTimeMs = -1L;

    private int frameMouseX = 0;
    private int frameMouseY = 0;
    private @Nullable String pendingTooltip;

    private int themeIndex = 0;
    private double guiOpacity = 0.5;
    private double uiScale = 1.0;
    private boolean animationsEnabled = true;
    private double animationSpeed = 0.7;
    private boolean outlineShadow = true;
    private boolean uiStatePersistenceEnabled = true;

    private boolean autoReloadConfig = false;

    private boolean globalHudToggle = true;
    private double overlayOpacity = 0.9;
    private boolean snapToGrid = true;
    private boolean showBoundingBoxes = false;

    private int fontIndex = 0;
    private double layoutSpacing = 0.4;
    private int densityIndex = 1;
    private int toggleStyleIndex = 0;

    private boolean showFeatureLogs = false;
    private boolean debugOverlay = false;
    private boolean debugEvents = false;

    private static final DataKey<Integer> K_THEME = DataKey.of("globalcfg.theme", Integer.class);
    private static final DataKey<Double> K_GUI_OPACITY = DataKey.of("globalcfg.guiOpacity", Double.class);
    private static final DataKey<Double> K_UI_SCALE = DataKey.of("globalcfg.uiScale", Double.class);
    private static final DataKey<Boolean> K_ANIM_ENABLED = DataKey.of("globalcfg.animEnabled", Boolean.class);
    private static final DataKey<Double> K_ANIM_SPEED = DataKey.of("globalcfg.animSpeed", Double.class);
    private static final DataKey<Boolean> K_OUTLINE = DataKey.of("globalcfg.outline", Boolean.class);
    private static final DataKey<Boolean> K_AUTO_RELOAD = DataKey.of("globalcfg.autoReload", Boolean.class);
    private static final DataKey<Boolean> K_HUD = DataKey.of("globalcfg.hud", Boolean.class);
    private static final DataKey<Double> K_OVERLAY_OPACITY = DataKey.of("globalcfg.overlayOpacity", Double.class);
    private static final DataKey<Boolean> K_SNAP = DataKey.of("globalcfg.snap", Boolean.class);
    private static final DataKey<Boolean> K_BOUNDS = DataKey.of("globalcfg.bounds", Boolean.class);
    private static final DataKey<Integer> K_FONT = DataKey.of("globalcfg.font", Integer.class);
    private static final DataKey<Double> K_SPACING = DataKey.of("globalcfg.spacing", Double.class);
    private static final DataKey<Integer> K_DENSITY = DataKey.of("globalcfg.density", Integer.class);
    private static final DataKey<Integer> K_TOGGLE_STYLE = DataKey.of("globalcfg.toggleStyle", Integer.class);
    private static final DataKey<Boolean> K_DEBUG_LOGS = DataKey.of("globalcfg.debugLogs", Boolean.class);
    private static final DataKey<Boolean> K_DEBUG_OVERLAY = DataKey.of("globalcfg.debugOverlay", Boolean.class);
    private static final DataKey<Boolean> K_DEBUG_EVENTS = DataKey.of("globalcfg.debugEvents", Boolean.class);
    private static final DataKey<Boolean> K_UI_STATE_PERSIST = DataKey.of("globalcfg.uiStatePersist", Boolean.class);

    private final IQPersistentDataStore store = IQPersistentDataStore.get();

    public IQGlobalConfigurationScreen(@Nullable Screen parent, Class<?>... configClasses) {
        super(Component.literal("IQ Configuration Hub"));
        this.parent = parent;
        this.configClasses = configClasses;
    }

    @Override
    protected void init() {
        if (TEMP_HIDDEN) {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
            return;
        }
        loadState();
        hoverAnims.clear();
        toggleAnims.clear();
        selectSlideAnims.clear();
        selectSlideDirs.clear();
        scrollVelocity = 0;
        catPillY = -1f;
        contentFadeAnim = 1f;
        lastRenderedSection = selectedSection;
        screenTransition = 0f;
        closingScreen = false;
        closeHandled = false;
        closeTarget = parent;
        lastTransitionTimeMs = -1L;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor ctx, int mouseX, int mouseY, float a) {
        updateScreenTransition();
        frameMouseX = mouseX;
        frameMouseY = mouseY;
        pendingTooltip = null;

        float animLerp = animationsEnabled ? (float) (0.12 + (animationSpeed * 0.22)) : 1f;
        panelAnim += (1f - panelAnim) * animLerp;
        if (panelAnim > 0.995f) panelAnim = 1f;

        actionHits.clear();
        sliderHits.clear();

        double scaled = clamp(uiScale, 0.75, 1.35);
        int panelW = (int) (Math.max(GUI_MIN_W, width * GUI_W_RATIO) * scaled);
        int panelH = (int) (Math.max(GUI_MIN_H, height * GUI_H_RATIO) * scaled);
        panelW = Math.min(panelW, width - 34);
        panelH = Math.min(panelH, height - 26);
        int panelX = (width - panelW) / 2;
        int basePanelY = (height - panelH) / 2;
        float renderTransition = closingScreen ? easeInQuad(screenTransition) : easeOutCubic(screenTransition);
        int panelY = basePanelY + (int) ((1f - panelAnim) * 8f) + Math.round((1f - renderTransition) * SCREEN_TRANSITION_Y);
        int sidebarW = computeSidebarWidth(panelW);
        updateScrollAnimation();

        int overlayAlpha = (BG_OUTER >>> 24) & 0xFF;
        overlayAlpha = (int) (overlayAlpha * renderTransition);
        ctx.fill(0, 0, width, height, (overlayAlpha << 24) | (BG_OUTER & 0x00FFFFFF));

        int radius = 0;
        int accent = currentAccentColor();
        drawGlowBorder(ctx, panelX, panelY, panelW, panelH, radius, accent);
        drawRoundedRect(ctx, panelX, panelY, panelW, panelH, themePanelColor(), radius);
        int splitX = panelX + sidebarW;
        ctx.fill(splitX - 1, panelY, splitX, panelY + panelH, 0x22FFFFFF);

        renderSidebar(ctx, panelX, panelY, sidebarW, panelH);
        renderHeader(ctx, panelX + sidebarW, panelY, panelW - sidebarW);
        renderSections(ctx, panelX + sidebarW, panelY + HEADER_H, panelW - sidebarW, panelH - HEADER_H, accent);

        if (pendingTooltip != null) renderTooltip(ctx, pendingTooltip);
    }

    private void renderHeader(GuiGraphicsExtractor ctx, int x, int y, int w) {
        drawRoundedRect(ctx, x, y, w, HEADER_H, themeHeaderColor(), 0);
        ctx.fill(x, y + HEADER_H - 1, x + w, y + HEADER_H, 0x22FFFFFF);

        ConfigSection section = selectedSection;
        String title = section.title;
        String subtitle = section.headerSummary;

        int titleY = y + 8;
        int btnY = y + (HEADER_H - HEADER_ACTION_BTN_SIZE) / 2;
        int backX = getHeaderActionButtonsStartX(x, w);
        int closeX = backX + HEADER_ACTION_BTN_SIZE + HEADER_ACTION_BTN_GAP;
        int textMaxW = Math.max(40, (backX - 8) - (x + 10));
        if (IqFonts.widthPx(title) > textMaxW) title = IqFonts.trim(title, textMaxW - 6) + "...";
        if (IqFonts.widthPx(subtitle) > textMaxW) subtitle = IqFonts.trim(subtitle, textMaxW - 6) + "...";

        IqFonts.draw(ctx, title, x + 10, titleY, tMain());
        IqFonts.draw(ctx, subtitle,
                x + 10, titleY + IqFonts.lineHeight() + 2, tMuted());

        boolean backHover  = isIn(frameMouseX, frameMouseY, backX,  btnY, HEADER_ACTION_BTN_SIZE, HEADER_ACTION_BTN_SIZE);
        boolean closeHover = isIn(frameMouseX, frameMouseY, closeX, btnY, HEADER_ACTION_BTN_SIZE, HEADER_ACTION_BTN_SIZE);
        float backAnim = hoverAnims.getOrDefault("hdr#back", 0f);
        backAnim += ((backHover ? 1f : 0f) - backAnim) * animRate(0.30f);
        hoverAnims.put("hdr#back", backAnim);

        float closeAnim = hoverAnims.getOrDefault("hdr#close", 0f);
        closeAnim += ((closeHover ? 1f : 0f) - closeAnim) * animRate(0.30f);
        hoverAnims.put("hdr#close", closeAnim);

        drawHeaderActionButton(ctx, backX,  btnY, backAnim);
        drawHeaderActionButton(ctx, closeX, btnY, closeAnim);

        int iconOffX = (HEADER_ACTION_BTN_SIZE - ICON_RENDER_SIZE) / 2;
        int iconOffY = (HEADER_ACTION_BTN_SIZE - ICON_RENDER_SIZE) / 2;

        IqIcons.draw(ctx, BACK_ICON_TEXTURE, backX + iconOffX, btnY + iconOffY, ICON_RENDER_SIZE, 0xFFFFFFFF);
        IqIcons.draw(ctx, CLOSE_ICON_TEXTURE, closeX + iconOffX, btnY + iconOffY, ICON_RENDER_SIZE, 0xFFFFFFFF);

        if (backHover)  pendingTooltip = "Back to main configuration";
        if (closeHover) pendingTooltip = "Close config";

        actionHits.add(new ActionHit(backX,  btnY, HEADER_ACTION_BTN_SIZE, HEADER_ACTION_BTN_SIZE,
                this::goBack, null));
        actionHits.add(new ActionHit(closeX, btnY, HEADER_ACTION_BTN_SIZE, HEADER_ACTION_BTN_SIZE,
                this::closeCompletely, null));
    }

    private void renderSidebar(GuiGraphicsExtractor ctx, int x, int y, int w, int h) {
        drawRoundedRect(ctx, x, y, w, h, themeSidebarColor(), 0);

        drawRoundedRect(ctx, x, y, w, HEADER_H, themeHeaderColor(), 0);
        ctx.fill(x, y + HEADER_H - 1, x + w, y + HEADER_H, 0x22FFFFFF);
        IqFonts.draw(ctx, themeHubTitle(), x + 10, y + 7, tHubTitle());
        IqFonts.draw(ctx, themeHubSubtitle(), x + 10,
                y + 7 + IqFonts.lineHeight() + 2, tHubSubtitle());

        sidebarSlots.clear();
        int rowY = y + HEADER_H + 8;
        for (ConfigSection section : ConfigSection.values()) {
            sidebarSlots.add(new SidebarSlot(section, rowY));
            if (section == selectedSection) {
                float target = rowY + (SIDEBAR_ROW_H - SIDEBAR_PILL_H) * 0.5f;
                if (catPillY < 0f) catPillY = target;
                catPillY += (target - catPillY) * animRate(0.18f);
            }
            rowY += SIDEBAR_ROW_H + SIDEBAR_ROW_GAP;
        }

        if (catPillY >= 0f) {
            int pillY = Math.round(catPillY);
            int accent = themeAccentColor();
            int pillX = x + 8;
            int pillW = w - 16;
            drawRoundedGlow(ctx, pillX, pillY, pillW, SIDEBAR_PILL_H,
                    withAlpha(accent, 0x4A), SIDEBAR_PILL_RADIUS, SIDEBAR_PILL_GLOW);
        }

        for (SidebarSlot slot : sidebarSlots) {
            ConfigSection section = slot.section();
            int sy = slot.y();
            boolean active = section == selectedSection;
            boolean hovered = isIn(frameMouseX, frameMouseY, x + 8, sy, w - 16, SIDEBAR_ROW_H);
            String hKey = "side#" + section.name();
            float hAnim = hoverAnims.getOrDefault(hKey, 0f);
            hAnim += ((hovered ? 1f : 0f) - hAnim) * animRate(0.28f);
            hoverAnims.put(hKey, hAnim);

            if (!active && hAnim > 0.01f) {
                int hoverY = sy + (SIDEBAR_ROW_H - SIDEBAR_PILL_H) / 2;
                drawRoundedRect(ctx, x + 8, hoverY, w - 16, SIDEBAR_PILL_H,
                        lerpArgb(0x00000000, 0x149A6BB8, hAnim), SIDEBAR_PILL_RADIUS);
            }

            String label = section.title;
            if (IqFonts.widthPx(label) > w - 28) {
                label = IqFonts.trim(label, w - 34) + "…";
            }
            IqFonts.draw(ctx, label, x + 16,
                    sy + (SIDEBAR_ROW_H - IqFonts.lineHeight()) * 0.5f,
                    active ? tMain() : (hovered ? tActionTextHov() : tMuted()));

            if (hovered) pendingTooltip = section.tooltip;

            actionHits.add(new ActionHit(x + 8, sy, w - 16, SIDEBAR_ROW_H,
                    () -> {
                        if (selectedSection != section) {
                            selectedSection = section;
                            contentFadeAnim = 0f;
                        }
                    }, section.tooltip));
        }
    }

    private void renderSections(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int accent) {
        ctx.fill(x, y, x + w, y + h, themePanelColor());
        if (lastRenderedSection != selectedSection) {
            contentFadeAnim = 0f;
            lastRenderedSection = selectedSection;
        }
        contentFadeAnim += (1f - contentFadeAnim) * animRate(0.20f);
        if (contentFadeAnim > 0.997f) contentFadeAnim = 1f;

        int clipX1 = x + PAD;
        int clipY1 = y + PAD;
        int clipX2 = x + w - PAD;
        int clipY2 = y + h;

        ctx.enableScissor(clipX1, clipY1, clipX2, clipY2);

        int contentW = w - PAD * 2;
        int cursorY = y + PAD - (int) contentScroll;
        int usedH = 0;

        int before = cursorY;
        cursorY = switch (selectedSection) {
            case GUI_VISUAL -> renderGuiVisualRows(ctx, x + PAD, cursorY, contentW);
            case SYSTEM_RELOAD -> renderSystemRows(ctx, x + PAD, cursorY, contentW);
            case FEATURES_CONTROL -> renderFeatureRows(ctx, x + PAD, cursorY, contentW);
            case HUD_OVERLAY -> renderHudRows(ctx, x + PAD, cursorY, contentW);
            case DEBUG -> renderDebugRows(ctx, x + PAD, cursorY, contentW);
        };
        usedH += (cursorY - before);

        ctx.disableScissor();
        maxScroll = Math.max(0, usedH - (h - 12));
        contentScroll = clamp(contentScroll, 0, maxScroll);

        if (maxScroll > 0) {
            int trackX = x + w - 6;
            int trackTop = y + 10;
            int trackH = h - 14;
            double ratio = (double) trackH / (trackH + maxScroll);
            int thumbH = Math.max(26, (int) (trackH * ratio));
            int thumbY = trackTop + (int) ((contentScroll / maxScroll) * (trackH - thumbH));
            ctx.fill(trackX, trackTop, trackX + 2, trackTop + trackH, 0x24FFFFFF);
            ctx.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, accent);
        }

        if (contentFadeAnim < 1f) {
            int overlayAlpha = (int) ((1f - contentFadeAnim) * 0xCC);
            if (overlayAlpha > 0) {
                ctx.fill(x, y, x + w, y + h, (overlayAlpha << 24) | (themePanelColor() & 0x00FFFFFF));
            }
        }
    }

    private int renderGuiVisualRows(GuiGraphicsExtractor ctx, int x, int y, int w) {
        y = renderCycleRow(ctx, x, y, w, "GUI Theme", "Theme profile for this interface.", visibleThemeLabel(),
                this::cycleThemeForward);
        y = renderSliderRow(ctx, x, y, w, "GUI Opacity", "Overall transparency of the panel.", 0.35, 1.0,
                () -> guiOpacity, v -> guiOpacity = v, valuePercent(guiOpacity));
        y = renderSliderRow(ctx, x, y, w, "UI Scale", "Scale multiplier for this panel.", 0.75, 1.35,
                () -> uiScale, v -> uiScale = v, String.format(Locale.ROOT, "%.2fx", uiScale));
        y = renderToggleRow(ctx, x, y, w, "GUI Animations", "Enable transitions and micro-interactions.",
                animationsEnabled, () -> animationsEnabled = !animationsEnabled);
        y = renderSliderRow(ctx, x, y, w, "Animation Speed", "Speed for hover/fade transitions.", 0.2, 1.0,
                () -> animationSpeed, v -> animationSpeed = v, valuePercent(animationSpeed));
        y = renderToggleRow(ctx, x, y, w, "Outline / Shadow", "Adds subtle border and depth around controls.",
                outlineShadow, () -> outlineShadow = !outlineShadow);
        y = renderToggleRow(ctx, x, y, w, "Persist UI Session", "Remember category, scroll and search when reopening config.",
                uiStatePersistenceEnabled, () -> uiStatePersistenceEnabled = !uiStatePersistenceEnabled);
        return y;
    }

    private int renderSystemRows(GuiGraphicsExtractor ctx, int x, int y, int w) {
        y = renderActionRow(ctx, x, y, w, "Update Config Files", "Updates JSON config files to default values.",
                this::updateConfigFiles);
        y = renderActionRow(ctx, x, y, w, "Reload IQ", "Runs /iq reload and refreshes config files.", this::runIqReload);
        y = renderActionRow(ctx, x, y, w, "Soft Restart Features", "Re-initializes feature states without restart.",
                this::softRestartFeatures);
        y = renderToggleRow(ctx, x, y, w, "Auto-reload Config", "Automatically refresh when config changes are detected.",
                autoReloadConfig, () -> autoReloadConfig = !autoReloadConfig);
        return y;
    }

    private int renderFeatureRows(GuiGraphicsExtractor ctx, int x, int y, int w) {
        y = renderActionRow(ctx, x, y, w, "Disable All Features", "Turns off all boolean feature toggles.", this::disableAllFeatures);
        y = renderActionRow(ctx, x, y, w, "Reset Features to Default", "Resets feature toggles to a neutral default.",
                this::resetAllFeaturesDefault);
        y = renderActionRow(ctx, x, y, w, "Export Config", "Copies global settings as text to clipboard.", this::exportConfigToClipboard);
        y = renderActionRow(ctx, x, y, w, "Import Config", "Reads settings from clipboard and applies them.", this::importConfigFromClipboard);
        return y;
    }

    private int renderHudRows(GuiGraphicsExtractor ctx, int x, int y, int w) {
        y = renderToggleRow(ctx, x, y, w, "Global HUD Toggle", "Master on/off for all HUD overlays.",
                globalHudToggle, () -> globalHudToggle = !globalHudToggle);
        y = renderSliderRow(ctx, x, y, w, "Overlay Opacity", "Opacity applied to in-world overlays.", 0.2, 1.0,
                () -> overlayOpacity, v -> overlayOpacity = v, valuePercent(overlayOpacity));
        y = renderToggleRow(ctx, x, y, w, "Snap to Grid", "Align HUD elements to a placement grid.",
                snapToGrid, () -> snapToGrid = !snapToGrid);
        y = renderToggleRow(ctx, x, y, w, "Show Bounding Boxes", "Displays HUD element bounds while editing.",
                showBoundingBoxes, () -> showBoundingBoxes = !showBoundingBoxes);
        y = renderActionRow(ctx, x, y, w, "Reset HUD Positions", "Moves all HUD elements back to default positions.",
                this::resetHudPositions);
        return y;
    }

    private int renderAdvancedRows(GuiGraphicsExtractor ctx, int x, int y, int w) {
        y = renderCycleRow(ctx, x, y, w, "GUI Font", "Selects a GUI font preset.", FONTS[fontIndex],
                () -> fontIndex = (fontIndex + 1) % FONTS.length);
        y = renderSliderRow(ctx, x, y, w, "Element Spacing", "Global padding/margin density.", 0.0, 1.0,
                () -> layoutSpacing, v -> layoutSpacing = v, valuePercent(layoutSpacing));
        y = renderCycleRow(ctx, x, y, w, "Layout Density", "Compactness of rows and controls.", DENSITY[densityIndex],
                () -> densityIndex = (densityIndex + 1) % DENSITY.length);
        y = renderCycleRow(ctx, x, y, w, "Toggle Style", "Visual style for switches and checkers.", TOGGLE_STYLES[toggleStyleIndex],
                () -> toggleStyleIndex = (toggleStyleIndex + 1) % TOGGLE_STYLES.length);
        y = renderActionRow(ctx, x, y, w, "Custom Styling", "Copies current style string for manual tweaks.", this::copyStylePreset);
        return y;
    }

    private int renderDebugRows(GuiGraphicsExtractor ctx, int x, int y, int w) {
        y = renderToggleRow(ctx, x, y, w, "Feature Logs", "Shows runtime logs for feature events.",
                showFeatureLogs, () -> showFeatureLogs = !showFeatureLogs);
        y = renderToggleRow(ctx, x, y, w, "Debug Overlay", "Renders technical overlays for debugging.",
                debugOverlay, () -> debugOverlay = !debugOverlay);
        y = renderToggleRow(ctx, x, y, w, "Debug Triggers", "Shows internal trigger/event activity.",
                debugEvents, () -> debugEvents = !debugEvents);
        y = renderActionRow(ctx, x, y, w, "Copy Debug Info", "Copies a concise debug report to clipboard.", this::copyDebugInfo);
        return y;
    }

    private int renderToggleRow(GuiGraphicsExtractor ctx, int x, int y, int w, String label, String tooltip,
                                boolean value, Runnable action) {
        boolean hover = isIn(frameMouseX, frameMouseY, x, y, w, ROW_H);
        String hKey = "row#tog#" + selectedSection.name() + "#" + label;
        float hAnim = hoverAnims.getOrDefault(hKey, 0f);
        hAnim += ((hover ? 1f : 0f) - hAnim) * animRate(0.28f);
        hoverAnims.put(hKey, hAnim);

        drawRoundedRect(ctx, x, y, w, ROW_H, lerpArgb(themeRowColor(), themeRowHoverColor(), hAnim), 0);
        drawRoundedHollowRect(ctx, x, y, w, ROW_H, lerpArgb(themeBorderDim(), themeOutlineHighlight(), hAnim), 0);
        int accentAlpha = (int) (0x30 * hAnim);
        if (accentAlpha > 0) ctx.fill(x, y, x + 2, y + ROW_H, withAlpha(themeAccentColor(), accentAlpha));

        IqFonts.draw(ctx, label, x + 8,
                y + (ROW_H - IqFonts.lineHeight()) / 2, tMain());

        int tx = x + w - 38;
        int ty = y + (ROW_H - 14) / 2;
        int tw = 28;
        int th = 14;

        String tKey = "tog#" + selectedSection.name() + "#" + label;
        float tTarget = value ? 1f : 0f;
        float tAnim = toggleAnims.getOrDefault(tKey, tTarget);
        tAnim += (tTarget - tAnim) * animRate(0.22f);
        if (Math.abs(tAnim - tTarget) < 0.004f) tAnim = tTarget;
        toggleAnims.put(tKey, tAnim);

        drawRoundedRect(ctx, tx, ty, tw, th, lerpArgb(themeToggleOff(), themeToggleOn(), tAnim), th / 2);
        int outline = hover ? TOGGLE_OUTLINE_HOV : TOGGLE_OUTLINE_IDLE;
        drawRoundedHollowRect(ctx, tx, ty, tw, th, outline, th / 2);
        int knobOffX = tx + 1;
        int knobOnX = tx + tw - th + 1;
        int knobX = knobOffX + (int) ((knobOnX - knobOffX) * tAnim);
        drawRoundedRect(ctx, knobX, ty + 1, th - 2, th - 2, themeToggleHandle(tAnim), (th - 2) / 2);

        actionHits.add(new ActionHit(x, y, w, ROW_H, action, tooltip));
        if (hover) pendingTooltip = tooltip;
        return y + ROW_H + 4;
    }

    private int renderCycleRow(GuiGraphicsExtractor ctx, int x, int y, int w, String label, String tooltip,
                               String value, Runnable action) {
        boolean hover = isIn(frameMouseX, frameMouseY, x, y, w, ROW_H);
        String hKey = "row#cyc#" + selectedSection.name() + "#" + label;
        float hAnim = hoverAnims.getOrDefault(hKey, 0f);
        hAnim += ((hover ? 1f : 0f) - hAnim) * animRate(0.28f);
        hoverAnims.put(hKey, hAnim);

        drawRoundedRect(ctx, x, y, w, ROW_H, lerpArgb(themeRowColor(), themeRowHoverColor(), hAnim), 0);
        drawRoundedHollowRect(ctx, x, y, w, ROW_H, lerpArgb(themeBorderDim(), themeOutlineHighlight(), hAnim), 0);
        int accentAlpha = (int) (0x30 * hAnim);
        if (accentAlpha > 0) ctx.fill(x, y, x + 2, y + ROW_H, withAlpha(themeAccentColor(), accentAlpha));

        IqFonts.draw(ctx, label, x + 8,
                y + (ROW_H - IqFonts.lineHeight()) / 2, tFeatureTitle());

        int bw = Math.min(170, IqFonts.widthPx(value) + 24);
        int bx = x + w - bw - 8;
        int by = y + 4;
        renderActionControl(ctx, bx, by, bw, hover);

        String sKey = "sel#" + selectedSection.name() + "#" + label;
        float prog = selectSlideAnims.getOrDefault(sKey, 0f);
        prog += (0f - prog) * animRate(0.22f);
        if (Math.abs(prog) < 0.004f) prog = 0f;
        selectSlideAnims.put(sKey, prog);
        int slideDir = selectSlideDirs.getOrDefault(sKey, 1);
        int xOff = (int) (prog * slideDir * (bw * 0.55f));

        ctx.enableScissor(bx + 2, by, bx + bw - 2, by + CONTROL_H);
        IqFonts.drawCentered(ctx, value + " >", bx + bw / 2 - xOff, y + (ROW_H - IqFonts.lineHeight()) / 2, 0, IqFonts.lineHeight(), hover ? tActionTextHov() : tActionText());
        ctx.disableScissor();

        actionHits.add(new ActionHit(x, y, w, ROW_H, () -> {
            action.run();
            selectSlideAnims.put(sKey, 1f);
            selectSlideDirs.put(sKey, -1);
        }, tooltip));
        if (hover) pendingTooltip = tooltip;
        return y + ROW_H + 4;
    }

    private int renderSliderRow(GuiGraphicsExtractor ctx, int x, int y, int w, String label, String tooltip,
                                double min, double max, DoubleSupplier getter, DoubleConsumer setter,
                                String valueText) {
        boolean hover = isIn(frameMouseX, frameMouseY, x, y, w, ROW_H);
        String hKey = "row#sld#" + selectedSection.name() + "#" + label;
        float hAnim = hoverAnims.getOrDefault(hKey, 0f);
        hAnim += ((hover ? 1f : 0f) - hAnim) * animRate(0.28f);
        hoverAnims.put(hKey, hAnim);

        int accent = currentAccentColor();
        drawRoundedRect(ctx, x, y, w, ROW_H, lerpArgb(themeRowColor(), themeRowHoverColor(), hAnim), 0);
        drawRoundedHollowRect(ctx, x, y, w, ROW_H, lerpArgb(themeBorderDim(), themeOutlineHighlight(), hAnim), 0);
        int accentAlpha = (int) (0x30 * hAnim);
        if (accentAlpha > 0) ctx.fill(x, y, x + 2, y + ROW_H, withAlpha(themeAccentColor(), accentAlpha));

        IqFonts.draw(ctx, label, x + 8,
                y + (ROW_H - IqFonts.lineHeight()) / 2, tMain());

        int sx = x + w - 148;
        int sy = y + ROW_H / 2 - 3;
        int sw = 96;

        double value = clamp(getter.getAsDouble(), min, max);
        double t = (value - min) / (max - min);

        drawRoundedRect(ctx, sx, sy, sw, 6, 0xA0352749, 3);
        drawRoundedRect(ctx, sx, sy, (int) (sw * t), 6, accent, 3);
        int hx = sx + (int) (sw * t) - 3;
        drawRoundedRect(ctx, hx, sy - 3, 6, 12, 0xFFFFFFFF, 3);

        String vt = "§8" + valueText;
        IqFonts.draw(ctx, vt, sx + sw + 8,
                y + (ROW_H - IqFonts.lineHeight()) / 2, tMuted());

        sliderHits.add(new SliderHit(sx, sy - 5, sw, 16, min, max, getter, setter, tooltip));
        if (hover) pendingTooltip = tooltip;
        return y + ROW_H + 4;
    }

    private int renderActionRow(GuiGraphicsExtractor ctx, int x, int y, int w, String label, String tooltip, Runnable action) {
        boolean hover = isIn(frameMouseX, frameMouseY, x, y, w, ROW_H);
        String hKey = "row#act#" + selectedSection.name() + "#" + label;
        float hAnim = hoverAnims.getOrDefault(hKey, 0f);
        hAnim += ((hover ? 1f : 0f) - hAnim) * animRate(0.28f);
        hoverAnims.put(hKey, hAnim);

        drawRoundedRect(ctx, x, y, w, ROW_H, lerpArgb(themeRowColor(), themeRowHoverColor(), hAnim), 0);
        drawRoundedHollowRect(ctx, x, y, w, ROW_H, lerpArgb(themeBorderDim(), themeOutlineHighlight(), hAnim), 0);
        int accentAlpha = (int) (0x30 * hAnim);
        if (accentAlpha > 0) ctx.fill(x, y, x + 2, y + ROW_H, withAlpha(themeAccentColor(), accentAlpha));

        int bx = x + w - 76;
        int by = y + 4;
        renderActionControl(ctx, bx, by, 68, hover);
        String buttonText = actionButtonText(label);

        IqFonts.draw(ctx, label, x + 8,
                y + (ROW_H - IqFonts.lineHeight()) / 2, tFeatureTitle());
        IqFonts.drawCentered(ctx, buttonText, bx + 34, y + (ROW_H - IqFonts.lineHeight()) / 2, 0, IqFonts.lineHeight(), hover ? tActionTextHov() : tActionText());

        actionHits.add(new ActionHit(x, y, w, ROW_H, action, tooltip));
        if (hover) pendingTooltip = tooltip;
        return y + ROW_H + 4;
    }

    private String actionButtonText(String label) {
        String l = label.toLowerCase(Locale.ROOT);
        if (l.startsWith("disable")) return "Disable";
        if (l.startsWith("enable")) return "Enable";
        if (l.startsWith("reset")) return "Reset";
        if (l.startsWith("reload")) return "Reload";
        if (l.startsWith("soft restart")) return "Restart";
        if (l.startsWith("export")) return "Export";
        if (l.startsWith("import")) return "Import";
        if (l.startsWith("copy")) return "Copy";
        if (l.startsWith("update")) return "Update";
        return "Run";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (closingScreen) return true;
        int mx = (int) click.x();
        int my = (int) click.y();

        if (click.button() == 0) {
            for (SliderHit sliderHit : sliderHits) {
                if (sliderHit.contains(mx, my)) {
                    draggingSlider = sliderHit;
                    sliderHit.update(mx);
                    return true;
                }
            }

            for (ActionHit actionHit : actionHits) {
                if (actionHit.contains(mx, my)) {
                    try {
                        actionHit.action().run();
                        saveState();
                    } catch (Exception e) {
                        log.warn("Configuration action failed", e);
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (closingScreen) return true;
        if (draggingSlider != null) {
            draggingSlider.update((int) click.x());
            saveState();
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (closingScreen) return true;
        draggingSlider = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (closingScreen) return true;
        scrollVelocity -= vAmt * 16;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (closingScreen) return true;
        if (input.key() == 256) {
            goBack();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        requestClose(parent);
    }

    private void goBack() {
        requestClose(parent);
    }

    private void closeCompletely() {
        requestClose(null);
    }

    private void requestClose(@Nullable Screen target) {
        if (closingScreen) return;
        closingScreen = true;
        closeHandled = false;
        closeTarget = target;
        draggingSlider = null;
        scrollVelocity = 0;
    }

    private void finalizeClose() {
        closeHandled = true;
        saveState();
        try {
            net.iqaddons.mod.IQModClient.get().getConfigurator().saveConfig(Configuration.class);
        } catch (Exception e) {
            log.warn("Failed to save global settings state", e);
        }
        if (minecraft != null) minecraft.setScreen(closeTarget);
    }

    private void updateScreenTransition() {
        long now = Util.getMillis();
        if (lastTransitionTimeMs < 0L) {
            lastTransitionTimeMs = now;
        }

        float dt = Math.min(50L, now - lastTransitionTimeMs) / 1000f;
        lastTransitionTimeMs = now;

        float speed = closingScreen ? SCREEN_CLOSE_SPEED : SCREEN_OPEN_SPEED;
        float direction = closingScreen ? -1f : 1f;
        screenTransition = (float) clamp(screenTransition + (direction * speed * dt), 0.0, 1.0);

        if (closingScreen && !closeHandled && screenTransition <= 0f) {
            finalizeClose();
        }
    }

    private float easeInQuad(float t) {
        float clamped = (float) clamp(t, 0.0, 1.0);
        return clamped * clamped;
    }

    private float easeOutCubic(float t) {
        float u = 1f - (float) clamp(t, 0.0, 1.0);
        return 1f - (u * u * u);
    }

    private void updateScrollAnimation() {
        if (scrollVelocity != 0) {
            contentScroll = clamp(contentScroll + scrollVelocity, 0, maxScroll);
            scrollVelocity *= animationsEnabled ? (0.64 + (animationSpeed * 0.20)) : 0.0;
            if (Math.abs(scrollVelocity) < 0.05) scrollVelocity = 0;
        }
        contentScroll = clamp(contentScroll, 0, maxScroll);
    }

    private void runIqReload() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.connection.sendCommand("iq reload");
            MessageUtil.SUCCESS.sendMessage("Executed /iq reload");
        }
    }

    private void softRestartFeatures() {
        contentScroll = 0;
        MessageUtil.INFO.sendMessage("Soft restart requested for feature systems.");
    }

    private void disableAllFeatures() {
        int updated = applyBooleanFeatures((entry, field) -> false);
        MessageUtil.WARNING.sendMessage("Disabled " + updated + " feature toggles.");
    }


    private void resetAllFeaturesDefault() {
        try {
            net.iqaddons.mod.IQModClient iq = net.iqaddons.mod.IQModClient.get();
            if (iq == null) {
                MessageUtil.ERROR.sendMessage("IQ client is not initialized yet.");
                return;
            }

            iq.resetMainConfigToDefaults();
            reloadRuntimeConfigCaches();
            MessageUtil.SUCCESS.sendMessage("Features reset to default values and applied instantly.");
        } catch (Exception e) {
            MessageUtil.ERROR.sendMessage("Failed to reset features: " + e.getMessage());
            log.warn("Error resetting features to default", e);
        }
    }

    private void reloadRuntimeConfigCaches() {
        try {
            java.util.List<net.iqaddons.mod.model.spot.PileLocation> pileLocations =
                    net.iqaddons.mod.config.loader.PileConfigLoader.get().reload();
            net.iqaddons.mod.manager.SupplyStateManager.get().reloadPileLocations(pileLocations);
            net.iqaddons.mod.config.loader.CratePriorityConfigLoader.get().reload();

            net.iqaddons.mod.IQModClient iq = net.iqaddons.mod.IQModClient.get();
            if (iq != null && iq.getFeatureManager() != null) {
                net.iqaddons.mod.features.kuudra.waypoints.PearlWaypointFeature pearlFeature =
                        iq.getFeatureManager().get(net.iqaddons.mod.features.kuudra.waypoints.PearlWaypointFeature.class);
                if (pearlFeature != null) {
                    pearlFeature.reloadConfig();
                } else {
                    net.iqaddons.mod.config.loader.WaypointConfigLoader.get().reload();
                }

                net.iqaddons.mod.features.kuudra.waypoints.EtherwarpHelperFeature etherwarpFeature =
                        iq.getFeatureManager().get(net.iqaddons.mod.features.kuudra.waypoints.EtherwarpHelperFeature.class);
                if (etherwarpFeature != null) {
                    etherwarpFeature.reloadConfig();
                } else {
                    java.util.List<net.iqaddons.mod.model.etherwarp.EtherwarpCategory> categories =
                            net.iqaddons.mod.config.loader.EtherwarpConfigLoader.get().reload();
                    net.iqaddons.mod.manager.EtherwarpCategoryToggleManager.get().syncWithCategories(categories);
                }
                return;
            }

            net.iqaddons.mod.config.loader.WaypointConfigLoader.get().reload();
            java.util.List<net.iqaddons.mod.model.etherwarp.EtherwarpCategory> categories =
                    net.iqaddons.mod.config.loader.EtherwarpConfigLoader.get().reload();
            net.iqaddons.mod.manager.EtherwarpCategoryToggleManager.get().syncWithCategories(categories);
        } catch (Exception e) {
            log.warn("Runtime cache reload failed after feature default reset, falling back to /iq reload", e);
            runIqReload();
        }
    }

    private void updateConfigFiles() {
        try {
            java.nio.file.Path configDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("iq");
            
            java.nio.file.Path[] configFiles = {
                configDir.resolve("crate_priority.json"),
                configDir.resolve("etherwarp_config.json"),
                configDir.resolve("pearl_waypoints.json"),
                configDir.resolve("pile_locations.json")
            };
            
            for (java.nio.file.Path file : configFiles) {
                if (java.nio.file.Files.exists(file)) {
                    java.nio.file.Files.delete(file);
                }
            }
            
            net.iqaddons.mod.config.loader.CratePriorityConfigLoader.get().reload();
            net.iqaddons.mod.config.loader.PileConfigLoader.get().reload();
            net.iqaddons.mod.config.loader.WaypointConfigLoader.get().reload();
            java.util.List<net.iqaddons.mod.model.etherwarp.EtherwarpCategory> categories = 
                net.iqaddons.mod.config.loader.EtherwarpConfigLoader.get().reload();
            net.iqaddons.mod.manager.EtherwarpCategoryToggleManager.get().syncWithCategories(categories);
            
            net.iqaddons.mod.IQModClient iqClient = net.iqaddons.mod.IQModClient.get();
            if (iqClient != null && iqClient.getFeatureManager() != null) {
                net.iqaddons.mod.features.kuudra.waypoints.PearlWaypointFeature pearlFeature = 
                    iqClient.getFeatureManager().get(net.iqaddons.mod.features.kuudra.waypoints.PearlWaypointFeature.class);
                if (pearlFeature != null) {
                    pearlFeature.reloadConfig();
                }
                
                net.iqaddons.mod.features.kuudra.waypoints.EtherwarpHelperFeature etherwarpFeature = 
                    iqClient.getFeatureManager().get(net.iqaddons.mod.features.kuudra.waypoints.EtherwarpHelperFeature.class);
                if (etherwarpFeature != null) {
                    etherwarpFeature.reloadConfig();
                }
            }
            
            MessageUtil.SUCCESS.sendMessage("Config files updated to default values successfully.");
        } catch (Exception e) {
            MessageUtil.ERROR.sendMessage("Failed to update config files: " + e.getMessage());
            log.warn("Error updating config files", e);
        }
    }

    private int applyBooleanFeatures(BooleanValueSelector selector) {
        int updated = 0;
        for (Class<?> configClass : configClasses) {
            ConfigCategory category = ConfigReflectionUtil.buildCategory(configClass);
            if (category == null) continue;
            updated += applyToEntryList(category.entries(), selector);
        }
        return updated;
    }

    private int applyToEntryList(List<ConfigEntryModel> entries, BooleanValueSelector selector) {
        int updated = 0;
        for (ConfigEntryModel entry : entries) {
            if (entry.getType() == EntryType.SECTION_HEADER && entry.getChildren() != null) {
                updated += applyToEntryList(entry.getChildren(), selector);
                continue;
            }
            if (entry.getType() != EntryType.BOOLEAN || entry.getField() == null) continue;
            try {
                boolean value = selector.select(entry, entry.getField());
                entry.getField().set(null, value);
                updated++;
            } catch (Exception e) {
                log.warn("Failed updating boolean entry {}", entry.getLabel(), e);
            }
        }
        return updated;
    }

    private void exportConfigToClipboard() {
        if (minecraft == null) return;
        java.util.Map<String, Field> fields = collectMainConfigFields();
        if (fields.isEmpty()) {
            MessageUtil.ERROR.sendMessage("No main config fields available to export.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        java.util.List<String> keys = new java.util.ArrayList<>(fields.keySet());
        java.util.Collections.sort(keys);
        for (String key : keys) {
            Field f = fields.get(key);
            if (f == null) continue;
            try {
                Object v = f.get(null);
                String value = v instanceof Enum<?> en ? en.name() : String.valueOf(v);
                if (sb.length() > 0) sb.append(';');
                sb.append(key).append('=').append(value);
            } catch (Exception ignored) {
            }
        }

        minecraft.keyboardHandler.setClipboard(sb.toString());
        MessageUtil.SUCCESS.sendMessage("Main config copied to clipboard.");
    }

    private void importConfigFromClipboard() {
        if (minecraft == null) return;
        String text = minecraft.keyboardHandler.getClipboard();
        if (text == null || text.isBlank()) {
            MessageUtil.ERROR.sendMessage("Clipboard is empty.");
            return;
        }

        java.util.Map<String, Field> fields = collectMainConfigFields();
        if (fields.isEmpty()) {
            MessageUtil.ERROR.sendMessage("No main config fields available to import.");
            return;
        }

        int applied = 0;
        for (String token : text.split(";")) {
            String ln = token.trim();
            if (ln.isEmpty() || !ln.contains("=")) continue;
            String[] split = ln.split("=", 2);
            String key = split[0].trim();
            String val = split[1].trim();
            Field field = fields.get(key);
            if (field == null) continue;
            try {
                Object parsed = parseFieldValue(field, val);
                if (parsed != null) {
                    field.set(null, parsed);
                    applied++;
                }
            } catch (Exception ignored) {
            }
        }

        MessageUtil.SUCCESS.sendMessage("Imported " + applied + " main config values.");
        try {
            net.iqaddons.mod.IQModClient.get().getConfigurator().saveConfig(Configuration.class);
        } catch (Exception ignored) {
        }
    }

    private java.util.Map<String, Field> collectMainConfigFields() {
        java.util.Map<String, Field> fields = new java.util.LinkedHashMap<>();
        for (Class<?> configClass : configClasses) {
            ConfigCategory category = ConfigReflectionUtil.buildCategory(configClass);
            if (category == null) continue;
            collectFieldEntries(category.entries(), fields);
        }
        return fields;
    }

    private void collectFieldEntries(List<ConfigEntryModel> entries, java.util.Map<String, Field> out) {
        for (ConfigEntryModel entry : entries) {
            if (entry.getType() == EntryType.SECTION_HEADER && entry.getChildren() != null) {
                collectFieldEntries(entry.getChildren(), out);
                continue;
            }
            Field f = entry.getField();
            if (f == null) continue;
            Class<?> type = f.getType();
            if (!(type == boolean.class || type == Boolean.class
                    || type == int.class || type == Integer.class
                    || type == float.class || type == Float.class
                    || type == double.class || type == Double.class
                    || type == long.class || type == Long.class
                    || type == String.class || type.isEnum())) {
                continue;
            }
            String key = f.getDeclaringClass().getSimpleName() + "." + f.getName();
            out.put(key, f);
        }
    }

    private @Nullable Object parseFieldValue(Field field, String raw) {
        Class<?> type = field.getType();
        try {
            if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(raw);
            if (type == int.class || type == Integer.class) return Integer.parseInt(raw);
            if (type == float.class || type == Float.class) return Float.parseFloat(raw);
            if (type == double.class || type == Double.class) return Double.parseDouble(raw);
            if (type == long.class || type == Long.class) return Long.parseLong(raw);
            if (type == String.class) return raw;
            if (type.isEnum()) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Object enumVal = Enum.valueOf((Class<? extends Enum>) type.asSubclass(Enum.class), raw);
                return enumVal;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void resetHudPositions() {
        MessageUtil.INFO.sendMessage("HUD positions reset to default.");
    }

    private void copyStylePreset() {
        if (minecraft == null) return;
        String style = "theme=" + THEMES[themeIndex]
                + ";density=" + DENSITY[densityIndex]
                + ";font=" + FONTS[fontIndex]
                + ";toggleStyle=" + TOGGLE_STYLES[toggleStyleIndex];
        minecraft.keyboardHandler.setClipboard(style);
        MessageUtil.SUCCESS.sendMessage("Copied style preset.");
    }

    private void copyDebugInfo() {
        if (minecraft == null) return;
        String debug = "IQ Debug"
                + " | debugOverlay=" + debugOverlay
                + " | debugEvents=" + debugEvents;
        minecraft.keyboardHandler.setClipboard(debug);
        MessageUtil.INFO.sendMessage("Debug info copied.");
    }

    private void loadState() {
        themeIndex      = store.getOrDefault(K_THEME,         themeIndex);
        guiOpacity      = store.getOrDefault(K_GUI_OPACITY,   guiOpacity);
        uiScale         = store.getOrDefault(K_UI_SCALE,      uiScale);
        animationsEnabled = store.getOrDefault(K_ANIM_ENABLED, animationsEnabled);
        animationSpeed  = store.getOrDefault(K_ANIM_SPEED,    animationSpeed);
        outlineShadow   = store.getOrDefault(K_OUTLINE,       outlineShadow);
        autoReloadConfig = store.getOrDefault(K_AUTO_RELOAD,  autoReloadConfig);
        globalHudToggle = store.getOrDefault(K_HUD,           globalHudToggle);
        overlayOpacity  = store.getOrDefault(K_OVERLAY_OPACITY, overlayOpacity);
        snapToGrid      = store.getOrDefault(K_SNAP,          snapToGrid);
        showBoundingBoxes = store.getOrDefault(K_BOUNDS,      showBoundingBoxes);
        fontIndex       = store.getOrDefault(K_FONT,          fontIndex);
        layoutSpacing   = store.getOrDefault(K_SPACING,       layoutSpacing);
        densityIndex    = store.getOrDefault(K_DENSITY,       densityIndex);
        toggleStyleIndex = store.getOrDefault(K_TOGGLE_STYLE, toggleStyleIndex);
        showFeatureLogs = store.getOrDefault(K_DEBUG_LOGS,    showFeatureLogs);
        debugOverlay    = store.getOrDefault(K_DEBUG_OVERLAY, debugOverlay);
        debugEvents     = store.getOrDefault(K_DEBUG_EVENTS,  debugEvents);
        uiStatePersistenceEnabled = store.getOrDefault(K_UI_STATE_PERSIST, uiStatePersistenceEnabled);
    }

    private void saveState() {
        store.set(K_THEME,         themeIndex);
        store.set(K_GUI_OPACITY,   guiOpacity);
        store.set(K_UI_SCALE,      uiScale);
        store.set(K_ANIM_ENABLED,  animationsEnabled);
        store.set(K_ANIM_SPEED,    animationSpeed);
        store.set(K_OUTLINE,       outlineShadow);
        store.set(K_AUTO_RELOAD,   autoReloadConfig);
        store.set(K_HUD,           globalHudToggle);
        store.set(K_OVERLAY_OPACITY, overlayOpacity);
        store.set(K_SNAP,          snapToGrid);
        store.set(K_BOUNDS,        showBoundingBoxes);
        store.set(K_FONT,          fontIndex);
        store.set(K_SPACING,       layoutSpacing);
        store.set(K_DENSITY,       densityIndex);
        store.set(K_TOGGLE_STYLE,  toggleStyleIndex);
        store.set(K_DEBUG_LOGS,    showFeatureLogs);
        store.set(K_DEBUG_OVERLAY, debugOverlay);
        store.set(K_DEBUG_EVENTS,  debugEvents);
        store.set(K_UI_STATE_PERSIST, uiStatePersistenceEnabled);
    }

    private int currentAccentColor() {
        return themeAccentColor();
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private static int clampIndex(int idx, int size) {
        if (size <= 0) return 0;
        return Math.max(0, Math.min(size - 1, idx));
    }

    private static final ThemePalette[] THEME_PALETTES = new ThemePalette[] {
            new ThemePalette(
                    0x07060D, 0x080710, BG_HEADER,
                    BG_ROW, BG_ROW_HOV,
                    ACCENT, BORDER_DIM, BORDER_MID,
                    BG_CONTROL_TOP, BG_CONTROL_TOP_HOV,
                    BG_CONTROL_BOTTOM, BG_CONTROL_BOTTOM_HOV,
                    T_MAIN, T_MUTED, T_HUB_TITLE, T_HUB_SUBTITLE,
                    T_FEATURE_TITLE, T_ACTION_TEXT, T_ACTION_TEXT_HOV,
                    TOGGLE_ON, TOGGLE_OFF,
                    0xEE100B16, T_MUTED
            ),
            new ThemePalette(
                    0x0D0D0D, 0x121212, 0xD5181818,
                    0xA3181818, 0xC5222222,
                    0xFFEAEAEA, 0x40404040, 0x6A8E8E8E,
                    0xCC1A1A1A, 0xD6222222,
                    0xCC161616, 0xD61E1E1E,
                    0xFFEEEEEE, 0xFF888888, 0xFFDDDDDD, 0xFF888888,
                    0xFFFFFFFF, 0xFFCCCCCC, 0xFFFFFFFF,
                    0xFFEAEAEA, 0xC22A2A2A,
                    0xEE111111, 0xFFD8D8D8
            ),
            new ThemePalette(
                    0xF5F7FA, 0xEBEEF2, 0xD5EBEEF2,
                    0xA3FFFFFF, 0xC5F0F3F6,
                    0xFF1F2328, 0x3A8A78A8, 0x6A7A5FB0,
                    0xCCFFFFFF, 0xD6F0F3F6,
                    0xCCF0F3F6, 0xD6E5EAF0,
                    0xFF1A1530, 0xFF5A4878, 0xFF5030A0, 0xFF7060A8,
                    0xFF1A1A2E, 0xFF3A2860, 0xFF1F1050,
                    0xFF1F2328, 0xC2D0D7DE,
                    0xEEF2F5F9, 0xFF3E3260
            ),
            new ThemePalette(
                    0x07101A, 0x081723, 0xD50C1A29,
                    0xA3132132, 0xC51B2D43,
                    0xFF4A9EFF, 0x30304F72, 0x6A5A9FD9,
                    0xCC1B3049, 0xD6283E58,
                    0xCC162436, 0xD61E2E43,
                    0xFFCCE8FF, 0xFF7AAAC8, 0xFF80C8FF, 0xFF5490A8,
                    0xFFE0F5FF, 0xFFD0ECFF, 0xFFFFFFFF,
                    0xFF3A96E8, 0xC21B2440,
                    0xEE0B1A2A, 0xFFAED7FB
            ),
            new ThemePalette(
                    0x16080B, 0x190B10, 0xD51C0D14,
                    0xA328151C, 0xC5381E29,
                    0xFFFF5A78, 0x304F2A3A, 0x6AD97A93,
                    0xCC3A1C2A, 0xD64B2534,
                    0xCC22121C, 0xD62E1824,
                    0xFFFFD0D8, 0xFFBB8890, 0xFFFF9AAA, 0xFF996672,
                    0xFFFFEEF0, 0xFFFFDDE0, 0xFFFFFFFF,
                    0xFFE83A50, 0xC227151F,
                    0xEE1D0D14, 0xFFDDA3AE
            ),
            new ThemePalette(
                    0x06110B, 0x081710, 0xD50B1A13,
                    0xA313251B, 0xC51C3326,
                    0xFF4BE08A, 0x30315A42, 0x6A63C78A,
                    0xCC1D3A2A, 0xD6294A35,
                    0xCC13241B, 0xD61A3124,
                    0xFFBEFFD0, 0xFF70A880, 0xFF70FF9E, 0xFF4A9A6A,
                    0xFFEEFFEE, 0xFFCCFFE0, 0xFFFFFFFF,
                    0xFF35C06E, 0xC21A2620,
                    0xEE0C1A13, 0xFFA7D8B8
            )
    };

    private ThemePalette palette() {
        return themePalette(themeIndex);
    }

    private static ThemePalette themePalette(int idx) {
        return THEME_PALETTES[clampIndex(idx, THEME_PALETTES.length)];
    }

    private boolean isThemeVisible(int idx) {
        return !(HIDE_LIGHT_THEME && idx == LIGHT_THEME_INDEX);
    }

    private String visibleThemeLabel() {
        int idx = clampIndex(themeIndex, THEMES.length);
        if (isThemeVisible(idx)) return THEMES[idx];
        for (int i = 0; i < THEMES.length; i++) {
            if (isThemeVisible(i)) return THEMES[i];
        }
        return THEMES[0];
    }

    private void cycleThemeForward() {
        int start = clampIndex(themeIndex, THEMES.length);
        int idx = start;
        for (int i = 0; i < THEMES.length; i++) {
            idx = (idx + 1) % THEMES.length;
            if (isThemeVisible(idx)) {
                themeIndex = idx;
                return;
            }
        }
    }

    private String valuePercent(double v) {
        return (int) Math.round(v * 100.0) + "%";
    }

    private float animRate(float base) {
        if (!animationsEnabled) return 1f;
        return base * (0.55f + (float) animationSpeed * 1.35f);
    }

    private int themeToggleOn() {
        return palette().toggleOn();
    }

    private int themeToggleOff() {
        return palette().toggleOff();
    }

    private int themeToggleHandle(float tAnim) {
        if (themeIndex == 1) return lerpArgb(0xFF6A6A6A, 0xFF0D0D0D, tAnim);
        return 0xFFFFFFFF;
    }

    private int applyGuiOpacity(int baseColor) {
        int alpha = (int) Math.round(clamp(guiOpacity, 0.0, 1.0) * 255.0);
        return (baseColor & 0x00FFFFFF) | (alpha << 24);
    }

    private String themeHubTitle() {
        return "Configuration";
    }

    private String themeHubSubtitle() {
        return "General Settings";
    }

    private int themePanelColor() {
        return applyGuiOpacity(palette().panelBase());
    }

    private int themeSidebarColor() {
        return applyGuiOpacity(palette().sidebarBase());
    }

    private int themeHeaderColor() {
        return palette().header();
    }

    private int themeRowColor() {
        return palette().row();
    }

    private int themeRowHoverColor() {
        return palette().rowHover();
    }

    private int themeAccentColor() {
        return palette().accent();
    }

    private int themeOutlineHighlight() {
        return withAlpha(themeAccentColor(), 0x44);
    }

    private int themeBorderDim() {
        if (!outlineShadow) return 0x00000000;
        return palette().borderDim();
    }

    private int themeBorderMid() {
        if (!outlineShadow) return 0x00000000;
        return palette().borderMid();
    }

    private int themeBorderBright() {
        return outlineShadow ? withAlpha(themeAccentColor(), 0xB5) : 0x00000000;
    }

    private int themeButtonTop(boolean hover) {
        ThemePalette p = palette();
        return hover ? p.buttonTopHover() : p.buttonTop();
    }

    private int themeButtonBottom(boolean hover) {
        ThemePalette p = palette();
        return hover ? p.buttonBottomHover() : p.buttonBottom();
    }

    private int themeButtonBody(boolean hover) {
        return lerpArgb(themeButtonTop(hover), themeButtonBottom(hover), 0.5f);
    }

    private int tMain() {
        return palette().textMain();
    }

    private int tMuted() {
        return palette().textMuted();
    }

    private int tHubTitle() {
        return palette().hubTitle();
    }

    private int tHubSubtitle() {
        return palette().hubSubtitle();
    }

    private int tFeatureTitle() {
        return palette().featureTitle();
    }

    private int tActionText() {
        return palette().actionText();
    }

    private int tActionTextHov() {
        return palette().actionTextHover();
    }

    public static int getSharedThemeIndex() {
        return IQPersistentDataStore.get().getOrDefault(K_THEME, 0);
    }

    public static boolean isSharedAnimationsEnabled() {
        return IQPersistentDataStore.get().getOrDefault(K_ANIM_ENABLED, true);
    }

    public static double getSharedAnimationSpeed() {
        return IQPersistentDataStore.get().getOrDefault(K_ANIM_SPEED, 0.7);
    }

    public static boolean isSharedOutlineShadowEnabled() {
        return IQPersistentDataStore.get().getOrDefault(K_OUTLINE, true);
    }

    public static double getSharedGuiOpacity() {
        return IQPersistentDataStore.get().getOrDefault(K_GUI_OPACITY, 0.5);
    }

    public static boolean isSharedUiStatePersistenceEnabled() {
        return IQPersistentDataStore.get().getOrDefault(K_UI_STATE_PERSIST, true);
    }

    public static int getSharedAccentColor() {
        int t = IQPersistentDataStore.get().getOrDefault(K_THEME, 0);
        return themePalette(t).accent();
    }

    public static int getSharedToggleOnColor() {
        int t = IQPersistentDataStore.get().getOrDefault(K_THEME, 0);
        return themePalette(t).toggleOn();
    }

    public static int getSharedToggleOffColor() {
        int t = IQPersistentDataStore.get().getOrDefault(K_THEME, 0);
        return themePalette(t).toggleOff();
    }

    private void drawGlowBorder(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius, int accent) {
        if (!outlineShadow) {
            drawRoundedHollowRect(ctx, x, y, w, h, themeBorderBright(), radius);
            return;
        }
        int accRgb = themeAccentColor() & 0x00FFFFFF;
        int[] alphas = {0x05, 0x10, 0x1E, 0x30};
        for (int i = 0; i < alphas.length; i++) {
            int d = alphas.length - i;
            drawRoundedHollowRect(ctx, x - d, y - d, w + d * 2, h + d * 2, (alphas[i] << 24) | accRgb, radius + d);
        }
        drawRoundedHollowRect(ctx, x, y, w, h, themeBorderBright(), radius);
    }

    private int getHeaderActionButtonsStartX(int headerX, int headerW) {
        int totalBtnW = (HEADER_ACTION_BTN_SIZE * 2) + HEADER_ACTION_BTN_GAP;
        return headerX + headerW - totalBtnW - HEADER_ACTION_BTN_RIGHT_PAD;
    }

    private void drawHeaderActionButton(GuiGraphicsExtractor ctx, int x, int y, float hover) {
        int size = HEADER_ACTION_BTN_SIZE;
        int glow = lerpArgb(0x10000000, withAlpha(themeAccentColor(), 0x34), hover);
        int top = lerpArgb(themeButtonTop(false), themeButtonTop(true), hover);
        int bottom = lerpArgb(themeButtonBottom(false), themeButtonBottom(true), hover);
        int body = lerpArgb(themeButtonBody(false), themeButtonBody(true), hover);
        int border = lerpArgb(withAlpha(themeBorderMid(), 0x88), withAlpha(themeAccentColor(), 0xAA), hover);

        ctx.fill(x - 1, y - 1, x + size + 1, y + size + 1, glow);
        ctx.fill(x, y, x + size, y + size / 2, top);
        ctx.fill(x, y + size / 2, x + size, y + size, bottom);
        ctx.fill(x + 1, y + 1, x + size - 1, y + size - 1, body);
        ctx.fill(x + 1, y + 1, x + size - 1, y + 2, 0x42FFFFFF);
        ctx.fill(x + 1, y + size - 2, x + size - 1, y + size - 1, 0x3A000000);
        drawHollowRect(ctx, x, y, size, size, border);
    }

    private void renderActionControl(GuiGraphicsExtractor ctx, int x, int y, int w, boolean hov) {
        int top = themeButtonTop(hov);
        int bottom = themeButtonBottom(hov);
        int border = hov ? themeOutlineHighlight() : themeBorderMid();

        drawRoundedRect(ctx, x, y, w, CONTROL_H, top, 0);
        ctx.fill(x, y + CONTROL_H / 2, x + w, y + CONTROL_H, bottom);
        drawRoundedHollowRect(ctx, x, y, w, CONTROL_H, border, 0);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, 0x2AFFFFFF);
        ctx.fill(x + 1, y + CONTROL_H - 2, x + w - 1, y + CONTROL_H - 1, 0x22000000);
    }

    private int lerpArgb(int c0, int c1, float t) {
        return ScreenUiUtil.lerpArgb(c0, c1, t);
    }

    private void drawHollowRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        ScreenUiUtil.drawHollowRect(ctx, x, y, w, h, color);
    }

    private void drawRoundedRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color, int radius) {
        ScreenUiUtil.drawRoundedRect(ctx, x, y, w, h, color, radius);
    }

    private void drawRoundedGlow(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color, int radius, float glowWidth) {
        ScreenUiUtil.drawRoundedGlow(ctx, x, y, w, h, color, radius, glowWidth);
    }

    private void drawRoundedHollowRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color, int radius) {
        ScreenUiUtil.drawRoundedHollowRect(ctx, x, y, w, h, color, radius);
    }

    private int computeSidebarWidth(int panelW) {
        int maxSidebarW = Math.max(SIDEBAR_W, panelW - 340);
        if (minecraft == null) return SIDEBAR_W;

        int sectionTextW = 0;
        for (ConfigSection section : ConfigSection.values()) {
            sectionTextW = Math.max(sectionTextW, IqFonts.widthPx(section.title));
        }

        int desiredW = Math.max(SIDEBAR_W, 16 + sectionTextW + 16);
        return Math.min(maxSidebarW, desiredW);
    }

    private void renderTooltip(GuiGraphicsExtractor ctx, String text) {
        List<String> lines = wrapText(text, 260);
        int fh = IqFonts.lineHeight();
        int lh = fh + 2;
        int tw = lines.stream().mapToInt(s -> IqFonts.widthPx(s)).max().orElse(80);
        int bw = tw + 14;
        int bh = lines.size() * lh + 8;
        int tx = frameMouseX + 12;
        int ty = frameMouseY + 12;

        if (tx + bw > width - 4) tx = frameMouseX - bw - 8;
        if (ty + bh > height - 4) ty = frameMouseY - bh - 8;

        int radius = 2;
        drawRoundedRect(ctx, tx, ty, bw, bh, themeTooltipBgColor(), radius);
        drawRoundedHollowRect(ctx, tx, ty, bw, bh, themeTooltipBorderColor(), radius);
        ctx.fill(tx + 1, ty + 1, tx + 3, ty + bh - 1, withAlpha(themeAccentColor(), 0x8A));
        for (int i = 0; i < lines.size(); i++) {
            IqFonts.draw(ctx, lines.get(i),
                    tx + 7, ty + 4 + i * lh, themeTooltipTextColor());
        }
    }

    private int themeTooltipBgColor() {
        return palette().tooltipBg();
    }

    private int themeTooltipBorderColor() {
        return withAlpha(themeAccentColor(), outlineShadow ? 0xB8 : 0x88);
    }

    private int themeTooltipTextColor() {
        return palette().tooltipText();
    }

    private record ThemePalette(
            int panelBase,
            int sidebarBase,
            int header,
            int row,
            int rowHover,
            int accent,
            int borderDim,
            int borderMid,
            int buttonTop,
            int buttonTopHover,
            int buttonBottom,
            int buttonBottomHover,
            int textMain,
            int textMuted,
            int hubTitle,
            int hubSubtitle,
            int featureTitle,
            int actionText,
            int actionTextHover,
            int toggleOn,
            int toggleOff,
            int tooltipBg,
            int tooltipText
    ) {
    }

    private List<String> wrapText(String text, int maxWidth) {
        if (minecraft == null) return List.of(text);
        return ScreenUiUtil.wrapTextSimple(s -> Math.round(IqFonts.width(s)), text, maxWidth);
    }

    private boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return ScreenUiUtil.isIn(mx, my, x, y, w, h);
    }

    private static double clamp(double v, double lo, double hi) {
        return ScreenUiUtil.clamp(v, lo, hi);
    }

    private enum ConfigSection {
        GUI_VISUAL("GUI & Visual", "Visual behavior, color, motion, and panel style.", "Theme, colors and animation behavior."),
        FEATURES_CONTROL("Manage Features", "Bulk feature actions and config transfer.", "Bulk actions and config import/export."),
        SYSTEM_RELOAD("System & Reload", "Reload flows and system-level controls.", "Reload and system-level actions."),
        HUD_OVERLAY("HUD & Overlay", "HUD editor behavior and overlay controls.", "HUD visibility and overlay options. (W.I.P)"),
        DEBUG("Debug", "Debug overlays, event tracing, and diagnostics.", "Logs and diagnostic debugging options. (W.I.P)");

        private final String title;
        private final String tooltip;
        private final String headerSummary;

        ConfigSection(String title, String tooltip, String headerSummary) {
            this.title = title;
            this.tooltip = tooltip;
            this.headerSummary = headerSummary;
        }
    }

    private record ActionHit(int x, int y, int w, int h, Runnable action, String tooltip) {
        boolean contains(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private record SliderHit(int x, int y, int w, int h, double min, double max,
                             DoubleSupplier getter, DoubleConsumer setter, String tooltip) {
        boolean contains(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        void update(int mx) {
            double t = clamp((mx - x) / (double) w, 0.0, 1.0);
            setter.accept(min + t * (max - min));
        }
    }

    private record SidebarSlot(ConfigSection section, int y) {
    }

    @FunctionalInterface
    private interface BooleanValueSelector {
        boolean select(ConfigEntryModel entry, Field field);
    }
}
