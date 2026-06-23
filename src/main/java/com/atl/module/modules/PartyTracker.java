package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;
import java.util.ArrayList;

public class PartyTracker extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Set<UUID> knownPlayers = new HashSet<>();
    private final List<JoinInfo> recentJoins = new ArrayList();
    private long lastWorldLoadTime = 0;

    private static class JoinInfo {
        final String name;
        final long time;
        boolean alerted;

        JoinInfo(String name, long time) {
            this.name = name;
            this.time = time;
            this.alerted = false;
        }
    }

    public PartyTracker() {
        super("PartyTracker", "Detects potential parties joining within 50ms", Category.ALERTS);
    }

    @Override
    public void onEnable() {
        knownPlayers.clear();
        recentJoins.clear();
        lastWorldLoadTime = System.currentTimeMillis();
        // Initialize known players with current players in the world
        if (mc.theWorld != null) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer) continue;
                if (AntiBot.isBot(player)) continue;
                knownPlayers.add(player.getUniqueID());
            }
        }
    }

    @Override
    public void onDisable() {
        knownPlayers.clear();
        recentJoins.clear();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        knownPlayers.clear();
        recentJoins.clear();
        lastWorldLoadTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        knownPlayers.clear();
        recentJoins.clear();
        lastWorldLoadTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Detect new players
        List<EntityPlayer> newPlayersThisTick = new java.util.ArrayList();
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;
            if (AntiBot.isBot(player)) continue;

            UUID uuid = player.getUniqueID();
            if (!knownPlayers.contains(uuid)) {
                knownPlayers.add(uuid);
                newPlayersThisTick.add(player);
            }
        }

        // 2. Add them to recentJoins if not in initial world load phase (first 500ms)
        if (now - lastWorldLoadTime > 500) {
            for (EntityPlayer player : newPlayersThisTick) {
                recentJoins.add(new JoinInfo(player.getName(), now));
            }
        }

        // 3. Clean up old joins (older than 1000ms)
        recentJoins.removeIf(join -> now - join.time > 1000);

        // 4. Cluster recent joins that occurred within 50ms of each other
        List<List<JoinInfo>> clusters = new java.util.ArrayList();
        for (JoinInfo join : recentJoins) {
            boolean added = false;
            for (List<JoinInfo> cluster : clusters) {
                if (join.time - cluster.get(cluster.size() - 1).time <= 50) {
                    cluster.add(join);
                    added = true;
                    break;
                }
            }
            if (!added) {
                List<JoinInfo> newCluster = new java.util.ArrayList();
                newCluster.add(join);
                clusters.add(newCluster);
            }
        }

        // 5. Evaluate clusters for potential parties
        for (List<JoinInfo> cluster : clusters) {
            int size = cluster.size();
            if (size >= 2 && size <= 4) {
                int unalertedCount = 0;
                for (JoinInfo join : cluster) {
                    if (!join.alerted) {
                        unalertedCount++;
                    }
                }

                if (unalertedCount >= 2) {
                    sendMessage("potential party.");
                    for (JoinInfo join : cluster) {
                        join.alerted = true;
                    }
                }
            }
        }
    }

    private void sendMessage(String message) {
        mc.thePlayer.addChatMessage(new ChatComponentText(
            EnumChatFormatting.GRAY + "[" + 
            EnumChatFormatting.LIGHT_PURPLE + "PartyTracker" + 
            EnumChatFormatting.GRAY + "] " + 
            EnumChatFormatting.RESET + message
        ));
    }
}
