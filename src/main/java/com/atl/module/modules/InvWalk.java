package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class InvWalk extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public InvWalk() {
        super("InvWalk", "Allows you to move while in inventories", Category.MOVEMENT);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || event.phase != TickEvent.Phase.START) return;

        // We check if a screen is open, but ensure it's not the Chat GUI.
        // Moving while typing in chat is usually a bad idea and can lead to bans.
        if (mc.currentScreen != null && !(mc.currentScreen instanceof GuiChat)) {
            
            KeyBinding[] keys = {
                    mc.gameSettings.keyBindForward,
                    mc.gameSettings.keyBindBack,
                    mc.gameSettings.keyBindLeft,
                    mc.gameSettings.keyBindRight,
                    mc.gameSettings.keyBindJump,
                    mc.gameSettings.keyBindSprint
            };

            for (KeyBinding key : keys) {
                // Set the key state based on whether the physical key is actually pressed down
                KeyBinding.setKeyBindState(key.getKeyCode(), Keyboard.isKeyDown(key.getKeyCode()));
            }
        }
    }
}