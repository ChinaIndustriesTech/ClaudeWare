package com.atl.module.modules;

import com.atl.module.management.Module;
import com.atl.module.management.Category;
import com.atl.module.management.BooleanSetting;
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


    private final Minecraft mc = Minecraft.getMinecraft();

    private float playerR = 1.0f, playerG = 1.0f, playerB = 1.0f, playerA = 1.0f;
    private float bedR    = 0.0f, bedG    = 0.5f, bedB    = 1.0f, bedA    = 1.0f;

    private static final int BED_SEARCH_RADIUS = 50;
    private static final long BED_RESCAN_INTERVAL = 5000; // ms

    private final List<BlockPos> cachedBeds = new ArrayList<>();
    private long lastBedScan = 0;

    public ESP() {
        super("ESP", "renders box around guys and beds", Category.RENDER);
        addSettings(showPlayers, showBeds);
    }

    @Override
    public void loadSettings(JsonObject settings) {
        if (settings.has("showPlayers")) showPlayers.enabled = settings.get("showPlayers").getAsBoolean();
        if (settings.has("showBeds")) showBeds.enabled = settings.get("showBeds").getAsBoolean();
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject settings = new JsonObject();
        settings.addProperty("showPlayers", showPlayers.enabled);
        settings.addProperty("showBeds", showBeds.enabled);
        return settings;
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 3) return false;
        String setting = parts[2].toLowerCase();
        if (setting.equals("players")) {
            showPlayers.enabled = Boolean.parseBoolean(parts[3]);
            return true;
        } else if (setting.equals("beds")) {
            showBeds.enabled = Boolean.parseBoolean(parts[3]);
            return true;
        }
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
                "players (true/false) - Current: " + showPlayers.enabled,
                "beds (true/false) - Current: " + showBeds.enabled
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

                drawOutline(bb, playerR, playerG, playerB, playerA);
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

                drawOutline(bb, bedR, bedG, bedB, bedA);
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
