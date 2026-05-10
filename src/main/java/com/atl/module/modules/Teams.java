package com.atl.module.modules;

import com.atl.module.management.BooleanSetting;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class Teams extends Module {

    private static Teams instance;
    private final Set<String> externalTeammates = new HashSet<>();

    public BooleanSetting armorMode = new BooleanSetting("Armor", true);
    public BooleanSetting colorMode = new BooleanSetting("Color", true);
    public BooleanSetting externalMode = new BooleanSetting("External", true); // New setting for external teammates

    public Teams() {
        super("Teams", "Prevents targeting or rendering teammates", Category.MISC);
        instance = this;
        addSettings(armorMode, colorMode, externalMode);
    }

    public static void addTeammate(String playerName) {
        if (instance != null && instance.externalMode.isEnabled()) {
            instance.externalTeammates.add(playerName);
        }
    }

    public static void removeTeammate(String playerName) {
        if (instance != null) {
            instance.externalTeammates.remove(playerName);
        }
    }

    public static void clearExternalTeammates() {
        if (instance != null) {
            instance.externalTeammates.clear();
        }
    }

    @Override
    public void onDisable() {
        clearExternalTeammates(); // Clear external teammates when Teams module is disabled
        super.onDisable();
    }

    public static boolean isTeammate(EntityPlayer target) {
        if (instance == null || !instance.isEnabled() || target == null) return false;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;

        // Check external teammates first
        if (instance.externalMode.isEnabled() && instance.externalTeammates.contains(target.getName())) {
            return true;
        }

        if (instance.armorMode.isEnabled()) {
            ItemStack myChest = mc.thePlayer.inventory.armorItemInSlot(2);
            ItemStack targetChest = target.inventory.armorItemInSlot(2);

            if (myChest != null && targetChest != null) {
                if (targetChest.getIsItemStackEqual(myChest)) {
                    return true;
                }
            }
        }

        if (instance.colorMode.isEnabled()) {
            if (isSameColor(mc.thePlayer, target)) {
                return true;
            }
        }

        return false;
    }


    private static boolean isSameColor(EntityPlayer p1, EntityPlayer p2) {
        String name1 = p1.getDisplayName().getFormattedText();
        String name2 = p2.getDisplayName().getFormattedText();

        if (name1.contains("§") && name2.contains("§")) {
            try {
                int index1 = name1.indexOf("§");
                int index2 = name2.indexOf("§");

                char color1 = name1.charAt(index1 + 1);
                char color2 = name2.charAt(index2 + 1);

                if (color1 != 'r' && color1 == color2) {
                    return true;
                }
            } catch (IndexOutOfBoundsException ignored) {}
        }

        return false;
    }
}