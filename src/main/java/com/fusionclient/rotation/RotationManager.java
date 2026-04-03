package com.fusionclient.rotation;

import com.fusionclient.module.ModuleManager;
import com.fusionclient.settings.SliderSetting;
import com.fusionclient.util.AnimationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class RotationManager {
    private static RotationManager instance;
    private final MinecraftClient client;
    private final Random random;
    
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private float currentYaw = 0.0f;
    private float currentPitch = 0.0f;
    
    private float originalYaw = 0.0f;
    private float originalPitch = 0.0f;
    
    private boolean isRotating = false;
    private boolean isReturning = false;
    private long rotationStartTime = 0;
    private long lastTargetTime = 0;
    private long returnDelay = 3000;
    
    private Entity currentTarget = null;
    private boolean silentRotation = false;
    
    private float jitterX = 0.0f;
    private float jitterY = 0.0f;
    private long lastJitterUpdate = 0;
    
    private static final int RETURN_DELAY_MS = 3000;

    private RotationManager() {
        this.client = MinecraftClient.getInstance();
        this.random = new Random();
    }

    public static RotationManager getInstance() {
        if (instance == null) {
            instance = new RotationManager();
        }
        return instance;
    }

    public void update() {
        if (client.player == null) return;
        
        currentYaw = client.player.getYaw();
        currentPitch = client.player.getPitch();
        
        if (isRotating && currentTarget != null) {
            updateJitter();
            
            float speed = getRotationSpeed();
            float easedSpeed = AnimationUtils.easeInOutCubic(Math.min(1.0f, speed));
            
            float newYaw = smoothAngle(currentYaw, targetYaw + jitterX, easedSpeed);
            float newPitch = smoothAngle(currentPitch, targetPitch + jitterY, easedSpeed);
            
            client.player.setYaw(newYaw);
            client.player.setPitch(newPitch);
            
            lastTargetTime = System.currentTimeMillis();
            
            if (isTargetReached()) {
                isReturning = true;
                rotationStartTime = System.currentTimeMillis();
            }
        }
        
        if (isReturning) {
            long elapsed = System.currentTimeMillis() - rotationStartTime;
            long delayPassed = System.currentTimeMillis() - lastTargetTime;
            
            if (delayPassed > returnDelay) {
                float returnProgress = Math.min(1.0f, elapsed / 2000.0f);
                float easedReturn = AnimationUtils.smoothStep(returnProgress);
                
                float returnYaw = smoothAngle(targetYaw, originalYaw, easedReturn);
                float returnPitch = smoothAngle(targetPitch, originalPitch, easedReturn);
                
                client.player.setYaw(returnYaw);
                client.player.setPitch(returnPitch);
                
                if (returnProgress >= 1.0f) {
                    reset();
                }
            }
        }
    }

    public void requestRotation(Entity target, boolean silent) {
        if (target == null) return;
        
        if (!isRotating && !isReturning) {
            originalYaw = client.player.getYaw();
            originalPitch = client.player.getPitch();
        }
        
        currentTarget = target;
        
        Vec3d targetPos = target.getPos().add(0, target.getStandingEyeHeight() * 0.7, 0);
        Vec3d playerPos = client.player.getPos().add(0, client.player.getStandingEyeHeight(), 0);
        
        float[] rotations = calculateLookAt(playerPos, targetPos);
        
        targetYaw = rotations[0];
        targetPitch = rotations[1];
        
        isRotating = true;
        silentRotation = silent;
        rotationStartTime = System.currentTimeMillis();
        
        SliderSetting searchRangeSetting = (SliderSetting) ModuleManager.getInstance().getModule("Kill Aura").getSettings().stream()
            .filter(s -> s.getName().equals("Range")).findFirst().orElse(null);
        float searchRange = searchRangeSetting != null ? searchRangeSetting.getValueFloat() : 3.5f;
        
        double distance = client.player.getPos().distanceTo(target.getPos());
        if (distance > searchRange) {
            isRotating = false;
            isReturning = true;
            rotationStartTime = System.currentTimeMillis();
            lastTargetTime = System.currentTimeMillis();
        }
    }

    public void cancelRotation() {
        if (isRotating && !isReturning) {
            isReturning = true;
            rotationStartTime = System.currentTimeMillis();
            lastTargetTime = System.currentTimeMillis();
        }
    }

    public void reset() {
        isRotating = false;
        isReturning = false;
        currentTarget = null;
        targetYaw = 0.0f;
        targetPitch = 0.0f;
        jitterX = 0.0f;
        jitterY = 0.0f;
    }

    private void updateJitter() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastJitterUpdate > 50 + random.nextInt(100)) {
            jitterX = (random.nextFloat() - 0.5f) * 2.0f;
            jitterY = (random.nextFloat() - 0.5f) * 1.5f;
            lastJitterUpdate = currentTime;
        }
    }

    private boolean isTargetReached() {
        float yawDiff = Math.abs(wrapAngle(targetYaw - currentYaw));
        float pitchDiff = Math.abs(targetPitch - currentPitch);
        
        return yawDiff < 1.0f && pitchDiff < 1.0f;
    }

    private float getRotationSpeed() {
        try {
            SliderSetting speedSetting = (SliderSetting) ModuleManager.getInstance().getModule("Aim Assist").getSettings().stream()
                .filter(s -> s.getName().equals("Speed")).findFirst().orElse(null);
            
            if (speedSetting != null) {
                return speedSetting.getValueFloat() / 10.0f;
            }
        } catch (Exception e) {
        }
        return 0.5f;
    }

    private float[] calculateLookAt(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horizontalDist));

        return new float[]{wrapAngle(yaw), pitch};
    }

    private float smoothAngle(float current, float target, float t) {
        float diff = wrapAngle(target - current);
        return current + diff * t;
    }

    private float wrapAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    public boolean isRotating() {
        return isRotating;
    }

    public boolean isReturning() {
        return isReturning;
    }

    public Entity getCurrentTarget() {
        return currentTarget;
    }

    public float getSilentYaw() {
        return silentRotation ? targetYaw : currentYaw;
    }

    public float getSilentPitch() {
        return silentRotation ? targetPitch : currentPitch;
    }

    public void setReturnDelay(long delayMs) {
        this.returnDelay = delayMs;
    }
}
