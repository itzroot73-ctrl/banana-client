package com.fusionclient.mixin;

import com.fusionclient.visual.TargetRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    
    @Shadow @Final
    private GameRenderer gameRenderer;
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(MatrixStack matrixStack, RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        TargetRenderer.getInstance().renderESP(matrixStack, tickCounter.getTickDelta(true));
    }
}
