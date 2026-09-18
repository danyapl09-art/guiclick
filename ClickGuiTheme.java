package dev.ddpaura.client.gui.clickgui;

public enum ClickGuiTheme {
    ONYX_INDIGO(
            "✦ Onyx Violet",
            0xFF6366F1, 0xFF818CF8, 0xFFEEF2FF,
            0xF80B0E14, 0xFC0F121A, 0xF40D1017,
            0xE8141824, 0xF01A2030, 0xF51E2538,
            0xD00E1118, 0x60262E42,
            0x356366F1, 0x252D374D, 0x506366F1, 0x80818CF8, 0x203E4C68,
            0x818CF8, 0x6366F1
    ),
    SLATE_SAPPHIRE(
            "⚡ Cyber Cyan",
            0xFF00E5FF, 0xFF38BDF8, 0xFFE0F7FA,
            0xF8080C14, 0xFC0C101A, 0xF40A0E17,
            0xE80F1622, 0xF0151F30, 0xF51A273D,
            0xD00A0E16, 0x601A2F45,
            0x3500E5FF, 0x251E334D, 0x5000E5FF, 0x8038BDF8, 0x202B4868,
            0x00E5FF, 0x38BDF8
    ),
    OBSIDIAN_MINT(
            "🌿 Emerald Client",
            0xFF00E599, 0xFF10DF8F, 0xFF6EE7B7,
            0xF80B1015, 0xFC0E141D, 0xF40B1017,
            0xF010161F, 0xF4141C27, 0xF6111A24,
            0xD00E141D, 0x601D2838,
            0x3500E599, 0x301E2B3A, 0xFF00E599, 0x8000E599, 0x251C2735,
            0x10DF8F, 0x00E599
    ),
    MIDNIGHT_ROSE(
            "🌸 Crimson Rose",
            0xFFF43F5E, 0xFFFB7185, 0xFFFFF1F2,
            0xF8120A0E, 0xFC170E12, 0xF4140C10,
            0xE81A1016, 0xF0241620, 0xF52E1C28,
            0xD010090E, 0x603A1C2A,
            0x35F43F5E, 0x28381D2C, 0x50F43F5E, 0x80FB7185, 0x224A263A,
            0xFB7185, 0xF43F5E
    ),
    SOLAR_GOLD(
            "🔥 Solar Gold",
            0xFFF59E0B, 0xFFFBBF24, 0xFFFEF3C7,
            0xF8120E0A, 0xFC17120D, 0xF4140F0B,
            0xE81C1610, 0xF0261E16, 0xF530261C,
            0xD0100C08, 0x603C2C19,
            0x35F59E0B, 0x28382718, 0x50F59E0B, 0x80FBBF24, 0x224A351F,
            0xFBBF24, 0xF59E0B
    ),
    TITANIUM_MINIMAL(
            "⚙ Pure Monochrome",
            0xFFFFFFFF, 0xFFCBD5E1, 0xFFFFFFFF,
            0xF80B0B0C, 0xFC111113, 0xF40E0E10,
            0xE8161619, 0xF01E1E22, 0xF526262B,
            0xD00C0C0D, 0x602C2C33,
            0x35FFFFFF, 0x252D2D35, 0x50FFFFFF, 0x80E2E8F0, 0x203D3D48,
            0xCBD5E1, 0xFFFFFF
    );

    private final String displayName;
    public final int accentPrimary;
    public final int accentSecondary;
    public final int accentLight;
    public final int bgWindow;
    public final int bgHeader;
    public final int bgSidebar;
    public final int bgCard;
    public final int bgCardHover;
    public final int bgCardActive;
    public final int bgSettingPanel;
    public final int bgSliderTrack;
    public final int borderWindow;
    public final int borderCardIdle;
    public final int borderCardHover;
    public final int borderCardActive;
    public final int borderSubtle;
    public final int particleColor1;
    public final int particleColor2;

    ClickGuiTheme(String displayName, int accentPrimary, int accentSecondary, int accentLight,
                 int bgWindow, int bgHeader, int bgSidebar, int bgCard, int bgCardHover, int bgCardActive,
                 int bgSettingPanel, int bgSliderTrack,
                 int borderWindow, int borderCardIdle, int borderCardHover, int borderCardActive, int borderSubtle,
                 int particleColor1, int particleColor2) {
        this.displayName = displayName;
        this.accentPrimary = accentPrimary;
        this.accentSecondary = accentSecondary;
        this.accentLight = accentLight;
        this.bgWindow = bgWindow;
        this.bgHeader = bgHeader;
        this.bgSidebar = bgSidebar;
        this.bgCard = bgCard;
        this.bgCardHover = bgCardHover;
        this.bgCardActive = bgCardActive;
        this.bgSettingPanel = bgSettingPanel;
        this.bgSliderTrack = bgSliderTrack;
        this.borderWindow = borderWindow;
        this.borderCardIdle = borderCardIdle;
        this.borderCardHover = borderCardHover;
        this.borderCardActive = borderCardActive;
        this.borderSubtle = borderSubtle;
        this.particleColor1 = particleColor1;
        this.particleColor2 = particleColor2;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getAccent() {
        return accentPrimary;
    }

    public int getPrimaryAccent() {
        return accentPrimary;
    }

    public int getSecondaryAccent() {
        return accentSecondary;
    }

    public int getAccentLight() {
        return accentLight;
    }
}
