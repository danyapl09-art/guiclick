package dev.ddpaura.client.gui.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ddpaura.client.gui.GuiTheme;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class RenderUtil {
    private static final int CIRCLE_SEGMENTS = 20;

    private RenderUtil() {
    }

    /** Establishes the deterministic 2D state used by every GUI primitive. */
    public static void beginFrame() {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.enableTexture();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Restores the vanilla state expected after a Screen has rendered. */
    public static void endFrame() {
        RenderSystem.disableScissor();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void rect(MatrixStack matrices, float x, float y, float width, float height, int color) {
        if (width <= 0.0F || height <= 0.0F || alpha(color) == 0) {
            return;
        }
        float x2 = x + width;
        float y2 = y + height;
        Matrix4f matrix = matrices.last().pose();

        RenderSystem.disableTexture();
        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(builder, matrix, x, y2, color);
        vertex(builder, matrix, x2, y2, color);
        vertex(builder, matrix, x2, y, color);
        vertex(builder, matrix, x, y, color);
        Tessellator.getInstance().end();
        RenderSystem.enableTexture();
    }

    public static void line(MatrixStack matrices, float x1, float y1, float x2, float y2, float thickness, int color) {
        if (thickness <= 0.0F || alpha(color) == 0) {
            return;
        }
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001F) {
            return;
        }
        float half = thickness * 0.5F;
        float nx = -dy / length * half;
        float ny = dx / length * half;

        Matrix4f matrix = matrices.last().pose();
        RenderSystem.disableTexture();
        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(builder, matrix, x1 + nx, y1 + ny, color);
        vertex(builder, matrix, x2 + nx, y2 + ny, color);
        vertex(builder, matrix, x2 - nx, y2 - ny, color);
        vertex(builder, matrix, x1 - nx, y1 - ny, color);
        Tessellator.getInstance().end();
        RenderSystem.enableTexture();
    }

    public static void drawTexturedQuad(MatrixStack matrices, float x, float y, float width, float height,
                                        float u0, float v0, float u1, float v1, float alpha) {
        if (width <= 0.0F || height <= 0.0F || alpha <= 0.001F) {
            return;
        }
        Matrix4f matrix = matrices.last().pose();
        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, Math.max(0.0F, Math.min(1.0F, alpha)));
        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        builder.vertex(matrix, x, y + height, 0.0F).uv(u0, v1).endVertex();
        builder.vertex(matrix, x + width, y + height, 0.0F).uv(u1, v1).endVertex();
        builder.vertex(matrix, x + width, y, 0.0F).uv(u1, v0).endVertex();
        builder.vertex(matrix, x, y, 0.0F).uv(u0, v0).endVertex();
        Tessellator.getInstance().end();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void gradientRect(MatrixStack matrices, float x, float y, float width, float height,
                                    int topLeft, int topRight, int bottomRight, int bottomLeft) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        Matrix4f matrix = matrices.last().pose();
        RenderSystem.disableTexture();
        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(builder, matrix, x, y + height, bottomLeft);
        vertex(builder, matrix, x + width, y + height, bottomRight);
        vertex(builder, matrix, x + width, y, topRight);
        vertex(builder, matrix, x, y, topLeft);
        Tessellator.getInstance().end();
        RenderSystem.enableTexture();
    }

    public static void horizontalGradient(MatrixStack matrices, float x, float y, float width, float height,
                                           int leftColor, int rightColor) {
        gradientRect(matrices, x, y, width, height, leftColor, rightColor, rightColor, leftColor);
    }

    public static void verticalGradient(MatrixStack matrices, float x, float y, float width, float height,
                                         int topColor, int bottomColor) {
        gradientRect(matrices, x, y, width, height, topColor, topColor, bottomColor, bottomColor);
    }

    public static void arc(MatrixStack matrices, float centerX, float centerY, float radius,
                           float thickness, float startDegrees, float endDegrees, int color) {
        if (radius <= 0.0F || thickness <= 0.0F || alpha(color) == 0) {
            return;
        }
        float span = endDegrees - startDegrees;
        int segments = Math.max(8, Math.min(64, (int) Math.ceil(Math.abs(span) / 7.5F)));
        float inner = Math.max(0.0F, radius - thickness * 0.5F);
        float outer = radius + thickness * 0.5F;
        Matrix4f matrix = matrices.last().pose();
        RenderSystem.disableTexture();
        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int index = 0; index <= segments; index++) {
            double angle = Math.toRadians(startDegrees + span * index / segments);
            float cosine = (float) Math.cos(angle);
            float sine = (float) Math.sin(angle);
            vertex(builder, matrix, centerX + cosine * outer, centerY + sine * outer, color);
            vertex(builder, matrix, centerX + cosine * inner, centerY + sine * inner, color);
        }
        Tessellator.getInstance().end();
        RenderSystem.enableTexture();
    }

    public static void cubicBezier(MatrixStack matrices,
                                   float x0, float y0, float x1, float y1,
                                   float x2, float y2, float x3, float y3,
                                   float thickness, int color) {
        if (thickness <= 0.0F || alpha(color) == 0) {
            return;
        }
        int segments = 28;
        float half = thickness * 0.5F;
        Matrix4f matrix = matrices.last().pose();
        RenderSystem.disableTexture();
        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int index = 0; index <= segments; index++) {
            float t = index / (float) segments;
            float inverse = 1.0F - t;
            float x = inverse * inverse * inverse * x0
                    + 3.0F * inverse * inverse * t * x1
                    + 3.0F * inverse * t * t * x2
                    + t * t * t * x3;
            float y = inverse * inverse * inverse * y0
                    + 3.0F * inverse * inverse * t * y1
                    + 3.0F * inverse * t * t * y2
                    + t * t * t * y3;
            float dx = 3.0F * inverse * inverse * (x1 - x0)
                    + 6.0F * inverse * t * (x2 - x1)
                    + 3.0F * t * t * (x3 - x2);
            float dy = 3.0F * inverse * inverse * (y1 - y0)
                    + 6.0F * inverse * t * (y2 - y1)
                    + 3.0F * t * t * (y3 - y2);
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            float nx = length < 0.0001F ? 0.0F : -dy / length * half;
            float ny = length < 0.0001F ? half : dx / length * half;
            vertex(builder, matrix, x + nx, y + ny, color);
            vertex(builder, matrix, x - nx, y - ny, color);
        }
        Tessellator.getInstance().end();
        RenderSystem.enableTexture();
    }

    public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height,
                                       float radius, int color) {
        roundedRect(matrices, x, y, width, height, radius, color);
    }

    public static void roundedRect(MatrixStack matrices, float x, float y, float width, float height,
                                   float radius, int color) {
        if (width <= 0.0F || height <= 0.0F || alpha(color) == 0) {
            return;
        }
        float safeRadius = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));
        if (safeRadius < 0.75F) {
            rect(matrices, x, y, width, height, color);
            return;
        }

        Matrix4f matrix = matrices.last().pose();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        final int cornerSegments = 24;
        float innerRadius = Math.max(0.0F, safeRadius - 0.6F);
        float[] centersX = {x + width - safeRadius, x + width - safeRadius, x + safeRadius, x + safeRadius};
        float[] centersY = {y + safeRadius, y + height - safeRadius, y + height - safeRadius, y + safeRadius};

        // 1. Inner solid body
        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        vertex(builder, matrix, x + width * 0.5F, y + height * 0.5F, color);
        for (int corner = 0; corner < 4; corner++) {
            double start = -Math.PI * 0.5D + corner * Math.PI * 0.5D;
            for (int segment = 0; segment <= cornerSegments; segment++) {
                double angle = start + Math.PI * 0.5D * segment / cornerSegments;
                vertex(builder, matrix, centersX[corner] + (float) Math.cos(angle) * innerRadius,
                        centersY[corner] + (float) Math.sin(angle) * innerRadius, color);
            }
        }
        vertex(builder, matrix, centersX[0] + innerRadius, centersY[0], color);
        Tessellator.getInstance().end();

        // 2. Anti-aliasing fringe ring (fades color to alpha 0 over 1.2px)
        int zeroAlphaColor = withAlpha(color, 0);
        float outerRadius = safeRadius + 0.6F;
        builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int corner = 0; corner < 4; corner++) {
            double start = -Math.PI * 0.5D + corner * Math.PI * 0.5D;
            for (int segment = 0; segment <= cornerSegments; segment++) {
                double angle = start + Math.PI * 0.5D * segment / cornerSegments;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                vertex(builder, matrix, centersX[corner] + cos * innerRadius, centersY[corner] + sin * innerRadius, color);
                vertex(builder, matrix, centersX[corner] + cos * outerRadius, centersY[corner] + sin * outerRadius, zeroAlphaColor);
            }
        }
        vertex(builder, matrix, centersX[0] + innerRadius, centersY[0], color);
        vertex(builder, matrix, centersX[0] + outerRadius, centersY[0], zeroAlphaColor);
        Tessellator.getInstance().end();

        RenderSystem.enableTexture();
    }

    public static void circleOutline(MatrixStack matrices, float cx, float cy, float radius, float thickness, int color) {
        if (radius <= 0.0F || thickness <= 0.0F || alpha(color) == 0) {
            return;
        }
        Matrix4f matrix = matrices.last().pose();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        int segments = 48;
        float rIn = Math.max(0.0F, radius - thickness * 0.5F);
        float rOut = radius + thickness * 0.5F;
        int zeroColor = withAlpha(color, 0);

        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = i * (Math.PI * 2.0D / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            vertex(builder, matrix, cx + cos * rOut, cy + sin * rOut, color);
            vertex(builder, matrix, cx + cos * rIn, cy + sin * rIn, color);
        }
        Tessellator.getInstance().end();

        // Outer AA fringe
        builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = i * (Math.PI * 2.0D / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            vertex(builder, matrix, cx + cos * (rOut + 0.7F), cy + sin * (rOut + 0.7F), zeroColor);
            vertex(builder, matrix, cx + cos * rOut, cy + sin * rOut, color);
        }
        Tessellator.getInstance().end();

        RenderSystem.enableTexture();
    }

    public static void drawHollowRoundedOutline(MatrixStack matrices, float x, float y, float width, float height,
                                                float radius, float thickness, int color) {
        if (width <= 0.0F || height <= 0.0F || thickness <= 0.0F || alpha(color) == 0) {
            return;
        }
        float safeRadius = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));
        float innerRadius = Math.max(0.0F, safeRadius - thickness);

        Matrix4f matrix = matrices.last().pose();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        final int cornerSegments = 24;
        float[] centersX = {
                x + width - safeRadius,
                x + width - safeRadius,
                x + safeRadius,
                x + safeRadius
        };
        float[] centersY = {
                y + safeRadius,
                y + height - safeRadius,
                y + height - safeRadius,
                y + safeRadius
        };

        int zeroColor = withAlpha(color, 0);

        // 1. Solid stroke core
        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int corner = 0; corner < 4; corner++) {
            double start = -Math.PI * 0.5D + corner * Math.PI * 0.5D;
            for (int segment = 0; segment <= cornerSegments; segment++) {
                double angle = start + Math.PI * 0.5D * segment / cornerSegments;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                vertex(builder, matrix, centersX[corner] + cos * safeRadius, centersY[corner] + sin * safeRadius, color);
                vertex(builder, matrix, centersX[corner] + cos * innerRadius, centersY[corner] + sin * innerRadius, color);
            }
        }
        vertex(builder, matrix, centersX[0], y, color);
        vertex(builder, matrix, centersX[0], y + thickness, color);
        Tessellator.getInstance().end();

        // 2. Outer AA fringe
        builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int corner = 0; corner < 4; corner++) {
            double start = -Math.PI * 0.5D + corner * Math.PI * 0.5D;
            for (int segment = 0; segment <= cornerSegments; segment++) {
                double angle = start + Math.PI * 0.5D * segment / cornerSegments;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                vertex(builder, matrix, centersX[corner] + cos * (safeRadius + 0.8F), centersY[corner] + sin * (safeRadius + 0.8F), zeroColor);
                vertex(builder, matrix, centersX[corner] + cos * safeRadius, centersY[corner] + sin * safeRadius, color);
            }
        }
        vertex(builder, matrix, centersX[0], y - 0.8F, zeroColor);
        vertex(builder, matrix, centersX[0], y, color);
        Tessellator.getInstance().end();

        // 3. Inner AA fringe (if innerRadius > 0.5F)
        if (innerRadius > 0.8F) {
            builder = Tessellator.getInstance().getBuilder();
            builder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int corner = 0; corner < 4; corner++) {
                double start = -Math.PI * 0.5D + corner * Math.PI * 0.5D;
                for (int segment = 0; segment <= cornerSegments; segment++) {
                    double angle = start + Math.PI * 0.5D * segment / cornerSegments;
                    float cos = (float) Math.cos(angle);
                    float sin = (float) Math.sin(angle);
                    vertex(builder, matrix, centersX[corner] + cos * innerRadius, centersY[corner] + sin * innerRadius, color);
                    vertex(builder, matrix, centersX[corner] + cos * (innerRadius - 0.8F), centersY[corner] + sin * (innerRadius - 0.8F), zeroColor);
                }
            }
            vertex(builder, matrix, centersX[0], y + thickness, color);
            vertex(builder, matrix, centersX[0], y + thickness + 0.8F, zeroColor);
            Tessellator.getInstance().end();
        }

        RenderSystem.enableTexture();
    }

    public static void drawGlowingRoundedOutline(MatrixStack matrices, float x, float y, float width, float height,
                                                 float radius, float thickness, int strokeColor, int glowColor, float glowSpread) {
        if (glowSpread > 0.5F && alpha(glowColor) > 0) {
            int baseAlpha = alpha(glowColor);
            int passes = 5;
            for (int i = 1; i <= passes; i++) {
                float progress = (float) i / (float) passes;
                float spread = glowSpread * progress;
                float falloff = (float) Math.exp(-progress * 2.2F);
                int ringAlpha = Math.max(1, (int) (baseAlpha * falloff * 0.45F));
                int ringColor = withAlpha(glowColor, ringAlpha);
                drawHollowRoundedOutline(matrices, x - spread, y - spread, width + spread * 2.0F, height + spread * 2.0F,
                        radius + spread, thickness + spread * 0.3F, ringColor);
            }
        }
        drawHollowRoundedOutline(matrices, x, y, width, height, radius, thickness, strokeColor);
    }

    public static void roundedRectGradient(MatrixStack matrices, float x, float y, float width, float height,
                                           float radius, int topColor, int bottomColor) {
        if (width <= 0.0F || height <= 0.0F || (alpha(topColor) == 0 && alpha(bottomColor) == 0)) {
            return;
        }
        float safeRadius = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));
        if (safeRadius < 0.75F) {
            verticalGradient(matrices, x, y, width, height, topColor, bottomColor);
            return;
        }

        Matrix4f matrix = matrices.last().pose();
        RenderSystem.disableTexture();
        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);

        float midY = y + height * 0.5F;
        int midColor = mix(topColor, bottomColor, 0.5F);
        vertex(builder, matrix, x + width * 0.5F, midY, midColor);

        final int cornerSegments = 6;
        float[] centersX = {x + width - safeRadius, x + width - safeRadius,
                x + safeRadius, x + safeRadius};
        float[] centersY = {y + safeRadius, y + height - safeRadius,
                y + height - safeRadius, y + safeRadius};

        for (int corner = 0; corner < 4; corner++) {
            double start = -Math.PI * 0.5D + corner * Math.PI * 0.5D;
            for (int segment = 0; segment <= cornerSegments; segment++) {
                double angle = start + Math.PI * 0.5D * segment / cornerSegments;
                float vx = centersX[corner] + (float) Math.cos(angle) * safeRadius;
                float vy = centersY[corner] + (float) Math.sin(angle) * safeRadius;
                float t = Math.max(0.0F, Math.min(1.0F, (vy - y) / height));
                vertex(builder, matrix, vx, vy, mix(topColor, bottomColor, t));
            }
        }
        vertex(builder, matrix, x + width - safeRadius, y, topColor);
        Tessellator.getInstance().end();
        RenderSystem.enableTexture();
    }

    public static void roundedRectDiagonalGradient(MatrixStack matrices, float x, float y, float width, float height,
                                                   float radius, int startColor, int endColor) {
        if (width <= 0.0F || height <= 0.0F || (alpha(startColor) == 0 && alpha(endColor) == 0)) {
            return;
        }
        float safeRadius = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));

        Matrix4f matrix = matrices.last().pose();
        RenderSystem.disableTexture();
        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);

        float midX = x + width * 0.5F;
        float midY = y + height * 0.5F;
        int midColor = mix(startColor, endColor, 0.5F);
        vertex(builder, matrix, midX, midY, midColor);

        final int cornerSegments = 6;
        float[] centersX = {x + width - safeRadius, x + width - safeRadius,
                x + safeRadius, x + safeRadius};
        float[] centersY = {y + safeRadius, y + height - safeRadius,
                y + height - safeRadius, y + safeRadius};

        for (int corner = 0; corner < 4; corner++) {
            double start = -Math.PI * 0.5D + corner * Math.PI * 0.5D;
            for (int segment = 0; segment <= cornerSegments; segment++) {
                double angle = start + Math.PI * 0.5D * segment / cornerSegments;
                float vx = centersX[corner] + (float) Math.cos(angle) * safeRadius;
                float vy = centersY[corner] + (float) Math.sin(angle) * safeRadius;
                float normX = Math.max(0.0F, Math.min(1.0F, (vx - x) / width));
                float normY = Math.max(0.0F, Math.min(1.0F, (vy - y) / height));
                float t = Math.max(0.0F, Math.min(1.0F, (normX + normY) * 0.5F));
                vertex(builder, matrix, vx, vy, mix(startColor, endColor, t));
            }
        }
        float closingNormX = Math.max(0.0F, Math.min(1.0F, (width - safeRadius) / width));
        float closingT = closingNormX * 0.5F;
        vertex(builder, matrix, x + width - safeRadius, y, mix(startColor, endColor, closingT));
        Tessellator.getInstance().end();
        RenderSystem.enableTexture();
    }

    public static void roundedOutline(MatrixStack matrices, float x, float y, float width, float height,
                                      float radius, float thickness, int outline, int inside) {
        if (alpha(inside) > 0) {
            roundedRect(matrices, x, y, width, height, radius, inside);
        }
        drawHollowRoundedOutline(matrices, x, y, width, height, radius, thickness, outline);
    }

    public static void shadow(MatrixStack matrices, float x, float y, float width, float height, float radius) {
        // 5-pass smooth ambient drop shadow with quadratic alpha falloff
        int passes = 5;
        for (int i = passes; i >= 1; i--) {
            float spread = i * 2.0F;
            float offsetY = i * 1.5F;
            float r = radius + spread;
            float factor = (float) (passes - i + 1) / (float) passes;
            int alpha = Math.round(18.0F * factor * factor);
            roundedRect(matrices, x - spread, y - spread + offsetY, width + spread * 2.0F, height + spread * 2.0F,
                    r, withAlpha(GuiTheme.SHADOW, alpha));
        }
    }

    public static void circle(MatrixStack matrices, float centerX, float centerY, float radius, int color) {
        if (radius <= 0.0F || alpha(color) == 0) {
            return;
        }
        Matrix4f matrix = matrices.last().pose();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        int segments = 48;
        float innerRadius = Math.max(0.0F, radius - 0.5F);
        float outerRadius = radius + 0.5F;
        int zeroColor = withAlpha(color, 0);

        // 1. Inner solid fan
        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        vertex(builder, matrix, centerX, centerY, color);
        for (int i = 0; i <= segments; i++) {
            double angle = -Math.PI * 2.0D * i / segments;
            vertex(builder, matrix,
                    centerX + (float) Math.cos(angle) * innerRadius,
                    centerY + (float) Math.sin(angle) * innerRadius,
                    color);
        }
        Tessellator.getInstance().end();

        // 2. Outer AA fringe
        builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = -Math.PI * 2.0D * i / segments;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            vertex(builder, matrix, centerX + cos * innerRadius, centerY + sin * innerRadius, color);
            vertex(builder, matrix, centerX + cos * outerRadius, centerY + sin * outerRadius, zeroColor);
        }
        Tessellator.getInstance().end();

        RenderSystem.enableTexture();
    }

    public static void hueBar(MatrixStack matrices, float x, float y, float width, float height) {
        if (width <= 0.0F || height <= 0.0F) return;
        int segments = 6;
        float segmentWidth = width / segments;
        Matrix4f matrix = matrices.last().pose();
        RenderSystem.disableTexture();
        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            int leftColor = 0xFF000000 | Color.HSBtoRGB(i / (float) segments, 0.90F, 1.0F);
            int rightColor = 0xFF000000 | Color.HSBtoRGB((i + 1) / (float) segments, 0.90F, 1.0F);
            float start = x + i * segmentWidth;
            float end = i == segments - 1 ? x + width : start + segmentWidth;
            vertex(builder, matrix, start, y + height, leftColor);
            vertex(builder, matrix, end, y + height, rightColor);
            vertex(builder, matrix, end, y, rightColor);
            vertex(builder, matrix, start, y, leftColor);
        }
        Tessellator.getInstance().end();
        RenderSystem.enableTexture();
    }

    public static void switchControl(MatrixStack matrices, float x, float y, float width, float height,
                                     boolean enabled, boolean hovered) {
        int background = enabled ? GuiTheme.ACCENT : (hovered ? GuiTheme.CONTROL_HOVER : GuiTheme.CONTROL);
        roundedRect(matrices, x, y, width, height, height * 0.5F, background);
        float radius = Math.max(2.0F, height * 0.5F - 2.0F);
        float knobX = enabled ? x + width - height * 0.5F : x + height * 0.5F;
        circle(matrices, knobX, y + height * 0.5F, radius, 0xFFF8F8FA);
    }

    public static String ellipsize(FontRenderer font, String value, int maxWidth) {
        if (value == null || maxWidth <= 0) {
            return "";
        }
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String suffix = "...";
        int available = Math.max(0, maxWidth - font.width(suffix));
        return font.plainSubstrByWidth(value, available) + suffix;
    }

    public static String ellipsize(dev.ddpaura.client.render.font.FontEngine font, String value, float maxWidth) {
        if (value == null || maxWidth <= 0) {
            return "";
        }
        if (font.getStringWidth(value) <= maxWidth) {
            return value;
        }
        String suffix = "...";
        float suffW = font.getStringWidth(suffix);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (font.getStringWidth(sb.toString() + c) + suffW > maxWidth) {
                break;
            }
            sb.append(c);
        }
        return sb.toString() + suffix;
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
    }

    public static int mix(int from, int to, float progress) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        int a = Math.round(alpha(from) + (alpha(to) - alpha(from)) * clamped);
        int r = Math.round(red(from) + (red(to) - red(from)) * clamped);
        int g = Math.round(green(from) + (green(to) - green(from)) * clamped);
        int b = Math.round(blue(from) + (blue(to) - blue(from)) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void vertex(BufferBuilder builder, Matrix4f matrix, float x, float y, int color) {
        builder.vertex(matrix, x, y, 0.0F).color(red(color), green(color), blue(color), alpha(color)).endVertex();
    }

    public static int alpha(int color) {
        return color >>> 24 & 0xFF;
    }

    public static int samplePalette(int[] palette, float progress) {
        if (palette == null || palette.length == 0) return 0xFF38BDF8;
        if (palette.length == 1) return palette[0];
        float p = progress % 1.0F;
        if (p < 0.0F) p += 1.0F;
        float scaled = p * palette.length;
        int idx0 = (int) scaled;
        int idx1 = (idx0 + 1) % palette.length;
        float frac = scaled - idx0;
        return mix(palette[idx0], palette[idx1], frac);
    }

    public static void drawMeshRounded(MatrixStack matrices, float x, float y, float width, float height,
                                       float radius, int cTL, int cTR, int cBR, int cBL) {
        if (width <= 0.0F || height <= 0.0F) return;
        float safeRadius = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));

        Matrix4f matrix = matrices.last().pose();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);

        float midX = x + width * 0.5F;
        float midY = y + height * 0.5F;
        int topMid = mix(cTL, cTR, 0.5F);
        int botMid = mix(cBL, cBR, 0.5F);
        int centerColor = mix(topMid, botMid, 0.5F);
        vertex(builder, matrix, midX, midY, centerColor);

        final int cornerSegments = 6;
        float[] centersX = {x + width - safeRadius, x + width - safeRadius,
                x + safeRadius, x + safeRadius};
        float[] centersY = {y + safeRadius, y + height - safeRadius,
                y + height - safeRadius, y + safeRadius};

        for (int corner = 0; corner < 4; corner++) {
            double start = -Math.PI * 0.5D + corner * Math.PI * 0.5D;
            for (int segment = 0; segment <= cornerSegments; segment++) {
                double angle = start + Math.PI * 0.5D * segment / cornerSegments;
                float vx = centersX[corner] + (float) Math.cos(angle) * safeRadius;
                float vy = centersY[corner] + (float) Math.sin(angle) * safeRadius;
                float normX = Math.max(0.0F, Math.min(1.0F, (vx - x) / width));
                float normY = Math.max(0.0F, Math.min(1.0F, (vy - y) / height));

                int colTop = mix(cTL, cTR, normX);
                int colBot = mix(cBL, cBR, normX);
                int finalCol = mix(colTop, colBot, normY);
                vertex(builder, matrix, vx, vy, finalCol);
            }
        }
        float closingNormX = Math.max(0.0F, Math.min(1.0F, (width - safeRadius) / width));
        int closingTop = mix(cTL, cTR, closingNormX);
        vertex(builder, matrix, x + width - safeRadius, y, closingTop);
        Tessellator.getInstance().end();
        RenderSystem.enableTexture();
    }

    public static void drawHollowRoundedOutlineGradient(MatrixStack matrices, float x, float y, float width, float height,
                                                        float radius, float thickness, int cLeft, int cRight) {
        if (width <= 0.0F || height <= 0.0F || thickness <= 0.0F) return;
        float safeRadius = Math.max(0.0F, Math.min(radius, Math.min(width, height) * 0.5F));
        float innerRadius = Math.max(0.0F, safeRadius - thickness);

        Matrix4f matrix = matrices.last().pose();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        BufferBuilder builder = Tessellator.getInstance().getBuilder();
        builder.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        final int cornerSegments = 7;
        float[] centersX = {
                x + width - safeRadius,
                x + width - safeRadius,
                x + safeRadius,
                x + safeRadius
        };
        float[] centersY = {
                y + safeRadius,
                y + height - safeRadius,
                y + height - safeRadius,
                y + safeRadius
        };

        for (int corner = 0; corner < 4; corner++) {
            double start = -Math.PI * 0.5D + corner * Math.PI * 0.5D;
            for (int segment = 0; segment <= cornerSegments; segment++) {
                double angle = start + Math.PI * 0.5D * segment / cornerSegments;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);

                float outerX = centersX[corner] + cos * safeRadius;
                float outerY = centersY[corner] + sin * safeRadius;
                float innerX = centersX[corner] + cos * innerRadius;
                float innerY = centersY[corner] + sin * innerRadius;

                float normX = Math.max(0.0F, Math.min(1.0F, (outerX - x) / width));
                int vertexColor = mix(cLeft, cRight, normX);

                vertex(builder, matrix, outerX, outerY, vertexColor);
                vertex(builder, matrix, innerX, innerY, vertexColor);
            }
        }

        float closingNormX = Math.max(0.0F, Math.min(1.0F, (centersX[0] - x) / width));
        int closingColor = mix(cLeft, cRight, closingNormX);
        vertex(builder, matrix, centersX[0], y, closingColor);
        vertex(builder, matrix, centersX[0], y + thickness, closingColor);

        Tessellator.getInstance().end();
        RenderSystem.enableTexture();
    }

    public static void drawCrystalAuroraCard(MatrixStack matrices, float x, float y, float width, float height,
                                             float radius, int cLeft, int cRight, float auraAlpha) {
        if (width <= 0.0F || height <= 0.0F) return;

        // 1. Deep crystalline glass background (tinted with primary palette color)
        int darkBase = mix(0xEE090E17, cLeft, 0.12F);
        roundedRect(matrices, x, y, width, height, radius, darkBase);

        // 2. Horizon bi-chromatic mesh: luminous top fading softly downwards
        int topL = withAlpha(cLeft, Math.round(135 * auraAlpha));
        int topR = withAlpha(cRight, Math.round(105 * auraAlpha));
        int botR = withAlpha(cRight, Math.round(28 * auraAlpha));
        int botL = withAlpha(cLeft, Math.round(38 * auraAlpha));
        drawMeshRounded(matrices, x, y, width, height, radius, topL, topR, botR, botL);

        // 3. Left active LED radial bloom (soft ambient glow near status indicator)
        float dotX = x + 9.0F;
        float dotY = y + Math.min(height, 24.0F) * 0.5F;
        int bloomColor = withAlpha(cLeft, Math.round(45 * auraAlpha));
        circle(matrices, dotX, dotY, 14.0F, bloomColor);

        // 4. Polished top glass specular reflection beam (curved glass bevel sheen)
        float bevelInset = Math.max(4.0F, radius * 0.75F);
        float bevelX = x + bevelInset;
        float bevelW = width - bevelInset * 2.0F;
        float bevelY = y + 1.2F;
        if (bevelW > 6.0F) {
            int beamCenter = withAlpha(mix(cLeft, 0xFFFFFFFF, 0.55F), Math.round(130 * auraAlpha));
            int beamEdge = 0x00FFFFFF;
            horizontalGradient(matrices, bevelX, bevelY, bevelW * 0.5F, 1.0F, beamEdge, beamCenter);
            horizontalGradient(matrices, bevelX + bevelW * 0.5F, bevelY, bevelW * 0.5F, 1.0F, beamCenter, beamEdge);
        }
    }

    public static int chromaWave(long offset, float speed, float saturation, float brightness) {
        float hue = ((System.currentTimeMillis() + offset) * (speed * 0.001F)) % 1.0F;
        if (hue < 0.0F) hue += 1.0F;
        return 0xFF000000 | Color.HSBtoRGB(hue, saturation, brightness);
    }

    private static int red(int color) {
        return color >>> 16 & 0xFF;
    }

    private static int green(int color) {
        return color >>> 8 & 0xFF;
    }

    private static int blue(int color) {
        return color & 0xFF;
    }
}
