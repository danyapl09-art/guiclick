package dev.ddpaura.module;

import dev.ddpaura.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;

    protected Module(String id, String name, String description, Category category) {
        if (id == null || !id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Module id must use lowercase letters, numbers and underscores");
        }
        if (name == null || name.trim().isEmpty() || description == null
                || description.trim().isEmpty() || category == null) {
            throw new IllegalArgumentException("Module metadata must not be empty");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected final <S extends Setting<?>> S add(S setting) {
        if (setting == null || getSetting(setting.getId()) != null) {
            throw new IllegalArgumentException("Duplicate or null setting in module " + id);
        }
        settings.add(setting);
        return setting;
    }

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        boolean previous = this.enabled;
        this.enabled = enabled;
        try {
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
        } catch (RuntimeException exception) {
            this.enabled = previous;
            try {
                if (enabled) onDisable(); else onEnable();
            } catch (RuntimeException rollbackFailure) {
                exception.addSuppressed(rollbackFailure);
            }
            throw exception;
        }
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public final String getId() {
        return id;
    }

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public final Category getCategory() {
        return category;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public final Setting<?> getSetting(String id) {
        for (Setting<?> setting : settings) {
            if (setting.getId().equals(id)) {
                return setting;
            }
        }
        return null;
    }

    private int key = -1;

    public final int getKey() {
        return key;
    }

    public final void setKey(int key) {
        this.key = key;
    }

    public final String getKeyName() {
        if (key <= 0) {
            return "NONE";
        }
        if (key >= 1000) {
            return "M" + (key - 1000);
        }
        switch (key) {
            case 32: return "SPACE";
            case 256: return "ESC";
            case 257: return "ENTER";
            case 258: return "TAB";
            case 259: return "BKSP";
            case 260: return "INS";
            case 261: return "DEL";
            case 262: return "RIGHT";
            case 263: return "LEFT";
            case 264: return "DOWN";
            case 265: return "UP";
            case 290: return "F1";
            case 291: return "F2";
            case 292: return "F3";
            case 293: return "F4";
            case 294: return "F5";
            case 295: return "F6";
            case 296: return "F7";
            case 297: return "F8";
            case 298: return "F9";
            case 299: return "F10";
            case 300: return "F11";
            case 301: return "F12";
            case 340: return "LSHIFT";
            case 341: return "LCTRL";
            case 342: return "LALT";
            case 344: return "RSHIFT";
            case 345: return "RCTRL";
            case 346: return "RALT";
            case 348: return "MENU";
            default:
                try {
                    String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, 0);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.toUpperCase(java.util.Locale.ROOT);
                    }
                } catch (Throwable ignored) {
                }
                return "KEY_" + key;
        }
    }
}
