package com.atl.module.handler;

import com.atl.module.management.Module;
import com.atl.module.management.ModuleManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class KeybindHandler {

    private final ModuleManager moduleManager;

    public KeybindHandler(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        int key = Keyboard.getEventKey();

        if (net.minecraft.client.Minecraft.getMinecraft().currentScreen != null || key == 0) return;

        boolean isKeyDown = Keyboard.getEventKeyState();
        
        for (Module module : moduleManager.getAll()) {
            if (module.getKeybind() != 0 && module.getKeybind() == key) {
                handleInput(module, isKeyDown);
            }
        }
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        int button = Mouse.getEventButton();
        boolean isButtonDown = Mouse.getEventButtonState();

        if (button == -1 || net.minecraft.client.Minecraft.getMinecraft().currentScreen != null) return;

        int mouseBind = -(button + 1);

        for (Module module : moduleManager.getAll()) {
            if (module.getKeybind() == mouseBind) {
                handleInput(module, isButtonDown);
            }
        }
    }

    private void handleInput(Module module, boolean state) {
        if (module.isHoldModule()) {
            if (state && !module.isEnabled()) {
                module.setEnabled(true);
            } else if (!state && module.isEnabled()) {
                module.setEnabled(false);
            }
        } else if (state) {
            module.toggle();
        }
    }
}
