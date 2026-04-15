package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Arrays;
import java.util.List;

public class AntiBot extends Module {

    private static AntiBot instance;
    
    private static boolean tabCheck = true;
    private static boolean idCheck = true;
    private static boolean pingCheck = true;

    public AntiBot() {
        super("AntiBot", "", Category.MISC);
        instance = this;
    }

    public static AntiBot getInstance() {
        return instance;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        if (settings.has("tabCheck")) tabCheck = settings.get("tabCheck").getAsBoolean();
        if (settings.has("idCheck")) idCheck = settings.get("idCheck").getAsBoolean();
        if (settings.has("pingCheck")) pingCheck = settings.get("pingCheck").getAsBoolean();
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject settings = new JsonObject();
        settings.addProperty("tabCheck", tabCheck);
        settings.addProperty("idCheck", idCheck);
        settings.addProperty("pingCheck", pingCheck);
        return settings;
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 3) return false;
        String setting = parts[2].toLowerCase();
        if (setting.equals("tabcheck")) {
            tabCheck = Boolean.parseBoolean(parts[3]);
            return true;
        } else if (setting.equals("idcheck")) {
            idCheck = Boolean.parseBoolean(parts[3]);
            return true;
        } else if (setting.equals("pingcheck")) {
            pingCheck = Boolean.parseBoolean(parts[3]);
            return true;
        }
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
                "tabCheck (true/false) - Current: " + tabCheck,
                "idCheck (true/false) - Current: " + idCheck,
                "pingCheck (true/false) - Current: " + pingCheck
        );
    }

    /**
     * Static helper to check if a player is a bot.
     * Can be used by ESP, Chams, etc.
     */
    public static boolean isBot(EntityPlayer player) {
        // If AntiBot is disabled, nothing is considered a bot
        if (instance == null || !instance.isEnabled()) return false;

        // 0. Death time check - don't target players currently in death animation
        if (player.deathTime != 0) return true;

        // 1. Network/Tab Checks
        if (tabCheck || pingCheck) {
            if (Minecraft.getMinecraft().getNetHandler() == null) return false;
            NetworkPlayerInfo info = Minecraft.getMinecraft().getNetHandler().getPlayerInfo(player.getUniqueID());
            
            if (tabCheck && info == null) return true;
            // 0 Ping check (Common bot indicator)
            if (pingCheck && info != null && info.getResponseTime() <= 0) return true;
        }

        // 2. low ID Check (Bots often have IDs < 0)
        if (idCheck) {
            if (player.getEntityId() < 0 ) return true;
        }

        // 3. Name and Character validation
        final String name = player.getName();
        
        // Check for invalid strings (NPC tags or formatting symbols)
        if (name.isEmpty() || name.contains(" ") || name.contains("[NPC] ") || name.contains("§")) return true;

        // Filter for characters not usable in a legitimate Minecraft IGN
        final String valid = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890_";
        for (int i = 0; i < name.length(); i++) {
            if (!valid.contains(String.valueOf(name.charAt(i)))) {
                return true;
            }
        }

        // Note: Removing automatic invisible check as it might flag players with actual potions

        return false;
    }
}
