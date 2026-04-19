package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.Set;

public class ItemESP extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Set<Item> targetItems = new HashSet<>();

    public ItemESP() {
        super("ItemESP", "shows dia gold ems and iron", Category.RENDER);
        targetItems.add(Items.iron_ingot);
        targetItems.add(Items.gold_ingot);
        targetItems.add(Items.diamond);
        targetItems.add(Items.emerald);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        double px = mc.getRenderManager().viewerPosX;
        double py = mc.getRenderManager().viewerPosY;
        double pz = mc.getRenderManager().viewerPosZ;

        Set<EntityItem> processedItems = new HashSet<>();

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (obj instanceof EntityItem) {
                EntityItem entityItem = (EntityItem) obj;
                if (processedItems.contains(entityItem)) continue;

                Item item = entityItem.getEntityItem().getItem();

                if (targetItems.contains(item)) {
                    // Grouping Logic: Find all items of the same type within 3 blocks
                    int totalStackSize = 0;
                    EntityItem newestItem = entityItem;

                    for (Entity other : mc.theWorld.loadedEntityList) {
                        if (other instanceof EntityItem) {
                            EntityItem otherItem = (EntityItem) other;
                            if (otherItem.getEntityItem().getItem() == item && entityItem.getDistanceToEntity(otherItem) <= 3.0) {
                                totalStackSize += otherItem.getEntityItem().stackSize;
                                processedItems.add(otherItem);
                                if (otherItem.ticksExisted < newestItem.ticksExisted) {
                                    newestItem = otherItem;
                                }
                            }
                        }
                    }

                    float r = 1.0f, g = 1.0f, b = 1.0f;
                    if (item == Items.iron_ingot) { r = 0.8f; g = 0.8f; b = 0.8f; }
                    else if (item == Items.gold_ingot) { r = 1.0f; g = 0.9f; b = 0.0f; }
                    else if (item == Items.diamond) { r = 0.0f; g = 1.0f; b = 1.0f; }
                    else if (item == Items.emerald) { r = 0.0f; g = 1.0f; b = 0.0f; }

                    // Interpolate position for smooth movement
                    double x = newestItem.lastTickPosX + (newestItem.posX - newestItem.lastTickPosX) * event.partialTicks - px;
                    double y = newestItem.lastTickPosY + (newestItem.posY - newestItem.lastTickPosY) * event.partialTicks - py;
                    double z = newestItem.lastTickPosZ + (newestItem.posZ - newestItem.lastTickPosZ) * event.partialTicks - pz;

                    // Draw Box
                    drawBox(x, y, z, r, g, b);

                    // Draw Stack Size Text with Shadow and Outline
                    drawText(String.valueOf(totalStackSize), x, y + 0.5, z);
                }
            }
        }
    }

    private void drawBox(double x, double y, double z, float r, float g, float b) {
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GL11.glLineWidth(1.5f);

        AxisAlignedBB bb = new AxisAlignedBB(
                x - 0.2, y, z - 0.2,
                x + 0.2, y + 0.4, z + 0.2
        );

        GlStateManager.color(r, g, b, 1.0f);
        RenderGlobal.drawOutlinedBoundingBox(bb, (int)(r * 255), (int)(g * 255), (int)(b * 255), 255);
        
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private void drawText(String text, double x, double y, double z) {
        FontRenderer fr = mc.fontRendererObj;
        float scale = 0.03f; // Slightly larger text
        
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0f, 0.0f, 0.0f);
        GlStateManager.scale(-scale, -scale, scale);
        
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        
        int width = fr.getStringWidth(text);
        
        // Custom background for better visibility
        drawRect(-width / 2 - 2, -1, width / 2 + 2, 9, 0x90000000); 

        // Draw with Shadow
        fr.drawStringWithShadow(text, -width / 2, 0, -1);
        
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    private void drawRect(int left, int top, int right, int bottom, int color) {
        if (left < right) {
            int i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            int j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float)(color >> 24 & 255) / 255.0F;
        float f = (float)(color >> 16 & 255) / 255.0F;
        float f1 = (float)(color >> 8 & 255) / 255.0F;
        float f2 = (float)(color & 255) / 255.0F;
        
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(f, f1, f2, f3);
        
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(left, bottom);
        GL11.glVertex2f(right, bottom);
        GL11.glVertex2f(right, top);
        GL11.glVertex2f(left, top);
        GL11.glEnd();
        
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
