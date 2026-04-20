package com.atl.mixin;

import com.atl.module.Claude;
import com.atl.module.modules.AutoBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S0BPacketAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(method = "handleAnimation", at = @At("HEAD"))
    private void onHandleAnimation(S0BPacketAnimation packetIn, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        Entity entity = mc.theWorld.getEntityByID(packetIn.getEntityID());
        if (entity instanceof EntityPlayer && entity != mc.thePlayer) {
            AutoBlock autoBlock = (AutoBlock) Claude.moduleManager.get("AutoBlock");
            if (autoBlock != null && autoBlock.isEnabled()) {
                autoBlock.handleEntitySwing((EntityPlayer) entity);
            }
        }
    }
}
