package dev.ddpaura.client.gui.clickgui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ddpaura.DdpauraClient;
import dev.ddpaura.client.gui.clickgui.Component;
import dev.ddpaura.client.gui.clickgui.WinterTheme;
import dev.ddpaura.client.gui.render.RenderUtil;
import dev.ddpaura.client.render.font.FontEngine;
import dev.ddpaura.module.Module;
import dev.ddpaura.setting.BooleanSetting;
import dev.ddpaura.setting.ColorSetting;
import dev.ddpaura.setting.ModeSetting;
import dev.ddpaura.setting.NumberSetting;
import dev.ddpaura.setting.Setting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ModuleCardComponent extends Component {
    private static final float HEADER_HEIGHT = 32.0F;

    private final Module module;
    private final List<SettingComponent> settingComponents = new ArrayList<>();

    private boolean expanded = false;
    private boolean binding = false;
    private float toggleAnim = 0.0F;
    private float expandAnim = 0.0F;
    private float hoverAnim = 0.0F;

    public ModuleCardComponent(Module module, float x, float y, float width) {
        super(x, y, width, HEADER_HEIGHT);
        this.module = module;
        this.toggleAnim = module.isEnabled() ? 1.0F : 0.0F;

        for (Setting<?> s : module.getSettings()) {
            if (s instanceof BooleanSetting) {
                settingComponents.add(new SettingComponent.BooleanSettingComponent((BooleanSetting) s, x, y, width, 20.0F));
            } else if (s instanceof NumberSetting) {
                settingComponents.add(new SettingComponent.NumberSettingComponent((NumberSetting) s, x, y, width, 24.0F));
            } else if (s instanceof ModeSetting) {
                settingComponents.add(new SettingComponent.ModeSettingComponent((ModeSetting) s, x, y, width, 22.0F));
            } else if (s instanceof ColorSetting) {
                settingComponents.add(new SettingComponent.ColorSettingComponent((ColorSetting) s, x, y, width, 20.0F));
            }
        }
        updateDimensions();
    }

    public float getSwitchWidth() {
        return 24.0F;
    }

    public float getSwitchHeight() {
        return 13.0F;
    }

    public float getSwitchX() {
        float chevronX = x + width - 16.0F;
        float switchW = getSwitchWidth();
        return settingComponents.isEmpty() ? (x + width - switchW - 8.0F) : (chevronX - switchW - 8.0F);
    }

    public float getSwitchY() {
        return y + (HEADER_HEIGHT - getSwitchHeight()) * 0.5F;
    }

    public boolean isSwitchHovered(double mouseX, double mouseY) {
        float swX = getSwitchX();
        float swY = getSwitchY();
        float swW = getSwitchWidth();
        float swH = getSwitchHeight();
        return mouseX >= swX - 3.5F && mouseX <= swX + swW + 3.5F
                && mouseY >= swY - 3.5F && mouseY <= swY + swH + 3.5F;
    }

    public boolean isKeybindHovered(double mouseX, double mouseY) {
        String keyText = binding ? "[...]" : (module.getKey() > 0 ? "[" + module.getKeyName() + "]" : "");
        if (keyText.isEmpty()) return false;
        float keyW = FontEngine.SMALL_14.getStringWidth(keyText);
        float keyX = getSwitchX() - keyW - 6.0F;
        return mouseX >= keyX - 3.0F && mouseX <= keyX + keyW + 3.0F
                && mouseY >= y + 4.0F && mouseY <= y + HEADER_HEIGHT - 4.0F;
    }

    private void updateDimensions() {
        float settingsH = 0.0F;
        if (expanded || expandAnim > 0.01F) {
            for (SettingComponent sc : settingComponents) {
                settingsH += sc.getHeight() + 2.0F;
            }
            if (settingsH > 0.0F) {
                settingsH += 4.0F;
            }
        }
        this.height = HEADER_HEIGHT + settingsH * expandAnim;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        toggleAnim += ((module.isEnabled() ? 1.0F : 0.0F) - toggleAnim) * 0.22F;
        expandAnim += ((expanded ? 1.0F : 0.0F) - expandAnim) * 0.22F;
        updateDimensions();

        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_HEIGHT;
        boolean switchHovered = isSwitchHovered(mouseX, mouseY);
        hoverAnim += ((hovered ? 1.0F : 0.0F) - hoverAnim) * 0.20F;

        // Card background: deep obsidian/slate with subtle hover brighten
        int bg = module.isEnabled()
                ? WinterTheme.BG_CARD_ACTIVE
                : (hovered ? WinterTheme.BG_CARD_HOVER : WinterTheme.BG_CARD);

        RenderUtil.shadow(matrices, x, y, width, height, 4.0F);
        RenderUtil.roundedRect(matrices, x, y, width, height, 6.0F, bg);

        // Multi-layer outline
        int outlineColor = module.isEnabled()
                ? WinterTheme.BORDER_CARD_ACTIVE
                : (hovered ? WinterTheme.BORDER_CARD_HOVER : WinterTheme.BORDER_CARD_IDLE);

        if (module.isEnabled() || hoverAnim > 0.05F) {
            int bloomCol = RenderUtil.withAlpha(WinterTheme.ACCENT_CYAN, (int) (32 * Math.max(toggleAnim, hoverAnim)));
            RenderUtil.roundedOutline(matrices, x, y, width, height, 6.0F, 1.0F, outlineColor, bloomCol);
        } else {
            RenderUtil.roundedOutline(matrices, x, y, width, height, 6.0F, 1.0F, outlineColor, 0);
        }

        // Active left edge indicator strip
        if (toggleAnim > 0.05F) {
            float barH = (HEADER_HEIGHT - 12.0F) * toggleAnim;
            float barY = y + (HEADER_HEIGHT - barH) * 0.5F;
            RenderUtil.roundedRect(matrices, x + 2.0F, barY, 2.5F, barH, 1.2F, WinterTheme.ACCENT_CYAN);
        }

        // 1. Module Name (Stable X position so it doesn't jump)
        int textColor = module.isEnabled() ? WinterTheme.TEXT_WHITE : (hovered ? WinterTheme.TEXT_WHITE : WinterTheme.TEXT_BODY);
        FontEngine.BOLD_18.drawStringWithShadow(matrices, module.getName(), x + 12.0F, y + 9.0F, textColor);

        // 2. Expand indicator chevron
        float chevronX = x + width - 16.0F;
        if (!settingComponents.isEmpty()) {
            String arrow = expanded ? "▲" : "▼";
            int arrowColor = expanded ? WinterTheme.ACCENT_SKY : (hovered ? WinterTheme.TEXT_WHITE : WinterTheme.TEXT_MUTED);
            FontEngine.SMALL_14.drawString(matrices, arrow, chevronX, y + 10.0F, arrowColor);
        }

        // 3. Modern toggle pill switch (iOS/Linear style)
        float switchW = getSwitchWidth();
        float switchH = getSwitchHeight();
        float switchX = getSwitchX();
        float switchY = getSwitchY();

        int trackColor = module.isEnabled()
                ? RenderUtil.withAlpha(WinterTheme.ACCENT_CYAN, (int) (180 + 60 * toggleAnim))
                : (switchHovered ? 0x70334155 : 0x50334155);
        RenderUtil.roundedRect(matrices, switchX, switchY, switchW, switchH, 6.5F, trackColor);

        int switchOutline = switchHovered
                ? WinterTheme.ACCENT_SKY
                : (module.isEnabled() ? WinterTheme.ACCENT_CYAN : 0x3064748B);
        RenderUtil.roundedOutline(matrices, switchX, switchY, switchW, switchH, 6.5F, 1.0F, switchOutline, 0);

        float thumbR = 4.2F;
        float thumbX = switchX + 2.0F + thumbR + (switchW - (thumbR * 2.0F) - 4.0F) * toggleAnim;
        float thumbY = switchY + switchH * 0.5F;

        if (module.isEnabled()) {
            RenderUtil.circle(matrices, thumbX, thumbY, thumbR + 2.0F, WinterTheme.ACCENT_GLOW);
        }
        int thumbColor = module.isEnabled() ? 0xFFFFFFFF : (switchHovered ? 0xFFCBD5E1 : 0xFF94A3B8);
        RenderUtil.circle(matrices, thumbX, thumbY, thumbR, thumbColor);

        // 4. Keybind badge
        String keyText = binding ? "[...]" : (module.getKey() > 0 ? "[" + module.getKeyName() + "]" : "");
        if (!keyText.isEmpty()) {
            float keyW = FontEngine.SMALL_14.getStringWidth(keyText);
            float keyX = switchX - keyW - 6.0F;
            FontEngine.SMALL_14.drawString(matrices, keyText, keyX, y + 10.0F, binding ? WinterTheme.ACCENT_CYAN : WinterTheme.TEXT_MUTED);
        }

        // 5. Render expanded settings with dedicated recessed background
        if (expandAnim > 0.05F && !settingComponents.isEmpty()) {
            float panelX = x + 4.0F;
            float panelY = y + HEADER_HEIGHT - 1.0F;
            float panelW = width - 8.0F;
            float panelH = Math.max(0.0F, height - HEADER_HEIGHT - 3.0F);

            if (panelH > 4.0F) {
                RenderUtil.roundedRect(matrices, panelX, panelY, panelW, panelH, 4.0F, WinterTheme.BG_SETTING_PANEL);
                RenderUtil.roundedOutline(matrices, panelX, panelY, panelW, panelH, 4.0F, 1.0F, WinterTheme.BORDER_SUBTLE, 0);
            }

            float curY = y + HEADER_HEIGHT + 2.0F;
            for (SettingComponent sc : settingComponents) {
                sc.setX(x);
                sc.setY(curY);
                sc.setWidth(width);
                sc.render(matrices, mouseX, mouseY, partialTicks);
                curY += sc.getHeight() + 2.0F;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (binding) {
            int key = 1000 + button;
            if (button == 0 || button == 1) {
                binding = false;
                return true;
            }
            module.setKey(key);
            binding = false;
            DdpauraClient.config().save();
            return true;
        }

        boolean headerHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEADER_HEIGHT;

        if (headerHovered) {
            if (isSwitchHovered(mouseX, mouseY)) {
                // 1) Toggle module ONLY on switch click
                if (button == 0) {
                    module.toggle();
                    DdpauraClient.config().save();
                    return true;
                }
            } else if (isKeybindHovered(mouseX, mouseY)) {
                if (button == 0 || button == 1) {
                    binding = true;
                    return true;
                }
            } else {
                // 2) Open settings on LMB anywhere on function (outside switch)
                if (button == 0 || button == 1) {
                    if (!settingComponents.isEmpty()) {
                        expanded = !expanded;
                    }
                    return true;
                } else if (button == 2) {
                    binding = true;
                    return true;
                }
            }
        }

        if (expanded && expandAnim > 0.4F) {
            float curY = y + HEADER_HEIGHT + 2.0F;
            for (SettingComponent sc : settingComponents) {
                if (mouseY >= curY && mouseY <= curY + sc.getHeight() + 2.0F) {
                    if (sc.mouseClicked(mouseX, mouseY, button)) {
                        DdpauraClient.config().save();
                        return true;
                    }
                }
                curY += sc.getHeight() + 2.0F;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (expanded) {
            for (SettingComponent sc : settingComponents) {
                sc.mouseReleased(mouseX, mouseY, button);
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (expanded) {
            for (SettingComponent sc : settingComponents) {
                if (sc.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                    DdpauraClient.config().save();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                module.setKey(-1);
            } else {
                module.setKey(keyCode);
            }
            binding = false;
            DdpauraClient.config().save();
            return true;
        }
        return false;
    }

    public Module getModule() { return module; }
}