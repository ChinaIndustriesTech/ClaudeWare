package com.atl.mixin;

import com.atl.module.Claude;
import com.atl.module.modules.FreeLook;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method = "setAngles(FF)V", at = @At("HEAD"), cancellable = true)
    private void onSetAngles(float yaw, float pitch, CallbackInfo ci) {
        FreeLook freeLook = (FreeLook) Claude.moduleManager.get("FreeLook");

        if (freeLook != null && freeLook.isEnabled()) {
            freeLook.handleMouseInput(yaw, pitch);
            ci.cancel();
        }
    }
}