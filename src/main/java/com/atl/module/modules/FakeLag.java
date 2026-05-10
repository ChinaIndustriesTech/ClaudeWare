package com.atl.module.modules;

import com.atl.module.Claude;
import com.atl.module.management.BlinkModules;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import com.atl.module.management.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.List;

public class FakeLag extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public NumberSetting blinkDuration = new NumberSetting("Duration", 250, 50, 500, 10);
    public NumberSetting triggerRange = new NumberSetting("Range", 6.0, 2.0, 20.0, 0.5);
    public BooleanSetting flushIfHit = new BooleanSetting("Flush if hit", true);

    private boolean lagging = false;
    private long lagStartTime = 0;
    private boolean waitingForOutOfRange = false;

    public FakeLag() {
        super("LagRange", "Holds packets to teleport near enemies", Category.COMBAT);
        addSettings(blinkDuration, triggerRange, flushIfHit);
    }

    @Override
    public void onDisable() {
        if (lagging) {
            Claude.blinkManager.setBlinkState(false, BlinkModules.FAKELAG);
            lagging = false;
        }
        waitingForOutOfRange = false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
                "duration: " + (int) blinkDuration.value + "ms",
                "range: " + triggerRange.value,
                "flushIfHit: " + flushIfHit.isEnabled()
        );
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!isEnabled()) return;
        if (mc.theWorld == null || mc.thePlayer == null || mc.getNetHandler() == null) return;

        EntityPlayer closest = getClosestPlayer();

        if (waitingForOutOfRange) {
            // Wait until closest enemy is outside trigger range before re-blinking
            if (closest == null || getEyeLevelDistance(closest) > triggerRange.value) {
                waitingForOutOfRange = false;
            }
            return;
        }

        if (!lagging) {
            // Start lagging if enemy is within range
            if (closest != null && getEyeLevelDistance(closest) <= triggerRange.value) {
                Claude.blinkManager.setBlinkState(true, BlinkModules.FAKELAG);
                lagging = true;
                lagStartTime = System.currentTimeMillis();
            }
            return;
        }

        // Currently lagging — check if we should release
        long elapsed = System.currentTimeMillis() - lagStartTime;
        boolean durationReached = elapsed >= blinkDuration.value;
        boolean withinStrikeRange = closest != null && getEyeLevelDistance(closest) <= 2.9;
        boolean tookDamage = flushIfHit.isEnabled() && mc.thePlayer.hurtTime > 0;

        if (durationReached || withinStrikeRange || tookDamage) {
            Claude.blinkManager.setBlinkState(false, BlinkModules.FAKELAG);
            lagging = false;
            waitingForOutOfRange = true;
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || !lagging) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        double startX = Claude.blinkManager.getStartX();
        double startY = Claude.blinkManager.getStartY();
        double startZ = Claude.blinkManager.getStartZ();

        // Offset by render position
        RenderManager rm = mc.getRenderManager();
        double rx = startX - rm.viewerPosX;
        double ry = startY - rm.viewerPosY;
        double rz = startZ - rm.viewerPosZ;

        // Player hitbox dimensions
        double w = 0.3; // half width
        double h = 1.8;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableLighting();

        GL11.glLineWidth(1.5f);
        GL11.glColor4f(1.0f, 0.0f, 0.0f, 0.5f); // Red, semi-transparent

        GL11.glBegin(GL11.GL_LINES);

        // Bottom face
        GL11.glVertex3d(rx - w, ry,     rz - w);
        GL11.glVertex3d(rx + w, ry,     rz - w);

        GL11.glVertex3d(rx + w, ry,     rz - w);
        GL11.glVertex3d(rx + w, ry,     rz + w);

        GL11.glVertex3d(rx + w, ry,     rz + w);
        GL11.glVertex3d(rx - w, ry,     rz + w);

        GL11.glVertex3d(rx - w, ry,     rz + w);
        GL11.glVertex3d(rx - w, ry,     rz - w);

        // Top face
        GL11.glVertex3d(rx - w, ry + h, rz - w);
        GL11.glVertex3d(rx + w, ry + h, rz - w);

        GL11.glVertex3d(rx + w, ry + h, rz - w);
        GL11.glVertex3d(rx + w, ry + h, rz + w);

        GL11.glVertex3d(rx + w, ry + h, rz + w);
        GL11.glVertex3d(rx - w, ry + h, rz + w);

        GL11.glVertex3d(rx - w, ry + h, rz + w);
        GL11.glVertex3d(rx - w, ry + h, rz - w);

        // Vertical edges
        GL11.glVertex3d(rx - w, ry,     rz - w);
        GL11.glVertex3d(rx - w, ry + h, rz - w);

        GL11.glVertex3d(rx + w, ry,     rz - w);
        GL11.glVertex3d(rx + w, ry + h, rz - w);

        GL11.glVertex3d(rx + w, ry,     rz + w);
        GL11.glVertex3d(rx + w, ry + h, rz + w);

        GL11.glVertex3d(rx - w, ry,     rz + w);
        GL11.glVertex3d(rx - w, ry + h, rz + w);

        GL11.glEnd();

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private EntityPlayer getClosestPlayer() {
        EntityPlayer closest = null;
        double closestDist = Double.MAX_VALUE;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || !player.isEntityAlive()) continue;
            if (AntiBot.isBot(player) || Teams.isTeammate(player)) continue;

            double dist = getEyeLevelDistance(player);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }
        return closest;
    }

    private double getEyeLevelDistance(EntityPlayer player) {
        double dx = mc.thePlayer.posX - player.posX;
        double dy = (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()) - (player.posY + player.getEyeHeight());
        double dz = mc.thePlayer.posZ - player.posZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}