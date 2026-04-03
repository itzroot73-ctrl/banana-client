package com.fusionclient.rotation;

import com.fusionclient.module.ModuleManager;
import com.fusionclient.social.FriendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class RotationHUD {
    private static RotationHUD instance;
    private final MinecraftClient client;
    
    private static final int ORANGE_COLOR = 0xFFFFA500;
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = 0x80FFFFFF;
    private static final int FRIEND_COLOR = 0xFF00FF00;

    private RotationHUD() {
        this.client = MinecraftClient.getInstance();
    }
    
    public static RotationHUD getInstance() {
        if (instance == null) {
            instance = new RotationHUD();
        }
        return instance;
    }

    public void render(DrawContext context, int screenWidth, int screenHeight) {
        boolean killAuraEnabled = ModuleManager.getInstance().isModuleEnabled("Kill Aura");
        boolean aimAssistEnabled = ModuleManager.getInstance().isModuleEnabled("Aim Assist");
        
        if (!killAuraEnabled && !aimAssistEnabled) return;
        
        RotationManager rotationManager = RotationManager.getInstance();
        Entity target = rotationManager.getCurrentTarget();
        
        if (target == null) return;
        
        int hudWidth = 160;
        int hudHeight = 50;
        int hudX = screenWidth / 2 - hudWidth / 2;
        int hudY = 50;
        
        context.fill(hudX, hudY, hudX + hudWidth, hudY + hudHeight, BG_COLOR);
        context.drawBorder(hudX, hudY, hudWidth, hudHeight, ORANGE_COLOR);
        
        boolean isFriend = false;
        String name = target.getName().getString();
        if (target instanceof PlayerEntity) {
            isFriend = FriendManager.getInstance().isFriendIgnoreCase(name);
        }
        
        int nameColor = isFriend ? FRIEND_COLOR : ORANGE_COLOR;
        context.drawText(client.textRenderer, "Target Locked", hudX + 10, hudY + 8, nameColor, false);
        
        context.drawText(client.textRenderer, name, hudX + 10, hudY + 22, WHITE_COLOR, false);
        
        double distance = client.player.getPos().distanceTo(target.getPos());
        String distText = String.format("%.1fm", distance);
        context.drawText(client.textRenderer, distText, hudX + 10, hudY + 36, isFriend ? FRIEND_COLOR : ORANGE_COLOR, false);
        
        if (rotationManager.isReturning()) {
            context.drawText(client.textRenderer, "Returning...", hudX + 80, hudY + 36, 0xFF808080, false);
        }
    }
}
