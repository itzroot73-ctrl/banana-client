package com.fusionclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.stream.Collectors;

public abstract class CombatModule extends Module {
    protected MinecraftClient client;
    
    public CombatModule(String name, String description, ModuleCategory category) {
        super(name, description, category);
        this.client = MinecraftClient.getInstance();
    }
    
    protected Entity findNearestEntity(double range, boolean targetPlayers, boolean targetMobs, boolean targetAnimals) {
        if (client.world == null || client.player == null) return null;
        
        List<Entity> entities = client.world.getEntities();
        Vec3d playerPos = client.player.getPos();
        
        return entities.stream()
            .filter(e -> e != client.player)
            .filter(e -> e instanceof LivingEntity)
            .filter(e -> !((LivingEntity) e).isDead())
            .filter(e -> {
                double dist = playerPos.squaredDistanceTo(e.getPos());
                return dist < range * range;
            })
            .filter(e -> {
                if (e instanceof PlayerEntity && targetPlayers) return true;
                if (e instanceof MobEntity && targetMobs) return true;
                if (e instanceof PassiveEntity && targetAnimals) return true;
                return false;
            })
            .min((e1, e2) -> Double.compare(
                playerPos.squaredDistanceTo(e1.getPos()),
                playerPos.squaredDistanceTo(e2.getPos())
            ))
            .orElse(null);
    }
    
    protected Entity findEntityInCrosshair(double maxDistance) {
        if (client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult) {
            EntityHitResult hitResult = (EntityHitResult) client.crosshairTarget;
            Entity entity = hitResult.getEntity();
            if (entity instanceof LivingEntity && entity != client.player) {
                double dist = client.player.getPos().distanceTo(entity.getPos());
                if (dist <= maxDistance) {
                    return entity;
                }
            }
        }
        return null;
    }
    
    protected float[] calculateRotation(Vec3d targetPos) {
        Vec3d playerPos = client.player.getPos().add(0, client.player.getStandingEyeHeight(), 0);
        double dx = targetPos.x - playerPos.x;
        double dy = targetPos.y - playerPos.y;
        double dz = targetPos.z - playerPos.z;
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horizontalDist));
        
        return new float[]{yaw, pitch};
    }
}
