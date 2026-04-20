package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;

public class SprintReset extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private int resetTicks = 0;
    private EntityLivingBase lastTarget = null;
    private boolean awaitingDamage = false;

    private final NumberSetting tapDelay = new NumberSetting("Delay (Ticks)", 1, 1, 10, 1);
    private long lastTapTime = 0;

    public SprintReset() {
        super("SprintReset", "Accurate W-Tap only on successful damage", Category.MOVEMENT);
        addSettings(tapDelay);
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
                "Delay: " + (int) tapDelay.value + " ticks"
        );
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4) return false;
        if (parts[2].equalsIgnoreCase("delay")) {
            try {
                tapDelay.setValue(Double.parseDouble(parts[3]));
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!isEnabled() || event.entityPlayer != mc.thePlayer || event.target == null) return;
        if (mc.thePlayer.hurtTime > 0) return;
        if (System.currentTimeMillis() - lastTapTime < (long)tapDelay.value * 50) {
            return;
        }

        if (event.target instanceof EntityLivingBase) {
            this.lastTarget = (EntityLivingBase) event.target;
            this.awaitingDamage = true;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || event.phase != TickEvent.Phase.START) return;

        int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();

        if (awaitingDamage && lastTarget != null) {
            if (lastTarget.hurtTime > 0 && lastTarget.hurtTime <= 10) {
                if (Keyboard.isKeyDown(forwardKey)) {
                    resetTicks = 1;
                    lastTapTime = System.currentTimeMillis();
                }
                awaitingDamage = false;
                lastTarget = null;
            } else if (lastTarget.isDead || mc.thePlayer.getDistanceSqToEntity(lastTarget) > 36.0D) {
                awaitingDamage = false;
                lastTarget = null;
            }
        }

        if (resetTicks > 0) {
            KeyBinding.setKeyBindState(forwardKey, false);
            resetTicks--;
        } else {
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
        lastTapTime = 0;
        if (mc.thePlayer != null) {
            int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();
            KeyBinding.setKeyBindState(forwardKey, Keyboard.isKeyDown(forwardKey));
        }
    }
}
