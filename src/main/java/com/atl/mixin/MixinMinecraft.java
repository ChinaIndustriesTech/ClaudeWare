package com.atl.mixin;

import com.atl.module.Claude;
import com.atl.module.management.Module;
import com.atl.module.modules.NoHitDelay;
import com.atl.ui.clickgui.FakeGuiInventory;
import com.atl.ui.clickgui.InventoryMoveReplayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "runTick", at = @At("TAIL"))
    private void onRunTickTail(CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        Module inventoryMove = Claude.moduleManager.get("InventoryMove");
        if (inventoryMove == null || !inventoryMove.isEnabled()) return;

        if (mc.currentScreen instanceof GuiInventory
                && !(mc.currentScreen instanceof FakeGuiInventory)
                && !InventoryMoveReplayer.isReplaying()) {
            mc.displayGuiScreen(new FakeGuiInventory(mc.thePlayer));
        }
    }

    @Shadow
    public int leftClickCounter;

    @Inject(method = "clickMouse", at = @At("HEAD"))
    private void onClickMouse(CallbackInfo ci) {
        NoHitDelay noHitDelay = (NoHitDelay) Claude.moduleManager.get("NoHitDelay");
        if (noHitDelay != null && noHitDelay.isEnabled()) {
            this.leftClickCounter = 0;
        }
    }
}