package com.fusionclient.mixin;

import com.fusionclient.module.ModuleManager;
import com.fusionclient.util.RotationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerMoveC2SPacket.class)
public class MixinPlayerMoveC2SPacket {
    
    @Inject(method = "<init>(DDDFFZ)V", at = @At("HEAD"), cancellable = true)
    private void onInit(double x, double y, double z, float yaw, float pitch, boolean onGround, CallbackInfo ci) {
        if (!ModuleManager.getInstance().isModuleEnabled("Kill Aura")) return;
        
        String rotationMode = ModuleManager.getInstance().getRotationMode();
        if (!rotationMode.equals("Silent")) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        ClientPlayerEntity player = client.player;
        
        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();
        
        RotationUtils.updateCurrentRotation(currentYaw, currentPitch);
        
        float targetYaw = RotationUtils.getSmoothedYaw();
        float targetPitch = RotationUtils.getSmoothedPitch();
        
        RotationUtils.setLastSentRotation(targetYaw, targetPitch);
        
        ci.cancel();
    }
}
