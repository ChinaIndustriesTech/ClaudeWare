package com.atl.mixin;

import com.atl.module.Claude;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.entity.EntityPlayerSP.class)
public class MixinEntityPlayerSP {

    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"))
    private void onUpdateWalkingPlayer(CallbackInfo ci) {
        if (Claude.rotationManager == null || !Claude.rotationManager.isSilentActive()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        // Save current visual rotation
        Claude.rotationManager.setSavedRotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);

        // Swap to silent rotation so packet is built with it
        mc.thePlayer.rotationYaw = Claude.rotationManager.getSilentYaw();
        mc.thePlayer.rotationPitch = Claude.rotationManager.getSilentPitch();
        mc.thePlayer.rotationYawHead = Claude.rotationManager.getSilentYaw();
    }

    @Inject(method = "onUpdateWalkingPlayer", at = @At("RETURN"))
    private void onUpdateWalkingPlayerReturn(CallbackInfo ci) {
        if (Claude.rotationManager == null || !Claude.rotationManager.isSilentActive()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        // Restore visual rotation after packet sent
        mc.thePlayer.rotationYaw = Claude.rotationManager.getSavedYaw();
        mc.thePlayer.rotationPitch = Claude.rotationManager.getSavedPitch();
        mc.thePlayer.rotationYawHead = Claude.rotationManager.getSavedYaw();

        // Clear silent after packet sent — module will re-set next tick if still needed
        Claude.rotationManager.clearSilentRotation();
    }
}