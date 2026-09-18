package dev.ddpaura.client.gui.clickgui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.ddpaura.client.gui.clickgui.Component;
import dev.ddpaura.client.gui.clickgui.WinterTheme;
import dev.ddpaura.client.gui.render.RenderUtil;
import dev.ddpaura.client.render.font.FontEngine;
import dev.ddpaura.module.Category;

import java.util.function.Consumer;

public final class CategoryTabComponent extends Component {
    private final Category category;
    private final Consumer<Category> onSelect;
    private boolean selected;
    private float hoverAnimation = 0.0F;

    public CategoryTabComponent(Category category, boolean selected, Consumer<Category> onSelect, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.category = category;
        this.selected = selected;
        this.onSelect = onSelect;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        boolean hovered = isHovered(mouseX, mouseY);
        hoverAnimation += ((hovered ? 1.0F : 0.0F) - hoverAnimation) * 0.22F;

        if (selected) {
            // Selected glowing capsule pill
            RenderUtil.roundedRect(matrices, x, y, width, height, 4.0F, RenderUtil.withAlpha(WinterTheme.ACCENT_CYAN, 45));
            RenderUtil.roundedOutline(matrices, x, y, width, height, 4.0F, 1.0F, WinterTheme.ACCENT_CYAN, RenderUtil.withAlpha(WinterTheme.ACCENT_CYAN, 30));
            FontEngine.BOLD_18.drawCenteredString(matrices, category.getDisplayName(), x + width * 0.5F, y + (height - FontEngine.BOLD_18.getFontHeight()) * 0.5F + 1.0F, WinterTheme.TEXT_WHITE);
        } else {
            // Idle or hovered tab
            int bgColor = (hoverAnimation > 0.05F) ? RenderUtil.withAlpha(WinterTheme.ACCENT_CYAN, (int) (22 * hoverAnimation)) : 0x00000000;
            if (bgColor != 0) {
                RenderUtil.roundedRect(matrices, x, y, width, height, 4.0F, bgColor);
            }
            int outlineColor = (hoverAnimation > 0.05F) ? RenderUtil.withAlpha(WinterTheme.ACCENT_CYAN, (int) (50 * hoverAnimation)) : 0;
            if (outlineColor != 0) {
                RenderUtil.roundedOutline(matrices, x, y, width, height, 4.0F, 1.0F, outlineColor, 0);
            }
            int textColor = (hoverAnimation > 0.05F) ? WinterTheme.TEXT_WHITE : WinterTheme.TEXT_MUTED;
            FontEngine.BOLD_18.drawCenteredString(matrices, category.getDisplayName(), x + width * 0.5F, y + (height - FontEngine.BOLD_18.getFontHeight()) * 0.5F + 1.0F, textColor);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            if (onSelect != null) {
                onSelect.accept(category);
            }
            return true;
        }
        return false;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public Category getCategory() {
        return category;
    }
}
