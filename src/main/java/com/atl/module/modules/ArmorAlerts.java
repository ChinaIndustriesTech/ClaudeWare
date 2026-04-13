package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArmorAlerts extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<UUID, String> playerArmorMap = new HashMap<>();

    public ArmorAlerts() {
        super("ArmorAlerts", "Alerts when a player equips Iron or Diamond armor", Category.MISC);
    }

    @Override
    public void onEnable() {
        // Cache is only reset on world change, not on toggle
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        playerArmorMap.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;

            String currentTier = getArmorTier(player);
            UUID uuid = player.getUniqueID();

            if (playerArmorMap.containsKey(uuid)) {
                String lastTier = playerArmorMap.get(uuid);
                if (!currentTier.equals(lastTier)) {
                    if (currentTier.equals("Iron") || currentTier.equals("Diamond")) {
                        if (lastTier.equals("None") || (lastTier.equals("Iron") && currentTier.equals("Diamond"))) {
                            sendMessage(EnumChatFormatting.WHITE + player.getName() + EnumChatFormatting.GRAY + " has bought " + 
                                (currentTier.equals("Iron") ? EnumChatFormatting.WHITE : EnumChatFormatting.AQUA) + currentTier + " Armor");
                            mc.thePlayer.playSound("random.orb", 1F, 1F);
                        }
                    }
                }
            }
            
            playerArmorMap.put(uuid, currentTier);
        }
    }

    private String getArmorTier(EntityPlayer player) {
        boolean hasDiamond = false;
        boolean hasIron = false;

        for (int i = 0; i < 4; i++) {
            ItemStack stack = player.inventory.armorInventory[i];
            if (stack != null) {
                Item item = stack.getItem();
                if (item == Items.diamond_boots || item == Items.diamond_helmet || item == Items.diamond_chestplate || item == Items.diamond_leggings) {
                    hasDiamond = true;
                } else if (item == Items.iron_boots || item == Items.iron_helmet || item == Items.iron_chestplate || item == Items.iron_leggings) {
                    hasIron = true;
                }
            }
        }

        if (hasDiamond) return "Diamond";
        if (hasIron) return "Iron";
        return "None";
    }

    private void sendMessage(String message) {
        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.BLUE + "ArmorAlerts" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.RESET + message));
    }
}
