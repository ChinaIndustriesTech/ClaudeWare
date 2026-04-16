package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class SprintReset extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private int tapTicks = -1;
    private EntityLivingBase lastTarget = null;

    public SprintReset() {
        super("SprintReset", "W-Taps automatically on hit for extra knockback", Category.MOVEMENT);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        // Ensure the attack is from us and we are currently moving forward
        if (!isEnabled() || event.entityPlayer != mc.thePlayer || event.target == null) return;

        // Store the target to check for I-frames in the next tick
        if (event.target instanceof EntityLivingBase) {
            this.lastTarget = (EntityLivingBase) event.target;
        }

        int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();
        if (Keyboard.isKeyDown(forwardKey)) {
            this.tapTicks = 1;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        if (event.phase == TickEvent.Phase.START) {
            int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();

            if (tapTicks == 1) {
                // Only reset sprint if the target actually took the hit (has I-frames)
                // We check for > 0 because hurtTime is set to 10 on a successful hit.
                if (lastTarget != null && lastTarget.hurtTime > 0) {
                    // Forcefully stop pressing W
                    KeyBinding.setKeyBindState(forwardKey, false);
                    tapTicks = 0;
                    lastTarget = null;
                } else if (lastTarget == null || mc.thePlayer.getDistanceSqToEntity(lastTarget) > 36.0D) {
                    // If the target is gone or too far, cancel the pending tap to preserve momentum
                    tapTicks = -1;
                    lastTarget = null;
                }
                // If lastTarget.hurtTime is still 0, we wait for the next tick to see if the hit confirms
            } else if (tapTicks == 0) {
                // Restore W if the physical key is still down
                KeyBinding.setKeyBindState(forwardKey, Keyboard.isKeyDown(forwardKey));
                tapTicks = -1;
            }
        }
    }

    @Override
    public void onDisable() {
        tapTicks = -1;
        lastTarget = null;
        if (mc.thePlayer != null) {
            int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();
            KeyBinding.setKeyBindState(forwardKey, Keyboard.isKeyDown(forwardKey));
        }
    }
}
