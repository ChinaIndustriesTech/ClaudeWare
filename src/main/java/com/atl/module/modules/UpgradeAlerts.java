package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UpgradeAlerts extends Module {

    private final Map<UUID, Integer> alertedStates = new HashMap<>();

    public UpgradeAlerts() {
        super("UpgradeAlerts", "Alerts you when players have upgraded gear", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        // Cache is only reset on world change
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        alertedStates.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null || !isEnabled()) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isDead || AntiBot.isBot(player) || Teams.isTeammate(player)) continue;

            boolean hasSharpness = false;
            ItemStack heldItem = player.getHeldItem();
            if (heldItem != null && heldItem.getItem() instanceof ItemSword) {
                if (EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, heldItem) > 0) {
                    hasSharpness = true;
                }
            }

            boolean hasProtection = false;
            for (int i = 0; i < 4; i++) {
                ItemStack armor = player.inventory.armorItemInSlot(i);
                if (armor != null && EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, armor) > 0) {
                    hasProtection = true;
                    break;
                }
            }

            int currentState = (hasSharpness ? 1 : 0) | (hasProtection ? 2 : 0);
            UUID uuid = player.getUniqueID();

            if (currentState != 0 && alertedStates.getOrDefault(uuid, 0) != currentState) {
                StringBuilder message = new StringBuilder(player.getName()).append(" has ");

                if (hasSharpness && hasProtection) {
                    message.append("sharpness and protection");
                } else if (hasSharpness) {
                    message.append("sharpness");
                } else {
                    message.append("protection");
                }

                mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "!" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.YELLOW + message.toString()));
                mc.thePlayer.playSound("random.orb", 1F, 1F);
                alertedStates.put(uuid, currentState);
            } else if (currentState == 0) {
                alertedStates.remove(uuid);
            }
        }

        if (mc.thePlayer.ticksExisted % 100 == 0) {
            alertedStates.keySet().removeIf(uuid -> mc.theWorld.getPlayerEntityByUUID(uuid) == null);
        }
    }
}
