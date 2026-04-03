package com.fusionclient.util;

import java.util.Random;

public class CPSUtils {
    private static final Random random = new Random();
    private static long lastClickTime = 0;
    private static int clickCount = 0;
    private static long cpsWindowStart = 0;
    private static int currentCPS = 0;
    
    public static void registerClick() {
        long currentTime = System.currentTimeMillis();
        
        if (cpsWindowStart == 0) {
            cpsWindowStart = currentTime;
        }
        
        if (currentTime - cpsWindowStart >= 1000) {
            currentCPS = clickCount;
            clickCount = 0;
            cpsWindowStart = currentTime;
        }
        
        clickCount++;
        lastClickTime = currentTime;
    }
    
    public static int getCurrentCPS() {
        return currentCPS;
    }
    
    public static boolean shouldClick(long nextClickTime) {
        return System.currentTimeMillis() >= nextClickTime;
    }
    
    public static long calculateNextClickDelay(int minCPS, int maxCPS) {
        int cps = minCPS + random.nextInt(maxCPS - minCPS + 1);
        long baseDelay = 1000 / cps;
        long jitter = random.nextInt(50) - 25;
        return baseDelay + jitter;
    }
    
    public static long getRandomDelay(int minCPS, int maxCPS) {
        int targetCPS = minCPS + random.nextInt(maxCPS - minCPS + 1);
        double delay = 1000.0 / targetCPS;
        double randomFactor = 0.85 + random.nextDouble() * 0.3;
        return (long)(delay * randomFactor);
    }
    
    public static boolean canAttack(int minCPS, int maxCPS) {
        long currentTime = System.currentTimeMillis();
        if (lastClickTime == 0) {
            return true;
        }
        
        long expectedDelay = getRandomDelay(minCPS, maxCPS);
        return (currentTime - lastClickTime) >= expectedDelay;
    }
    
    public static long getTimeSinceLastClick() {
        if (lastClickTime == 0) return Long.MAX_VALUE;
        return System.currentTimeMillis() - lastClickTime;
    }
}
