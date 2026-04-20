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
    private int resetTicks = 0;
    private EntityLivingBase lastTarget = null;
    private boolean awaitingDamage = false;

    public SprintReset() {
        super("SprintReset", "Accurate W-Tap only on successful damage", Category.MOVEMENT);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!isEnabled() || event.entityPlayer != mc.thePlayer || event.target == null) return;

        // Don't prepare a reset if we are currently being hit
        if (mc.thePlayer.hurtTime > 0) return;

        if (event.target instanceof EntityLivingBase) {
            this.lastTarget = (EntityLivingBase) event.target;
            this.awaitingDamage = true;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || event.phase != TickEvent.Phase.START) return;

        int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();

        // 1. DAMAGE CONFIRMATION LOGIC
        if (awaitingDamage && lastTarget != null) {
            // Check if the target actually took damage (hurtTime starts at 10 and ticks down)
            if (lastTarget.hurtTime > 0 && lastTarget.hurtTime <= 10) {
                // SUCCESS: They took damage, now we can legit W-Tap
                if (Keyboard.isKeyDown(forwardKey)) {
                    resetTicks = 1;
                }
                awaitingDamage = false;
                lastTarget = null;
            } else if (lastTarget.isDead || mc.thePlayer.getDistanceSqToEntity(lastTarget) > 36.0D) {
                // FAILED: Target is gone or too far, cancel reset
                awaitingDamage = false;
                lastTarget = null;
            }
            // Otherwise, we keep awaitingDamage = true and check again next tick
        }

        // 2. PHYSICAL INPUT SIMULATION
        if (resetTicks > 0) {
            // Release W for this tick
            KeyBinding.setKeyBindState(forwardKey, false);
            resetTicks--;
        } else {
            // Ensure W state matches physical keyboard
            if (Keyboard.isKeyDown(forwardKey)) {
                KeyBinding.setKeyBindState(forwardKey, true);
            }
        }
    }

    @Override
    public void onDisable() {
        resetTicks = 0;
        awaitingDamage = false;
        lastTarget = null;
        if (mc.thePlayer != null) {
            int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();
            KeyBinding.setKeyBindState(forwardKey, Keyboard.isKeyDown(forwardKey));
        }
    }
}
