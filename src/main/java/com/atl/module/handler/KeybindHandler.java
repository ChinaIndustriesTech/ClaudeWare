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
        
        // Ignore if typing in chat or a GUI is open
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

        // Ignore if no button state changed or GUI is open
        if (button == -1 || net.minecraft.client.Minecraft.getMinecraft().currentScreen != null) return;

        // Map mouse buttons to negative IDs: Mouse0 -> -1, Mouse1 -> -2, etc.
        int mouseBind = -(button + 1);

        for (Module module : moduleManager.getAll()) {
            if (module.getKeybind() == mouseBind) {
                handleInput(module, isButtonDown);
            }
        }
    }

    private void handleInput(Module module, boolean state) {
        if (module.isHoldModule()) {
            // For hold modules, enable on press, disable on release
            if (state && !module.isEnabled()) {
                module.setEnabled(true);
            } else if (!state && module.isEnabled()) {
                module.setEnabled(false);
            }
        } else if (state) {
            // Regular modules only toggle on the initial press
            module.toggle();
        }
    }
}
