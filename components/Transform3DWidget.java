package dev.ddpaura.client.gui.clickgui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ddpaura.client.gui.clickgui.Component;
import dev.ddpaura.client.gui.render.RenderUtil;
import dev.ddpaura.client.gui.render.VectorIcons;
import dev.ddpaura.client.render.font.FontEngine;
import net.minecraft.util.math.MathHelper;

/**
 * Modern 3D Weapon/Hand Transform Editor widget matching Image 2.
 * Side-by-side layout: 3D isometric viewport on left, coordinate steppers and reset on right.
 * Eliminates overlapping text and oversized widgets.
 */
public final class Transform3DWidget extends Component {

    private String phase = "Начало";
    private static final String[] PHASES = {"Начало", "Конец", "Просмотр"};

    private float posX = 0.0F;
    private float posY = 0.0F;
    private float posZ = 0.0F;

    public Transform3DWidget(float x, float y, float width) {
        super(x, y, width, 142.0F);
    }

    public void reset() {
        posX = 0.0F;
        posY = 0.0F;
        posZ = 0.0F;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        this.height = 142.0F;

        // Outer Card Container
        RenderUtil.roundedRect(matrices, x, y, width, height, 7.0F, 0xF013171F);
        RenderUtil.drawHollowRoundedOutline(matrices, x, y, width, height, 7.0F, 1.0F, 0x302A3645);

        // Header: "Редактор" on left
        FontEngine.BOLD_12.drawString(matrices, "Редактор", x + 10.0F, y + 8.0F, 0xFFFFFFFF);

        // Header Tabs: [Начало] [Конец] [Просмотр] on right
        float[] tabWidths = {32.0F, 32.0F, 44.0F};
        float tabH = 14.0F;
        float tabGap = 3.0F;
        float totalTabsW = tabWidths[0] + tabWidths[1] + tabWidths[2] + tabGap * 2.0F;
        float tabX = x + width - totalTabsW - 8.0F;
        float tabY = y + 6.0F;

        for (int i = 0; i < PHASES.length; i++) {
            String p = PHASES[i];
            float tw = tabWidths[i];
            boolean selected = p.equals(phase);
            boolean hov = mouseX >= tabX && mouseX <= tabX + tw && mouseY >= tabY && mouseY <= tabY + tabH;

            int bg = selected ? 0xFF22C55E : (hov ? 0x302A3645 : 0xFF181E25);
            RenderUtil.roundedRect(matrices, tabX, tabY, tw, tabH, 2.5F, bg);
            if (!selected) {
                RenderUtil.drawHollowRoundedOutline(matrices, tabX, tabY, tw, tabH, 2.5F, 1.0F, 0x302A3645);
            }
            int textColor = selected ? 0xFFFFFFFF : (hov ? 0xFFE2E8F0 : 0xFF94A3B8);
            FontEngine.TINY_12.drawCenteredString(matrices, p, tabX + tw * 0.5F, tabY + 2.5F, textColor);

            tabX += tw + tabGap;
        }

        // -------------------------------------------------------------
        // ROW: Side-by-Side (3D Viewport on left, Steppers on right)
        // -------------------------------------------------------------
        float vpX = x + 10.0F;
        float vpY = y + 24.0F;
        float vpW = 92.0F;
        float vpH = 108.0F;

        // Viewport Box
        RenderUtil.roundedRect(matrices, vpX, vpY, vpW, vpH, 5.0F, 0xFF0D1117);
        RenderUtil.drawHollowRoundedOutline(matrices, vpX, vpY, vpW, vpH, 5.0F, 1.0F, 0x352A3645);

        // Isometric Grid Floor
        float centerX = vpX + vpW * 0.48F;
        float centerY = vpY + vpH * 0.62F;

        int gridLines = 3;
        float step = 11.0F;
        for (int i = -gridLines; i <= gridLines; i++) {
            float x0 = centerX + (i - gridLines) * step * 0.866F;
            float y0 = centerY + (i + gridLines) * step * 0.5F;
            float x1 = centerX + (i + gridLines) * step * 0.866F;
            float y1 = centerY + (i - gridLines) * step * 0.5F;
            RenderUtil.line(matrices, x0, y0, x1, y1, 1.0F, 0x14334155);

            float x2 = centerX + (-gridLines + i) * step * 0.866F;
            float y2 = centerY + (-gridLines - i) * step * 0.5F;
            float x3 = centerX + (gridLines + i) * step * 0.866F;
            float y3 = centerY + (gridLines - i) * step * 0.5F;
            RenderUtil.line(matrices, x2, y2, x3, y3, 1.0F, 0x14334155);
        }

        // Coordinate Origin
        float originX = centerX + posX * 6.0F;
        float originY = centerY - posY * 6.0F + posZ * 3.5F;

        // X-axis: Emerald Green -> pointing right-down
        float axisX_endX = originX + 28.0F;
        float axisX_endY = originY + 9.0F;
        RenderUtil.line(matrices, originX, originY, axisX_endX, axisX_endY, 1.8F, 0xFF22C55E);
        RenderUtil.circle(matrices, axisX_endX, axisX_endY, 2.0F, 0xFF22C55E);

        // Y-axis: Red -> pointing UP
        float axisY_endX = originX;
        float axisY_endY = originY - 28.0F;
        RenderUtil.line(matrices, originX, originY, axisY_endX, axisY_endY, 1.8F, 0xFFEF4444);
        RenderUtil.circle(matrices, axisY_endX, axisY_endY, 2.0F, 0xFFEF4444);

        // Z-axis: Blue -> pointing left-down
        float axisZ_endX = originX - 20.0F;
        float axisZ_endY = originY + 12.0F;
        RenderUtil.line(matrices, originX, originY, axisZ_endX, axisZ_endY, 1.8F, 0xFF3B82F6);
        RenderUtil.circle(matrices, axisZ_endX, axisZ_endY, 2.0F, 0xFF3B82F6);

        // Origin point
        RenderUtil.circle(matrices, originX, originY, 2.5F, 0xFFFFFFFF);

        // Right side: X, Y, Z steppers and Reset Button
        float ctrlX = vpX + vpW + 10.0F;
        float ctrlW = width - (ctrlX - x) - 10.0F;

        float sY = vpY + 2.0F;
        float sGap = 24.0F;

        // X stepper
        renderCoordinateStepper(matrices, "X", posX, ctrlX, sY, ctrlW, 0xFF22C55E, mouseX, mouseY);

        // Y stepper
        renderCoordinateStepper(matrices, "Y", posY, ctrlX, sY + sGap, ctrlW, 0xFFEF4444, mouseX, mouseY);

        // Z stepper
        renderCoordinateStepper(matrices, "Z", posZ, ctrlX, sY + sGap * 2.0F, ctrlW, 0xFF3B82F6, mouseX, mouseY);

        // Reset Position Button
        float resetY = sY + sGap * 3.0F + 2.0F;
        float resetH = 18.0F;
        boolean resetHov = mouseX >= ctrlX && mouseX <= ctrlX + ctrlW && mouseY >= resetY && mouseY <= resetY + resetH;
        int resetBg = resetHov ? 0x302A3645 : 0xFF181E25;
        RenderUtil.roundedRect(matrices, ctrlX, resetY, ctrlW, resetH, 3.5F, resetBg);
        RenderUtil.drawHollowRoundedOutline(matrices, ctrlX, resetY, ctrlW, resetH, 3.5F, 1.0F, resetHov ? 0xFF22C55E : 0x302A3645);

        float rIconX = ctrlX + 8.0F;
        VectorIcons.drawReset(matrices, rIconX, resetY + 9.0F, 3.5F, resetHov ? 0xFFFFFFFF : 0xFF94A3B8);
        FontEngine.TINY_12.drawString(matrices, "Сбросить позицию", rIconX + 8.0F, resetY + 4.0F, resetHov ? 0xFFFFFFFF : 0xFF94A3B8);
    }

    private void renderCoordinateStepper(MatrixStack matrices, String axis, float val, float sx, float sy, float sw,
                                         int axisColor, int mx, int my) {
        // Axis label on left
        FontEngine.BOLD_12.drawString(matrices, axis, sx + 2.0F, sy + 4.0F, axisColor);

        // Box on right
        float boxX = sx + 16.0F;
        float boxW = sw - 16.0F;
        float boxH = 17.0F;

        RenderUtil.roundedRect(matrices, boxX, sy, boxW, boxH, 3.0F, 0xFF181E25);
        RenderUtil.drawHollowRoundedOutline(matrices, boxX, sy, boxW, boxH, 3.0F, 1.0F, 0x302A3645);

        String valStr = String.format(java.util.Locale.ROOT, "%.1f", val);
        FontEngine.TINY_12.drawString(matrices, valStr, boxX + 6.0F, sy + 3.5F, 0xFFCBD5E1);

        // Up & down arrow buttons on far right of box
        float arrX = boxX + boxW - 10.0F;
        boolean upHov = mx >= arrX - 3.0F && mx <= arrX + 7.0F && my >= sy && my <= sy + 8.5F;
        boolean downHov = mx >= arrX - 3.0F && mx <= arrX + 7.0F && my >= sy + 8.5F && my <= sy + 17.0F;

        VectorIcons.drawArrowUp(matrices, arrX, sy + 4.5F, 5.0F, upHov ? 0xFF22C55E : 0xFF64748B);
        VectorIcons.drawArrowDown(matrices, arrX, sy + 12.5F, 5.0F, downHov ? 0xFF22C55E : 0xFF64748B);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        // Header Tabs click
        float[] tabWidths = {32.0F, 32.0F, 44.0F};
        float tabH = 14.0F;
        float tabGap = 3.0F;
        float totalTabsW = tabWidths[0] + tabWidths[1] + tabWidths[2] + tabGap * 2.0F;
        float tabX = x + width - totalTabsW - 8.0F;
        float tabY = y + 6.0F;

        for (int i = 0; i < PHASES.length; i++) {
            float tw = tabWidths[i];
            if (mouseX >= tabX && mouseX <= tabX + tw && mouseY >= tabY && mouseY <= tabY + tabH) {
                phase = PHASES[i];
                return true;
            }
            tabX += tw + tabGap;
        }

        float vpW = 92.0F;
        float ctrlX = x + 10.0F + vpW + 10.0F;
        float ctrlW = width - (ctrlX - x) - 10.0F;
        float sY = y + 24.0F + 2.0F;
        float sGap = 24.0F;

        // X stepper click
        if (handleStepperClick(ctrlX, sY, ctrlW, mouseX, mouseY, 1)) return true;
        // Y stepper click
        if (handleStepperClick(ctrlX, sY + sGap, ctrlW, mouseX, mouseY, 2)) return true;
        // Z stepper click
        if (handleStepperClick(ctrlX, sY + sGap * 2.0F, ctrlW, mouseX, mouseY, 3)) return true;

        // Reset button
        float resetY = sY + sGap * 3.0F + 2.0F;
        float resetH = 18.0F;
        if (mouseX >= ctrlX && mouseX <= ctrlX + ctrlW && mouseY >= resetY && mouseY <= resetY + resetH) {
            reset();
            return true;
        }

        return false;
    }

    private boolean handleStepperClick(float sx, float sy, float sw, double mx, double my, int axis) {
        float boxX = sx + 16.0F;
        float boxW = sw - 16.0F;
        float arrX = boxX + boxW - 10.0F;

        if (mx >= arrX - 4.0F && mx <= arrX + 8.0F) {
            if (my >= sy && my <= sy + 8.5F) {
                if (axis == 1) posX = Math.min(5.0F, posX + 0.1F);
                else if (axis == 2) posY = Math.min(5.0F, posY + 0.1F);
                else if (axis == 3) posZ = Math.min(5.0F, posZ + 0.1F);
                return true;
            } else if (my >= sy + 8.5F && my <= sy + 17.0F) {
                if (axis == 1) posX = Math.max(-5.0F, posX - 0.1F);
                else if (axis == 2) posY = Math.max(-5.0F, posY - 0.1F);
                else if (axis == 3) posZ = Math.max(-5.0F, posZ - 0.1F);
                return true;
            }
        }
        return false;
    }
}
