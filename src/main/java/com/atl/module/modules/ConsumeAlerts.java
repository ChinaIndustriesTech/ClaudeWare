package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConsumeAlerts extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<UUID, Long> usingPlayers = new HashMap<>();

    public ConsumeAlerts() {
        super("ConsumeAlerts", "Alerts when a player consumes a Golden Apple or Potion", Category.MISC);
    }

    @Override
    public void onEnable() {
        usingPlayers.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;

            UUID uuid = player.getUniqueID();
            ItemStack heldItem = player.getItemInUse();

            if (player.isUsingItem() && heldItem != null) {
                if (heldItem.getItem() == Items.golden_apple || heldItem.getItem() == Items.potionitem) {
                    if (!usingPlayers.containsKey(uuid)) {
                        usingPlayers.put(uuid, System.currentTimeMillis());
                    }
                    
                    if (player.getItemInUseDuration() >= 31) {
                        String itemName = heldItem.getItem() == Items.golden_apple ? "Golden Apple" : "Potion";
                        sendMessage(EnumChatFormatting.WHITE + player.getName() + EnumChatFormatting.GRAY + " has consumed a " + 
                            (itemName.equals("Golden Apple") ? EnumChatFormatting.GOLD : EnumChatFormatting.LIGHT_PURPLE) + itemName);
                        
                        mc.thePlayer.playSound("random.orb", 1F, 1F);
                        usingPlayers.put(uuid, Long.MAX_VALUE); 
                    }
                }
            } else {
                usingPlayers.remove(uuid);
            }
        }
    }

    private void sendMessage(String message) {
        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.YELLOW + "ConsumeAlerts" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.RESET + message));
    }
}
