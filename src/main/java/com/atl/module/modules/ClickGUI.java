package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import com.atl.ui.clickgui.ClickGuiScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;

public class ClickGUI extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public final NumberSetting red = new NumberSetting("Red", 182, 0, 255, 1);
    public final NumberSetting green = new NumberSetting("Green", 0, 0, 255, 1);
    public final NumberSetting blue = new NumberSetting("Blue", 0, 0, 255, 1);

    public final NumberSetting textRed = new NumberSetting("Text Red", 0, 0, 255, 1);
    public final NumberSetting textGreen = new NumberSetting("Text Green", 0, 0, 255, 1);
    public final NumberSetting textBlue = new NumberSetting("Text Blue", 0, 0, 255, 1);

    public ClickGUI() {
        super("ClickGUI", "The visual interface to manage modules", Category.RENDER);
        this.setKeybind(Keyboard.KEY_RSHIFT);
        addSettings(red, green, blue, textRed, textGreen, textBlue);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            mc.displayGuiScreen(new ClickGuiScreen(this));
        }
    }

    public int getThemeColor() {
        return (255 << 24) | ((int)red.value << 16) | ((int)green.value << 8) | (int)blue.value;
    }
    
    public int getHeaderTextColor() {
        return (255 << 24) | ((int)textRed.value << 16) | ((int)textGreen.value << 8) | (int)textBlue.value;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("Red: " + (int)red.value, "Green: " + (int)green.value, "Blue: " + (int)blue.value);
    }
}
