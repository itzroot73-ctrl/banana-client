package com.fusionclient.inventory;

import com.fusionclient.module.ModuleManager;
import com.fusionclient.settings.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.opengl.GL11;

public class TotemHUD {
    private static TotemHUD instance;
    private final MinecraftClient client;
    
    private static final int ORANGE_COLOR = 0xFFFFA500;
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = 0x80FFFFFF;
    private static final int DISABLED_COLOR = 0xFF252525;
    
    private float glowAnimation = 0.0f;
    private float scaleAnimation = 1.0f;
    private boolean animating = false;

    private TotemHUD() {
        this.client = MinecraftClient.getInstance();
    }
    
    public static TotemHUD getInstance() {
        if (instance == null) {
            instance = new TotemHUD();
        }
        return instance;
    }

    public void render(DrawContext context, int screenWidth, int screenHeight) {
        if (!ModuleManager.getInstance().isModuleEnabled("Auto Totem")) return;
        
        AutoTotemModule totemModule = (AutoTotemModule) ModuleManager.getInstance().getModule("Auto Totem");
        if (totemModule == null) return;
        
        BooleanSetting legitMode = (BooleanSetting) totemModule.getSetting("Legit Mode");
        if (legitMode == null || !legitMode.isEnabled()) return;
        
        int iconSize = 32;
        int hudWidth = iconSize + 50;
        int hudHeight = iconSize + 10;
        int hudX = 20;
        int hudY = screenHeight - hudHeight - 20;
        
        float scale = 1.0f;
        if (totemModule.isJustSwapped()) {
            scale = 1.0f + totemModule.getAnimationProgress() * 0.3f;
        }
        
        int scaledWidth = (int)(hudWidth * scale);
        int scaledHeight = (int)(hudHeight * scale);
        int scaledX = hudX - (scaledWidth - hudWidth) / 2;
        int scaledY = hudY - (scaledHeight - hudHeight) / 2;
        
        GL11.glPushMatrix();
        
        context.fill(scaledX, scaledY, scaledX + scaledWidth, scaledY + scaledHeight, BG_COLOR);
        context.drawBorder(scaledX, scaledY, scaledWidth, scaledHeight, ORANGE_COLOR);
        
        float glowAlpha = 0.0f;
        if (totemModule.isJustSwapped()) {
            glowAlpha = totemModule.getAnimationProgress() * 0.5f;
            int glowColor = (int)(glowAlpha * 255) << 24 | 0xFFA500;
            context.fill(scaledX - 2, scaledY - 2, scaledX + scaledWidth + 2, scaledY + scaledHeight + 2, glowColor);
        }
        
        renderTotemIcon(context, scaledX + 8, scaledY + 5, iconSize - 10);
        
        int count = totemModule.getTotemCount();
        String countText = String.valueOf(count);
        
        context.drawText(client.textRenderer, countText, scaledX + iconSize + 10, scaledY + 12, 
            count > 0 ? WHITE_COLOR : 0xFF555555, false);
        
        String status = totemModule.hasTotemInOffhand() ? "Protected" : "Vulnerable";
        int statusColor = totemModule.hasTotemInOffhand() ? ORANGE_COLOR : 0xFFFF5555;
        
        float health = client.player != null ? client.player.getHealth() : 20.0f;
        SliderSetting healthSetting = (SliderSetting) totemModule.getSetting("Health Threshold");
        float threshold = healthSetting != null ? healthSetting.getValueFloat() : 6.0f;
        
        if (health <= threshold) {
            statusColor = 0xFFFF5555;
        }
        
        context.drawText(client.textRenderer, status, scaledX + iconSize + 10, scaledY + 24, statusColor, false);
        
        GL11.glPopMatrix();
    }
    
    private void renderTotemIcon(DrawContext context, int x, int y, int size) {
        context.fill(x + size/3, y + size/4, x + size*2/3, y + size*3/4, ORANGE_COLOR);
        
        context.fill(x + size/4, y + size/3, x + size*3/4, y + size/2, ORANGE_COLOR);
        
        context.fill(x + size/3, y + size/2, x + size*2/3, y + size*3/4, ORANGE_COLOR);
        
        context.fill(x + size*3/5, y + size/5, x + size*4/5, y + size*3/5, WHITE_COLOR);
    }

    public void triggerGlowAnimation() {
        this.animating = true;
        this.glowAnimation = 1.0f;
    }

    public void update() {
        if (animating) {
            glowAnimation -= 0.05f;
            scaleAnimation += 0.02f;
            
            if (glowAnimation <= 0.0f) {
                glowAnimation = 0.0f;
                animating = false;
            }
            if (scaleAnimation >= 1.3f) {
                scaleAnimation = 1.3f;
            }
        } else if (scaleAnimation > 1.0f) {
            scaleAnimation -= 0.05f;
            if (scaleAnimation < 1.0f) scaleAnimation = 1.0f;
        }
    }
}
