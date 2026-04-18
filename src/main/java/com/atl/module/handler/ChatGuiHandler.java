package com.atl.module.handler;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;

public class ChatGuiHandler extends GuiChat {

    private static CommandHandler commandHandler;

    public ChatGuiHandler(String initialText) {
        // This 'super' call is what tells Minecraft to type the "/" for you
        super(initialText);
    }

    public static void setCommandHandler(CommandHandler handler) {
        commandHandler = handler;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (this.mc.ingameGUI != null && this.mc.ingameGUI.getChatGUI() != null) {
            this.mc.ingameGUI.getChatGUI().resetScroll();
        }
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