package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class Sprint extends Module {

    public Sprint() {
        super("Sprint", "Automatically sprints while moving forward", Category.MOVEMENT);
        this.setEnabled(true);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();
        int sprintKey = mc.gameSettings.keyBindSprint.getKeyCode();
        if (Keyboard.isKeyDown(forwardKey)) {
            KeyBinding.setKeyBindState(sprintKey, true);
        }
    }
}
