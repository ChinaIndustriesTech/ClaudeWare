package com.atl.mixin;

import com.atl.module.Claude;
import com.atl.module.modules.Nametags;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRenderLivingEntity<T extends EntityLivingBase> {

    @Inject(method = "canRenderName", at = @At("HEAD"), cancellable = true)
    private void onCanRenderName(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof EntityPlayer) {
            Nametags nametags = (Nametags) Claude.moduleManager.get("Nametags");
            if (nametags != null && nametags.isEnabled()) {
                cir.setReturnValue(false);
            }
        }
    }
}
