package com.fusionclient.mixin;

import com.fusionclient.gui.FusionGuiScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayConnectionHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayConnectionHandler.class)
public class MixinClientPlayConnectionHandler {
    
    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void onDisconnected(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof FusionGuiScreen) {
            client.currentScreen = null;
        }
    }
}
