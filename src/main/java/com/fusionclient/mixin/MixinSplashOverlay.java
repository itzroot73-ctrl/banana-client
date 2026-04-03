package com.fusionclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.*;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@Mixin(SplashOverlay.class)
public class MixinSplashOverlay {
    
    @Shadow @Final
    private static int GUI_HEIGHT = 240;
    
    @Shadow @Final
    private MinecraftClient client;
    
    @Shadow @Final
    private float progress;
    
    private static final int ORANGE_COLOR = 0xFFFFA500;
    private static final int BG_COLOR = 0xFFFFFFFF;
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    private static final int BG_TINT = 0xFF000000;
    
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(CallbackInfo ci) {
        ci.cancel();
        
        MatrixStack matrixStack = new MatrixStack();
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        
        int windowWidth = this.client.getWindow().getScaledWidth();
        int windowHeight = this.client.getWindow().getScaledHeight();
        
        RenderSystem.setShaderColour(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShader(ShaderProgram.POSITION_TEX_COLOR);
        
        drawFill(matrix, 0, 0, windowWidth, windowHeight, BG_COLOR);
        
        float alpha = Math.min(this.progress * 2.0f, 1.0f);
        
        String title = "BANANA CLIENT";
        int titleWidth = this.client.textRenderer.getWidth(title);
        
        float titleX = (windowWidth - titleWidth) / 2.0f;
        float titleY = windowHeight / 2.0f - 40;
        
        float textAlpha = Math.min(this.progress * 1.5f, 1.0f);
        
        drawCenteredText(matrix, title, windowWidth / 2, (int)titleY, ORANGE_COLOR, false);
        
        int barWidth = 200;
        int barHeight = 8;
        int barX = (windowWidth - barWidth) / 2;
        int barY = windowHeight / 2 + 20;
        
        drawFill(matrix, barX, barY, barX + barWidth, barY + barHeight, 0xFFE0E0E0);
        
        int fillWidth = (int)(barWidth * this.progress);
        drawFill(matrix, barX, barY, barX + fillWidth, barY + barHeight, ORANGE_COLOR);
        
        drawFill(matrix, barX - 1, barY - 1, barX + barWidth + 1, barY, ORANGE_COLOR);
        drawFill(matrix, barX - 1, barY + barHeight, barX + barWidth + 1, barY + barHeight + 1, ORANGE_COLOR);
        drawFill(matrix, barX - 1, barY, barX, barY + barHeight, ORANGE_COLOR);
        drawFill(matrix, barX + barWidth, barY, barX + barWidth + 1, barY + barHeight, ORANGE_COLOR);
        
        if (this.client.world != null) {
            this.client.world.mapColorProvider.updateTexture();
        }
    }
    
    private static void drawFill(Matrix4f matrix, int x1, int y1, int x2, int y2, int color) {
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buffer.vertex(matrix, x1, y2, 0.0f).next();
        buffer.vertex(matrix, x2, y2, 0.0f).next();
        buffer.vertex(matrix, x2, y1, 0.0f).next();
        buffer.vertex(matrix, x1, y1, 0.0f).next();
        
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;
        
        bufferBuilder.vertex(matrix, x1, y2, 0).color(r, g, b, a).next();
        bufferBuilder.vertex(matrix, x2, y2, 0).color(r, g, b, a).next();
        bufferBuilder.vertex(matrix, x2, y1, 0).color(r, g, b, a).next();
        bufferBuilder.vertex(matrix, x1, y1, 0).color(r, g, b, a).next();
        
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }
    
    private static void drawCenteredText(Matrix4f matrix, String text, int x, int y, int color, boolean shadow) {
        int width = MinecraftClient.getInstance().textRenderer.getWidth(text);
        int adjustedX = x - width / 2;
        
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        
        MinecraftClient.getInstance().textRenderer.draw(text, adjustedX, y, color, shadow, matrix, 
            MinecraftClient.getInstance().getBatchBuilder(), TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
    }
}
