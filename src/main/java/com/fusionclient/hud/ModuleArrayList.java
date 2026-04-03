package com.fusionclient.hud;

import com.fusionclient.module.Module;
import com.fusionclient.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ModuleArrayList {
    private static ModuleArrayList instance;
    private final MinecraftClient client;
    
    private final Map<String, ModuleEntry> moduleEntries = new ConcurrentHashMap<>();
    private final List<String> activeModules = new ArrayList<>();
    
    private static final int ORANGE_COLOR = 0xFFFFA500;
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = 0x80FFFFFF;
    private static final int DISABLED_COLOR = 0xFF252525;
    private static final int HOVER_COLOR = 0xFF353535;
    
    private static final int ENTRY_HEIGHT = 18;
    private static final int ENTRY_WIDTH = 130;

    private ModuleArrayList() {
        this.client = MinecraftClient.getInstance();
    }
    
    public static ModuleArrayList getInstance() {
        if (instance == null) {
            instance = new ModuleArrayList();
        }
        return instance;
    }

    public void render(DrawContext context, int screenWidth, int screenHeight) {
        updateModuleList();
        
        int listX = screenWidth - ENTRY_WIDTH - 10;
        int listY = 50;
        
        for (String moduleName : activeModules) {
            ModuleEntry entry = moduleEntries.get(moduleName);
            if (entry == null) {
                entry = new ModuleEntry(moduleName);
                moduleEntries.put(moduleName, entry);
            }
            
            entry.update();
            
            if (entry.isVisible()) {
                int currentX = listX + (int)(entry.getOffset() * (ENTRY_WIDTH + 10));
                
                context.fill(currentX, listY, currentX + ENTRY_WIDTH, listY + ENTRY_HEIGHT, BG_COLOR);
                
                context.fill(currentX, listY, currentX + 3, listY + ENTRY_HEIGHT, ORANGE_COLOR);
                
                context.drawBorder(currentX, listY, ENTRY_WIDTH, ENTRY_HEIGHT, 0x40FFFFFF);
                
                context.drawText(client.textRenderer, moduleName, currentX + 8, listY + 4, WHITE_COLOR, false);
                
                listY += ENTRY_HEIGHT + 2;
            }
        }
        
        renderPotionEffects(context, screenWidth, screenHeight);
    }
    
    private void updateModuleList() {
        List<Module> enabledModules = ModuleManager.getInstance().getEnabledModules();
        
        List<String> currentNames = new ArrayList<>();
        for (Module module : enabledModules) {
            currentNames.add(module.getName());
        }
        
        for (String name : new ArrayList<>(moduleEntries.keySet())) {
            if (!currentNames.contains(name)) {
                ModuleEntry entry = moduleEntries.get(name);
                if (entry != null) {
                    entry.setVisible(false);
                }
            }
        }
        
        activeModules.clear();
        activeModules.addAll(currentNames);
        
        for (String name : currentNames) {
            ModuleEntry entry = moduleEntries.get(name);
            if (entry == null) {
                entry = new ModuleEntry(name);
                moduleEntries.put(name, entry);
            }
            entry.setVisible(true);
        }
    }
    
    private void renderPotionEffects(DrawContext context, int screenWidth, int screenHeight) {
        if (client.player == null) return;
        
        List<StatusEffectInstance> effects = new ArrayList<>(client.player.getStatusEffects());
        if (effects.isEmpty()) return;
        
        int startY = client.player != null ? screenHeight - 100 : 50;
        
        int iconSize = 24;
        int iconY = startY;
        
        for (StatusEffectInstance effect : effects) {
            if (iconY > screenHeight - 50) break;
            
            int iconX = 15;
            
            context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, BG_COLOR);
            context.drawBorder(iconX, iconY, iconSize, iconSize, ORANGE_COLOR);
            
            context.drawText(client.textRenderer, getEffectSymbol(effect.getEffectType()), iconX + 6, iconY + 5, ORANGE_COLOR, false);
            
            int duration = effect.getDuration();
            int seconds = duration / 20;
            String timeText = formatTime(seconds);
            
            if (duration < 300) {
                context.drawText(client.textRenderer, timeText, iconX + 2, iconY + 14, 0xFFFF5555, false);
            } else {
                context.drawText(client.textRenderer, timeText, iconX + 2, iconY + 14, WHITE_COLOR, false);
            }
            
            iconY += iconSize + 4;
        }
    }
    
    private String getEffectSymbol(StatusEffect effect) {
        if (effect == StatusEffects.SPEED) return "S";
        if (effect == StatusEffects.SLOWNESS) return "s";
        if (effect == StatusEffects.HASTE) return "H";
        if (effect == StatusEffects.MINING_FATIGUE) return "M";
        if (effect == StatusEffects.STRENGTH) return "St";
        if (effect == StatusEffects.INSTANT_HEALTH) return "+";
        if (effect == StatusEffects.INSTANT_DAMAGE) return "-";
        if (effect == StatusEffects.JUMP_BOOST) return "J";
        if (effect == StatusEffects.NAUSEA) return "N";
        if (effect == StatusEffects.REGENERATION) return "R";
        if (effect == StatusEffects.RESISTANCE) return "Rs";
        if (effect == StatusEffects.FIRE_RESISTANCE) return "Fr";
        if (effect == StatusEffects.WATER_BREATHING) return "W";
        if (effect == StatusEffects.INVISIBILITY) return "I";
        if (effect == StatusEffects.BLINDNESS) return "B";
        if (effect == StatusEffects.NIGHT_VISION) return "Nv";
        if (effect == StatusEffects.GLOWING) return "G";
        if (effect == StatusEffects.LEVITATION) return "L";
        if (effect == StatusEffects.LUCK) return "L";
        if (effect == StatusEffects.UNLUCK) return "Ul";
        return "?";
    }
    
    private String formatTime(int seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m";
        return (seconds / 3600) + "h";
    }
    
    public void onModuleToggle(Module module) {
        String name = module.getName();
        if (module.isEnabled()) {
            ModuleEntry entry = new ModuleEntry(name);
            entry.setVisible(true);
            moduleEntries.put(name, entry);
        } else {
            ModuleEntry entry = moduleEntries.get(name);
            if (entry != null) {
                entry.setVisible(false);
            }
        }
    }
    
    private static class ModuleEntry {
        private final String name;
        private boolean visible = false;
        private float offset = 1.0f;
        
        public ModuleEntry(String name) {
            this.name = name;
        }
        
        public void update() {
            float targetOffset = visible ? 0.0f : 1.0f;
            offset += (targetOffset - offset) * 0.2f;
        }
        
        public boolean isVisible() {
            return visible || offset < 0.99f;
        }
        
        public void setVisible(boolean visible) {
            this.visible = visible;
        }
        
        public float getOffset() {
            return offset;
        }
    }
}
