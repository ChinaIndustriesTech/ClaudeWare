package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;


public class PotMacro extends Module {

    private final NumberSetting healthThreshold = new NumberSetting("Health", 12.0, 1.0, 20.0, 0.5);

    private enum State { IDLE, SWAP, THROW, RESTORE }
    private State state = State.IDLE;

    private int originalSlot = -1;
    private int potionSlot = -1;
    private int tickDelay = 0;

    public PotMacro() {
        super("PotMacro", "Automatically splashes a potion when low on health", Category.COMBAT);
        addSettings(healthThreshold);
    }

    @Override
    public void onEnable() {
        state = State.IDLE;
        originalSlot = -1;
        potionSlot = -1;
        tickDelay = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || mc.thePlayer == null || event.phase != TickEvent.Phase.START) {
            return;
        }

        // We remove the 'for' loop chaining because Minecraft's inventory 
        // needs at least one tick to register a slot change before throwing.
        if (tickDelay > 0) {
            tickDelay--;
        } else {
            processState(mc);
        }
    }

    private void processState(Minecraft mc) {
        switch (state) {
            case IDLE:
                if (mc.thePlayer.getHealth() <= (float) healthThreshold.value) {
                    potionSlot = findPotionSlot(mc);
                    if (potionSlot != -1) {
                        originalSlot = mc.thePlayer.inventory.currentItem;
                        state = State.SWAP;
                    }
                } else {
                    setEnabled(false); // Health is fine, no need to run
                }
                break;

            case SWAP:
                // Press the hotbar key and update local state
                KeyBinding.onTick(mc.gameSettings.keyBindsHotbar[potionSlot].getKeyCode());
                mc.thePlayer.inventory.currentItem = potionSlot;

                state = State.THROW;
                tickDelay = 1; // Wait 1 tick for the engine to register the swap
                break;

            case THROW:
                // Replicate Right Click
                KeyBinding.onTick(mc.gameSettings.keyBindUseItem.getKeyCode());
                
                state = State.RESTORE;
                tickDelay = 1; // Wait 1 tick for the potion to be processed
                break;

            case RESTORE:
                // Only restore if we actually changed slots
                if (originalSlot != -1 && originalSlot != potionSlot) {
                    KeyBinding.onTick(mc.gameSettings.keyBindsHotbar[originalSlot].getKeyCode());
                    mc.thePlayer.inventory.currentItem = originalSlot;
                }
                
                setEnabled(false); // Task finished
                break;
        }
    }

    private int findPotionSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemPotion) {
                if (ItemPotion.isSplash(stack.getMetadata())) {
                    return i;
                }
            }
        }
        return -1;
    }
}
