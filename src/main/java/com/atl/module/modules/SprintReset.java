package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class SprintReset extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private int tapTicks = -1;

    public SprintReset() {
        super("SprintReset", "W-Taps automatically on hit for extra knockback", Category.MOVEMENT);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        // Ensure the attack is from us and we are currently moving forward
        if (!isEnabled() || event.entityPlayer != mc.thePlayer || event.target == null) return;

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
                // Forcefully stop pressing W
                KeyBinding.setKeyBindState(forwardKey, false);
                tapTicks = 0;
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
        if (mc.thePlayer != null) {
            int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();
            KeyBinding.setKeyBindState(forwardKey, Keyboard.isKeyDown(forwardKey));
        }
    }
}
