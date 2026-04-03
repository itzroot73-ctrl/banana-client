package com.fusionclient.combat;

import com.fusionclient.module.Module;
import com.fusionclient.module.ModuleCategory;
import com.fusionclient.settings.BooleanSetting;
import com.fusionclient.settings.SliderSetting;
import com.fusionclient.util.CPSUtils;
import com.fusionclient.util.RotationUtils;
import com.fusionclient.visual.TargetRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

public class MacePvPModule extends Module {
    private final MinecraftClient client;
    private final Random random;
    
    private boolean windChargeCooldown = false;
    private boolean maceAttackCooldown = false;
    private boolean isLaunched = false;
    private Entity currentTarget = null;
    private long lastAttackTime = 0;
    
    private static final int WIND_CHARGE_SLOT = -1;
    private static final int MACE_SLOT = -1;

    public MacePvPModule() {
        super("Mace PvP", "Advanced mace combat system", ModuleCategory.COMBAT);
        this.client = MinecraftClient.getInstance();
        this.random = new Random();
        
        addSetting(new BooleanSetting("Auto Wind Charge", "Use wind charge when looking down", true));
        addSetting(new BooleanSetting("Smart Mace Attack", "Attack only when entity nearby", true));
        addSetting(new BooleanSetting("Elytra Smash", "Smash when falling with elytra", true));
        addSetting(new BooleanSetting("Target Visuals", "Show target ring and HUD", true));
        addSetting(new BooleanSetting("Rotation", "Look at target during fall", true));
        addSetting(new SliderSetting("Min Fall Distance", "Min fall distance for attack", 1.5f, 0.5f, 4.0f, 0.1f));
        addSetting(new SliderSetting("Max Range", "Maximum target range", 3.5f, 2.0f, 6.0f, 0.1f));
        addSetting(new SliderSetting("Hit Delay", "Random hit delay in ms", 75.0f, 0.0f, 200.0f, 5.0f));
    }

    @Override
    public void onTick() {
        if (client.player == null || client.world == null) return;
        
        BooleanSetting autoWindCharge = (BooleanSetting) getSetting("Auto Wind Charge");
        BooleanSetting smartMace = (BooleanSetting) getSetting("Smart Mace Attack");
        BooleanSetting elytraSmash = (BooleanSetting) getSetting("Elytra Smash");
        BooleanSetting targetVisuals = (BooleanSetting) getSetting("Target Visuals");
        BooleanSetting rotation = (BooleanSetting) getSetting("Rotation");
        
        if (autoWindCharge != null && autoWindCharge.isEnabled()) {
            handleWindCharge();
        }
        
        float fallDistance = client.player.fallDistance;
        SliderSetting minFallDistSetting = (SliderSetting) getSetting("Min Fall Distance");
        float minFallDist = minFallDistSetting != null ? minFallDistSetting.getValueFloat() : 1.5f;
        
        SliderSetting maxRangeSetting = (SliderSetting) getSetting("Max Range");
        float maxRange = maxRangeSetting != null ? maxRangeSetting.getValueFloat() : 3.5f;
        
        if (fallDistance > minFallDist && !maceAttackCooldown) {
            Entity target = findNearestTarget(maxRange);
            
            if (target != null) {
                currentTarget = target;
                
                if (targetVisuals != null && targetVisuals.isEnabled()) {
                    TargetRenderer.getInstance().setTarget(target);
                }
                
                if (rotation != null && rotation.isEnabled()) {
                    rotateToTarget(target);
                }
                
                if (smartMace != null && smartMace.isEnabled()) {
                    performMaceAttack(target);
                }
            } else {
                currentTarget = null;
                TargetRenderer.getInstance().clearTarget();
            }
        }
        
        if (elytraSmash != null && elytraSmash.isEnabled() && client.player.isFallFlying()) {
            handleElytraSmash(maxRange);
        }
        
        if (client.player.isOnGround()) {
            isLaunched = false;
        }
        
        if (client.player.getVelocity().y < -0.5 && !isLaunched) {
            isLaunched = true;
        }
    }
    
    private void handleWindCharge() {
        if (windChargeCooldown || client.player == null) return;
        
        if (client.player.getPitch() > 75 && client.player.isOnGround()) {
            int windChargeSlot = findItemSlot(Items.WIND_CHARGE);
            
            if (windChargeSlot != -1) {
                int previousSlot = client.player.getInventory().selectedSlot;
                
                client.player.getInventory().selectedSlot = windChargeSlot;
                
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                
                client.player.getInventory().selectedSlot = previousSlot;
                
                windChargeCooldown = true;
                
                scheduleCooldown(() -> windChargeCooldown = false, 600);
            }
        }
    }
    
    private void handleElytraSmash(float range) {
        if (maceAttackCooldown) return;
        
        Entity target = findNearestTarget(range);
        if (target == null) return;
        
        currentTarget = target;
        TargetRenderer.getInstance().setTarget(target);
        
        double relativeY = target.getY() - client.player.getY();
        
        if (relativeY < 1.0 || client.player.fallDistance > 1.0) {
            performMaceAttack(target);
        }
    }
    
    private void performMaceAttack(Entity target) {
        if (maceAttackCooldown) return;
        
        SliderSetting hitDelaySetting = (SliderSetting) getSetting("Hit Delay");
        long hitDelay = hitDelaySetting != null ? (long) hitDelaySetting.getValueFloat() : 75;
        
        long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime;
        
        if (timeSinceLastAttack < 100) return;
        
        long randomDelay = hitDelay + random.nextInt((int)hitDelay);
        
        if (randomDelay < 50) randomDelay = 50 + random.nextInt(50);
        
        int maceSlot = findItemSlot(Items.MACE);
        
        if (maceSlot != -1 && isEntityInReach(target)) {
            scheduleCooldown(() -> {
                if (client.player != null && isEntityInReach(target)) {
                    int prevSlot = client.player.getInventory().selectedSlot;
                    
                    client.player.getInventory().selectedSlot = maceSlot;
                    
                    client.player.swingHand(Hand.MAIN_HAND);
                    client.interactionManager.attackEntity(client.player, target);
                    
                    client.player.getInventory().selectedSlot = prevSlot;
                    
                    lastAttackTime = System.currentTimeMillis();
                }
            }, randomDelay);
            
            maceAttackCooldown = true;
            scheduleCooldown(() -> maceAttackCooldown = false, 800 + random.nextInt(400));
        }
    }
    
    private void rotateToTarget(Entity target) {
        if (client.player == null || target == null) return;
        
        Vec3d targetPos = target.getPos().add(0, target.getStandingEyeHeight() * 0.7, 0);
        Vec3d playerPos = client.player.getPos().add(0, client.player.getStandingEyeHeight(), 0);
        
        float[] rotations = RotationUtils.calculateLookAt(playerPos, targetPos);
        
        float smoothYaw = RotationUtils.smoothAngle(client.player.getYaw(), rotations[0], 0.15f);
        float smoothPitch = RotationUtils.smoothAngle(client.player.getPitch(), rotations[1], 0.15f);
        
        client.player.setYaw(smoothYaw);
        client.player.setPitch(smoothPitch);
    }
    
    private Entity findNearestTarget(double range) {
        if (client.player == null || client.world == null) return null;
        
        Vec3d playerPos = client.player.getPos();
        
        Entity nearest = null;
        double nearestDist = range * range;
        
        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (((LivingEntity) entity).isDead()) continue;
            if (entity.isInvisible()) continue;
            
            double dist = playerPos.squaredDistanceTo(entity.getPos());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entity;
            }
        }
        
        return nearest;
    }
    
    private boolean isEntityInReach(Entity entity) {
        if (client.player == null || entity == null) return false;
        
        double dist = client.player.getPos().distanceTo(entity.getPos());
        
        SliderSetting maxRangeSetting = (SliderSetting) getSetting("Max Range");
        float maxRange = maxRangeSetting != null ? maxRangeSetting.getValueFloat() : 3.5f;
        
        return dist <= maxRange;
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
    
    private void scheduleCooldown(Runnable action, long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                action.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public void onEnable() {
        windChargeCooldown = false;
        maceAttackCooldown = false;
        isLaunched = false;
        currentTarget = null;
    }

    @Override
    public void onDisable() {
        TargetRenderer.getInstance().clearTarget();
        currentTarget = null;
    }
}
