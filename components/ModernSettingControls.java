package dev.ddpaura.client.gui.clickgui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ddpaura.client.gui.ModuleConfigSchema;
import dev.ddpaura.client.gui.render.RenderUtil;
import dev.ddpaura.client.gui.render.VectorIcons;
import dev.ddpaura.client.render.font.FontEngine;
import dev.ddpaura.module.Module;
import dev.ddpaura.setting.BooleanSetting;
import dev.ddpaura.setting.ColorSetting;
import dev.ddpaura.setting.ModeSetting;
import dev.ddpaura.setting.NumberSetting;
import dev.ddpaura.setting.Setting;
import dev.ddpaura.client.gui.clickgui.ThemeManager;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * Universal Modern Setting Renderer matching both Image 1 (Quick Settings)
 * and Image 2 (Advanced Grouped Settings). Zero emoji fonts, solid smooth geometry.
 */
public final class ModernSettingControls {

    private static Setting<?> activeDraggingSlider = null;
    private static ModeSetting openDropdown = null;
    private static float dropdownMenuX = 0;
    private static float dropdownMenuY = 0;
    private static float dropdownMenuW = 0;

    public static void clearInteraction() {
        activeDraggingSlider = null;
        openDropdown = null;
    }

    public static boolean isDropdownOpen() {
        return openDropdown != null;
    }

    public static Setting<?> getActiveDraggingSlider() {
        return activeDraggingSlider;
    }

    // ==========================================
    // QUICK SETTINGS RENDERERS (Right Panel in Image 1)
    // ==========================================

    public static float renderQuickSetting(MatrixStack matrices, Module module, Setting<?> setting,
                                           float sx, float sy, float sw, int mouseX, int mouseY) {
        String name = ModuleConfigSchema.getSettingName(module, setting);
        String unit = ModuleConfigSchema.getSettingUnit(module, setting);

        if (setting instanceof NumberSetting) {
            NumberSetting ns = (NumberSetting) setting;
            float sh = 28.0F;

            // Label on left, formatted value on right
            FontEngine.BODY_13.drawString(matrices, name, sx, sy, 0xFFCBD5E1);

            String valStr = formatNumber(ns.get()) + (unit.isEmpty() ? "" : " " + unit);
            float valW = FontEngine.SMALL_11.getStringWidth(valStr);
            FontEngine.SMALL_11.drawString(matrices, valStr, sx + sw - valW, sy + 1.0F, 0xFF94A3B8);

            // Slider track
            float trackY = sy + 14.0F;
            float trackH = 3.5F;
            RenderUtil.roundedRect(matrices, sx, trackY, sw, trackH, 1.75F, 0xFF242E3B);

            double norm = ns.normalized();
            RenderUtil.roundedRect(matrices, sx, trackY, (float) (sw * norm), trackH, 1.75F, ThemeManager.getAccent());

            float thumbX = sx + (float) (sw * norm);
            RenderUtil.circle(matrices, thumbX, trackY + trackH * 0.5F, 4.5F, 0xFFFFFFFF);

            if (activeDraggingSlider == ns) {
                double newNorm = MathHelper.clamp((mouseX - sx) / (double) sw, 0.0D, 1.0D);
                ns.setNormalized(newNorm);
            }
            return sh;
        } else if (setting instanceof ModeSetting) {
            ModeSetting ms = (ModeSetting) setting;
            float sh = 38.0F;

            FontEngine.BODY_13.drawString(matrices, name, sx, sy, 0xFFCBD5E1);

            float dropY = sy + 14.0F;
            float dropH = 20.0F;
            boolean hov = mouseX >= sx && mouseX <= sx + sw && mouseY >= dropY && mouseY <= dropY + dropH;

            RenderUtil.roundedRect(matrices, sx, dropY, sw, dropH, 4.0F, 0xFF181E24);
            RenderUtil.drawHollowRoundedOutline(matrices, sx, dropY, sw, dropH, 4.0F, 1.0F, hov ? ThemeManager.getAccent() : 0x402A3645);

            FontEngine.SMALL_11.drawString(matrices, ms.get(), sx + 8.0F, dropY + 4.5F, 0xFFFFFFFF);
            VectorIcons.drawChevronDown(matrices, sx + sw - 12.0F, dropY + dropH * 0.5F, 5.5F, 1.3F, 0xFF94A3B8);

            return sh;
        } else if (setting instanceof BooleanSetting) {
            BooleanSetting bs = (BooleanSetting) setting;
            float sh = 22.0F;

            FontEngine.BODY_13.drawString(matrices, name, sx, sy + 1.5F, 0xFFCBD5E1);

            float togW = 24.0F;
            float togH = 13.0F;
            float togX = sx + sw - togW;
            float togY = sy + 2.0F;

            int trackBg = bs.get() ? ThemeManager.getAccent() : 0xFF28323D;
            RenderUtil.roundedRect(matrices, togX, togY, togW, togH, togH * 0.5F, trackBg);
            float knobX = bs.get() ? (togX + togW - togH * 0.5F) : (togX + togH * 0.5F);
            RenderUtil.circle(matrices, knobX, togY + togH * 0.5F, (togH - 3.5F) * 0.5F, 0xFFFFFFFF);

            return sh;
        } else if (setting instanceof ColorSetting) {
            ColorSetting cs = (ColorSetting) setting;
            float sh = 22.0F;

            FontEngine.BODY_13.drawString(matrices, name, sx, sy + 1.5F, 0xFFCBD5E1);

            float colorBoxW = 22.0F;
            float colorBoxH = 12.0F;
            float colorBoxX = sx + sw - colorBoxW;
            float colorBoxY = sy + 2.5F;
            RenderUtil.roundedRect(matrices, colorBoxX, colorBoxY, colorBoxW, colorBoxH, 3.0F, cs.get());
            RenderUtil.drawHollowRoundedOutline(matrices, colorBoxX, colorBoxY, colorBoxW, colorBoxH, 3.0F, 1.0F, 0xFF2A3645);

            return sh;
        }

        return 20.0F;
    }

    public static boolean handleQuickClick(Module module, Setting<?> setting, float sx, float sy, float sw, double mouseX, double mouseY) {
        if (setting instanceof NumberSetting) {
            NumberSetting ns = (NumberSetting) setting;
            float trackY = sy + 14.0F;
            if (mouseX >= sx - 4.0F && mouseX <= sx + sw + 4.0F && mouseY >= trackY - 5.0F && mouseY <= trackY + 10.0F) {
                activeDraggingSlider = ns;
                double newNorm = MathHelper.clamp((mouseX - sx) / (double) sw, 0.0D, 1.0D);
                ns.setNormalized(newNorm);
                return true;
            }
        } else if (setting instanceof ModeSetting) {
            ModeSetting ms = (ModeSetting) setting;
            float dropY = sy + 14.0F;
            float dropH = 20.0F;
            if (mouseX >= sx && mouseX <= sx + sw && mouseY >= dropY && mouseY <= dropY + dropH) {
                openDropdown = (openDropdown == ms) ? null : ms;
                dropdownMenuX = sx;
                dropdownMenuY = dropY + dropH + 2.0F;
                dropdownMenuW = sw;
                return true;
            }
        } else if (setting instanceof BooleanSetting) {
            BooleanSetting bs = (BooleanSetting) setting;
            if (mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + 18.0F) {
                bs.toggle();
                return true;
            }
        } else if (setting instanceof ColorSetting) {
            ColorSetting cs = (ColorSetting) setting;
            if (mouseX >= sx + sw - 24.0F && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + 18.0F) {
                int[] presets = {0xFF22C55E, 0xFF00E5FF, 0xFF3B82F6, 0xFFA855F7, 0xFFEC4899, 0xFFEAB308, 0xFFEF4444};
                int cur = cs.get();
                int next = presets[0];
                for (int i = 0; i < presets.length; i++) {
                    if (presets[i] == cur) {
                        next = presets[(i + 1) % presets.length];
                        break;
                    }
                }
                cs.set(next);
                return true;
            }
        }
        return false;
    }

    // ==========================================
    // ADVANCED SETTINGS RENDERERS (Image 2)
    // ==========================================

    public static float renderAdvancedSlider(MatrixStack matrices, Module module, NumberSetting ns,
                                             float sx, float sy, float sw, int mouseX, int mouseY) {
        float sh = 38.0F;
        String name = ModuleConfigSchema.getSettingName(module, ns);
        String desc = ModuleConfigSchema.getSettingDescription(module, ns);
        String unit = ModuleConfigSchema.getSettingUnit(module, ns);

        // Title on left
        FontEngine.BODY_13.drawString(matrices, name, sx, sy, 0xFFE2E8F0);

        // Value on right
        String valStr = formatNumber(ns.get()) + (unit.isEmpty() ? "" : " " + unit);
        float valW = FontEngine.SMALL_11.getStringWidth(valStr);
        FontEngine.SMALL_11.drawString(matrices, valStr, sx + sw - valW, sy, 0xFFFFFFFF);

        // Slider track
        float trackY = sy + 14.0F;
        float trackH = 3.5F;
        RenderUtil.roundedRect(matrices, sx, trackY, sw, trackH, 1.75F, 0xFF242E3B);

        double norm = ns.normalized();
        RenderUtil.roundedRect(matrices, sx, trackY, (float) (sw * norm), trackH, 1.75F, ThemeManager.getAccent());

        float thumbX = sx + (float) (sw * norm);
        RenderUtil.circle(matrices, thumbX, trackY + trackH * 0.5F, 4.5F, 0xFFFFFFFF);

        // Subtitle: Description on left, Min - Max range on right
        float subY = trackY + 7.0F;
        FontEngine.SMALL_11.drawString(matrices, desc, sx, subY, 0xFF64748B);

        String rangeStr = formatNumber(ns.getMin()) + " – " + formatNumber(ns.getMax());
        float rangeW = FontEngine.SMALL_11.getStringWidth(rangeStr);
        FontEngine.SMALL_11.drawString(matrices, rangeStr, sx + sw - rangeW, subY, 0xFF64748B);

        if (activeDraggingSlider == ns) {
            double newNorm = MathHelper.clamp((mouseX - sx) / (double) sw, 0.0D, 1.0D);
            ns.setNormalized(newNorm);
        }

        return sh;
    }

    public static float renderAdvancedStepper(MatrixStack matrices, Module module, NumberSetting ns,
                                              float sx, float sy, float sw, int mouseX, int mouseY) {
        float sh = 36.0F;
        String name = ModuleConfigSchema.getSettingName(module, ns);
        String unit = ModuleConfigSchema.getSettingUnit(module, ns);

        FontEngine.BODY_13.drawString(matrices, name, sx, sy, 0xFFE2E8F0);

        float boxY = sy + 13.0F;
        float boxH = 19.0F;
        RenderUtil.roundedRect(matrices, sx, boxY, sw, boxH, 3.5F, 0xFF181E24);
        RenderUtil.drawHollowRoundedOutline(matrices, sx, boxY, sw, boxH, 3.5F, 1.0F, 0x402A3645);

        String valStr = formatNumber(ns.get()) + (unit.isEmpty() ? "" : " " + unit);
        FontEngine.SMALL_11.drawString(matrices, valStr, sx + 8.0F, boxY + 4.5F, 0xFFFFFFFF);

        // Up & Down arrow buttons on right
        float arrowX = sx + sw - 12.0F;
        boolean upHov = mouseX >= arrowX - 4.0F && mouseX <= arrowX + 8.0F && mouseY >= boxY && mouseY <= boxY + 9.5F;
        boolean downHov = mouseX >= arrowX - 4.0F && mouseX <= arrowX + 8.0F && mouseY >= boxY + 9.5F && mouseY <= boxY + 19.0F;

        VectorIcons.drawArrowUp(matrices, arrowX, boxY + 5.0F, 5.0F, upHov ? ThemeManager.getAccent() : 0xFF64748B);
        VectorIcons.drawArrowDown(matrices, arrowX, boxY + 13.5F, 5.0F, downHov ? ThemeManager.getAccent() : 0xFF64748B);

        return sh;
    }

    public static float renderAdvancedToggle(MatrixStack matrices, Module module, BooleanSetting bs,
                                             float sx, float sy, float sw, int mouseX, int mouseY) {
        float sh = 26.0F;
        String name = ModuleConfigSchema.getSettingName(module, bs);
        String desc = ModuleConfigSchema.getSettingDescription(module, bs);

        // Title and Description
        FontEngine.BODY_13.drawString(matrices, name, sx, sy, 0xFFE2E8F0);
        FontEngine.SMALL_11.drawString(matrices, desc, sx, sy + 11.5F, 0xFF64748B);

        // Switch on right
        float swW = 24.0F;
        float swH = 13.0F;
        float swX = sx + sw - swW;
        float swY = sy + 2.0F;

        int trackBg = bs.get() ? ThemeManager.getAccent() : 0xFF28323D;
        RenderUtil.roundedRect(matrices, swX, swY, swW, swH, swH * 0.5F, trackBg);
        float knobX = bs.get() ? (swX + swW - swH * 0.5F) : (swX + swH * 0.5F);
        RenderUtil.circle(matrices, knobX, swY + swH * 0.5F, (swH - 3.5F) * 0.5F, 0xFFFFFFFF);

        return sh;
    }

    public static float renderAdvancedDropdown(MatrixStack matrices, Module module, ModeSetting ms,
                                               float sx, float sy, float sw, int mouseX, int mouseY) {
        float sh = 36.0F;
        String name = ModuleConfigSchema.getSettingName(module, ms);

        FontEngine.BODY_13.drawString(matrices, name, sx, sy, 0xFFE2E8F0);

        float dropY = sy + 13.0F;
        float dropH = 19.0F;
        boolean hov = mouseX >= sx && mouseX <= sx + sw && mouseY >= dropY && mouseY <= dropY + dropH;

        RenderUtil.roundedRect(matrices, sx, dropY, sw, dropH, 3.5F, 0xFF181E24);
        RenderUtil.drawHollowRoundedOutline(matrices, sx, dropY, sw, dropH, 3.5F, 1.0F, hov ? ThemeManager.getAccent() : 0x402A3645);

        FontEngine.SMALL_11.drawString(matrices, ms.get(), sx + 8.0F, dropY + 4.5F, 0xFFFFFFFF);
        VectorIcons.drawChevronDown(matrices, sx + sw - 12.0F, dropY + dropH * 0.5F, 6.0F, 1.4F, 0xFF94A3B8);

        return sh;
    }

    public static boolean handleAdvancedClick(Module module, Setting<?> setting, float sx, float sy, float sw, double mouseX, double mouseY) {
        if (setting instanceof NumberSetting) {
            NumberSetting ns = (NumberSetting) setting;
            if (ns.isStepper()) {
                float boxY = sy + 13.0F;
                float boxH = 19.0F;
                float arrowX = sx + sw - 12.0F;
                if (mouseX >= arrowX - 5.0F && mouseX <= arrowX + 10.0F && mouseY >= boxY && mouseY <= boxY + boxH) {
                    if (mouseY <= boxY + 9.5F) {
                        ns.increment();
                    } else {
                        ns.decrement();
                    }
                    return true;
                }
            } else {
                float trackY = sy + 14.0F;
                if (mouseX >= sx - 4.0F && mouseX <= sx + sw + 4.0F && mouseY >= trackY - 5.0F && mouseY <= trackY + 10.0F) {
                    activeDraggingSlider = ns;
                    double newNorm = MathHelper.clamp((mouseX - sx) / (double) sw, 0.0D, 1.0D);
                    ns.setNormalized(newNorm);
                    return true;
                }
            }
        } else if (setting instanceof ModeSetting) {
            ModeSetting ms = (ModeSetting) setting;
            float dropY = sy + 13.0F;
            float dropH = 19.0F;
            if (mouseX >= sx && mouseX <= sx + sw && mouseY >= dropY && mouseY <= dropY + dropH) {
                openDropdown = (openDropdown == ms) ? null : ms;
                dropdownMenuX = sx;
                dropdownMenuY = dropY + dropH + 2.0F;
                dropdownMenuW = sw;
                return true;
            }
        } else if (setting instanceof BooleanSetting) {
            BooleanSetting bs = (BooleanSetting) setting;
            if (mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + 24.0F) {
                bs.toggle();
                return true;
            }
        } else if (setting instanceof ColorSetting) {
            ColorSetting cs = (ColorSetting) setting;
            if (mouseX >= sx && mouseX <= sx + sw && mouseY >= sy && mouseY <= sy + 20.0F) {
                int[] presets = {0xFF22C55E, 0xFF00E5FF, 0xFF3B82F6, 0xFFA855F7, 0xFFEC4899, 0xFFEAB308, 0xFFEF4444};
                int cur = cs.get();
                int next = presets[0];
                for (int i = 0; i < presets.length; i++) {
                    if (presets[i] == cur) {
                        next = presets[(i + 1) % presets.length];
                        break;
                    }
                }
                cs.set(next);
                return true;
            }
        }
        return false;
    }

    // ==========================================
    // FLOATING POPUP DROPDOWN MENU
    // ==========================================

    public static void renderFloatingDropdown(MatrixStack matrices, int mouseX, int mouseY) {
        if (openDropdown == null) return;

        List<String> values = openDropdown.getValues();
        float itemH = 18.0F;
        float totalH = values.size() * itemH + 6.0F;

        // Shadow & Backdrop
        RenderUtil.shadow(matrices, dropdownMenuX, dropdownMenuY, dropdownMenuW, totalH, 8.0F);
        RenderUtil.roundedRect(matrices, dropdownMenuX, dropdownMenuY, dropdownMenuW, totalH, 4.0F, 0xFA14181F);
        RenderUtil.drawHollowRoundedOutline(matrices, dropdownMenuX, dropdownMenuY, dropdownMenuW, totalH, 4.0F, 1.0F, 0xFF2A3645);

        for (int i = 0; i < values.size(); i++) {
            String val = values.get(i);
            float iy = dropdownMenuY + 3.0F + i * itemH;
            boolean selected = val.equalsIgnoreCase(openDropdown.get());
            boolean hov = mouseX >= dropdownMenuX && mouseX <= dropdownMenuX + dropdownMenuW && mouseY >= iy && mouseY <= iy + itemH;

            if (hov) {
                RenderUtil.roundedRect(matrices, dropdownMenuX + 2.0F, iy, dropdownMenuW - 4.0F, itemH, 3.0F,
                        (ThemeManager.getAccent() & 0x00FFFFFF) | 0x40000000);
            }

            int col = selected ? ThemeManager.getAccent() : (hov ? 0xFFFFFFFF : 0xFFCBD5E1);
            if (selected) {
                VectorIcons.drawCheckmark(matrices, dropdownMenuX + 8.0F, iy + itemH * 0.5F, 5.0F, ThemeManager.getAccent());
            }
            FontEngine.SMALL_11.drawString(matrices, val, dropdownMenuX + 16.0F, iy + 3.5F, col);
        }
    }

    public static boolean handleDropdownClick(double mouseX, double mouseY) {
        if (openDropdown == null) return false;

        List<String> values = openDropdown.getValues();
        float itemH = 18.0F;
        float totalH = values.size() * itemH + 6.0F;

        if (mouseX >= dropdownMenuX && mouseX <= dropdownMenuX + dropdownMenuW && mouseY >= dropdownMenuY && mouseY <= dropdownMenuY + totalH) {
            int index = (int) ((mouseY - (dropdownMenuY + 3.0F)) / itemH);
            if (index >= 0 && index < values.size()) {
                openDropdown.set(values.get(index));
            }
            openDropdown = null;
            return true;
        }

        // Click outside closes dropdown
        openDropdown = null;
        return true;
    }

    public static void mouseReleased() {
        activeDraggingSlider = null;
    }

    private static String formatNumber(double val) {
        if (Math.abs(val - Math.round(val)) < 0.001D) {
            return String.valueOf(Math.round(val));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", val);
    }
}
