package com.fusionclient.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class NotificationSystem {
    private static NotificationSystem instance;
    private final MinecraftClient client;
    private final List<Notification> notifications;
    
    private static final int ORANGE_COLOR = 0xFFFFA500;
    private static final int WHITE_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = 0xE0FFFFFF;
    private static final int ENABLED_COLOR = 0xFF00AA00;
    private static final int DISABLED_COLOR = 0xFFFF5555;

    private NotificationSystem() {
        this.client = MinecraftClient.getInstance();
        this.notifications = new ArrayList<>();
    }
    
    public static NotificationSystem getInstance() {
        if (instance == null) {
            instance = new NotificationSystem();
        }
        return instance;
    }

    public void render(DrawContext context, int screenWidth, int screenHeight) {
        int startY = screenHeight - 100;
        
        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification notification = notifications.get(i);
            notification.update();
            
            if (notification.isExpired()) {
                notifications.remove(i);
                continue;
            }
            
            int notifWidth = 180;
            int notifHeight = 40;
            int notifX = screenWidth - notifWidth - 15;
            int notifY = startY + i * (notifHeight + 8);
            
            float slideOffset = notification.getSlideOffset();
            int renderX = notifX + (int)(slideOffset * notifWidth);
            
            if (slideOffset < 0.01f) continue;
            
            float alpha = notification.getAlpha();
            int bgColor = ((int)(alpha * 0xE0) << 24) | 0xFFFFFF;
            
            context.fill(renderX, notifY, renderX + notifWidth, notifY + notifHeight, bgColor);
            context.drawBorder(renderX, notifY, notifWidth, notifHeight, ORANGE_COLOR);
            
            int iconColor = notification.isEnabled() ? ENABLED_COLOR : DISABLED_COLOR;
            String icon = notification.isEnabled() ? "+" : "-";
            context.drawText(client.textRenderer, icon, renderX + 8, notifY + 12, iconColor, false);
            
            context.drawText(client.textRenderer, notification.getModuleName(), renderX + 25, notifY + 8, WHITE_COLOR, false);
            
            String status = notification.isEnabled() ? "Enabled" : "Disabled";
            context.drawText(client.textRenderer, status, renderX + 25, notifY + 20, 
                notification.isEnabled() ? ENABLED_COLOR : DISABLED_COLOR, false);
        }
    }
    
    public void showNotification(String moduleName, boolean enabled) {
        notifications.add(new Notification(moduleName, enabled));
    }
    
    public void clear() {
        notifications.clear();
    }
    
    private static class Notification {
        private final String moduleName;
        private final boolean enabled;
        private final long startTime;
        private final long duration = 2000;
        
        private float slideOffset = 1.0f;
        private float alpha = 1.0f;
        
        public Notification(String moduleName, boolean enabled) {
            this.moduleName = moduleName;
            this.enabled = enabled;
            this.startTime = System.currentTimeMillis();
        }
        
        public void update() {
            long elapsed = System.currentTimeMillis() - startTime;
            
            if (elapsed < 200) {
                slideOffset = 1.0f - (elapsed / 200.0f);
                alpha = elapsed / 200.0f;
            } else if (elapsed > duration - 300) {
                long fadeTime = elapsed - (duration - 300);
                alpha = 1.0f - (fadeTime / 300.0f);
                slideOffset = 0;
            } else {
                slideOffset = 0;
                alpha = 1.0f;
            }
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > duration;
        }
        
        public String getModuleName() {
            return moduleName;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public float getSlideOffset() {
            return slideOffset;
        }
        
        public float getAlpha() {
            return alpha;
        }
    }
}
