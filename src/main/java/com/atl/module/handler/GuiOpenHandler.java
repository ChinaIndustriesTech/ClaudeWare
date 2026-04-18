package com.atl.module.handler;

import com.atl.mixin.accessor.IGuiChat;
import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GuiOpenHandler {

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // This is safer than instanceof and prevents recursive loops or conflicts with other mods.
        if (event.gui != null && event.gui.getClass() == GuiChat.class && !(event.gui instanceof ChatGuiHandler)) {

            // Safely capture the text (like the "/") using the Mixin accessor
            IGuiChat accessor = (IGuiChat) event.gui;
            String initialText = accessor.getDefaultInputFieldText();

            if (initialText == null) {
                initialText = "";
            }
            
            // Replace with our custom handler while preserving the text
            event.gui = new ChatGuiHandler(initialText);
        }
    }
}