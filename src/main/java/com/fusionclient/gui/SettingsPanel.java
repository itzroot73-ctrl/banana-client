package com.fusionclient.gui;

import com.fusionclient.module.Module;
import com.fusionclient.settings.BooleanSetting;
import com.fusionclient.settings.ModeSetting;
import com.fusionclient.settings.Setting;
import com.fusionclient.settings.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SettingsPanel {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 300;
    private static final int ITEM_HEIGHT = 35;
    private static final int BG_COLOR = 0x80FFFFFF;
    private static final int ORANGE_COLOR = 0xFFFFA500;
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    private static final int DISABLED_COLOR = 0xFF303030;
    private static final int HOVER_COLOR = 0xFF505050;
    
    private final Screen parent;
    private Module selectedModule;
    private final ConcurrentMap<Module, Float> panelAnimations = new ConcurrentHashMap<>();
    private float globalAnim = 0.0f;
    
    public SettingsPanel(Screen parent) {
        this.parent = parent;
    }
    
    public void setSelectedModule(Module module) {
        if (this.selectedModule != module) {
            this.selectedModule = module;
            panelAnimations.put(module, 0.0f);
        }
    }
    
    public Module getSelectedModule() {
        return selectedModule;
    }
    
    public void update(float delta) {
        if (globalAnim < 1.0f) {
            globalAnim += delta * 0.05f;
            if (globalAnim > 1.0f) globalAnim = 1.0f;
        }
        
        for (Module module : panelAnimations.keySet()) {
            float anim = panelAnimations.get(module);
            if (anim < 1.0f) {
                panelAnimations.put(module, Math.min(1.0f, anim + delta * 0.08f));
            }
        }
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta, int screenWidth, int screenHeight) {
        if (selectedModule == null) return;
        
        update(delta);
        
        float slideOffset = (1.0f - globalAnim) * 50.0f;
        int panelX = screenWidth - PANEL_WIDTH - 20 + (int)slideOffset;
        int panelY = screenHeight / 2 - PANEL_HEIGHT / 2;
        
        int panelInnerWidth = PANEL_WIDTH - 8;
        
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, BG_COLOR);
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, ORANGE_COLOR);
        
        context.drawText(MinecraftClient.getInstance().textRenderer, selectedModule.getName(), panelX + 10, panelY + 10, ORANGE_COLOR, false);
        
        context.fill(panelX + 5, panelY + 25, panelX + PANEL_WIDTH - 5, panelY + 26, ORANGE_COLOR);
        
        List<Setting<?>> settings = selectedModule.getSettings();
        int settingY = panelY + 35;
        
        for (Setting<?> setting : settings) {
            if (settingY + ITEM_HEIGHT > panelY + PANEL_HEIGHT - 10) break;
            
            renderSetting(context, setting, panelX + 5, settingY, panelInnerWidth, mouseX, mouseY);
            settingY += ITEM_HEIGHT;
        }
        
        context.drawText(MinecraftClient.getInstance().textRenderer, "Click to toggle", panelX + 10, panelY + PANEL_HEIGHT - 15, 0xFF808080, false);
    }
    
    private void renderSetting(DrawContext context, Setting<?> setting, int x, int y, int width, int mouseX, int mouseY) {
        boolean isHovered = isMouseOver(mouseX, mouseY, x, y, width, ITEM_HEIGHT - 5);
        
        context.fill(x, y, x + width, y + ITEM_HEIGHT - 5, isHovered ? HOVER_COLOR : DISABLED_COLOR);
        context.drawBorder(x, y, width, ITEM_HEIGHT - 5, ORANGE_COLOR);
        
        context.drawText(MinecraftClient.getInstance().textRenderer, setting.getName(), x + 8, y + 8, WHITE_COLOR, false);
        
        if (setting instanceof BooleanSetting) {
            BooleanSetting boolSetting = (BooleanSetting) setting;
            String status = boolSetting.isEnabled() ? "[ON]" : "[OFF]";
            context.drawText(MinecraftClient.getInstance().textRenderer, status, x + width - 40, y + 8, boolSetting.isEnabled() ? ORANGE_COLOR : 0xFFAAAAAA, false);
        } else if (setting instanceof SliderSetting) {
            SliderSetting slider = (SliderSetting) setting;
            String value = String.format("%.1f", slider.getValueFloat());
            context.drawText(MinecraftClient.getInstance().textRenderer, value, x + width - 50, y + 8, WHITE_COLOR, false);
            
            float percent = (slider.getValueFloat() - slider.getMin()) / (slider.getMax() - slider.getMin());
            int sliderWidth = width - 16;
            int sliderX = x + 8;
            int sliderY = y + 20;
            
            context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + 4, 0xFF303030);
            context.fill(sliderX, sliderY, sliderX + (int)(sliderWidth * percent), sliderY + 4, ORANGE_COLOR);
        } else if (setting instanceof ModeSetting) {
            ModeSetting mode = (ModeSetting) setting;
            String display = "< " + mode.getValue() + " >";
            context.drawText(MinecraftClient.getInstance().textRenderer, display, x + width - 80, y + 8, ORANGE_COLOR, false);
        }
    }
    
    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public boolean handleClick(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (selectedModule == null) return false;
        
        float slideOffset = (1.0f - globalAnim) * 50.0f;
        int panelX = screenWidth - PANEL_WIDTH - 20 + (int)slideOffset;
        int panelY = screenHeight / 2 - PANEL_HEIGHT / 2;
        
        List<Setting<?>> settings = selectedModule.getSettings();
        int settingY = panelY + 35;
        
        for (Setting<?> setting : settings) {
            if (settingY + ITEM_HEIGHT > panelY + PANEL_HEIGHT - 10) break;
            
            if (isMouseOver(mouseX, mouseY, panelX + 5, settingY, PANEL_WIDTH - 8, ITEM_HEIGHT - 5)) {
                if (setting instanceof BooleanSetting) {
                    ((BooleanSetting) setting).toggle();
                } else if (setting instanceof ModeSetting) {
                    ((ModeSetting) setting).cycleMode();
                } else if (setting instanceof SliderSetting) {
                    SliderSetting slider = (SliderSetting) setting;
                    double relativeX = mouseX - (panelX + 8);
                    float percent = (float)(relativeX / (PANEL_WIDTH - 16));
                    float newValue = slider.getMin() + percent * (slider.getMax() - slider.getMin());
                    slider.setValueFloat(newValue);
                }
                return true;
            }
            settingY += ITEM_HEIGHT;
        }
        
        return false;
    }
}
