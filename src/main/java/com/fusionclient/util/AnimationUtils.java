package com.fusionclient.util;

public class AnimationUtils {
    
    public static float easeOutCubic(float t) {
        return 1.0f - (float)Math.pow(1.0f - t, 3.0);
    }
    
    public static float easeInOutCubic(float t) {
        if (t < 0.5f) {
            return 4.0f * t * t * t;
        } else {
            return 1.0f - (float)Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f;
        }
    }
    
    public static float easeOutQuad(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }
    
    public static float easeInOutQuad(float t) {
        if (t < 0.5f) {
            return 2.0f * t * t;
        } else {
            return 1.0f - (float)Math.pow(-2.0f * t + 2.0f, 2.0f) / 2.0f;
        }
    }
    
    public static float easeOutBounce(float t) {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        
        if (t < 1.0f / d1) {
            return n1 * t * t;
        } else if (t < 2.0f / d1) {
            t -= 1.5f / d1;
            return n1 * t * t + 0.75f;
        } else if (t < 2.5f / d1) {
            t -= 2.25f / d1;
            return n1 * t * t + 0.9375f;
        } else {
            t -= 2.625f / d1;
            return n1 * t * t + 0.984375f;
        }
    }
    
    public static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }
    
    public static float smoothStep(float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        return t * t * (3.0f - 2.0f * t);
    }
    
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static float approach(float current, float target, float rate) {
        if (current < target) {
            return Math.min(current + rate, target);
        } else if (current > target) {
            return Math.max(current - rate, target);
        }
        return current;
    }
}
