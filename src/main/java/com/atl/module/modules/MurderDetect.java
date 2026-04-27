package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Set;

public class MurderDetect extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Set<String> detectedMurderers = new HashSet<>();
    private final Set<Item> murderWeapons = new HashSet<>();

    public MurderDetect() {
        super("MurderDetect", "murder mystery hacke.", Category.MISC);
        setupWeapons();
    }

    private void setupWeapons() {
        murderWeapons.add(Items.iron_sword);
        murderWeapons.add(Items.diamond_hoe);
        murderWeapons.add(Items.bread);
        murderWeapons.add(Items.boat);
        murderWeapons.add(Items.stick);
        murderWeapons.add(Items.stone_sword);
        murderWeapons.add(Items.wooden_sword);
        murderWeapons.add(Items.name_tag);
        murderWeapons.add(Items.quartz);
        murderWeapons.add(Items.coal);
        murderWeapons.add(Items.flint);
        murderWeapons.add(Items.bone);
        murderWeapons.add(Items.leather);
        murderWeapons.add(Items.golden_pickaxe);
        murderWeapons.add(Items.pumpkin_pie);
        murderWeapons.add(Items.diamond_shovel);
        murderWeapons.add(Items.blaze_rod);
        murderWeapons.add(Items.stone_shovel);
        murderWeapons.add(Items.reeds);
        murderWeapons.add(Items.wooden_axe);
        murderWeapons.add(Items.carrot);
        murderWeapons.add(Items.golden_carrot);
        murderWeapons.add(Items.cookie);
        murderWeapons.add(Items.diamond_axe);
        murderWeapons.add(Items.prismarine_shard);
        murderWeapons.add(Items.cooked_beef);
        murderWeapons.add(Item.getItemFromBlock(Blocks.nether_brick));
        murderWeapons.add(Items.nether_wart);
        murderWeapons.add(Items.cooked_chicken);
        murderWeapons.add(Items.record_chirp);
        murderWeapons.add(Items.golden_hoe);
        murderWeapons.add(Items.golden_sword);
        murderWeapons.add(Items.diamond_sword);
        murderWeapons.add(Items.shears);
        murderWeapons.add(Items.speckled_melon);
        murderWeapons.add(Items.book);
        murderWeapons.add(Items.golden_axe);
        murderWeapons.add(Items.diamond_pickaxe);
        murderWeapons.add(Items.golden_shovel);
        murderWeapons.add(Item.getItemFromBlock(Blocks.sapling));
        murderWeapons.add(Item.getItemFromBlock(Blocks.double_plant));
        murderWeapons.add(Item.getItemFromBlock(Blocks.deadbush));
        murderWeapons.add(Items.dye);
        murderWeapons.add(Items.fish);
    }

    @Override
    public void onEnable() {
        detectedMurderers.clear();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            
            String name = player.getName();
            if (detectedMurderers.contains(name)) continue;

            ItemStack heldItem = player.getHeldItem();
            if (heldItem != null) {
                Item item = heldItem.getItem();
                if (murderWeapons.contains(item)) {
                    detectedMurderers.add(name);
                    String itemName = heldItem.getDisplayName();
                    sendMessage(EnumChatFormatting.RED + name + EnumChatFormatting.WHITE + " is the Murderer! (Holding " + itemName + ")");
                    mc.thePlayer.playSound("random.orb", 1F, 1F);
                }
            }
        }
    }

    private void sendMessage(String message) {
        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.DARK_RED + "MurderDetect" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.RESET + message));
    }
}
