package com.atl.module.modules;

import com.atl.module.management.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.List;

public class PotionHelper extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public BooleanSetting stopUselessPot = new BooleanSetting("Stop Useless Pot", true);
    public NumberSetting minPotHP = new NumberSetting("Min Pot HP", 12.0, 1.0, 20.0, 0.5);
    
    public BooleanSetting throwBowls = new BooleanSetting("Throw Bowls", true);
    public BooleanSetting throwBottle = new BooleanSetting("Throw Bottle", true);
    public NumberSetting throwDelay = new NumberSetting("Trash Delay", 20, 0, 500, 10);
    
    public BooleanSetting refillSoup = new BooleanSetting("Refill Soup", true);
    public BooleanSetting refillPots = new BooleanSetting("Refill Pots", true);
    public NumberSetting refillDelay = new NumberSetting("Refill Delay", 50, 0, 500, 10);

    private long timerBowl = 0;
    private long timerPot = 0;
    private long refillTimer = 0;
    private int inventoryOpenTicks = 0;

    public PotionHelper() {
        super("PotionHelper", "Utility for pots, soup, and inventory", Category.MISC);
        addSettings(stopUselessPot, minPotHP, throwBowls, throwBottle, throwDelay, refillSoup, refillPots, refillDelay);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null || event.phase != TickEvent.Phase.START) {
            return;
        }

        // --- 1. Safe Pot (Legit Input Blocking) ---
        if (stopUselessPot.isEnabled()) {
            ItemStack heldItem = mc.thePlayer.getHeldItem();
            if (heldItem != null && heldItem.getItem() instanceof ItemPotion) {
                if (ItemPotion.isSplash(heldItem.getMetadata()) && mc.thePlayer.getHealth() > (float) minPotHP.value) {
                    // LEGIT FIX: Only release the key state. 
                    // Do NOT touch the delay timer as that messes with FastPlace.
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                }
            }
        }

        handleTrashing();
        handleRefilling();
    }

    private void handleTrashing() {
        ItemStack stack = mc.thePlayer.inventory.getCurrentItem();
        if (stack == null) return;

        boolean isBowl = throwBowls.isEnabled() && stack.getItem() == Items.bowl;
        boolean isBottle = throwBottle.isEnabled() && stack.getItem() == Items.glass_bottle;

        if (isBowl || isBottle) {
            long lastTime = isBowl ? timerBowl : timerPot;
            if (System.currentTimeMillis() - lastTime > (long) (throwDelay.value + (30 * Math.random()))) {
                mc.thePlayer.dropOneItem(true);
                if (isBowl) timerBowl = System.currentTimeMillis();
                else timerPot = System.currentTimeMillis();
            }
        }
    }

    private void handleRefilling() {
        if (!(mc.currentScreen instanceof GuiInventory)) {
            inventoryOpenTicks = 0;
            return;
        }

        inventoryOpenTicks++;
        if (inventoryOpenTicks < 3) return;

        if (System.currentTimeMillis() - refillTimer < (long) (refillDelay.value + (Math.random() * 10))) return;

        if (isHotBarEmpty()) {
            int targetSlot = -1;
            if (refillSoup.isEnabled()) targetSlot = findSoupInInventory();
            if (targetSlot == -1 && refillPots.isEnabled()) targetSlot = findPotionInInventory();

            if (targetSlot != -1) {
                mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, targetSlot, 0, 1, mc.thePlayer);
                refillTimer = System.currentTimeMillis();
            }
        }
    }

    private int findPotionInInventory() {
        for (int i = 9; i <= 35; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemPotion && ItemPotion.isSplash(stack.getMetadata())) return i;
        }
        return -1;
    }

    private int findSoupInInventory() {
        for (int i = 9; i <= 35; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemSoup) return i;
        }
        return -1;
    }

    private boolean isHotBarEmpty() {
        for (int i = 0; i < 9; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) == null) return true;
        }
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("Min HP: " + minPotHP.value, "Refill: " + (int)refillDelay.value + "ms");
    }
}
