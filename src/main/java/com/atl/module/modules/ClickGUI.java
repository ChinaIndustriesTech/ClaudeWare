package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.ui.clickgui.ClickGuiScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class ClickGUI extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public ClickGUI() {
        super("ClickGUI", "The visual interface to manage modules", Category.RENDER);
        // Standard default keybind for ClickGUIs
        this.setKeybind(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            mc.displayGuiScreen(new ClickGuiScreen(this));
        }
    }
}
