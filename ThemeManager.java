package dev.ddpaura.client.gui.clickgui;

public final class ThemeManager {
    private static ClickGuiTheme currentTheme = ClickGuiTheme.OBSIDIAN_MINT;

    static {
        applyToWinterTheme(currentTheme);
    }

    private ThemeManager() {}

    public static ClickGuiTheme getTheme() {
        return currentTheme;
    }

    public static int getAccent() {
        return currentTheme != null ? currentTheme.accentPrimary : 0xFF10B981;
    }

    public static int getAccentSecondary() {
        return currentTheme != null ? currentTheme.accentSecondary : 0xFF34D399;
    }

    public static int getAccentLight() {
        return currentTheme != null ? currentTheme.accentLight : 0xFFECFDF5;
    }

    public static void setTheme(ClickGuiTheme theme) {
        if (theme == null) return;
        currentTheme = theme;
        applyToWinterTheme(theme);
    }

    public static void cycle() {
        ClickGuiTheme[] themes = ClickGuiTheme.values();
        int nextIdx = (currentTheme.ordinal() + 1) % themes.length;
        setTheme(themes[nextIdx]);
    }

    private static void applyToWinterTheme(ClickGuiTheme t) {
        WinterTheme.ACCENT_CYAN = t.accentPrimary;
        WinterTheme.ACCENT_SKY = t.accentSecondary;
        WinterTheme.ACCENT_ICE_WHITE = t.accentLight;
        WinterTheme.ACCENT_GLOW = (t.accentPrimary & 0x00FFFFFF) | 0x40000000;
        WinterTheme.ACCENT_TOGGLE_ON = t.accentPrimary;

        WinterTheme.BG_WINDOW = t.bgWindow;
        WinterTheme.BG_HEADER = t.bgHeader;
        WinterTheme.BG_SIDEBAR = t.bgSidebar;
        WinterTheme.BG_CARD = t.bgCard;
        WinterTheme.BG_CARD_HOVER = t.bgCardHover;
        WinterTheme.BG_CARD_ACTIVE = t.bgCardActive;
        WinterTheme.BG_SETTING_PANEL = t.bgSettingPanel;
        WinterTheme.BG_SLIDER_TRACK = t.bgSliderTrack;

        WinterTheme.BORDER_WINDOW = t.borderWindow;
        WinterTheme.BORDER_CARD_IDLE = t.borderCardIdle;
        WinterTheme.BORDER_CARD_HOVER = t.borderCardHover;
        WinterTheme.BORDER_CARD_ACTIVE = t.borderCardActive;
        WinterTheme.BORDER_SUBTLE = t.borderSubtle;
        WinterTheme.TEXT_ACCENT = t.accentSecondary;
    }
}
