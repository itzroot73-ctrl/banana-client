package com.fusionclient.mixin;

import com.fusionclient.gui.FusionGuiScreen;
import com.fusionclient.visual.TargetRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(net.minecraft.client.gui.DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.currentScreen == null && 
            (com.fusionclient.module.ModuleManager.getInstance().isModuleEnabled("Kill Aura") ||
             com.fusionclient.module.ModuleManager.getInstance().isModuleEnabled("Aim Assist"))) {
            TargetRenderer.getInstance().renderTargetHUD(context, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        }
    }
}
