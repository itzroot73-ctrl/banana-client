package com.fusionclient.settings;

public class SliderSetting extends Setting<Number> {
    private final float min;
    private final float max;
    private final float step;

    public SliderSetting(String name, String description, float defaultValue, float min, float max, float step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float getStep() {
        return step;
    }

    public float getValueFloat() {
        return value.floatValue();
    }

    public void setValueFloat(float value) {
        float newValue = Math.max(min, Math.min(max, value));
        newValue = Math.round(newValue / step) * step;
        this.value = newValue;
    }
}
