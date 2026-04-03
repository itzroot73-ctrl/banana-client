package com.fusionclient.mixin;

import com.fusionclient.rotation.RotationManager;
import com.fusionclient.rotation.SmoothRotationSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinRotationHandler {
    
    @Shadow @Final
    MinecraftClient client;
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        RotationManager.getInstance().update();
        SmoothRotationSystem.getInstance().update();
    }
    
    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPackets(CallbackInfo ci) {
        RotationManager rotationManager = RotationManager.getInstance();
        
        if (rotationManager.isRotating() && rotationManager.getCurrentTarget() != null) {
            float silentYaw = rotationManager.getSilentYaw();
            float silentPitch = rotationManager.getSilentPitch();
            
            client.player.setYaw(silentYaw);
            client.player.setPitch(silentPitch);
        }
    }
}
