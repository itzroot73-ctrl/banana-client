package com.fusionclient.gui;

import com.fusionclient.module.Module;
import com.fusionclient.module.ModuleCategory;
import com.fusionclient.module.ModuleManager;
import com.fusionclient.settings.BooleanSetting;
import com.fusionclient.settings.ModeSetting;
import com.fusionclient.settings.Setting;
import com.fusionclient.settings.SliderSetting;
import com.fusionclient.util.AnimationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class BananaGuiScreen extends Screen {
    private final Screen parent;
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private Module selectedModule = null;
    
    private float tabAnimation = 0.0f;
    private float listAnimation = 0.0f;
    private float settingsAnimation = 0.0f;
    
    private final Map<ModuleCategory, Float> categoryExpand = new ConcurrentHashMap<>();
    private final Map<String, Float> moduleHover = new ConcurrentHashMap<>();
    
    private int scrollOffset = 0;
    private int maxScroll = 0;
    
    private static final int ORANGE_COLOR = 0xFFFFA500;
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = 0x80FFFFFF;
    private static final int DISABLED_COLOR = 0xFF252525;
    private static final int HOVER_COLOR = 0xFF404040;
    
    private static final int TAB_WIDTH = 80;
    private static final int TAB_HEIGHT = 30;
    private static final int PANEL_WIDTH = 320;
    private static final int MODULE_HEIGHT = 28;

    public BananaGuiScreen(Screen parent) {
        super(Text.literal("Banana Client"));
        this.parent = parent;
        
        for (ModuleCategory category : ModuleCategory.values()) {
            categoryExpand.put(category, 0.0f);
        }
    }

    @Override
    protected void init() {
        super.init();
        tabAnimation = 0.0f;
        listAnimation = 0.0f;
        settingsAnimation = 0.0f;
        scrollOffset = 0;
        
        for (ModuleCategory category : ModuleCategory.values()) {
            categoryExpand.put(category, category == selectedCategory ? 1.0f : 0.0f);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateAnimations(delta);
        
        int centerX = width / 2;
        int centerY = height / 2;
        
        context.fill(0, 0, width, height, 0xCC000000);
        
        renderCategoryTabs(context, mouseX, mouseY);
        
        renderModuleList(context, mouseX, mouseY);
        
        if (selectedModule != null) {
            renderSettingsPanel(context, mouseX, mouseY);
        }
        
        renderHeader(context);
    }

    private void updateAnimations(float delta) {
        float animSpeed = delta * 0.06f;
        
        if (tabAnimation < 1.0f) tabAnimation = Math.min(1.0f, tabAnimation + animSpeed);
        if (listAnimation < 1.0f) listAnimation = Math.min(1.0f, listAnimation + animSpeed);
        if (settingsAnimation > 0.0f && selectedModule == null) {
            settingsAnimation = Math.max(0.0f, settingsAnimation - animSpeed);
        } else if (settingsAnimation < 1.0f && selectedModule != null) {
            settingsAnimation = Math.min(1.0f, settingsAnimation + animSpeed);
        }
        
        for (ModuleCategory category : ModuleCategory.values()) {
            float target = category == selectedCategory ? 1.0f : 0.0f;
            float current = categoryExpand.getOrDefault(category, 0.0f);
            float newVal = current;
            
            if (current < target) {
                newVal = Math.min(target, current + animSpeed);
            } else if (current > target) {
                newVal = Math.max(target, current - animSpeed * 2.0f);
            }
            
            categoryExpand.put(category, newVal);
        }
    }

    private void renderCategoryTabs(DrawContext context, int mouseX, int mouseY) {
        int tabX = 20;
        int tabStartY = height / 2 - 150;
        
        float easedTabAnim = AnimationUtils.easeOutCubic(tabAnimation);
        
        for (ModuleCategory category : ModuleCategory.values()) {
            float expand = categoryExpand.getOrDefault(category, 0.0f);
            if (expand <= 0.0f && category != selectedCategory) continue;
            
            int currentY = tabStartY + category.ordinal() * (TAB_HEIGHT + 5);
            
            boolean isHovered = mouseX >= tabX && mouseX <= tabX + TAB_WIDTH + 30 && 
                              mouseY >= currentY && mouseY <= currentY + TAB_HEIGHT;
            boolean isSelected = category == selectedCategory;
            
            float scale = isHovered ? 1.05f : 1.0f;
            int adjustedWidth = (int)(TAB_WIDTH * scale);
            int adjustedX = tabX - (adjustedWidth - TAB_WIDTH) / 2;
            
            int bgColor = isSelected ? ORANGE_COLOR : (isHovered ? HOVER_COLOR : DISABLED_COLOR);
            
            drawRoundedRect(context, adjustedX, currentY, adjustedX + adjustedWidth, currentY + TAB_HEIGHT, bgColor);
            context.drawBorder(adjustedX, currentY, adjustedWidth, TAB_HEIGHT, ORANGE_COLOR);
            
            String name = category.getDisplayName();
            context.drawText(this.textRenderer, name, adjustedX + 8, currentY + 9, WHITE_COLOR, false);
            
            int count = ModuleManager.getInstance().getModulesByCategory(category).size();
            String countStr = String.valueOf(count);
            context.drawText(this.textRenderer, countStr, adjustedX + adjustedWidth - 15, currentY + 9, 0xFFAAAAAA, false);
        }
    }

    private void renderModuleList(DrawContext context, int mouseX, int mouseY) {
        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(selectedCategory);
        
        int panelX = 130;
        int panelY = height / 2 - 160;
        int panelWidth = PANEL_WIDTH;
        int panelHeight = 320;
        
        float slideOffset = (1.0f - listAnimation) * 30.0f;
        
        context.fill(panelX + (int)slideOffset, panelY, panelX + panelWidth + (int)slideOffset, panelY + panelHeight, BG_COLOR);
        context.drawBorder(panelX + (int)slideOffset, panelY, panelWidth, panelHeight, ORANGE_COLOR);
        
        context.drawText(this.textRenderer, selectedCategory.getDisplayName() + " (" + modules.size() + ")", 
            panelX + 15 + (int)slideOffset, panelY + 12, ORANGE_COLOR, false);
        
        context.fill(panelX + 10 + (int)slideOffset, panelY + 28, panelX + panelWidth - 10 + (int)slideOffset, panelY + 29, ORANGE_COLOR);
        
        maxScroll = Math.max(0, modules.size() * MODULE_HEIGHT - (panelHeight - 50));
        
        int listY = panelY + 40;
        int visibleCount = (panelHeight - 50) / MODULE_HEIGHT;
        
        for (int i = scrollOffset; i < Math.min(modules.size(), scrollOffset + visibleCount + 1); i++) {
            Module module = modules.get(i);
            int itemY = listY + (i - scrollOffset) * MODULE_HEIGHT;
            
            if (itemY > panelY + panelHeight - 10) break;
            
            boolean isHovered = mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && 
                              mouseY >= itemY && mouseY <= itemY + MODULE_HEIGHT;
            
            String hoverKey = module.getName() + "_hover";
            float hoverTarget = isHovered ? 1.0f : 0.0f;
            float currentHover = moduleHover.getOrDefault(hoverKey, 0.0f);
            moduleHover.put(hoverKey, currentHover + (hoverTarget - currentHover) * 0.2f);
            
            float hoverScale = 1.0f + moduleHover.getOrDefault(hoverKey, 0.0f) * 0.02f;
            int itemWidth = (int)(panelWidth - 20 * hoverScale);
            int itemX = panelX + 10 + (int)((panelWidth - 20 - itemWidth) / 2);
            
            int bgColor = module.isEnabled() ? ORANGE_COLOR : (isHovered ? HOVER_COLOR : DISABLED_COLOR);
            
            drawRoundedRect(context, itemX, itemY, itemX + itemWidth, itemY + MODULE_HEIGHT, bgColor);
            context.drawBorder(itemX, itemY, itemWidth, MODULE_HEIGHT, ORANGE_COLOR);
            
            context.drawText(this.textRenderer, module.getName(), itemX + 10, itemY + 8, WHITE_COLOR, false);
            
            if (!module.getSettings().isEmpty()) {
                String settingsIcon = "⚙";
                context.drawText(this.textRenderer, settingsIcon, itemX + itemWidth - 20, itemY + 8, 0xFFAAAAAA, false);
            }
        }
        
        if (maxScroll > 0) {
            int scrollBarHeight = (panelHeight - 50) * (panelHeight - 50) / (modules.size() * MODULE_HEIGHT);
            int scrollBarY = panelY + 40 + (scrollOffset * (panelHeight - 50 - scrollBarHeight) / maxScroll);
            
            context.fill(panelX + panelWidth - 8, panelY + 40, panelX + panelWidth - 4, panelY + panelHeight - 10, 0xFF252525);
            context.fill(panelX + panelWidth - 8, scrollBarY, panelX + panelWidth - 4, scrollBarY + scrollBarHeight, ORANGE_COLOR);
        }
        
        context.drawText(this.textRenderer, "Left-Click: Toggle  |  Right-Click: Settings  |  Scroll: Move", 
            panelX + 15, panelY + panelHeight - 15, 0xFF808080, false);
    }

    private void renderSettingsPanel(DrawContext context, int mouseX, int mouseY) {
        if (settingsAnimation <= 0.0f) return;
        
        int panelX = width - 250;
        int panelY = height / 2 - 160;
        int panelWidth = 220;
        int panelHeight = 320;
        
        float easedSettings = AnimationUtils.easeOutCubic(settingsAnimation);
        float slideOffset = (1.0f - easedSettings) * 50.0f;
        
        int renderX = panelX + (int)slideOffset;
        
        context.fill(renderX, panelY, renderX + panelWidth, panelY + panelHeight, BG_COLOR);
        context.drawBorder(renderX, panelY, panelWidth, panelHeight, ORANGE_COLOR);
        
        context.drawText(this.textRenderer, selectedModule.getName(), renderX + 15, panelY + 12, ORANGE_COLOR, false);
        
        context.fill(renderX + 10, panelY + 28, renderX + panelWidth - 10, panelY + 29, ORANGE_COLOR);
        
        List<Setting<?>> settings = selectedModule.getSettings();
        int settingY = panelY + 40;
        
        for (Setting<?> setting : settings) {
            if (settingY + 35 > panelY + panelHeight - 10) break;
            
            renderSettingControl(context, setting, renderX + 10, settingY, panelWidth - 20, mouseX, mouseY);
            settingY += 38;
        }
        
        context.drawText(this.textRenderer, "Click to toggle/slide", renderX + 15, panelY + panelHeight - 15, 0xFF808080, false);
    }

    private void renderSettingControl(DrawContext context, Setting<?> setting, int x, int y, int width, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 32;
        
        context.fill(x, y, x + width, y + 32, isHovered ? HOVER_COLOR : DISABLED_COLOR);
        context.drawBorder(x, y, width, 32, ORANGE_COLOR);
        
        context.drawText(this.textRenderer, setting.getName(), x + 8, y + 8, WHITE_COLOR, false);
        
        if (setting instanceof BooleanSetting) {
            BooleanSetting bool = (BooleanSetting) setting;
            String status = bool.isEnabled() ? "[ON]" : "[OFF]";
            context.drawText(this.textRenderer, status, x + width - 45, y + 8, bool.isEnabled() ? ORANGE_COLOR : 0xFFAAAAAA, false);
        } else if (setting instanceof SliderSetting) {
            SliderSetting slider = (SliderSetting) setting;
            String value = String.format("%.1f", slider.getValueFloat());
            context.drawText(this.textRenderer, value, x + width - 45, y + 8, WHITE_COLOR, false);
            
            float percent = (slider.getValueFloat() - slider.getMin()) / (slider.getMax() - slider.getMin());
            int barWidth = width - 16;
            context.fill(x + 8, y + 20, x + 8 + (int)(barWidth * percent), y + 24, ORANGE_COLOR);
        } else if (setting instanceof ModeSetting) {
            ModeSetting mode = (ModeSetting) setting;
            String display = "< " + mode.getValue() + " >";
            context.drawText(this.textRenderer, display, x + width - 80, y + 8, ORANGE_COLOR, false);
        }
    }

    private void drawRoundedRect(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1 + 2, y1, x2 - 2, y1 + 2, color);
        context.fill(x1, y1 + 2, x2, y2 - 2, color);
        context.fill(x1 + 2, y2 - 2, x2 - 2, y2, color);
        context.fill(x1, y1 + 2, x1 + 2, y2 - 2, color);
        context.fill(x2 - 2, y1 + 2, x2, y2 - 2, color);
    }

    private void renderHeader(DrawContext context) {
        context.drawText(this.textRenderer, "BANANA CLIENT", 10, 10, ORANGE_COLOR, false);
        
        String status = "Modules: " + ModuleManager.getInstance().getTotalModuleCount();
        context.drawText(this.textRenderer, status, 10, 22, 0xFF808080, false);
        
        context.drawText(this.textRenderer, "Shift+Right Click: Close", width - 150, 10, 0xFF808080, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = width / 2;
        
        if (button == 0) {
            int tabX = 20;
            int tabStartY = height / 2 - 150;
            
            for (ModuleCategory category : ModuleCategory.values()) {
                int currentY = tabStartY + category.ordinal() * (TAB_HEIGHT + 5);
                
                if (mouseX >= tabX && mouseX <= tabX + TAB_WIDTH + 30 && 
                    mouseY >= currentY && mouseY <= currentY + TAB_HEIGHT) {
                    selectedCategory = category;
                    selectedModule = null;
                    scrollOffset = 0;
                    return true;
                }
            }
            
            List<Module> modules = ModuleManager.getInstance().getModulesByCategory(selectedCategory);
            int panelX = 130;
            int panelY = height / 2 - 160;
            int panelWidth = PANEL_WIDTH;
            
            for (int i = scrollOffset; i < modules.size(); i++) {
                Module module = modules.get(i);
                int itemY = panelY + 40 + (i - scrollOffset) * MODULE_HEIGHT;
                
                if (mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && 
                    mouseY >= itemY && mouseY <= itemY + MODULE_HEIGHT) {
                    
                    if (mouseX >= panelX + panelWidth - 30 && !module.getSettings().isEmpty()) {
                        selectedModule = module;
                    } else {
                        ModuleManager.getInstance().toggleModule(module.getName());
                    }
                    return true;
                }
            }
            
            if (selectedModule != null) {
                int panelX2 = width - 250;
                int panelY2 = height / 2 - 160;
                int panelWidth2 = 220;
                
                if (mouseX < panelX2 - 10) {
                    selectedModule = null;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0) {
            List<Module> modules = ModuleManager.getInstance().getModulesByCategory(selectedCategory);
            int panelX = 130;
            int panelY = height / 2 - 160;
            int panelWidth = PANEL_WIDTH;
            int panelHeight = 320;
            
            if (mouseX >= panelX + panelWidth - 15 && mouseX <= panelX + panelWidth) {
                scrollOffset = (int) Math.max(0, Math.min(maxScroll, 
                    scrollOffset - deltaY / 2));
                return true;
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset + (int)(verticalAmount * 3)));
        return true;
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
