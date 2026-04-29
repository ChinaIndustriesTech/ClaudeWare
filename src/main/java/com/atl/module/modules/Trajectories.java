package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class Trajectories extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public Trajectories() {
        super("Trajectories", "shows proj path", Category.RENDER);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null) return;

        Item item = stack.getItem();
        if (!(item instanceof ItemBow || item instanceof ItemEnderPearl)) return;

        double playerX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * event.partialTicks;
        double playerY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * event.partialTicks;
        double playerZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * event.partialTicks;

        double motionX = -Math.sin(mc.thePlayer.rotationYaw / 180.0F * (float) Math.PI) * Math.cos(mc.thePlayer.rotationPitch / 180.0F * (float) Math.PI);
        double motionY = -Math.sin(mc.thePlayer.rotationPitch / 180.0F * (float) Math.PI);
        double motionZ = Math.cos(mc.thePlayer.rotationYaw / 180.0F * (float) Math.PI) * Math.cos(mc.thePlayer.rotationPitch / 180.0F * (float) Math.PI);

        double power = 1.5; 
        float gravity = 0.03f;
        
        if (item instanceof ItemBow) {
            int useCount = mc.thePlayer.getItemInUseDuration();
            float f = (float) useCount / 20.0F;
            f = (f * f + f * 2.0F) / 3.0F;
            if (f < 0.1D) return;
            if (f > 1.0F) f = 1.0F;
            power = f * 3.0f;
            gravity = 0.05f;
        }

        double posX = playerX - (Math.cos(mc.thePlayer.rotationYaw / 180.0F * (float) Math.PI) * 0.16F);
        double posY = playerY + mc.thePlayer.getEyeHeight() - 0.10000000149011612D;
        double posZ = playerZ - (Math.sin(mc.thePlayer.rotationYaw / 180.0F * (float) Math.PI) * 0.16F);

        double distance = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= distance;
        motionY /= distance;
        motionZ /= distance;
        motionX *= power;
        motionY *= power;
        motionZ *= power;

        Entity hitEntity = null;
        MovingObjectPosition landing = null;

        List<Vec3> pathPoints = new ArrayList<>();
        pathPoints.add(new Vec3(posX, posY, posZ));

        for (int i = 0; i < 100; i++) {
            Vec3 posVec = new Vec3(posX, posY, posZ);
            Vec3 nextPosVec = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);

            landing = mc.theWorld.rayTraceBlocks(posVec, nextPosVec, false, true, false);

            double currentDist = (landing != null) ? posVec.distanceTo(landing.hitVec) : posVec.distanceTo(nextPosVec);

            AxisAlignedBB checkBB = new AxisAlignedBB(
                    Math.min(posX, nextPosVec.xCoord), Math.min(posY, nextPosVec.yCoord), Math.min(posZ, nextPosVec.zCoord),
                    Math.max(posX, nextPosVec.xCoord), Math.max(posY, nextPosVec.yCoord), Math.max(posZ, nextPosVec.zCoord)
            ).expand(0.1, 0.1, 0.1);

            List<Entity> entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.thePlayer, checkBB);
            for (Entity entity : entities) {
                if (entity instanceof EntityLivingBase && entity != mc.thePlayer) {
                    if (entity instanceof EntityPlayer && AntiBot.isBot((EntityPlayer) entity)) continue;

                    MovingObjectPosition intercept = entity.getEntityBoundingBox().calculateIntercept(posVec, nextPosVec);
                    if (intercept != null) {
                        double dist = posVec.distanceTo(intercept.hitVec);
                        if (dist < currentDist) {
                            currentDist = dist;
                            hitEntity = entity;
                            landing = intercept;
                        }
                    }
                }
            }

            if (landing != null || hitEntity != null) {
                if (landing != null) {
                    posX = landing.hitVec.xCoord;
                    posY = landing.hitVec.yCoord;
                    posZ = landing.hitVec.zCoord;
                }
                
                pathPoints.add(new Vec3(posX, posY, posZ));
                break;
            }

            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            motionX *= 0.99;
            motionY *= 0.99;
            motionZ *= 0.99;
            motionY -= gravity;

            pathPoints.add(new Vec3(posX, posY, posZ));
        }

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GL11.glLineWidth(2.0f);

        double renderPosX = mc.getRenderManager().viewerPosX;
        double renderPosY = mc.getRenderManager().viewerPosY;
        double renderPosZ = mc.getRenderManager().viewerPosZ;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        float r = hitEntity != null ? 0.0f : 1.0f;
        float b = hitEntity != null ? 0.0f : 1.0f;

        worldrenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (Vec3 point : pathPoints) {
            worldrenderer.pos(point.xCoord - renderPosX, point.yCoord - renderPosY, point.zCoord - renderPosZ)
                    .color(r, 1.0f, b, 1.0f).endVertex();
        }
        tessellator.draw();

        if (landing != null || hitEntity != null) {
            GlStateManager.color(r, 1.0f, b, 1.0f);
            drawHitMarker(posX - renderPosX, posY - renderPosY, posZ - renderPosZ, landing != null ? landing.sideHit : null);
        }

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private void drawHitMarker(double x, double y, double z, EnumFacing side) {
        double size = 0.2;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        
        worldrenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        
        if (side == null) {
            worldrenderer.pos(x - size, y - size, z - size).endVertex();
            worldrenderer.pos(x + size, y + size, z + size).endVertex();
            worldrenderer.pos(x + size, y - size, z - size).endVertex();
            worldrenderer.pos(x - size, y + size, z + size).endVertex();
            worldrenderer.pos(x - size, y - size, z + size).endVertex();
            worldrenderer.pos(x + size, y + size, z - size).endVertex();
            worldrenderer.pos(x + size, y - size, z + size).endVertex();
            worldrenderer.pos(x - size, y + size, z - size).endVertex();
        } else {
            double off = 0.01;
            
            switch (side) {
                case UP:
                case DOWN:
                    double yOff = (side == EnumFacing.UP) ? off : -off;
                    worldrenderer.pos(x - size, y + yOff, z - size).endVertex();
                    worldrenderer.pos(x + size, y + yOff, z + size).endVertex();
                    worldrenderer.pos(x - size, y + yOff, z + size).endVertex();
                    worldrenderer.pos(x + size, y + yOff, z - size).endVertex();
                    break;
                case NORTH:
                case SOUTH:
                    double zOff = (side == EnumFacing.SOUTH) ? off : -off;
                    worldrenderer.pos(x - size, y - size, z + zOff).endVertex();
                    worldrenderer.pos(x + size, y + size, z + zOff).endVertex();
                    worldrenderer.pos(x - size, y + size, z + zOff).endVertex();
                    worldrenderer.pos(x + size, y - size, z + zOff).endVertex();
                    break;
                case EAST:
                case WEST:
                    double xOff = (side == EnumFacing.EAST) ? off : -off;
                    worldrenderer.pos(x + xOff, y - size, z - size).endVertex();
                    worldrenderer.pos(x + xOff, y + size, z + size).endVertex();
                    worldrenderer.pos(x + xOff, y + size, z - size).endVertex();
                    worldrenderer.pos(x + xOff, y - size, z + size).endVertex();
                    break;
            }
        }

        tessellator.draw();
    }
}
