package com.fusionclient.hud;

import com.fusionclient.module.Module;
import com.fusionclient.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class HudManager {
    private static HudManager instance;
    private final MinecraftClient client;
    
    private final PlayerStatusWidget playerStatusWidget;
    private final ModuleArrayList moduleArrayList;
    private final NotificationSystem notificationSystem;
    
    private boolean enabled = true;
    private int position = 0;

    private HudManager() {
        this.client = MinecraftClient.getInstance();
        this.playerStatusWidget = PlayerStatusWidget.getInstance();
        this.moduleArrayList = ModuleArrayList.getInstance();
        this.notificationSystem = NotificationSystem.getInstance();
    }
    
    public static HudManager getInstance() {
        if (instance == null) {
            instance = new HudManager();
        }
        return instance;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!enabled) return;
        if (client.currentScreen != null) return;
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        playerStatusWidget.render(context, screenWidth, screenHeight);
        
        moduleArrayList.render(context, screenWidth, screenHeight);
        
        notificationSystem.render(context, screenWidth, screenHeight);
    }
    
    public void onModuleToggle(Module module) {
        notificationSystem.showNotification(module.getName(), module.isEnabled());
        moduleArrayList.onModuleToggle(module);
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void toggle() {
        this.enabled = !this.enabled;
    }
    
    public void setPosition(int position) {
        this.position = position;
    }
    
    public int getPosition() {
        return position;
    }
}
