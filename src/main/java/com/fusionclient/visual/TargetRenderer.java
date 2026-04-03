package com.fusionclient.visual;

import com.fusionclient.module.ModuleManager;
import com.fusionclient.social.FriendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class TargetRenderer {
    private static TargetRenderer instance;
    private MinecraftClient client;
    private Entity currentTarget;
    
    private static final int ORANGE_COLOR = 0xFFFFA500;
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = 0x80FFFFFF;
    private static final int GLOW_COLOR = 0x80FFA500;
    private static final int FRIEND_COLOR = 0xFF00FF00;
    private static final int FRIEND_GLOW = 0x8000FF00;
    
    private float ringAnimation = 0.0f;
    private float boxRotation = 0.0f;
    private float tracerAlpha = 0.0f;
    
    private boolean macePvPEnabled = false;
    private boolean killAuraEnabled = false;
    private boolean smartElytraEnabled = false;
    private boolean windChargeEnabled = false;

    private TargetRenderer() {
        this.client = MinecraftClient.getInstance();
    }
    
    public static TargetRenderer getInstance() {
        if (instance == null) {
            instance = new TargetRenderer();
        }
        return instance;
    }
    
    public void setTarget(Entity target) {
        this.currentTarget = target;
    }
    
    public Entity getCurrentTarget() {
        return currentTarget;
    }
    
    public void clearTarget() {
        this.currentTarget = null;
    }
    
    public void setMacePvPEnabled(boolean enabled) {
        this.macePvPEnabled = enabled;
    }
    
    public void renderESP(MatrixStack matrixStack, float tickDelta) {
        if (currentTarget == null || client.world == null || client.player == null) return;
        if (currentTarget.isInvisible()) return;
        
        ringAnimation += 0.03f;
        boxRotation += 0.02f;
        if (ringAnimation > 360.0f) ringAnimation = 0.0f;
        if (boxRotation > 360.0f) boxRotation = 0.0f;
        
        killAuraEnabled = ModuleManager.getInstance().isModuleEnabled("Kill Aura");
        macePvPEnabled = ModuleManager.getInstance().isModuleEnabled("Mace PvP");
        smartElytraEnabled = ModuleManager.getInstance().isModuleEnabled("Smart Elytra");
        windChargeEnabled = ModuleManager.getInstance().isModuleEnabled("Wind Charge");
        
        if (macePvPEnabled || killAuraEnabled || smartElytraEnabled || windChargeEnabled) {
            int targetColor = getTargetColor(currentTarget);
            render3DGlowingBox(matrixStack, currentTarget, tickDelta, targetColor);
            renderTracer(matrixStack, targetColor);
        }
    }
    
    public int getTargetColor(Entity entity) {
        if (entity instanceof PlayerEntity) {
            String name = ((PlayerEntity) entity).getName().getString();
            if (FriendManager.getInstance().isFriendIgnoreCase(name)) {
                return FRIEND_COLOR;
            }
        }
        return ORANGE_COLOR;
    }
    
    public boolean isFriend(Entity entity) {
        if (entity instanceof PlayerEntity) {
            String name = ((PlayerEntity) entity).getName().getString();
            return FriendManager.getInstance().isFriendIgnoreCase(name);
        }
        return false;
    }
    
    private void render3DGlowingBox(MatrixStack matrixStack, Entity entity, float tickDelta, int targetColor) {
        if (client.gameRenderer.getCamera() == null) return;
        
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        
        double minX = entity.getBoundingBox().minX - 0.1 - cameraPos.x;
        double minY = entity.getBoundingBox().minY - 0.1 - cameraPos.y;
        double minZ = entity.getBoundingBox().minZ - 0.1 - cameraPos.z;
        double maxX = entity.getBoundingBox().maxX + 0.1 - cameraPos.x;
        double maxY = entity.getBoundingBox().maxY + 0.2 - cameraPos.y;
        double maxZ = entity.getBoundingBox().maxZ + 0.1 - cameraPos.z;
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2.5f);
        
        float pulse = (float)(Math.sin(ringAnimation * 0.08) * 0.25 + 0.75);
        float alpha = 0.7f * pulse;
        
        float rotationOffset = boxRotation;
        float glowIntensity = pulse;
        
        renderRotatedBox(minX, minY, minZ, maxX, maxY, maxZ, targetColor, alpha, glowIntensity);
        
        float innerAlpha = alpha * 0.5f;
        renderRotatedBox(
            minX + 0.1, minY + 0.1, minZ + 0.1,
            maxX - 0.1, maxY - 0.1, maxZ - 0.1,
            WHITE_COLOR, innerAlpha, glowIntensity * 0.5f
        );
        
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }
    
    private void renderRotatedBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float alpha, float glow) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        buffer.vertex(minX, minY, minZ).color(r, g, b, alpha).next();
        buffer.vertex(maxX, minY, minZ).color(r, g, b, alpha).next();
        
        buffer.vertex(maxX, minY, minZ).color(r, g, b, alpha).next();
        buffer.vertex(maxX, minY, maxZ).color(r, g, b, alpha).next();
        
        buffer.vertex(maxX, minY, maxZ).color(r, g, b, alpha).next();
        buffer.vertex(minX, minY, maxZ).color(r, g, b, alpha).next();
        
        buffer.vertex(minX, minY, maxZ).color(r, g, b, alpha).next();
        buffer.vertex(minX, minY, minZ).color(r, g, b, alpha).next();
        
        buffer.vertex(minX, maxY, minZ).color(r, g, b, alpha).next();
        buffer.vertex(maxX, maxY, minZ).color(r, g, b, alpha).next();
        
        buffer.vertex(maxX, maxY, minZ).color(r, g, b, alpha).next();
        buffer.vertex(maxX, maxY, maxZ).color(r, g, b, alpha).next();
        
        buffer.vertex(maxX, maxY, maxZ).color(r, g, b, alpha).next();
        buffer.vertex(minX, maxY, maxZ).color(r, g, b, alpha).next();
        
        buffer.vertex(minX, maxY, maxZ).color(r, g, b, alpha).next();
        buffer.vertex(minX, maxY, minZ).color(r, g, b, alpha).next();
        
        buffer.vertex(minX, minY, minZ).color(r, g, b, alpha).next();
        buffer.vertex(minX, maxY, minZ).color(r, g, b, alpha).next();
        
        buffer.vertex(maxX, minY, minZ).color(r, g, b, alpha).next();
        buffer.vertex(maxX, maxY, minZ).color(r, g, b, alpha).next();
        
        buffer.vertex(maxX, minY, maxZ).color(r, g, b, alpha).next();
        buffer.vertex(maxX, maxY, maxZ).color(r, g, b, alpha).next();
        
        buffer.vertex(minX, minY, maxZ).color(r, g, b, alpha).next();
        buffer.vertex(minX, maxY, maxZ).color(r, g, b, alpha).next();
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
    
    private void renderTracer(MatrixStack matrixStack, int targetColor) {
        if (client.player == null || currentTarget == null) return;
        
        Vec3d startPos = client.player.getPos().add(0, client.player.getStandingEyeHeight() * 0.8, 0);
        Vec3d endPos = currentTarget.getPos().add(0, currentTarget.getStandingEyeHeight() * 0.7, 0);
        
        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
        
        double startX = startPos.x - cameraPos.x;
        double startY = startPos.y - cameraPos.y;
        double startZ = startPos.z - cameraPos.z;
        
        double endX = endPos.x - cameraPos.x;
        double endY = endPos.y - cameraPos.y;
        double endZ = endPos.z - cameraPos.z;
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(1.0f);
        
        float alpha = 0.15f;
        
        float r = ((targetColor >> 16) & 0xFF) / 255.0f;
        float g = ((targetColor >> 8) & 0xFF) / 255.0f;
        float b = (targetColor & 0xFF) / 255.0f;
        
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        buffer.vertex(startX, startY, startZ).color(r, g, b, alpha).next();
        buffer.vertex(endX, endY, endZ).color(r, g, b, 0.0f).next();
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
    }
    
    public void renderTargetHUD(DrawContext context, int screenWidth, int screenHeight) {
        if (currentTarget == null || !(currentTarget instanceof LivingEntity)) return;
        
        killAuraEnabled = ModuleManager.getInstance().isModuleEnabled("Kill Aura");
        macePvPEnabled = ModuleManager.getInstance().isModuleEnabled("Mace PvP");
        
        if (!killAuraEnabled && !macePvPEnabled) return;
        
        LivingEntity target = (LivingEntity) currentTarget;
        
        int hudWidth = 220;
        int hudHeight = 75;
        int hudX = (screenWidth - hudWidth) / 2;
        int hudY = 30;
        
        context.fill(hudX, hudY, hudX + hudWidth, hudY + hudHeight, BG_COLOR);
        context.drawBorder(hudX, hudY, hudWidth, hudHeight, ORANGE_COLOR);
        
        String name = currentTarget.getName().getString();
        if (currentTarget instanceof PlayerEntity) {
            name = ((PlayerEntity) currentTarget).getName().getString();
        }
        
        context.drawText(this.client.textRenderer, name, hudX + 10, hudY + 10, WHITE_COLOR, false);
        
        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        float healthPercent = Math.max(0, health / maxHealth);
        
        int barWidth = hudWidth - 20;
        int barHeight = 12;
        int barX = hudX + 10;
        int barY = hudY + 30;
        
        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF1A1A1A);
        
        int healthBarWidth = (int)(barWidth * healthPercent);
        if (healthBarWidth > 0) {
            drawGradientBar(context, barX, barY, healthBarWidth, barHeight);
        }
        
        context.drawBorder(barX, barY, barWidth, barHeight, ORANGE_COLOR);
        
        String healthText = String.format("%.1f / %.1f", health, maxHealth);
        context.drawText(this.client.textRenderer, healthText, hudX + hudWidth - 75, barY + 15, WHITE_COLOR, false);
        
        double distance = client.player.getPos().distanceTo(currentTarget.getPos());
        String distText = String.format("%.1fm away", distance);
        
        String targetType = currentTarget instanceof PlayerEntity ? "Player" : "Entity";
        context.drawText(this.client.textRenderer, "[" + targetType + "] " + distText, hudX + 10, hudY + 55, ORANGE_COLOR, false);
    }
    
    private void drawGradientBar(DrawContext context, int x, int y, int width, int height) {
        int orangeStart = 0xFFFFA500;
        int orangeEnd = 0xFFFF8C00;
        
        for (int i = 0; i < width; i++) {
            float t = (float) i / width;
            int r = (int) (0xFF * (1 - t * 0.2));
            int g = (int) ((0xA5 + 0x2B * t));
            int b = (int) (0x00);
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;
            
            context.fill(x + i, y, x + i + 1, y + height, color);
        }
    }
}
