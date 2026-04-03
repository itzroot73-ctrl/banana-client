package com.fusionclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

public class TargetUtils {
    
    public enum SortMode {
        DISTANCE,
        HEALTH,
        FOV
    }
    
    public static Entity findNearestTarget(double range, boolean targetPlayers, boolean targetMobs, boolean targetAnimals, SortMode sortMode) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return null;
        
        List<Entity> entities = client.world.getEntities();
        Vec3d playerPos = client.player.getPos();
        
        return entities.stream()
            .filter(e -> e != client.player)
            .filter(e -> e instanceof LivingEntity)
            .filter(e -> !((LivingEntity) e).isDead())
            .filter(e -> !e.isInvisible())
            .filter(e -> playerPos.distanceTo(e.getPos()) <= range)
            .filter(e -> {
                if (e instanceof PlayerEntity && targetPlayers) return true;
                if (e instanceof MobEntity && targetMobs) return true;
                if (e instanceof PassiveEntity && targetAnimals) return true;
                return false;
            })
            .min(getComparator(sortMode, playerPos))
            .orElse(null);
    }
    
    public static Entity findTargetInFOV(double range, double fovAngle, boolean targetPlayers, boolean targetMobs, boolean targetAnimals) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return null;
        
        List<Entity> entities = client.world.getEntities();
        Vec3d playerPos = client.player.getPos();
        Vec3d playerLookDir = client.player.getRotationVecClient();
        
        return entities.stream()
            .filter(e -> e != client.player)
            .filter(e -> e instanceof LivingEntity)
            .filter(e -> !((LivingEntity) e).isDead())
            .filter(e -> playerPos.distanceTo(e.getPos()) <= range)
            .filter(e -> {
                Vec3d toEntity = e.getPos().subtract(playerPos).normalize();
                double dot = playerLookDir.dot(toEntity);
                double angle = Math.toDegrees(Math.acos(dot));
                return angle <= fovAngle;
            })
            .filter(e -> {
                if (e instanceof PlayerEntity && targetPlayers) return true;
                if (e instanceof MobEntity && targetMobs) return true;
                if (e instanceof PassiveEntity && targetAnimals) return true;
                return false;
            })
            .min(getComparator(SortMode.DISTANCE, playerPos))
            .orElse(null);
    }
    
    private static Comparator<Entity> getComparator(SortMode mode, Vec3d playerPos) {
        switch (mode) {
            case HEALTH:
                return Comparator.comparingDouble(e -> {
                    if (e instanceof LivingEntity) {
                        return ((LivingEntity) e).getHealth();
                    }
                    return Double.MAX_VALUE;
                });
            case FOV:
                return Comparator.comparingDouble(e -> {
                    if (client.player == null) return Double.MAX_VALUE;
                    Vec3d lookDir = client.player.getRotationVecClient();
                    Vec3d toEntity = e.getPos().subtract(client.player.getPos()).normalize();
                    return -lookDir.dot(toEntity);
                });
            case DISTANCE:
            default:
                return Comparator.comparingDouble(e -> playerPos.squaredDistanceTo(e.getPos()));
        }
    }
    
    private static MinecraftClient client() {
        return MinecraftClient.getInstance();
    }
}
