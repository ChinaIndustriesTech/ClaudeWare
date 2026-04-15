package com.atl.module.handler;

import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class GuiOpenHandler {

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui instanceof GuiChat && !(event.gui instanceof ChatGuiHandler)) {
            event.gui = new ChatGuiHandler();
        }
    }
}