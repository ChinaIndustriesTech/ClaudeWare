package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class FireballDetector extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public FireballDetector() {
        super("FireballDetector", "indicates fireballs so u dont die retardedly", Category.RENDER);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled() || event.type != RenderGameOverlayEvent.ElementType.ALL || mc.theWorld == null || mc.thePlayer == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float centerX = sr.getScaledWidth() / 2.0f;
        float centerY = sr.getScaledHeight() / 2.0f;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLargeFireball) {
                double dist = mc.thePlayer.getDistanceToEntity(entity);
                if (dist <= 64.0) {
                    drawIndicator(entity, centerX, centerY, dist);
                }
            }
        }
    }

    private void drawIndicator(Entity entity, float centerX, float centerY, double distance) {
        double diffX = entity.posX - mc.thePlayer.posX;
        double diffZ = entity.posZ - mc.thePlayer.posZ;
        
        // Calculate the angle to the fireball relative to the player's yaw
        float yaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f);
        float angle = yaw - mc.thePlayer.rotationYaw;

        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.rotate(angle, 0, 0, 1);
        
        // Offset the arrow from the center
        float radius = 40.0f;
        GlStateManager.translate(0, -radius, 0);

        // Draw a simple triangle/arrow
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        // Change color based on distance
        if (distance < 15) {
            GlStateManager.color(1.0f, 0.0f, 0.0f, 0.8f); // Red (Dangerous)
        } else if (distance < 30) {
            GlStateManager.color(1.0f, 0.5f, 0.0f, 0.8f); // Orange
        } else {
            GlStateManager.color(1.0f, 1.0f, 0.0f, 0.8f); // Yellow
        }

        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glVertex2f(0, -5);   // Tip
        GL11.glVertex2f(-4, 3);   // Left base
        GL11.glVertex2f(4, 3);    // Right base
        GL11.glEnd();

        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();

        // Optional: Draw distance text near the arrow
        GlStateManager.pushMatrix();
        // Calculate position for text
        double rad = Math.toRadians(angle - 90);
        float textX = centerX + (float) Math.cos(rad) * (radius + 15);
        float textY = centerY + (float) Math.sin(rad) * (radius + 15);
        
        String distStr = (int) distance + "m";
        int color = distance < 15 ? 0xFFFF0000 : (distance < 30 ? 0xFFFF6600 : 0xFFFFFF00);
        mc.fontRendererObj.drawStringWithShadow(distStr, textX - (mc.fontRendererObj.getStringWidth(distStr) / 2.0f), textY - 4, color);
        GlStateManager.popMatrix();
    }
}
