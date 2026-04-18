package com.atl.module.management;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Centralizes player rotations to ensure smooth movement and 
 * prevent conflicts between different modules.
 */
public class RotationManager {

    private float lastUpdate;
    private float yawDelta;
    private float pitchDelta;
    private int priority;
    private boolean rotated;

    public RotationManager() {
        resetRotationState();
    }

    /**
     * Applies a slice of the rotation delta based on the current partial ticks.
     * This ensures the rotation looks smooth regardless of the frame rate.
     */
    private void applyRotation(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && !Float.isNaN(this.yawDelta) && !Float.isNaN(this.pitchDelta) && !Float.isNaN(this.lastUpdate)) {
            
            float yaw = this.yawDelta * (partialTicks - this.lastUpdate);
            if (yaw != 0.0F) {
                mc.thePlayer.prevRotationYaw = mc.thePlayer.rotationYaw;
                mc.thePlayer.rotationYaw += yaw;
            }

            float pitch = this.pitchDelta * (partialTicks - this.lastUpdate);
            if (pitch != 0.0F) {
                mc.thePlayer.prevRotationPitch = mc.thePlayer.rotationPitch;
                mc.thePlayer.rotationPitch += pitch;
                // Enforce Minecraft pitch limits (-90 to 90)
                mc.thePlayer.rotationPitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch, -90.0F, 90.0F);
            }

            this.lastUpdate = partialTicks;
        }
    }

    private void resetRotationState() {
        this.lastUpdate = Float.NaN;
        this.yawDelta = Float.NaN;
        this.pitchDelta = Float.NaN;
        this.priority = Integer.MIN_VALUE;
        this.rotated = false;
    }

    /**
     * Modules call this to request a rotation.
     * @param yaw The target yaw
     * @param pitch The target pitch
     * @param priority Higher numbers override lower numbers
     * @param force Whether to mark the 'rotated' flag as true
     */
    public void setRotation(float yaw, float pitch, int priority, boolean force) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && this.priority <= priority) {
            this.priority = priority;
            this.yawDelta = MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);
            this.pitchDelta = MathHelper.clamp_float(pitch - mc.thePlayer.rotationPitch, -90.0F, 90.0F);
            this.lastUpdate = 0.0F;
            this.rotated = force;
            
            // Apply the initial 0.0 tick slice immediately
            this.applyRotation(0.0F);
        }
    }

    public boolean isRotated() {
        return this.rotated;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        // Finish the final slice of the previous tick's rotation
        this.applyRotation(1.0F);
        
        // Clear state for the new tick
        this.resetRotationState();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRender3D(RenderWorldLastEvent event) {
        // Distribute the rotation delta across the render frames for sub-tick smoothness
        this.applyRotation(event.partialTicks);
    }
}
