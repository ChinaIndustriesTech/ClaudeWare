package com.atl.module.handler;

import com.atl.module.Claude;
import com.atl.module.management.Module;
import com.atl.module.management.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class CommandHandler {

    private static final String PREFIX = ",";
    private final ModuleManager moduleManager;
    private final Minecraft mc = Minecraft.getMinecraft();

    public CommandHandler(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
    }

    public boolean handleCommand(String message) {
        if (!message.startsWith(PREFIX)) return false;

        String[] parts = message.substring(PREFIX.length()).split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return false;

        String firstArg = parts[0].toLowerCase();

        switch (firstArg) {
            case "t":
            case "toggle" :
                handleToggle(parts);
                break;
            case "enable":
                handleEnable(parts);
                break;
            case "disable":
                handleDisable(parts);
                break;
            case "ls":
            case "list":
                handleList();
                break;
            case "h":
            case "help":
                handleHelp();
                break;
            case "s":
            case "set":
                handleSet(parts);
                break;
            case "b":
            case "bind":
                handleBind(parts);
                break;
            case "drawn":
                handleDrawn(parts);
                break;
            case "save":
                handleSave(parts);
                break;
            case "load":
                handleLoad(parts);
                break;
            case "configs":
                handleConfigs();
                break;
            default:
                Module module = moduleManager.get(firstArg);
                if (module != null) {
                    if (parts.length == 1) {
                        displayModuleSettings(module);
                    } else {
                        String[] newParts = new String[parts.length + 1];
                        newParts[0] = "set";
                        newParts[1] = firstArg;
                        System.arraycopy(parts, 1, newParts, 2, parts.length - 1);
                        handleSet(newParts);
                    }
                } else {
                    sendMessage(EnumChatFormatting.RED + "Unknown command or module. Type " + PREFIX + "help for a list.");
                }
        }
        return true;
    }

    private void handleToggle(String[] parts) {
        if (parts.length < 2) {
            sendMessage(EnumChatFormatting.RED + "Usage: " + PREFIX + "toggle <module>");
            return;
        }
        Module module = moduleManager.get(parts[1]);
        if (module == null) {
            sendMessage(EnumChatFormatting.RED + "Module not found: " + parts[1]);
            return;
        }
        module.toggle();
        String state = module.isEnabled()
                ? EnumChatFormatting.GREEN + "enabled"
                : EnumChatFormatting.RED + "disabled";
        sendMessage(EnumChatFormatting.GRAY + module.getName() + " " + state);
    }

    private void handleDrawn(String[] parts) {
        if (parts.length < 2) {
            sendMessage(EnumChatFormatting.RED + "Usage: " + PREFIX + "drawn <module>");
            return;
        }
        Module module = moduleManager.get(parts[1]);
        if (module == null) {
            sendMessage(EnumChatFormatting.RED + "Module not found: " + parts[1]);
            return;
        }
        module.setDrawn(!module.isDrawn());
        String state = module.isDrawn() ? EnumChatFormatting.GREEN + "shown" : EnumChatFormatting.RED + "hidden";
        sendMessage(EnumChatFormatting.GRAY + module.getName() + " is now " + state + 
                EnumChatFormatting.GRAY + " on the HUD.");
    }

    private void handleSet(String[] parts) {
        if (parts.length < 3) {
            Module module = moduleManager.get(parts[1]);
            if (module != null) {
                displayModuleSettings(module);
            } else {
                sendMessage(EnumChatFormatting.RED + "Usage: " + PREFIX + "set <module> <setting> <value>");
            }
            return;
        }
        Module module = moduleManager.get(parts[1]);
        if (module == null) {
            sendMessage(EnumChatFormatting.RED + "Module not found: " + parts[1]);
            return;
        }
        
        if (module.handleSetCommand(parts)) {
            sendMessage(EnumChatFormatting.GREEN + "Set " + parts[1] + " setting.");
        } else {
            sendMessage(EnumChatFormatting.RED + "Failed to set value. Check setting name.");
        }
    }

    private void displayModuleSettings(Module module) {
        sendMessage(EnumChatFormatting.GRAY + "--- " + module.getName() + " Settings ---");
        List<String> settings = module.getSettings();
        if (settings.isEmpty()) {
            sendMessage(EnumChatFormatting.WHITE + "No configurable settings found.");
        } else {
            for (String setting : settings) {
                sendMessage(EnumChatFormatting.WHITE + "- " + setting);
            }
        }
        sendMessage(EnumChatFormatting.AQUA + "Usage: " + PREFIX + module.getName().toLowerCase() + " <setting> <value>");
    }

    private void handleSave(String[] parts) {
        if (parts.length < 2) {
            sendMessage(EnumChatFormatting.RED + "Usage: " + PREFIX + "save <name>");
            return;
        }
        boolean success = Claude.configManager.save(parts[1]);
        if (success) {
            sendMessage(EnumChatFormatting.GREEN + "Config saved: " + parts[1]);
        } else {
            sendMessage(EnumChatFormatting.RED + "Failed to save config: " + parts[1]);
        }
    }

    private void handleLoad(String[] parts) {
        if (parts.length < 2) {
            sendMessage(EnumChatFormatting.RED + "Usage: " + PREFIX + "load <name>");
            return;
        }
        boolean success = Claude.configManager.load(parts[1]);
        if (success) {
            sendMessage(EnumChatFormatting.GREEN + "Config loaded: " + parts[1]);
        } else {
            sendMessage(EnumChatFormatting.RED + "Config not found: " + parts[1]);
        }
    }

    private void handleConfigs() {
        String[] configs = Claude.configManager.listConfigs();
        if (configs.length == 0) {
            sendMessage(EnumChatFormatting.YELLOW + "No configs saved yet.");
            return;
        }
        sendMessage(EnumChatFormatting.GRAY + "--- Configs ---");
        for (String config : configs) {
            sendMessage(EnumChatFormatting.WHITE + config);
        }
    }

    private void handleEnable(String[] parts) {
        if (parts.length < 2) {
            sendMessage(EnumChatFormatting.RED + "Usage: " + PREFIX + "enable <module>");
            return;
        }
        Module module = moduleManager.get(parts[1]);
        if (module == null) {
            sendMessage(EnumChatFormatting.RED + "Module not found: " + parts[1]);
            return;
        }
        if (module.isEnabled()) {
            sendMessage(EnumChatFormatting.YELLOW + module.getName() + " is already enabled.");
            return;
        }
        module.setEnabled(true);
        sendMessage(EnumChatFormatting.GREEN + module.getName() + " enabled.");
    }

    private void handleDisable(String[] parts) {
        if (parts.length < 2) {
            sendMessage(EnumChatFormatting.RED + "Usage: " + PREFIX + "disable <module>");
            return;
        }
        Module module = moduleManager.get(parts[1]);
        if (module == null) {
            sendMessage(EnumChatFormatting.RED + "Module not found: " + parts[1]);
            return;
        }
        if (!module.isEnabled()) {
            sendMessage(EnumChatFormatting.YELLOW + module.getName() + " is already disabled.");
            return;
        }
        module.setEnabled(false);
        sendMessage(EnumChatFormatting.RED + module.getName() + " disabled.");
    }

    private void handleList() {
        sendMessage(EnumChatFormatting.GRAY + "--- Modules ---");
        for (Module m : moduleManager.getAll()) {
            String state = m.isEnabled()
                    ? EnumChatFormatting.GREEN + "[ON]"
                    : EnumChatFormatting.RED + "[OFF]";
            sendMessage(state + EnumChatFormatting.WHITE + " " + m.getName()
                    + EnumChatFormatting.DARK_GRAY + " - " + m.getDescription());
        }
    }

    private void handleBind(String[] parts) {
        if (parts.length < 3) {
            sendMessage(EnumChatFormatting.RED + "Usage: " + PREFIX + "bind <module> <key/none>");
            return;
        }
        Module module = moduleManager.get(parts[1]);
        if (module == null) {
            sendMessage(EnumChatFormatting.RED + "Module not found: " + parts[1]);
            return;
        }
        
        int key = parseKeybind(parts[2]);
        if (key == 0 && !parts[2].equalsIgnoreCase("none") && !parts[2].equals("0")) {
            sendMessage(EnumChatFormatting.RED + "Invalid key: " + parts[2]);
            return;
        }

        if (key == 0) {
            module.setKeybind(0);
            sendMessage(EnumChatFormatting.RED + "Unbound " + module.getName());
            return;
        }

        module.setKeybind(key);
        sendMessage(EnumChatFormatting.GREEN + "Bound " + module.getName() + " to " + parts[2].toUpperCase());
    }
    private int parseKeybind(String input) {
        String s = input.toLowerCase();

        if (s.equals("alt") || s.equals("lalt")) return Keyboard.KEY_LMENU;
        if (s.equals("ralt")) return Keyboard.KEY_RMENU;
        if (s.equals("ctrl") || s.equals("lctrl")) return Keyboard.KEY_LCONTROL;
        if (s.equals("rctrl")) return Keyboard.KEY_RCONTROL;
        if (s.equals("shift") || s.equals("lshift")) return Keyboard.KEY_LSHIFT;
        if (s.equals("rshift")) return Keyboard.KEY_RSHIFT;
        if (s.equals("escape") || s.equals("esc")) return Keyboard.KEY_ESCAPE;
        if (s.equals("tab")) return Keyboard.KEY_TAB;
        if (s.equals("space")) return Keyboard.KEY_SPACE;
        if (s.equals("back")) return Keyboard.KEY_BACK;
        if (s.equals("delete") || s.equals("del")) return Keyboard.KEY_DELETE;
        if (s.equals("insert") || s.equals("ins")) return Keyboard.KEY_INSERT;
        if (s.equals("home")) return Keyboard.KEY_HOME;
        if (s.equals("end")) return Keyboard.KEY_END;
        if (s.equals("pgup") || s.equals("pageup")) return Keyboard.KEY_PRIOR;
        if (s.equals("pgdn") || s.equals("pagedown")) return Keyboard.KEY_NEXT;

        if (s.startsWith("mouse")) {
            try {
                return -Integer.parseInt(s.substring(5));
            } catch (NumberFormatException e) { return 0; }
        }

        int keyIndex = Keyboard.getKeyIndex(s.toUpperCase());
        return keyIndex != Keyboard.KEY_NONE ? keyIndex : 0;
    }

    private void handleHelp() {
        sendMessage(EnumChatFormatting.GRAY + "--- Commands ---");
        sendMessage(EnumChatFormatting.AQUA + PREFIX + "t/toggle <module>" + EnumChatFormatting.WHITE + "  Toggle a module on/off");
        sendMessage(EnumChatFormatting.AQUA + PREFIX + "enable <module>" + EnumChatFormatting.WHITE + "  Enable a module");
        sendMessage(EnumChatFormatting.AQUA + PREFIX + "disable <module>" + EnumChatFormatting.WHITE + "  Disable a module");
        sendMessage(EnumChatFormatting.AQUA + PREFIX + "ls/list" + EnumChatFormatting.WHITE + "  List all modules and their state");
        sendMessage(EnumChatFormatting.AQUA + PREFIX + "h/help" + EnumChatFormatting.WHITE + "  Show this help message");
        sendMessage(EnumChatFormatting.AQUA + PREFIX + "<module> <setting> <value>" + EnumChatFormatting.WHITE + "  Set a module setting");
        sendMessage(EnumChatFormatting.AQUA + PREFIX + "drawn <module>" + EnumChatFormatting.WHITE + "  Toggle visibility on the HUD");
        sendMessage(EnumChatFormatting.AQUA + PREFIX + "b/bind <module> <key/none>" + EnumChatFormatting.WHITE + "  Bind a module to a key");
        sendMessage(EnumChatFormatting.AQUA + PREFIX + "save <name>" + EnumChatFormatting.WHITE + "  Save current config");
        sendMessage(EnumChatFormatting.AQUA + PREFIX + "load <name>" + EnumChatFormatting.WHITE + "  Load a config");
        sendMessage(EnumChatFormatting.AQUA + PREFIX + "configs"     + EnumChatFormatting.WHITE + "  List all saved configs");
    }

    private void sendMessage(String text) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(text));
        }
    }
}
