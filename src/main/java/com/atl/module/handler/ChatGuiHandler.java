package com.atl.module.handler;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;

public class ChatGuiHandler extends GuiChat {

    private static CommandHandler commandHandler;

    public ChatGuiHandler(String initialText) {
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

    @Override
    public void sendChatMessage(String msg) {
        if (commandHandler != null && commandHandler.handleCommand(msg)) {
            return;
        }
        super.sendChatMessage(msg);
    }
}