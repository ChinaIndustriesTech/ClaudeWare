package com.atl.module.modules;

import com.atl.module.management.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PotMacro extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public NumberSetting minDelay = new NumberSetting("Min Delay", 50, 50, 200, 10);
    public NumberSetting maxDelay = new NumberSetting("Max Delay", 80, 50, 300, 10);

    private boolean running = false;

    public PotMacro() {
        super("PotMacro", "Legit potion splash macro", Category.COMBAT);
        addSettings(minDelay, maxDelay);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer == null || running) {
            setEnabled(false);
            return;
        }

        int potSlot = findPotionSlot();
        if (potSlot == -1) {
            setEnabled(false);
            return;
        }

        running = true;
        int originalSlot = mc.thePlayer.inventory.currentItem;

        // --- LEGIT INPUT SIMULATION SEQUENCE ---
        
        // 1. Human delay before swapping
        long swapDelay = getRandomDelay();
        
        scheduler.schedule(() -> {
            mc.addScheduledTask(() -> {
                mc.thePlayer.inventory.currentItem = potSlot;
                
                // 2. Human delay before clicking
                long throwDelay = getRandomDelay();
                
                scheduler.schedule(() -> {
                    mc.addScheduledTask(() -> {
                        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
                        KeyBinding.setKeyBindState(useKey, true);
                        KeyBinding.onTick(useKey);
                        KeyBinding.setKeyBindState(useKey, false);

                        // 3. Human delay before swapping back
                        long restoreDelay = getRandomDelay();
                        
                        scheduler.schedule(() -> {
                            mc.addScheduledTask(() -> {
                                int swordSlot = findSwordSlot();
                                mc.thePlayer.inventory.currentItem = (swordSlot != -1) ? swordSlot : originalSlot;
                                running = false;
                                setEnabled(false);
                            });
                        }, restoreDelay, TimeUnit.MILLISECONDS);
                    });
                }, throwDelay, TimeUnit.MILLISECONDS);
            });
        }, swapDelay, TimeUnit.MILLISECONDS);
    }

    private long getRandomDelay() {
        int min = (int) Math.min(minDelay.value, maxDelay.value);
        int max = (int) Math.max(minDelay.value, maxDelay.value);
        return min + random.nextInt(max - min + 1);
    }

    private int findPotionSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemPotion && ItemPotion.isSplash(stack.getMetadata())) {
                return i;
            }
        }
        return -1;
    }

    private int findSwordSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemSword) return i;
        }
        return -1;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("Min: " + (int)minDelay.value + "ms", "Max: " + (int)maxDelay.value + "ms");
    }

    @Override
    public void onDisable() {
        running = false;
    }
}
