package com.fusionclient.module;

public enum ModuleCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    WORLD("World"),
    PLAYER("Player"),
    INVENTORY("Inventory"),
    CHAT("Chat"),
    EXPLOIT("Exploit"),
    MISC("Misc");

    private final String displayName;
    private final String icon;

    ModuleCategory(String displayName) {
        this.displayName = displayName;
        this.icon = getDefaultIcon(displayName);
    }

    private String getDefaultIcon(String name) {
        switch (name) {
            case "Combat": return "⚔";
            case "Movement": return "⚡";
            case "Render": return "👁";
            case "World": return "🌍";
            case "Player": return "👤";
            case "Inventory": return "🎒";
            case "Chat": return "💬";
            case "Exploit": return "💣";
            case "Misc": return "⚙";
            default: return "•";
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }
}
