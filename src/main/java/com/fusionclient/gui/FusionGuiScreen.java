package com.fusionclient.gui;

import com.fusionclient.module.Module;
import com.fusionclient.module.ModuleCategory;
import com.fusionclient.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FusionGuiScreen extends Screen {
    private float animationProgress = 0.0f;
    private final Screen parent;
    private final Map<ModuleCategory, Float> categoryAnimations = new ConcurrentHashMap<>();
    private final SettingsPanel settingsPanel;
    
    private static final int BG_COLOR = 0x80FFFFFF;
    private static final int ORANGE_COLOR = 0xFFFFA500;
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    private static final int ENABLED_COLOR = 0xFFFF8C00;
    private static final int DISABLED_COLOR = 0xFF303030;
    private static final int HOVER_COLOR = 0xFF505050;
    
    private static final int PANEL_WIDTH = 180;
    private static final int PANEL_HEIGHT = 220;
    private static final int BUTTON_HEIGHT = 28;
    private static final int BUTTON_SPACING = 32;
    private static final int CATEGORY_HEADER_HEIGHT = 25;

    public FusionGuiScreen(Screen parent) {
        super(Text.literal("Fusion Client"));
        this.parent = parent;
        this.settingsPanel = new SettingsPanel(this);
        
        for (ModuleCategory category : ModuleCategory.values()) {
            categoryAnimations.put(category, 0.0f);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.animationProgress = 0.0f;
        
        for (ModuleCategory category : ModuleCategory.values()) {
            categoryAnimations.put(category, 0.0f);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (animationProgress < 1.0f) {
            animationProgress += delta * 0.04f;
            if (animationProgress > 1.0f) animationProgress = 1.0f;
        }
        
        for (ModuleCategory category : ModuleCategory.values()) {
            Float catAnim = categoryAnimations.get(category);
            if (catAnim < 1.0f) {
                categoryAnimations.put(category, Math.min(1.0f, catAnim + delta * 0.05f));
            }
        }

        float scale = 0.85f + (0.15f * animationProgress);
        
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1.0f);

        renderCategoryPanel(context, ModuleCategory.COMBAT, mouseX, mouseY);
        renderCategoryPanel(context, ModuleCategory.MOVEMENT, mouseX, mouseY);
        renderCategoryPanel(context, ModuleCategory.VISUAL, mouseX, mouseY);

        renderHeader(context);
        
        GL11.glPopMatrix();
        
        settingsPanel.render(context, mouseX, mouseY, delta, width, height);
    }

    private void renderHeader(int y) {
    }

    private void renderCategoryPanel(DrawContext context, ModuleCategory category, int mouseX, int mouseY) {
        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
        
        int panelX = getPanelX(category);
        int panelY = height / 2 - PANEL_HEIGHT / 2;
        
        float anim = categoryAnimations.getOrDefault(category, 0.0f);
        float slideOffset = (1.0f - anim) * 50.0f;
        
        panelX += slideOffset;
        
        int panelInnerWidth = PANEL_WIDTH - 8;
        
        context.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, BG_COLOR);
        
        context.drawBorder(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, ORANGE_COLOR);
        
        int headerY = panelY + 5;
        context.drawText(this.textRenderer, category.getDisplayName(), panelX + 10, headerY, ORANGE_COLOR, false);
        
        context.fill(panelX + 5, panelY + CATEGORY_HEADER_HEIGHT, panelX + PANEL_WIDTH - 5, panelY + CATEGORY_HEADER_HEIGHT + 1, ORANGE_COLOR);
        
        int buttonY = panelY + CATEGORY_HEADER_HEIGHT + 10;
        
        for (Module module : modules) {
            renderModuleButton(context, module, panelX + 5, buttonY, panelInnerWidth, mouseX, mouseY);
            buttonY += BUTTON_SPACING;
        }
        
        context.drawText(this.textRenderer, "L-Click: Toggle", panelX + 25, panelY + PANEL_HEIGHT - 35, 0xFF808080, false);
        context.drawText(this.textRenderer, "R-Click: Settings", panelX + 25, panelY + PANEL_HEIGHT - 20, 0xFF808080, false);
    }

    private int getPanelX(ModuleCategory category) {
        int centerX = width / 2;
        int panelSpacing = PANEL_WIDTH + 20;
        
        switch (category) {
            case COMBAT:
                return centerX - panelSpacing - PANEL_WIDTH / 2;
            case MOVEMENT:
                return centerX - PANEL_WIDTH / 2;
            case VISUAL:
                return centerX + panelSpacing - PANEL_WIDTH / 2;
            default:
                return centerX;
        }
    }

    private void renderModuleButton(DrawContext context, Module module, int x, int y, int width, int mouseX, int mouseY) {
        boolean isEnabled = module.isEnabled();
        boolean isHovered = isMouseOverButton(mouseX, mouseY, x, y, width, BUTTON_HEIGHT);
        
        int bgColor = isEnabled ? ENABLED_COLOR : (isHovered ? HOVER_COLOR : DISABLED_COLOR);
        
        drawRoundedRect(context, x, y, x + width, y + BUTTON_HEIGHT, bgColor);
        
        context.drawBorder(x, y, width, BUTTON_HEIGHT, ORANGE_COLOR);
        
        int textColor = isEnabled ? WHITE_COLOR : 0xFFAAAAAA;
        context.drawText(this.textRenderer, module.getName(), x + 8, y + 8, textColor, false);
        
        String status = isEnabled ? "[ON]" : "[OFF]";
        int statusWidth = textRenderer.getWidth(status);
        context.drawText(this.textRenderer, status, x + width - statusWidth - 8, y + 8, WHITE_COLOR, false);
    }

    private void drawRoundedRect(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1 + 2, y1, x2 - 2, y1 + 2, color);
        context.fill(x1, y1 + 2, x2, y2 - 2, color);
        context.fill(x1 + 2, y2 - 2, x2 - 2, y2, color);
        
        context.fill(x1, y1 + 2, x1 + 2, y2 - 2, color);
        context.fill(x2 - 2, y1 + 2, x2, y2 - 2, color);
    }

    private boolean isMouseOverButton(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (settingsPanel.handleClick(mouseX, mouseY, width, height)) {
                return true;
            }
            
            for (ModuleCategory category : ModuleCategory.values()) {
                List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
                int panelX = getPanelX(category);
                float anim = categoryAnimations.getOrDefault(category, 0.0f);
                panelX += (1.0f - anim) * 50.0f;
                
                int panelY = height / 2 - PANEL_HEIGHT / 2;
                int buttonY = panelY + CATEGORY_HEADER_HEIGHT + 10;
                
                for (Module module : modules) {
                    if (isMouseOverButton(mouseX, mouseY, panelX + 5, buttonY, PANEL_WIDTH - 8, BUTTON_HEIGHT)) {
                        ModuleManager.getInstance().toggleModule(module.getName());
                        return true;
                    }
                    buttonY += BUTTON_SPACING;
                }
            }
        }
        
        if (button == 1) {
            for (ModuleCategory category : ModuleCategory.values()) {
                List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
                int panelX = getPanelX(category);
                float anim = categoryAnimations.getOrDefault(category, 0.0f);
                panelX += (1.0f - anim) * 50.0f;
                
                int panelY = height / 2 - PANEL_HEIGHT / 2;
                int buttonY = panelY + CATEGORY_HEADER_HEIGHT + 10;
                
                for (Module module : modules) {
                    if (isMouseOverButton(mouseX, mouseY, panelX + 5, buttonY, PANEL_WIDTH - 8, BUTTON_HEIGHT)) {
                        if (!module.getSettings().isEmpty()) {
                            settingsPanel.setSelectedModule(module);
                        }
                        return true;
                    }
                    buttonY += BUTTON_SPACING;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }
}
