package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

public class AutoArmor extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", 150, 50, 500, 10);
    private final Random random = new Random();
    private long nextActionTime;

    public AutoArmor() {
        super("AutoArmor", "Automatically equips the best armor in your inventory", Category.COMBAT);
        addSettings(delay);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || mc.thePlayer == null || event.phase != TickEvent.Phase.START) return;

        // Legit Mode: Only work in inventory or while standing perfectly still
        boolean inInv = mc.currentScreen instanceof GuiInventory;
        boolean standingStill = mc.thePlayer.motionX == 0 && mc.thePlayer.motionZ == 0;
        
        if (!inInv && !standingStill) return;
        if (System.currentTimeMillis() < nextActionTime) return;

        // armorInventory indices: 0: boots, 1: legs, 2: chest, 3: helmet
        for (int type = 0; type < 4; type++) {
            if (equipBest(type)) {
                // Set randomized delay for the next piece
                nextActionTime = System.currentTimeMillis() + (long) delay.value + random.nextInt(50);
                return;
            }
        }
    }

    private boolean equipBest(int armorType) {
        Minecraft mc = Minecraft.getMinecraft();
        int bestSlot = -1;
        ItemStack currentArmor = mc.thePlayer.inventory.armorInventory[armorType];
        float bestScore = getArmorScore(currentArmor);

        // Search the main inventory
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                // ItemArmor.armorType: 0: helmet, 1: chest, 2: legs, 3: boots
                // Mapping back to armorInventory order
                if (armor.armorType == (3 - armorType)) {
                    float score = getArmorScore(stack);
                    if (score > bestScore) {
                        bestScore = score;
                        bestSlot = i;
                    }
                }
            }
        }

        if (bestSlot != -1) {
            // If we are already wearing something, we must unequip it first
            if (currentArmor != null) {
                // Shift-click armor slot to move to inventory
                // Armor slots in ContainerPlayer: 5 (head) to 8 (boots)
                clickSlot(8 - armorType, 0, 1);
            } else {
                // Shift-click item in inventory to equip
                // Inventory slots start at 9 in the container
                int windowSlot = bestSlot < 9 ? bestSlot + 36 : bestSlot;
                clickSlot(windowSlot, 0, 1);
                return true;
            }
        }
        return false;
    }

    private float getArmorScore(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemArmor)) return -1;
        ItemArmor armor = (ItemArmor) stack.getItem();
        
        float score = (float) armor.damageReduceAmount;
        
        // Add weight for Protection enchantment (standard in 1.8.9)
        int protLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack);
        score += (float) protLevel * 0.25f;
        
        return score;
    }

    private void clickSlot(int slot, int button, int mode) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.playerController.windowClick(
            mc.thePlayer.inventoryContainer.windowId, 
            slot, 
            button, 
            mode, 
            mc.thePlayer
        );
    }
}
