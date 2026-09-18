package dev.ddpaura.module;

public enum Category {
    COMBAT("Combat", "⚔", "Модули для PVP и сражений"),
    MOVEMENT("Movement", "🏃", "Модули для перемещения и скорости"),
    RENDER("Render", "👁", "Визуальные эффекты и кастомизация"),
    PLAYER("Player", "👤", "Улучшения персонажа и взаимодействие"),
    MISC("Misc", "⊞", "Вспомогательные утилиты и инструменты"),
    CONFIGS("Configs", "⚙", "Управление пресетами и настройками");

    public static final Category VISUAL = RENDER;

    private final String displayName;
    private final String glyph;
    private final String description;

    Category(String displayName, String glyph, String description) {
        this.displayName = displayName;
        this.glyph = glyph;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGlyph() {
        return glyph;
    }

    public String getDescription() {
        return description;
    }
}
