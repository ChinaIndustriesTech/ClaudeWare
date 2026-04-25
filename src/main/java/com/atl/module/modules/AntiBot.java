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


    public static boolean isBot(EntityPlayer player) {
        if (instance == null || !instance.isEnabled()) return false;
        if (player.deathTime != 0) return true;
        if (tabCheck || pingCheck) {
            if (Minecraft.getMinecraft().getNetHandler() == null) return false;
            NetworkPlayerInfo info = Minecraft.getMinecraft().getNetHandler().getPlayerInfo(player.getUniqueID());
            if (tabCheck && info == null) return true;
            if (pingCheck && info != null && info.getResponseTime() <= 0) return true;
        }

        if (idCheck) {
            if (player.getEntityId() < 0 ) return true;
        }

        final String name = player.getName();

        if (name.isEmpty() || name.contains(" ") || name.contains("[NPC] ") || name.contains("§")) return true;

        final String valid = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890_";
        for (int i = 0; i < name.length(); i++) {
            if (!valid.contains(String.valueOf(name.charAt(i)))) {
                return true;
            }
        }
        return false;
    }
}
