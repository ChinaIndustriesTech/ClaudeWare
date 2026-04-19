package com.atl.module.modules;

import com.atl.module.management.Module;
import com.atl.module.management.Category;
import com.atl.module.management.BooleanSetting;
import com.atl.module.management.NumberSetting;
import com.google.gson.JsonObject;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ESP extends Module {
    public BooleanSetting showPlayers = new BooleanSetting("Players", true);
    public BooleanSetting showBeds = new BooleanSetting("Beds", true);

    public NumberSetting playerR = new NumberSetting("Player Red", 1.0, 0.0, 1.0, 0.01);
    public NumberSetting playerG = new NumberSetting("Player Green", 1.0, 0.0, 1.0, 0.01);
    public NumberSetting playerB = new NumberSetting("Player Blue", 1.0, 0.0, 1.0, 0.01);
    public NumberSetting playerA = new NumberSetting("Player Alpha", 1.0, 0.0, 1.0, 0.01);

    public NumberSetting bedR = new NumberSetting("Bed Red", 0.0, 0.0, 1.0, 0.01);
    public NumberSetting bedG = new NumberSetting("Bed Green", 0.5, 0.0, 1.0, 0.01);
    public NumberSetting bedB = new NumberSetting("Bed Blue", 1.0, 0.0, 1.0, 0.01);
    public NumberSetting bedA = new NumberSetting("Bed Alpha", 1.0, 0.0, 1.0, 0.01);

    private final Minecraft mc = Minecraft.getMinecraft();

    private static final int BED_SEARCH_RADIUS = 50;
    private static final long BED_RESCAN_INTERVAL = 5000; // ms

    private final List<BlockPos> cachedBeds = new ArrayList<>();
    private long lastBedScan = 0;

    public ESP() {
        super("ESP", "renders box around guys and beds", Category.RENDER);
        addSettings(showPlayers, showBeds, playerR, playerG, playerB, playerA, bedR, bedG, bedB, bedA);
    }

    @Override
    public void loadSettings(JsonObject settings) {
        if (settings.has("showPlayers")) showPlayers.enabled = settings.get("showPlayers").getAsBoolean();
        if (settings.has("showBeds")) showBeds.enabled = settings.get("showBeds").getAsBoolean();

        if (settings.has("playerR")) playerR.setValue(settings.get("playerR").getAsDouble());
        if (settings.has("playerG")) playerG.setValue(settings.get("playerG").getAsDouble());
        if (settings.has("playerB")) playerB.setValue(settings.get("playerB").getAsDouble());
        if (settings.has("playerA")) playerA.setValue(settings.get("playerA").getAsDouble());

        if (settings.has("bedR")) bedR.setValue(settings.get("bedR").getAsDouble());
        if (settings.has("bedG")) bedG.setValue(settings.get("bedG").getAsDouble());
        if (settings.has("bedB")) bedB.setValue(settings.get("bedB").getAsDouble());
        if (settings.has("bedA")) bedA.setValue(settings.get("bedA").getAsDouble());
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject settings = new JsonObject();
        settings.addProperty("showPlayers", showPlayers.enabled);
        settings.addProperty("showBeds", showBeds.enabled);

        settings.addProperty("playerR", playerR.value);
        settings.addProperty("playerG", playerG.value);
        settings.addProperty("playerB", playerB.value);
        settings.addProperty("playerA", playerA.value);

        settings.addProperty("bedR", bedR.value);
        settings.addProperty("bedG", bedG.value);
        settings.addProperty("bedB", bedB.value);
        settings.addProperty("bedA", bedA.value);
        return settings;
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4) return false;
        String setting = parts[2].toLowerCase();
        try {
            if (setting.equals("players")) {
                showPlayers.enabled = Boolean.parseBoolean(parts[3]);
                return true;
            } else if (setting.equals("beds")) {
                showBeds.enabled = Boolean.parseBoolean(parts[3]);
                return true;
            }
            
            double val = Double.parseDouble(parts[3]);
            switch (setting) {
                case "playerr": playerR.setValue(val); return true;
                case "playerg": playerG.setValue(val); return true;
                case "playerb": playerB.setValue(val); return true;
                case "playera": playerA.setValue(val); return true;
                case "bedr": bedR.setValue(val); return true;
                case "bedg": bedG.setValue(val); return true;
                case "bedb": bedB.setValue(val); return true;
                case "beda": bedA.setValue(val); return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
                "players (true/false)", "beds (true/false)",
                "playerR (0-1)", "playerG (0-1)", "playerB (0-1)", "playerA (0-1)",
                "bedR (0-1)", "bedG (0-1)", "bedB (0-1)", "bedA (0-1)"
        );
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!isEnabled() || !showBeds.enabled) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        long now = System.currentTimeMillis();
        if (now - lastBedScan < BED_RESCAN_INTERVAL) return;
        lastBedScan = now;

        cachedBeds.clear();
        BlockPos playerPos = new BlockPos(mc.thePlayer);

        for (int dx = -BED_SEARCH_RADIUS; dx <= BED_SEARCH_RADIUS; dx++) {
            for (int dy = -BED_SEARCH_RADIUS; dy <= BED_SEARCH_RADIUS; dy++) {
                for (int dz = -BED_SEARCH_RADIUS; dz <= BED_SEARCH_RADIUS; dz++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);
                    IBlockState state = mc.theWorld.getBlockState(pos);
                    if (state.getBlock() instanceof BlockBed) {
                        if (state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD) {
                            cachedBeds.add(pos);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        double px = mc.getRenderManager().viewerPosX;
        double py = mc.getRenderManager().viewerPosY;
        double pz = mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GL11.glLineWidth(1.0f);

        // --- Player Hitbox ESP ---
        if (showPlayers.enabled) {
            for (EntityPlayer entity : mc.theWorld.playerEntities) {
                if (entity == mc.thePlayer) continue;
                if (AntiBot.isBot(entity)) continue;

                double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * event.partialTicks - px;
                double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * event.partialTicks - py;
                double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * event.partialTicks - pz;

                // Interpolate the actual bounding box for precision (handles sneaking)
                AxisAlignedBB entBB = entity.getEntityBoundingBox();
                AxisAlignedBB bb = new AxisAlignedBB(
                        entBB.minX - entity.posX + x,
                        entBB.minY - entity.posY + y,
                        entBB.minZ - entity.posZ + z,
                        entBB.maxX - entity.posX + x,
                        entBB.maxY - entity.posY + y,
                        entBB.maxZ - entity.posZ + z
                );

                drawOutline(bb, (float)playerR.value, (float)playerG.value, (float)playerB.value, (float)playerA.value);
            }
        }

        // --- Bed ESP ---
        if (showBeds.enabled) {
            for (BlockPos pos : cachedBeds) {
                IBlockState state = mc.theWorld.getBlockState(pos);
                if (!(state.getBlock() instanceof BlockBed) || state.getValue(BlockBed.PART) != BlockBed.EnumPartType.HEAD) continue;

                AxisAlignedBB bb = new AxisAlignedBB(
                        pos.getX()       - px, pos.getY()       - py, pos.getZ()       - pz,
                        pos.getX() + 1.0 - px, pos.getY() + 0.5 - py, pos.getZ() + 1.0 - pz
                );

                drawOutline(bb, (float)bedR.value, (float)bedG.value, (float)bedB.value, (float)bedA.value);
            }
        }

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private void drawOutline(AxisAlignedBB bb, float r, float g, float b, float a) {
        GlStateManager.color(r, g, b, a);
        RenderGlobal.drawOutlinedBoundingBox(bb,
                (int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(a * 255));
    }

    @Override
    public void onDisable() {
        cachedBeds.clear();
    }
}
