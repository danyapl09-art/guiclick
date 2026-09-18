package dev.ddpaura.setting;

public final class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String id, String name, String description, boolean defaultValue) {
        super(id, name, description, defaultValue);
    }

    public void toggle() {
        set(!get());
    }

    @Override
    protected Boolean sanitize(Boolean value) {
        return value == null ? getDefaultValue() : value;
    }
}
