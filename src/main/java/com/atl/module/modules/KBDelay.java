package com.atl.module.modules;

import com.atl.event.PacketEvent;
import com.atl.module.Claude;
import com.atl.module.management.BlinkModules;
import com.atl.module.management.BooleanSetting;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.*;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KBDelay extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final NumberSetting distanceToTarget = new NumberSetting("Distance to target", 6.0, 3.0, 12.0, 0.1);
    private final NumberSetting maximumDelay = new NumberSetting("Maximum delay", 200, 50, 1000, 10);
    private final NumberSetting chance = new NumberSetting("Chance %", 100, 0, 100, 1);
    
    private final BooleanSetting inAir = new BooleanSetting("In air", true);
    private final BooleanSetting lookingAtPlayer = new BooleanSetting("Looking at player", false);
    private final BooleanSetting requireLMB = new BooleanSetting("Require LMB", false);
    private final BooleanSetting bidirectional = new BooleanSetting("Bidirectional", true);
    private final BooleanSetting showBox = new BooleanSetting("Show Box", true);

    private final Queue<TimedPacket> inboundQueue = new ConcurrentLinkedQueue<>();
    private boolean blinking = false;
    private long lastBlinkStartTime = 0;
    private double savedX, savedY, savedZ;

    public KBDelay() {
        super("KBDelay", "Delays knockback packets", Category.COMBAT);
        addSettings(distanceToTarget, maximumDelay, chance, inAir, lookingAtPlayer, requireLMB, bidirectional, showBox);
    }

    @Override
    public void onDisable() {
        flush();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof S08PacketPlayerPosLook) {
            flush();
            return;
        }

        if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity velocityPacket = (S12PacketEntityVelocity) packet;
            if (velocityPacket.getEntityID() == mc.thePlayer.getEntityId()) {
                
                if (blinking) {
                    event.setCanceled(true);
                    inboundQueue.add(new TimedPacket(packet, System.currentTimeMillis()));
                    return;
                }

                if (shouldDelay()) {
                    event.setCanceled(true);
                    inboundQueue.add(new TimedPacket(packet, System.currentTimeMillis()));
                    startBlinking();
                }
                return;
            }
        }

        if (!blinking) return;

        if (packet instanceof S07PacketRespawn) return;
        if (packet instanceof S03PacketTimeUpdate) return;
        if (packet instanceof S06PacketUpdateHealth) return;
        if (packet instanceof S13PacketDestroyEntities) return;
        if (packet instanceof S02PacketChat) return;
        if (packet instanceof S2FPacketSetSlot) return;

        event.setCanceled(true);
        inboundQueue.add(new TimedPacket(packet, System.currentTimeMillis()));
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.isDead) {
            flush();
            return;
        }

        if (!blinking) return;

        long now = System.currentTimeMillis();
        
        // FORCE FLUSH check: If the lag session has lasted longer than maximumDelay, flush everything.
        if (now - lastBlinkStartTime >= maximumDelay.value) {
            flush();
            return;
        }

        // Standard release of expired packets
        while (!inboundQueue.isEmpty()) {
            TimedPacket timed = inboundQueue.peek();
            if (timed != null && now - timed.time >= maximumDelay.value) {
                inboundQueue.poll();
                processPacket(timed.packet);
            } else {
                break;
            }
        }

        if (inboundQueue.isEmpty()) {
            stopBlinking();
        }
    }

    private boolean shouldDelay() {
        if (chance.value < 100 && Math.random() * 100 > chance.value) return false;
        
        EntityPlayer target = getNearestPlayer(distanceToTarget.value);
        if (target == null) return false;

        if (inAir.isEnabled() && mc.thePlayer.onGround) return false;
        if (lookingAtPlayer.isEnabled() && !isLookingAtPlayer(target)) return false;
        if (requireLMB.isEnabled() && !Mouse.isButtonDown(0)) return false;

        return true;
    }

    private void startBlinking() {
        blinking = true;
        lastBlinkStartTime = System.currentTimeMillis();
        savedX = mc.thePlayer.posX;
        savedY = mc.thePlayer.posY;
        savedZ = mc.thePlayer.posZ;
        if (bidirectional.isEnabled()) {
            Claude.blinkManager.setBlinkState(true, BlinkModules.KBDELAY);
        }
    }

    private void stopBlinking() {
        blinking = false;
        if (bidirectional.isEnabled()) {
            Claude.blinkManager.setBlinkState(false, BlinkModules.KBDELAY);
        }
    }

    private void flush() {
        stopBlinking();
        while (!inboundQueue.isEmpty()) {
            TimedPacket timed = inboundQueue.poll();
            if (timed != null) processPacket(timed.packet);
        }
    }

    @SuppressWarnings("unchecked")
    private void processPacket(Packet<?> packet) {
        if (mc.getNetHandler() != null) {
            ((Packet<INetHandlerPlayClient>) packet).processPacket(mc.getNetHandler());
        }
    }

    private EntityPlayer getNearestPlayer(double range) {
        EntityPlayer closest = null;
        double closestDist = range * range;
        
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isDead) continue;
            if (AntiBot.isBot(player)) continue;
            
            double distSq = mc.thePlayer.getDistanceSqToEntity(player);
            if (distSq <= closestDist) {
                closestDist = distSq;
                closest = player;
            }
        }
        return closest;
    }

    private boolean isLookingAtPlayer(EntityPlayer target) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);
        Vec3 look = mc.thePlayer.getLook(1.0F);
        double range = distanceToTarget.value;
        Vec3 end = eyes.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range);
        
        AxisAlignedBB bb = target.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
        MovingObjectPosition mop = bb.calculateIntercept(eyes, end);
        
        return mop != null;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled() || !showBox.isEnabled() || !blinking) return;
        
        net.minecraft.client.renderer.entity.RenderManager rm = mc.getRenderManager();
        double x = savedX - rm.viewerPosX;
        double y = savedY - rm.viewerPosY;
        double z = savedZ - rm.viewerPosZ;
        double w = 0.3, h = 1.8;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableLighting();
        GL11.glLineWidth(1.5f);
        GL11.glColor4f(1.0f, 0.5f, 0.0f, 0.6f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x-w, y, z-w); GL11.glVertex3d(x+w, y, z-w);
        GL11.glVertex3d(x+w, y, z-w); GL11.glVertex3d(x+w, y, z+w);
        GL11.glVertex3d(x+w, y, z+w); GL11.glVertex3d(x-w, y, z+w);
        GL11.glVertex3d(x-w, y, z+w); GL11.glVertex3d(x-w, y, z-w);
        GL11.glVertex3d(x-w, y+h, z-w); GL11.glVertex3d(x+w, y+h, z-w);
        GL11.glVertex3d(x+w, y+h, z-w); GL11.glVertex3d(x+w, y+h, z+w);
        GL11.glVertex3d(x+w, y+h, z+w); GL11.glVertex3d(x-w, y+h, z+w);
        GL11.glVertex3d(x-w, y+h, z+w); GL11.glVertex3d(x-w, y+h, z-w);
        GL11.glVertex3d(x-w, y, z-w); GL11.glVertex3d(x-w, y+h, z-w);
        GL11.glVertex3d(x+w, y, z-w); GL11.glVertex3d(x+w, y+h, z-w);
        GL11.glVertex3d(x+w, y, z+w); GL11.glVertex3d(x+w, y+h, z+w);
        GL11.glVertex3d(x-w, y, z+w); GL11.glVertex3d(x-w, y+h, z+w);
        GL11.glEnd();
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private static class TimedPacket {
        final Packet<?> packet;
        final long time;
        TimedPacket(Packet<?> packet, long time) { this.packet = packet; this.time = time; }
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("Delay: " + (int)maximumDelay.value + "ms", "Bidirectional: " + bidirectional.isEnabled());
    }
}