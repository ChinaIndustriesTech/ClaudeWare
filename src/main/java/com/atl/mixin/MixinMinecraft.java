package com.atl.mixin;

import com.atl.module.Claude;
import com.atl.module.modules.NoHitDelay;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow
    public int leftClickCounter;

    /**
     * Resets the hit delay counter if NoHitDelay is enabled.
     * MCP: clickMouse | SRG: func_147116_af
     */
    @Inject(method = "clickMouse", at = @At("HEAD"))
    private void onClickMouse(CallbackInfo ci) {
        NoHitDelay noHitDelay = (NoHitDelay) Claude.moduleManager.get("NoHitDelay");
        if (noHitDelay != null && noHitDelay.isEnabled()) {
            this.leftClickCounter = 0;
        }
    }
}