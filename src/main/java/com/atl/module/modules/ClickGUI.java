package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import com.atl.ui.clickgui.ClickGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || !(mc.currentScreen instanceof ClickGuiScreen)) return;

        boolean forward = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
        boolean back    = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
        boolean left    = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
        boolean right   = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
        boolean jump    = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
        boolean sneak   = Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());

        mc.thePlayer.movementInput.moveForward = forward ? 1.0f : (back  ? -1.0f : 0.0f);
        mc.thePlayer.movementInput.moveStrafe  = left   ? 1.0f : (right ? -1.0f : 0.0f);
        mc.thePlayer.movementInput.jump        = jump;
        mc.thePlayer.movementInput.sneak       = sneak;
        mc.thePlayer.setSprinting(Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()) && forward);
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