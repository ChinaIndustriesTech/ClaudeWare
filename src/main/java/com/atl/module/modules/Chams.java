package com.atl.module.modules;

import com.atl.module.management.Module;
import com.atl.module.modules.AntiBot;
import com.atl.module.management.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Chams extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public Chams() {
        super("Chams", "Allows you to see thru walls", Category.RENDER);
    }

    @SubscribeEvent
    public void onPreRender(RenderLivingEvent.Pre event) {
        if (!isEnabled()) return;
        if (event.entity instanceof EntityPlayer && event.entity != mc.thePlayer) {
            EntityPlayer player = (EntityPlayer) event.entity;
            
            if (AntiBot.isBot(player)) return;

            // Use PolygonOffset to pull the player model "forward" in the depth buffer
            GlStateManager.enablePolygonOffset();
            GlStateManager.doPolygonOffset(1.0F, -1500000.0F);
            
            // Optional: Disable lighting to make them "glow" through walls
            GlStateManager.disableLighting();
        }
    }

    @SubscribeEvent
    public void onPostRender(RenderLivingEvent.Post event) {
        if (!isEnabled()) return;
        if (event.entity instanceof EntityPlayer && event.entity != mc.thePlayer) {
            EntityPlayer player = (EntityPlayer) event.entity;
            
            if (AntiBot.isBot(player)) return;

            // Cleanup state
            GlStateManager.enableLighting();
            GlStateManager.disablePolygonOffset();
            GlStateManager.doPolygonOffset(1.0F, 1.0F);
        }
    }
}
