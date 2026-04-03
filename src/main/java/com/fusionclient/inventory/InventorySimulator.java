package com.fusionclient.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickWindowC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Random;

public class InventorySimulator {
    private final MinecraftClient client;
    private final Random random;
    private boolean inventoryOpen = false;

    public InventorySimulator() {
        this.client = MinecraftClient.getInstance();
        this.random = new Random();
    }

    public void legitSwapTotem(long initialDelay) {
        new Thread(() -> {
            try {
                Thread.sleep(initialDelay);
                
                if (client.player == null) return;
                
                openInventory();
                
                long findDelay = 50 + random.nextInt(70);
                Thread.sleep(findDelay);
                
                int totemSlot = findTotemSlot();
                
                if (totemSlot == -1) {
                    closeInventory();
                    return;
                }
                
                clickSlot(totemSlot);
                
                long pickupDelay = 40 + random.nextInt(60);
                Thread.sleep(pickupDelay);
                
                clickSlot(45);
                
                long placeDelay = 40 + random.nextInt(60);
                Thread.sleep(placeDelay);
                
                clickSlot(45);
                
                Thread.sleep(30 + random.nextInt(40));
                
                closeInventory();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void openInventory() {
        if (client.player == null) return;
        
        client.player.networkHandler.sendPacket(new HandledScreenC2SPacket.OpenPacket(0));
        
        inventoryOpen = true;
    }

    private void closeInventory() {
        if (client.player == null) return;
        
        client.player.networkHandler.sendPacket(new CloseHandheldScreenC2SPacket());
        
        inventoryOpen = false;
    }

    private void clickSlot(int slot) {
        if (client.player == null) return;
        
        int windowId = 0;
        int button = 0;
        
        ClickWindowC2SPacket.Action action = new ClickWindowC2SPacket.Action(
            windowId, slot, button, SlotActionType.PICKUP, 
            client.player.getInventory().getStack(slot), 
            client.player.getInventory().getState()
        );
        
        client.player.networkHandler.sendPacket(new ClickWindowC2SPacket(windowId, slot, button, SlotActionType.PICKUP, 
            client.player.getInventory().getStack(slot), 
            client.player.getInventory().getState()));
    }

    private int findTotemSlot() {
        if (client.player == null) return -1;
        
        PlayerInventory inventory = client.player.getInventory();
        
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return i + 9;
            }
        }
        
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return i + 9;
            }
        }
        
        return -1;
    }

    public void silentSwapTotem() {
        if (client.player == null) return;
        
        int totemSlot = findTotemSlot();
        
        if (totemSlot == -1) return;
        
        sendSwapPackets(totemSlot, 45);
    }

    private void sendSwapPackets(int fromSlot, int toSlot) {
        if (client.player == null) return;
        
        int windowId = 0;
        
        client.player.networkHandler.sendPacket(new ClickWindowC2SPacket(
            windowId, fromSlot, 0, SlotActionType.PICKUP,
            client.player.getInventory().getStack(fromSlot - 9),
            client.player.getInventory().getState()
        ));
        
        try {
            Thread.sleep(20 + random.nextInt(30));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        client.player.networkHandler.sendPacket(new ClickWindowC2SPacket(
            windowId, toSlot, 0, SlotActionType.PICKUP,
            ItemStack.EMPTY,
            client.player.getInventory().getState()
        ));
    }

    public boolean isInventoryOpen() {
        return inventoryOpen;
    }
}
