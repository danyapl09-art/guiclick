package dev.ddpaura.client.gui.clickgui;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ddpaura.client.gui.render.RenderUtil;

import java.util.Random;

public final class WinterSnowParticles {
    private static final int PARTICLE_COUNT = 85;

    private static final class Particle {
        float x;
        float y;
        float size;
        float speedY;
        float sway;
        float swaySpeed;
        float baseAlpha;
        int color;
    }

    private final Particle[] particles = new Particle[PARTICLE_COUNT];
    private final Random random = new Random(42L);
    private boolean initialized = false;

    public WinterSnowParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles[i] = new Particle();
        }
    }

    public void updateAndRender(MatrixStack matrices, int width, int height, int mouseX, int mouseY) {
        if (!initialized && width > 0 && height > 0) {
            for (Particle f : particles) {
                resetParticle(f, width, height, true);
            }
            initialized = true;
        }

        float mouseNormX = (mouseX / (float) Math.max(1, width)) - 0.5F;
        ClickGuiTheme theme = ThemeManager.getTheme();

        for (Particle f : particles) {
            f.y += f.speedY;
            f.sway += f.swaySpeed;
            f.x += (float) Math.sin(f.sway) * 0.45F + mouseNormX * 0.25F;

            if (f.y > height + 8.0F || f.x < -10.0F || f.x > width + 10.0F) {
                resetParticle(f, width, height, false);
            }

            int alphaInt = (int) (f.baseAlpha * 255.0F);
            int pColor = (alphaInt << 24) | (f.color & 0xFFFFFF);

            // Draw crisp glowing particle point
            RenderUtil.rect(matrices, f.x, f.y, f.size, f.size, pColor);
            if (f.size > 2.0F) {
                // Cross sparkle on larger particles
                int sparkleColor = ((alphaInt / 2) << 24) | (theme.accentLight & 0xFFFFFF);
                RenderUtil.rect(matrices, f.x - 1.0F, f.y + (f.size * 0.5F) - 0.5F, f.size + 2.0F, 1.0F, sparkleColor);
                RenderUtil.rect(matrices, f.x + (f.size * 0.5F) - 0.5F, f.y - 1.0F, 1.0F, f.size + 2.0F, sparkleColor);
            }
        }
    }

    private void resetParticle(Particle f, int width, int height, boolean initialScatter) {
        ClickGuiTheme theme = ThemeManager.getTheme();
        f.x = random.nextFloat() * Math.max(1.0F, width);
        f.y = initialScatter ? random.nextFloat() * Math.max(1.0F, height) : -6.0F - random.nextFloat() * 14.0F;
        f.size = 1.0F + random.nextFloat() * 2.4F;
        f.speedY = 0.45F + random.nextFloat() * 1.1F;
        f.sway = random.nextFloat() * 6.28F;
        f.swaySpeed = 0.02F + random.nextFloat() * 0.035F;
        f.baseAlpha = 0.25F + random.nextFloat() * 0.55F;
        f.color = random.nextFloat() > 0.45F ? theme.particleColor1 : theme.particleColor2;
    }
}
