package com.atl.listener;

import com.atl.module.Claude;
import com.atl.module.management.Module;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class KeyListener {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        int key = Keyboard.getEventKey();
        boolean pressed = Keyboard.getEventKeyState();

        for (Module m : Claude.moduleManager.getAll()) {
            if (m.getName().equalsIgnoreCase("FreeLook") && !pressed && m.getKeybind() == key) {
                m.setEnabled(false);
            }

            if (m.getKeybind() == key && key != 0 && pressed) {
                m.toggle();
            }
        }
    }
}