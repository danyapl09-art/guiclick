package dev.ddpaura.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ddpaura.DdpauraClient;
import dev.ddpaura.client.gui.clickgui.WinterSnowParticles;
import dev.ddpaura.client.gui.clickgui.components.BezierCurveWidget;
import dev.ddpaura.client.gui.clickgui.components.ConfigsComponent;
import dev.ddpaura.client.gui.clickgui.components.ModernSettingControls;
import dev.ddpaura.client.gui.clickgui.components.Transform3DWidget;
import dev.ddpaura.client.gui.render.RenderUtil;
import dev.ddpaura.client.gui.render.ScissorStack;
import dev.ddpaura.client.gui.clickgui.ThemeManager;
import dev.ddpaura.client.gui.clickgui.ClickGuiTheme;
import dev.ddpaura.client.gui.render.VectorIcons;
import dev.ddpaura.client.render.font.FontEngine;
import dev.ddpaura.module.Category;
import dev.ddpaura.module.Module;
import dev.ddpaura.module.ModuleManager;
import dev.ddpaura.setting.BooleanSetting;
import dev.ddpaura.setting.ColorSetting;
import dev.ddpaura.setting.ModeSetting;
import dev.ddpaura.setting.NumberSetting;
import dev.ddpaura.setting.Setting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Pixel-perfect ClickGUI precisely recreating the design from Image 1 and Image 2.
 * Features Main Modules Grid View with Quick Settings, Advanced Settings with Sub-Tabs,
 * 2-Column Grouped Settings, Bézier Curve Animation Editor, 3D Transform Viewport,
 * and Procedural Vector Icons with zero emoji rendering issues.
 */
public final class ClickGuiScreen extends Screen {

    public enum ViewMode {
        MAIN, ADVANCED
    }

    private static final float WINDOW_WIDTH = 820.0F;
    private static final float WINDOW_HEIGHT = 475.0F;
    private static final float HEADER_HEIGHT = 44.0F;
    private static final float STATUS_BAR_HEIGHT = 26.0F;
    private static final float SIDEBAR_WIDTH = 130.0F;
    private static final float CENTER_WIDTH = 415.0F;

    private static ViewMode viewMode = ViewMode.MAIN;
    private static Category currentCategory = Category.MOVEMENT;
    private static Module selectedModule = null;
    private static String activeSubTab = "Общее";

    private static float windowX = -1.0F;
    private static float windowY = -1.0F;

    private final ModuleManager moduleManager;
    private final WinterSnowParticles snowParticles = new WinterSnowParticles();
    private ConfigsComponent configsComponent;
    private BezierCurveWidget bezierWidget;
    private Transform3DWidget transformWidget;

    private boolean draggingWindow = false;
    private float dragOffsetX = 0.0F;
    private float dragOffsetY = 0.0F;

    private float centerScrollY = 0.0F;
    private float targetCenterScrollY = 0.0F;
    private float maxCenterScrollY = 0.0F;

    private String searchQuery = "";
    private boolean searchFocused = false;

    private Module bindingModule = null;

    private boolean themeDropdownOpen = false;
    private boolean clientInfoModalOpen = false;
    private String toastText = null;
    private long toastStartTime = 0L;
    private int quoteIndex = 0;
    private static final String[] QUOTES = {
            "Лучше, чем вчера",
            "Быстрее ветра",
            "Точность в кликах",
            "Искусство побед",
            "Превосходство",
            "Идеальный контроль"
    };

    public void showToast(String text) {
        this.toastText = text;
        this.toastStartTime = System.currentTimeMillis();
    }

    public ClickGuiScreen(ModuleManager moduleManager) {
        super(new StringTextComponent("ClickGUI"));
        this.moduleManager = moduleManager;
    }

    public ClickGuiScreen(ModuleManager moduleManager, String targetModuleId) {
        this(moduleManager);
        if (targetModuleId != null && !targetModuleId.trim().isEmpty()) {
            Module m = moduleManager.byId(targetModuleId);
            if (m != null) {
                selectedModule = m;
                currentCategory = m.getCategory();
            } else {
                this.searchQuery = targetModuleId;
            }
        }
    }

    @Override
    protected void init() {
        if (windowX < 0.0F || windowY < 0.0F) {
            windowX = Math.max(10.0F, (width - WINDOW_WIDTH) * 0.5F);
            windowY = Math.max(10.0F, (height - WINDOW_HEIGHT) * 0.5F);
        }

        windowX = MathHelper.clamp(windowX, 0.0F, Math.max(0.0F, width - WINDOW_WIDTH));
        windowY = MathHelper.clamp(windowY, 0.0F, Math.max(0.0F, height - WINDOW_HEIGHT));

        if (selectedModule == null) {
            List<Module> inCat = moduleManager.in(currentCategory);
            if (!inCat.isEmpty()) {
                selectedModule = inCat.get(0);
            } else if (!moduleManager.all().isEmpty()) {
                selectedModule = moduleManager.all().get(0);
                currentCategory = selectedModule.getCategory();
            }
        }

        configsComponent = new ConfigsComponent(moduleManager, windowX + SIDEBAR_WIDTH, windowY + HEADER_HEIGHT,
                WINDOW_WIDTH - SIDEBAR_WIDTH, WINDOW_HEIGHT - HEADER_HEIGHT - STATUS_BAR_HEIGHT);

        float rightPanelW = WINDOW_WIDTH - SIDEBAR_WIDTH - CENTER_WIDTH;
        float rightPanelX = windowX + SIDEBAR_WIDTH + CENTER_WIDTH;
        float mainAreaY = windowY + HEADER_HEIGHT + 74.0F;
        bezierWidget = new BezierCurveWidget(rightPanelX, mainAreaY, rightPanelW);
        transformWidget = new Transform3DWidget(rightPanelX, mainAreaY + 150.0F, rightPanelW);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        // 1. Crisp dark ambient backdrop (zero shader blur / zero soap)
        RenderUtil.beginFrame();
        RenderUtil.rect(matrices, 0, 0, width, height, 0xC006090E);
        snowParticles.updateAndRender(matrices, width, height, mouseX, mouseY);

        // Keep window inside screen bounds
        windowX = MathHelper.clamp(windowX, 0.0F, Math.max(0.0F, width - WINDOW_WIDTH));
        windowY = MathHelper.clamp(windowY, 0.0F, Math.max(0.0F, height - WINDOW_HEIGHT));

        // Smooth scroll interpolation
        centerScrollY += (targetCenterScrollY - centerScrollY) * 0.25F;

        // 2. Main Window Container
        RenderUtil.shadow(matrices, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 18.0F);
        RenderUtil.roundedRect(matrices, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 10.0F, 0xF010141B);
        RenderUtil.drawHollowRoundedOutline(matrices, windowX, windowY, WINDOW_WIDTH, WINDOW_HEIGHT, 10.0F, 1.0F, 0x35283442);

        // 3. Top Header Bar
        renderHeader(matrices, mouseX, mouseY);

        // 4. Main View vs Advanced View
        if (viewMode == ViewMode.MAIN) {
            renderMainView(matrices, mouseX, mouseY, partialTicks);
        } else {
            renderAdvancedView(matrices, mouseX, mouseY, partialTicks);
        }

        // 5. Bottom Status Bar
        renderStatusBar(matrices, mouseX, mouseY);

        // 6. Theme Selection Dropdown (if active)
        renderThemeDropdown(matrices, mouseX, mouseY);

        // 7. Floating Dropdown Menu (rendered on top of all windows)
        ModernSettingControls.renderFloatingDropdown(matrices, mouseX, mouseY);

        // 8. Toast notification
        renderToast(matrices);

        // 9. Client Info Modal Dialog (modal on very top)
        renderClientInfoModal(matrices, mouseX, mouseY);

        RenderUtil.endFrame();
    }

    // =========================================================================
    // TOP HEADER
    // =========================================================================
    private void renderHeader(MatrixStack matrices, int mouseX, int mouseY) {
        float hx = windowX;
        float hy = windowY;
        float hw = WINDOW_WIDTH;
        float hh = HEADER_HEIGHT;

        int accent = ThemeManager.getAccent();
        int accentSec = ThemeManager.getAccentSecondary();

        RenderUtil.roundedRect(matrices, hx, hy, hw, hh, 10.0F, 0x50161C24);
        RenderUtil.rect(matrices, hx, hy + hh - 1.0F, hw, 1.0F, 0x202A3645);

        // Emerald Brand Logo (3D Faceted Gem) + Brand Text (Interactive -> Client Info)
        boolean brandHov = mouseX >= hx + 10.0F && mouseX <= hx + 165.0F && mouseY >= hy + 6.0F && mouseY <= hy + 38.0F;
        VectorIcons.drawEmeraldLogo(matrices, hx + 22.0F, hy + 22.0F, 11.0F);
        FontEngine.HEADING_18.drawString(matrices, "Emerald", hx + 38.0F, hy + 10.0F, brandHov ? 0xFFFFFFFF : 0xFFF1F5F9);

        // Version badge
        float badgeX = hx + 38.0F + FontEngine.HEADING_18.getStringWidth("Emerald") + 6.0F;
        float badgeY = hy + 13.0F;
        RenderUtil.roundedRect(matrices, badgeX, badgeY, 34.0F, 13.5F, 3.5F, 0x3000E599);
        RenderUtil.drawHollowRoundedOutline(matrices, badgeX, badgeY, 34.0F, 13.5F, 3.5F, 1.0F, 0x6000E599);
        FontEngine.TAG_10.drawCenteredString(matrices, "v1.8.0", badgeX + 17.0F, badgeY + 2.5F, 0xFF00E599);

        // Subtitle: "Minecraft Client"
        FontEngine.TAG_10.drawString(matrices, "Minecraft Client", hx + 39.0F, hy + 27.0F, 0xFF64748B);

        // Search Bar in Header
        float searchW = 155.0F;
        float searchH = 24.0F;
        float searchX = hx + 180.0F;
        float searchY = hy + 10.0F;

        int searchBg = searchFocused ? 0xF0151C26 : 0xFF121720;
        RenderUtil.roundedRect(matrices, searchX, searchY, searchW, searchH, 5.0F, searchBg);
        RenderUtil.drawHollowRoundedOutline(matrices, searchX, searchY, searchW, searchH, 5.0F, 1.0F,
                searchFocused ? accent : 0x352A3645);

        VectorIcons.drawSearch(matrices, searchX + 11.0F, searchY + 12.0F, 8.0F, 0xFF94A3B8);
        String placeholder = "Поиск модулей...";
        String displaySearch = searchQuery.isEmpty() ? (searchFocused ? "" : placeholder) : searchQuery;
        FontEngine.SMALL_11.drawString(matrices, displaySearch, searchX + 25.0F, searchY + 6.5F,
                searchQuery.isEmpty() ? 0xFF64748B : 0xFFFFFFFF);

        // Right inside search bar: Ctrl + K badge
        float kPillW = 38.0F;
        float kPillH = 14.0F;
        float kPillX = searchX + searchW - kPillW - 5.0F;
        float kPillY = searchY + 5.0F;
        RenderUtil.roundedRect(matrices, kPillX, kPillY, kPillW, kPillH, 3.0F, 0xFF1A232E);
        FontEngine.TAG_10.drawCenteredString(matrices, "Ctrl + K", kPillX + kPillW * 0.5F, kPillY + 2.5F, 0xFF64748B);

        // World Status Pill
        float worldX = searchX + searchW + 12.0F;
        float worldW = 145.0F;
        float worldH = 26.0F;
        float worldY = hy + 9.0F;
        boolean worldHov = mouseX >= worldX && mouseX <= worldX + worldW && mouseY >= worldY && mouseY <= worldY + worldH;

        RenderUtil.roundedRect(matrices, worldX, worldY, worldW, worldH, 5.0F, worldHov ? 0xFF18202A : 0xFF121720);
        RenderUtil.drawHollowRoundedOutline(matrices, worldX, worldY, worldW, worldH, 5.0F, 1.0F, worldHov ? accent : 0x302A3645);

        // 3D Grass Block Icon
        VectorIcons.drawGrassBlock(matrices, worldX + 13.0F, worldY + 13.0F, 7.5F);

        String worldTitle = (minecraft != null && minecraft.getCurrentServer() != null) ? minecraft.getCurrentServer().name : "Одиночная игра";
        if (worldTitle.length() > 15) worldTitle = worldTitle.substring(0, 13) + "..";
        String worldSub = (minecraft != null && minecraft.getCurrentServer() != null) ? minecraft.getCurrentServer().ip : "Локальный мир";
        if (worldSub.length() > 17) worldSub = worldSub.substring(0, 15) + "..";

        FontEngine.SMALL_11.drawString(matrices, worldTitle, worldX + 27.0F, worldY + 4.0F, 0xFFE2E8F0);
        FontEngine.TAG_10.drawString(matrices, worldSub, worldX + 27.0F, worldY + 15.0F, 0xFF64748B);

        // Player Profile Pill
        float playerX = worldX + worldW + 10.0F;
        float playerW = 135.0F;
        float playerH = 26.0F;
        float playerY = hy + 9.0F;
        boolean playerHov = mouseX >= playerX && mouseX <= playerX + playerW && mouseY >= playerY && mouseY <= playerY + playerH;

        RenderUtil.roundedRect(matrices, playerX, playerY, playerW, playerH, 5.0F, playerHov ? 0xFF18202A : 0xFF121720);
        RenderUtil.drawHollowRoundedOutline(matrices, playerX, playerY, playerW, playerH, 5.0F, 1.0F, playerHov ? accent : 0x302A3645);

        // Player Avatar Icon
        VectorIcons.drawPlayerAvatar(matrices, playerX + 6.0F, playerY + 5.0F, 16.0F);

        String playerName = (minecraft != null && minecraft.player != null) ? minecraft.player.getScoreboardName() :
                ((minecraft != null && minecraft.getUser() != null) ? minecraft.getUser().getName() : "Player");
        if (playerName.length() > 13) playerName = playerName.substring(0, 11) + "..";

        FontEngine.SMALL_11.drawString(matrices, playerName, playerX + 27.0F, playerY + 4.0F, 0xFFE2E8F0);

        // Green dot + "В сети"
        RenderUtil.circle(matrices, playerX + 31.0F, playerY + 18.5F, 2.0F, 0xFF00E599);
        FontEngine.TAG_10.drawString(matrices, "В сети", playerX + 36.0F, playerY + 15.0F, 0xFF00E599);

        // Window Minimize Button (Dash)
        float minX = hx + hw - 48.0F;
        float minY = hy + 13.0F;
        boolean minHov = mouseX >= minX - 3.0F && mouseX <= minX + 17.0F && mouseY >= minY - 3.0F && mouseY <= minY + 17.0F;
        if (minHov) {
            RenderUtil.roundedRect(matrices, minX - 3.0F, minY - 3.0F, 18.0F, 18.0F, 3.5F, 0x30FFFFFF);
        }
        RenderUtil.rect(matrices, minX, minY + 6.0F, 11.0F, 1.8F, minHov ? 0xFFFFFFFF : 0xFF94A3B8);

        // Window Close Button (✕)
        float closeX = hx + hw - 26.0F;
        float closeY = hy + 13.0F;
        boolean closeHovered = mouseX >= closeX - 3.0F && mouseX <= closeX + 17.0F && mouseY >= closeY - 3.0F && mouseY <= closeY + 17.0F;
        if (closeHovered) {
            RenderUtil.roundedRect(matrices, closeX - 3.0F, closeY - 3.0F, 18.0F, 18.0F, 3.5F, 0x40EF4444);
        }
        VectorIcons.drawClose(matrices, closeX + 5.5F, closeY + 6.0F, 6.5F, closeHovered ? 0xFFEF4444 : 0xFF94A3B8);
    }

    // =========================================================================
    // MAIN VIEW (Image 1)
    // =========================================================================
    private void renderMainView(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        float bodyY = windowY + HEADER_HEIGHT;
        float bodyH = WINDOW_HEIGHT - HEADER_HEIGHT - STATUS_BAR_HEIGHT;

        // 1. Left Sidebar (Categories)
        renderCategoriesSidebar(matrices, mouseX, mouseY, bodyY, bodyH);

        // 2. Configs Tab Mode or Modules Mode
        if (currentCategory == Category.CONFIGS) {
            configsComponent.setX(windowX + SIDEBAR_WIDTH);
            configsComponent.setY(bodyY);
            configsComponent.setWidth(WINDOW_WIDTH - SIDEBAR_WIDTH);
            configsComponent.setHeight(bodyH);
            configsComponent.render(matrices, mouseX, mouseY, partialTicks);
            return;
        }

        // Center Area: Modules Grid (Width: CENTER_WIDTH)
        float centerX = windowX + SIDEBAR_WIDTH;
        renderModulesGrid(matrices, mouseX, mouseY, centerX, bodyY, CENTER_WIDTH, bodyH);

        // Right Area: Quick Settings Pane (Width: WINDOW_WIDTH - SIDEBAR_WIDTH - CENTER_WIDTH)
        float rightX = centerX + CENTER_WIDTH;
        float rightW = WINDOW_WIDTH - SIDEBAR_WIDTH - CENTER_WIDTH;
        renderQuickSettingsPane(matrices, mouseX, mouseY, rightX, bodyY, rightW, bodyH);
    }

    private void renderCategoriesSidebar(MatrixStack matrices, int mouseX, int mouseY, float bodyY, float bodyH) {
        float sx = windowX;
        float sw = SIDEBAR_WIDTH;

        int accent = ThemeManager.getAccent();
        int accentSec = ThemeManager.getAccentSecondary();

        // Divider
        RenderUtil.rect(matrices, sx + sw - 1.0F, bodyY, 1.0F, bodyH, 0x202A3645);

        Category[] categories = Category.values();
        float tabH = 32.0F;
        float tabPad = 5.0F;
        float startY = bodyY + 12.0F;

        for (int i = 0; i < categories.length; i++) {
            Category cat = categories[i];
            float ty = startY + i * (tabH + tabPad);
            boolean isSel = cat == currentCategory;
            boolean hov = mouseX >= sx + 8.0F && mouseX <= sx + sw - 8.0F && mouseY >= ty && mouseY <= ty + tabH;

            float btnX = sx + 8.0F;
            float btnW = sw - 16.0F;

            if (isSel) {
                RenderUtil.roundedRect(matrices, btnX, ty, btnW, tabH, 5.0F, (accent & 0x00FFFFFF) | 0x2A000000);
                RenderUtil.drawGlowingRoundedOutline(matrices, btnX, ty, btnW, tabH, 5.0F, 1.0F, accent, (accent & 0x00FFFFFF) | 0x30000000, 2.0F);
            } else if (hov) {
                RenderUtil.roundedRect(matrices, btnX, ty, btnW, tabH, 5.0F, 0x201E2630);
            }

            int iconCol = isSel ? accent : (hov ? 0xFFFFFFFF : 0xFF94A3B8);
            int textCol = isSel ? 0xFFFFFFFF : (hov ? 0xFFE2E8F0 : 0xFF94A3B8);

            VectorIcons.drawCategoryIcon(matrices, cat, btnX + 14.0F, ty + tabH * 0.5F, 12.0F, iconCol);
            FontEngine.BODY_13.drawString(matrices, cat.getDisplayName(), btnX + 28.0F, ty + 9.5F, textCol);
        }

        // Bottom "PLAY BIGGER" Watermark Card
        float cardH = 50.0F;
        float cardY = bodyY + bodyH - cardH - 12.0F;
        float cardX = sx + 8.0F;
        float cardW = sw - 16.0F;

        RenderUtil.roundedRect(matrices, cardX, cardY, cardW, cardH, 5.0F, 0x400C1117);
        RenderUtil.drawHollowRoundedOutline(matrices, cardX, cardY, cardW, cardH, 5.0F, 1.0F, 0x202A3645);

        FontEngine.HEADING_18.drawString(matrices, "PLAY", cardX + 10.0F, cardY + 7.0F, 0xFF64748B);
        FontEngine.HEADING_18.drawString(matrices, "BIGGER", cardX + 10.0F, cardY + 23.0F, 0xFF00E599);
        RenderUtil.circle(matrices, cardX + cardW - 12.0F, cardY + cardH - 12.0F, 2.5F, 0xFF00E599);
    }

    private void renderModulesGrid(MatrixStack matrices, int mouseX, int mouseY,
                                   float cx, float cy, float cw, float ch) {
        int accent = ThemeManager.getAccent();
        int accentSec = ThemeManager.getAccentSecondary();

        // Category Header
        float headY = cy + 12.0F;

        // Spaced category slug (e.g. M O V E M E N T)
        FontEngine.TAG_10.drawString(matrices, getCategorySlug(currentCategory), cx + 16.0F, headY, accent);

        // Big Title
        FontEngine.HEADING_18.drawString(matrices, getCategoryTitle(currentCategory), cx + 16.0F, headY + 11.0F, 0xFFFFFFFF);

        // Count Pill Badge
        List<Module> activeModules = getFilteredModules();
        String countText = activeModules.size() + " модулей";
        float countW = FontEngine.TAG_10.getStringWidth(countText) + 14.0F;
        float countX = cx + cw - countW - 16.0F;
        float countY = headY + 12.0F;
        RenderUtil.roundedRect(matrices, countX, countY, countW, 15.0F, 3.5F, 0xFF141C25);
        RenderUtil.drawHollowRoundedOutline(matrices, countX, countY, countW, 15.0F, 3.5F, 1.0F, 0x302A3645);
        FontEngine.TAG_10.drawCenteredString(matrices, countText, countX + countW * 0.5F, countY + 3.0F, 0xFF94A3B8);

        // Subtitle
        FontEngine.SMALL_11.drawString(matrices, getCategorySubtitle(currentCategory), cx + 16.0F, headY + 28.0F, 0xFF64748B);

        // Modules Cards Grid (2-Column)
        float gridY = cy + 46.0F;
        float gridH = ch - 52.0F;
        float cardW = (cw - 32.0F - 10.0F) * 0.5F;
        float cardH = 78.0F;
        float cardGap = 10.0F;

        int totalRows = (int) Math.ceil(activeModules.size() / 2.0D);
        float totalContentH = totalRows * (cardH + cardGap);
        maxCenterScrollY = Math.max(0.0F, totalContentH - gridH);

        try (ScissorStack.Scope ignored = ScissorStack.push(minecraft, cx + 8.0F, gridY, cw - 16.0F, gridH)) {
            for (int i = 0; i < activeModules.size(); i++) {
                Module mod = activeModules.get(i);
                int col = i % 2;
                int row = i / 2;

                float cardX = cx + 16.0F + col * (cardW + cardGap);
                float cardY = gridY + centerScrollY + row * (cardH + cardGap);

                if (cardY + cardH < gridY || cardY > gridY + gridH) {
                    continue;
                }

                boolean isSel = (mod == selectedModule);
                boolean hov = mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH;

                // Card Container
                int cardBg = isSel ? 0xF0141B24 : (hov ? 0xF016202A : 0xEE11161E);
                RenderUtil.roundedRect(matrices, cardX, cardY, cardW, cardH, 6.0F, cardBg);

                if (isSel) {
                    RenderUtil.drawGlowingRoundedOutline(matrices, cardX, cardY, cardW, cardH, 6.0F, 1.2F, accent, (accent & 0x00FFFFFF) | 0x35000000, 2.0F);
                } else {
                    RenderUtil.drawHollowRoundedOutline(matrices, cardX, cardY, cardW, cardH, 6.0F, 1.0F, 0x252A3645);
                }

                // Row 1: Module Vector Icon in container + Name + Switch
                float iconBoxSize = 20.0F;
                float iconBoxX = cardX + 8.0F;
                float iconBoxY = cardY + 8.0F;
                RenderUtil.roundedRect(matrices, iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 4.0F, 0xFF0B211A);
                RenderUtil.drawHollowRoundedOutline(matrices, iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 4.0F, 1.0F, 0x3500E599);
                VectorIcons.drawModuleIcon(matrices, mod, iconBoxX + iconBoxSize * 0.5F, iconBoxY + iconBoxSize * 0.5F, 10.0F, mod.isEnabled() ? accent : 0xFF00E599);

                FontEngine.BODY_13.drawString(matrices, mod.getName(), cardX + 34.0F, cardY + 11.5F, 0xFFFFFFFF);

                // Module Switch (iOS style)
                float swW = 24.0F;
                float swH = 13.0F;
                float swX = cardX + cardW - swW - 8.0F;
                float swY = cardY + 9.5F;
                int trackBg = mod.isEnabled() ? accent : 0xFF252E38;
                RenderUtil.roundedRect(matrices, swX, swY, swW, swH, swH * 0.5F, trackBg);
                float knobX = mod.isEnabled() ? (swX + swW - swH * 0.5F) : (swX + swH * 0.5F);
                RenderUtil.circle(matrices, knobX, swY + swH * 0.5F, (swH - 3.5F) * 0.5F, 0xFFFFFFFF);

                // Row 2: Russian Description
                String ruDesc = ModuleLocalization.getRussianDescription(mod);
                List<String> lines = wrapText(ruDesc, cardW - 16.0F);
                for (int l = 0; l < Math.min(2, lines.size()); l++) {
                    FontEngine.SMALL_11.drawString(matrices, lines.get(l), cardX + 8.0F, cardY + 32.0F + l * 10.5F, 0xFF8A99AD);
                }

                // Row 3: Bottom action row: [ R ] Bind and Pencil Icon
                float botY = cardY + cardH - 18.0F;

                // Bind pill
                String keyText = (bindingModule == mod) ? "[...]" : (mod.getKey() > 0 ? "[" + mod.getKeyName() + "]" : "[ None ]");
                float keyPillW = FontEngine.TAG_10.getStringWidth(keyText) + 8.0F;
                float keyPillX = cardX + 8.0F;
                RenderUtil.roundedRect(matrices, keyPillX, botY, keyPillW, 13.0F, 3.0F, 0xFF18202A);
                FontEngine.TAG_10.drawCenteredString(matrices, keyText, keyPillX + keyPillW * 0.5F, botY + 2.0F, (bindingModule == mod) ? accent : 0xFFCBD5E1);

                // Pencil icon beside keybind
                float pencilX = keyPillX + keyPillW + 6.0F;
                float pencilY = botY + 6.5F;
                boolean pencilHov = mouseX >= pencilX - 4.0F && mouseX <= pencilX + 8.0F && mouseY >= botY && mouseY <= botY + 13.0F;
                VectorIcons.drawPencil(matrices, pencilX, pencilY, 6.0F, pencilHov ? accent : 0xFF64748B);

                // Chevron right on far right
                float arrowX = cardX + cardW - 14.0F;
                VectorIcons.drawChevronRight(matrices, arrowX, botY + 6.5F, 5.0F, 1.2F, 0xFF475569);
            }
        }
    }

    private String getCategorySlug(Category cat) {
        String name = cat.name();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            sb.append(name.charAt(i));
            if (i < name.length() - 1) sb.append(' ');
        }
        return sb.toString();
    }

    private String getCategoryTitle(Category cat) {
        switch (cat) {
            case COMBAT: return "Боевые модули и ассистенты";
            case MOVEMENT: return "Модули для перемещения и скорости";
            case RENDER: return "Визуальные эффекты и интерфейс";
            case PLAYER: return "Управление игроком и инвентарем";
            case MISC: return "Вспомогательные утилиты";
            case CONFIGS: return "Конфигурации и профили";
            default: return cat.getDisplayName();
        }
    }

    private String getCategorySubtitle(Category cat) {
        switch (cat) {
            case COMBAT: return "Преимущество в сражениях, критические удары и защита.";
            case MOVEMENT: return "Быстрое и плавное передвижение по миру.";
            case RENDER: return "Настройка графики, подсветка и визуальный стиль.";
            case PLAYER: return "Автоматизация инвентаря, броня и выживание.";
            case MISC: return "Дополнительные функции и оптимизация клиента.";
            case CONFIGS: return "Сохранение, импорт и экспорт ваших настроек.";
            default: return cat.getDescription();
        }
    }

    private void renderQuickSettingsPane(MatrixStack matrices, int mouseX, int mouseY,
                                         float rx, float ry, float rw, float rh) {
        int accent = ThemeManager.getAccent();
        int accentSec = ThemeManager.getAccentSecondary();

        // Divider on left
        RenderUtil.rect(matrices, rx, ry, 1.0F, rh, 0x202A3645);

        if (selectedModule == null) {
            FontEngine.BODY_13.drawCenteredString(matrices, "Выберите модуль", rx + rw * 0.5F, ry + rh * 0.4F, 0xFF64748B);
            return;
        }

        // 1. Spaced Tag: Н А С Т Р О Й К А   М О Д У Л Я
        float tagY = ry + 12.0F;
        FontEngine.TAG_10.drawString(matrices, "Н А С Т Р О Й К А   М О Д У Л Я", rx + 14.0F, tagY, 0xFF64748B);

        // 2. Hero Banner: Large Icon Box + Name + Status + Master Switch
        float heroY = tagY + 16.0F;
        float heroBoxSize = 34.0F;
        RenderUtil.roundedRect(matrices, rx + 14.0F, heroY, heroBoxSize, heroBoxSize, 6.0F, 0xFF0C231D);
        RenderUtil.drawHollowRoundedOutline(matrices, rx + 14.0F, heroY, heroBoxSize, heroBoxSize, 6.0F, 1.0F, 0x6000E599);
        VectorIcons.drawModuleIcon(matrices, selectedModule, rx + 14.0F + heroBoxSize * 0.5F, heroY + heroBoxSize * 0.5F, 16.0F, accent);

        FontEngine.HEADING_18.drawString(matrices, selectedModule.getName(), rx + 56.0F, heroY + 2.0F, 0xFFFFFFFF);

        int statusCol = selectedModule.isEnabled() ? accent : 0xFF64748B;
        RenderUtil.circle(matrices, rx + 59.0F, heroY + 23.5F, 2.5F, statusCol);
        FontEngine.SMALL_11.drawString(matrices, selectedModule.isEnabled() ? "Включен" : "Выключен", rx + 66.0F, heroY + 18.0F, statusCol);

        // Master iOS Switch
        float swW = 28.0F;
        float swH = 15.0F;
        float swX = rx + rw - swW - 14.0F;
        float swY = heroY + 8.0F;
        int trackBg = selectedModule.isEnabled() ? accent : 0xFF252E38;
        RenderUtil.roundedRect(matrices, swX, swY, swW, swH, swH * 0.5F, trackBg);
        float knobX = selectedModule.isEnabled() ? (swX + swW - swH * 0.5F) : (swX + swH * 0.5F);
        RenderUtil.circle(matrices, knobX, swY + swH * 0.5F, (swH - 3.5F) * 0.5F, 0xFFFFFFFF);

        // 3. Full Russian Description
        float descY = heroY + heroBoxSize + 10.0F;
        String fullDesc = ModuleLocalization.getRussianDescription(selectedModule);
        List<String> descLines = wrapText(fullDesc, rw - 28.0F);
        for (int i = 0; i < Math.min(3, descLines.size()); i++) {
            FontEngine.SMALL_11.drawString(matrices, descLines.get(i), rx + 14.0F, descY + i * 11.0F, 0xFF94A3B8);
        }

        // 4. Section Header: Sliders Icon + "Настройки" + "Сбросить" (Reset)
        float secY = descY + Math.min(3, descLines.size()) * 11.0F + 12.0F;
        VectorIcons.drawSliders(matrices, rx + 19.0F, secY + 7.0F, 8.5F, accent);
        FontEngine.SUBHEADING_15.drawString(matrices, "Настройки", rx + 28.0F, secY, 0xFFFFFFFF);

        // "Сбросить" button on right
        String resetStr = "Сбросить";
        float resetW = FontEngine.SMALL_11.getStringWidth(resetStr);
        float resetX = rx + rw - resetW - 14.0F;
        boolean resetHov = mouseX >= resetX - 4.0F && mouseX <= resetX + resetW + 4.0F && mouseY >= secY - 2.0F && mouseY <= secY + 14.0F;
        FontEngine.SMALL_11.drawString(matrices, resetStr, resetX, secY + 2.0F, resetHov ? 0xFFEF4444 : 0xFF64748B);

        // 5. Quick Settings Controls List
        float ctrlY = secY + 20.0F;
        float ctrlW = rw - 28.0F;
        float listH = rh - (ctrlY - ry) - 40.0F;

        List<Setting<?>> quickSettings = getQuickSettings(selectedModule);

        try (ScissorStack.Scope ignored = ScissorStack.push(minecraft, rx + 8.0F, ctrlY, rw - 16.0F, listH)) {
            float curY = ctrlY;
            for (Setting<?> s : quickSettings) {
                float h = ModernSettingControls.renderQuickSetting(matrices, selectedModule, s, rx + 14.0F, curY, ctrlW, mouseX, mouseY);
                curY += h + 6.0F;
            }
        }

        // 6. Bottom Action Button: "⚙ Дополнительные настройки >"
        float btnH = 26.0F;
        float btnY = ry + rh - btnH - 10.0F;
        float btnX = rx + 14.0F;
        float btnW = rw - 28.0F;

        boolean btnHov = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        int btnBg = btnHov ? 0xFF1F2937 : 0xFF141A22;
        RenderUtil.roundedRect(matrices, btnX, btnY, btnW, btnH, 5.0F, btnBg);
        RenderUtil.drawHollowRoundedOutline(matrices, btnX, btnY, btnW, btnH, 5.0F, 1.0F, btnHov ? accent : 0x402A3645);

        VectorIcons.drawGear(matrices, btnX + 14.0F, btnY + 13.0F, 4.0F, btnHov ? accent : 0xFFCBD5E1);
        FontEngine.BODY_13.drawString(matrices, "Дополнительные настройки", btnX + 24.0F, btnY + 7.0F, btnHov ? 0xFFFFFFFF : 0xFFCBD5E1);
        VectorIcons.drawChevronRight(matrices, btnX + btnW - 14.0F, btnY + 13.0F, 5.0F, 1.2F, btnHov ? accent : 0xFF94A3B8);
    }

    // =========================================================================
    // ADVANCED VIEW (Image 2)
    // =========================================================================
    private void renderAdvancedView(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        float bodyY = windowY + HEADER_HEIGHT;
        float bodyH = WINDOW_HEIGHT - HEADER_HEIGHT - STATUS_BAR_HEIGHT;

        int accent = ThemeManager.getAccent();
        int accentSec = ThemeManager.getAccentSecondary();

        // 1. Left Sidebar with [<- Назад] and Category Module List
        renderAdvancedSidebar(matrices, mouseX, mouseY, bodyY, bodyH);

        // 2. Center & Right Content Area
        float contentX = windowX + SIDEBAR_WIDTH;
        float contentW = WINDOW_WIDTH - SIDEBAR_WIDTH;

        if (selectedModule == null) return;

        // Header: Vector Icon + "Расширенные настройки" + Module Name + Subtitle
        float headY = bodyY + 12.0F;
        VectorIcons.drawModuleIcon(matrices, selectedModule, contentX + 22.0F, headY + 10.0F, 14.0F, accentSec);
        FontEngine.BOLD_18.drawString(matrices, "Расширенные настройки", contentX + 36.0F, headY, 0xFFFFFFFF);
        FontEngine.BOLD_18.drawString(matrices, selectedModule.getName(),
                contentX + 36.0F + FontEngine.BOLD_18.getStringWidth("Расширенные настройки "), headY, accentSec);
        FontEngine.TINY_12.drawString(matrices, "Тонкая настройка поведения, приоритета и анимации.", contentX + 36.0F, headY + 19.0F, 0xFF94A3B8);

        // Sub-Tabs Bar: [Общее] [Таргетинг] [Ротация] [Анимация] [Редактор]
        List<String> tabs = ModuleConfigSchema.getTabs(selectedModule);
        float tabY = headY + 36.0F;
        float tabH = 22.0F;
        float tabGap = 6.0F;
        float curTabX = contentX + 16.0F;

        for (String tab : tabs) {
            float tw = FontEngine.SMALL_14.getStringWidth(tab) + 24.0F;
            boolean isSel = tab.equalsIgnoreCase(activeSubTab);
            boolean hov = mouseX >= curTabX && mouseX <= curTabX + tw && mouseY >= tabY && mouseY <= tabY + tabH;

            int bg = isSel ? ((accent & 0x00FFFFFF) | 0x40000000) : (hov ? 0x251E2630 : 0xFF161D24);
            RenderUtil.roundedRect(matrices, curTabX, tabY, tw, tabH, 4.0F, bg);
            RenderUtil.drawHollowRoundedOutline(matrices, curTabX, tabY, tw, tabH, 4.0F, 1.0F, isSel ? accent : 0x302A3645);

            int textCol = isSel ? 0xFFFFFFFF : (hov ? 0xFFE2E8F0 : 0xFF94A3B8);
            FontEngine.SMALL_14.drawCenteredString(matrices, tab, curTabX + tw * 0.5F, tabY + 4.5F, textCol);

            curTabX += tw + tabGap;
        }

        // Split remaining space into Center (2-Column Grouped Settings) and Right (Widgets)
        float mainAreaY = tabY + tabH + 10.0F;
        float mainAreaH = bodyH - (mainAreaY - bodyY) - 6.0F;

        boolean showWidgets = "killaura".equalsIgnoreCase(selectedModule.getId())
                || "custom_sword".equalsIgnoreCase(selectedModule.getId())
                || "custom_hand".equalsIgnoreCase(selectedModule.getId())
                || "cosmetics".equalsIgnoreCase(selectedModule.getId())
                || "Анимация".equalsIgnoreCase(activeSubTab)
                || "Редактор".equalsIgnoreCase(activeSubTab);

        float rightWidgetsW = showWidgets ? 216.0F : 0.0F;
        float centerSettingsW = contentW - rightWidgetsW - 28.0F;

        // Render 2-Column Grouped Settings in Center Column
        renderGroupedSettings(matrices, mouseX, mouseY, contentX + 16.0F, mainAreaY, centerSettingsW, mainAreaH);

        // Render Right Widgets (Bézier Curve & 3D Transform) if applicable
        if (showWidgets) {
            float rwX = contentX + 16.0F + centerSettingsW + 12.0F;

            bezierWidget.setX(rwX);
            bezierWidget.setY(mainAreaY);
            bezierWidget.setWidth(rightWidgetsW - 8.0F);
            bezierWidget.setHeight(142.0F);
            bezierWidget.render(matrices, mouseX, mouseY, partialTicks);

            transformWidget.setX(rwX);
            transformWidget.setY(mainAreaY + 150.0F);
            transformWidget.setWidth(rightWidgetsW - 8.0F);
            transformWidget.setHeight(142.0F);
            transformWidget.render(matrices, mouseX, mouseY, partialTicks);
        }
    }

    private void renderAdvancedSidebar(MatrixStack matrices, int mouseX, int mouseY, float bodyY, float bodyH) {
        float sx = windowX;
        float sw = SIDEBAR_WIDTH;

        int accent = ThemeManager.getAccent();
        int accentSec = ThemeManager.getAccentSecondary();

        // Divider
        RenderUtil.rect(matrices, sx + sw - 1.0F, bodyY, 1.0F, bodyH, 0x202A3645);

        // [<- Назад] Button
        float backX = sx + 8.0F;
        float backY = bodyY + 10.0F;
        float backW = sw - 16.0F;
        float backH = 24.0F;

        boolean backHov = mouseX >= backX && mouseX <= backX + backW && mouseY >= backY && mouseY <= backY + backH;
        int backBg = backHov ? 0x402A3645 : 0xFF181E25;
        RenderUtil.roundedRect(matrices, backX, backY, backW, backH, 4.0F, backBg);
        RenderUtil.drawHollowRoundedOutline(matrices, backX, backY, backW, backH, 4.0F, 1.0F, backHov ? accent : 0x302A3645);

        VectorIcons.drawArrowLeft(matrices, backX + 16.0F, backY + 12.0F, 9.0F, 1.4F, backHov ? 0xFFFFFFFF : 0xFFCBD5E1);
        FontEngine.BOLD_12.drawString(matrices, "Назад", backX + 26.0F, backY + 6.0F, backHov ? 0xFFFFFFFF : 0xFFCBD5E1);

        // Category Label
        float catLabelY = backY + backH + 12.0F;
        FontEngine.TINY_12.drawString(matrices, currentCategory.getDisplayName(), sx + 12.0F, catLabelY, 0xFF64748B);

        // Vertical List of Modules under this category
        List<Module> inCat = moduleManager.in(currentCategory);
        float itemY = catLabelY + 14.0F;
        float itemH = 34.0F;
        float itemGap = 4.0F;

        for (Module m : inCat) {
            boolean isSel = m == selectedModule;
            boolean hov = mouseX >= backX && mouseX <= backX + backW && mouseY >= itemY && mouseY <= itemY + itemH;

            if (isSel) {
                RenderUtil.roundedRect(matrices, backX, itemY, backW, itemH, 4.0F, (accent & 0x00FFFFFF) | 0x30000000);
                RenderUtil.drawHollowRoundedOutline(matrices, backX, itemY, backW, itemH, 4.0F, 1.0F, accent);
            } else if (hov) {
                RenderUtil.roundedRect(matrices, backX, itemY, backW, itemH, 4.0F, 0x201E2630);
            }

            VectorIcons.drawModuleIcon(matrices, m, backX + 14.0F, itemY + 14.0F, 11.0F, isSel ? accentSec : 0xFF94A3B8);
            FontEngine.BOLD_12.drawString(matrices, m.getName(), backX + 26.0F, itemY + 6.0F, isSel ? 0xFFFFFFFF : 0xFFCBD5E1);
            if (isSel) {
                FontEngine.TINY_12.drawString(matrices, "Настройка модуля", backX + 8.0F, itemY + 20.0F, accent);
            }

            itemY += itemH + itemGap;
        }

        // Bottom text
        float botY = bodyY + bodyH - 34.0F;
        FontEngine.TINY_12.drawString(matrices, "Больше", sx + 12.0F, botY, 0xFF64748B);
        FontEngine.TINY_12.drawString(matrices, "возможностей.", sx + 12.0F, botY + 10.0F, 0xFF64748B);
        FontEngine.TINY_12.drawString(matrices, "Больше контроля.", sx + 12.0F, botY + 20.0F, 0xFF475569);
    }

    // =========================================================================
    // 2-COLUMN GROUPED SETTINGS (Matching Card 1 & Card 2 in Image 2)
    // =========================================================================
    private void renderGroupedSettings(MatrixStack matrices, int mouseX, int mouseY,
                                       float gx, float gy, float gw, float gh) {
        // Group settings by group name for current activeSubTab
        Map<String, List<Setting<?>>> groups = new LinkedHashMap<>();
        for (Setting<?> s : selectedModule.getSettings()) {
            String tab = ModuleConfigSchema.getSettingTab(selectedModule, s);
            if (tab.equalsIgnoreCase(activeSubTab)) {
                String g = ModuleConfigSchema.getSettingGroup(selectedModule, s);
                groups.computeIfAbsent(g, k -> new ArrayList<>()).add(s);
            }
        }

        if (groups.isEmpty()) {
            groups.computeIfAbsent("Основные параметры", k -> new ArrayList<>()).addAll(selectedModule.getSettings());
        }

        try (ScissorStack.Scope ignored = ScissorStack.push(minecraft, gx - 2.0F, gy, gw + 4.0F, gh)) {
            float curY = gy;

            for (Map.Entry<String, List<Setting<?>>> entry : groups.entrySet()) {
                String groupName = entry.getKey();
                List<Setting<?>> setList = entry.getValue();

                float cardPad = 8.0F;
                float cardInnerW = gw - cardPad * 2.0F;
                float colGap = 12.0F;
                float colW = (cardInnerW - colGap) * 0.5F;

                // Divide into 2 columns: Column 1 (even) and Column 2 (odd)
                List<Setting<?>> col1 = new ArrayList<>();
                List<Setting<?>> col2 = new ArrayList<>();
                for (int i = 0; i < setList.size(); i++) {
                    if (i % 2 == 0) col1.add(setList.get(i));
                    else col2.add(setList.get(i));
                }

                // Compute heights
                float col1H = computeColHeight(col1);
                float col2H = computeColHeight(col2);
                float cardContentH = Math.max(col1H, col2H);
                float groupCardH = cardContentH + 28.0F;

                // Group Card Container
                RenderUtil.roundedRect(matrices, gx, curY, gw, groupCardH, 6.0F, 0xF013171F);
                RenderUtil.drawHollowRoundedOutline(matrices, gx, curY, gw, groupCardH, 6.0F, 1.0F, 0x302A3645);

                // Group Title
                FontEngine.BOLD_12.drawString(matrices, groupName, gx + 10.0F, curY + 8.0F, 0xFFFFFFFF);

                float startContentY = curY + 24.0F;

                // Render Column 1
                float c1Y = startContentY;
                float col1X = gx + 10.0F;
                for (Setting<?> s : col1) {
                    c1Y += renderAdvancedControl(matrices, s, col1X, c1Y, colW, mouseX, mouseY) + 4.0F;
                }

                // Render Column 2
                float c2Y = startContentY;
                float col2X = col1X + colW + colGap;
                for (Setting<?> s : col2) {
                    c2Y += renderAdvancedControl(matrices, s, col2X, c2Y, colW, mouseX, mouseY) + 4.0F;
                }

                curY += groupCardH + 10.0F;
            }
        }
    }

    private float computeColHeight(List<Setting<?>> settings) {
        float h = 0.0F;
        for (Setting<?> s : settings) {
            if (s instanceof BooleanSetting) {
                h += 30.0F;
            } else if (s instanceof NumberSetting) {
                NumberSetting ns = (NumberSetting) s;
                h += ns.isStepper() ? 40.0F : 42.0F;
            } else if (s instanceof ModeSetting) {
                h += 40.0F;
            } else {
                h += 28.0F;
            }
        }
        return h;
    }

    private float renderAdvancedControl(MatrixStack matrices, Setting<?> s, float sx, float sy, float sw, int mx, int my) {
        if (s instanceof NumberSetting) {
            NumberSetting ns = (NumberSetting) s;
            if (ns.isStepper()) {
                return ModernSettingControls.renderAdvancedStepper(matrices, selectedModule, ns, sx, sy, sw, mx, my);
            } else {
                return ModernSettingControls.renderAdvancedSlider(matrices, selectedModule, ns, sx, sy, sw, mx, my);
            }
        } else if (s instanceof BooleanSetting) {
            return ModernSettingControls.renderAdvancedToggle(matrices, selectedModule, (BooleanSetting) s, sx, sy, sw, mx, my);
        } else if (s instanceof ModeSetting) {
            return ModernSettingControls.renderAdvancedDropdown(matrices, selectedModule, (ModeSetting) s, sx, sy, sw, mx, my);
        }
        return 24.0F;
    }

    // =========================================================================
    // BOTTOM STATUS BAR
    // =========================================================================
    private void renderStatusBar(MatrixStack matrices, int mouseX, int mouseY) {
        float bx = windowX;
        float by = windowY + WINDOW_HEIGHT - STATUS_BAR_HEIGHT;
        float bw = WINDOW_WIDTH;
        float bh = STATUS_BAR_HEIGHT;

        int accent = ThemeManager.getAccent();
        int accentSec = ThemeManager.getAccentSecondary();

        RenderUtil.roundedRect(matrices, bx, by, bw, bh, 6.0F, 0xFF0A0E13);
        RenderUtil.rect(matrices, bx, by, bw, 1.0F, 0x202A3645);

        float curX = bx + 14.0F;

        // 1. Game Platform (Grass Block Icon + Minecraft 1.16.5)
        VectorIcons.drawGrassBlock(matrices, curX + 6.0F, by + 13.0F, 6.5F);
        String gameStr = "Minecraft 1.16.5";
        float gameW = FontEngine.TAG_10.getStringWidth(gameStr);
        FontEngine.TAG_10.drawString(matrices, gameStr, curX + 16.0F, by + 8.5F, 0xFF94A3B8);
        curX += gameW + 26.0F;

        // Divider |
        FontEngine.TAG_10.drawString(matrices, "|", curX, by + 8.5F, 0xFF334155);
        curX += 10.0F;

        // 2. Server Status (Globe Icon + Сервер: IP)
        VectorIcons.drawGlobe(matrices, curX + 5.0F, by + 13.0F, 5.5F, 0xFF94A3B8);
        String server = (minecraft != null && minecraft.getCurrentServer() != null) ? minecraft.getCurrentServer().ip : "Одиночный мир";
        String serverStr = "Сервер: " + server;
        float serverW = FontEngine.TAG_10.getStringWidth(serverStr);
        FontEngine.TAG_10.drawString(matrices, serverStr, curX + 14.0F, by + 8.5F, 0xFF94A3B8);
        curX += serverW + 24.0F;

        // Divider |
        FontEngine.TAG_10.drawString(matrices, "|", curX, by + 8.5F, 0xFF334155);
        curX += 10.0F;

        // 3. FPS
        String fps = (minecraft != null && minecraft.fpsString != null) ? minecraft.fpsString.split(" ")[0] : "60";
        String fpsStr = "FPS: " + fps;
        float fpsW = FontEngine.TAG_10.getStringWidth(fpsStr);
        FontEngine.TAG_10.drawString(matrices, fpsStr, curX, by + 8.5F, 0xFF94A3B8);
        curX += fpsW + 14.0F;

        // Divider |
        FontEngine.TAG_10.drawString(matrices, "|", curX, by + 8.5F, 0xFF334155);
        curX += 10.0F;

        // 4. BPS
        FontEngine.TAG_10.drawString(matrices, "BPS: 0.00", curX, by + 8.5F, 0xFF94A3B8);

        // 5. Right Side Slogan: "СОЗДАН ДЛЯ БОЛЬШИХ ВОЗМОЖНОСТЕЙ" + Emerald Accent Gem
        String slogan = "СОЗДАН ДЛЯ БОЛЬШИХ ВОЗМОЖНОСТЕЙ";
        float sloganW = FontEngine.TAG_10.getStringWidth(slogan);
        float sloganX = bx + bw - sloganW - 24.0F;
        FontEngine.TAG_10.drawString(matrices, slogan, sloganX, by + 8.5F, 0xFF64748B);

        // Emerald neon small gem icon
        VectorIcons.drawEmeraldLogo(matrices, bx + bw - 14.0F, by + 13.0F, 5.0F);
    }

    // =========================================================================
    // INPUT & INTERACTION
    // =========================================================================
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 0. Client Info Modal Dialog clicks (modal absorbs all clicks)
        if (clientInfoModalOpen) {
            float mw = 360.0F;
            float mh = 240.0F;
            float mx = (width - mw) * 0.5F;
            float my = (height - mh) * 0.5F;

            // Close button [ ✕ ]
            float closeBtnX = mx + mw - 26.0F;
            float closeBtnY = my + 10.0F;
            if (mouseX >= closeBtnX - 4.0F && mouseX <= closeBtnX + 18.0F && mouseY >= closeBtnY - 4.0F && mouseY <= closeBtnY + 18.0F) {
                clientInfoModalOpen = false;
                return true;
            }

            // Copy info button [ Скопировать инфо ]
            float btnY = my + mh - 36.0F;
            float btn1W = 165.0F;
            float btn1X = mx + 20.0F;
            float btnH = 24.0F;
            if (mouseX >= btn1X && mouseX <= btn1X + btn1W && mouseY >= btnY && mouseY <= btnY + btnH) {
                long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024L * 1024L);
                long totalMem = Runtime.getRuntime().totalMemory() / (1024L * 1024L);
                int enabledCount = 0;
                for (Module m : moduleManager.all()) {
                    if (m.isEnabled()) enabledCount++;
                }
                String server = (minecraft.getCurrentServer() != null) ? minecraft.getCurrentServer().ip : "Singleplayer";
                String fps = (minecraft.fpsString != null) ? minecraft.fpsString.split(" ")[0] : "60";

                StringBuilder sb = new StringBuilder();
                sb.append("=== Emerald Client Information ===\n");
                sb.append("Version: v1.8.0 Release (Forge 36.2.39)\n");
                sb.append("Theme: ").append(ThemeManager.getTheme().getDisplayName()).append("\n");
                sb.append("Modules: ").append(moduleManager.all().size()).append(" total, ").append(enabledCount).append(" active\n");
                sb.append("RAM: ").append(usedMem).append(" MB / ").append(totalMem).append(" MB\n");
                sb.append("FPS: ").append(fps).append("\n");
                sb.append("Server: ").append(server).append("\n");
                minecraft.keyboardHandler.setClipboard(sb.toString());
                showToast("Данные клиента скопированы!");
                return true;
            }

            // Close button [ Закрыть ]
            float btn2W = 135.0F;
            float btn2X = mx + mw - btn2W - 20.0F;
            if (mouseX >= btn2X && mouseX <= btn2X + btn2W && mouseY >= btnY && mouseY <= btnY + btnH) {
                clientInfoModalOpen = false;
                return true;
            }

            // Clicking outside modal closes it
            if (mouseX < mx || mouseX > mx + mw || mouseY < my || mouseY > my + mh) {
                clientInfoModalOpen = false;
                return true;
            }

            // Absorb any other click inside modal
            return true;
        }

        // 1. Handle floating dropdown first
        if (ModernSettingControls.isDropdownOpen()) {
            if (ModernSettingControls.handleDropdownClick(mouseX, mouseY)) {
                return true;
            }
        }

        // 2. Theme Selection Dropdown Click
        if (themeDropdownOpen) {
            float hx = windowX;
            float hy = windowY;
            float searchW = 150.0F;
            float searchX = hx + 168.0F;
            float themeBtnX = searchX + searchW + 10.0F;
            float themeBtnW = 145.0F;
            float themeBtnY = hy + 8.0F;

            ClickGuiTheme[] themes = ClickGuiTheme.values();
            float itemH = 22.0F;
            float dropW = 160.0F;
            float dropH = themes.length * itemH + 8.0F;
            float dropX = themeBtnX;
            float dropY = themeBtnY + 24.0F;

            if (mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= dropY && mouseY <= dropY + dropH) {
                int index = (int) ((mouseY - (dropY + 4.0F)) / itemH);
                if (index >= 0 && index < themes.length) {
                    ThemeManager.setTheme(themes[index]);
                    showToast("Тема: " + themes[index].getDisplayName());
                }
                themeDropdownOpen = false;
                return true;
            }

            themeDropdownOpen = false;
            return true;
        }

        // 3. Minimize button
        float minX = windowX + WINDOW_WIDTH - 48.0F;
        float minY = windowY + 13.0F;
        if (mouseX >= minX - 3.0F && mouseX <= minX + 17.0F && mouseY >= minY - 3.0F && mouseY <= minY + 17.0F) {
            onClose();
            return true;
        }

        // Close button
        float closeX = windowX + WINDOW_WIDTH - 26.0F;
        float closeY = windowY + 13.0F;
        if (mouseX >= closeX - 3.0F && mouseX <= closeX + 17.0F && mouseY >= closeY - 3.0F && mouseY <= closeY + 17.0F) {
            onClose();
            return true;
        }

        // 4. Header Brand Logo + Name Click -> Opens Client Info Modal or cycles theme on right click
        float hx = windowX;
        float hy = windowY;
        if (mouseX >= hx + 10.0F && mouseX <= hx + 165.0F && mouseY >= hy + 6.0F && mouseY <= hy + 38.0F) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                ThemeManager.cycle();
                showToast("Тема: " + ThemeManager.getTheme().getDisplayName());
            } else {
                clientInfoModalOpen = true;
            }
            return true;
        }

        // 5. Search Bar Focus
        float searchW = 155.0F;
        float searchH = 24.0F;
        float searchX = hx + 180.0F;
        float searchY = hy + 10.0F;
        if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }

        // 6. World Status Pill Click
        float worldX = searchX + searchW + 12.0F;
        float worldW = 145.0F;
        float worldH = 26.0F;
        float worldY = hy + 9.0F;
        if (mouseX >= worldX && mouseX <= worldX + worldW && mouseY >= worldY && mouseY <= worldY + worldH) {
            if (minecraft != null && minecraft.getCurrentServer() != null) {
                minecraft.keyboardHandler.setClipboard(minecraft.getCurrentServer().ip);
                showToast("IP скопирован: " + minecraft.getCurrentServer().ip);
            } else {
                showToast("Локальный одиночный мир");
            }
            return true;
        }

        // 7. Player Profile Pill Click
        float playerX = worldX + worldW + 10.0F;
        float playerW = 135.0F;
        float playerH = 26.0F;
        float playerY = hy + 9.0F;
        if (mouseX >= playerX && mouseX <= playerX + playerW && mouseY >= playerY && mouseY <= playerY + playerH) {
            String pName = (minecraft != null && minecraft.player != null) ? minecraft.player.getScoreboardName() : "Player";
            showToast("Игрок: " + pName + " (В сети)");
            return true;
        }

        // 8. Dragging window by header
        if (mouseY >= windowY && mouseY <= windowY + HEADER_HEIGHT && mouseX >= windowX && mouseX <= windowX + WINDOW_WIDTH) {
            draggingWindow = true;
            dragOffsetX = (float) mouseX - windowX;
            dragOffsetY = (float) mouseY - windowY;
            return true;
        }

        float bodyY = windowY + HEADER_HEIGHT;
        float bodyH = WINDOW_HEIGHT - HEADER_HEIGHT - STATUS_BAR_HEIGHT;

        if (viewMode == ViewMode.MAIN) {
            // Category sidebar clicks
            Category[] categories = Category.values();
            float tabH = 32.0F;
            float tabPad = 5.0F;
            float startY = bodyY + 12.0F;

            for (int i = 0; i < categories.length; i++) {
                Category cat = categories[i];
                float ty = startY + i * (tabH + tabPad);
                if (mouseX >= windowX + 8.0F && mouseX <= windowX + SIDEBAR_WIDTH - 8.0F && mouseY >= ty && mouseY <= ty + tabH) {
                    currentCategory = cat;
                    List<Module> inCat = moduleManager.in(cat);
                    if (!inCat.isEmpty()) {
                        selectedModule = inCat.get(0);
                    }
                    targetCenterScrollY = 0.0F;
                    return true;
                }
            }

            if (currentCategory == Category.CONFIGS) {
                return configsComponent.mouseClicked(mouseX, mouseY, button);
            }

            // Center Area: Module cards clicks
            float centerW = CENTER_WIDTH;
            float centerX = windowX + SIDEBAR_WIDTH;
            float gridY = bodyY + 46.0F;
            float cardW = (centerW - 32.0F - 10.0F) * 0.5F;
            float cardH = 78.0F;
            float cardGap = 10.0F;

            List<Module> activeModules = getFilteredModules();
            for (int i = 0; i < activeModules.size(); i++) {
                Module mod = activeModules.get(i);
                int col = i % 2;
                int row = i / 2;

                float cardX = centerX + 16.0F + col * (cardW + cardGap);
                float cardY = gridY + centerScrollY + row * (cardH + cardGap);

                if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH) {
                    // Check switch toggle
                    float swW = 24.0F;
                    float swH = 13.0F;
                    float swX = cardX + cardW - swW - 8.0F;
                    float swY = cardY + 9.5F;
                    if (mouseX >= swX - 3.0F && mouseX <= swX + swW + 3.0F && mouseY >= swY - 3.0F && mouseY <= swY + swH + 3.0F) {
                        mod.toggle();
                        return true;
                    }

                    // Check bind pill or pencil icon
                    float botY = cardY + cardH - 18.0F;
                    String keyText = (bindingModule == mod) ? "[...]" : (mod.getKey() > 0 ? "[" + mod.getKeyName() + "]" : "[ None ]");
                    float keyPillW = FontEngine.TAG_10.getStringWidth(keyText) + 8.0F;
                    float keyPillX = cardX + 8.0F;
                    float pencilX = keyPillX + keyPillW + 6.0F;

                    if ((mouseX >= keyPillX && mouseX <= keyPillX + keyPillW && mouseY >= botY && mouseY <= botY + 13.0F)
                            || (mouseX >= pencilX - 4.0F && mouseX <= pencilX + 10.0F && mouseY >= botY && mouseY <= botY + 13.0F)) {
                        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                            mod.setKey(0);
                            bindingModule = null;
                        } else {
                            bindingModule = (bindingModule == mod) ? null : mod;
                        }
                        return true;
                    }

                    selectedModule = mod;
                    return true;
                }
            }

            // Right Quick Settings clicks
            float rightX = centerX + centerW;
            float rightW = WINDOW_WIDTH - SIDEBAR_WIDTH - centerW;

            if (selectedModule != null) {
                float tagY = bodyY + 12.0F;
                float heroY = tagY + 16.0F;

                // Master Switch click
                float swW = 28.0F;
                float swH = 15.0F;
                float swX = rightX + rightW - swW - 14.0F;
                float swY = heroY + 8.0F;
                if (mouseX >= swX - 3.0F && mouseX <= swX + swW + 3.0F && mouseY >= swY - 3.0F && mouseY <= swY + swH + 3.0F) {
                    selectedModule.toggle();
                    return true;
                }

                // Description height
                float heroBoxSize = 34.0F;
                float descY = heroY + heroBoxSize + 10.0F;
                String fullDesc = ModuleLocalization.getRussianDescription(selectedModule);
                List<String> descLines = wrapText(fullDesc, rightW - 28.0F);
                float secY = descY + Math.min(3, descLines.size()) * 11.0F + 12.0F;

                // "Сбросить" (Reset) button click
                String resetStr = "Сбросить";
                float resetW = FontEngine.SMALL_11.getStringWidth(resetStr);
                float resetX = rightX + rightW - resetW - 14.0F;
                if (mouseX >= resetX - 6.0F && mouseX <= resetX + resetW + 6.0F && mouseY >= secY - 3.0F && mouseY <= secY + 15.0F) {
                    for (Setting<?> s : selectedModule.getSettings()) {
                        s.reset();
                    }
                    showToast("Настройки " + selectedModule.getName() + " сброшены");
                    return true;
                }

                // Additional settings button -> opens Advanced View
                float btnH = 26.0F;
                float btnY = bodyY + bodyH - btnH - 10.0F;
                float btnX = rightX + 14.0F;
                float btnW = rightW - 28.0F;
                if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                    viewMode = ViewMode.ADVANCED;
                    List<String> tabs = ModuleConfigSchema.getTabs(selectedModule);
                    if (!tabs.isEmpty()) activeSubTab = tabs.get(0);
                    return true;
                }

                // Quick Settings items clicks
                float ctrlY = secY + 20.0F;
                float ctrlW = rightW - 28.0F;

                List<Setting<?>> quickSettings = getQuickSettings(selectedModule);
                float curY = ctrlY;
                for (Setting<?> s : quickSettings) {
                    if (ModernSettingControls.handleQuickClick(selectedModule, s, rightX + 14.0F, curY, ctrlW, mouseX, mouseY)) {
                        return true;
                    }
                    if (s instanceof NumberSetting) curY += 34.0F;
                    else if (s instanceof ModeSetting) curY += 44.0F;
                    else curY += 28.0F;
                }
            }
        } else {
            // ADVANCED VIEW CLICKS
            // Back button
            float backX = windowX + 8.0F;
            float backY = bodyY + 10.0F;
            float backW = SIDEBAR_WIDTH - 16.0F;
            float backH = 24.0F;
            if (mouseX >= backX && mouseX <= backX + backW && mouseY >= backY && mouseY <= backY + backH) {
                viewMode = ViewMode.MAIN;
                return true;
            }

            // Left sidebar module list click
            List<Module> inCat = moduleManager.in(currentCategory);
            float itemY = backY + backH + 12.0F + 14.0F;
            float itemH = 34.0F;
            float itemGap = 4.0F;
            for (Module m : inCat) {
                if (mouseX >= backX && mouseX <= backX + backW && mouseY >= itemY && mouseY <= itemY + itemH) {
                    selectedModule = m;
                    List<String> tabs = ModuleConfigSchema.getTabs(selectedModule);
                    if (!tabs.isEmpty()) activeSubTab = tabs.get(0);
                    return true;
                }
                itemY += itemH + itemGap;
            }

            // Sub-tabs clicks
            float contentX = windowX + SIDEBAR_WIDTH;
            float headY = bodyY + 12.0F;
            float tabY = headY + 36.0F;
            float tabH = 22.0F;
            float tabGap = 6.0F;
            float curTabX = contentX + 16.0F;

            List<String> tabs = ModuleConfigSchema.getTabs(selectedModule);
            for (String tab : tabs) {
                float tw = FontEngine.SMALL_14.getStringWidth(tab) + 24.0F;
                if (mouseX >= curTabX && mouseX <= curTabX + tw && mouseY >= tabY && mouseY <= tabY + tabH) {
                    activeSubTab = tab;
                    return true;
                }
                curTabX += tw + tabGap;
            }

            // 2-Column Grouped Settings Clicks in Center Area
            float mainAreaY = tabY + tabH + 10.0F;
            float contentW = WINDOW_WIDTH - SIDEBAR_WIDTH;
            boolean showWidgets = "killaura".equalsIgnoreCase(selectedModule.getId())
                    || "custom_sword".equalsIgnoreCase(selectedModule.getId())
                    || "custom_hand".equalsIgnoreCase(selectedModule.getId())
                    || "cosmetics".equalsIgnoreCase(selectedModule.getId())
                    || "Анимация".equalsIgnoreCase(activeSubTab)
                    || "Редактор".equalsIgnoreCase(activeSubTab);

            float rightWidgetsW = showWidgets ? 216.0F : 0.0F;
            float centerSettingsW = contentW - rightWidgetsW - 28.0F;

            Map<String, List<Setting<?>>> groups = new LinkedHashMap<>();
            for (Setting<?> s : selectedModule.getSettings()) {
                String tab = ModuleConfigSchema.getSettingTab(selectedModule, s);
                if (tab.equalsIgnoreCase(activeSubTab)) {
                    String g = ModuleConfigSchema.getSettingGroup(selectedModule, s);
                    groups.computeIfAbsent(g, k -> new ArrayList<>()).add(s);
                }
            }
            if (groups.isEmpty()) {
                groups.computeIfAbsent("Основные параметры", k -> new ArrayList<>()).addAll(selectedModule.getSettings());
            }

            float gx = contentX + 16.0F;
            float curY = mainAreaY;

            for (Map.Entry<String, List<Setting<?>>> entry : groups.entrySet()) {
                List<Setting<?>> setList = entry.getValue();
                float cardPad = 8.0F;
                float cardInnerW = centerSettingsW - cardPad * 2.0F;
                float colGap = 12.0F;
                float colW = (cardInnerW - colGap) * 0.5F;

                List<Setting<?>> col1 = new ArrayList<>();
                List<Setting<?>> col2 = new ArrayList<>();
                for (int i = 0; i < setList.size(); i++) {
                    if (i % 2 == 0) col1.add(setList.get(i));
                    else col2.add(setList.get(i));
                }

                float startContentY = curY + 24.0F;

                // Check Column 1 clicks
                float c1Y = startContentY;
                float col1X = gx + 10.0F;
                for (Setting<?> s : col1) {
                    if (ModernSettingControls.handleAdvancedClick(selectedModule, s, col1X, c1Y, colW, mouseX, mouseY)) {
                        return true;
                    }
                    c1Y += computeSingleSettingHeight(s) + 4.0F;
                }

                // Check Column 2 clicks
                float c2Y = startContentY;
                float col2X = col1X + colW + colGap;
                for (Setting<?> s : col2) {
                    if (ModernSettingControls.handleAdvancedClick(selectedModule, s, col2X, c2Y, colW, mouseX, mouseY)) {
                        return true;
                    }
                    c2Y += computeSingleSettingHeight(s) + 4.0F;
                }

                float col1H = computeColHeight(col1);
                float col2H = computeColHeight(col2);
                curY += Math.max(col1H, col2H) + 28.0F + 10.0F;
            }

            // Widgets clicks (Bézier & 3D Transform)
            if (showWidgets) {
                if (bezierWidget != null && bezierWidget.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (transformWidget != null && transformWidget.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
        }

        // Status Bar clicks (clickable in both Main and Advanced modes)
        float bx = windowX;
        float by = windowY + WINDOW_HEIGHT - STATUS_BAR_HEIGHT;
        float bw = WINDOW_WIDTH;
        float bh = STATUS_BAR_HEIGHT;

        if (mouseY >= by && mouseY <= by + bh && mouseX >= bx && mouseX <= bx + bw) {
            float curX = bx + 14.0F;

            // Game version click
            String gameStr = "Minecraft 1.16.5";
            float gameW = FontEngine.TAG_10.getStringWidth(gameStr) + 26.0F;
            if (mouseX >= curX && mouseX <= curX + gameW) {
                if (minecraft != null && minecraft.keyboardHandler != null) {
                    minecraft.keyboardHandler.setClipboard("Minecraft 1.16.5 Forge 36.2.39");
                }
                showToast("Платформа: Minecraft 1.16.5 Forge");
                return true;
            }
            curX += gameW + 10.0F;

            // Server click
            String server = (minecraft != null && minecraft.getCurrentServer() != null) ? minecraft.getCurrentServer().ip : "Одиночный мир";
            String serverStr = "Сервер: " + server;
            float serverW = FontEngine.TAG_10.getStringWidth(serverStr) + 24.0F;
            if (mouseX >= curX && mouseX <= curX + serverW) {
                if (minecraft != null && minecraft.getCurrentServer() != null) {
                    minecraft.keyboardHandler.setClipboard(server);
                    showToast("IP сервера скопирован: " + server);
                } else {
                    showToast("Одиночный мир");
                }
                return true;
            }
            curX += serverW + 10.0F;

            // FPS click
            String fps = (minecraft != null && minecraft.fpsString != null) ? minecraft.fpsString.split(" ")[0] : "60";
            String fpsStr = "FPS: " + fps;
            float fpsW = FontEngine.TAG_10.getStringWidth(fpsStr);
            if (mouseX >= curX && mouseX <= curX + fpsW) {
                showToast("Текущий FPS: " + fps);
                return true;
            }

            // Brand / Slogan click on far right -> Opens Telemetry Modal
            if (mouseX >= bx + bw - 220.0F && mouseX <= bx + bw) {
                clientInfoModalOpen = true;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private float computeSingleSettingHeight(Setting<?> s) {
        if (s instanceof BooleanSetting) return 26.0F;
        if (s instanceof NumberSetting) {
            return ((NumberSetting) s).isStepper() ? 36.0F : 38.0F;
        }
        if (s instanceof ModeSetting) return 36.0F;
        return 24.0F;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingWindow = false;
        ModernSettingControls.mouseReleased();
        if (bezierWidget != null) bezierWidget.mouseReleased(mouseX, mouseY, button);
        if (transformWidget != null) transformWidget.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingWindow && button == 0) {
            windowX = MathHelper.clamp((float) mouseX - dragOffsetX, 0.0F, Math.max(0.0F, width - WINDOW_WIDTH));
            windowY = MathHelper.clamp((float) mouseY - dragOffsetY, 0.0F, Math.max(0.0F, height - WINDOW_HEIGHT));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        targetCenterScrollY = MathHelper.clamp(targetCenterScrollY + (float) delta * 26.0F, -maxCenterScrollY, 0.0F);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (clientInfoModalOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                clientInfoModalOpen = false;
                return true;
            }
        }

        if (themeDropdownOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                themeDropdownOpen = false;
                return true;
            }
        }

        // Ctrl + K focuses search bar
        if (keyCode == GLFW.GLFW_KEY_K && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            searchFocused = true;
            return true;
        }

        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                bindingModule.setKey(0);
            } else {
                bindingModule.setKey(keyCode);
            }
            bindingModule = null;
            return true;
        }

        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                }
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (viewMode == ViewMode.ADVANCED) {
                viewMode = ViewMode.MAIN;
                return true;
            }
            onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused) {
            if (codePoint >= 32 && codePoint != 127) {
                searchQuery += codePoint;
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    private List<Module> getFilteredModules() {
        List<Module> inCat = moduleManager.in(currentCategory);
        if (searchQuery.trim().isEmpty()) {
            return inCat;
        }
        String q = searchQuery.toLowerCase(Locale.ROOT);
        List<Module> filtered = new ArrayList<>();
        for (Module m : inCat) {
            if (m.getName().toLowerCase(Locale.ROOT).contains(q)
                    || m.getId().toLowerCase(Locale.ROOT).contains(q)
                    || ModuleLocalization.getRussianName(m).toLowerCase(Locale.ROOT).contains(q)
                    || ModuleLocalization.getRussianDescription(m).toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(m);
            }
        }
        return filtered;
    }

    private List<Setting<?>> getQuickSettings(Module module) {
        List<Setting<?>> quicks = new ArrayList<>();
        if (module == null) return quicks;
        for (Setting<?> s : module.getSettings()) {
            if (ModuleConfigSchema.isQuick(module, s)) {
                quicks.add(s);
            }
        }
        if (quicks.isEmpty()) {
            List<Setting<?>> all = module.getSettings();
            for (int i = 0; i < Math.min(4, all.size()); i++) {
                quicks.add(all.get(i));
            }
        }
        return quicks;
    }

    private List<String> wrapText(String text, float maxW) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            if (cur.length() > 0 && FontEngine.SMALL_11.getStringWidth(cur + " " + w) > maxW) {
                lines.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                if (cur.length() > 0) cur.append(" ");
                cur.append(w);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    private void renderThemeDropdown(MatrixStack matrices, int mouseX, int mouseY) {
        if (!themeDropdownOpen) return;

        float hx = windowX;
        float hy = windowY;
        float searchW = 155.0F;
        float searchX = hx + 180.0F;
        float themeBtnX = searchX;
        float themeBtnY = hy + 38.0F;

        ClickGuiTheme[] themes = ClickGuiTheme.values();
        float itemH = 22.0F;
        float dropW = 160.0F;
        float dropH = themes.length * itemH + 8.0F;
        float dropX = themeBtnX;
        float dropY = themeBtnY;

        int accent = ThemeManager.getAccent();

        // Dropdown card background with crisp border
        RenderUtil.roundedRect(matrices, dropX - 2.0F, dropY - 2.0F, dropW + 4.0F, dropH + 4.0F, 7.0F, 0x50000000);
        RenderUtil.roundedRect(matrices, dropX, dropY, dropW, dropH, 6.0F, 0xFA121720);
        RenderUtil.drawHollowRoundedOutline(matrices, dropX, dropY, dropW, dropH, 6.0F, 1.0F, (accent & 0x00FFFFFF) | 0x80000000);

        for (int i = 0; i < themes.length; i++) {
            ClickGuiTheme theme = themes[i];
            float itemY = dropY + 4.0F + i * itemH;
            boolean hovered = mouseX >= dropX + 4.0F && mouseX <= dropX + dropW - 4.0F && mouseY >= itemY && mouseY <= itemY + itemH;
            boolean active = (theme == ThemeManager.getTheme());

            if (active) {
                RenderUtil.roundedRect(matrices, dropX + 4.0F, itemY, dropW - 8.0F, itemH, 4.0F, (theme.getPrimaryAccent() & 0x00FFFFFF) | 0x30000000);
            } else if (hovered) {
                RenderUtil.roundedRect(matrices, dropX + 4.0F, itemY, dropW - 8.0F, itemH, 4.0F, 0x20FFFFFF);
            }

            // Theme color dot
            RenderUtil.circle(matrices, dropX + 14.0F, itemY + itemH * 0.5F, 3.5F, theme.getPrimaryAccent());
            RenderUtil.circleOutline(matrices, dropX + 14.0F, itemY + itemH * 0.5F, 3.5F, 1.0F, 0xFFFFFFFF);

            // Theme name
            int textColor = active ? 0xFFFFFFFF : (hovered ? 0xFFE2E8F0 : 0xFF94A3B8);
            FontEngine.SMALL_11.drawString(matrices, theme.getDisplayName(), dropX + 24.0F, itemY + 5.5F, textColor);

            // Active checkmark
            if (active) {
                VectorIcons.drawCheckmark(matrices, dropX + dropW - 14.0F, itemY + itemH * 0.5F, 7.0F, theme.getPrimaryAccent());
            }
        }
    }

    private void renderToast(MatrixStack matrices) {
        if (toastText == null || toastText.isEmpty()) return;

        long elapsed = System.currentTimeMillis() - toastStartTime;
        if (elapsed > 2500L) {
            toastText = null;
            return;
        }

        float alpha = 1.0F;
        if (elapsed < 200L) {
            alpha = elapsed / 200.0F;
        } else if (elapsed > 2100L) {
            alpha = (2500L - elapsed) / 400.0F;
        }
        alpha = MathHelper.clamp(alpha, 0.0F, 1.0F);

        int aInt = (int) (alpha * 255.0F);
        if (aInt <= 0) return;

        int accent = ThemeManager.getAccent();
        float tw = FontEngine.BODY_13.getStringWidth(toastText) + 28.0F;
        float th = 24.0F;
        float tx = windowX + (WINDOW_WIDTH - tw) * 0.5F;
        float ty = windowY + WINDOW_HEIGHT - STATUS_BAR_HEIGHT - th - 10.0F;

        int bgColor = ((int) (alpha * 245.0F) << 24) | 0x00131922;
        int outlineColor = ((int) (alpha * 200.0F) << 24) | (accent & 0x00FFFFFF);
        int textColor = (aInt << 24) | 0x00F8FAFC;

        RenderUtil.roundedRect(matrices, tx, ty, tw, th, 6.0F, bgColor);
        RenderUtil.drawHollowRoundedOutline(matrices, tx, ty, tw, th, 6.0F, 1.0F, outlineColor);

        // Accent indicator dot
        RenderUtil.circle(matrices, tx + 12.0F, ty + th * 0.5F, 3.0F, (aInt << 24) | (accent & 0x00FFFFFF));
        FontEngine.BODY_13.drawString(matrices, toastText, tx + 20.0F, ty + 5.5F, textColor);
    }

    private void renderClientInfoModal(MatrixStack matrices, int mouseX, int mouseY) {
        if (!clientInfoModalOpen) return;

        // Semi-transparent backdrop to isolate modal
        RenderUtil.rect(matrices, 0.0F, 0.0F, width, height, 0x8505080E);

        float mw = 360.0F;
        float mh = 240.0F;
        float mx = windowX + (WINDOW_WIDTH - mw) * 0.5F;
        float my = windowY + (WINDOW_HEIGHT - mh) * 0.5F;

        int accent = ThemeManager.getAccent();
        int accentLight = ThemeManager.getAccentLight();

        // Modal shadow & background
        RenderUtil.roundedRect(matrices, mx - 4.0F, my - 4.0F, mw + 8.0F, mh + 8.0F, 12.0F, 0x60000000);
        RenderUtil.roundedRect(matrices, mx, my, mw, mh, 10.0F, 0xF811161F);
        RenderUtil.drawHollowRoundedOutline(matrices, mx, my, mw, mh, 10.0F, 1.2F, (accent & 0x00FFFFFF) | 0x90000000);

        // Modal Header: 3D Emerald Logo + Title
        VectorIcons.drawEmeraldLogo(matrices, mx + 20.0F, my + 17.5F, 10.0F);
        FontEngine.SUBHEADING_15.drawString(matrices, "Emerald Telemetry", mx + 34.0F, my + 10.0F, 0xFFFFFFFF);

        // Close 'X' Button
        float closeBtnX = mx + mw - 22.0F;
        float closeBtnY = my + 10.0F;
        boolean closeHov = mouseX >= closeBtnX - 4.0F && mouseX <= closeBtnX + 18.0F && mouseY >= closeBtnY - 4.0F && mouseY <= closeBtnY + 18.0F;
        if (closeHov) {
            RenderUtil.roundedRect(matrices, closeBtnX - 3.0F, closeBtnY - 2.0F, 16.0F, 16.0F, 3.0F, 0x40EF4444);
        }
        VectorIcons.drawClose(matrices, closeBtnX + 5.0F, closeBtnY + 6.0F, 7.0F, closeHov ? 0xFFEF4444 : 0xFF94A3B8);

        // Header Divider
        RenderUtil.rect(matrices, mx + 14.0F, my + 30.0F, mw - 28.0F, 1.0F, 0x252A3645);

        // Telemetry Data Rows
        float rowX = mx + 20.0F;
        float rowW = mw - 40.0F;
        float rowY = my + 40.0F;
        float rowGap = 18.0F;

        drawInfoRow(matrices, "Версия клиента:", "v1.8.0 Release", rowX, rowY, rowW, accentLight);
        rowY += rowGap;

        drawInfoRow(matrices, "Платформа:", "Minecraft 1.16.5 Forge 36.2.39", rowX, rowY, rowW, 0xFFE2E8F0);
        rowY += rowGap;

        drawInfoRow(matrices, "Текущая тема:", ThemeManager.getTheme().getDisplayName(), rowX, rowY, rowW, accent);
        rowY += rowGap;

        int enabledCount = 0;
        for (Module m : moduleManager.all()) {
            if (m.isEnabled()) enabledCount++;
        }
        drawInfoRow(matrices, "Модули:", enabledCount + " активно / " + moduleManager.all().size() + " всего", rowX, rowY, rowW, 0xFFE2E8F0);
        rowY += rowGap;

        long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024L * 1024L);
        long totalMem = Runtime.getRuntime().totalMemory() / (1024L * 1024L);
        drawInfoRow(matrices, "Память JVM:", usedMem + " MB / " + totalMem + " MB", rowX, rowY, rowW, 0xFFE2E8F0);
        rowY += rowGap;

        String fps = (minecraft != null && minecraft.fpsString != null) ? minecraft.fpsString.split(" ")[0] : "60";
        drawInfoRow(matrices, "Частота кадров:", fps + " FPS", rowX, rowY, rowW, accentLight);
        rowY += rowGap;

        String server = (minecraft != null && minecraft.getCurrentServer() != null) ? minecraft.getCurrentServer().ip : "Одиночная игра / Меню";
        drawInfoRow(matrices, "Сервер:", server, rowX, rowY, rowW, 0xFF94A3B8);

        // Bottom Action Buttons
        float btnY = my + mh - 36.0F;
        float btnH = 24.0F;

        // Button 1: [ Скопировать инфо ]
        float btn1W = 165.0F;
        float btn1X = mx + 20.0F;
        boolean btn1Hov = mouseX >= btn1X && mouseX <= btn1X + btn1W && mouseY >= btnY && mouseY <= btnY + btnH;
        int btn1Bg = btn1Hov ? ((accent & 0x00FFFFFF) | 0x40000000) : ((accent & 0x00FFFFFF) | 0x25000000);
        RenderUtil.roundedRect(matrices, btn1X, btnY, btn1W, btnH, 5.0F, btn1Bg);
        RenderUtil.drawHollowRoundedOutline(matrices, btn1X, btnY, btn1W, btnH, 5.0F, 1.0F, btn1Hov ? accent : ((accent & 0x00FFFFFF) | 0x60000000));
        String b1Text = "Скопировать данные";
        float b1TextW = FontEngine.SMALL_11.getStringWidth(b1Text);
        FontEngine.SMALL_11.drawString(matrices, b1Text, btn1X + (btn1W - b1TextW) * 0.5F, btnY + 6.0F, 0xFFFFFFFF);

        // Button 2: [ Закрыть ]
        float btn2W = 135.0F;
        float btn2X = mx + mw - btn2W - 20.0F;
        boolean btn2Hov = mouseX >= btn2X && mouseX <= btn2X + btn2W && mouseY >= btnY && mouseY <= btnY + btnH;
        int btn2Bg = btn2Hov ? 0xFF2A3645 : 0xFF1C2430;
        RenderUtil.roundedRect(matrices, btn2X, btnY, btn2W, btnH, 5.0F, btn2Bg);
        RenderUtil.drawHollowRoundedOutline(matrices, btn2X, btnY, btn2W, btnH, 5.0F, 1.0F, btn2Hov ? 0xFF94A3B8 : 0x352A3645);
        String b2Text = "Закрыть";
        float b2TextW = FontEngine.SMALL_11.getStringWidth(b2Text);
        FontEngine.SMALL_11.drawString(matrices, b2Text, btn2X + (btn2W - b2TextW) * 0.5F, btnY + 6.0F, btn2Hov ? 0xFFFFFFFF : 0xFF94A3B8);
    }

    private void drawInfoRow(MatrixStack matrices, String label, String value, float rx, float ry, float rw, int valColor) {
        FontEngine.SMALL_11.drawString(matrices, label, rx, ry, 0xFF94A3B8);
        float valW = FontEngine.SMALL_11.getStringWidth(value);
        FontEngine.SMALL_11.drawString(matrices, value, rx + rw - valW, ry, valColor);
    }

    @Override
    public void onClose() {
        ModernSettingControls.clearInteraction();
        DdpauraClient.config().save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
