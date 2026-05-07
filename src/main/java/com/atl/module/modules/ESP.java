package com.atl.module.modules;

import com.atl.module.management.Module;
import com.atl.module.management.Category;
import com.atl.module.management.BooleanSetting;
import com.atl.module.management.NumberSetting;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
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
    public BooleanSetting showChests = new BooleanSetting("Chests", true);

    public NumberSetting playerR = new NumberSetting("Player Red", 1.0, 0.0, 1.0, 0.01);
    public NumberSetting playerG = new NumberSetting("Player Green", 1.0, 0.0, 1.0, 0.01);
    public NumberSetting playerB = new NumberSetting("Player Blue", 1.0, 0.0, 1.0, 0.01);

    public NumberSetting bedR = new NumberSetting("Bed Red", 0.0, 0.0, 1.0, 0.01);
    public NumberSetting bedG = new NumberSetting("Bed Green", 0.5, 0.0, 1.0, 0.01);
    public NumberSetting bedB = new NumberSetting("Bed Blue", 1.0, 0.0, 1.0, 0.01);

    public NumberSetting chestR = new NumberSetting("Chest Red", 1.0, 0.0, 1.0, 0.01);
    public NumberSetting chestG = new NumberSetting("Chest Green", 0.6, 0.0, 1.0, 0.01);
    public NumberSetting chestB = new NumberSetting("Chest Blue", 0.0, 0.0, 1.0, 0.01);

    public NumberSetting enderChestR = new NumberSetting("Ender Red", 0.5, 0.0, 1.0, 0.01);
    public NumberSetting enderChestG = new NumberSetting("Ender Green", 0.0, 0.0, 1.0, 0.01);
    public NumberSetting enderChestB = new NumberSetting("Ender Blue", 0.5, 0.0, 1.0, 0.01);

    private final Minecraft mc = Minecraft.getMinecraft();

    private static final int SEARCH_RADIUS = 30;
    private static final long SCAN_INTERVAL = 2000;

    private final List<BlockPos> cachedBeds = new ArrayList<>();
    private final List<BlockPos> cachedChests = new ArrayList<>();
    private final List<BlockPos> cachedEnderChests = new ArrayList<>();
    private long lastScan = 0;

    public ESP() {
        super("ESP", "renders box around guys and blocks", Category.RENDER);
        addSettings(showPlayers, showBeds, showChests, 
                playerR, playerG, playerB, 
                bedR, bedG, bedB, 
                chestR, chestG, chestB,
                enderChestR, enderChestG, enderChestB);
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
                "players (true/false)", "beds (true/false)", "chests (true/false)"
        );
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!isEnabled()) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        long now = System.currentTimeMillis();
        if (now - lastScan < SCAN_INTERVAL) return;
        lastScan = now;

        cachedBeds.clear();
        cachedChests.clear();
        cachedEnderChests.clear();

        BlockPos playerPos = new BlockPos(mc.thePlayer);

        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);
                    IBlockState state = mc.theWorld.getBlockState(pos);
                    if (showBeds.enabled && state.getBlock() instanceof BlockBed) {
                        cachedBeds.add(pos);
                    } else if (showChests.enabled) {
                        if (state.getBlock() instanceof BlockChest) {
                            cachedChests.add(pos);
                        } else if (state.getBlock() instanceof BlockEnderChest) {
                            cachedEnderChests.add(pos);
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
        GL11.glLineWidth(1.5f);

        if (showPlayers.enabled) {
            for (EntityPlayer entity : mc.theWorld.playerEntities) {
                if (entity == mc.thePlayer) continue;
                if (AntiBot.isBot(entity)) continue;

                double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * event.partialTicks - px;
                double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * event.partialTicks - py;
                double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * event.partialTicks - pz;

                AxisAlignedBB entBB = entity.getEntityBoundingBox();
                AxisAlignedBB bb = new AxisAlignedBB(
                        entBB.minX - entity.posX + x,
                        entBB.minY - entity.posY + y,
                        entBB.minZ - entity.posZ + z,
                        entBB.maxX - entity.posX + x,
                        entBB.maxY - entity.posY + y,
                        entBB.maxZ - entity.posZ + z
                );

                drawOutline(bb, (float)playerR.value, (float)playerG.value, (float)playerB.value);
            }
        }

        if (showBeds.enabled) {
            for (BlockPos pos : cachedBeds) {
                AxisAlignedBB bb = new AxisAlignedBB(
                        pos.getX()       - px, pos.getY()       - py, pos.getZ()       - pz,
                        pos.getX() + 1.0 - px, pos.getY() + 0.5 - py, pos.getZ() + 1.0 - pz
                );
                drawOutline(bb, (float)bedR.value, (float)bedG.value, (float)bedB.value);
            }
        }

        if (showChests.enabled) {
            for (BlockPos pos : cachedChests) {
                AxisAlignedBB bb = new AxisAlignedBB(
                        pos.getX() + 0.0625 - px, pos.getY() - py, pos.getZ() + 0.0625 - pz,
                        pos.getX() + 0.9375 - px, pos.getY() + 0.875 - py, pos.getZ() + 0.9375 - pz
                );
                drawOutline(bb, (float)chestR.value, (float)chestG.value, (float)chestB.value);
            }
            for (BlockPos pos : cachedEnderChests) {
                AxisAlignedBB bb = new AxisAlignedBB(
                        pos.getX() + 0.0625 - px, pos.getY() - py, pos.getZ() + 0.0625 - pz,
                        pos.getX() + 0.9375 - px, pos.getY() + 0.875 - py, pos.getZ() + 0.9375 - pz
                );
                drawOutline(bb, (float)enderChestR.value, (float)enderChestG.value, (float)enderChestB.value);
            }
        }

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private void drawOutline(AxisAlignedBB bb, float r, float g, float b) {
        float alpha = 1.0f;
        GlStateManager.color(r, g, b, alpha);
        RenderGlobal.drawOutlinedBoundingBox(bb,
                (int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(alpha * 255));
    }

    @Override
    public void onDisable() {
        cachedBeds.clear();
        cachedChests.clear();
        cachedEnderChests.clear();
    }
}
