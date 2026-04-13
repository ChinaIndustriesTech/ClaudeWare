package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlayerTracker extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public PlayerTracker() {
        super("PlayerTracker", "hud playertracker", Category.RENDER);
    }

    @SubscribeEvent
    public void onRenderHUD(RenderGameOverlayEvent.Post event) {
        if (!isEnabled()) return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Collect players within 100 blocks
        List<EntityPlayer> nearby = new ArrayList<>();
        for (Object obj : mc.theWorld.playerEntities) {
            if (!(obj instanceof EntityPlayer)) continue;
            EntityPlayer player = (EntityPlayer) obj;
            if (player == mc.thePlayer) continue;
            
            // AntiBot Check
            if (AntiBot.isBot(player)) continue;

            if (mc.thePlayer.getDistanceToEntity(player) <= 100.0f) {
                nearby.add(player);
            }
        }

        if (nearby.isEmpty()) return;

        // Sort by distance, closest first
        nearby.sort((a, b) -> Double.compare(
                mc.thePlayer.getDistanceToEntity(a),
                mc.thePlayer.getDistanceToEntity(b)
        ));

        FontRenderer fr = mc.fontRendererObj;
        ScaledResolution sr = new ScaledResolution(mc);

        int x = 4;
        int y = sr.getScaledHeight() - 10 - (nearby.size() * (fr.FONT_HEIGHT + 2));

        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);

        for (EntityPlayer player : nearby) {
            float dist = mc.thePlayer.getDistanceToEntity(player);
            String line = player.getName() + " - " + String.format("%.1f", dist) + "m";
            fr.drawStringWithShadow(line, x, y, 0xFFFFFFFF);
            y += fr.FONT_HEIGHT + 2;
        }

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
