package com.fusionclient.module;

import com.fusionclient.config.ConfigManager;
import com.fusionclient.settings.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Module {
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private boolean enabled;
    private final List<Setting<?>> settings;
    private final List<Consumer<Module>> listeners;
    
    private int keybind;
    private boolean visible;

    public Module(String name, String description, ModuleCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = false;
        this.settings = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.keybind = -1;
        this.visible = true;
    }

    public Module(String name, ModuleCategory category) {
        this(name, "", category);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getKeybind() {
        return keybind;
    }

    public void setKeybind(int keybind) {
        this.keybind = keybind;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
                notifyListeners(true);
            } else {
                onDisable();
                notifyListeners(false);
            }
            ConfigManager.getInstance().saveAsync();
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public void addSetting(Setting<?> setting) {
        this.settings.add(setting);
    }

    public <T extends Setting<?>> T getSetting(String name) {
        return (T) settings.stream()
            .filter(s -> s.getName().equals(name))
            .findFirst()
            .orElse(null);
    }

    public void addListener(Consumer<Module> listener) {
        this.listeners.add(listener);
    }

    private void notifyListeners(boolean enabled) {
        for (Consumer<Module> listener : listeners) {
            listener.accept(this);
        }
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onRender() {}
    public void onKeybind() {}
    
    public boolean isModule() {
        return true;
    }
}
