package com.atl.mixin;

import com.atl.ui.clickgui.ClickGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovementInputFromOptions;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementInputFromOptions.class)
public class MixinMovementInput {

    @Inject(method = "updatePlayerMoveState", at = @At("RETURN"))
    private void onUpdatePlayerMoveState(CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof ClickGuiScreen)) return;

        MovementInputFromOptions input = (MovementInputFromOptions)(Object) this;

        boolean forward = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
        boolean back    = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
        boolean left    = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
        boolean right   = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());

        input.moveForward = forward ? 1.0f : (back  ? -1.0f : 0.0f);
        input.moveStrafe  = left   ? 1.0f : (right ? -1.0f : 0.0f);
        input.jump        = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
        input.sneak       = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());

        if (input.sneak) {
            input.moveForward *= 0.3f;
            input.moveStrafe  *= 0.3f;
        }

        boolean sprinting = input.moveForward > 0 && !input.sneak
                && mc.thePlayer != null
                && !mc.thePlayer.isCollidedHorizontally;

        if (sprinting) {
            mc.thePlayer.setSprinting(true);
        }
    }
}