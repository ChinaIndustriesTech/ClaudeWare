package com.atl.module;

import com.atl.module.config.ConfigManager;
import com.atl.module.handler.ChatGuiHandler;
import com.atl.module.handler.CommandHandler;
import com.atl.module.handler.GuiOpenHandler;
import com.atl.module.handler.KeybindHandler;
import com.atl.module.management.ModuleManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "claude", name = "ClaudeUtils", version = "6.9", acceptedMinecraftVersions = "[1.8.9]")
public class ExampleMod {

    public static ModuleManager moduleManager;
    public static CommandHandler commandHandler;

    public static ConfigManager configManager;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        moduleManager  = new ModuleManager();
        commandHandler = new CommandHandler(moduleManager);
        configManager  = new ConfigManager(moduleManager);
        ChatGuiHandler.setCommandHandler(commandHandler);
        MinecraftForge.EVENT_BUS.register(commandHandler);
        MinecraftForge.EVENT_BUS.register(new KeybindHandler(moduleManager));
        MinecraftForge.EVENT_BUS.register(new GuiOpenHandler());
    }

}