package com.atl.mixin;

import com.atl.module.ExampleMod;
import com.atl.module.modules.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Unique private float atl$originalYaw, atl$originalPitch;
    @Unique private float atl$originalPrevYaw, atl$originalPrevPitch;

    @Inject(method = "orientCamera(F)V", at = @At("HEAD"))
    private void preOrientCamera(float partialTicks, CallbackInfo ci) {
        FreeLook freeLook = (FreeLook) ExampleMod.moduleManager.get("FreeLook");

        if (freeLook != null && freeLook.isEnabled() && Minecraft.getMinecraft().getRenderViewEntity() != null) {
            Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
            if (entity != null) {
                // Backup the real rotations
                atl$originalYaw = entity.rotationYaw;
                atl$originalPitch = entity.rotationPitch;
                atl$originalPrevYaw = entity.prevRotationYaw;
                atl$originalPrevPitch = entity.prevRotationPitch;

                // Inject the FreeLook rotations for the camera's math
                // Using prevFreeYaw/Pitch ensures smooth interpolation at high FPS
                entity.prevRotationYaw = freeLook.prevFreeYaw;
                entity.rotationYaw = freeLook.freeYaw;
                entity.prevRotationPitch = freeLook.prevFreePitch;
                entity.rotationPitch = freeLook.freePitch;
            }
        }
    }

    @Inject(method = "orientCamera(F)V", at = @At("RETURN"))
    private void postOrientCamera(float partialTicks, CallbackInfo ci) {
        FreeLook freeLook = (FreeLook) ExampleMod.moduleManager.get("FreeLook");
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        
        if (freeLook != null && freeLook.isEnabled() && entity != null) {
            // Restore real rotations so hitboxes and model rendering aren't broken
            entity.rotationYaw = atl$originalYaw;
            entity.rotationPitch = atl$originalPitch;
            entity.prevRotationYaw = atl$originalPrevYaw;
            entity.prevRotationPitch = atl$originalPrevPitch;
        }
    }
}