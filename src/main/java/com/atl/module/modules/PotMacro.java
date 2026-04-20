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

public class PotMacro extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public BooleanSetting stopUselessPot = new BooleanSetting("Stop Useless Pot", true);
    public NumberSetting minPotHP = new NumberSetting("Min Pot HP", 13, 0, 20, 1);
    public BooleanSetting throwBowls = new BooleanSetting("Throw Bowls", true);
    public BooleanSetting throwBottle = new BooleanSetting("Throw Bottle", true);
    public NumberSetting throwDelay = new NumberSetting("Throw Delay", 20, 0, 500, 10);
    public NumberSetting refillDelay = new NumberSetting("Refill Delay", 50, 0, 500, 10);
    public BooleanSetting refillSoup = new BooleanSetting("Refill Soup", true);
    public BooleanSetting refillPots = new BooleanSetting("Refill Pots", true);
    public BooleanSetting randomizeFill = new BooleanSetting("Randomize Fill", false);

    private long timerBowl = 0;
    private long timerPot = 0;
    private long refillBowlTimer = 0;
    private long refillPotTimer = 0;
    private int inventoryOpenTicks = 0;

    public PotMacro() {
        super("PotionHelper", "Helps with healing and inventory management", Category.MISC);
        addSettings(stopUselessPot, minPotHP, throwBowls, throwBottle, throwDelay, refillDelay, refillSoup, refillPots, randomizeFill);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null || event.phase != TickEvent.Phase.START) {
            return;
        }

        // --- Stop Useless Potion ---
        if (stopUselessPot.isEnabled()) {
            ItemStack stack = mc.thePlayer.inventory.getCurrentItem();
            if (stack != null && stack.getItem() instanceof ItemPotion) {
                if (ItemPotion.isSplash(stack.getMetadata()) && mc.thePlayer.getHealth() > (float) minPotHP.value) {
                    // Use KeyBinding.setKeyBindState instead of direct .pressed access
                    KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                }
            }
        }

        // --- Throw Bowls ---
        if (throwBowls.isEnabled()) {
            ItemStack stack = mc.thePlayer.inventory.getCurrentItem();
            if (stack != null && stack.getItem() == Items.bowl) {
                if (System.currentTimeMillis() - timerBowl > (long) (throwDelay.value + (30 * Math.random()))) {
                    mc.thePlayer.dropOneItem(true);
                    timerBowl = System.currentTimeMillis();
                }
            }
        }

        // --- Throw Bottle ---
        if (throwBottle.isEnabled()) {
            ItemStack stack = mc.thePlayer.inventory.getCurrentItem();
            if (stack != null && stack.getItem() == Items.glass_bottle) {
                if (System.currentTimeMillis() - timerPot > (long) (throwDelay.value + (30 * Math.random()))) {
                    mc.thePlayer.dropOneItem(true);
                    timerPot = System.currentTimeMillis();
                }
            }
        }

        // --- Refill Logic ---
        if (!(mc.currentScreen instanceof GuiInventory)) {
            inventoryOpenTicks = 0;
        } else {
            inventoryOpenTicks++;
        }

        if (inventoryOpenTicks >= 3 && mc.currentScreen instanceof GuiInventory) {
            if (refillSoup.isEnabled() && isHotBarEmpty() && invContainsSoup()) {
                int soupSlot = getRandomSoupSlot();
                if (soupSlot != -1) {
                    if (System.currentTimeMillis() - refillBowlTimer > (long) (refillDelay.value + (Math.random() * 10))) {
                        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, soupSlot, 0, 1, mc.thePlayer);
                        refillBowlTimer = System.currentTimeMillis();
                    }
                }
            }

            if (refillPots.isEnabled() && isHotBarEmpty() && invContainsPotions()) {
                int potSlot = getRandomPotionSlot();
                if (potSlot != -1) {
                    if (System.currentTimeMillis() - refillPotTimer > (long) (refillDelay.value + (Math.random() * 10))) {
                        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, potSlot, 0, 1, mc.thePlayer);
                        refillPotTimer = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    private boolean invContainsPotions() {
        for (int i = 9; i <= 35; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemPotion) {
                if (ItemPotion.isSplash(stack.getMetadata())) return true;
            }
        }
        return false;
    }

    private boolean invContainsSoup() {
        for (int i = 9; i <= 35; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemSoup) return true;
        }
        return false;
    }

    private int getRandomPotionSlot() {
        for (int i = 9; i <= 35; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemPotion) {
                if (ItemPotion.isSplash(stack.getMetadata())) return i;
            }
        }
        return -1;
    }

    private int getRandomSoupSlot() {
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
        return Arrays.asList("Pot HP: " + (int)minPotHP.value, "Refill: " + (int)refillDelay.value + "ms");
    }
}
