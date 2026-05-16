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
import java.util.*;
import java.util.stream.Collectors;

public class IQConfigScreen extends Screen {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IQConfigScreen.class);

    private static final Identifier LOGO_TEXTURE = Identifier.of("iq", "textures/icon.png");
    private static final Identifier DISCORD_ICON_TEXTURE = Identifier.of("iq", "textures/social/discord.png");
    private static final Identifier MODRINTH_ICON_TEXTURE = Identifier.of("iq", "textures/social/modrinth.png");
    private static final Identifier PATREON_ICON_TEXTURE = Identifier.of("iq", "textures/social/patreon.png");
    private static final Identifier SETTINGS_ICON_TEXTURE = Identifier.of("iq", "textures/social/settings.png");
    private static final Identifier CLOSE_ICON_TEXTURE = Identifier.of("iq", "textures/social/close.png");
    private static final int LOGO_TEX_SIZE = 160;
    private static final int LOGO_SIZE = 24;

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
    private static final int T_ACTION_TEXT_HOV = 0xFFFFC2E7;

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

    private static final float GUI_W_RATIO = 0.49f;
    private static final float GUI_H_RATIO = 0.47f;
    private static final int GUI_MIN_W = 575;
    private static final int GUI_MIN_H = 320;
    private static final float SCREEN_OPEN_SPEED = 7.5f;
    private static final float SCREEN_CLOSE_SPEED = 11.0f;
    private static final int SCREEN_TRANSITION_Y = 10;

    private static final int SIDEBAR_BASE_W = 118;
    private static final int CONTENT_MIN_W = 340;
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
    private static final DataKey<String> K_EXPANDED_SECTIONS = DataKey.of("iqconfig.expandedSections", String.class);
    private static final DataKey<Double> K_UI_STATE_TIMESTAMP = DataKey.of("iqconfig.uiStateTimestamp", Double.class);
    private static final long UI_STATE_MAX_AGE_MS = 60_000L;

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

    private final StringBuilder searchQuery = new StringBuilder();
    private @Nullable String cachedDisplayQuery;
    private int cachedDisplayCategory = Integer.MIN_VALUE;
    private @Nullable List<ConfigEntryModel> cachedDisplayEntries;
    private final Map<String, List<String>> wrapTextCache = new HashMap<>();
    private boolean searchFocused = false;
    private int cursorTick = 0;

    private @Nullable ConfigEntryModel draggingSlider;
    private int sliderTrackX, sliderTrackW;

    private @Nullable ConfigEntryModel editingColor;
    private double uiScale = 1.0;

    private final List<RenderedEntry> renderedEntries = new ArrayList<>();
    private final List<SidebarCategorySlot> sidebarSlots = new ArrayList<>();

    private float catPillY = -1f;

    private final Map<String, Float> toggleAnims  = new HashMap<>();
    private final Map<String, Float> sectionAnims = new HashMap<>();
    private final Map<String, Float> hoverAnims   = new HashMap<>();
    private final Map<String, Float> selectSlideAnims = new HashMap<>();
    private final Map<String, Integer> selectSlideDirs = new HashMap<>();
    private double scrollVelocity = 0;
    private float contentFadeAnim = 1f;
    private int lastRenderedCategory = -1;

     private int frameMouseX = 0, frameMouseY = 0;
     private int cachedGx, cachedGy, cachedGw, cachedGh;
     private int cachedSidebarW = SIDEBAR_BASE_W;
     private @Nullable String pendingTooltip;
     private float screenTransition = 0f;
     private boolean closingScreen = false;
     private boolean closeHandled = false;
     private @Nullable Screen closeTarget;
     private long lastTransitionTimeMs = -1L;

    private int sharedThemeIndex = 0;
    private double sharedGuiOpacity = 0.5;
    private boolean sharedAnimationsEnabled = true;
    private double sharedAnimationSpeed = 0.7;
    private boolean sharedOutlineShadow = true;

    @SuppressWarnings("unused")
    public static IQConfigScreen atCategory(@Nullable Screen parent, String categoryId,
                                            Class<?>... configClasses) {
        IQConfigScreen s = new IQConfigScreen(parent, configClasses);
        s.initialCategoryId = categoryId;
        return s;
    }

    private @Nullable String initialCategoryId = null;

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
        if (persistUiState) {
            long savedTimestamp = store.getOrDefault(K_UI_STATE_TIMESTAMP, 0.0).longValue();
            if (System.currentTimeMillis() - savedTimestamp > UI_STATE_MAX_AGE_MS) {
                persistUiState = false;
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
            String savedExpanded = store.getOrDefault(K_EXPANDED_SECTIONS, "");
            if (!savedExpanded.isBlank()) {
                java.util.Set<String> expandedLabels = new java.util.HashSet<>(
                        Arrays.asList(savedExpanded.split(",", -1)));
                for (ConfigCategory cat : categories) {
                    applyExpandedState(cat.entries(), expandedLabels);
                }
            }
        }
        catPillY = -1f;
        toggleAnims.clear();
        sectionAnims.clear();
        hoverAnims.clear();
        selectSlideAnims.clear();
        selectSlideDirs.clear();
        for (ConfigCategory cat : categories) {
            preseedSectionAnims(cat.entries());
        }
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

        updateScrollAnimation();

        int overlayBaseAlpha = (BG_OUTER >>> 24) & 0xFF;
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

    }

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

    private void renderHeader(DrawContext ctx) {
        int x = cachedGx + cachedSidebarW, y = cachedGy, w = cachedGw - cachedSidebarW;

        drawRoundedRect(ctx, x, y, w, HEADER_H, themeHeaderColor(), CORNER_R_MED);

        int acc0 = themeAccentColor();
        int r0 = (acc0 >> 16) & 0xFF, g0 = (acc0 >> 8) & 0xFF, b0 = acc0 & 0xFF;
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

        HeaderSearchBox searchBox = getHeaderSearchBox(x, y, w);
        int sbW = searchBox.w();
        int sbX = searchBox.x();
        int sbY = searchBox.y();

        boolean sbHov = isIn(frameMouseX, frameMouseY, sbX, sbY, sbW, SEARCH_H);
        drawRoundedRect(ctx, sbX, sbY, sbW, SEARCH_H,
                searchFocused ? themeSearchActiveColor() : themeSearchColor(), CORNER_R_SMALL);
        drawRoundedHollowRect(ctx, sbX, sbY, sbW, SEARCH_H,
                searchFocused ? BORDER_BRIGHT : (sbHov ? BORDER_MID : BORDER_DIM));

        int iconY = sbY + (SEARCH_H - client.textRenderer.fontHeight) / 2;
        ctx.drawTextWithShadow(client.textRenderer, Text.literal("⌕"), sbX + 5, iconY, 0xA0E64BA8);
        ctx.drawTextWithShadow(client.textRenderer, Text.literal("⌕"), sbX + 4, iconY, 0xE8FFFFFF);

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

        if (!q.isEmpty()) {
            boolean clrHov = isIn(frameMouseX, frameMouseY, sbX + sbW - 15, sbY, 12, SEARCH_H);
            ctx.drawTextWithShadow(client.textRenderer,
                    Text.literal(clrHov ? "§c✕" : "§8✕"),
                    sbX + sbW - 14, sbY + (SEARCH_H - client.textRenderer.fontHeight) / 2, T_MUTED);
        }

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

    private void renderSidebar(DrawContext ctx) {
        int gx = cachedGx, gy = cachedGy, gh = cachedGh, sidebarW = cachedSidebarW;
        boolean searchActive = isSearchActive();

        drawRoundedRect(ctx, gx, gy, sidebarW, gh, themeSidebarColor(), CORNER_R_MED);
        ctx.fill(gx + sidebarW - 1, gy, gx + sidebarW, gy + gh, 0x22FFFFFF);

        drawRoundedRect(ctx, gx, gy, sidebarW, LOGO_ZONE_H, themeHeaderColor(), CORNER_R_MED);
        ctx.fill(gx, gy + LOGO_ZONE_H - 1, gx + sidebarW, gy + LOGO_ZONE_H, 0x22FFFFFF);

        int lx = gx + 10, ly = gy + (LOGO_ZONE_H - LOGO_SIZE) / 2;

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

    private void renderContent(DrawContext ctx, int cx, int cy, int cw, int ch) {
        if (categories.isEmpty()) return;
        ctx.fill(cx, cy, cx + cw, cy + ch, themePanelColor());

        if (lastRenderedCategory != selectedCategory) {
            if (lastRenderedCategory != -1) contentFadeAnim = 0f;
            lastRenderedCategory = selectedCategory;
        }

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
        if (cachedDisplayEntries != null
                && selectedCategory == cachedDisplayCategory
                && q.equals(cachedDisplayQuery)) {
            return cachedDisplayEntries;
        }

        List<ConfigEntryModel> resolved;
        if (q.isEmpty()) {
            resolved = (selectedCategory >= categories.size())
                    ? List.of()
                    : categories.get(selectedCategory).entries();
        } else {
            List<ConfigEntryModel> results = new ArrayList<>();
            categories.stream()
                    .flatMap(cat -> cat.entries().stream())
                    .forEach(e -> collectSearchResults(e, q, results));
            resolved = List.copyOf(results);
        }

        cachedDisplayCategory = selectedCategory;
        cachedDisplayQuery = q;
        cachedDisplayEntries = resolved;
        return resolved;
    }

    private void collectSearchResults(ConfigEntryModel e, String q, List<ConfigEntryModel> results) {
        if (e.getType() == EntryType.SECTION_HEADER) {
            if (e.getLabel().toLowerCase(Locale.ROOT).contains(q)) {
                results.add(e);
            } else {
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
            case COLOR -> renderColorSwatch(ctx, entry, re, cy, hov);
            case BUTTON -> renderButton(ctx, entry, re, cy, hov);
        }
    }

    private void renderBuildOverlayModeControl(DrawContext ctx, int rx, int cy, boolean hov) {
        int w = MODE_PICKER_W;
        int h = CONTROL_H;
        int x = rx - w;
        int y = cy - h / 2;

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

    private void renderToggle(DrawContext ctx, ConfigEntryModel e, int rx, int cy, boolean hov) {
        try {
            boolean val = (boolean) e.getField().get(null);
            float tTarget = val ? 1f : 0f;
            String tKey   = entryKey(e);
            float tAnim   = toggleAnims.getOrDefault(tKey, tTarget);
            tAnim += (tTarget - tAnim) * 0.22f;
            if (Math.abs(tAnim - tTarget) < 0.004f) tAnim = tTarget;
            toggleAnims.put(tKey, tAnim);

            int x  = rx - TOGGLE_W, y = cy - TOGGLE_H / 2;
            int bg = lerpArgb(themeToggleOffColor(), themeToggleOnColor(), tAnim);

            drawRoundedRect(ctx, x, y, TOGGLE_W, TOGGLE_H, bg, TOGGLE_H / 2);

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

    private void renderActionControl(DrawContext ctx, int x, int y, int w, boolean hov) {
        int top = hov ? themeControlTopHover() : themeControlTop();
        int bottom = hov ? themeControlBottomHover() : themeControlBottom();
        int border = hov ? themedBorderHighlight() : themedBorderMid();

        drawRoundedRect(ctx, x, y, w, CONTROL_H, top, CONTROL_RADIUS);
        ctx.fill(x, y + CONTROL_H / 2, x + w, y + CONTROL_H, bottom);
        drawRoundedHollowRect(ctx, x, y, w, CONTROL_H, border, CONTROL_RADIUS);

        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, 0x2AFFFFFF);
        ctx.fill(x + 1, y + CONTROL_H - 2, x + w - 1, y + CONTROL_H - 1, 0x22000000);
    }

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
            drawHueTrack(ctx, tx, layout.hueY(), tw);
            int hx = tx + (int) (h * tw);
            drawHollowRect(ctx, hx - 2, layout.hueY() - 2, 5, 12, 0xFFFFFFFF);
            ctx.drawTextWithShadow(client.textRenderer, Text.literal("H"), tx - 10, layout.hueY() - 1, themeTextMuted());

            if (editingColor.isHasAlpha()) {
                renderCheckerboard(ctx, tx, layout.alphaY(), tw, 8);
                drawAlphaTrack(ctx, tx, layout.alphaY(), tw, h, s, v);
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

    private void drawHueTrack(DrawContext ctx, int x, int y, int w) {
        final int trackH = 8;
        int step = 2;
        for (int xx = 0; xx < w; xx += step) {
            float hue = xx / (float) Math.max(1, w - 1);
            int c = hsvToArgb(hue, 1f, 1f, 1f);
            ctx.fill(x + xx, y, Math.min(x + xx + step, x + w), y + trackH, c);
        }
        drawHollowRect(ctx, x - 1, y - 1, w + 2, trackH + 2, themedBorderMid());
    }

    private void drawAlphaTrack(DrawContext ctx, int x, int y, int w, float hue, float sat, float val) {
        final int trackH = 8;
        int step = 2;
        for (int xx = 0; xx < w; xx += step) {
            float a = xx / (float) Math.max(1, w - 1);
            int c = hsvToArgb(hue, sat, val, a);
            ctx.fill(x + xx, y, Math.min(x + xx + step, x + w), y + trackH, c);
        }
        drawHollowRect(ctx, x - 1, y - 1, w + 2, trackH + 2, themedBorderMid());
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

    @Override
     public boolean mouseClicked(Click click, boolean doubled) {
         if (closingScreen) return true;
         int imx = (int) click.x(), imy = (int) click.y();

         int headerBtnY = cachedGy + (HEADER_H - HEADER_ACTION_BTN_SIZE) / 2;
         int headerBtnX = getHeaderActionButtonsStartX(cachedGx + cachedSidebarW, cachedGw - cachedSidebarW);
         
         if (editingColor == null && isIn(imx, imy, headerBtnX, headerBtnY, HEADER_ACTION_BTN_SIZE, HEADER_ACTION_BTN_SIZE)) {
             if (client != null) client.setScreen(new net.iqaddons.mod.screen.IQGlobalConfigurationScreen(this, configClasses));
             return true;
         }
         
         int closeBtnX = headerBtnX + HEADER_ACTION_BTN_SIZE + HEADER_ACTION_BTN_GAP;
         if (editingColor == null && isIn(imx, imy, closeBtnX, headerBtnY, HEADER_ACTION_BTN_SIZE, HEADER_ACTION_BTN_SIZE)) {
             close();
             return true;
         }

        if (editingColor != null) {
            handleColorEditorClick(imx, imy);
            return true;
        }

        HeaderSearchBox searchBox = getHeaderSearchBox(cachedGx + cachedSidebarW, cachedGy, cachedGw - cachedSidebarW);
        int sbW = searchBox.w();
        int sbX = searchBox.x();
        int sbY = searchBox.y();
        if (isIn(imx, imy, sbX, sbY, sbW, SEARCH_H)) {
            searchFocused = true;
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
                        PhaseTwoConfig.simpleBuildProgressOverlay = mx < mid;
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
        scrollVelocity -= vAmt * 16;
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (closingScreen) return true;
        if (input.key() == 256) {
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
        if (searchFocused && input.key() == 259 && !searchQuery.isEmpty()) {
            searchQuery.deleteCharAt(searchQuery.length() - 1);
            scrollOffset = 0; return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (closingScreen) return true;
        if (searchFocused) {
            searchQuery.append(input.asString());
            scrollOffset = 0;
            return true;
        }
        return super.charTyped(input);
    }

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
            String key = entryKey(e);
            selectSlideAnims.put(key, 1.0f);
            selectSlideDirs.put(key, -direction);
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
    }

    private int applySharedOpacity(int baseRgb) {
        int alpha = (int) Math.round(Math.max(0.0, Math.min(1.0, sharedGuiOpacity)) * 255.0);
        return (baseRgb & 0x00FFFFFF) | (alpha << 24);
    }

    private int scaleAlphaByOpacity(int color) {
        int existingAlpha = (color >>> 24) & 0xFF;
        int newAlpha = (int) Math.round(existingAlpha * Math.max(0.0, Math.min(1.0, sharedGuiOpacity)));
        return (color & 0x00FFFFFF) | (newAlpha << 24);
    }

    private static final ThemePalette[] THEME_PALETTES = new ThemePalette[] {
            new ThemePalette(
                    0x07060D, 0x080710, BG_HEADER,
                    BG_SEARCH, BG_SEARCH_ACT,
                    BG_ENTRY, BG_ENTRY_HOV, BG_SECTION, BG_CHILD,
                    ACCENT, BG_TOGGLE_ON, BG_TOGGLE_OFF,
                    0xFFFFF7FD, 0xFF9E86AF, 0xFFFFF6FD, 0xFFAA8FBF,
                    0xFFFFECF8, T_ACTION_TEXT_HOV, T_ACCENT,
                    BORDER_DIM, BORDER_MID,
                    SEP_PILL_BG, SEP_PILL_BORDER, SEP_PILL_GLOW, SEP_TEXT,
                    0xEE100B16, 0xFF9E86AF,
                    SEP_LINE,
                    0xFFFFF7FD, 0xFFF2A5DA, 0xFF9B84AD,
                    BG_CONTROL_TOP, BG_CONTROL_TOP_HOV, BG_CONTROL_BOTTOM, BG_CONTROL_BOTTOM_HOV
            ),
            new ThemePalette(
                    0x0D0D0D, 0x121212, 0xD5181818,
                    0xC0131313, 0xD01B1B1B,
                    0xA3181818, 0xC5222222, 0xB0161616, 0xA31A1A1A,
                    0xFFEAEAEA, 0xFFEAEAEA, 0xC22A2A2A,
                    0xFFF5F5F5, 0xFF7D7D7D, 0xFFFFFFFF, 0xFF848484,
                    0xFFCCCCCC, 0xFFFFFFFF, 0xFFDDDDDD,
                    0x40404040, 0x6A8E8E8E,
                    0x6B1A1A1A, 0x30808080, 0x10EAEAEA, 0xFF5A5A5A,
                    0xEE111111, 0xFFD8D8D8,
                    0x282A2A2A,
                    0xFFF0F0F0, 0xFFC8C8C8, 0xFF737373,
                    0xCC1A1A1A, 0xD6222222, 0xCC161616, 0xD61E1E1E
            ),
            new ThemePalette(
                    0xF5F7FA, 0xEBEEF2, 0xD5EBEEF2,
                    0xC0FFFFFF, 0xD6F8F9FA,
                    0xA3FFFFFF, 0xC5F0F3F6, 0xB0EBEEF2, 0xA3F0F3F6,
                    0xFF1F2328, 0xFF1F2328, 0xC2D0D7DE,
                    0xFF1A1530, 0xFF544372, 0xFF1A1A2E, 0xFF63537E,
                    0xFF3A2860, 0xFF1F1050, 0xFF5030A0,
                    0x3A8A78A8, 0x6A7A5FB0,
                    0xB0D4CCE8, 0x506858A8, 0x20BC7FE8, 0xFF5A4878,
                    0xEEF2F5F9, 0xFF3E3260,
                    0x28000000,
                    0xFF1A1530, 0xFF6B3DAA, 0xFF544372,
                    0xCCC7B4DA, 0xD6D6C4E4, 0xCCC2B4D0, 0xD6CDBFD9
            ),
            new ThemePalette(
                    0x07101A, 0x081723, 0xD50C1A29,
                    0xC00F1D2D, 0xD0152940,
                    0xA3132132, 0xC51B2D43, 0xB0142235, 0xA316263A,
                    0xFF4A9EFF, 0xFF3A96E8, 0xC21B2440,
                    0xFFDDF1FF, 0xFF6E9CB8, 0xFFE9F8FF, 0xFF709EB9,
                    0xFFD9F0FF, 0xFFFFFFFF, 0xFF80C8FF,
                    0x30304F72, 0x6A5A9FD9,
                    0x6B0A1A2A, 0x304A9EFF, 0x154A9EFF, 0xFF6A9ACC,
                    0xEE0B1A2A, 0xFFAED7FB,
                    SEP_LINE,
                    0xFFDDF1FF, 0xFF8BCBFF, 0xFF6D95AC,
                    0xCC1B3049, 0xD6283E58, 0xCC162436, 0xD61E2E43
            ),
            new ThemePalette(
                    0x16080B, 0x190B10, 0xD51C0D14,
                    0xC0180D16, 0xD6201320,
                    0xA328151C, 0xC5381E29, 0xB026131C, 0xA3291722,
                    0xFFFF5A78, 0xFFE83A50, 0xC227151F,
                    0xFFFFDEE5, 0xFFA97881, 0xFFFFF1F4, 0xFFAD7D86,
                    0xFFFFE4E8, 0xFFFFFFFF, 0xFFFF9AAA,
                    0x304F2A3A, 0x6AD97A93,
                    0x6B160A12, 0x30FF5A78, 0x15FF5A78, 0xFFB06070,
                    0xEE1D0D14, 0xFFDDA3AE,
                    SEP_LINE,
                    0xFFFFDEE5, 0xFFFF9CAC, 0xFF9E6E78,
                    0xCC3A1C2A, 0xD64B2534, 0xCC22121C, 0xD62E1824
            ),
            new ThemePalette(
                    0x06110B, 0x081710, 0xD50B1A13,
                    0xC00E1B16, 0xD613281F,
                    0xA313251B, 0xC51C3326, 0xB014241C, 0xA316281F,
                    0xFF4BE08A, 0xFF35C06E, 0xC21A2620,
                    0xFFD4FFE0, 0xFF669A77, 0xFFF3FFF3, 0xFF689B78,
                    0xFFD8FFE8, 0xFFFFFFFF, 0xFF70FF9E,
                    0x30315A42, 0x6A63C78A,
                    0x6B0A160E, 0x304BE08A, 0x154BE08A, 0xFF50A070,
                    0xEE0C1A13, 0xFFA7D8B8,
                    SEP_LINE,
                    0xFFD4FFE0, 0xFF76F3A0, 0xFF5F8F6E,
                    0xCC1D3A2A, 0xD6294A35, 0xCC13241B, 0xD61A3124
            )
    };

    private ThemePalette palette() {
        return themePalette(sharedThemeIndex);
    }

    private static ThemePalette themePalette(int idx) {
        int clamped = Math.max(0, Math.min(THEME_PALETTES.length - 1, idx));
        return THEME_PALETTES[clamped];
    }

    private int themePanelColor() {
        return applySharedOpacity(palette().panelBase());
    }

    private int themeSidebarColor() {
        return applySharedOpacity(palette().sidebarBase());
    }

    private int themeHeaderColor() {
        return scaleAlphaByOpacity(palette().header());
    }

    private int themeSearchColor() {
        return palette().search();
    }

    private int themeSearchActiveColor() {
        return palette().searchActive();
    }

    private int themeEntryColor() {
        return scaleAlphaByOpacity(palette().entry());
    }

    private int themeEntryHoverColor() {
        return scaleAlphaByOpacity(palette().entryHover());
    }

    private int themeSectionColor() {
        return scaleAlphaByOpacity(palette().section());
    }

    private int themeChildColor() {
        return scaleAlphaByOpacity(palette().child());
    }

    private int themeAccentColor() {
        return palette().accent();
    }

    private int themeToggleOnColor() {
        return palette().toggleOn();
    }

    private int themeToggleOffColor() {
        return palette().toggleOff();
    }

    private int themeToggleHandle(float tAnim) {
        if (sharedThemeIndex == 1) return lerpArgb(0xFF6A6A6A, 0xFF0D0D0D, tAnim);
        return TOGGLE_HANDLE;
    }

    private int themeTextMain() {
        return palette().textMain();
    }

    private int themeTextMuted() {
        return palette().textMuted();
    }

    private int themeFeatureTitleColor() {
        return palette().featureTitle();
    }

    private int themeFeatureDescriptionColor() {
        return palette().featureDescription();
    }

    private int themeActionTextColor() {
        return palette().actionText();
    }

    private int themeActionTextHoverColor() {
        return palette().actionTextHover();
    }

    private int themeHubAccentTextColor() {
        return palette().hubAccentText();
    }

    private int themedBorderDim() {
        if (!sharedOutlineShadow) return 0x00000000;
        return palette().borderDim();
    }

    private int themedBorderMid() {
        if (!sharedOutlineShadow) return 0x00000000;
        return palette().borderMid();
    }
    private int themedBorderBright() { return sharedOutlineShadow ? withAlpha(themeAccentColor(), 0xB5) : 0x00000000; }
    private int themedBorderHighlight() { return sharedOutlineShadow ? withAlpha(themeAccentColor(), 0x44) : 0x00000000; }

    private int themeSepPillBg() {
        return palette().sepPillBg();
    }

    private int themeSepPillBorder() {
        return palette().sepPillBorder();
    }

    private int themeSepPillGlow() {
        return palette().sepPillGlow();
    }

    private int themeSepText() {
        return palette().sepText();
    }

    private int themeTooltipBgColor() {
        return palette().tooltipBg();
    }

    private int themeTooltipBorderColor() {
        return withAlpha(themeAccentColor(), sharedOutlineShadow ? 0xB8 : 0x88);
    }

    private int themeTooltipTextColor() {
        return palette().tooltipText();
    }

    private int themeSepLine() {
        return palette().sepLine();
    }

    private int themeSepLineBright() {
        int acc = themeAccentColor();
        return withAlpha(acc, sharedThemeIndex == 2 ? 0x40 : 0x18);
    }

    private int themeSidebarTextActive() {
        return palette().sidebarTextActive();
    }

    private int themeSidebarTextHover() {
        return palette().sidebarTextHover();
    }

    private int themeSidebarTextMuted() {
        return palette().sidebarTextMuted();
    }

    private int themeControlTop() {
        return palette().controlTop();
    }

    private int themeControlTopHover() {
        return palette().controlTopHover();
    }

    private int themeControlBottom() {
        return palette().controlBottom();
    }

    private int themeControlBottomHover() {
        return palette().controlBottomHover();
    }

    private record ThemePalette(
            int panelBase,
            int sidebarBase,
            int header,
            int search,
            int searchActive,
            int entry,
            int entryHover,
            int section,
            int child,
            int accent,
            int toggleOn,
            int toggleOff,
            int textMain,
            int textMuted,
            int featureTitle,
            int featureDescription,
            int actionText,
            int actionTextHover,
            int hubAccentText,
            int borderDim,
            int borderMid,
            int sepPillBg,
            int sepPillBorder,
            int sepPillGlow,
            int sepText,
            int tooltipBg,
            int tooltipText,
            int sepLine,
            int sidebarTextActive,
            int sidebarTextHover,
            int sidebarTextMuted,
            int controlTop,
            int controlTopHover,
            int controlBottom,
            int controlBottomHover
    ) {
    }

    private int controlWidth(@NotNull ConfigEntryModel e) {
        return switch (e.getType()) {
            case BOOLEAN -> isBuildOverlayStyleEntry(e) ? MODE_PICKER_W + 4 : TOGGLE_W + 4;
            case INT_SLIDER, FLOAT_SLIDER, DOUBLE_SLIDER -> SLIDER_W + 30;
            case SELECT -> selectControlWidth(e);
            case COLOR -> SWATCH_W + 4;
            case BUTTON -> buttonControlWidth(e);
            default -> 0;
        };
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

    private List<String> wrapText(String text, int maxWidth) {
        if (client == null) return List.of(text);
        String cacheKey = maxWidth + "\n" + text;
        List<String> cached = wrapTextCache.get(cacheKey);
        if (cached != null) return cached;

        List<String> wrapped = ScreenUiUtil.wrapTextParagraphs(client.textRenderer, text, maxWidth);
        List<String> snapshot = List.copyOf(wrapped);
        if (wrapTextCache.size() >= 512) wrapTextCache.clear();
        wrapTextCache.put(cacheKey, snapshot);
        return snapshot;
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

    private String entryKey(ConfigEntryModel e) {
        if (e.getField() != null)
            return e.getField().getDeclaringClass().getName() + "#" + e.getField().getName();
        return "lbl#" + e.getLabel();
    }

    private String sectionKey(ConfigEntryModel e) {
        return "sec#" + e.getLabel();
    }

    private int lerpArgb(int c0, int c1, float t) {
        return ScreenUiUtil.lerpArgb(c0, c1, t);
    }

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
        ScreenUiUtil.drawRoundedRect(ctx, x, y, w, h, color, radius);
    }

    private void drawRoundedHollowRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        drawRoundedHollowRect(ctx, x, y, w, h, color, CORNER_R_SMALL);
    }

    private void drawRoundedHollowRect(DrawContext ctx, int x, int y, int w, int h, int color, int radius) {
        ScreenUiUtil.drawRoundedHollowRect(ctx, x, y, w, h, color, radius);
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
        int svSize = 118;
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

        ctx.fill(x + 1, y + 1, x + size - 1, y + 2, 0x42FFFFFF);
        ctx.fill(x + 1, y + size - 2, x + size - 1, y + size - 1, 0x3A000000);
        drawHollowRect(ctx, x, y, size, size, border);
    }

    private void drawHollowRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ScreenUiUtil.drawHollowRect(ctx, x, y, w, h, color);
    }

    private boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return ScreenUiUtil.isIn(mx, my, x, y, w, h);
    }

    private static double clamp01(double v) {
        return ScreenUiUtil.clamp01(v);
    }

    private static double clamp(double v, double lo, double hi) {
        return ScreenUiUtil.clamp(v, lo, hi);
    }

    private static int withAlpha(int color, int alpha) {
        return ScreenUiUtil.withAlpha(color, alpha);
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
        store.set(K_EXPANDED_SECTIONS, collectExpandedSectionLabels());
        store.set(K_UI_STATE_TIMESTAMP, (double) System.currentTimeMillis());
    }

    private String collectExpandedSectionLabels() {
        List<String> expanded = new ArrayList<>();
        for (ConfigCategory cat : categories) {
            collectExpandedFromEntries(cat.entries(), expanded);
        }
        return String.join(",", expanded);
    }

    private void collectExpandedFromEntries(List<ConfigEntryModel> entries, List<String> out) {
        for (ConfigEntryModel e : entries) {
            if (e.getType() == EntryType.SECTION_HEADER) {
                if (e.isExpanded()) out.add(e.getLabel());
                if (e.getChildren() != null) collectExpandedFromEntries(e.getChildren(), out);
            }
        }
    }

    private void applyExpandedState(List<ConfigEntryModel> entries, java.util.Set<String> expandedLabels) {
        for (ConfigEntryModel e : entries) {
            if (e.getType() == EntryType.SECTION_HEADER) {
                if (expandedLabels.contains(e.getLabel()) && !e.isExpanded()) {
                    e.toggleExpanded();
                }
                if (e.getChildren() != null) applyExpandedState(e.getChildren(), expandedLabels);
            }
        }
    }

    private void preseedSectionAnims(List<ConfigEntryModel> entries) {
        for (ConfigEntryModel e : entries) {
            if (e.getType() == EntryType.SECTION_HEADER) {
                sectionAnims.put(sectionKey(e), e.isExpanded() ? 1f : 0f);
                if (e.getChildren() != null) preseedSectionAnims(e.getChildren());
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
