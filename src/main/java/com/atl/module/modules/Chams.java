package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Chams extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public Chams() {
        super("Chams", "Allows you to see players through walls", Category.RENDER);
    }

    @SubscribeEvent
    public void onPreRender(RenderLivingEvent.Pre event) {
        if (!isEnabled()) return;

        // Only apply to other players to avoid visual glitches with your own hand/model
        if (event.entity instanceof EntityPlayer && event.entity != mc.thePlayer) {
            // Enable polygon offset and push the model to the front of the depth buffer
            GlStateManager.enablePolygonOffset();
            GlStateManager.doPolygonOffset(1.0F, -1000000.0F);

            // Disable lighting to make the player "glow" through walls
            GlStateManager.disableLighting();
        }
    }

    @SubscribeEvent
    public void onPostRender(RenderLivingEvent.Post event) {
        if (!isEnabled()) return;

        if (event.entity instanceof EntityPlayer && event.entity != mc.thePlayer) {
            // Cleanup GL state to avoid leaking settings to the rest of the world
            GlStateManager.disablePolygonOffset();
            GlStateManager.doPolygonOffset(1.0F, 1.0F);
            GlStateManager.enableLighting();
        }
    }
}
