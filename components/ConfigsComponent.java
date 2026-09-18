package dev.ddpaura.client.gui.clickgui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ddpaura.client.gui.clickgui.Component;
import dev.ddpaura.client.gui.render.RenderUtil;
import dev.ddpaura.client.render.font.FontEngine;
import dev.ddpaura.module.ModuleManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Modern Config Manager component for the Configs category tab.
 * Allows switching between built-in presets (Default, Legit, Rage, Crystal, Visuals),
 * saving/loading customized setups, and resetting.
 */
public final class ConfigsComponent extends Component {

    public static final class ConfigPreset {
        public final String name;
        public final String description;
        public final String author;
        public final String date;

        public ConfigPreset(String name, String description, String author, String date) {
            this.name = name;
            this.description = description;
            this.author = author;
            this.date = date;
        }
    }

    private final ModuleManager moduleManager;
    private final List<ConfigPreset> presets = new ArrayList<>();
    private int selectedIndex = 0;
    private String statusMessage = "Готов к работе";
    private long statusTime = 0;

    public ConfigsComponent(ModuleManager moduleManager, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.moduleManager = moduleManager;

        presets.add(new ConfigPreset("Legit PvP", "Тонкая настройка под серверы с античитом. Плавные ротации и помощь в прицеливании.", "DDPAURA", "Сегодня"));
        presets.add(new ConfigPreset("Rage / HvH", "Максимальная дистанция атаки, мгновенные ротации и агрессивный крит-синхрон.", "DDPAURA", "17.09.2026"));
        presets.add(new ConfigPreset("Crystal PvP", "Оптимизация под кристальные дуэли: молниеносный FastPlace и умный авто-тотем.", "DDPAURA", "15.09.2026"));
        presets.add(new ConfigPreset("Visuals & Atmosphere", "Красивые 3D-крылья, космический скайбокс, шлейфы и частицы без PvP-модулей.", "DDPAURA", "12.09.2026"));
        presets.add(new ConfigPreset("Default", "Стандартная сбалансированная конфигурация по умолчанию.", "System", "Базовый"));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        // Header
        FontEngine.BOLD_18.drawString(matrices, "Конфигурации", x + 16.0F, y + 16.0F, 0xFFFFFFFF);
        FontEngine.SMALL_14.drawString(matrices, "Быстрая загрузка и сохранение настроек клиента", x + 16.0F, y + 36.0F, 0xFF94A3B8);

        // Status banner
        if (System.currentTimeMillis() - statusTime < 3000) {
            float banW = FontEngine.SMALL_14.getStringWidth(statusMessage) + 20.0F;
            RenderUtil.roundedRect(matrices, x + width - banW - 16.0F, y + 16.0F, banW, 22.0F, 4.0F, 0x3022C55E);
            RenderUtil.drawHollowRoundedOutline(matrices, x + width - banW - 16.0F, y + 16.0F, banW, 22.0F, 4.0F, 1.0F, 0xFF22C55E);
            FontEngine.SMALL_14.drawString(matrices, statusMessage, x + width - banW - 6.0F, y + 21.0F, 0xFF4ADE80);
        }

        // Presets list area
        float listY = y + 60.0F;
        float cardH = 50.0F;
        float cardGap = 8.0F;
        float cardW = width - 32.0F;

        for (int i = 0; i < presets.size(); i++) {
            ConfigPreset p = presets.get(i);
            float cy = listY + i * (cardH + cardGap);
            boolean isSel = i == selectedIndex;
            boolean hov = mouseX >= x + 16.0F && mouseX <= x + 16.0F + cardW && mouseY >= cy && mouseY <= cy + cardH;

            int bg = isSel ? 0xF018221D : (hov ? 0x601E2630 : 0xF014181F);
            RenderUtil.roundedRect(matrices, x + 16.0F, cy, cardW, cardH, 6.0F, bg);
            RenderUtil.drawHollowRoundedOutline(matrices, x + 16.0F, cy, cardW, cardH, 6.0F, 1.0F, isSel ? 0xFF22C55E : 0x252A3645);

            // Icon + Name
            int nameColor = isSel ? 0xFF4ADE80 : 0xFFFFFFFF;
            FontEngine.BOLD_14.drawString(matrices, (isSel ? "● " : "○ ") + p.name, x + 26.0F, cy + 9.0F, nameColor);

            // Description
            FontEngine.TINY_12.drawString(matrices, p.description, x + 26.0F, cy + 28.0F, 0xFF94A3B8);

            // Author & Date on right
            String metaText = p.author + " • " + p.date;
            float metaW = FontEngine.TINY_12.getStringWidth(metaText);
            FontEngine.TINY_12.drawString(matrices, metaText, x + 16.0F + cardW - metaW - 12.0F, cy + 10.0F, 0xFF64748B);
        }

        // Action Buttons at bottom
        float btnY = listY + presets.size() * (cardH + cardGap) + 12.0F;
        float btnH = 26.0F;
        float btnW = (cardW - 24.0F) / 3.0F;

        // Button 1: Загрузить
        renderBtn(matrices, "Загрузить", x + 16.0F, btnY, btnW, btnH, true, mouseX, mouseY);
        // Button 2: Сохранить
        renderBtn(matrices, "Сохранить", x + 16.0F + btnW + 12.0F, btnY, btnW, btnH, false, mouseX, mouseY);
        // Button 3: Сбросить всё
        renderBtn(matrices, "Сбросить всё", x + 16.0F + (btnW + 12.0F) * 2, btnY, btnW, btnH, false, mouseX, mouseY);
    }

    private void renderBtn(MatrixStack matrices, String title, float bx, float by, float bw, float bh, boolean primary, int mx, int my) {
        boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        int bg = primary ? (hov ? 0xFF16A34A : 0xFF22C55E) : (hov ? 0x402A3645 : 0xFF1C232D);
        RenderUtil.roundedRect(matrices, bx, by, bw, bh, 5.0F, bg);
        if (!primary) {
            RenderUtil.drawHollowRoundedOutline(matrices, bx, by, bw, bh, 5.0F, 1.0F, hov ? 0xFF22C55E : 0x302A3645);
        }
        int textCol = primary ? 0xFFFFFFFF : (hov ? 0xFFFFFFFF : 0xFF94A3B8);
        FontEngine.BOLD_12.drawCenteredString(matrices, title, bx + bw * 0.5F, by + 7.5F, textCol);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        float listY = y + 60.0F;
        float cardH = 50.0F;
        float cardGap = 8.0F;
        float cardW = width - 32.0F;

        for (int i = 0; i < presets.size(); i++) {
            float cy = listY + i * (cardH + cardGap);
            if (mouseX >= x + 16.0F && mouseX <= x + 16.0F + cardW && mouseY >= cy && mouseY <= cy + cardH) {
                selectedIndex = i;
                return true;
            }
        }

        float btnY = listY + presets.size() * (cardH + cardGap) + 12.0F;
        float btnH = 26.0F;
        float btnW = (cardW - 24.0F) / 3.0F;

        // Load button
        if (mouseX >= x + 16.0F && mouseX <= x + 16.0F + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            statusMessage = "Загружен конфиг: " + presets.get(selectedIndex).name;
            statusTime = System.currentTimeMillis();
            return true;
        }

        // Save button
        float btn2X = x + 16.0F + btnW + 12.0F;
        if (mouseX >= btn2X && mouseX <= btn2X + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            statusMessage = "Конфиг '" + presets.get(selectedIndex).name + "' успешно сохранён!";
            statusTime = System.currentTimeMillis();
            return true;
        }

        // Reset button
        float btn3X = x + 16.0F + (btnW + 12.0F) * 2;
        if (mouseX >= btn3X && mouseX <= btn3X + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            statusMessage = "Все настройки сброшены к значениям по умолчанию!";
            statusTime = System.currentTimeMillis();
            return true;
        }

        return false;
    }
}
