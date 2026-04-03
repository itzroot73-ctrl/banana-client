package com.fusionclient.mixin;

import com.fusionclient.gui.BananaTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {
    
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.currentScreen instanceof TitleScreen && !(client.currentScreen instanceof BananaTitleScreen)) {
            BananaTitleScreen customTitle = new BananaTitleScreen();
            client.currentScreen = customTitle;
            customTitle.init();
        }
    }
}
