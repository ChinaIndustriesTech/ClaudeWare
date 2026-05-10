package com.atl.module.modules;

import com.atl.module.management.*;
import com.atl.ui.clickgui.ClickGuiScreen;
import com.atl.ui.clickgui.SidebarGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;

public class ClickGUI extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public Category lastCategory = Category.COMBAT;

    public final ModeSetting style = new ModeSetting("Style", "Modern", "Classic", "Modern");

    public final NumberSetting red = new NumberSetting("Red", 182, 0, 255, 1);
    public final NumberSetting green = new NumberSetting("Green", 0, 0, 255, 1);
    public final NumberSetting blue = new NumberSetting("Blue", 0, 0, 255, 1);

    public final NumberSetting textRed = new NumberSetting("Text Red", 0, 0, 255, 1);
    public final NumberSetting textGreen = new NumberSetting("Text Green", 0, 0, 255, 1);
    public final NumberSetting textBlue = new NumberSetting("Text Blue", 0, 0, 255, 1);
    public final BooleanSetting fadeAnimation = new BooleanSetting("Fade Animation", true);

    public ClickGUI() {
        super("ClickGUI", "The visual interface to manage modules", Category.RENDER);
        this.setKeybind(Keyboard.KEY_RSHIFT);
        addSettings(style, red, green, blue, textRed, textGreen, textBlue, fadeAnimation);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || !(mc.currentScreen instanceof ClickGuiScreen || mc.currentScreen instanceof SidebarGuiScreen)) return;

        sync(mc.gameSettings.keyBindForward);
        sync(mc.gameSettings.keyBindBack);
        sync(mc.gameSettings.keyBindLeft);
        sync(mc.gameSettings.keyBindRight);
        sync(mc.gameSettings.keyBindJump);
        sync(mc.gameSettings.keyBindSneak);
        sync(mc.gameSettings.keyBindSprint);
    }

    private void sync(KeyBinding bind) {
        KeyBinding.setKeyBindState(bind.getKeyCode(), Keyboard.isKeyDown(bind.getKeyCode()));
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            if (style.getValue().equals("Modern")) {
                mc.displayGuiScreen(new SidebarGuiScreen(this));
            } else {
                mc.displayGuiScreen(new ClickGuiScreen(this));
            }
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
        return Arrays.asList("Style: " + style.getValue(), "Red: " + (int)red.value, "Green: " + (int)green.value, "Blue: " + (int)blue.value);
    }
}