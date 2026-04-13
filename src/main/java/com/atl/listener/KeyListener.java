package com.atl.listener;

import com.atl.module.ExampleMod;
import com.atl.module.management.Module;
import com.atl.module.modules.HUD;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class KeyListener {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        int key = Keyboard.getEventKey();
        boolean pressed = Keyboard.getEventKeyState();

        for (Module m : ExampleMod.moduleManager.getAll()) {
            // If module is "FreeLook" and we just released the key, turn it off
            if (m.getName().equalsIgnoreCase("FreeLook") && !pressed && m.getKeybind() == key) {
                m.setEnabled(false);
            }

            // Only toggle if the key is PRESSED (not released)
            if (m.getKeybind() == key && key != 0 && pressed) {
                m.toggle();

                // Invalidate HUD cache so the list updates on screen
                HUD hud = (HUD) ExampleMod.moduleManager.get("HUD");
                if (hud != null) {
                    hud.invalidateCache();
                }
            }
        }
    }
}