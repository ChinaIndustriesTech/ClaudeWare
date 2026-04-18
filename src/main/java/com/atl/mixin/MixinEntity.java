package com.atl.mixin;

import com.atl.module.ExampleMod;
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
        FreeLook freeLook = (FreeLook) ExampleMod.moduleManager.get("FreeLook");

        if (freeLook != null && freeLook.isEnabled()) {
            // Pass raw mouse delta to FreeLook logic
            freeLook.handleMouseInput(yaw, pitch);
            // Cancel the turn so the player's actual rotation stays still
            ci.cancel();
        }
    }
}