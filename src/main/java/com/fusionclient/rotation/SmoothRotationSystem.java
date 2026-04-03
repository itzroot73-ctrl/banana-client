package com.fusionclient.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class SmoothRotationSystem {
    private static SmoothRotationSystem instance;
    private final MinecraftClient client;
    private final Random random;
    
    private enum RotationMode {
        LINEAR,
        BEZIER,
        SMOOTHSTEP,
        EASE_IN_OUT
    }
    
    private RotationMode currentMode = RotationMode.EASE_IN_OUT;
    private float bezierControlPoint = 0.5f;
    private float lerpFactor = 0.1f;
    private float smoothingAmount = 0.15f;
    
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    private float velocityYaw = 0.0f;
    private float velocityPitch = 0.0f;
    
    private boolean aimSmoothing = true;
    private boolean prediction = false;
    private float predictionAmount = 0.1f;

    private SmoothRotationSystem() {
        this.client = MinecraftClient.getInstance();
        this.random = new Random();
    }

    public static SmoothRotationSystem getInstance() {
        if (instance == null) {
            instance = new SmoothRotationSystem();
        }
        return instance;
    }

    public float[] calculateSmoothRotation(float currentYaw, float currentPitch, float targetYaw, float targetPitch, float delta) {
        switch (currentMode) {
            case LINEAR:
                return calculateLinear(currentYaw, currentPitch, targetYaw, targetPitch, delta);
            case BEZIER:
                return calculateBezier(currentYaw, currentPitch, targetYaw, targetPitch, delta);
            case SMOOTHSTEP:
                return calculateSmoothStep(currentYaw, currentPitch, targetYaw, targetPitch, delta);
            case EASE_IN_OUT:
            default:
                return calculateEaseInOut(currentYaw, currentPitch, targetYaw, targetPitch, delta);
        }
    }

    private float[] calculateLinear(float currentYaw, float currentPitch, float targetYaw, float targetPitch, float delta) {
        float diffYaw = wrapAngle(targetYaw - currentYaw);
        float diffPitch = targetPitch - currentPitch;
        
        float newYaw = currentYaw + diffYaw * lerpFactor;
        float newPitch = currentPitch + diffPitch * lerpFactor;
        
        return new float[]{newYaw, newPitch};
    }

    private float[] calculateBezier(float currentYaw, float currentPitch, float targetYaw, float targetPitch, float delta) {
        float t = lerpFactor;
        float oneMinusT = 1.0f - t;
        
        float controlYaw = currentYaw + (targetYaw - currentYaw) * bezierControlPoint;
        float controlPitch = currentPitch + (targetPitch - currentPitch) * bezierControlPoint;
        
        float newYaw = oneMinusT * oneMinusT * currentYaw + 2 * oneMinusT * t * controlYaw + t * t * targetYaw;
        float newPitch = oneMinusT * oneMinusT * currentPitch + 2 * oneMinusT * t * controlPitch + t * t * targetPitch;
        
        return new float[]{newYaw, newPitch};
    }

    private float[] calculateSmoothStep(float currentYaw, float currentPitch, float targetYaw, float targetPitch, float delta) {
        float diffYaw = wrapAngle(targetYaw - currentYaw);
        float diffPitch = targetPitch - currentPitch;
        
        float smoothT = tween(lerpFactor);
        
        float newYaw = currentYaw + diffYaw * smoothT;
        float newPitch = currentPitch + diffPitch * smoothT;
        
        return new float[]{newYaw, newPitch};
    }

    private float[] calculateEaseInOut(float currentYaw, float currentPitch, float targetYaw, float targetPitch, float delta) {
        float diffYaw = wrapAngle(targetYaw - currentYaw);
        float diffPitch = targetPitch - currentPitch;
        
        float t = lerpFactor;
        
        if (aimSmoothing) {
            float smoothedT = t < 0.5 ? 2 * t * t : 1 - (float)Math.pow(-2 * t + 2, 2) / 2;
            t = smoothedT;
        }
        
        velocityYaw = velocityYaw * 0.9f + (diffYaw * t - velocityYaw) * 0.1f;
        velocityPitch = velocityPitch * 0.9f + (diffPitch * t - velocityPitch) * 0.1f;
        
        float newYaw = currentYaw + velocityYaw;
        float newPitch = currentPitch + velocityPitch;
        
        if (prediction) {
            float predictedYaw = newYaw + (targetYaw - newYaw) * predictionAmount;
            float predictedPitch = newPitch + (targetPitch - newPitch) * predictionAmount;
            newYaw = newYaw * (1 - predictionAmount) + predictedYaw * predictionAmount;
            newPitch = newPitch * (1 - predictionAmount) + predictedPitch * predictionAmount;
        }
        
        return new float[]{newYaw, newPitch};
    }

    private float tween(float t) {
        return smoothingAmount * t;
    }

    public float calculateHumanError(float targetValue) {
        float errorRange = 0.5f;
        float humanError = (random.nextFloat() - 0.5f) * errorRange;
        
        float microMovement = (random.nextFloat() - 0.5f) * 0.2f;
        
        return targetValue + humanError + microMovement;
    }

    public float[] applyHumanVariance(float yaw, float pitch) {
        return new float[]{
            calculateHumanError(yaw),
            calculateHumanError(pitch)
        };
    }

    public void setRotationMode(RotationMode mode) {
        this.currentMode = mode;
    }

    public void setLerpFactor(float factor) {
        this.lerpFactor = Math.max(0.01f, Math.min(1.0f, factor));
    }

    public void setSmoothingAmount(float amount) {
        this.smoothingAmount = Math.max(0.05f, Math.min(1.0f, amount));
    }

    public void setBezierControlPoint(float point) {
        this.bezierControlPoint = Math.max(0.0f, Math.min(1.0f, point));
    }

    public void setAimSmoothing(boolean enabled) {
        this.aimSmoothing = enabled;
    }

    public void setPrediction(boolean enabled) {
        this.prediction = enabled;
    }

    private float wrapAngle(float angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    public void update() {
        if (client.player != null) {
            lastYaw = client.player.getYaw();
            lastPitch = client.player.getPitch();
        }
    }

    public float getVelocityYaw() {
        return velocityYaw;
    }

    public float getVelocityPitch() {
        return velocityPitch;
    }
}
