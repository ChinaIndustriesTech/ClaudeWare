package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.BooleanSetting;
import com.atl.module.management.NumberSetting;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class BedTracker extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    
    private BlockPos bedPos = null;
    private boolean needsScanning = false;
    private int scanTicks = 0;
    
    public BooleanSetting autoEnable = new BooleanSetting("AutoEnable", false);
    public NumberSetting xPos = new NumberSetting("X Position", 2, 0, 1000, 1);
    public NumberSetting yPos = new NumberSetting("Y Position", 20, 0, 1000, 1);

    private boolean manualDisabled = false; 
    private String lastMapString = "";

    private final HashMap<String, Long> alertCooldowns = new HashMap<>();
    private final HashSet<Integer> trackedPearls = new HashSet<>();
    private final Set<String> whitelistedPlayers = new HashSet<>(); 
    private boolean playersWhitelisted = false; 

    public BedTracker() {
        super("BedTracker", "Alerts when enemies are near your bed", Category.ALERTS);
        addSettings(autoEnable, xPos, yPos);
    }

    @Override
    public void onEnable() {
        needsScanning = true;
        scanTicks = 0;
        bedPos = null;
        trackedPearls.clear();
        alertCooldowns.clear();
        whitelistedPlayers.clear(); 
        playersWhitelisted = false; 
        manualDisabled = false;
    }

    @Override
    public void onDisable() {
        manualDisabled = true;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        if (settings.has("autoEnable")) {
            this.autoEnable.enabled = settings.get("autoEnable").getAsBoolean();
        }
        if (settings.has("xPos")) {
            this.xPos.setValue(settings.get("xPos").getAsDouble());
        }
        if (settings.has("yPos")) {
            this.yPos.setValue(settings.get("yPos").getAsDouble());
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject settings = new JsonObject();
        settings.addProperty("autoEnable", autoEnable.isEnabled());
        settings.addProperty("xPos", xPos.value);
        settings.addProperty("yPos", yPos.value);
        return settings;
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4) return false;
        String setting = parts[2].toLowerCase();
        
        if (setting.equalsIgnoreCase("autoenable")) {
            this.autoEnable.enabled = Boolean.parseBoolean(parts[3]);
            return true;
        } else if (setting.equalsIgnoreCase("xpos")) {
            try {
                this.xPos.setValue(Double.parseDouble(parts[3]));
                return true;
            } catch (NumberFormatException e) { return false; }
        } else if (setting.equalsIgnoreCase("ypos")) {
            try {
                this.yPos.setValue(Double.parseDouble(parts[3]));
                return true;
            } catch (NumberFormatException e) { return false; }
        }
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
            "autoEnable: " + autoEnable.isEnabled(),
            "XPos: " + xPos.value,
            "YPos: " + yPos.value
        );
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Unload event) {
        if (isEnabled()) {
            setEnabled(false);
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!isEnabled()) return;

        String message = event.message.getUnformattedText();
        String formatted = event.message.getFormattedText();

        if (message.contains("You have been eliminated!") && formatted.contains("§c")) {
            setEnabled(false);
            sendMessage(EnumChatFormatting.RED + "Auto-Disabled (Eliminated)!");
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null) return;

        if (autoEnable.isEnabled() && !isEnabled()) {
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            if (scoreboard != null) {
                ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1); 
                if (objective != null) {
                    Collection<Score> scores = scoreboard.getSortedScores(objective);
                    boolean mapFound = false;
                    String currentMap = "";

                    for (Score score : scores) {
                        ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
                        String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
                        String cleanLine = EnumChatFormatting.getTextWithoutFormattingCodes(line).toLowerCase();
                        if (cleanLine.contains("map:")) {
                            mapFound = true;
                            currentMap = cleanLine;
                            break;
                        }
                    }

                    if (!currentMap.equals(lastMapString)) {
                        manualDisabled = false;
                        lastMapString = currentMap;
                    }

                    if (mapFound && !manualDisabled) {
                        setEnabled(true);
                        sendMessage(EnumChatFormatting.GREEN + "Auto-Enabled (Map detected)!");
                    }
                }
            }
        }

        if (!isEnabled()) return;

        if (needsScanning && bedPos == null) {
            scanTicks++;
            if (scanTicks % 40 == 0) {
                findNearestBed();
            }
        }

        if (bedPos != null) {
            if (mc.theWorld.getBlockState(bedPos).getBlock() != Blocks.bed) {
                setEnabled(false);
                sendMessage(EnumChatFormatting.RED + "Bed destroyed! Auto-Disabled.");
                return;
            }

            if (!playersWhitelisted) {
                whitelistNearbyPlayers();
                playersWhitelisted = true;
            }

            long now = System.currentTimeMillis();

            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityEnderPearl && !trackedPearls.contains(entity.getEntityId())) {
                    trackedPearls.add(entity.getEntityId());
                    sendMessage(EnumChatFormatting.LIGHT_PURPLE + "Pearl Detected!");
                    mc.thePlayer.playSound("random.orb", 1F, 1F);
                }
            }

            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer || player.isInvisible()) continue;
                
                if (whitelistedPlayers.contains(player.getName())) {
                    continue;
                }

                double dist = player.getDistance(bedPos.getX(), bedPos.getY(), bedPos.getZ());
                if (dist < 30) { 
                    if (!alertCooldowns.containsKey(player.getName()) || now - alertCooldowns.get(player.getName()) > 5000) {
                        sendMessage(EnumChatFormatting.RED + player.getName() + EnumChatFormatting.WHITE + " is near your bed!");
                        alertCooldowns.put(player.getName(), now);
                        mc.thePlayer.playSound("note.pling", 1F, 1F);
                    }
                }
            }
        }
    }

    private boolean isInFOV(BlockPos pos) {
        // Calculate the vector from the player's eyes to the center of the block
        Vec3 toBlock = new Vec3(pos.getX() + 0.5 - mc.thePlayer.posX,
                                pos.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()),
                                pos.getZ() + 0.5 - mc.thePlayer.posZ).normalize();
        Vec3 lookVec = mc.thePlayer.getLookVec();
        // Dot product > 0.5 corresponds to roughly a 60-degree cone
        return lookVec.dotProduct(toBlock) > 0.5;
    }

    private void findNearestBed() {
        int horizontalRadius = 20; // Keep horizontal search wide
        int verticalSearchRange = 5; // Check 4 Y levels above and below current Y
        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        
        int startY = Math.max(0, playerPos.getY() - verticalSearchRange);
        int endY = Math.min(255, playerPos.getY() + verticalSearchRange);

        BlockPos backupBed = null;

        for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
            for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                for (int y = startY; y <= endY; y++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    if (mc.theWorld.getBlockState(checkPos).getBlock() == Blocks.bed) {
                        // If the bed is in our field of view, prioritize it and stop searching
                        if (isInFOV(checkPos)) {
                            this.bedPos = checkPos;
                            this.needsScanning = false;
                            sendMessage(EnumChatFormatting.GREEN + "Bed located in FOV!");
                            return;
                        }
                        // Otherwise, keep track of the first bed found as a fallback
                        if (backupBed == null) {
                            backupBed = checkPos;
                        }
                    }
                }
            }
        }

        if (backupBed != null) {
            this.bedPos = backupBed;
            this.needsScanning = false;
            sendMessage(EnumChatFormatting.GREEN + "Bed located (outside FOV)!");
        }
    }

    private void whitelistNearbyPlayers() {
        if (bedPos == null) return;

        int whitelistRadius = 30;
        int count = 0;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer) continue;

            double dist = player.getDistance(bedPos.getX(), bedPos.getY(), bedPos.getZ());
            if (dist <= whitelistRadius) {
                whitelistedPlayers.add(player.getName());
                count++;
            }
        }
        if (count > 0) {
            sendMessage(EnumChatFormatting.GRAY + "Whitelisted " + count + " nearby players.");
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!isEnabled() || event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        
        String display = "Bed: " + (bedPos != null ? EnumChatFormatting.GREEN + "TRACKING" : EnumChatFormatting.RED + "NOT FOUND");
        mc.fontRendererObj.drawStringWithShadow(display, (float)xPos.value, (float)yPos.value, -1);
    }

    private void sendMessage(String message) {
        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.BLUE + "BedTracker" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.RESET + message));
    }
}
