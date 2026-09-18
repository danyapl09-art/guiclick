package dev.ddpaura.client.gui.clickgui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ddpaura.client.gui.clickgui.Component;
import dev.ddpaura.client.gui.render.RenderUtil;
import dev.ddpaura.client.gui.render.VectorIcons;
import dev.ddpaura.client.render.font.FontEngine;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * High-precision Bézier curve animation editor widget matching Image 2.
 * Compact side-by-side layout: Bézier graph on left, swing speed/smoothness/chips on right,
 * return-to-start toggle on bottom. Solid anti-aliased geometry, zero emoji glyphs.
 */
public final class BezierCurveWidget extends Component {

    private float p1x = 0.25F;
    private float p1y = 0.10F;
    private float p2x = 0.25F;
    private float p2y = 1.00F;

    private float speed = 2.0F;
    private float smoothness = 1.0F;
    private String animType = "Sharp";
    private boolean returnToStart = true;

    private int draggingPoint = 0; // 0 = none, 1 = P1, 2 = P2
    private boolean draggingSpeed = false;
    private boolean draggingSmoothness = false;

    private static final String[] ANIM_TYPES = {"Sharp", "Standard", "Forward", "Default"};

    public BezierCurveWidget(float x, float y, float width) {
        super(x, y, width, 142.0F);
    }

    public void reset() {
        p1x = 0.25F;
        p1y = 0.10F;
        p2x = 0.25F;
        p2y = 1.00F;
        speed = 2.0F;
        smoothness = 1.0F;
        animType = "Sharp";
        returnToStart = true;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        this.height = 142.0F;

        // Outer Card Container
        RenderUtil.roundedRect(matrices, x, y, width, height, 7.0F, 0xF013171F);
        RenderUtil.drawHollowRoundedOutline(matrices, x, y, width, height, 7.0F, 1.0F, 0x302A3645);

        // Header: "Анимация" on left, "↺ Сбросить" on right
        FontEngine.BOLD_12.drawString(matrices, "Анимация", x + 10.0F, y + 8.0F, 0xFFFFFFFF);

        float resetW = 60.0F;
        float resetH = 14.0F;
        float resetX = x + width - resetW - 8.0F;
        float resetY = y + 7.0F;
        boolean resetHov = mouseX >= resetX && mouseX <= resetX + resetW && mouseY >= resetY && mouseY <= resetY + resetH;
        int resetCol = resetHov ? 0xFF22C55E : 0xFF94A3B8;
        VectorIcons.drawReset(matrices, resetX + 5.0F, resetY + 6.0F, 3.5F, resetCol);
        FontEngine.TINY_12.drawString(matrices, "Сбросить", resetX + 13.0F, resetY + 2.0F, resetCol);

        // -------------------------------------------------------------
        // ROW 1: Side-by-Side (Bézier Graph on left, Controls on right)
        // -------------------------------------------------------------
        float graphX = x + 10.0F;
        float graphY = y + 24.0F;
        float graphW = 90.0F;
        float graphH = 68.0F;

        // Graph Backdrop
        RenderUtil.roundedRect(matrices, graphX, graphY, graphW, graphH, 5.0F, 0xFF0D1117);
        RenderUtil.drawHollowRoundedOutline(matrices, graphX, graphY, graphW, graphH, 5.0F, 1.0F, 0x352A3645);

        // Subtle grid
        for (int i = 1; i < 4; i++) {
            float gx = graphX + (graphW * i) / 4.0F;
            RenderUtil.rect(matrices, gx, graphY + 2.0F, 1.0F, graphH - 4.0F, 0x12FFFFFF);
            float gy = graphY + (graphH * i) / 4.0F;
            RenderUtil.rect(matrices, graphX + 2.0F, gy, graphW - 4.0F, 1.0F, 0x12FFFFFF);
        }

        // Anchor points
        float p0x = graphX + 8.0F;
        float p0y = graphY + graphH - 8.0F;
        float p3x = graphX + graphW - 8.0F;
        float p3y = graphY + 8.0F;

        float activeP1X = p0x + (p3x - p0x) * p1x;
        float activeP1Y = p0y - (p0y - p3y) * p1y;
        float activeP2X = p0x + (p3x - p0x) * p2x;
        float activeP2Y = p0y - (p0y - p3y) * p2y;

        // Handle lines
        RenderUtil.line(matrices, p0x, p0y, activeP1X, activeP1Y, 1.0F, 0x4064748B);
        RenderUtil.line(matrices, p3x, p3y, activeP2X, activeP2Y, 1.0F, 0x4064748B);

        // Smooth Green Bézier Curve
        RenderUtil.cubicBezier(matrices, p0x, p0y, activeP1X, activeP1Y, activeP2X, activeP2Y, p3x, p3y, 2.0F, 0xFF22C55E);

        // Control handle points
        boolean p1Hover = MathHelper.sqrt((mouseX - activeP1X) * (mouseX - activeP1X) + (mouseY - activeP1Y) * (mouseY - activeP1Y)) <= 6.0F;
        boolean p2Hover = MathHelper.sqrt((mouseX - activeP2X) * (mouseX - activeP2X) + (mouseY - activeP2Y) * (mouseY - activeP2Y)) <= 6.0F;

        RenderUtil.circle(matrices, activeP1X, activeP1Y, 3.5F, (p1Hover || draggingPoint == 1) ? 0xFFFFFFFF : 0xFF22C55E);
        RenderUtil.circleOutline(matrices, activeP1X, activeP1Y, 3.5F, 1.0F, 0xFF10B981);

        RenderUtil.circle(matrices, activeP2X, activeP2Y, 3.5F, (p2Hover || draggingPoint == 2) ? 0xFFFFFFFF : 0xFF22C55E);
        RenderUtil.circleOutline(matrices, activeP2X, activeP2Y, 3.5F, 1.0F, 0xFF10B981);

        // Drag handles update
        if (draggingPoint == 1) {
            p1x = MathHelper.clamp((mouseX - p0x) / (p3x - p0x), 0.0F, 1.0F);
            p1y = MathHelper.clamp((p0y - mouseY) / (p0y - p3y), 0.0F, 1.0F);
        } else if (draggingPoint == 2) {
            p2x = MathHelper.clamp((mouseX - p0x) / (p3x - p0x), 0.0F, 1.0F);
            p2y = MathHelper.clamp((p0y - mouseY) / (p0y - p3y), 0.0F, 1.0F);
        }

        // Right side controls (aligned with graph)
        float ctrlX = graphX + graphW + 10.0F;
        float ctrlW = width - (ctrlX - x) - 10.0F;

        // Slider 1: Скорость взмаха
        float s1Y = graphY;
        FontEngine.TINY_12.drawString(matrices, "Скорость взмаха", ctrlX, s1Y, 0xFFCBD5E1);
        String s1Val = String.format(java.util.Locale.ROOT, "%.1f", speed);
        FontEngine.TINY_12.drawString(matrices, s1Val, ctrlX + ctrlW - FontEngine.TINY_12.getStringWidth(s1Val), s1Y, 0xFF94A3B8);

        float s1TrackY = s1Y + 11.0F;
        RenderUtil.roundedRect(matrices, ctrlX, s1TrackY, ctrlW, 3.0F, 1.5F, 0xFF242E3B);
        float s1Norm = MathHelper.clamp((speed - 0.5F) / (4.0F - 0.5F), 0.0F, 1.0F);
        RenderUtil.roundedRect(matrices, ctrlX, s1TrackY, ctrlW * s1Norm, 3.0F, 1.5F, 0xFF22C55E);
        float thumb1X = ctrlX + ctrlW * s1Norm;
        RenderUtil.circle(matrices, thumb1X, s1TrackY + 1.5F, 4.0F, 0xFFFFFFFF);

        if (draggingSpeed) {
            float n = MathHelper.clamp((mouseX - ctrlX) / ctrlW, 0.0F, 1.0F);
            speed = 0.5F + n * 3.5F;
        }

        // Slider 2: Плавность
        float s2Y = s1TrackY + 8.0F;
        FontEngine.TINY_12.drawString(matrices, "Плавность", ctrlX, s2Y, 0xFFCBD5E1);
        String s2Val = String.format(java.util.Locale.ROOT, "%.1f", smoothness);
        FontEngine.TINY_12.drawString(matrices, s2Val, ctrlX + ctrlW - FontEngine.TINY_12.getStringWidth(s2Val), s2Y, 0xFF94A3B8);

        float s2TrackY = s2Y + 11.0F;
        RenderUtil.roundedRect(matrices, ctrlX, s2TrackY, ctrlW, 3.0F, 1.5F, 0xFF242E3B);
        float s2Norm = MathHelper.clamp((smoothness - 0.1F) / (3.0F - 0.1F), 0.0F, 1.0F);
        RenderUtil.roundedRect(matrices, ctrlX, s2TrackY, ctrlW * s2Norm, 3.0F, 1.5F, 0xFF22C55E);
        float thumb2X = ctrlX + ctrlW * s2Norm;
        RenderUtil.circle(matrices, thumb2X, s2TrackY + 1.5F, 4.0F, 0xFFFFFFFF);

        if (draggingSmoothness) {
            float n = MathHelper.clamp((mouseX - ctrlX) / ctrlW, 0.0F, 1.0F);
            smoothness = 0.1F + n * 2.9F;
        }

        // "Тип анимации" chips
        float typeY = s2TrackY + 8.0F;
        FontEngine.TINY_12.drawString(matrices, "Тип анимации", ctrlX, typeY, 0xFFCBD5E1);

        float chipY = typeY + 10.0F;
        float chipGap = 3.0F;
        float chipW = (ctrlW - chipGap * (ANIM_TYPES.length - 1)) / ANIM_TYPES.length;
        float chipH = 13.0F;

        for (int i = 0; i < ANIM_TYPES.length; i++) {
            String t = ANIM_TYPES[i];
            float chX = ctrlX + i * (chipW + chipGap);
            boolean sel = t.equalsIgnoreCase(animType);
            boolean chHov = mouseX >= chX && mouseX <= chX + chipW && mouseY >= chipY && mouseY <= chipY + chipH;

            int bg = sel ? 0xFF22C55E : (chHov ? 0x302A3645 : 0xFF181E25);
            RenderUtil.roundedRect(matrices, chX, chipY, chipW, chipH, 2.5F, bg);
            if (!sel) {
                RenderUtil.drawHollowRoundedOutline(matrices, chX, chipY, chipW, chipH, 2.5F, 1.0F, 0x302A3645);
            }
            int textCol = sel ? 0xFFFFFFFF : (chHov ? 0xFFE2E8F0 : 0xFF94A3B8);
            FontEngine.TINY_12.drawCenteredString(matrices, t, chX + chipW * 0.5F, chipY + 2.0F, textCol);
        }

        // -------------------------------------------------------------
        // ROW 2: Bottom Toggle (Возвращать в начало)
        // -------------------------------------------------------------
        float row2Y = y + 102.0F;
        FontEngine.SMALL_14.drawString(matrices, "Возвращать в начало", x + 10.0F, row2Y, 0xFFE2E8F0);
        FontEngine.TINY_12.drawString(matrices, "Автоматически возвращает в исходное положение.", x + 10.0F, row2Y + 12.0F, 0xFF64748B);

        float swW = 24.0F;
        float swH = 13.0F;
        float swX = x + width - swW - 10.0F;
        float swY = row2Y + 4.0F;

        int trackBg = returnToStart ? 0xFF22C55E : 0xFF28323D;
        RenderUtil.roundedRect(matrices, swX, swY, swW, swH, swH * 0.5F, trackBg);
        float knobX = returnToStart ? (swX + swW - swH * 0.5F) : (swX + swH * 0.5F);
        RenderUtil.circle(matrices, knobX, swY + swH * 0.5F, (swH - 3.5F) * 0.5F, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        // Reset button
        float resetW = 60.0F;
        float resetH = 14.0F;
        float resetX = x + width - resetW - 8.0F;
        float resetY = y + 7.0F;
        if (mouseX >= resetX && mouseX <= resetX + resetW && mouseY >= resetY && mouseY <= resetY + resetH) {
            reset();
            return true;
        }

        // Graph points
        float graphX = x + 10.0F;
        float graphY = y + 24.0F;
        float graphW = 90.0F;
        float graphH = 68.0F;

        float p0x = graphX + 8.0F;
        float p0y = graphY + graphH - 8.0F;
        float p3x = graphX + graphW - 8.0F;
        float p3y = graphY + 8.0F;

        float activeP1X = p0x + (p3x - p0x) * p1x;
        float activeP1Y = p0y - (p0y - p3y) * p1y;
        float activeP2X = p0x + (p3x - p0x) * p2x;
        float activeP2Y = p0y - (p0y - p3y) * p2y;

        if (MathHelper.sqrt((mouseX - activeP1X) * (mouseX - activeP1X) + (mouseY - activeP1Y) * (mouseY - activeP1Y)) <= 8.0F) {
            draggingPoint = 1;
            return true;
        }
        if (MathHelper.sqrt((mouseX - activeP2X) * (mouseX - activeP2X) + (mouseY - activeP2Y) * (mouseY - activeP2Y)) <= 8.0F) {
            draggingPoint = 2;
            return true;
        }

        // Sliders
        float ctrlX = graphX + graphW + 10.0F;
        float ctrlW = width - (ctrlX - x) - 10.0F;

        float s1TrackY = graphY + 11.0F;
        if (mouseX >= ctrlX && mouseX <= ctrlX + ctrlW && mouseY >= s1TrackY - 4.0F && mouseY <= s1TrackY + 8.0F) {
            draggingSpeed = true;
            float n = MathHelper.clamp(((float) mouseX - ctrlX) / ctrlW, 0.0F, 1.0F);
            speed = 0.5F + n * 3.5F;
            return true;
        }

        float s2TrackY = s1TrackY + 19.0F;
        if (mouseX >= ctrlX && mouseX <= ctrlX + ctrlW && mouseY >= s2TrackY - 4.0F && mouseY <= s2TrackY + 8.0F) {
            draggingSmoothness = true;
            float n = MathHelper.clamp(((float) mouseX - ctrlX) / ctrlW, 0.0F, 1.0F);
            smoothness = 0.1F + n * 2.9F;
            return true;
        }

        // Chips
        float chipY = s2TrackY + 18.0F;
        float chipGap = 3.0F;
        float chipW = (ctrlW - chipGap * (ANIM_TYPES.length - 1)) / ANIM_TYPES.length;
        float chipH = 13.0F;
        for (int i = 0; i < ANIM_TYPES.length; i++) {
            float chX = ctrlX + i * (chipW + chipGap);
            if (mouseX >= chX && mouseX <= chX + chipW && mouseY >= chipY && mouseY <= chipY + chipH) {
                animType = ANIM_TYPES[i];
                return true;
            }
        }

        // Bottom Toggle
        float row2Y = y + 102.0F;
        if (mouseX >= x + 10.0F && mouseX <= x + width - 10.0F && mouseY >= row2Y && mouseY <= row2Y + 24.0F) {
            returnToStart = !returnToStart;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingPoint = 0;
        draggingSpeed = false;
        draggingSmoothness = false;
        return false;
    }
}
