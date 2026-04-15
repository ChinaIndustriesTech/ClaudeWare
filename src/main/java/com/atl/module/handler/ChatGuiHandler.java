package com.atl.module.handler;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.gui.GuiChat;

public class ChatGuiHandler extends GuiChat {

    private static CommandHandler commandHandler;

    public static void setCommandHandler(CommandHandler handler) {
        commandHandler = handler;
    }

    @Override
    public void onGuiClosed() {
        // not used
    }

    // Override sendChatMessage to intercept outgoing chat
    @Override
    public void sendChatMessage(String msg) {
        if (commandHandler != null && commandHandler.handleCommand(msg)) {
            return; // swallow the message, don't send to server
        }
        super.sendChatMessage(msg);
    }
}