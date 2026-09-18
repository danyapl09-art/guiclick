package dev.ddpaura.client.gui.clickgui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ddpaura.client.gui.clickgui.Component;
import dev.ddpaura.client.gui.clickgui.WinterTheme;
import dev.ddpaura.client.gui.render.RenderUtil;
import dev.ddpaura.client.render.font.FontEngine;
import dev.ddpaura.setting.BooleanSetting;
import dev.ddpaura.setting.ColorSetting;
import dev.ddpaura.setting.ModeSetting;
import dev.ddpaura.setting.NumberSetting;
import dev.ddpaura.setting.Setting;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;

public abstract class SettingComponent extends Component {
    protected final Setting<?> setting;

    public SettingComponent(Setting<?> setting, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.setting = setting;
    }

    public Setting<?> getSetting() {
        return setting;
    }

    // --- Boolean Setting Component ---
    public static final class BooleanSettingComponent extends SettingComponent {
        private final BooleanSetting boolSetting;
        private float toggleAnim;
        private float hoverAnim;

        public BooleanSettingComponent(BooleanSetting setting, float x, float y, float width, float height) {
            super(setting, x, y, width, height);
            this.boolSetting = setting;
            this.toggleAnim = setting.get() ? 1.0F : 0.0F;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
            boolean hovered = isHovered(mouseX, mouseY);
            toggleAnim += ((boolSetting.get() ? 1.0F : 0.0F) - toggleAnim) * 0.25F;
            hoverAnim += ((hovered ? 1.0F : 0.0F) - hoverAnim) * 0.20F;

            // Setting label
            int textCol = hovered ? WinterTheme.TEXT_WHITE : WinterTheme.TEXT_BODY;
            FontEngine.SMALL_14.drawString(matrices, boolSetting.getName(), x + 8.0F, y + 4.0F, textCol);

            // Toggle switch pill
            float switchW = 26.0F;
            float switchH = 13.0F;
            float switchX = x + width - switchW - 8.0F;
            float switchY = y + (height - switchH) * 0.5F;

            int trackColor = boolSetting.get()
                    ? RenderUtil.withAlpha(WinterTheme.ACCENT_CYAN, (int) (120 + 80 * toggleAnim))
                    : 0x501E293B;

            RenderUtil.roundedRect(matrices, switchX, switchY, switchW, switchH, 6.5F, trackColor);
            RenderUtil.roundedOutline(matrices, switchX, switchY, switchW, switchH, 6.5F, 1.0F,
                    boolSetting.get() ? WinterTheme.ACCENT_CYAN : 0x3064748B, 0);

            // Animated thumb bead
            float thumbRadius = 4.8F;
            float thumbX = switchX + 2.0F + thumbRadius + (switchW - (thumbRadius * 2.0F) - 4.0F) * toggleAnim;
            float thumbY = switchY + switchH * 0.5F;

            if (boolSetting.get()) {
                RenderUtil.circle(matrices, thumbX, thumbY, thumbRadius + 2.0F, WinterTheme.ACCENT_GLOW);
            }
            int thumbColor = boolSetting.get() ? WinterTheme.TEXT_WHITE : 0xFF94A3B8;
            RenderUtil.circle(matrices, thumbX, thumbY, thumbRadius, thumbColor);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && isHovered(mouseX, mouseY)) {
                boolSetting.set(!boolSetting.get());
                return true;
            }
            return false;
        }
    }

    // --- Number Setting Component (Slider with Hovering Value Badge) ---
    public static final class NumberSettingComponent extends SettingComponent {
        private final NumberSetting numSetting;
        private boolean sliding = false;
        private float displayRatio = -1.0F;
        private float hoverAlpha = 0.0F;

        public NumberSettingComponent(NumberSetting setting, float x, float y, float width, float height) {
            super(setting, x, y, width, height);
            this.numSetting = setting;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
            boolean hovered = isHovered(mouseX, mouseY);
            hoverAlpha += ((hovered || sliding ? 1.0F : 0.0F) - hoverAlpha) * 0.22F;

            int labelCol = hovered ? WinterTheme.TEXT_WHITE : WinterTheme.TEXT_BODY;
            FontEngine.SMALL_14.drawString(matrices, numSetting.getName(), x + 8.0F, y + 2.0F, labelCol);

            String valStr = numSetting.getStep() >= 1.0D
                    ? String.format(Locale.ROOT, "%d", (long) numSetting.get().doubleValue())
                    : String.format(Locale.ROOT, "%.2f", numSetting.get().doubleValue());
            float valW = FontEngine.SMALL_14.getStringWidth(valStr);
            FontEngine.SMALL_14.drawString(matrices, valStr, x + width - valW - 8.0F, y + 2.0F, WinterTheme.TEXT_ACCENT);

            // Slider track
            float trackX = x + 8.0F;
            float trackY = y + 14.0F;
            float trackW = width - 16.0F;
            float trackH = 4.5F;

            RenderUtil.roundedRect(matrices, trackX, trackY, trackW, trackH, 2.2F, WinterTheme.BG_SLIDER_TRACK);
            RenderUtil.roundedOutline(matrices, trackX, trackY, trackW, trackH, 2.2F, 1.0F, WinterTheme.BORDER_SUBTLE, 0);

            double min = numSetting.getMin();
            double max = numSetting.getMax();
            double cur = numSetting.get();
            float targetRatio = (float) MathHelper.clamp((cur - min) / Math.max(0.001D, max - min), 0.0D, 1.0D);
            if (displayRatio < 0.0F) {
                displayRatio = targetRatio;
            } else {
                displayRatio += (targetRatio - displayRatio) * 0.28F;
            }

            float fillW = trackW * displayRatio;

            if (fillW > 1.5F) {
                RenderUtil.horizontalGradient(matrices, trackX, trackY, fillW, trackH,
                        WinterTheme.ACCENT_CYAN, WinterTheme.ACCENT_SKY);
            }

            float thumbX = trackX + fillW;
            float thumbY = trackY + trackH * 0.5F;

            if (hoverAlpha > 0.05F) {
                int glowCol = RenderUtil.withAlpha(WinterTheme.ACCENT_CYAN, (int) (hoverAlpha * 120));
                RenderUtil.circle(matrices, thumbX, thumbY, 6.0F, glowCol);
            }
            RenderUtil.circle(matrices, thumbX, thumbY, 3.8F, WinterTheme.TEXT_WHITE);

            // Floating value badge tooltip when sliding or hovered
            if (hoverAlpha > 0.05F) {
                String badgeText = valStr;
                float badgeW = FontEngine.SMALL_14.getStringWidth(badgeText) + 8.0F;
                float badgeH = 13.0F;
                float badgeX = MathHelper.clamp(thumbX - badgeW * 0.5F, trackX, trackX + trackW - badgeW);
                float badgeY = trackY - badgeH - 3.0F;

                int badgeAlpha = (int) (hoverAlpha * 240);
                RenderUtil.shadow(matrices, badgeX, badgeY, badgeW, badgeH, 3.0F);
                RenderUtil.roundedRect(matrices, badgeX, badgeY, badgeW, badgeH, 3.0F,
                        RenderUtil.withAlpha(WinterTheme.BG_WINDOW, badgeAlpha));
                RenderUtil.roundedOutline(matrices, badgeX, badgeY, badgeW, badgeH, 3.0F, 1.0F,
                        RenderUtil.withAlpha(WinterTheme.ACCENT_CYAN, badgeAlpha), 0);
                FontEngine.SMALL_14.drawCenteredString(matrices, badgeText, badgeX + badgeW * 0.5F, badgeY + 2.0F,
                        RenderUtil.withAlpha(WinterTheme.TEXT_WHITE, badgeAlpha));
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && isHovered(mouseX, mouseY)) {
                sliding = true;
                updateValue(mouseX);
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button == 0) {
                sliding = false;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (sliding) {
                updateValue(mouseX);
                return true;
            }
            return false;
        }

        private void updateValue(double mouseX) {
            float trackX = x + 8.0F;
            float trackW = width - 16.0F;
            double pct = MathHelper.clamp((mouseX - trackX) / trackW, 0.0D, 1.0D);
            double min = numSetting.getMin();
            double max = numSetting.getMax();
            double step = numSetting.getStep();
            double rawVal = min + pct * (max - min);
            double steppedVal = Math.round(rawVal / step) * step;
            numSetting.set(MathHelper.clamp(steppedVal, min, max));
        }
    }

    // --- Mode Setting Component ---
    public static final class ModeSettingComponent extends SettingComponent {
        private final ModeSetting modeSetting;
        private float hoverProgress = 0.0F;

        public ModeSettingComponent(ModeSetting setting, float x, float y, float width, float height) {
            super(setting, x, y, width, height);
            this.modeSetting = setting;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
            boolean hovered = isHovered(mouseX, mouseY);
            hoverProgress += ((hovered ? 1.0F : 0.0F) - hoverProgress) * 0.20F;

            int labelCol = hovered ? WinterTheme.TEXT_WHITE : WinterTheme.TEXT_BODY;
            FontEngine.SMALL_14.drawString(matrices, modeSetting.getName(), x + 8.0F, y + 4.0F, labelCol);

            String modeStr = modeSetting.get();
            float modeW = FontEngine.SMALL_14.getStringWidth(modeStr) + 12.0F;
            float pillW = Math.max(40.0F, modeW);
            float pillH = 14.0F;
            float pillX = x + width - pillW - 8.0F;
            float pillY = y + (height - pillH) * 0.5F;

            int pillBg = hovered ? RenderUtil.withAlpha(WinterTheme.ACCENT_CYAN, 70) : 0x301E293B;
            RenderUtil.roundedRect(matrices, pillX, pillY, pillW, pillH, 4.0F, pillBg);
            RenderUtil.roundedOutline(matrices, pillX, pillY, pillW, pillH, 4.0F, 1.0F,
                    hovered ? WinterTheme.ACCENT_CYAN : WinterTheme.BORDER_SUBTLE, 0);

            FontEngine.SMALL_14.drawCenteredString(matrices, modeStr, pillX + pillW * 0.5F, pillY + 3.0F,
                    hovered ? WinterTheme.TEXT_WHITE : WinterTheme.ACCENT_ICE_WHITE);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isHovered(mouseX, mouseY)) {
                if (button == 0) {
                    modeSetting.cycle(1);
                    return true;
                } else if (button == 1) {
                    modeSetting.cycle(-1);
                    return true;
                }
            }
            return false;
        }
    }

    // --- Color Setting Component ---
    public static final class ColorSettingComponent extends SettingComponent {
        private final ColorSetting colorSetting;

        public ColorSettingComponent(ColorSetting setting, float x, float y, float width, float height) {
            super(setting, x, y, width, height);
            this.colorSetting = setting;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
            boolean hovered = isHovered(mouseX, mouseY);
            FontEngine.SMALL_14.drawString(matrices, colorSetting.getName(), x + 8.0F, y + 4.0F,
                    hovered ? WinterTheme.TEXT_WHITE : WinterTheme.TEXT_BODY);

            float chipSize = 13.0F;
            float chipX = x + width - chipSize - 8.0F;
            float chipY = y + (height - chipSize) * 0.5F;

            RenderUtil.roundedRect(matrices, chipX, chipY, chipSize, chipSize, 3.0F, colorSetting.get() | 0xFF000000);
            RenderUtil.roundedOutline(matrices, chipX, chipY, chipSize, chipSize, 3.0F, 1.0F,
                    hovered ? WinterTheme.TEXT_WHITE : WinterTheme.BORDER_CARD_IDLE, 0);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && isHovered(mouseX, mouseY)) {
                int cur = colorSetting.get();
                int next = cur ^ 0x00A0C0E0;
                colorSetting.set(next);
                return true;
            }
            return false;
        }
    }
}