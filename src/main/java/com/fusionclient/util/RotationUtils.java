package com.fusionclient.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class RotationUtils {
    private static float lastSentYaw = 0.0f;
    private static float lastSentPitch = 0.0f;
    private static float currentYaw = 0.0f;
    private static float currentPitch = 0.0f;
    private static float targetYaw = 0.0f;
    private static float targetPitch = 0.0f;
    
    public static float[] calculateLookAt(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horizontalDist));
        
        return new float[]{yaw, pitch};
    }
    
    public static float[] calculateLookAt(Entity target) {
        if (target == null) return new float[]{0, 0};
        Vec3d targetPos = target.getPos().add(0, target.getStandingEyeHeight() * 0.5, 0);
        return calculateLookAt(target.getX(), target.getY(), target.getZ(), targetPos);
    }
    
    public static float[] calculateLookAt(double playerX, double playerY, double playerZ, Vec3d targetPos) {
        double dx = targetPos.x - playerX;
        double dy = targetPos.y - playerY;
        double dz = targetPos.z - playerZ;
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horizontalDist));
        
        return new float[]{wrapAngle(yaw), pitch};
    }
    
    public static float wrapAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }
    
    public static float smoothAngle(float current, float target, float speed) {
        float diff = wrapAngle(target - current);
        return current + diff * Math.min(1.0f, speed);
    }
    
    public static void setTargetRotation(float yaw, float pitch) {
        targetYaw = wrapAngle(yaw);
        targetPitch = pitch;
    }
    
    public static void updateCurrentRotation(float yaw, float pitch) {
        currentYaw = yaw;
        currentPitch = pitch;
    }
    
    public static float[] getTargetRotation() {
        return new float[]{targetYaw, targetPitch};
    }
    
    public static float[] getCurrentRotation() {
        return new float[]{currentYaw, currentPitch};
    }
    
    public static void setLastSentRotation(float yaw, float pitch) {
        lastSentYaw = yaw;
        lastSentPitch = pitch;
    }
    
    public static float[] getLastSentRotation() {
        return new float[]{lastSentYaw, lastSentPitch};
    }
    
    public static float getSmoothedYaw() {
        return smoothAngle(currentYaw, targetYaw, 0.2f);
    }
    
    public static float getSmoothedPitch() {
        return smoothAngle(currentPitch, targetPitch, 0.2f);
    }
    
    public static float normalizeAngle(float angle) {
        while (angle > 360) angle -= 360;
        while (angle < 0) angle += 360;
        return angle;
    }
    
    public static float getAngleDifference(float a, float b) {
        float diff = normalizeAngle(a - b);
        if (diff > 180) diff -= 360;
        return diff;
    }
}
