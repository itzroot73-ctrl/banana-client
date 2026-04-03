package com.fusionclient.module;

import com.fusionclient.combat.MacePvPModule;
import com.fusionclient.combat.SmartElytraModule;
import com.fusionclient.combat.WindChargeModule;
import com.fusionclient.config.ConfigManager;
import com.fusionclient.inventory.AutoTotemModule;
import com.fusionclient.settings.BooleanSetting;
import com.fusionclient.settings.ModeSetting;
import com.fusionclient.settings.Setting;
import com.fusionclient.settings.SliderSetting;
import com.fusionclient.visual.TargetRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ModuleManager {
    private static ModuleManager instance;
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("BananaClient");
    
    private final Map<String, Module> moduleMap;
    private final Map<ModuleCategory, List<Module>> modulesByCategory;
    private final List<Module> enabledModules;
    private final List<Consumer<Module>> globalListeners;
    private final Map<String, Long> moduleCooldowns;
    
    private boolean initialized = false;
    private MinecraftClient client;

    private ModuleManager() {
        this.moduleMap = new ConcurrentHashMap<>(200);
        this.modulesByCategory = new EnumMap<>(ModuleCategory.class);
        this.enabledModules = new CopyOnWriteArrayList<>();
        this.globalListeners = new ArrayList<>();
        this.moduleCooldowns = new ConcurrentHashMap<>();
        this.client = MinecraftClient.getInstance();
        
        for (ModuleCategory category : ModuleCategory.values()) {
            modulesByCategory.put(category, new ArrayList<>());
        }
    }

    public static ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    public void initialize() {
        if (initialized) return;
        
        LOGGER.info("Initializing Banana Client Module System...");
        
        registerDefaultModules();
        
        ConfigManager.getInstance().load();
        
        initialized = true;
        LOGGER.info("Registered {} modules across {} categories", 
            moduleMap.size(), ModuleCategory.values().length);
    }

    private void registerDefaultModules() {
        registerModule(new MacePvPModule());
        registerModule(new SmartElytraModule());
        registerModule(new WindChargeModule());
        registerModule(new AutoTotemModule());
        
        registerModule(new Module("Kill Aura", "Auto attack nearby entities", ModuleCategory.COMBAT));
        registerModule(new Module("Trigger Bot", "Click when crosshair on entity", ModuleCategory.COMBAT));
        registerModule(new Module("Aim Assist", "Smooth aim towards entities", ModuleCategory.COMBAT));
        registerModule(new Module("Auto Mace", "Critical smash when falling", ModuleCategory.COMBAT));
        registerModule(new Module("Velocity", "Modify knockback taken", ModuleCategory.COMBAT));
        registerModule(new Module("HitBox", "Expand entity hitboxes", ModuleCategory.COMBAT));
        registerModule(new Module("KeepSprint", "Keep sprinting after hitting", ModuleCategory.COMBAT));
        registerModule(new Module("AntiFire", "Fire immunity toggle", ModuleCategory.COMBAT));
        
        registerModule(new Module("Auto Elytra", "Auto deploy Elytra when falling", ModuleCategory.MOVEMENT));
        registerModule(new Module("Auto Wind Charge", "Auto use Wind Charge when jumping", ModuleCategory.MOVEMENT));
        registerModule(new Module("Flight", "Creative-like flight", ModuleCategory.MOVEMENT));
        registerModule(new Module("Speed", "Increase movement speed", ModuleCategory.MOVEMENT));
        registerModule(new Module("NoSlow", "Remove slow effects", ModuleCategory.MOVEMENT));
        registerModule(new Module("Sprint", "Auto sprint", ModuleCategory.MOVEMENT));
        registerModule(new Module("Jesus", "Walk on water", ModuleCategory.MOVEMENT));
        registerModule(new Module("Phase", "Walk through blocks", ModuleCategory.MOVEMENT));
        
        registerModule(new Module("ESP", "Highlight entities", ModuleCategory.RENDER));
        registerModule(new Module("Tracers", "Draw lines to entities", ModuleCategory.RENDER));
        registerModule(new Module("Chams", "Make entities transparent", ModuleCategory.RENDER));
        registerModule(new Module("Fullbright", "Remove darkness", ModuleCategory.RENDER));
        registerModule(new Module("NoOverlay", "Remove potion overlay", ModuleCategory.RENDER));
        registerModule(new Module("Camera", "Third person camera options", ModuleCategory.RENDER));
        registerModule(new Module("Zoom", "Mouse zoom", ModuleCategory.RENDER));
        registerModule(new Module("Nametags", "Enhanced nametags", ModuleCategory.RENDER));
        
        registerModule(new Module("Xray", "Reveal ores", ModuleCategory.WORLD));
        registerModule(new Module("VoidESP", "Highlight void holes", ModuleCategory.WORLD));
        registerModule(new Module("StorageESP", "Highlight chests/shulkers", ModuleCategory.WORLD));
        registerModule(new Module("AntiVoid", "Teleport out of void", ModuleCategory.WORLD));
        registerModule(new Module("NoWeather", "Remove weather effects", ModuleCategory.WORLD));
        registerModule(new Module("TimeChanger", "Change world time", ModuleCategory.WORLD));
        
        registerModule(new Module("AutoAccept", "Auto accept party/duel", ModuleCategory.PLAYER));
        registerModule(new Module("AutoReconnect", "Auto reconnect on kick", ModuleCategory.PLAYER));
        registerModule(new Module("NoFOV", "Remove FOV change", ModuleCategory.PLAYER));
        registerModule(new Module("SkinBlink", "Toggle skin visibility", ModuleCategory.PLAYER));
        registerModule(new Module("Timer", "Game speed modifier", ModuleCategory.PLAYER));
        
        registerModule(new Module("InventoryManager", "Sort inventory", ModuleCategory.INVENTORY));
        registerModule(new Module("ItemSaver", "Prevent item durability loss", ModuleCategory.INVENTORY));
        registerModule(new Module("AutoDrop", "Auto drop items", ModuleCategory.INVENTORY));
        registerModule(new Module("AutoSteal", "Steal from inventories", ModuleCategory.INVENTORY));
        registerModule(new Module("ChestStealer", "Quickly take items", ModuleCategory.INVENTORY));
        
        registerModule(new Module("AutoTip", "Auto send tips", ModuleCategory.CHAT));
        registerModule(new Module("ChatModifier", "Modify chat messages", ModuleCategory.CHAT));
        registerModule(new Module("Spammer", "Auto spam chat", ModuleCategory.CHAT));
        registerModule(new Module("NameHistory", "Show name history", ModuleCategory.CHAT));
        
        registerModule(new Module("PacketFly", "Packet-based flying", ModuleCategory.EXPLOIT));
        registerModule(new Module("Phase", "Walk through blocks", ModuleCategory.EXPLOIT));
        registerModule(new Module("Reach", "Extended reach", ModuleCategory.EXPLOIT));
        registerModule(new Module("NoFall", "Prevent fall damage", ModuleCategory.EXPLOIT));
        registerModule(new Module("Crasher", "Crash other players", ModuleCategory.EXPLOIT));
        
        registerModule(new Module("AutoCraft", "Auto craft items", ModuleCategory.MISC));
        registerModule(new Module("AutoFish", "Auto fish when bite", ModuleCategory.MISC));
        registerModule(new Module("AutoSoup", "Auto eat soup", ModuleCategory.MISC));
        registerModule(new Module("ChestAura", "Open nearby chests", ModuleCategory.MISC));
        registerModule(new Module("PearlAura", "Auto throw pearls", ModuleCategory.MISC));
        
        populateSampleModules();
    }
    
    private void populateSampleModules() {
        String[] combatNames = {"WTap", "CTap", "BPS", "AimPredict", "Criticals", "SuperPhys"};
        String[] renderNames = {"Blur", "Scoreboard", "FakePlayer", "HitMarkers", "MotionBlur"};
        String[] worldNames = {"AntiCactus", "BedNuker", "TreeNuker", "MobNuker", "LiquidNuker"};
        String[] playerNames = {"PacketCanceller", "LatencyCompensator", "Scaffold"};
        
        for (String name : combatNames) {
            registerModule(new Module(name, ModuleCategory.COMBAT));
        }
        for (String name : renderNames) {
            registerModule(new Module(name, ModuleCategory.RENDER));
        }
        for (String name : worldNames) {
            registerModule(new Module(name, ModuleCategory.WORLD));
        }
        for (String name : playerNames) {
            registerModule(new Module(name, ModuleCategory.PLAYER));
        }
    }

    private void registerModule(Module module) {
        moduleMap.put(module.getName(), module);
        modulesByCategory.get(module.getCategory()).add(module);
    }

    public Module getModule(String name) {
        return moduleMap.get(name);
    }

    public List<Module> getModulesByCategory(ModuleCategory category) {
        return modulesByCategory.getOrDefault(category, Collections.emptyList());
    }

    public Collection<Module> getAllModules() {
        return moduleMap.values();
    }

    public List<Module> getEnabledModules() {
        return enabledModules;
    }

    public boolean isModuleEnabled(String name) {
        Module module = moduleMap.get(name);
        return module != null && module.isEnabled();
    }

    public void toggleModule(String name) {
        Module module = moduleMap.get(name);
        if (module != null) {
            module.toggle();
            updateEnabledList(module);
        }
    }

    public void setModuleEnabled(String name, boolean enabled) {
        Module module = moduleMap.get(name);
        if (module != null) {
            module.setEnabled(enabled);
            updateEnabledList(module);
        }
    }

    private void updateEnabledList(Module module) {
        if (module.isEnabled()) {
            if (!enabledModules.contains(module)) {
                enabledModules.add(module);
            }
        } else {
            enabledModules.remove(module);
        }
    }

    public void onTick() {
        if (client.player == null || client.world == null) return;
        
        long tickStart = System.nanoTime();
        
        for (Module module : enabledModules) {
            try {
                module.onTick();
            } catch (Exception e) {
                LOGGER.error("Error in module " + module.getName(), e);
            }
        }
        
        Entity target = findNearestCombatTarget();
        if (target != null) {
            TargetRenderer.getInstance().setTarget(target);
        } else {
            TargetRenderer.getInstance().clearTarget();
        }
        
        long tickTime = System.nanoTime() - tickStart;
        if (tickTime > 5000000) {
            LOGGER.warn("Module tick took {}ms", tickTime / 1000000);
        }
    }

    private Entity findNearestCombatTarget() {
        boolean combatEnabled = enabledModules.stream()
            .anyMatch(m -> m.getCategory() == ModuleCategory.COMBAT && m.isEnabled());
        
        if (!combatEnabled) return null;
        
        Vec3d playerPos = client.player.getPos();
        double range = 4.0;
        
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

    public void onRender() {
        for (Module module : enabledModules) {
            try {
                module.onRender();
            } catch (Exception e) {
                LOGGER.error("Error in module render " + module.getName(), e);
            }
        }
    }

    public void addGlobalListener(Consumer<Module> listener) {
        globalListeners.add(listener);
    }

    public void registerModuleSetting(String moduleName, Setting<?> setting) {
        Module module = moduleMap.get(moduleName);
        if (module != null) {
            module.addSetting(setting);
        }
    }

    public int getTotalModuleCount() {
        return moduleMap.size();
    }

    public int getEnabledModuleCount() {
        return enabledModules.size();
    }
}
