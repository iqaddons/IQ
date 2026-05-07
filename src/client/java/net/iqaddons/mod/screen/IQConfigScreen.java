package net.iqaddons.mod.screen;

import net.iqaddons.mod.IQModClient;
import net.iqaddons.mod.config.Configuration;
import net.iqaddons.mod.config.categories.PhaseTwoConfig;
import net.iqaddons.mod.manager.IQPersistentDataStore;
import net.iqaddons.mod.screen.model.ConfigCategory;
import net.iqaddons.mod.screen.model.ConfigEntryModel;
import net.iqaddons.mod.screen.model.ConfigEntryModel.EntryType;
import net.iqaddons.mod.utils.ConfigReflectionUtil;
import net.iqaddons.mod.utils.data.DataKey;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * IQ Addons — Config Screen.
 *
 * <p>Design: soft noir palette (#100C14 + #D6A7FF + #F7A8D7), compact translucent layout,
 * sidebar with animated pill, search bar, collapsible ConfigObject sections.
 *
 * <p>Standard usage:
 * <pre>{@code
 * new IQConfigScreen(parent,
 *     Configuration.class, KuudraGeneralConfig.class, ...)
 * }</pre>
 *
 * <p>Open directly at a category (e.g. from HUD editor middle-click):
 * <pre>{@code
 * IQConfigScreen.atCategory(parent, "PhaseTwoConfig",
 *     Configuration.class, KuudraGeneralConfig.class, ...)
 * }</pre>
 */
public class IQConfigScreen extends Screen {

    // Fallback explicit logger to keep compile stable even if Lombok processing is skipped.
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IQConfigScreen.class);

    // ═══════════════════════════════════════════════════════════════════════
    //  Resources
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Resolves to assets/iq/textures/icon.png.
     * DrawContext.drawTexture requires the full texture path in the identifier.
     */
      private static final Identifier LOGO_TEXTURE = Identifier.of("iq", "textures/icon.png");
      private static final Identifier DISCORD_ICON_TEXTURE = Identifier.of("iq", "textures/social/discord.png");
      private static final Identifier MODRINTH_ICON_TEXTURE = Identifier.of("iq", "textures/social/modrinth.png");
      private static final Identifier PATREON_ICON_TEXTURE = Identifier.of("iq", "textures/social/patreon.png");
      private static final Identifier SETTINGS_ICON_TEXTURE = Identifier.of("iq", "textures/social/settings.png");
      private static final Identifier CLOSE_ICON_TEXTURE = Identifier.of("iq", "textures/social/close.png");
    /**
     * Actual pixel dimensions of icon.png
     */
    private static final int LOGO_TEX_SIZE = 160;
    /**
     * Rendered size in the sidebar
     */
    private static final int LOGO_SIZE = 24;

    // ═══════════════════════════════════════════════════════════════════════
    //  Palette (soft purple / pink + black, translucent)
    // ═══════════════════════════════════════════════════════════════════════

    private static final int BG_HEADER = 0xD50D0A16;
    private static final int BG_ENTRY = 0xA3120E1C;
    private static final int BG_ENTRY_HOV = 0xC51F1330;
    private static final int BG_SECTION = 0xB00F0B18;
    private static final int BG_CHILD = 0xA3151022;
    private static final int BG_TOGGLE_ON = 0xFFEC4BAF;
    private static final int BG_TOGGLE_OFF = 0xC2261B2D;
    private static final int BG_SLIDER_TRK = 0xC2261B2D;
    private static final int BG_CONTROL_TOP = 0xCC2A1A39;
    private static final int BG_CONTROL_TOP_HOV = 0xD63C234D;
    private static final int BG_CONTROL_BOTTOM = 0xCC1A1027;
    private static final int BG_CONTROL_BOTTOM_HOV = 0xD3261635;
    private static final int BG_SEARCH = 0xC0090712;
    private static final int BG_SEARCH_ACT = 0xD0130B1E;
    private static final int BG_OUTER = 0x7A000000;
    private static final int BG_COLOR_ED = 0xE00A0711;

    private static final int BORDER_DIM = 0x3034253D;
    private static final int BORDER_MID = 0x6A7A3D75;
    private static final int BORDER_BRIGHT = 0xB5E54BA8;

    private static final int T_MAIN = 0xFFF8F1FB;
    private static final int T_MUTED = 0xFFC9B4D5;
    private static final int T_ACCENT = 0xFFF7A8DC;
    private static final int T_ACTION_TEXT = 0xFFFFE7F4;
    private static final int T_ACTION_TEXT_HOV = 0xFFFFC2E7;
    // Feature rows keep a clear text hierarchy: stronger title, softer description.
    private static final int T_FEATURE_TITLE = 0xFFFFF3FC;
    private static final int T_FEATURE_DESC = 0xFFC1ADD0;

     private static final int ACCENT = 0xFFD650AB;
     private static final int SLIDER_HANDLE = 0xFFFBE9F6;
     private static final int SEP_LINE = 0x10FFFFFF;
     private static final int SEP_PILL_BG = 0x6B0F0D17;
     private static final int SEP_PILL_BORDER = 0x244E3C5E;
     private static final int SEP_PILL_GLOW = 0x03D57CB2;
     private static final int SEP_TEXT = 0xFF9C8CAE;
     private static final float SEP_TEXT_SCALE = 0.70f;
     private static final int TOGGLE_HANDLE  = 0xFFFAF1FB;
     private static final int TOGGLE_OUTLINE_IDLE = 0x664C335A;
     private static final int TOGGLE_OUTLINE_HOV = 0x8A7A4B90;
     private static final int GROUP_SEP_LINE = 0x26FFFFFF;

    // ═══════════════════════════════════════════════════════════════════════
    //  Layout
    // ═══════════════════════════════════════════════════════════════════════

    private static final float GUI_W_RATIO = 0.49f;
    private static final float GUI_H_RATIO = 0.47f;
    private static final int GUI_MIN_W = 575;
    private static final int GUI_MIN_H = 320;
    private static final float SCREEN_OPEN_SPEED = 7.5f;
    private static final float SCREEN_CLOSE_SPEED = 11.0f;
    private static final int SCREEN_TRANSITION_Y = 10;

    private static final int SIDEBAR_BASE_W = 118;
    private static final int CONTENT_MIN_W = 340;
    // Match sidebar top zone height so both columns align.
    private static final int HEADER_H = 42;
    private static final int LOGO_ZONE_H = 42;
    private static final int PADDING = 10;
    private static final int CHILD_INDENT = 9;
    private static final int SCROLL_W = 3;

    private static final int ROW_H_SLIM = 26;
    private static final int ROW_H_FULL_1 = 38;
    private static final int ROW_H_FULL_2 = 50;
    private static final int SEP_H = 14;
    private static final int SEP_TOP_GAP = 5;
    private static final int SEC_H = 32;

    private static final int TOGGLE_W = 32;
    private static final int TOGGLE_H = 16;
    private static final int SLIDER_W = 100;
    private static final int CONTROL_H = 15;
    private static final int CONTROL_RADIUS = 0;
    private static final int CONTROL_TEXT_PAD_X = 6;
    private static final int CONTROL_TEXT_GAP = 3;
    private static final int CONTROL_MIN_W = 34;
    private static final int CONTROL_MAX_W = 116;
    private static final int SELECT_W = 72;
    private static final int BUTTON_W = 51;
    private static final int TEXT_INPUT_W = 116;
    private static final int TEXT_INPUT_MAX_LEN = 64;
    private static final int SWATCH_W = 36;
    private static final int SWATCH_H = 16;
    private static final int SEARCH_H      = 18;
    private static final int MODE_PICKER_W = 94;

     private static final int LINK_BTN_SIZE = 14;
     private static final int LINK_BTN_GAP = 4;
     private static final int LINK_ICON_SIZE = 10;
     private static final int SOCIAL_ICON_TEX_SIZE = 64;
     private static final int SETTINGS_ICON_TEX_W = 24;
     private static final int SETTINGS_ICON_TEX_H = 24;
     private static final int CLOSE_ICON_TEX_SIZE = 24;
     private static final int HEADER_ACTION_BTN_SIZE = 19;
     private static final int HEADER_ACTION_BTN_GAP = 6;
     private static final int HEADER_ACTION_BTN_RIGHT_PAD = 8;

    private static final int CORNER_R_SMALL = 0;
    private static final int CORNER_R_MED = 0;
    private static final int CORNER_R_LARGE = 0;

    private static final int SIDEBAR_ROW_H = 24;
    private static final int SIDEBAR_ROW_GAP = 2;
    private static final int SIDEBAR_GROUP_GAP = 7;

    private static final ExternalLinkButton[] EXTERNAL_LINKS = new ExternalLinkButton[] {
            new ExternalLinkButton(DISCORD_ICON_TEXTURE, "Discord", "https://discord.com/invite/25aaMJMGMc",
                    "Discord: support, suggestions, and development updates."),
            new ExternalLinkButton(MODRINTH_ICON_TEXTURE, "Modrinth", "https://modrinth.com/mod/iq-addons",
                    "Modrinth: download the latest IQ versions."),
            new ExternalLinkButton(PATREON_ICON_TEXTURE, "Patreon", "https://patreon.com/IQAddons",
                    "Patreon: support IQ and get the exclusive version.")
    };

    private static final DataKey<Double> K_UI_SCALE = DataKey.of("globalcfg.uiScale", Double.class);
    private static final DataKey<String> K_LAST_CATEGORY_ID = DataKey.of("iqconfig.lastCategoryId", String.class);
    private static final DataKey<Double> K_LAST_SCROLL = DataKey.of("iqconfig.lastScroll", Double.class);
    private static final DataKey<String> K_LAST_SEARCH = DataKey.of("iqconfig.lastSearch", String.class);
    private static final DataKey<String> K_LAST_EXPANDED = DataKey.of("iqconfig.expandedSections", String.class);
    private static final DataKey<Long> K_LAST_CLOSE_TIME = DataKey.of("iqconfig.lastCloseTime", Long.class);

    /** Session timeout: UI state is discarded if the screen has been closed for more than this many ms. */
    private static final long SESSION_TIMEOUT_MS = 3L * 60L * 1_000L;

    // ═══════════════════════════════════════════════════════════════════════
    //  State
    // ═══════════════════════════════════════════════════════════════════════

    private final Screen parent;
    private final Class<?>[] configClasses;
    private final IQPersistentDataStore store = IQPersistentDataStore.get();

    private final List<ConfigCategory> categories = new ArrayList<>();
    private int selectedCategory = 0;

    private double scrollOffset = 0;
    private double maxScroll = 0;
    private boolean draggingScrollbar = false;
    private int scrollbarDragOffsetY = 0;
    private boolean restoreSavedScrollPending = false;
    private double savedScrollOffset = 0;

    // Search
    private final StringBuilder searchQuery = new StringBuilder();
    private boolean searchFocused = false;
    private int cursorTick = 0;

    // Slider drag
    private @Nullable ConfigEntryModel draggingSlider;
    private int sliderTrackX, sliderTrackW;

    // Color editor
    private @Nullable ConfigEntryModel editingColor;
    private @Nullable ConfigEntryModel editingTextEntry;
    private final StringBuilder editingTextBuffer = new StringBuilder();
    private double uiScale = 1.0;

    // Hit-test cache
    private final List<RenderedEntry> renderedEntries = new ArrayList<>();
    private final List<SidebarCategorySlot> sidebarSlots = new ArrayList<>();

    // Animated sidebar pill
    private float catPillY = -1f;

    // ── Animation state ──────────────────────────────────────────────────────
    /** handle position: 0.0 = off, 1.0 = on */
    private final Map<String, Float> toggleAnims  = new HashMap<>();
    /** expand progress: 0.0 = collapsed, 1.0 = expanded */
    private final Map<String, Float> sectionAnims = new HashMap<>();
    /** hover fade: 0.0 = idle, 1.0 = fully hovered */
    private final Map<String, Float> hoverAnims   = new HashMap<>();
    /** select slide: 1.0 = just cycled, 0.0 = settled */
    private final Map<String, Float> selectSlideAnims = new HashMap<>();
    /** select slide direction: +1 = forward (left), -1 = backward (right) */
    private final Map<String, Integer> selectSlideDirs = new HashMap<>();
    /** scroll momentum velocity (pixels/frame) */
    private double scrollVelocity = 0;
    /** content fade-in: 0.0 = invisible, 1.0 = fully visible */
    private float contentFadeAnim = 1f;
    /** tracks last rendered category to detect switches */
    private int lastRenderedCategory = -1;

     // Frame-local state
     private int frameMouseX = 0, frameMouseY = 0;
     private int cachedGx, cachedGy, cachedGw, cachedGh;
     private int cachedSidebarW = SIDEBAR_BASE_W;
     private @Nullable String pendingTooltip;
     private float screenTransition = 0f;
     private boolean closingScreen = false;
     private boolean closeHandled = false;
     private @Nullable Screen closeTarget;
     private long lastTransitionTimeMs = -1L;

    // Shared UI settings from Global Configuration Hub
    private int sharedThemeIndex = 0;
    private double sharedGuiOpacity = 0.5;
    private boolean sharedAnimationsEnabled = true;
    private double sharedAnimationSpeed = 0.7;
    private boolean sharedOutlineShadow = true;
    private boolean sharedBlurEnabled = true;
    private double sharedBlurIntensity = 0.45;

    // ═══════════════════════════════════════════════════════════════════════
    //  Static factory — open at a specific category (HUD editor hook)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Opens the config screen with the given category pre-selected.
     *
     * <p>Use this in {@code HudEditScreen} on middle-click:
     * <pre>{@code
     * // widget.getCategoryId() returns e.g. "PhaseTwoConfig"
     * mc.setScreen(IQConfigScreen.atCategory(this, widget.getCategoryId(),
     *     Configuration.class, KuudraGeneralConfig.class,
     *     PhaseOneConfig.class, PhaseTwoConfig.class,
     *     PhaseThreeConfig.class, PhaseFourConfig.class));
     * }</pre>
     *
     * @param parent        the screen to return to on close
     * @param categoryId    {@code Class.getSimpleName()} of the target config class
     * @param configClasses all config classes to register
     */
    @SuppressWarnings("unused")
    public static IQConfigScreen atCategory(@Nullable Screen parent, String categoryId,
                                            Class<?>... configClasses) {
        IQConfigScreen s = new IQConfigScreen(parent, configClasses);
        s.initialCategoryId = categoryId;
        return s;
    }

    private @Nullable String initialCategoryId = null;

    // ═══════════════════════════════════════════════════════════════════════
    //  Construction & lifecycle
    // ═══════════════════════════════════════════════════════════════════════

    public IQConfigScreen(@Nullable Screen parent, Class<?>... configClasses) {
        super(Text.literal("IQ Addons · Config"));
        this.parent        = parent;
        this.configClasses = configClasses;
    }

    @Override
    protected void init() {
        uiScale = store.getOrDefault(K_UI_SCALE, uiScale);
        categories.clear();
        for (Class<?> cls : configClasses) {
            ConfigCategory cat = ConfigReflectionUtil.buildCategory(cls);
            if (cat != null) categories.add(cat);
        }
        boolean persistUiState = IQGlobalConfigurationScreen.isSharedUiStatePersistenceEnabled();
        // Session expiry: discard saved state if the screen was last closed more than SESSION_TIMEOUT_MS ago.
        if (persistUiState) {
            long lastClose = store.getOrDefault(K_LAST_CLOSE_TIME, 0L);
            long elapsed = System.currentTimeMillis() - lastClose;
            if (elapsed > SESSION_TIMEOUT_MS) {
                persistUiState = false; // treat as a fresh open — go to General at top
            }
        }
        if (initialCategoryId != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).id().equals(initialCategoryId)) {
                    selectedCategory = i;
                    break;
                }
            }
        } else if (persistUiState) {
            String lastCategoryId = store.getOrDefault(K_LAST_CATEGORY_ID, "");
            if (!lastCategoryId.isBlank()) {
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).id().equals(lastCategoryId)) {
                        selectedCategory = i;
                        break;
                    }
                }
            }
        }
        scrollOffset = 0;
        savedScrollOffset = 0;
        restoreSavedScrollPending = false;
        searchQuery.setLength(0);
        if (persistUiState) {
            savedScrollOffset = Math.max(0, store.getOrDefault(K_LAST_SCROLL, 0.0));
            restoreSavedScrollPending = true;
            String savedSearch = store.getOrDefault(K_LAST_SEARCH, "");
            if (!savedSearch.isBlank()) {
                searchQuery.append(savedSearch);
            }
            // Restore expanded/collapsed state of all section headers
            String expandedEncoded = store.getOrDefault(K_LAST_EXPANDED, "");
            if (!expandedEncoded.isBlank()) {
                restoreExpandedSections(expandedEncoded);
            }
        }
        catPillY = -1f;
        // reset all animation maps so they don't hold stale entries
        toggleAnims.clear();
        sectionAnims.clear();
        hoverAnims.clear();
        selectSlideAnims.clear();
        selectSlideDirs.clear();
        scrollVelocity = 0;
        contentFadeAnim = 1f;
        lastRenderedCategory = -1;
        screenTransition = 0f;
        closingScreen = false;
        closeHandled = false;
        closeTarget = parent;
        lastTransitionTimeMs = -1L;
    }

    @Override
    public boolean shouldPause() { return false; }

    // ═══════════════════════════════════════════════════════════════════════
    //  Master render
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void render(@NotNull DrawContext ctx, int mouseX, int mouseY, float delta) {
        refreshSharedUiSettings();
        updateScreenTransition();
        frameMouseX = mouseX;
        frameMouseY = mouseY;
        cursorTick++;
        pendingTooltip = null;

        double scaled = clamp(uiScale, 0.75, 1.35);
        cachedGw = (int) (Math.max(GUI_MIN_W, width * GUI_W_RATIO) * scaled);
        cachedGh = (int) (Math.max(GUI_MIN_H, height * GUI_H_RATIO) * scaled);
        cachedGw = Math.min(cachedGw, width - 34);
        cachedGh = Math.min(cachedGh, height - 26);
        cachedGx = (width - cachedGw) / 2;
        int baseGy = (height - cachedGh) / 2;
        float renderTransition = closingScreen ? easeInQuad(screenTransition) : easeOutCubic(screenTransition);
        cachedGy = baseGy + Math.round((1f - renderTransition) * SCREEN_TRANSITION_Y);
        cachedSidebarW = computeSidebarWidth();

        // apply momentum scroll before rendering
        updateScrollAnimation();

        int overlayBaseAlpha = sharedBlurEnabled ? (int) (0x66 + (sharedBlurIntensity * 0x18)) : ((BG_OUTER >>> 24) & 0xFF);
        int bgAlpha = (int) (overlayBaseAlpha * renderTransition);
        int bgColor = (bgAlpha << 24) | (BG_OUTER & 0x00FFFFFF);
        ctx.fill(0, 0, width, height, bgColor);
        drawGlowBorder(ctx, cachedGx, cachedGy, cachedGw, cachedGh);
        drawRoundedRect(ctx, cachedGx, cachedGy, cachedGw, cachedGh, themePanelColor(), CORNER_R_LARGE);

        renderSidebar(ctx);
        renderHeader(ctx);

        int cx = cachedGx + cachedSidebarW;
        int cy = cachedGy + HEADER_H;
        renderContent(ctx, cx, cy, cachedGw - cachedSidebarW, cachedGh - HEADER_H);

        if (editingColor != null) renderColorEditor(ctx);
        if (pendingTooltip != null) renderTooltip(ctx, pendingTooltip);

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Soft glow border
    // ═══════════════════════════════════════════════════════════════════════

    private void drawGlowBorder(DrawContext ctx, int x, int y, int w, int h) {
        if (!sharedOutlineShadow) {
            drawRoundedHollowRect(ctx, x, y, w, h, themedBorderBright(), CORNER_R_LARGE);
            return;
        }
        int accRgb = themeAccentColor() & 0x00FFFFFF;
        int[] alphas = {0x05, 0x10, 0x1E, 0x30};
        for (int i = 0; i < alphas.length; i++) {
            int d = alphas.length - i;
            drawRoundedHollowRect(ctx, x - d, y - d, w + d * 2, h + d * 2,
                    (alphas[i] << 24) | accRgb, CORNER_R_LARGE);
        }
        drawRoundedHollowRect(ctx, x, y, w, h, themedBorderBright(), CORNER_R_LARGE);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Header — search bar + breadcrumb + close
    // ═══════════════════════════════════════════════════════════════════════

    private void renderHeader(DrawContext ctx) {
        int x = cachedGx + cachedSidebarW, y = cachedGy, w = cachedGw - cachedSidebarW;

        drawRoundedRect(ctx, x, y, w, HEADER_H, themeHeaderColor(), CORNER_R_MED);

        // Soft gradient line at bottom of header using themed accent
        int acc0 = themeAccentColor();
        int r0 = (acc0 >> 16) & 0xFF, g0 = (acc0 >> 8) & 0xFF, b0 = acc0 & 0xFF;
        // shift slightly toward a secondary accent shade
        int r1 = Math.min(255, r0 + 30), g1 = Math.max(0, g0 - 10), b1 = Math.min(255, b0 + 20);
        for (int i = 0; i < w; i++) {
            double t = (double) i / w;
            int a = (int) ((1.0 - t * 0.45) * 170);
            int r = (int) (r0 + (r1 - r0) * t);
            int g = (int) (g0 + (g1 - g0) * t);
            int b = (int) (b0 + (b1 - b0) * t);
            ctx.fill(x + i, y + HEADER_H - 1, x + i + 1, y + HEADER_H,
                    (a << 24) | (r << 16) | (g << 8) | b);
        }

        // ── Search bar (centred) ───────────────────────────────────────────
        HeaderSearchBox searchBox = getHeaderSearchBox(x, y, w);
        int sbW = searchBox.w();
        int sbX = searchBox.x();
        int sbY = searchBox.y();

        boolean sbHov = isIn(frameMouseX, frameMouseY, sbX, sbY, sbW, SEARCH_H);
        drawRoundedRect(ctx, sbX, sbY, sbW, SEARCH_H,
                searchFocused ? themeSearchActiveColor() : themeSearchColor(), CORNER_R_SMALL);
        drawRoundedHollowRect(ctx, sbX, sbY, sbW, SEARCH_H,
                searchFocused ? BORDER_BRIGHT : (sbHov ? BORDER_MID : BORDER_DIM));

        // Magnifier icon
        int iconY = sbY + (SEARCH_H - client.textRenderer.fontHeight) / 2;
        ctx.drawTextWithShadow(client.textRenderer, Text.literal("⌕"), sbX + 5, iconY, 0xA0E64BA8);
        ctx.drawTextWithShadow(client.textRenderer, Text.literal("⌕"), sbX + 4, iconY, 0xE8FFFFFF);

        // Query + blinking cursor
        String q = searchQuery.toString();
        boolean blink = searchFocused && (cursorTick / 10) % 2 == 0;
        String display;
        if (q.isEmpty() && !searchFocused) {
            display = "§8Search…";
        } else {
            String trimmed = client.textRenderer.trimToWidth(q, sbW - 32);
            display = "§f" + trimmed + (blink ? "§7|" : "");
        }
        ctx.drawTextWithShadow(client.textRenderer, Text.literal(display),
                sbX + 15, sbY + (SEARCH_H - client.textRenderer.fontHeight) / 2, T_MAIN);

        // Clear ×
        if (!q.isEmpty()) {
            boolean clrHov = isIn(frameMouseX, frameMouseY, sbX + sbW - 15, sbY, 12, SEARCH_H);
            ctx.drawTextWithShadow(client.textRenderer,
                    Text.literal(clrHov ? "§c✕" : "§8✕"),
                    sbX + sbW - 14, sbY + (SEARCH_H - client.textRenderer.fontHeight) / 2, T_MUTED);
        }

         // ── Settings & Close buttons (right side) ───────────────────────────
         int headerBtnY = y + (HEADER_H - HEADER_ACTION_BTN_SIZE) / 2;
         int settingsBtnX = getHeaderActionButtonsStartX(x, w);
         int closeBtnX = settingsBtnX + HEADER_ACTION_BTN_SIZE + HEADER_ACTION_BTN_GAP;
         int iconSize = 12;

         boolean settingsHov = editingColor == null
                 && isIn(frameMouseX, frameMouseY, settingsBtnX, headerBtnY, HEADER_ACTION_BTN_SIZE, HEADER_ACTION_BTN_SIZE);
         boolean closeHov = editingColor == null
                 && isIn(frameMouseX, frameMouseY, closeBtnX, headerBtnY, HEADER_ACTION_BTN_SIZE, HEADER_ACTION_BTN_SIZE);

         float settingsAnim = hoverAnims.getOrDefault("hdr#settings", 0f);
         settingsAnim += ((settingsHov ? 1f : 0f) - settingsAnim) * 0.30f;
         hoverAnims.put("hdr#settings", settingsAnim);

         float closeAnim = hoverAnims.getOrDefault("hdr#close", 0f);
         closeAnim += ((closeHov ? 1f : 0f) - closeAnim) * 0.30f;
         hoverAnims.put("hdr#close", closeAnim);

         drawHeaderActionButton(ctx, settingsBtnX, headerBtnY, settingsAnim);
         drawHeaderActionButton(ctx, closeBtnX, headerBtnY, closeAnim);

         int settingsIconX = settingsBtnX + (HEADER_ACTION_BTN_SIZE - iconSize) / 2;
         int closeIconX = closeBtnX + (HEADER_ACTION_BTN_SIZE - iconSize) / 2;
         int iconY2 = headerBtnY + (HEADER_ACTION_BTN_SIZE - iconSize) / 2;

         ctx.drawTexture(RenderPipelines.GUI_TEXTURED, SETTINGS_ICON_TEXTURE,
                 settingsIconX, iconY2, 0f, 0f,
                 iconSize, iconSize,
                 SETTINGS_ICON_TEX_W, SETTINGS_ICON_TEX_H,
                 SETTINGS_ICON_TEX_W, SETTINGS_ICON_TEX_H);
         ctx.drawTexture(RenderPipelines.GUI_TEXTURED, CLOSE_ICON_TEXTURE,
                 closeIconX, iconY2, 0f, 0f,
                 iconSize, iconSize,
                 CLOSE_ICON_TEX_SIZE, CLOSE_ICON_TEX_SIZE,
                 CLOSE_ICON_TEX_SIZE, CLOSE_ICON_TEX_SIZE);

         if (settingsHov) pendingTooltip = "Open Configuration Hub";
         if (closeHov) pendingTooltip = "Close config";
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Sidebar
    // ═══════════════════════════════════════════════════════════════════════

    private void renderSidebar(DrawContext ctx) {
        int gx = cachedGx, gy = cachedGy, gh = cachedGh, sidebarW = cachedSidebarW;
        boolean searchActive = isSearchActive();

        drawRoundedRect(ctx, gx, gy, sidebarW, gh, themeSidebarColor(), CORNER_R_MED);
        ctx.fill(gx + sidebarW - 1, gy, gx + sidebarW, gy + gh, 0x22FFFFFF);

        // ── Logo zone ──────────────────────────────────────────────────────
        drawRoundedRect(ctx, gx, gy, sidebarW, LOGO_ZONE_H, themeHeaderColor(), CORNER_R_MED);
        ctx.fill(gx, gy + LOGO_ZONE_H - 1, gx + sidebarW, gy + LOGO_ZONE_H, 0x22FFFFFF);

        int lx = gx + 10, ly = gy + (LOGO_ZONE_H - LOGO_SIZE) / 2;

        // Logo: Identifier.of("iq", "textures/icon.png") → assets/iq/textures/icon.png
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO_TEXTURE,
                lx, ly, 0f, 0f,
                LOGO_SIZE, LOGO_SIZE,
                LOGO_TEX_SIZE, LOGO_TEX_SIZE,
                LOGO_TEX_SIZE, LOGO_TEX_SIZE);

        ctx.drawTextWithShadow(client.textRenderer, Text.literal("§d§lIQ"),
                lx + LOGO_SIZE + 8, ly + 4, T_ACCENT);
        ctx.drawTextWithShadow(client.textRenderer, Text.literal("§8Config"),
                lx + LOGO_SIZE + 8, ly + 4 + client.textRenderer.fontHeight + 3, 0x44FFFFFF);

        renderExternalLinkButtons(ctx, gx, gy, sidebarW);

        // ── Animated category pill ─────────────────────────────────────────
        int listTop = gy + LOGO_ZONE_H + 6;
        rebuildSidebarSlots(listTop);

        if (!searchActive) {
            for (SidebarCategorySlot slot : sidebarSlots) {
                if (slot.categoryIndex() != selectedCategory) continue;
                float catPillTarget = slot.y() + 2f;
                if (catPillY < 0) catPillY = catPillTarget;
                catPillY += (catPillTarget - catPillY) * 0.18f;

                int pillY = (int) catPillY;
                int pillH = SIDEBAR_ROW_H - 4;
                int acc = themeAccentColor();
                drawRoundedRect(ctx, gx + 7, pillY - 1, sidebarW - 14, pillH + 2, withAlpha(acc, 0x1E), CORNER_R_MED);
                drawRoundedRect(ctx, gx + 8, pillY, sidebarW - 16, pillH, withAlpha(acc, 0x4A), CORNER_R_SMALL);
                drawRoundedRect(ctx, gx + 8, pillY, 3, pillH, acc, CORNER_R_SMALL);
                break;
            }
        }

        // ── Category list ──────────────────────────────────────────────────
        int lastGroup = -1;
        for (SidebarCategorySlot slot : sidebarSlots) {
            ConfigCategory category = categories.get(slot.categoryIndex());
            int group = getSidebarGroup(category.id());
            if (lastGroup != -1 && group != lastGroup) {
                int sepY = slot.y() - ((SIDEBAR_GROUP_GAP + SIDEBAR_ROW_GAP) / 2);
                ctx.fill(gx + 14, sepY, gx + sidebarW - 14, sepY + 1, GROUP_SEP_LINE);
            }

            boolean active = !searchActive && slot.categoryIndex() == selectedCategory;
            boolean hovered = !active && editingColor == null
                    && isIn(frameMouseX, frameMouseY, gx + 8, slot.y() + 2, sidebarW - 16, SIDEBAR_ROW_H - 4);

            if (hovered) {
                drawRoundedRect(ctx, gx + 8, slot.y() + 2, sidebarW - 16, SIDEBAR_ROW_H - 4, 0x149A6BB8, CORNER_R_SMALL);
            }

            String name = getSidebarDisplayName(category);
            if (client.textRenderer.getWidth(name) > sidebarW - 28)
                name = client.textRenderer.trimToWidth(name, sidebarW - 36) + "…";

            int col = active ? themeSidebarTextActive() : (hovered ? themeSidebarTextHover() : themeSidebarTextMuted());
            ctx.drawTextWithShadow(client.textRenderer, Text.literal(name),
                    gx + 16, slot.y() + (SIDEBAR_ROW_H - client.textRenderer.fontHeight) / 2, col);
            lastGroup = group;
        }

        ctx.drawTextWithShadow(client.textRenderer, Text.literal("§8MODRINTH VERSION v1.0.2"),
                gx + 10, gy + gh - client.textRenderer.fontHeight - 8, 0x22FFFFFF);
    }

     private void renderExternalLinkButtons(DrawContext ctx, int gx, int gy, int sidebarW) {
        int bx = getExternalLinksStartX(gx, sidebarW);
        int by = getExternalLinksY(gy);

        for (ExternalLinkButton link : EXTERNAL_LINKS) {
            boolean hov = editingColor == null && isIn(frameMouseX, frameMouseY, bx, by, LINK_BTN_SIZE, LINK_BTN_SIZE);
            int btnBg = hov ? themeControlTopHover() : themeControlTop();
            int btnBorder = hov ? withAlpha(themeAccentColor(), 0x88) : themedBorderDim();
            drawRoundedRect(ctx, bx, by, LINK_BTN_SIZE, LINK_BTN_SIZE, btnBg, CORNER_R_SMALL);
            drawRoundedHollowRect(ctx, bx, by, LINK_BTN_SIZE, LINK_BTN_SIZE, btnBorder);

            int ix = bx + (LINK_BTN_SIZE - LINK_ICON_SIZE) / 2;
            int iy = by + (LINK_BTN_SIZE - LINK_ICON_SIZE) / 2;
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, link.iconTexture(),
                    ix, iy, 0f, 0f,
                    LINK_ICON_SIZE, LINK_ICON_SIZE,
                    SOCIAL_ICON_TEX_SIZE, SOCIAL_ICON_TEX_SIZE,
                    SOCIAL_ICON_TEX_SIZE, SOCIAL_ICON_TEX_SIZE);
            if (hov) pendingTooltip = link.tooltip();
            bx += LINK_BTN_SIZE + LINK_BTN_GAP;
        }
    }

    private int getExternalLinksStartX(int gx, int sidebarW) {
        int totalW = EXTERNAL_LINKS.length * LINK_BTN_SIZE + (EXTERNAL_LINKS.length - 1) * LINK_BTN_GAP;
        return gx + sidebarW - totalW - 6;
    }

    private int getExternalLinksY(int gy) {
        int iqLabelY = gy + (LOGO_ZONE_H - LOGO_SIZE) / 2 + 4;
        return iqLabelY + 1;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Content
    // ═══════════════════════════════════════════════════════════════════════

    private void renderContent(DrawContext ctx, int cx, int cy, int cw, int ch) {
        if (categories.isEmpty()) return;
        ctx.fill(cx, cy, cx + cw, cy + ch, themePanelColor());

        // ── Detect category switch (e.g. triggered via atCategory factory) ──
        if (lastRenderedCategory != selectedCategory) {
            if (lastRenderedCategory != -1) contentFadeAnim = 0f;
            lastRenderedCategory = selectedCategory;
        }

        // Advance fade-in animation
        contentFadeAnim += (1f - contentFadeAnim) * 0.20f;
        if (contentFadeAnim > 0.997f) contentFadeAnim = 1f;

        ctx.enableScissor(cx, cy, cx + cw, cy + ch);

        renderedEntries.clear();
        List<ConfigEntryModel> entries = getDisplayEntries();
        int y = cy + 10 - (int) scrollOffset;
        int totalH = renderEntries(ctx, entries, cx, y, cw, ch, cy, 0);

        maxScroll = Math.max(0, totalH + 10 - ch);
        if (restoreSavedScrollPending) {
            scrollOffset = clamp(savedScrollOffset, 0, maxScroll);
            restoreSavedScrollPending = false;
        } else {
            scrollOffset = clamp(scrollOffset, 0, maxScroll);
        }
        ctx.disableScissor();
        if (maxScroll > 0) renderScrollbar(ctx, cx + cw - SCROLL_W - 2, cy, ch);

        // ── Fade overlay (dark → transparent as contentFadeAnim goes 0→1) ──
        if (contentFadeAnim < 1f) {
            int overlayAlpha = (int) ((1f - contentFadeAnim) * 0xCC);
            if (overlayAlpha > 0)
                ctx.fill(cx, cy, cx + cw, cy + ch, (overlayAlpha << 24) | (themePanelColor() & 0x00FFFFFF));
        }

        if (entries.isEmpty() && !searchQuery.isEmpty()) {
            ctx.drawCenteredTextWithShadow(client.textRenderer,
                    Text.literal("§8No results for §7\"" + searchQuery + "\""),
                    cx + cw / 2, cy + ch / 2, T_MUTED);
        }
    }

    private List<ConfigEntryModel> getDisplayEntries() {
        String q = searchQuery.toString().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            if (selectedCategory >= categories.size()) return List.of();
            return categories.get(selectedCategory).entries();
        }
        List<ConfigEntryModel> results = new ArrayList<>();
        categories.stream()
                .flatMap(cat -> cat.entries().stream())
                .forEach(e -> collectSearchResults(e, q, results));
        return results;
    }

    /**
     * Recursively collects entries matching the search query.
     * If a SECTION_HEADER label matches, it is added as-is (expandable with all children).
     * If a SECTION_HEADER label does not match, its children are searched individually.
     */
    private void collectSearchResults(ConfigEntryModel e, String q, List<ConfigEntryModel> results) {
        if (e.getType() == EntryType.SECTION_HEADER) {
            if (e.getLabel().toLowerCase(Locale.ROOT).contains(q)) {
                // Section header matches: include it as-is (expandable)
                results.add(e);
            } else {
                // Header doesn't match: search inside children
                if (e.getChildren() != null) {
                    for (ConfigEntryModel child : e.getChildren()) {
                        collectSearchResults(child, q, results);
                    }
                }
            }
        } else if (isSearchableType(e.getType()) && e.getLabel().toLowerCase(Locale.ROOT).contains(q)) {
            results.add(e);
        }
    }

    private boolean isSearchActive() {
        return !searchQuery.toString().trim().isEmpty();
    }


    private boolean isSearchableType(EntryType t) {
        return t != EntryType.SEPARATOR && t != EntryType.SECTION_HEADER && t != EntryType.UNSUPPORTED;
    }

    private int renderEntries(DrawContext ctx, List<ConfigEntryModel> entries,
                              int cx, int startY, int cw, int ch, int clipTop, int indent) {
        int y = startY, used = 0;
        int ew = cw - 14 - indent - SCROLL_W - 4;

        for (ConfigEntryModel entry : entries) {
            int rh = rowHeight(entry);
            if (rh == 0) continue;

            // Add subtle vertical breathing room before separators, except when first.
            if (entry.getType() == EntryType.SEPARATOR && used > 0) {
                y += SEP_TOP_GAP;
                used += SEP_TOP_GAP;
            }

            boolean visible = y + rh > clipTop && y < clipTop + ch;
            int ex = cx + 7 + indent;
            boolean hovered = editingColor == null
                    && entry.getType() != EntryType.SEPARATOR
                    && isIn(frameMouseX, frameMouseY, ex, Math.max(y, clipTop), ew, rh);

            if (visible) renderRow(ctx, entry, ex, y, ew, rh, hovered, indent > 0);

            if (entry.getType() != EntryType.SEPARATOR)
                renderedEntries.add(new RenderedEntry(entry, ex, y, ew, rh));

            int gap = (entry.getType() == EntryType.SEPARATOR) ? 0 : 4;
            y += rh + gap;
            used += rh + gap;

            // ── Animated section expand / collapse ────────────────────────
            if (entry.getType() == EntryType.SECTION_HEADER
                    && entry.getChildren() != null && !entry.getChildren().isEmpty()) {

                String sKey = sectionKey(entry);
                float sTarget = entry.isExpanded() ? 1f : 0f;
                float sAnim   = sectionAnims.getOrDefault(sKey, sTarget);
                sAnim += (sTarget - sAnim) * 0.18f;
                if (Math.abs(sAnim - sTarget) < 0.004f) sAnim = sTarget;
                sectionAnims.put(sKey, sAnim);

                if (sAnim > 0.003f) {
                    int fullChildH  = measureEntries(entry.getChildren());
                    int animChildH  = Math.max(1, (int) (sAnim * fullChildH));

                    int clipY1 = y;
                    int clipY2 = Math.min(y + animChildH, clipTop + ch);

                    if (clipY2 > clipY1) {
                        ctx.enableScissor(cx, clipY1, cx + cw, clipY2);
                        renderEntries(ctx, entry.getChildren(), cx, y, cw, animChildH, y, indent + CHILD_INDENT);
                        ctx.disableScissor();
                    }
                    y    += animChildH;
                    used += animChildH;
                }
            }
        }
        return used;
    }

    private void renderRow(DrawContext ctx, ConfigEntryModel e,
                           int x, int y, int w, int h, boolean hov, boolean isChild) {
        switch (e.getType()) {
            case SEPARATOR -> renderSeparator(ctx, x, y, w, h, e.getSeparatorLabel());
            case SECTION_HEADER -> renderSectionHeader(ctx, e, x, y, w, h, hov);
            case UNSUPPORTED -> {
            }
            default -> renderEntry(ctx, e, x, y, w, h, hov, isChild);
        }
    }

    private void renderSeparator(DrawContext ctx, int x, int y, int w, int h, @Nullable String lbl) {
        int centerY = y + (h - 1) / 2;
        int sepLine = themeSepLine();
        if (lbl == null || lbl.isBlank()) {
            ctx.fill(x, centerY, x + w, centerY + 1, sepLine);
            return;
        }

        String up = lbl
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
        if (up.isEmpty()) {
            ctx.fill(x, centerY, x + w, centerY + 1, sepLine);
            return;
        }

        Text separatorText = Text.literal(up).copy().styled(style -> style.withBold(true));
        int textPaddingX = 5;
        int textPaddingY = 2;
        int minPillW = 24;
        int maxPillW = Math.max(minPillW, w - 28);

        int textWRaw = client.textRenderer.getWidth(separatorText);
        int textW = Math.max(1, (int) Math.ceil(textWRaw * SEP_TEXT_SCALE));
        int pillW = Math.min(maxPillW, Math.max(minPillW, textW + (textPaddingX * 2)));
        if (textW + (textPaddingX * 2) > maxPillW) {
            int maxTextW = Math.max(10, (int) Math.floor((maxPillW - (textPaddingX * 2)) / SEP_TEXT_SCALE));
            String trimmed = client.textRenderer.trimToWidth(up, maxTextW);
            separatorText = Text.literal(trimmed).copy().styled(style -> style.withBold(true));
            textWRaw = client.textRenderer.getWidth(separatorText);
            textW = Math.max(1, (int) Math.ceil(textWRaw * SEP_TEXT_SCALE));
            pillW = Math.min(maxPillW, Math.max(minPillW, textW + (textPaddingX * 2)));
        }

        int scaledFontH = Math.max(1, (int) Math.ceil(client.textRenderer.fontHeight * SEP_TEXT_SCALE));
        int pillH = Math.max(10, scaledFontH + (textPaddingY * 2));
        int pillX = x + (w - pillW) / 2;
        int pillY = centerY - (pillH / 2);

        int leftEnd = pillX - 6;
        int rightStart = pillX + pillW + 6;

        int lineColor = themeSepLine();
        int lineBright = themeSepLineBright();
        if (leftEnd > x + 2) {
            drawSeparatorGradientLine(ctx, x + 2, leftEnd, centerY, false, lineColor, lineBright);
        }
        if (rightStart < x + w - 2) {
            drawSeparatorGradientLine(ctx, rightStart, x + w - 2, centerY, true, lineColor, lineBright);
        }

        drawRoundedRect(ctx, pillX - 1, pillY - 1, pillW + 2, pillH + 2, themeSepPillGlow(), CORNER_R_SMALL);
        drawRoundedRect(ctx, pillX, pillY, pillW, pillH, themeSepPillBg(), CORNER_R_SMALL);
        drawRoundedHollowRect(ctx, pillX, pillY, pillW, pillH, themeSepPillBorder(), CORNER_R_SMALL);

        int tx = pillX + (pillW - textW) / 2;
        int ty = pillY + (pillH - scaledFontH) / 2;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(tx, ty);
        ctx.getMatrices().scale(SEP_TEXT_SCALE, SEP_TEXT_SCALE);
        ctx.drawTextWithShadow(client.textRenderer, separatorText, 0, 0, themeSepText());
        ctx.getMatrices().popMatrix();
    }

    private void renderSectionHeader(DrawContext ctx, ConfigEntryModel entry,
                                     int x, int y, int w, int h, boolean hov) {
        boolean exp = entry.isExpanded();

        // ── hover fade ──────────────────────────────────────────────────────
        String hKey = "secHov#" + sectionKey(entry);
        float hAnim = hoverAnims.getOrDefault(hKey, 0f);
        hAnim += ((hov ? 1f : 0f) - hAnim) * 0.28f;
        hoverAnims.put(hKey, hAnim);

        drawRoundedRect(ctx, x, y, w, h, lerpArgb(themeSectionColor(), themeEntryHoverColor(), hAnim), CORNER_R_SMALL);
        drawRoundedRect(ctx, x, y, 2, h, themeAccentColor(), CORNER_R_SMALL);
        if (exp) drawRoundedRect(ctx, x, y, 2, 3, withAlpha(themeAccentColor(), 0xFF), CORNER_R_SMALL);

        ctx.drawTextWithShadow(client.textRenderer, Text.literal(entry.getLabel()),
                x + 10, y + (h - client.textRenderer.fontHeight) / 2, themeTextMain());

        int count = entry.getChildren() != null ? countControls(entry.getChildren()) : 0;
        String hint = count + (exp ? " ▾" : " ▸");
        int hintW = client.textRenderer.getWidth(hint);
        ctx.drawTextWithShadow(client.textRenderer, Text.literal(hint),
                x + w - hintW - PADDING,
                y + (h - client.textRenderer.fontHeight) / 2, themeTextMuted());

        drawRoundedHollowRect(ctx, x, y, w, h, lerpArgb(themedBorderDim(), themedBorderBright(), hAnim * (exp ? 1f : 0.4f)));
    }

    private void renderEntry(DrawContext ctx, ConfigEntryModel entry,
                             int x, int y, int w, int h, boolean hov, boolean isChild) {
        // ── hover fade ──────────────────────────────────────────────────────
        String hKey = entryKey(entry);
        float hAnim = hoverAnims.getOrDefault(hKey, 0f);
        hAnim += ((hov ? 1f : 0f) - hAnim) * 0.28f;
        hoverAnims.put(hKey, hAnim);

        drawRoundedRect(ctx, x, y, w, h, isChild ? themeChildColor() : lerpArgb(themeEntryColor(), themeEntryHoverColor(), hAnim), CORNER_R_SMALL);
        drawRoundedHollowRect(ctx, x, y, w, h, lerpArgb(themedBorderDim(), themedBorderHighlight(), hAnim));
        int accentAlpha = (int) (0x38 * hAnim);
        if (accentAlpha > 0) ctx.fill(x, y, x + 2, y + h, withAlpha(themeAccentColor(), accentAlpha));

        int ctrlW = controlWidth(entry);
        int descAvailW = w - PADDING - ctrlW - CONTROL_TEXT_GAP - PADDING;
        boolean hasDesc = entry.getDescription() != null && !entry.getDescription().isBlank();

        if (hasDesc) {
            int labelY = y + 7;
            ctx.drawTextWithShadow(client.textRenderer, Text.literal(entry.getLabel()),
                    x + PADDING, labelY, themeFeatureTitleColor());

            List<String> lines = wrapText(entry.getDescription(), Math.max(60, descAvailW));
            int descY = labelY + client.textRenderer.fontHeight + 3;
            for (int li = 0; li < Math.min(2, lines.size()); li++) {
                String ln = lines.get(li);
                if (li == 1 && lines.size() > 2)
                    ln = client.textRenderer.trimToWidth(ln, descAvailW - 10) + "…";
                ctx.drawTextWithShadow(client.textRenderer, Text.literal(ln),
                        x + PADDING, descY + li * (client.textRenderer.fontHeight + 1), themeFeatureDescriptionColor());
            }
            if (hov && lines.size() > 2) pendingTooltip = entry.getDescription();
        } else {
            ctx.drawTextWithShadow(client.textRenderer, Text.literal(entry.getLabel()),
                    x + PADDING, y + (h - client.textRenderer.fontHeight) / 2, themeFeatureTitleColor());
        }

        int re = x + w - PADDING, cy = y + h / 2;
        switch (entry.getType()) {
            case BOOLEAN -> {
                if (isBuildOverlayStyleEntry(entry)) {
                    renderBuildOverlayModeControl(ctx, re, cy, hov);
                } else {
                    renderToggle(ctx, entry, re, cy, hov);
                }
            }
            case INT_SLIDER, FLOAT_SLIDER, DOUBLE_SLIDER -> renderSlider(ctx, entry, re, cy);
            case SELECT -> renderSelect(ctx, entry, re, cy, hov);
            case STRING -> renderStringInput(ctx, entry, re, cy, hov);
            case COLOR -> renderColorSwatch(ctx, entry, re, cy, hov);
            case BUTTON -> renderButton(ctx, entry, re, cy, hov);
        }
    }

    private void renderBuildOverlayModeControl(DrawContext ctx, int rx, int cy, boolean hov) {
        int w = MODE_PICKER_W;
        int h = CONTROL_H;
        int x = rx - w;
        int y = cy - h / 2;

        // Match the same square style used by the rest of IQ action controls.
        renderActionControl(ctx, x, y, w, hov);

        int innerX = x + 1;
        int innerY = y + 1;
        int innerW = w - 2;
        int innerH = h - 2;
        int segW = innerW / 2;
        int splitX = innerX + segW;

        boolean simple = PhaseTwoConfig.simpleBuildProgressOverlay;
        boolean normal = !simple;

        int activeBg = hov ? 0xB41A344B : 0xA1162C40;
        int idleBg = hov ? 0x5A121A25 : 0x4A0E141D;
        int activeText = hov ? themeActionTextHoverColor() : themeActionTextColor();
        int idleText = themeTextMuted();

        ctx.fill(innerX, innerY, innerX + segW - 1, innerY + innerH, simple ? activeBg : idleBg);
        ctx.fill(splitX + 1, innerY, innerX + innerW, innerY + innerH, normal ? activeBg : idleBg);
        ctx.fill(splitX, innerY + 2, splitX + 1, innerY + innerH - 2, 0x2E8EA3BF);

        ctx.drawCenteredTextWithShadow(client.textRenderer, Text.literal("Simple"),
                innerX + (segW / 2), cy - client.textRenderer.fontHeight / 2, simple ? activeText : idleText);
        ctx.drawCenteredTextWithShadow(client.textRenderer, Text.literal("Normal"),
                splitX + ((innerW - segW) / 2), cy - client.textRenderer.fontHeight / 2, normal ? activeText : idleText);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Controls
    // ═══════════════════════════════════════════════════════════════════════

    private void renderToggle(DrawContext ctx, ConfigEntryModel e, int rx, int cy, boolean hov) {
        try {
            boolean val = (boolean) e.getField().get(null);
            float tTarget = val ? 1f : 0f;
            String tKey   = entryKey(e);
            float tAnim   = toggleAnims.getOrDefault(tKey, tTarget);
            // lerp handle towards target
            tAnim += (tTarget - tAnim) * 0.22f;
            if (Math.abs(tAnim - tTarget) < 0.004f) tAnim = tTarget;
            toggleAnims.put(tKey, tAnim);

            int x  = rx - TOGGLE_W, y = cy - TOGGLE_H / 2;
            int bg = lerpArgb(themeToggleOffColor(), themeToggleOnColor(), tAnim);

            // Keep the toggle track as a proper pill in both states.
            drawRoundedRect(ctx, x, y, TOGGLE_W, TOGGLE_H, bg, TOGGLE_H / 2);

            // animated handle position
            int handleSize = TOGGLE_H - 4;
            int hxOff = x + 2;
            int hxOn  = x + TOGGLE_W - handleSize - 2;
            int hx    = hxOff + (int) (tAnim * (hxOn - hxOff));
            drawRoundedRect(ctx, hx, y + 2, handleSize, handleSize, themeToggleHandle(tAnim), handleSize / 2);

            int outline = hov ? TOGGLE_OUTLINE_HOV : TOGGLE_OUTLINE_IDLE;
            drawRoundedHollowRect(ctx, x, y, TOGGLE_W, TOGGLE_H, outline, TOGGLE_H / 2);
        } catch (Exception ex) {
            log.warn("Toggle: {}", e.getLabel(), ex);
        }
    }

    private void renderSlider(DrawContext ctx, ConfigEntryModel e, int rx, int cy) {
        try {
            double v = getDouble(e.getField());
            double t = clamp01((v - e.getRangeMin()) / (e.getRangeMax() - e.getRangeMin()));
            boolean drag = draggingSlider == e;
            int sx = rx - SLIDER_W, ty = cy - 3;
            ctx.fill(sx, ty, sx + SLIDER_W, ty + 6, BG_SLIDER_TRK);
            int fw = (int)(t * SLIDER_W);
            int accentCol = themeAccentColor();
            if (fw > 0) {
                ctx.fill(sx, ty, sx + fw, ty + 6, accentCol);
                if (fw > 2) ctx.fill(sx + fw - 2, ty, sx + fw, ty + 6, withAlpha(accentCol, 0xFF));
            }
            int hx = sx + fw - 4, hh = drag ? 14 : 10;
            ctx.fill(hx, cy - hh / 2, hx + 8, cy + hh / 2, SLIDER_HANDLE);
            String vs = formatSlider(e, v);
            ctx.drawTextWithShadow(client.textRenderer, Text.literal(vs),
                    sx - client.textRenderer.getWidth(vs) - 5,
                    cy - client.textRenderer.fontHeight / 2, themeTextMuted());
        } catch (Exception ex) {
            log.warn("Slider: {}", e.getLabel(), ex);
        }
    }

    private void renderSelect(DrawContext ctx, ConfigEntryModel e, int rx, int cy, boolean hov) {
        try {
            Object val = e.getField().get(null);
            String vs = toTitleCase(val != null ? val.toString() : "?");
            int bw = selectControlWidth(e);
            int bx = rx - bw, by = cy - CONTROL_H / 2;
            renderActionControl(ctx, bx, by, bw, hov);

            // Slide animation: prog decays from 1 → 0 after a cycle
            String sKey = entryKey(e);
            float prog = selectSlideAnims.getOrDefault(sKey, 0f);
            prog += (0f - prog) * 0.22f;
            if (Math.abs(prog) < 0.004f) prog = 0f;
            selectSlideAnims.put(sKey, prog);
            int slideDir = selectSlideDirs.getOrDefault(sKey, 1);
            int xOff = (int) (prog * slideDir * (bw * 0.55f));

            String label = hov
                    ? ("< " + vs + " >")
                    : (vs + " >");

            ctx.enableScissor(bx + 2, by, bx + bw - 2, by + CONTROL_H);
            ctx.drawCenteredTextWithShadow(client.textRenderer,
                    Text.literal(label),
                    bx + bw / 2 - xOff, cy - client.textRenderer.fontHeight / 2,
                    hov ? themeActionTextHoverColor() : themeActionTextColor());
            ctx.disableScissor();
        } catch (Exception ex) {
            log.warn("Select: {}", e.getLabel(), ex);
        }
    }

    private void renderColorSwatch(DrawContext ctx, ConfigEntryModel e, int rx, int cy, boolean hov) {
        try {
            int argb = (int) e.getField().get(null);
            int sx = rx - SWATCH_W, sy = cy - SWATCH_H / 2;
            renderCheckerboard(ctx, sx, sy, SWATCH_W, SWATCH_H);
            ctx.fill(sx, sy, sx + SWATCH_W, sy + SWATCH_H, argb);
            drawHollowRect(ctx, sx - 1, sy - 1, SWATCH_W + 2, SWATCH_H + 2, hov ? themeAccentColor() : BORDER_MID);
            if (hov) ctx.drawCenteredTextWithShadow(client.textRenderer, Text.literal("§f✎"),
                    sx + SWATCH_W / 2, cy - client.textRenderer.fontHeight / 2, 0xFFFFFFFF);
        } catch (Exception ex) {
            log.warn("Swatch: {}", e.getLabel(), ex);
        }
    }

    private void renderButton(DrawContext ctx, ConfigEntryModel e, int rx, int cy, boolean hov) {
        String text = e.getButtonText() != null ? e.getButtonText() : "RUN";
        int bw = buttonControlWidth(e);
        int bx = rx - bw, by = cy - CONTROL_H / 2;
        renderActionControl(ctx, bx, by, bw, hov);
        ctx.drawCenteredTextWithShadow(client.textRenderer, Text.literal(text),
                bx + bw / 2, cy - client.textRenderer.fontHeight / 2,
                hov ? themeActionTextHoverColor() : themeActionTextColor());
    }

    private void renderStringInput(DrawContext ctx, ConfigEntryModel e, int rx, int cy, boolean hov) {
        int bw = TEXT_INPUT_W;
        int bx = rx - bw, by = cy - CONTROL_H / 2;
        boolean editingThis = editingTextEntry == e;
        renderActionControl(ctx, bx, by, bw, hov || editingThis);

        String raw;
        if (editingThis) {
            raw = editingTextBuffer.toString();
        } else {
            try {
                Object value = e.getField().get(null);
                raw = value instanceof String s ? s : "";
            } catch (Exception ex) {
                raw = "";
            }
        }

        boolean blink = editingThis && (cursorTick / 10) % 2 == 0;
        String content = raw.isEmpty() ? " " : raw;
        String display = client.textRenderer.trimToWidth(content, bw - 12);
        if (editingThis && blink) {
            display = client.textRenderer.trimToWidth(display + "|", bw - 12);
        }

        ctx.drawTextWithShadow(client.textRenderer,
                Text.literal(display),
                bx + 6,
                cy - client.textRenderer.fontHeight / 2,
                hov || editingThis ? themeActionTextHoverColor() : themeActionTextColor());
    }

    private void renderActionControl(DrawContext ctx, int x, int y, int w, boolean hov) {
        int top = hov ? themeControlTopHover() : themeControlTop();
        int bottom = hov ? themeControlBottomHover() : themeControlBottom();
        int border = hov ? themedBorderHighlight() : themedBorderMid();

        drawRoundedRect(ctx, x, y, w, CONTROL_H, top, CONTROL_RADIUS);
        ctx.fill(x, y + CONTROL_H / 2, x + w, y + CONTROL_H, bottom);
        drawRoundedHollowRect(ctx, x, y, w, CONTROL_H, border, CONTROL_RADIUS);

        // Keep a subtle bevel so square controls still feel crisp and readable.
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, 0x2AFFFFFF);
        ctx.fill(x + 1, y + CONTROL_H - 2, x + w - 1, y + CONTROL_H - 1, 0x22000000);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Scrollbar
    // ═══════════════════════════════════════════════════════════════════════

    private void renderScrollbar(DrawContext ctx, int sx, int top, int ch) {
        ScrollbarMetrics metrics = getScrollbarMetrics();
        if (metrics == null) return;
        ctx.fill(sx, top, sx + SCROLL_W, top + ch, 0x14FFFFFF);
        ctx.fill(sx, metrics.thumbY(), sx + SCROLL_W, metrics.thumbY() + metrics.thumbH(), themeAccentColor());
    }

    private @Nullable ScrollbarMetrics getScrollbarMetrics() {
        if (maxScroll <= 0) return null;

        int cx = cachedGx + cachedSidebarW;
        int cy = cachedGy + HEADER_H;
        int cw = cachedGw - cachedSidebarW;
        int ch = cachedGh - HEADER_H;
        int sx = cx + cw - SCROLL_W - 2;

        double ratio = (double) ch / (maxScroll + ch);
        int thumbH = Math.max(20, (int) (ratio * ch));
        int thumbY = cy + (int) ((scrollOffset / maxScroll) * (ch - thumbH));
        return new ScrollbarMetrics(sx, cy, ch, thumbY, thumbH);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Color editor modal
    // ═══════════════════════════════════════════════════════════════════════

    private void renderColorEditor(DrawContext ctx) {
        if (editingColor == null) return;
        ColorEditorLayout layout = getColorEditorLayout();
        int ceH = layout.h();
        int ceW = layout.w();
        int px = layout.x();
        int py = layout.y();

        ctx.fill(cachedGx + cachedSidebarW, cachedGy + HEADER_H,
                cachedGx + cachedGw, cachedGy + cachedGh, 0x88000000);
        ctx.fill(px, py, px + ceW, py + ceH, BG_COLOR_ED);
        drawGlowBorder(ctx, px, py, ceW, ceH);

        ctx.drawCenteredTextWithShadow(client.textRenderer, Text.literal("Edit Color"),
                px + ceW / 2, py + 9, themeHubAccentTextColor());
        ctx.drawTextWithShadow(client.textRenderer, Text.literal("✕"), layout.closeX(), py + 9, themeTextMuted());

        try {
            int argb = (int) editingColor.getField().get(null);
            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            float[] hsv = rgbToHsv(r, g, b);
            float h = hsv[0], s = hsv[1], v = hsv[2];

            int prevX = layout.previewX(), prevY = layout.previewY();
            renderCheckerboard(ctx, prevX, prevY, layout.previewW(), 18);
            ctx.fill(prevX, prevY, prevX + layout.previewW(), prevY + 18, argb);
            drawHollowRect(ctx, prevX - 1, prevY - 1, layout.previewW() + 2, 20, BORDER_BRIGHT);
            ctx.drawCenteredTextWithShadow(client.textRenderer,
                    Text.literal(String.format("#%08X", argb)),
                    px + ceW / 2, prevY + 22, themeTextMuted());

            int svX = layout.svX(), svY = layout.svY(), svSize = layout.svSize();
            drawSvSquare(ctx, svX, svY, svSize, h);
            int cx = svX + (int) (s * (svSize - 1));
            int cy = svY + (int) ((1f - v) * (svSize - 1));
            drawHollowRect(ctx, cx - 2, cy - 2, 5, 5, 0xFFFFFFFF);

            int tx = layout.trackX(), tw = layout.trackW();
            drawHueTrack(ctx, tx, layout.hueY(), tw, 8);
            int hx = tx + (int) (h * tw);
            drawHollowRect(ctx, hx - 2, layout.hueY() - 2, 5, 12, 0xFFFFFFFF);
            ctx.drawTextWithShadow(client.textRenderer, Text.literal("H"), tx - 10, layout.hueY() - 1, themeTextMuted());

            if (editingColor.isHasAlpha()) {
                renderCheckerboard(ctx, tx, layout.alphaY(), tw, 8);
                drawAlphaTrack(ctx, tx, layout.alphaY(), tw, 8, h, s, v);
                int ax = tx + (int) ((a / 255f) * tw);
                drawHollowRect(ctx, ax - 2, layout.alphaY() - 2, 5, 12, 0xFFFFFFFF);
                ctx.drawTextWithShadow(client.textRenderer, Text.literal("A"), tx - 10, layout.alphaY() - 1, themeTextMuted());
            }
        } catch (Exception e) {
            log.warn("Color editor render", e); }
    }

    private void drawSvSquare(DrawContext ctx, int x, int y, int size, float hue) {
        int step = 2;
        for (int yy = 0; yy < size; yy += step) {
            float v = 1f - (yy / (float) Math.max(1, size - 1));
            for (int xx = 0; xx < size; xx += step) {
                float s = xx / (float) Math.max(1, size - 1);
                int c = hsvToArgb(hue, s, v, 1f);
                ctx.fill(x + xx, y + yy, Math.min(x + xx + step, x + size), Math.min(y + yy + step, y + size), c);
            }
        }
        drawHollowRect(ctx, x - 1, y - 1, size + 2, size + 2, themedBorderMid());
    }

    private void drawHueTrack(DrawContext ctx, int x, int y, int w, int h) {
        int step = 2;
        for (int xx = 0; xx < w; xx += step) {
            float hue = xx / (float) Math.max(1, w - 1);
            int c = hsvToArgb(hue, 1f, 1f, 1f);
            ctx.fill(x + xx, y, Math.min(x + xx + step, x + w), y + h, c);
        }
        drawHollowRect(ctx, x - 1, y - 1, w + 2, h + 2, themedBorderMid());
    }

    private void drawAlphaTrack(DrawContext ctx, int x, int y, int w, int h, float hue, float sat, float val) {
        int step = 2;
        for (int xx = 0; xx < w; xx += step) {
            float a = xx / (float) Math.max(1, w - 1);
            int c = hsvToArgb(hue, sat, val, a);
            ctx.fill(x + xx, y, Math.min(x + xx + step, x + w), y + h, c);
        }
        drawHollowRect(ctx, x - 1, y - 1, w + 2, h + 2, themedBorderMid());
    }

    private void renderChannelSlider(DrawContext ctx, int x, int y, int w,
                                     int value, int color, String lbl) {
        ctx.drawTextWithShadow(client.textRenderer, Text.literal(lbl), x, y + 2, themeTextMuted());
        int tx = x + 14, tw = Math.max(30, w - 36), fw = (int)((value / 255.0) * tw);
        ctx.fill(tx, y + 2, tx + tw, y + 10, 0xCC1C1428);
        if (fw > 0) ctx.fill(tx, y + 2, tx + fw, y + 10, color);
        int handleX = Math.max(tx - 2, Math.min(tx + tw - 2, tx + fw - 2));
        ctx.fill(handleX, y, handleX + 5, y + 13, SLIDER_HANDLE);
        ctx.drawTextWithShadow(client.textRenderer, Text.literal(String.valueOf(value)),
                tx + tw + 6, y + 2, themeTextMuted());
    }

    private void renderCheckerboard(DrawContext ctx, int x, int y, int w, int h) {
        int sz = 4;
        for (int ry = y; ry < y + h; ry += sz)
            for (int rx = x; rx < x + w; rx += sz) {
                boolean dark = ((rx - x) / sz + (ry - y) / sz) % 2 == 0;
                ctx.fill(rx, ry, Math.min(rx + sz, x + w), Math.min(ry + sz, y + h),
                        dark ? 0xFFB9B0C6 : 0xFFF6F0FA);
            }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Tooltip
    // ═══════════════════════════════════════════════════════════════════════

    private void renderTooltip(DrawContext ctx, String text) {
        List<String> lines = wrapText(text, 240);
        int fh = client.textRenderer.fontHeight, lh = fh + 2;
        int tw = lines.stream().mapToInt(l -> client.textRenderer.getWidth(l)).max().orElse(60);
        int bw = tw + 14, bh = lines.size() * lh + 10;
        int tx = frameMouseX + 12, ty = frameMouseY + 12;
        if (tx + bw > width - 4) tx = frameMouseX - bw - 8;
        if (ty + bh > height - 4) ty = frameMouseY - bh - 8;
        drawRoundedRect(ctx, tx, ty, bw, bh, themeTooltipBgColor(), CORNER_R_SMALL);
        drawRoundedHollowRect(ctx, tx, ty, bw, bh, themeTooltipBorderColor(), CORNER_R_SMALL);
        ctx.fill(tx + 1, ty + 1, tx + 3, ty + bh - 1, withAlpha(themeAccentColor(), 0x8A));
        for (int i = 0; i < lines.size(); i++)
            ctx.drawTextWithShadow(client.textRenderer, Text.literal(lines.get(i)),
                    tx + 7, ty + 5 + i * lh, themeTooltipTextColor());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Input
    // ═══════════════════════════════════════════════════════════════════════


     @Override
     public boolean mouseClicked(Click click, boolean doubled) {
         if (closingScreen) return true;
         int imx = (int) click.x(), imy = (int) click.y();

         // Settings & Close buttons
         int headerBtnY = cachedGy + (HEADER_H - HEADER_ACTION_BTN_SIZE) / 2;
         int headerBtnX = getHeaderActionButtonsStartX(cachedGx + cachedSidebarW, cachedGw - cachedSidebarW);
         
         // Settings button
         if (editingColor == null && isIn(imx, imy, headerBtnX, headerBtnY, HEADER_ACTION_BTN_SIZE, HEADER_ACTION_BTN_SIZE)) {
             if (client != null) client.setScreen(new net.iqaddons.mod.screen.IQGlobalConfigurationScreen(this, configClasses));
             return true;
         }
         
         // Close button
         int closeBtnX = headerBtnX + HEADER_ACTION_BTN_SIZE + HEADER_ACTION_BTN_GAP;
         if (editingColor == null && isIn(imx, imy, closeBtnX, headerBtnY, HEADER_ACTION_BTN_SIZE, HEADER_ACTION_BTN_SIZE)) {
             close();
             return true;
         }

        // Color editor
        if (editingColor != null) {
            handleColorEditorClick(imx, imy);
            return true;
        }

        // Search bar
        HeaderSearchBox searchBox = getHeaderSearchBox(cachedGx + cachedSidebarW, cachedGy, cachedGw - cachedSidebarW);
        int sbW = searchBox.w();
        int sbX = searchBox.x();
        int sbY = searchBox.y();
        if (isIn(imx, imy, sbX, sbY, sbW, SEARCH_H)) {
            searchFocused = true;
            // Clear × button
            if (!searchQuery.isEmpty() && isIn(imx, imy, sbX + sbW - 15, sbY, 12, SEARCH_H)) {
                searchQuery.setLength(0);
                scrollOffset = 0;
            }
            return true;
        }
        if (searchFocused) searchFocused = false;

        if (click.button() == 0 && editingColor == null) {
            int bx = getExternalLinksStartX(cachedGx, cachedSidebarW);
            int by = getExternalLinksY(cachedGy);
            for (ExternalLinkButton link : EXTERNAL_LINKS) {
                if (isIn(imx, imy, bx, by, LINK_BTN_SIZE, LINK_BTN_SIZE)) {
                    try {
                        Util.getOperatingSystem().open(link.url());
                    } catch (Exception e) {
                        log.warn("Failed to open {} link", link.name(), e);
                    }
                    return true;
                }
                bx += LINK_BTN_SIZE + LINK_BTN_GAP;
            }
        }

        if (click.button() == 0 && editingColor == null) {
            ScrollbarMetrics metrics = getScrollbarMetrics();
            if (metrics != null && isIn(imx, imy, metrics.x(), metrics.thumbY(), SCROLL_W, metrics.thumbH())) {
                draggingScrollbar = true;
                scrollbarDragOffsetY = imy - metrics.thumbY();
                return true;
            }
        }

        // Sidebar categories
        int listTop = cachedGy + LOGO_ZONE_H + 6;
        if (sidebarSlots.isEmpty()) rebuildSidebarSlots(listTop);


        if (imx >= cachedGx && imx < cachedGx + cachedSidebarW && imy > cachedGy + LOGO_ZONE_H) {
            for (SidebarCategorySlot slot : sidebarSlots) {
                if (isIn(imx, imy, cachedGx + 8, slot.y() + 2, cachedSidebarW - 16, SIDEBAR_ROW_H - 4)) {
                    if (slot.categoryIndex() != selectedCategory) {
                        selectedCategory = slot.categoryIndex();
                        scrollOffset = 0;
                        scrollVelocity = 0;
                        contentFadeAnim = 0f;
                    }
                    searchQuery.setLength(0);
                    return true;
                }
            }
        }

        // Entries
        for (RenderedEntry re : renderedEntries) {
            if (re.contains(imx, imy)) {
                handleEntryClick(re, imx, click.button()); return true; }
        }
        return super.mouseClicked(click, doubled);
    }

    private void handleEntryClick(RenderedEntry re, int mx, int button) {
        ConfigEntryModel entry = re.entry();
        int re2 = re.x() + re.w() - PADDING;
        switch (entry.getType()) {
            case BOOLEAN -> {
                if (button == 0) {
                    if (isBuildOverlayStyleEntry(entry)) {
                        int x = re2 - MODE_PICKER_W;
                        int mid = x + (MODE_PICKER_W / 2);
                        if (mx < mid) {
                            PhaseTwoConfig.simpleBuildProgressOverlay = true;
                        } else {
                            PhaseTwoConfig.simpleBuildProgressOverlay = false;
                        }
                    } else {
                        toggleBoolean(entry);
                    }
                }
            }
            case SELECT  -> {
                if (button == 0) cycleSelect(entry, +1);
                else if (button == 1) cycleSelect(entry, -1);
            }
            case COLOR   -> { if (button == 0) editingColor = entry; }
            case SECTION_HEADER -> { if (button == 0) entry.toggleExpanded(); }
            case BUTTON  -> {
                if (button == 0 && entry.getButtonAction() != null) {
                    try {
                        entry.getButtonAction().run();
                    } catch (Exception e) {
                        log.warn("Button: {}", entry.getLabel(), e); }
                }
            }
            case STRING -> {
                if (button == 0) startStringEdit(entry);
            }
            case INT_SLIDER, FLOAT_SLIDER, DOUBLE_SLIDER -> {
                if (button == 0) {
                    int sx = re2 - SLIDER_W;
                    if (mx >= sx && mx <= re2) {
                        draggingSlider = entry;
                        sliderTrackX = sx; sliderTrackW = SLIDER_W;
                        updateSlider(entry, mx);
                    }
                }
            }
        }
    }

   private void handleColorEditorClick(int mx, int my) {
        if (editingColor == null) return;
        ColorEditorLayout layout = getColorEditorLayout();
        int px = layout.x();
        int py = layout.y();
        if (mx < px || mx > px + layout.w() || my < py || my > py + layout.h()
                || (mx >= layout.closeX() - 2 && my <= py + 22)) {
            editingColor = null; return; }
        try {
            int argb = (int) editingColor.getField().get(null);
            int a = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            float[] hsv = rgbToHsv(r, g, b);
            float h = hsv[0], s = hsv[1], v = hsv[2];

            if (isIn(mx, my, layout.svX(), layout.svY(), layout.svSize(), layout.svSize())) {
                s = (float) clamp01((mx - layout.svX()) / (double) Math.max(1, layout.svSize() - 1));
                v = (float) (1.0 - clamp01((my - layout.svY()) / (double) Math.max(1, layout.svSize() - 1)));
            }
            if (isIn(mx, my, layout.trackX(), layout.hueY() - 2, layout.trackW(), 12)) {
                h = (float) clamp01((mx - layout.trackX()) / (double) Math.max(1, layout.trackW() - 1));
            }
            if (editingColor.isHasAlpha() && isIn(mx, my, layout.trackX(), layout.alphaY() - 2, layout.trackW(), 12)) {
                a = (int) Math.round(clamp01((mx - layout.trackX()) / (double) Math.max(1, layout.trackW() - 1)) * 255.0);
            }

            editingColor.getField().set(null, hsvToArgb(h, s, v, a / 255f));
        } catch (Exception e) {
            log.warn("Color click", e); }
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (closingScreen) return true;
        if (editingColor != null) {
            draggingScrollbar = false;
            handleColorEditorClick((int) click.x(), (int) click.y());
            return true;
        }
        if (draggingScrollbar) {
            ScrollbarMetrics metrics = getScrollbarMetrics();
            if (metrics == null) {
                draggingScrollbar = false;
                return true;
            }

            int trackRange = metrics.height() - metrics.thumbH();
            if (trackRange <= 0) {
                scrollOffset = 0;
                return true;
            }

            int thumbMin = metrics.top();
            int thumbMax = metrics.top() + trackRange;
            double thumbY = clamp((int) click.y() - scrollbarDragOffsetY, thumbMin, thumbMax);
            double t = (thumbY - thumbMin) / trackRange;
            scrollOffset = clamp(t * maxScroll, 0, maxScroll);
            return true;
        }
        if (draggingSlider != null) {
            updateSlider(draggingSlider, (int)click.x()); return true; }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (closingScreen) return true;
        draggingScrollbar = false;
        draggingSlider = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        if (closingScreen) return true;
        if (editingColor != null) return true;
        // add momentum instead of instantly jumping
        scrollVelocity -= vAmt * 16;
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (closingScreen) return true;
        if (editingTextEntry != null) {
            if (input.key() == 256) {
                editingTextEntry = null;
                return true;
            }
            if (input.key() == 257 || input.key() == 335) {
                saveStringEdit();
                return true;
            }
            if (input.key() == 259 && !editingTextBuffer.isEmpty()) {
                editingTextBuffer.deleteCharAt(editingTextBuffer.length() - 1);
                saveStringEdit();
                return true;
            }
        }
        if (input.key() == 256) {  // ESC
            if (editingColor != null) {
                editingColor = null;
                return true;
            }
            if (searchFocused && !searchQuery.isEmpty()) {
                searchQuery.setLength(0);
                scrollOffset = 0;
                return true;
            }
            close();
            return true;
        }
        if (searchFocused && input.key() == 259 && !searchQuery.isEmpty()) {  // BACKSPACE
            searchQuery.deleteCharAt(searchQuery.length() - 1);
            scrollOffset = 0; return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (closingScreen) return true;
        if (editingTextEntry != null) {
            String s = input.asString();
            if (!s.isEmpty() && editingTextBuffer.length() < TEXT_INPUT_MAX_LEN) {
                char c = s.charAt(0);
                if (c >= 32 && c != 127) {
                    editingTextBuffer.append(c);
                    saveStringEdit();
                    return true;
                }
            }
            return true;
        }
        if (searchFocused) {
            searchQuery.append(input.asString());
            scrollOffset = 0;
            return true;
        }
        return super.charTyped(input);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Mutation helpers
    // ═══════════════════════════════════════════════════════════════════════

    private void toggleBoolean(ConfigEntryModel e) {
        try {
            e.getField().set(null, !(boolean) e.getField().get(null));
        } catch (Exception ex) {
            log.warn("Toggle: {}", e.getLabel(), ex); }
    }

    private void cycleSelect(ConfigEntryModel e, int direction) {
        try {
            Object[] vals = e.getEnumValues();
            if (vals == null) return;
            Object cur = e.getField().get(null);
            int idx = 0;
            for (int i = 0; i < vals.length; i++) if (vals[i].equals(cur)) { idx = i; break; }
            int newIdx = ((idx + direction) % vals.length + vals.length) % vals.length;
            e.getField().set(null, vals[newIdx]);
            // Trigger slide: text slides in opposite to direction of travel
            String key = entryKey(e);
            selectSlideAnims.put(key, 1.0f);
            selectSlideDirs.put(key, -direction); // text slides in from opposite side
        } catch (Exception ex) {
            log.warn("Cycle: {}", e.getLabel(), ex); }
    }

    private void updateSlider(ConfigEntryModel e, int mx) {
        double t = clamp01((double) (mx - sliderTrackX) / sliderTrackW);
        double val = e.getRangeMin() + t * (e.getRangeMax() - e.getRangeMin());
        try {
            switch (e.getType()) {
                case INT_SLIDER -> e.getField().set(null, (int) Math.round(val));
                case FLOAT_SLIDER -> e.getField().set(null, (float) val);
                case DOUBLE_SLIDER -> e.getField().set(null, val);
            }
        } catch (Exception ex) {
            log.warn("Slider: {}", e.getLabel(), ex); }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Layout helpers
    // ═══════════════════════════════════════════════════════════════════════

    private int rowHeight(ConfigEntryModel e) {
        return switch (e.getType()) {
            case SEPARATOR      -> SEP_H;
            case SECTION_HEADER -> SEC_H;
            case UNSUPPORTED -> 0;
            default -> {
                if (e.getDescription() == null || e.getDescription().isBlank()) yield ROW_H_SLIM;
                int ctrlW = controlWidth(e);
                int descW = cachedGw - cachedSidebarW - 14 - PADDING - ctrlW - CONTROL_TEXT_GAP - PADDING;
                List<String> lines = wrapText(e.getDescription(), Math.max(60, descW));
                yield lines.size() > 1 ? ROW_H_FULL_2 : ROW_H_FULL_1;
            }
        };
    }

    private int computeSidebarWidth() {
        int maxSidebarW = Math.max(SIDEBAR_BASE_W, cachedGw - CONTENT_MIN_W);
        if (client == null) return SIDEBAR_BASE_W;

        int categoryTextW = categories.stream()
                .map(ConfigCategory::name)
                .mapToInt(client.textRenderer::getWidth)
                .max()
                .orElse(0);

        int logoTitleW = client.textRenderer.getWidth("IQ");
        int logoSubtitleW = client.textRenderer.getWidth("Config");
        int footerW = client.textRenderer.getWidth("MODRINTH VERSION v1.0.2");

        int desiredW = Math.max(
                SIDEBAR_BASE_W,
                Math.max(
                        16 + categoryTextW + 16,
                        Math.max(10 + LOGO_SIZE + 8 + Math.max(logoTitleW, logoSubtitleW) + 12,
                                10 + footerW + 12)
                )
        );

        return Math.min(maxSidebarW, desiredW);
    }

    private void refreshSharedUiSettings() {
        sharedThemeIndex = IQGlobalConfigurationScreen.getSharedThemeIndex();
        sharedGuiOpacity = IQGlobalConfigurationScreen.getSharedGuiOpacity();
        sharedAnimationsEnabled = IQGlobalConfigurationScreen.isSharedAnimationsEnabled();
        sharedAnimationSpeed = IQGlobalConfigurationScreen.getSharedAnimationSpeed();
        sharedOutlineShadow = IQGlobalConfigurationScreen.isSharedOutlineShadowEnabled();
        sharedBlurEnabled = IQGlobalConfigurationScreen.isSharedBlurEnabled();
        sharedBlurIntensity = IQGlobalConfigurationScreen.getSharedBlurIntensity();
    }

    private int applySharedOpacity(int baseRgb) {
        int alpha = (int) Math.round(Math.max(0.0, Math.min(1.0, sharedGuiOpacity)) * 255.0);
        return (baseRgb & 0x00FFFFFF) | (alpha << 24);
    }

    private int themePanelColor() {
        int base = switch (sharedThemeIndex) {
            case 1 -> 0x0D0D0D;
            case 2 -> 0xF5F7FA;
            case 3 -> 0x07101A;
            case 4 -> 0x16080B;
            case 5 -> 0x06110B;
            default -> 0x07060D;
        };
        return applySharedOpacity(base);
    }

    private int themeSidebarColor() {
        int base = switch (sharedThemeIndex) {
            case 1 -> 0x121212;
            case 2 -> 0xEBEEF2;
            case 3 -> 0x081723;
            case 4 -> 0x190B10;
            case 5 -> 0x081710;
            default -> 0x080710;
        };
        return applySharedOpacity(base);
    }

    private int themeHeaderColor() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xD5181818;
            case 2 -> 0xD5EBEEF2;
            case 3 -> 0xD50C1A29;
            case 4 -> 0xD51C0D14;
            case 5 -> 0xD50B1A13;
            default -> BG_HEADER;
        };
    }

    private int themeSearchColor() {
        return switch (sharedThemeIndex) {
            case 2 -> 0xC0FFFFFF;
            case 3 -> 0xC00F1D2D;
            case 4 -> 0xC0180D16;
            case 5 -> 0xC00E1B16;
            case 1 -> 0xC0131313;
            default -> BG_SEARCH;
        };
    }

    private int themeSearchActiveColor() {
        return switch (sharedThemeIndex) {
            case 2 -> 0xD6F8F9FA;
            case 3 -> 0xD0152940;
            case 4 -> 0xD6201320;
            case 5 -> 0xD613281F;
            case 1 -> 0xD01B1B1B;
            default -> BG_SEARCH_ACT;
        };
    }

    private int themeEntryColor() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xA3181818;
            case 2 -> 0xA3FFFFFF; // Light - card surface
            case 3 -> 0xA3132132;
            case 4 -> 0xA328151C;
            case 5 -> 0xA313251B;
            default -> BG_ENTRY;
        };
    }

    private int themeEntryHoverColor() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xC5222222;
            case 2 -> 0xC5F0F3F6; // Light - subtle hover
            case 3 -> 0xC51B2D43;
            case 4 -> 0xC5381E29;
            case 5 -> 0xC51C3326;
            default -> BG_ENTRY_HOV;
        };
    }

    private int themeSectionColor() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xB0161616;
            case 2 -> 0xB0EBEEF2; // Light - secondary bg
            case 3 -> 0xB0142235;
            case 4 -> 0xB026131C;
            case 5 -> 0xB014241C;
            default -> BG_SECTION;
        };
    }

    private int themeChildColor() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xA31A1A1A;
            case 2 -> 0xA3F0F3F6; // Light - slightly offset from entry
            case 3 -> 0xA316263A;
            case 4 -> 0xA3291722;
            case 5 -> 0xA316281F;
            default -> BG_CHILD;
        };
    }

    private int themeAccentColor() {
        return switch (sharedThemeIndex) {
            case 3 -> 0xFF4A9EFF;
            case 4 -> 0xFFFF5A78;
            case 5 -> 0xFF4BE08A;
            case 2 -> 0xFF1F2328; // Light - text primary as accent
            case 1 -> 0xFFEAEAEA;
            default -> ACCENT;
        };
    }

    private int themeToggleOnColor() {
        return switch (sharedThemeIndex) {
            case 3 -> 0xFF3A96E8; // Ocean - blue
            case 4 -> 0xFFE83A50; // Crimson - red
            case 5 -> 0xFF35C06E; // Emerald - green
            case 2 -> 0xFF1F2328; // Light - dark bg
            case 1 -> 0xFFEAEAEA; // Dark - near-white
            default -> BG_TOGGLE_ON; // Default IQ - pink
        };
    }

    private int themeToggleOffColor() {
        return switch (sharedThemeIndex) {
            case 3 -> 0xC21B2440; // Ocean
            case 4 -> 0xC227151F; // Crimson
            case 5 -> 0xC21A2620; // Emerald
            case 2 -> 0xC2D0D7DE; // Light - soft border gray
            case 1 -> 0xC22A2A2A; // Dark
            default -> BG_TOGGLE_OFF;
        };
    }

    private int themeToggleHandle(float tAnim) {
        if (sharedThemeIndex == 1) return lerpArgb(0xFF6A6A6A, 0xFF0D0D0D, tAnim);
        return TOGGLE_HANDLE;
    }

    private int themeTextMain() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xFFF5F5F5;
            case 2 -> 0xFF1A1530;
            case 3 -> 0xFFDDF1FF;
            case 4 -> 0xFFFFDEE5;
            case 5 -> 0xFFD4FFE0;
            default -> 0xFFFFF7FD;
        };
    }

    private int themeTextMuted() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xFF7D7D7D;
            case 2 -> 0xFF544372;
            case 3 -> 0xFF6E9CB8;
            case 4 -> 0xFFA97881;
            case 5 -> 0xFF669A77;
            default -> 0xFF9E86AF;
        };
    }

    private int themeFeatureTitleColor() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xFFFFFFFF;
            case 2 -> 0xFF1A1A2E;
            case 3 -> 0xFFE9F8FF;
            case 4 -> 0xFFFFF1F4;
            case 5 -> 0xFFF3FFF3;
            default -> 0xFFFFF6FD;
        };
    }

    private int themeFeatureDescriptionColor() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xFF848484;
            case 2 -> 0xFF63537E;
            case 3 -> 0xFF709EB9;
            case 4 -> 0xFFAD7D86;
            case 5 -> 0xFF689B78;
            default -> 0xFFAA8FBF;
        };
    }

    private int themeActionTextColor() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xFFCCCCCC;
            case 2 -> 0xFF3A2860;
            case 3 -> 0xFFD9F0FF;
            case 4 -> 0xFFFFE4E8;
            case 5 -> 0xFFD8FFE8;
            default -> 0xFFFFECF8;
        };
    }

    private int themeActionTextHoverColor() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xFFFFFFFF;
            case 2 -> 0xFF1F1050;
            case 3, 4, 5 -> 0xFFFFFFFF;
            default -> T_ACTION_TEXT_HOV;
        };
    }

    private int themeHubAccentTextColor() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xFFDDDDDD;
            case 2 -> 0xFF5030A0;
            case 3 -> 0xFF80C8FF;
            case 4 -> 0xFFFF9AAA;
            case 5 -> 0xFF70FF9E;
            default -> T_ACCENT;
        };
    }

    private int themedBorderDim() {
        if (!sharedOutlineShadow) return 0x00000000;
        return switch (sharedThemeIndex) {
            case 1 -> 0x40404040; // Dark
            case 2 -> 0x3A8A78A8; // Light
            case 3 -> 0x30304F72; // Ocean
            case 4 -> 0x304F2A3A; // Crimson
            case 5 -> 0x30315A42; // Emerald
            default -> BORDER_DIM;
        };
    }

    private int themedBorderMid() {
        if (!sharedOutlineShadow) return 0x00000000;
        return switch (sharedThemeIndex) {
            case 1 -> 0x6A8E8E8E; // Dark
            case 2 -> 0x6A7A5FB0; // Light
            case 3 -> 0x6A5A9FD9; // Ocean
            case 4 -> 0x6AD97A93; // Crimson
            case 5 -> 0x6A63C78A; // Emerald
            default -> BORDER_MID;
        };
    }
    private int themedBorderBright() { return sharedOutlineShadow ? withAlpha(themeAccentColor(), 0xB5) : 0x00000000; }
    private int themedBorderHighlight() { return sharedOutlineShadow ? withAlpha(themeAccentColor(), 0x44) : 0x00000000; }

    private int themeSepPillBg() {
        return switch (sharedThemeIndex) {
            case 1 -> 0x6B1A1A1A; // Dark
            case 2 -> 0xB0D4CCE8; // Light — soft lavender pill
            case 3 -> 0x6B0A1A2A; // Ocean
            case 4 -> 0x6B160A12; // Crimson
            case 5 -> 0x6B0A160E; // Emerald
            default -> SEP_PILL_BG;
        };
    }

    private int themeSepPillBorder() {
        return switch (sharedThemeIndex) {
            case 1 -> 0x30808080; // Dark — mid gray border
            case 2 -> 0x506858A8; // Light — visible purple border
            case 3 -> withAlpha(0x4A9EFF, 0x30); // Ocean
            case 4 -> withAlpha(0xFF5A78, 0x30); // Crimson
            case 5 -> withAlpha(0x4BE08A, 0x30); // Emerald
            default -> SEP_PILL_BORDER;
        };
    }

    private int themeSepPillGlow() {
        return switch (sharedThemeIndex) {
            case 1 -> 0x10EAEAEA; // Dark — near-white glow
            case 2 -> 0x20BC7FE8; // Light — purple glow
            case 3 -> 0x154A9EFF; // Ocean
            case 4 -> 0x15FF5A78; // Crimson
            case 5 -> 0x154BE08A; // Emerald
            default -> SEP_PILL_GLOW;
        };
    }

    private int themeSepText() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xFF5A5A5A; // Dark — mid gray
            case 2 -> 0xFF5A4878; // Light — dark purple on light bg
            case 3 -> 0xFF6A9ACC; // Ocean
            case 4 -> 0xFFB06070; // Crimson
            case 5 -> 0xFF50A070; // Emerald
            default -> SEP_TEXT;
        };
    }

    private int themeTooltipBgColor() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xEE111111; // Dark
            case 2 -> 0xEEF2F5F9; // Light
            case 3 -> 0xEE0B1A2A; // Ocean
            case 4 -> 0xEE1D0D14; // Crimson
            case 5 -> 0xEE0C1A13; // Emerald
            default -> 0xEE100B16;
        };
    }

    private int themeTooltipBorderColor() {
        return withAlpha(themeAccentColor(), sharedOutlineShadow ? 0xB8 : 0x88);
    }

    private int themeTooltipTextColor() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xFFD8D8D8;
            case 2 -> 0xFF3E3260;
            case 3 -> 0xFFAED7FB;
            case 4 -> 0xFFDDA3AE;
            case 5 -> 0xFFA7D8B8;
            default -> themeTextMuted();
        };
    }

    private int themeSepLine() {
        return switch (sharedThemeIndex) {
            case 2 -> 0x28000000; // Light — dark line visible on light bg
            case 1 -> 0x282A2A2A; // Dark
            default -> SEP_LINE;
        };
    }

    private int themeSepLineBright() {
        int acc = themeAccentColor();
        return withAlpha(acc, sharedThemeIndex == 2 ? 0x40 : 0x18);
    }

    private int themeSidebarTextActive() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xFFF0F0F0;
            case 2 -> 0xFF1A1530;
            case 3 -> 0xFFDDF1FF;
            case 4 -> 0xFFFFDEE5;
            case 5 -> 0xFFD4FFE0;
            default -> 0xFFFFF7FD;
        };
    }

    private int themeSidebarTextHover() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xFFC8C8C8;
            case 2 -> 0xFF6B3DAA;
            case 3 -> 0xFF8BCBFF;
            case 4 -> 0xFFFF9CAC;
            case 5 -> 0xFF76F3A0;
            default -> 0xFFF2A5DA;
        };
    }

    private int themeSidebarTextMuted() {
        return switch (sharedThemeIndex) {
            case 1 -> 0xFF737373;
            case 2 -> 0xFF544372;
            case 3 -> 0xFF6D95AC;
            case 4 -> 0xFF9E6E78;
            case 5 -> 0xFF5F8F6E;
            default -> 0xFF9B84AD;
        };
    }

    private int themeControlTop() {
        return switch (sharedThemeIndex) {
            case 3 -> 0xCC1B3049;
            case 4 -> 0xCC3A1C2A;
            case 5 -> 0xCC1D3A2A;
            case 2 -> 0xCCC7B4DA;
            case 1 -> 0xCC1A1A1A;
            default -> BG_CONTROL_TOP;
        };
    }

    private int themeControlTopHover() {
        return switch (sharedThemeIndex) {
            case 3 -> 0xD6283E58;
            case 4 -> 0xD64B2534;
            case 5 -> 0xD6294A35;
            case 2 -> 0xD6D6C4E4;
            case 1 -> 0xD6222222;
            default -> BG_CONTROL_TOP_HOV;
        };
    }

    private int themeControlBottom() {
        return switch (sharedThemeIndex) {
            case 3 -> 0xCC162436;
            case 4 -> 0xCC22121C;
            case 5 -> 0xCC13241B;
            case 2 -> 0xCCC2B4D0;
            case 1 -> 0xCC161616;
            default -> BG_CONTROL_BOTTOM;
        };
    }

    private int themeControlBottomHover() {
        return switch (sharedThemeIndex) {
            case 3 -> 0xD61E2E43;
            case 4 -> 0xD62E1824;
            case 5 -> 0xD61A3124;
            case 2 -> 0xD6CDBFD9;
            case 1 -> 0xD61E1E1E;
            default -> BG_CONTROL_BOTTOM_HOV;
        };
    }

    private int controlWidth(@NotNull ConfigEntryModel e) {
        return switch (e.getType()) {
            case BOOLEAN -> isBuildOverlayStyleEntry(e) ? MODE_PICKER_W + 4 : TOGGLE_W + 4;
            case INT_SLIDER, FLOAT_SLIDER, DOUBLE_SLIDER -> SLIDER_W + 30;
            case SELECT -> selectControlWidth(e);
            case STRING -> TEXT_INPUT_W + 4;
            case COLOR -> SWATCH_W + 4;
            case BUTTON -> buttonControlWidth(e);
            default -> 0;
        };
    }

    private void startStringEdit(ConfigEntryModel entry) {
        editingTextEntry = entry;
        editingTextBuffer.setLength(0);
        try {
            Object value = entry.getField().get(null);
            if (value instanceof String text) {
                editingTextBuffer.append(text);
            }
        } catch (Exception ex) {
            log.warn("String edit start: {}", entry.getLabel(), ex);
        }
    }

    private void saveStringEdit() {
        if (editingTextEntry == null || editingTextEntry.getField() == null) return;
        try {
            editingTextEntry.getField().set(null, editingTextBuffer.toString());
        } catch (Exception ex) {
            log.warn("String edit save: {}", editingTextEntry.getLabel(), ex);
        }
    }

    private int selectControlWidth(ConfigEntryModel e) {
        if (client == null) return SELECT_W;
        try {
            Object val = e.getField().get(null);
            String vs = toTitleCase(val != null ? val.toString() : "?");
            int textW = client.textRenderer.getWidth(vs + " >");
            return clampControlWidth(textW + (CONTROL_TEXT_PAD_X * 2));
        } catch (Exception ignored) {
            return SELECT_W;
        }
    }

    private int buttonControlWidth(ConfigEntryModel e) {
        if (client == null) return BUTTON_W;
        String text = e.getButtonText() != null ? e.getButtonText() : "RUN";
        int textW = client.textRenderer.getWidth(text);
        return clampControlWidth(textW + (CONTROL_TEXT_PAD_X * 2));
    }

    private int clampControlWidth(int width) {
        return Math.max(CONTROL_MIN_W, Math.min(CONTROL_MAX_W, width));
    }

    private int countControls(List<ConfigEntryModel> list) {
        return (int) list.stream()
                .filter(e -> e.getType() != EntryType.SEPARATOR && e.getType() != EntryType.UNSUPPORTED)
                .count();
    }

    private boolean isBuildOverlayStyleEntry(ConfigEntryModel e) {
        if (e.getField() == null) return false;
        return "simpleBuildProgressOverlay".equals(e.getField().getName())
                && "PhaseTwoConfig".equals(e.getField().getDeclaringClass().getSimpleName());
    }

    private boolean shouldHideEntry(ConfigEntryModel e) {
        return false;
    }

    private List<ConfigEntryModel> filterHiddenEntries(List<ConfigEntryModel> entries) {
        return entries;
    }

    private void rebuildSidebarSlots(int listTop) {
        sidebarSlots.clear();
        int y = listTop;
        int lastGroup = -1;

        for (int categoryIndex : getOrderedCategoryIndexes()) {
            int group = getSidebarGroup(categories.get(categoryIndex).id());
            if (lastGroup != -1 && group != lastGroup) y += SIDEBAR_GROUP_GAP;

            sidebarSlots.add(new SidebarCategorySlot(categoryIndex, y));
            y += SIDEBAR_ROW_H + SIDEBAR_ROW_GAP;
            lastGroup = group;
        }
    }

    private List<Integer> getOrderedCategoryIndexes() {
        List<Integer> ordered = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) ordered.add(i);
        ordered.sort((a, b) -> {
            ConfigCategory ca = categories.get(a);
            ConfigCategory cb = categories.get(b);

            int ga = getSidebarGroup(ca.id());
            int gb = getSidebarGroup(cb.id());
            if (ga != gb) return Integer.compare(ga, gb);

            int oa = getSidebarOrder(ca.id());
            int ob = getSidebarOrder(cb.id());
            if (oa != ob) return Integer.compare(oa, ob);
            return Integer.compare(a, b);
        });
        return ordered;
    }

    private int getSidebarGroup(String categoryId) {
        return switch (categoryId) {
            case "Configuration" -> 0;
            case "KuudraGeneralConfig", "PhaseOneConfig", "PhaseTwoConfig", "PhaseThreeConfig", "PhaseFourConfig" -> 1;
            case "SupporterCategory", "SupporterHelperCategory" -> 2;
            default -> 3;
        };
    }

    private int getSidebarOrder(String categoryId) {
        return switch (categoryId) {
            case "Configuration" -> 0;
            case "KuudraGeneralConfig" -> 10;
            case "PhaseOneConfig" -> 20;
            case "PhaseTwoConfig" -> 30;
            case "PhaseThreeConfig" -> 40;
            case "PhaseFourConfig" -> 50;
            case "SupporterCategory" -> 60;
            case "SupporterHelperCategory" -> 70;
            default -> 999;
        };
    }

    private String getSidebarDisplayName(ConfigCategory category) {
        if ("PhaseFourConfig".equals(category.id())) return "Phase 4 - Boss";
        return category.name();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Text helpers
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Wraps {@code text} to lines of at most {@code maxWidth} pixels.
     */
    private List<String> wrapText(String text, int maxWidth) {
        if (maxWidth <= 0) return List.of(text);
        List<String> lines = new ArrayList<>();
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\n", -1);

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (client.textRenderer.getWidth(candidate) <= maxWidth) {
                    line = new StringBuilder(candidate);
                } else {
                    if (!line.isEmpty()) {
                        lines.add(line.toString());
                        line = new StringBuilder();
                    }
                    if (client.textRenderer.getWidth(word) > maxWidth)
                        lines.add(client.textRenderer.trimToWidth(word, maxWidth - 6) + "…");
                    else
                        line = new StringBuilder(word);
                }
            }
            if (!line.isEmpty()) lines.add(line.toString());
        }

        return lines.isEmpty() ? List.of(text) : lines;
    }

    private double getDouble(Field f) throws Exception {
        Object v = f.get(null);
        return (v instanceof Number n) ? n.doubleValue() : 0.0;
    }

    private String formatSlider(ConfigEntryModel e, double v) {
        return switch (e.getType()) {
            case INT_SLIDER -> String.valueOf((int) Math.round(v));
            case FLOAT_SLIDER -> String.format(Locale.ROOT, "%.2f", v);
            case DOUBLE_SLIDER -> String.format(Locale.ROOT, "%.2f", v);
            default -> "";
        };
    }

    private String toTitleCase(String s) {
        return Arrays.stream(s.split("_"))
                .map(w -> w.isEmpty() ? w
                        : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Animation helpers
    // ═══════════════════════════════════════════════════════════════════════════════════════

    /** Applies momentum-based scroll each frame. */
    private void updateScrollAnimation() {
        if (!sharedAnimationsEnabled) {
            if (scrollVelocity != 0) {
                scrollOffset = clamp(scrollOffset + scrollVelocity, 0, maxScroll);
            }
            scrollVelocity = 0;
            scrollOffset = clamp(scrollOffset, 0, maxScroll);
            return;
        }
        if (scrollVelocity != 0) {
            scrollOffset = clamp(scrollOffset + scrollVelocity, 0, maxScroll);
            scrollVelocity *= (0.64 + (sharedAnimationSpeed * 0.20));
            if (Math.abs(scrollVelocity) < 0.05) scrollVelocity = 0;
        }
        scrollOffset = clamp(scrollOffset, 0, maxScroll);
    }

    private void updateScreenTransition() {
        if (!sharedAnimationsEnabled) {
            screenTransition = closingScreen ? 0f : 1f;
            if (closingScreen && !closeHandled) finalizeClose();
            return;
        }
        long now = Util.getMeasuringTimeMs();
        if (lastTransitionTimeMs < 0L) {
            lastTransitionTimeMs = now;
        }

        float dt = Math.min(50L, now - lastTransitionTimeMs) / 1000f;
        lastTransitionTimeMs = now;

        float speed = closingScreen ? SCREEN_CLOSE_SPEED : SCREEN_OPEN_SPEED;
        speed *= (float) (0.55 + (sharedAnimationSpeed * 1.35));
        float direction = closingScreen ? -1f : 1f;
        screenTransition = (float) clamp(screenTransition + (direction * speed * dt), 0.0, 1.0);

        if (closingScreen && !closeHandled && screenTransition <= 0f) {
            finalizeClose();
        }
    }

    private float easeInQuad(float t) {
        float clamped = (float) clamp01(t);
        return clamped * clamped;
    }

    private float easeOutCubic(float t) {
        float u = 1f - (float) clamp01(t);
        return 1f - (u * u * u);
    }

    /** Stable key based on field path, used for animation maps. */
    private String entryKey(ConfigEntryModel e) {
        if (e.getField() != null)
            return e.getField().getDeclaringClass().getName() + "#" + e.getField().getName();
        return "lbl#" + e.getLabel();
    }

    /** Key for section headers (no field). */
    private String sectionKey(ConfigEntryModel e) {
        return "sec#" + e.getLabel();
    }

    /**
     * Linearly interpolates between two ARGB colours.
     * {@code t=0} -> c0, {@code t=1} -> c1.
     */
    private int lerpArgb(int c0, int c1, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a0 = (c0 >> 24) & 0xFF, r0 = (c0 >> 16) & 0xFF, g0 = (c0 >> 8) & 0xFF, b0 = c0 & 0xFF;
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        return ((int) (a0 + (a1 - a0) * t) << 24)
             | ((int) (r0 + (r1 - r0) * t) << 16)
             | ((int) (g0 + (g1 - g0) * t) << 8)
             | (int) (b0 + (b1 - b0) * t);
    }

    // Draws a 1px smooth gradient line: dim → bright at 50% → dim.
    private void drawSeparatorGradientLine(DrawContext ctx, int x0, int x1, int y, boolean reverse,
                                           int dimColor, int brightColor) {
        if (x1 <= x0) return;
        int span = Math.max(1, x1 - x0 - 1);
        for (int x = x0; x < x1; x++) {
            float t = (x - x0) / (float) span;
            if (reverse) t = 1f - t;
            float bell = 1f - Math.abs(t * 2f - 1f);
            int color = lerpArgb(dimColor, brightColor, bell);
            ctx.fill(x, y, x + 1, y + 1, color);
        }
    }

    private int measureEntries(List<ConfigEntryModel> entries) {
        int used = 0;
        for (ConfigEntryModel entry : entries) {
            int rh = rowHeight(entry);
            if (rh == 0) continue;
            if (entry.getType() == EntryType.SEPARATOR && used > 0) {
                used += SEP_TOP_GAP;
            }
            int gap = (entry.getType() == EntryType.SEPARATOR) ? 0 : 4;
            used += rh + gap;
            if (entry.getType() == EntryType.SECTION_HEADER
                    && entry.getChildren() != null && !entry.getChildren().isEmpty()) {
                float sAnim = sectionAnims.getOrDefault(sectionKey(entry),
                        entry.isExpanded() ? 1f : 0f);
                if (sAnim > 0f) {
                    int fullChildH = measureEntries(entry.getChildren());
                    used += (int) (sAnim * fullChildH);
                }
            }
        }
        return used;
    }

    private void drawRoundedRect(DrawContext ctx, int x, int y, int w, int h, int color, int radius) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }

        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + w, y + h - r, color);

        // Use circular corner insets to avoid faceted/hexagonal corners.
        for (int i = 0; i < r; i++) {
            int inset = cornerInset(i, r);
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    private void drawRoundedHollowRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        drawRoundedHollowRect(ctx, x, y, w, h, color, CORNER_R_SMALL);
    }

    private void drawRoundedHollowRect(DrawContext ctx, int x, int y, int w, int h, int color, int radius) {
        if (w <= 1 || h <= 1) return;
        if (radius <= 0) {
            drawHollowRect(ctx, x, y, w, h, color);
            return;
        }

        int r = Math.min(radius, Math.min(w, h) / 2);

        for (int i = 0; i < r; i++) {
            int inset = cornerInset(i, r);
            // top / bottom arcs
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }

        // straight sides between rounded arcs
        ctx.fill(x, y + r, x + 1, y + h - r, color);
        ctx.fill(x + w - 1, y + r, x + w, y + h - r, color);
    }

    private int cornerInset(int row, int radius) {
        double dy = radius - row - 0.5;
        double dx = Math.sqrt(Math.max(0.0, (radius * (double) radius) - (dy * dy)));
        return Math.max(0, (int) Math.ceil(radius - dx));
    }

    private int getHeaderActionButtonsStartX(int headerX, int headerW) {
        int totalBtnW = (HEADER_ACTION_BTN_SIZE * 2) + HEADER_ACTION_BTN_GAP;
        return headerX + headerW - totalBtnW - HEADER_ACTION_BTN_RIGHT_PAD;
    }

    private HeaderSearchBox getHeaderSearchBox(int headerX, int headerY, int headerW) {
        int buttonsStartX = getHeaderActionButtonsStartX(headerX, headerW);
        int searchLeft = headerX + 10;
        int searchRight = Math.max(searchLeft + 60, buttonsStartX - 8);
        int availableW = Math.max(60, searchRight - searchLeft);
        int searchW = Math.min(190, availableW);
        int searchX = searchLeft + Math.max(0, (availableW - searchW) / 2);
        int searchY = headerY + (HEADER_H - SEARCH_H) / 2;
        return new HeaderSearchBox(searchX, searchY, searchW);
    }

    private ColorEditorLayout getColorEditorLayout() {
        int modalH = editingColor != null && editingColor.isHasAlpha() ? 228 : 192;
        int modalW = Math.min(248, cachedGw - 18);
        modalW = Math.max(188, modalW);
        int modalX = cachedGx + (cachedGw - modalW) / 2;
        int modalY = cachedGy + (cachedGh - modalH) / 2;
        int previewW = Math.max(60, Math.min(80, modalW - 128));
        int previewX = modalX + (modalW - previewW) / 2;
        int previewY = modalY + 26;
        int svSize = Math.max(96, Math.min(118, modalW - 40));
        int svX = modalX + (modalW - svSize) / 2;
        int svY = previewY + 30;
        int trackX = modalX + 18;
        int trackW = Math.max(140, modalW - 36);
        int hueY = svY + svSize + 10;
        int alphaY = hueY + 20;
        int closeX = modalX + modalW - 16;
        return new ColorEditorLayout(modalX, modalY, modalW, modalH,
                previewX, previewY, previewW,
                svX, svY, svSize,
                trackX, trackW, hueY, alphaY,
                closeX);
    }

    private float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float d = max - min;

        float h;
        if (d == 0f) h = 0f;
        else if (max == rf) h = ((gf - bf) / d + (gf < bf ? 6f : 0f)) / 6f;
        else if (max == gf) h = (((bf - rf) / d) + 2f) / 6f;
        else h = (((rf - gf) / d) + 4f) / 6f;

        float s = max == 0f ? 0f : d / max;
        return new float[]{h, s, max};
    }

    private int hsvToArgb(float h, float s, float v, float a) {
        h = (float) clamp01(h);
        s = (float) clamp01(s);
        v = (float) clamp01(v);
        a = (float) clamp01(a);

        float c = v * s;
        float hh = h * 6f;
        float x = c * (1f - Math.abs((hh % 2f) - 1f));
        float m = v - c;

        float rf = 0f, gf = 0f, bf = 0f;
        if (hh < 1f) {
            rf = c; gf = x;
        } else if (hh < 2f) {
            rf = x; gf = c;
        } else if (hh < 3f) {
            gf = c; bf = x;
        } else if (hh < 4f) {
            gf = x; bf = c;
        } else if (hh < 5f) {
            rf = x; bf = c;
        } else {
            rf = c; bf = x;
        }

        int ai = (int) Math.round(a * 255.0);
        int ri = (int) Math.round((rf + m) * 255.0);
        int gi = (int) Math.round((gf + m) * 255.0);
        int bi = (int) Math.round((bf + m) * 255.0);
        return ((ai & 0xFF) << 24) | ((ri & 0xFF) << 16) | ((gi & 0xFF) << 8) | (bi & 0xFF);
    }

    private void drawHeaderActionButton(DrawContext ctx, int x, int y, float hover) {
        int size = HEADER_ACTION_BTN_SIZE;
        int accRgb = themeAccentColor() & 0x00FFFFFF;
        int glow = lerpArgb(0x10000000, (0x34 << 24) | accRgb, hover);
        int top = lerpArgb(themeControlTop(), themeControlTopHover(), hover);
        int bottom = lerpArgb(themeControlBottom(), themeControlBottomHover(), hover);
        int body = lerpArgb(
                lerpArgb(themeControlTop(), themeControlBottom(), 0.5f),
                lerpArgb(themeControlTopHover(), themeControlBottomHover(), 0.5f),
                hover);
        int border = lerpArgb(withAlpha(themedBorderMid(), 0x88), (0xAA << 24) | accRgb, hover);

        ctx.fill(x - 1, y - 1, x + size + 1, y + size + 1, glow);
        ctx.fill(x, y, x + size, y + size / 2, top);
        ctx.fill(x, y + size / 2, x + size, y + size, bottom);
        ctx.fill(x + 1, y + 1, x + size - 1, y + size - 1, body);

        // Subtle bevel for a cleaner "closed" pixel look.
        ctx.fill(x + 1, y + 1, x + size - 1, y + 2, 0x42FFFFFF);
        ctx.fill(x + 1, y + size - 2, x + size - 1, y + size - 1, 0x3A000000);
        drawHollowRect(ctx, x, y, size, size, border);
    }

    private void drawHollowRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static double clamp01(double v) {
        return Math.min(1.0, Math.max(0.0, v));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.min(hi, Math.max(lo, v));
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    @Override
    public void close() {
        requestClose(parent);
    }

    private void requestClose(@Nullable Screen target) {
        if (closingScreen) return;
        closingScreen = true;
        closeHandled = false;
        closeTarget = target;
        editingColor = null;
        searchFocused = false;
        draggingScrollbar = false;
        draggingSlider = null;
        scrollVelocity = 0;
    }

    private void finalizeClose() {
        closeHandled = true;
        saveUiState();
        try {
            IQModClient.get().getConfigurator().saveConfig(Configuration.class);
        } catch (Exception e) {
            log.warn("Failed to save config on close", e);
        }
        if (client != null) client.setScreen(closeTarget);
    }

    private void saveUiState() {
        if (!IQGlobalConfigurationScreen.isSharedUiStatePersistenceEnabled()) return;
        if (selectedCategory >= 0 && selectedCategory < categories.size()) {
            store.set(K_LAST_CATEGORY_ID, categories.get(selectedCategory).id());
        }
        store.set(K_LAST_SCROLL, Math.max(0, scrollOffset));
        store.set(K_LAST_SEARCH, searchQuery.toString());
        store.set(K_LAST_EXPANDED, buildExpandedSectionsString());
        store.set(K_LAST_CLOSE_TIME, System.currentTimeMillis());
    }

    /**
     * Encodes all currently-expanded SECTION_HEADER entries across every category as a single
     * string.  Format: {@code categoryId~label1[~label2...];...} where {@code ~} separates path
     * segments and {@code ;} separates individual entries.  Nested sections are stored with their
     * full ancestor path so they can be restored independently of the parent's order.
     */
    private String buildExpandedSectionsString() {
        StringBuilder sb = new StringBuilder();
        for (ConfigCategory cat : categories) {
            collectExpandedPaths(cat.id(), cat.entries(), new java.util.ArrayDeque<>(), sb);
        }
        return sb.toString();
    }

    private void collectExpandedPaths(String categoryId, List<ConfigEntryModel> entries,
                                      java.util.Deque<String> parentPath, StringBuilder sb) {
        for (ConfigEntryModel entry : entries) {
            if (entry.getType() != EntryType.SECTION_HEADER) continue;
            // Sanitise label — replace reserved chars so the format stays parseable.
            String safeLabel = entry.getLabel().replace("~", " ").replace(";", " ");
            parentPath.addLast(safeLabel);
            if (entry.isExpanded()) {
                if (sb.length() > 0) sb.append(';');
                sb.append(categoryId).append('~');
                sb.append(String.join("~", parentPath));
            }
            if (entry.getChildren() != null && !entry.getChildren().isEmpty()) {
                collectExpandedPaths(categoryId, entry.getChildren(), parentPath, sb);
            }
            parentPath.removeLast();
        }
    }

    /**
     * Parses the string produced by {@link #buildExpandedSectionsString()} and sets the
     * corresponding SECTION_HEADER entries to expanded.
     */
    private void restoreExpandedSections(String encoded) {
        for (String part : encoded.split(";")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            String[] segments = trimmed.split("~", -1);
            if (segments.length < 2) continue;
            String targetCategoryId = segments[0];
            List<String> path = Arrays.asList(segments).subList(1, segments.length);
            for (ConfigCategory cat : categories) {
                if (cat.id().equals(targetCategoryId)) {
                    setExpandedByPath(cat.entries(), path, 0);
                    break;
                }
            }
        }
    }

    private void setExpandedByPath(List<ConfigEntryModel> entries, List<String> path, int depth) {
        if (depth >= path.size()) return;
        String label = path.get(depth);
        for (ConfigEntryModel entry : entries) {
            if (entry.getType() != EntryType.SECTION_HEADER) continue;
            String safeLabel = entry.getLabel().replace("~", " ").replace(";", " ");
            if (!safeLabel.equals(label)) continue;
            if (depth == path.size() - 1) {
                // This is the target — expand it if not already expanded.
                if (!entry.isExpanded()) entry.toggleExpanded();
            }
            if (entry.getChildren() != null) {
                setExpandedByPath(entry.getChildren(), path, depth + 1);
            }
        }
    }

    private record RenderedEntry(ConfigEntryModel entry, int x, int y, int w, int h) {
        boolean contains(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private record HeaderSearchBox(int x, int y, int w) {
    }

    private record SidebarCategorySlot(int categoryIndex, int y) {
    }

    private record ScrollbarMetrics(int x, int top, int height, int thumbY, int thumbH) {
    }

    private record ColorEditorLayout(int x, int y, int w, int h,
                                     int previewX, int previewY, int previewW,
                                     int svX, int svY, int svSize,
                                     int trackX, int trackW, int hueY, int alphaY,
                                     int closeX) {
    }

    private record ExternalLinkButton(Identifier iconTexture, String name, String url, String tooltip) {
    }
}
