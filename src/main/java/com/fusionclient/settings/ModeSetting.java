package com.fusionclient.settings;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;
    private int selectedIndex;

    public ModeSetting(String name, String description, String defaultMode, String... modes) {
        super(name, description, defaultMode);
        this.modes = Arrays.asList(modes);
        this.selectedIndex = this.modes.indexOf(defaultMode);
        if (selectedIndex < 0) selectedIndex = 0;
    }

    public List<String> getModes() {
        return modes;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public String getValue() {
        return modes.get(selectedIndex);
    }

    public void setMode(String mode) {
        int index = modes.indexOf(mode);
        if (index >= 0) {
            this.selectedIndex = index;
            this.value = mode;
        }
    }

    public void setIndex(int index) {
        if (index >= 0 && index < modes.size()) {
            this.selectedIndex = index;
            this.value = modes.get(index);
        }
    }

    public void cycleMode() {
        selectedIndex = (selectedIndex + 1) % modes.size();
        this.value = modes.get(selectedIndex);
    }
}
