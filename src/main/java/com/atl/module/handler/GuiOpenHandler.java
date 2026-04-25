package com.atl.module.handler;

import com.atl.mixin.accessor.IGuiChat;
import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GuiOpenHandler {

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui != null && event.gui.getClass() == GuiChat.class && !(event.gui instanceof ChatGuiHandler)) {

            IGuiChat accessor = (IGuiChat) event.gui;
            String initialText = accessor.getDefaultInputFieldText();

            if (initialText == null) {
                initialText = "";
            }

            event.gui = new ChatGuiHandler(initialText);
        }
    }
}