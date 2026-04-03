package com.fusionclient.settings;

public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public boolean isEnabled() {
        return value;
    }

    public void toggle() {
        this.value = !this.value;
    }
}
