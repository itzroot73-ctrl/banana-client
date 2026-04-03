package com.fusionclient.mixin;

import com.fusionclient.command.CommandManager;
import com.fusionclient.module.ModuleManager;
import com.fusionclient.social.FriendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.MouseInput;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseInput.class)
public class MixinMouseInput {
    
    @Shadow
    private MinecraftClient client;
    
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (button == 2 && action == 1) {
            handleMiddleClick();
        }
    }
    
    private void handleMiddleClick() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null || client.world == null) return;
        
        if (client.crosshairTarget instanceof EntityHitResult) {
            EntityHitResult hitResult = (EntityHitResult) client.crosshairTarget;
            Entity entity = hitResult.getEntity();
            
            if (entity instanceof PlayerEntity && entity != client.player) {
                PlayerEntity target = (PlayerEntity) entity;
                String targetName = target.getName().getString();
                
                double distance = client.player.getPos().distanceTo(entity.getPos());
                
                if (distance <= 5.0) {
                    if (FriendManager.getInstance().isFriendIgnoreCase(targetName)) {
                        FriendManager.getInstance().removeFriend(targetName);
                        CommandManager.getInstance().sendMessage("§cRemoved §e" + targetName + " §cfrom friends!");
                    } else {
                        FriendManager.getInstance().addFriend(targetName);
                        CommandManager.getInstance().sendMessage("§aAdded §e" + targetName + " §ato friends!");
                    }
                }
            }
        }
    }
}
