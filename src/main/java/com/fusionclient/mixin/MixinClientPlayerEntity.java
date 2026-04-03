package com.fusionclient.mixin;

import com.fusionclient.gui.FusionGuiScreen;
import com.fusionclient.module.ModuleManager;
import com.fusionclient.settings.BooleanSetting;
import com.fusionclient.settings.ModeSetting;
import com.fusionclient.settings.SliderSetting;
import com.fusionclient.social.FriendManager;
import com.fusionclient.util.CPSUtils;
import com.fusionclient.util.RotationUtils;
import com.fusionclient.util.TargetUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity {
    
    @Shadow @Final
    MinecraftClient client;
    
    @Shadow
    public float fallDistance;
    
    @Shadow
    public float yaw;
    
    @Shadow
    public float pitch;
    
    @Shadow
    public float prevYaw;
    
    @Shadow
    public float prevPitch;
    
    private boolean windChargeCooldown = false;
    private boolean maceCooldown = false;
    private boolean elytraDeployed = false;
    private boolean isAttacking = false;
    private long lastAttackTime = 0;
    private float silentYaw = 0.0f;
    private float silentPitch = 0.0f;
    private boolean silentRotationActive = false;
    
    @Inject(method = "jump", at = @At("HEAD"))
    private void onJump(CallbackInfo ci) {
        if (ModuleManager.getInstance().isModuleEnabled("Auto Wind Charge")) {
            if (client.player != null) {
                SliderSetting pitchSetting = (SliderSetting) ModuleManager.getInstance().getModule("Auto Wind Charge").getSettings().stream()
                    .filter(s -> s.getName().equals("Min Pitch")).findFirst().orElse(null);
                float minPitch = pitchSetting != null ? pitchSetting.getValueFloat() : 75.0f;
                
                if (client.player.getPitch() > minPitch) {
                    useWindCharge();
                }
            }
        }
    }
    
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovement(CallbackInfo ci) {
        if (client.player == null) return;
        
        if (ModuleManager.getInstance().isModuleEnabled("Auto Mace")) {
            SliderSetting fallDistSetting = (SliderSetting) ModuleManager.getInstance().getModule("Auto Mace").getSettings().stream()
                .filter(s -> s.getName().equals("Fall Distance")).findFirst().orElse(null);
            float minFallDist = fallDistSetting != null ? fallDistSetting.getValueFloat() : 1.5f;
            
            if (fallDistance > minFallDist && !maceCooldown) {
                triggerMaceAttack();
            }
        }
        
        if (ModuleManager.getInstance().isModuleEnabled("Auto Elytra")) {
            SliderSetting fallDistSetting = (SliderSetting) ModuleManager.getInstance().getModule("Auto Elytra").getSettings().stream()
                .filter(s -> s.getName().equals("Fall Distance")).findFirst().orElse(null);
            float minFallDist = fallDistSetting != null ? fallDistSetting.getValueFloat() : 2.0f;
            
            if (fallDistance > minFallDist && !elytraDeployed && !client.player.isOnGround()) {
                deployElytra();
            }
        }
        
        boolean killAuraEnabled = ModuleManager.getInstance().isModuleEnabled("Kill Aura");
        boolean aimAssistEnabled = ModuleManager.getInstance().isModuleEnabled("Aim Assist");
        
        if (killAuraEnabled || aimAssistEnabled) {
            runKillAura();
        }
        
        if (ModuleManager.getInstance().isModuleEnabled("Trigger Bot")) {
            runTriggerBot();
        }
        
        if (ModuleManager.getInstance().isModuleEnabled("Aim Assist")) {
            runAimAssist();
        }
    }
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (client.player == null) return;
        
        if (isShiftPressed() && isRightMouseClicked()) {
            if (!(client.currentScreen instanceof FusionGuiScreen)) {
                client.setScreen(new FusionGuiScreen(client.currentScreen));
            }
        }
        
        RotationUtils.updateCurrentRotation(yaw, pitch);
    }
    
    private boolean isShiftPressed() {
        return InputUtil.isKeyPressed(client.getWindow().getHandle(), 340) || 
               InputUtil.isKeyPressed(client.getWindow().getHandle(), 341);
    }
    
    private boolean isRightMouseClicked() {
        return org.lwjgl.glfw.GLFW.glfwGetMouseButton(client.getWindow().getHandle(), 1) == org.lwjgl.glfw.GLFW.GLFW_PRESSED;
    }
    
    private void runKillAura() {
        SliderSetting rangeSetting = (SliderSetting) ModuleManager.getInstance().getModule("Kill Aura").getSettings().stream()
            .filter(s -> s.getName().equals("Range")).findFirst().orElse(null);
        float range = rangeSetting != null ? rangeSetting.getValueFloat() : 3.8f;
        
        BooleanSetting targetPlayersSetting = (BooleanSetting) ModuleManager.getInstance().getModule("Kill Aura").getSettings().stream()
            .filter(s -> s.getName().equals("Target Players")).findFirst().orElse(null);
        
        Entity target = findNearestEntity(range);
        
        if (target != null) {
            if (target instanceof PlayerEntity) {
                String targetName = ((PlayerEntity) target).getName().getString();
                if (FriendManager.getInstance().isFriendIgnoreCase(targetName)) {
                    ModuleManager.getInstance().currentTarget = null;
                    return;
                }
            }
            
            ModuleManager.getInstance().currentTarget = target;
            
            Vec3d targetPos = target.getPos().add(0, target.getStandingEyeHeight() * 0.5, 0);
            float[] targetRotations = RotationUtils.calculateLookAt(client.player.getPos().add(0, client.player.getStandingEyeHeight(), 0), targetPos);
            
            switch (rotationMode) {
                case "Silent":
                    silentYaw = targetRotations[0];
                    silentPitch = targetRotations[1];
                    silentRotationActive = true;
                    RotationUtils.setTargetRotation(silentYaw, silentPitch);
                    break;
                    
                case "Client":
                    RotationUtils.setTargetRotation(targetRotations[0], targetRotations[1]);
                    float smoothYaw = RotationUtils.getSmoothedYaw();
                    float smoothPitch = RotationUtils.getSmoothedPitch();
                    client.player.setYaw(smoothYaw);
                    client.player.setPitch(smoothPitch);
                    break;
                    
                case "None":
                default:
                    silentRotationActive = false;
                    break;
            }
            
            if (CPSUtils.canAttack(minCPS, maxCPS)) {
                attackEntity(target);
                lastAttackTime = System.currentTimeMillis();
                CPSUtils.registerClick();
            }
        } else {
            ModuleManager.getInstance().currentTarget = null;
            silentRotationActive = false;
        }
    }
    
    private void runTriggerBot() {
        Module triggerBot = ModuleManager.getInstance().getModule("Trigger Bot");
        
        SliderSetting rangeSetting = (SliderSetting) triggerBot.getSettings().stream()
            .filter(s -> s.getName().equals("Range")).findFirst().orElse(null);
        float range = rangeSetting != null ? rangeSetting.getValueFloat() : 3.5f;
        
        if (client.crosshairTarget instanceof EntityHitResult) {
            EntityHitResult hitResult = (EntityHitResult) client.crosshairTarget;
            Entity entity = hitResult.getEntity();
            
            if (entity instanceof LivingEntity && entity != client.player) {
                double dist = client.player.getPos().distanceTo(entity.getPos());
                if (dist <= range) {
                    attackEntity(entity);
                    CPSUtils.registerClick();
                }
            }
        }
    }
    
    private void runAimAssist() {
        if (!ModuleManager.getInstance().isModuleEnabled("Kill Aura")) {
            Module aimAssist = ModuleManager.getInstance().getModule("Aim Assist");
            
            SliderSetting rangeSetting = (SliderSetting) aimAssist.getSettings().stream()
                .filter(s -> s.getName().equals("Range")).findFirst().orElse(null);
            float range = rangeSetting != null ? rangeSetting.getValueFloat() : 4.0f;
            
            SliderSetting speedSetting = (SliderSetting) aimAssist.getSettings().stream()
                .filter(s -> s.getName().equals("Speed")).findFirst().orElse(null);
            float speed = speedSetting != null ? speedSetting.getValueFloat() : 5.0f;
            
            boolean targetPlayers = true;
            boolean targetMobs = true;
            BooleanSetting playersSetting = (BooleanSetting) aimAssist.getSettings().stream()
                .filter(s -> s.getName().equals("Target Players")).findFirst().orElse(null);
            if (playersSetting != null) targetPlayers = playersSetting.isEnabled();
            
            BooleanSetting mobsSetting = (BooleanSetting) aimAssist.getSettings().stream()
                .filter(s -> s.getName().equals("Target Mobs")).findFirst().orElse(null);
            if (mobsSetting != null) targetMobs = mobsSetting.isEnabled();
            
            Entity target = TargetUtils.findNearestTarget(range, targetPlayers, targetMobs, false, TargetUtils.SortMode.DISTANCE);
            
            if (target != null) {
                Vec3d targetPos = target.getPos().add(0, target.getStandingEyeHeight() * 0.5, 0);
                float[] targetRotations = RotationUtils.calculateLookAt(client.player.getPos().add(0, client.player.getStandingEyeHeight(), 0), targetPos);
                
                float smoothYaw = RotationUtils.smoothAngle(yaw, targetRotations[0], speed / 20.0f);
                float smoothPitch = RotationUtils.smoothAngle(pitch, targetRotations[1], speed / 20.0f);
                
                client.player.setYaw(smoothYaw);
                client.player.setPitch(smoothPitch);
            }
        }
    }
    
    private void attackEntity(Entity target) {
        client.player.swingHand(Hand.MAIN_HAND);
        client.interactionManager.attackEntity(client.player, target);
    }
    
    private void useWindCharge() {
        if (windChargeCooldown || client.player == null) return;
        
        int windChargeSlot = findItemInHotbar(Items.WIND_CHARGE);
        if (windChargeSlot != -1) {
            client.player.getInventory().selectedSlot = windChargeSlot;
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            windChargeCooldown = true;
            
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    windChargeCooldown = false;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
    
    private void triggerMaceAttack() {
        if (maceCooldown || client.player == null) return;
        
        int maceSlot = findItemInHotbar(Items.MACE);
        if (maceSlot != -1) {
            if (client.crosshairTarget instanceof EntityHitResult) {
                client.player.getInventory().selectedSlot = maceSlot;
                client.player.swingHand(Hand.MAIN_HAND);
                maceCooldown = true;
                
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        maceCooldown = false;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
    }
    
    private void deployElytra() {
        if (client.player == null) return;
        
        if (client.player.isFallFlying()) {
            elytraDeployed = true;
            return;
        }
        
        if (hasElytraEquipped()) {
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndRotation(
                client.player.getX(),
                client.player.getY() + 0.1,
                client.player.getZ(),
                client.player.getYaw(),
                client.player.getPitch(),
                client.player.isOnGround()
            ));
            
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    if (client.player != null) {
                        client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndRotation(
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
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    elytraDeployed = false;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
    
    private int findItemInHotbar(Items item) {
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
    
    public boolean isSilentRotationActive() {
        return silentRotationActive;
    }
    
    public float getSilentYaw() {
        return silentYaw;
    }
    
    public float getSilentPitch() {
        return silentPitch;
    }
}
