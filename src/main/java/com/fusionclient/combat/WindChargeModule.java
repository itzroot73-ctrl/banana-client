package com.fusionclient.combat;

import com.fusionclient.module.Module;
import com.fusionclient.module.ModuleCategory;
import com.fusionclient.settings.BooleanSetting;
import com.fusionclient.settings.SliderSetting;
import com.fusionclient.social.FriendManager;
import com.fusionclient.visual.TargetRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class WindChargeModule extends Module {
    private final MinecraftClient client;
    private final Random random;
    
    private boolean windChargeCooldown = false;
    private boolean launchPrepared = false;
    private boolean macePrepared = false;
    private Entity currentTarget = null;
    private long lastWindChargeUse = 0;

    public WindChargeModule() {
        super("Wind Charge", "Wind charge launch & mobility", ModuleCategory.MOVEMENT);
        this.client = MinecraftClient.getInstance();
        this.random = new Random();
        
        addSetting(new BooleanSetting("Auto Launch", "Auto launch when looking down", true));
        addSetting(new BooleanSetting("Burst Logic", "Prevent fall damage with wind charge", true));
        addSetting(new BooleanSetting("Mace Synergy", "Switch to mace after launch", true));
        addSetting(new BooleanSetting("Target Visuals", "Show target ring", true));
        addSetting(new BooleanSetting("Ignore Friends", "Don't attack friends", true));
        addSetting(new SliderSetting("Min Pitch", "Min pitch to trigger launch", 75f, 45f, 90f, 5f));
        addSetting(new SliderSetting("Burst Height", "Height to trigger burst", 3f, 1f, 10f, 0.5f));
        addSetting(new SliderSetting("Mace Range", "Range to attack after launch", 4f, 2f, 6f, 0.1f));
        addSetting(new SliderSetting("Hit Delay", "Random hit delay ms", 80f, 30f, 150f, 5f));
    }

    @Override
    public void onTick() {
        if (client.player == null || client.world == null) return;
        
        BooleanSetting autoLaunch = (BooleanSetting) getSetting("Auto Launch");
        BooleanSetting burstLogic = (BooleanSetting) getSetting("Burst Logic");
        BooleanSetting maceSynergy = (BooleanSetting) getSetting("Mace Synergy");
        
        if (autoLaunch != null && autoLaunch.isEnabled()) {
            handleAutoLaunch();
        }
        
        if (burstLogic != null && burstLogic.isEnabled()) {
            handleBurstLogic();
        }
        
        if (maceSynergy != null && maceSynergy.isEnabled()) {
            handleMaceSynergy();
        }
    }
    
    private void handleAutoLaunch() {
        if (windChargeCooldown) return;
        
        SliderSetting minPitchSetting = (SliderSetting) getSetting("Min Pitch");
        float minPitch = minPitchSetting != null ? minPitchSetting.getValueFloat() : 75f;
        
        if (client.player.isOnGround() && client.player.getPitch() > minPitch) {
            int windChargeSlot = findItemSlot(Items.WIND_CHARGE);
            
            if (windChargeSlot != -1) {
                int prevSlot = client.player.getInventory().selectedSlot;
                
                client.player.getInventory().selectedSlot = windChargeSlot;
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                
                client.player.getInventory().selectedSlot = prevSlot;
                
                launchPrepared = true;
                macePrepared = true;
                
                windChargeCooldown = true;
                lastWindChargeUse = System.currentTimeMillis();
                
                new Thread(() -> {
                    try {
                        Thread.sleep(600);
                        windChargeCooldown = false;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
    
    private void handleBurstLogic() {
        if (windChargeCooldown) return;
        
        if (!client.player.isOnGround() && client.player.getVelocity().y < -0.5) {
            SliderSetting burstHeightSetting = (SliderSetting) getSetting("Burst Height");
            float burstHeight = burstHeightSetting != null ? burstHeightSetting.getValueFloat() : 3f;
            
            double heightAboveGround = client.player.getY();
            
            if (heightAboveGround < burstHeight) {
                int windChargeSlot = findItemSlot(Items.WIND_CHARGE);
                
                if (windChargeSlot != -1) {
                    int prevSlot = client.player.getInventory().selectedSlot;
                    
                    client.player.getInventory().selectedSlot = windChargeSlot;
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    
                    new Thread(() -> {
                        try {
                            Thread.sleep(50);
                            if (client.player != null) {
                                client.player.getInventory().selectedSlot = prevSlot;
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                    
                    windChargeCooldown = true;
                    
                    new Thread(() -> {
                        try {
                            Thread.sleep(600);
                            windChargeCooldown = false;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
            }
        }
    }
    
    private void handleMaceSynergy() {
        if (!launchPrepared && !macePrepared) return;
        
        if (!client.player.isOnGround() && client.player.fallDistance > 1.0f) {
            SliderSetting maceRangeSetting = (SliderSetting) getSetting("Mace Range");
            float maceRange = maceRangeSetting != null ? maceRangeSetting.getValueFloat() : 4f;
            
            Entity target = findNearestTarget(maceRange);
            
            if (target != null) {
                BooleanSetting ignoreFriends = (BooleanSetting) getSetting("Ignore Friends");
                if (ignoreFriends != null && ignoreFriends.isEnabled()) {
                    if (target instanceof PlayerEntity) {
                        String targetName = ((PlayerEntity) target).getName().getString();
                        if (FriendManager.getInstance().isFriendIgnoreCase(targetName)) {
                            return;
                        }
                    }
                }
                
                currentTarget = target;
                
                BooleanSetting targetVisuals = (BooleanSetting) getSetting("Target Visuals");
                if (targetVisuals != null && targetVisuals.isEnabled()) {
                    TargetRenderer.getInstance().setTarget(target);
                }
                
                double distance = client.player.getPos().distanceTo(target.getPos());
                
                if (distance <= maceRange) {
                    performMaceAttack(target);
                    
                    launchPrepared = false;
                    macePrepared = false;
                }
            }
        }
        
        if (client.player.isOnGround()) {
            launchPrepared = false;
            macePrepared = false;
            TargetRenderer.getInstance().clearTarget();
            currentTarget = null;
        }
    }
    
    private void performMaceAttack(Entity target) {
        int maceSlot = findItemSlot(Items.MACE);
        
        if (maceSlot == -1) return;
        
        SliderSetting hitDelaySetting = (SliderSetting) getSetting("Hit Delay");
        long hitDelay = hitDelaySetting != null ? (long) hitDelaySetting.getValueFloat() : 80;
        long randomDelay = hitDelay + random.nextInt((int)hitDelay);
        
        new Thread(() -> {
            try {
                Thread.sleep(randomDelay);
                
                if (client.player != null) {
                    int prevSlot = client.player.getInventory().selectedSlot;
                    
                    client.player.getInventory().selectedSlot = maceSlot;
                    client.player.swingHand(Hand.MAIN_HAND);
                    client.interactionManager.attackEntity(client.player, target);
                    
                    client.player.getInventory().selectedSlot = prevSlot;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private Entity findNearestTarget(double range) {
        if (client.player == null) return null;
        
        Vec3d playerPos = client.player.getPos();
        Entity nearest = null;
        double nearestDist = range * range;
        
        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (((LivingEntity) entity).isDead()) continue;
            
            double dist = playerPos.squaredDistanceTo(entity.getPos());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entity;
            }
        }
        
        return nearest;
    }
    
    private int findItemSlot(net.minecraft.item.Item item) {
        if (client.player == null) return -1;
        
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onEnable() {
        windChargeCooldown = false;
        launchPrepared = false;
        macePrepared = false;
    }

    @Override
    public void onDisable() {
        TargetRenderer.getInstance().clearTarget();
        currentTarget = null;
    }
}
