package com.atl.module.management;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class RotationManager {

    private float lastUpdate;
    private float yawDelta;
    private float pitchDelta;
    private int priority;
    private boolean rotated;
    
    private float targetYaw;
    private float targetPitch;

    public RotationManager() {
        resetRotationState();
    }

    private void applyRotation(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && !Float.isNaN(this.yawDelta) && !Float.isNaN(this.pitchDelta) && !Float.isNaN(this.lastUpdate)) {
            
            float sensitivity = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float gcd = sensitivity * sensitivity * sensitivity * 1.2F;

            float yawIncrement = this.yawDelta * (partialTicks - this.lastUpdate);
            yawIncrement = yawIncrement - (yawIncrement % gcd);

            if (yawIncrement != 0.0F) {
                mc.thePlayer.prevRotationYaw = mc.thePlayer.rotationYaw;
                mc.thePlayer.rotationYaw += yawIncrement;
            }

            float pitchIncrement = this.pitchDelta * (partialTicks - this.lastUpdate);
            pitchIncrement = pitchIncrement - (pitchIncrement % gcd);

            if (pitchIncrement != 0.0F) {
                mc.thePlayer.prevRotationPitch = mc.thePlayer.rotationPitch;
                mc.thePlayer.rotationPitch += pitchIncrement;
                mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch, -89.9F, 89.9F);
            }

            this.lastUpdate = partialTicks;
        }
    }

    private void resetRotationState() {
        this.lastUpdate = Float.NaN;
        this.yawDelta = Float.NaN;
        this.pitchDelta = Float.NaN;
        this.targetYaw = Float.NaN;
        this.targetPitch = Float.NaN;
        this.priority = Integer.MIN_VALUE;
        this.rotated = false;
    }

    public void setRotation(float yaw, float pitch, int priority, boolean instant) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && this.priority <= priority) {
            this.priority = priority;
            this.targetYaw = yaw;
            this.targetPitch = pitch;
            
            if (instant) {
                mc.thePlayer.rotationYaw = yaw;
                mc.thePlayer.rotationPitch = MathHelper.clamp_float(pitch, -89.9F, 89.9F);
                this.rotated = true;
                this.yawDelta = 0;
                this.pitchDelta = 0;
            } else {
                this.yawDelta = MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);
                this.pitchDelta = MathHelper.clamp_float(pitch - mc.thePlayer.rotationPitch, -89.9F, 89.9F);
                this.lastUpdate = 0.0F;
                this.rotated = false;
            }
        }
    }

    public boolean isRotated() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || Float.isNaN(targetYaw)) return false;
        
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw));
        float pitchDiff = Math.abs(targetPitch - mc.thePlayer.rotationPitch);
        
        return yawDiff < 1.0F && pitchDiff < 1.0F;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        this.applyRotation(1.0F);
        this.resetRotationState();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRender3D(RenderWorldLastEvent event) {
        this.applyRotation(event.partialTicks);
    }
}
