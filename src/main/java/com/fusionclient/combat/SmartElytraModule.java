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

import java.util.List;
import java.util.Random;

public class SmartElytraModule extends Module {
    private final MinecraftClient client;
    private final Random random;
    
    private boolean elytraDeployed = false;
    private boolean fireworkCooldown = false;
    private boolean maceReady = false;
    private Entity currentTarget = null;
    private double lastAltitude = 0;
    
    private static final int FIREWORK_SLOT = -1;
    private static final int MACE_SLOT = -1;

    public SmartElytraModule() {
        super("Smart Elytra", "Advanced elytra combat & flight", ModuleCategory.MOVEMENT);
        this.client = MinecraftClient.getInstance();
        this.random = new Random();
        
        addSetting(new BooleanSetting("Auto Deploy", "Auto deploy elytra when falling", true));
        addSetting(new BooleanSetting("Firework Boost", "Auto use fireworks for speed", true));
        addSetting(new BooleanSetting("Mace Dive", "Attack with mace when diving", true));
        addSetting(new BooleanSetting("Altitude HUD", "Show altitude bar", true));
        addSetting(new BooleanSetting("Ignore Friends", "Don't attack friends", true));
        addSetting(new SliderSetting("Min Fall Distance", "Min fall to deploy", 1.0f, 0.5f, 3.0f, 0.1f));
        addSetting(new SliderSetting("Firework Delay", "Delay between fireworks", 1000f, 200f, 3000f, 100f));
        addSetting(new SliderSetting("Mace Range", "Range to trigger mace", 3.5f, 2.0f, 5.0f, 0.1f));
        addSetting(new SliderSetting("Hit Delay", "Random hit delay ms", 75f, 20f, 150f, 5f));
    }

    @Override
    public void onTick() {
        if (client.player == null || client.world == null) return;
        
        BooleanSetting autoDeploy = (BooleanSetting) getSetting("Auto Deploy");
        BooleanSetting fireworkBoost = (BooleanSetting) getSetting("Firework Boost");
        BooleanSetting maceDive = (BooleanSetting) getSetting("Mace Dive");
        
        if (autoDeploy != null && autoDeploy.isEnabled()) {
            handleElytraDeploy();
        }
        
        if (fireworkBoost != null && fireworkBoost.isEnabled()) {
            handleFireworkBoost();
        }
        
        if (maceDive != null && maceDive.isEnabled()) {
            handleMaceDive();
        }
        
        lastAltitude = client.player.getY();
    }
    
    private void handleElytraDeploy() {
        if (client.player.isOnGround() || client.player.isFallFlying()) return;
        
        SliderSetting minFallSetting = (SliderSetting) getSetting("Min Fall Distance");
        float minFall = minFallSetting != null ? minFallSetting.getValueFloat() : 1.0f;
        
        if (client.player.fallDistance > minFall && !elytraDeployed) {
            if (hasElytraEquipped()) {
                client.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndRotation(
                    client.player.getX(),
                    client.player.getY() + 0.1,
                    client.player.getZ(),
                    client.player.getYaw(),
                    client.player.getPitch(),
                    false
                ));
                
                new Thread(() -> {
                    try {
                        Thread.sleep(50);
                        if (client.player != null) {
                            client.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndRotation(
                                client.player.getX(),
                                client.player.getY() + 0.1,
                                client.player.getZ(),
                                client.player.getYaw(),
                                client.player.getPitch(),
                                false
                            ));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                
                elytraDeployed = true;
            }
        }
        
        if (client.player.isOnGround()) {
            elytraDeployed = false;
        }
    }
    
    private void handleFireworkBoost() {
        if (!client.player.isFallFlying() || fireworkCooldown) return;
        
        double currentSpeed = Math.sqrt(
            client.player.getVelocity().x * client.player.getVelocity().x +
            client.player.getVelocity().y * client.player.getVelocity().y +
            client.player.getVelocity().z * client.player.getVelocity().z
        );
        
        if (currentSpeed < 0.8 || client.player.getVelocity().y < -0.5) {
            int fireworkSlot = findItemSlot(Items.FIREWORK_ROCKET);
            
            if (fireworkSlot != -1) {
                int prevSlot = client.player.getInventory().selectedSlot;
                
                client.player.getInventory().selectedSlot = fireworkSlot;
                client.player.swingHand(Hand.MAIN_HAND);
                
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        if (client.player != null) {
                            client.player.getInventory().selectedSlot = prevSlot;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                
                SliderSetting delaySetting = (SliderSetting) getSetting("Firework Delay");
                long delay = delaySetting != null ? (long) delaySetting.getValueFloat() : 1000;
                
                fireworkCooldown = true;
                new Thread(() -> {
                    try {
                        Thread.sleep(delay + random.nextInt(500));
                        fireworkCooldown = false;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
    
    private void handleMaceDive() {
        if (!client.player.isFallFlying()) return;
        
        if (client.player.getVelocity().y < -0.3) {
            SliderSetting maceRangeSetting = (SliderSetting) getSetting("Mace Range");
            float maceRange = maceRangeSetting != null ? maceRangeSetting.getValueFloat() : 3.5f;
            
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
                TargetRenderer.getInstance().setTarget(target);
                
                double distance = client.player.getPos().distanceTo(target.getPos());
                
                if (distance <= maceRange) {
                    performMaceAttack(target);
                }
            }
        }
    }
    
    private void performMaceAttack(Entity target) {
        int maceSlot = findItemSlot(Items.MACE);
        
        if (maceSlot == -1) return;
        
        SliderSetting hitDelaySetting = (SliderSetting) getSetting("Hit Delay");
        long hitDelay = hitDelaySetting != null ? (long) hitDelaySetting.getValueFloat() : 75;
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
    
    private boolean hasElytraEquipped() {
        for (int i = 0; i < 4; i++) {
            if (client.player.getInventory().getArmorStack(i).getItem() == Items.ELYTRA) {
                return true;
            }
        }
        return false;
    }
    
    public void renderAltitudeHUD(net.minecraft.client.gui.DrawContext context, int screenWidth, int screenHeight) {
        BooleanSetting altitudeHUD = (BooleanSetting) getSetting("Altitude HUD");
        if (altitudeHUD == null || !altitudeHUD.isEnabled()) return;
        
        if (client.player == null || !client.player.isFallFlying()) return;
        
        int barWidth = 100;
        int barHeight = 8;
        int barX = screenWidth / 2 - barWidth / 2;
        int barY = screenHeight - 50;
        
        double altitude = client.player.getY();
        
        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF1A1A1A);
        
        int fillWidth = (int)(barWidth * Math.min(1.0, (altitude + 10) / 100));
        context.fill(barX, barY, barX + fillWidth, barY + barHeight, 0xFFFFA500);
        
        context.drawBorder(barX, barY, barWidth, barHeight, 0xFFFFFFFF);
        
        String altText = String.format("%.1f", altitude);
        context.drawText(this.client.textRenderer, altText, barX + barWidth / 2 - 15, barY - 12, 0xFFFFFFFF, false);
    }

    @Override
    public void onEnable() {
        elytraDeployed = false;
        fireworkCooldown = false;
    }

    @Override
    public void onDisable() {
        TargetRenderer.getInstance().clearTarget();
        currentTarget = null;
    }
}
