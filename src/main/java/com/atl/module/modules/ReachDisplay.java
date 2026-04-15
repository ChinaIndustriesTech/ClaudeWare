package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.List;

public class ReachDisplay extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    
    private double lastReach = -1.0;
    private long lastHitTime = 0;
    private static final long COOLDOWN_MS = 500; // 500ms cooldown between updates

    public NumberSetting xPos = new NumberSetting("X Position", 415, 0, 1000, 0.5);
    public NumberSetting yPos = new NumberSetting("Y Position", 278, 0, 1000, 0.5);

    public ReachDisplay() {
        super("ReachDisplay", "Displays the distance of your last hit", Category.RENDER);
        addSettings(xPos, yPos);
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
                "XPos: " + xPos.value,
                "YPos: " + yPos.value
        );
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!isEnabled()) return;
        if (event.entityPlayer == mc.thePlayer && event.target != null) {
            long currentTime = System.currentTimeMillis();
            
            // Only update the display if 500ms has passed since the last recorded hit
            if (currentTime - lastHitTime >= COOLDOWN_MS) {
                // Accurate Eye-to-Hitbox calculation
                Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
                AxisAlignedBB bb = event.target.getEntityBoundingBox();
                
                double closestX = clamp(eyePos.xCoord, bb.minX, bb.maxX);
                double closestY = clamp(eyePos.yCoord, bb.minY, bb.maxY);
                double closestZ = clamp(eyePos.zCoord, bb.minZ, bb.maxZ);
                
                double dist = eyePos.distanceTo(new Vec3(closestX, closestY, closestZ));
                
                // Assume 3.00 if the hit was registered but measured outside 3 blocks
                if (dist > 3.0) {
                    dist = 3.0;
                }
                
                lastReach = dist;
                lastHitTime = currentTime;
            }
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!isEnabled() || event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        // Display for 3 seconds after the hit
        if (lastReach == -1.0 || System.currentTimeMillis() - lastHitTime > 3000) return;

        String text = EnumChatFormatting.GRAY + "[" + EnumChatFormatting.WHITE + String.format("%.2f", lastReach) + EnumChatFormatting.GRAY + "]";
        mc.fontRendererObj.drawStringWithShadow(text, (float)xPos.value, (float)yPos.value, -1);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
