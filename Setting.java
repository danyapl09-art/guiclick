package dev.ddpaura.setting;

public abstract class Setting<T> {
    private final String id;
    private final String name;
    private final String description;
    private final T defaultValue;
    private T value;

    protected Setting(String id, String name, String description, T defaultValue) {
        if (id == null || !id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Setting id must use lowercase letters, numbers and underscores");
        }
        if (name == null || name.trim().isEmpty() || description == null
                || description.trim().isEmpty() || defaultValue == null) {
            throw new IllegalArgumentException("Setting metadata and default value must not be empty");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    private boolean quick = false;
    private String tab = "Общее";
    private String group = "Основные параметры";
    private String unit = "";
    private boolean stepper = false;

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S quick() {
        this.quick = true;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S quick(boolean quick) {
        this.quick = quick;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S tab(String tab) {
        if (tab != null && !tab.trim().isEmpty()) {
            this.tab = tab;
        }
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S group(String group) {
        if (group != null && !group.trim().isEmpty()) {
            this.group = group;
        }
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S unit(String unit) {
        this.unit = unit == null ? "" : unit;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S stepper(boolean stepper) {
        this.stepper = stepper;
        return (S) this;
    }

    public boolean isQuick() {
        return quick;
    }

    public String getTab() {
        return tab;
    }

    public String getGroup() {
        return group;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isStepper() {
        return stepper;
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

    public final T getDefaultValue() {
        return defaultValue;
    }

    public final T get() {
        return value;
    }

    public final void set(T value) {
        this.value = sanitize(value);
    }

    public final void reset() {
        value = sanitize(defaultValue);
    }

    protected abstract T sanitize(T value);
}
