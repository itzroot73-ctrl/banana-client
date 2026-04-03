package com.fusionclient.mixin;

import com.fusionclient.command.CommandManager;
import net.minecraft.client.network.ClientPlayConnectionHandler;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayConnectionHandler.class)
public class MixinChatHandler {
    
    @Shadow @Final
    private net.minecraft.client.network.ClientConnection connection;
    
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (message.startsWith(".")) {
            ci.cancel();
            
            CommandManager.getInstance().handleMessage(message);
            
            if (message.startsWith(".bind") || message.startsWith(".friend") || 
                message.startsWith(".toggle") || message.startsWith(".config")) {
                return;
            }
        }
    }
}
