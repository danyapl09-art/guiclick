package dev.ddpaura.setting;

public final class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String id, String name, String description, double defaultValue,
                         double min, double max, double step) {
        super(id, name, description, defaultValue);
        if (!Double.isFinite(min) || !Double.isFinite(max) || !Double.isFinite(step)
                || max <= min || step <= 0.0D) {
            throw new IllegalArgumentException("Invalid numeric range");
        }
        if (!Double.isFinite(defaultValue) || defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("Default value must be inside the numeric range");
        }
        this.min = min;
        this.max = max;
        this.step = step;
        set(defaultValue);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public double normalized() {
        return Math.max(0.0D, Math.min(1.0D, (get() - min) / (max - min)));
    }

    public void setNormalized(double normalized) {
        set(min + Math.max(0.0D, Math.min(1.0D, normalized)) * (max - min));
    }

    public void increment() {
        set(Math.min(max, get() + step));
    }

    public void decrement() {
        set(Math.max(min, get() - step));
    }

    @Override
    protected Double sanitize(Double value) {
        double safe = value == null || Double.isNaN(value) || Double.isInfinite(value)
                ? getDefaultValue() : value;
        safe = Math.max(min, Math.min(max, safe));
        double stepped = min + Math.round((safe - min) / step) * step;
        return Math.max(min, Math.min(max, stepped));
    }
}
