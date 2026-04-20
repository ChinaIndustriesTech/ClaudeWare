package com.atl.mixin;

import com.atl.module.Claude;
import com.atl.module.modules.Reach;
import com.atl.module.modules.FreeLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Unique private float atl$originalYaw, atl$originalPitch;
    @Unique private float atl$originalPrevYaw, atl$originalPrevPitch;

    @Inject(method = "orientCamera(F)V", at = @At("HEAD"))
    private void preOrientCamera(float partialTicks, CallbackInfo ci) {
        FreeLook freeLook = (FreeLook) Claude.moduleManager.get("FreeLook");

        if (freeLook != null && freeLook.isEnabled() && Minecraft.getMinecraft().getRenderViewEntity() != null) {
            Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
            if (entity != null) {
                atl$originalYaw = entity.rotationYaw;
                atl$originalPitch = entity.rotationPitch;
                atl$originalPrevYaw = entity.prevRotationYaw;
                atl$originalPrevPitch = entity.prevRotationPitch;

                entity.prevRotationYaw = freeLook.prevFreeYaw;
                entity.rotationYaw = freeLook.freeYaw;
                entity.prevRotationPitch = freeLook.prevFreePitch;
                entity.rotationPitch = freeLook.freePitch;
            }
        }
    }

    @Inject(method = "orientCamera(F)V", at = @At("RETURN"))
    private void postOrientCamera(float partialTicks, CallbackInfo ci) {
        FreeLook freeLook = (FreeLook) Claude.moduleManager.get("FreeLook");
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
        
        if (freeLook != null && freeLook.isEnabled() && entity != null) {
            entity.rotationYaw = atl$originalYaw;
            entity.rotationPitch = atl$originalPitch;
            entity.prevRotationYaw = atl$originalPrevYaw;
            entity.prevRotationPitch = atl$originalPrevPitch;
        }
    }

    @ModifyConstant(method = "getMouseOver", constant = @Constant(doubleValue = 3.0D))
    private double getReach(double distance) {
        Reach reach = (Reach) Claude.moduleManager.get("Reach");
        if (reach != null && reach.isEnabled()) {
            if (Math.random() * 100.0 > reach.chance.value) {
                return distance;
            }
            
            return reach.distance.value;
        }
        return distance;
    }

    @ModifyConstant(method = "getMouseOver", constant = @Constant(doubleValue = 6.0D))
    private double getReachTrace(double distance) {
        Reach reach = (Reach) Claude.moduleManager.get("Reach");
        if (reach != null && reach.isEnabled()) {
            if (Math.random() * 100.0 > reach.chance.value) {
                return distance;
            }

            return reach.distance.value;
        }
        return distance;
    }
}