package com.fusionclient.inventory;

import com.fusionclient.module.Module;
import com.fusionclient.module.ModuleCategory;
import com.fusionclient.settings.BooleanSetting;
import com.fusionclient.settings.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickWindowC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Random;

public class AutoTotemModule extends Module {
    private final MinecraftClient client;
    private final Random random;
    private final InventorySimulator inventorySimulator;
    
    private boolean hasTotemInOffhand = false;
    private boolean isSwapping = false;
    private long lastSwapTime = 0;
    private int totemCount = 0;
    private boolean justSwapped = false;
    private float animationProgress = 0.0f;

    public AutoTotemModule() {
        super("Auto Totem", "Legit auto totem swap", ModuleCategory.PLAYER);
        this.client = MinecraftClient.getInstance();
        this.random = new Random();
        this.inventorySimulator = new InventorySimulator();
        
        addSetting(new BooleanSetting("Legit Mode", "Human-like inventory movements", true));
        addSetting(new BooleanSetting("Sound Alert", "Play sound on swap", false));
        addSetting(new SliderSetting("Health Threshold", "Swap below health", 6.0f, 1.0f, 10.0f, 0.5f));
        addSetting(new SliderSetting("Reaction Time", "Delay before swap ms", 80f, 30f, 200f, 10f));
        addSetting(new SliderSetting("Swap Delay", "Delay between swaps ms", 500f, 200f, 1500f, 50f));
    }

    @Override
    public void onTick() {
        if (client.player == null || client.player.isDead()) return;
        
        if (isSwapping) {
            animationProgress += 0.1f;
            if (animationProgress > 1.0f) animationProgress = 1.0f;
            return;
        }
        
        if (justSwapped) {
            animationProgress -= 0.15f;
            if (animationProgress <= 0.0f) {
                animationProgress = 0.0f;
                justSwapped = false;
            }
        }
        
        long timeSinceLastSwap = System.currentTimeMillis() - lastSwapTime;
        SliderSetting swapDelaySetting = (SliderSetting) getSetting("Swap Delay");
        long swapDelay = swapDelaySetting != null ? (long) swapDelaySetting.getValueFloat() : 500;
        
        if (timeSinceLastSwap < swapDelay) return;
        
        updateTotemCount();
        
        checkOffhandTotem();
        
        BooleanSetting legitMode = (BooleanSetting) getSetting("Legit Mode");
        
        float health = client.player.getHealth();
        SliderSetting healthThresholdSetting = (SliderSetting) getSetting("Health Threshold");
        float healthThreshold = healthThresholdSetting != null ? healthThresholdSetting.getValueFloat() : 6.0f;
        
        if (health <= healthThreshold && !hasTotemInOffhand && totemCount > 0) {
            SliderSetting reactionTimeSetting = (SliderSetting) getSetting("Reaction Time");
            long reactionTime = reactionTimeSetting != null ? (long) reactionTimeSetting.getValueFloat() : 80;
            long randomDelay = reactionTime + random.nextInt((int)reactionTime);
            
            if (legitMode != null && legitMode.isEnabled()) {
                inventorySimulator.legitSwapTotem(randomDelay);
            } else {
                quickSwapTotem();
            }
            
            isSwapping = true;
            lastSwapTime = System.currentTimeMillis();
            justSwapped = true;
        }
        
        if (hasTotemInOffhand) {
            isSwapping = false;
        }
    }
    
    private void updateTotemCount() {
        totemCount = 0;
        
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                totemCount += stack.getCount();
            }
        }
    }
    
    private void checkOffhandTotem() {
        ItemStack offhandStack = client.player.getInventory().getOffHandStack();
        hasTotemInOffhand = offhandStack.getItem() == Items.TOTEM_OF_UNDYING;
    }
    
    private void quickSwapTotem() {
        if (client.player == null) return;
        
        int totemSlot = findTotemSlot();
        
        if (totemSlot == -1) return;
        
        int windowId = 0;
        int slotId = totemSlot + 9;
        int button = 0;
        
        client.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.ClickWindowC2SPacket(
            windowId, slotId, button, SlotActionType.PICKUP, 
            client.player.getInventory().getStack(totemSlot), 
            client.player.getInventory().getState()
        ));
        
        client.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.ClickWindowC2SPacket(
            windowId, 45, button, SlotActionType.PICKUP, 
            ItemStack.EMPTY, 
            client.player.getInventory().getState()
        ));
    }
    
    private int findTotemSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        
        for (int i = 9; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        
        return -1;
    }
    
    public int getTotemCount() {
        return totemCount;
    }
    
    public boolean hasTotemInOffhand() {
        return hasTotemInOffhand;
    }
    
    public float getAnimationProgress() {
        return animationProgress;
    }
    
    public boolean isJustSwapped() {
        return justSwapped;
    }

    @Override
    public void onEnable() {
        updateTotemCount();
        checkOffhandTotem();
    }

    @Override
    public void onDisable() {
        isSwapping = false;
        justSwapped = false;
    }
}
