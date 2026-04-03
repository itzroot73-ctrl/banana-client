package com.fusionclient.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Matrix4f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class PlayerStatusWidget {
    private static PlayerStatusWidget instance;
    private final MinecraftClient client;
    
    private float displayedHealth = 20.0f;
    private float targetHealth = 20.0f;
    private float animProgress = 1.0f;
    
    private static final int ORANGE_COLOR = 0xFFFFA500;
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = 0x80FFFFFF;
    private static final int DISABLED_COLOR = 0xFF252525;

    private PlayerStatusWidget() {
        this.client = MinecraftClient.getInstance();
    }
    
    public static PlayerStatusWidget getInstance() {
        if (instance == null) {
            instance = new PlayerStatusWidget();
        }
        return instance;
    }

    public void render(DrawContext context, int screenWidth, int screenHeight) {
        if (client.player == null) return;
        
        PlayerEntity player = client.player;
        
        updateHealthAnimation();
        
        int widgetWidth = 200;
        int widgetHeight = 50;
        int widgetX = 20;
        int widgetY = screenHeight - widgetHeight - 20;
        
        context.fill(widgetX, widgetY, widgetX + widgetWidth, widgetY + widgetHeight, BG_COLOR);
        context.drawBorder(widgetX, widgetY, widgetWidth, widgetHeight, ORANGE_COLOR);
        
        renderPlayerHead(context, widgetX + 10, widgetY + 10, 30);
        
        int barX = widgetX + 50;
        int barY = widgetY + 12;
        int barWidth = 130;
        int barHeight = 12;
        
        float healthPercent = Math.max(0, displayedHealth / player.getMaxHealth());
        
        context.fill(barX, barY, barX + barWidth, barY + barHeight, DISABLED_COLOR);
        
        if (healthPercent > 0) {
            int healthWidth = (int)(barWidth * healthPercent);
            drawGradientBar(context, barX, barY, healthWidth, barHeight);
        }
        
        context.drawBorder(barX, barY, barWidth, barHeight, ORANGE_COLOR);
        
        String name = player.getName().getString();
        context.drawText(client.textRenderer, name, barX + 2, barY - 10, WHITE_COLOR, false);
        
        String healthText = String.format("%.1f HP", displayedHealth);
        int textWidth = client.textRenderer.getWidth(healthText);
        context.drawText(client.textRenderer, healthText, barX + barWidth - textWidth - 2, barY + 15, ORANGE_COLOR, false);
        
        float absorption = player.getAbsorptionAmount();
        if (absorption > 0) {
            String absText = String.format("+%.1f", absorption);
            context.drawText(client.textRenderer, absText, barX + barWidth - textWidth - 35, barY + 15, 0xFFFFFF00, false);
        }
    }
    
    private void renderPlayerHead(DrawContext context, int x, int y, int size) {
        context.fill(x, y, x + size, y + size, DISABLED_COLOR);
        context.drawBorder(x, y, size, size, ORANGE_COLOR);
        
        String faceUrl = "https://mineskin.eu/helm/" + client.player.getName().getString() + "/30";
        
        context.drawText(client.textRenderer, "?", x + 10, y + 8, WHITE_COLOR, false);
    }
    
    private void drawGradientBar(DrawContext context, int x, int y, int width, int height) {
        for (int i = 0; i < width; i++) {
            float t = (float) i / width;
            
            int r = (int)(255);
            int g = (int)(165 - 45 * t);
            int b = 0;
            
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            
            context.fill(x + i, y, x + i + 1, y + height, color);
        }
    }
    
    private void updateHealthAnimation() {
        if (client.player == null) return;
        
        targetHealth = client.player.getHealth();
        
        if (Math.abs(displayedHealth - targetHealth) > 0.1f) {
            displayedHealth += (targetHealth - displayedHealth) * 0.15f;
        } else {
            displayedHealth = targetHealth;
        }
    }
    
    public float getDisplayedHealth() {
        return displayedHealth;
    }
}
