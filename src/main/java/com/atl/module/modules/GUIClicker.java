package com.atl.module.modules;

import com.atl.mixin.accessor.IGuiScreen;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import com.atl.ui.clickgui.ClickGuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Random;

/**
 * GUIClicker Module
 * Automatically performs mouse clicks while a GUI (like a chest or inventory) is open.
 * Designed for Minecraft 1.8.9.
 */
public class GUIClicker extends Module {

    private final NumberSetting minCPS = new NumberSetting("Min CPS", 8.0, 1.0, 30.0, 0.5);
    private final NumberSetting maxCPS = new NumberSetting("Max CPS", 12.0, 1.0, 30.0, 0.5);

    private final Random random = new Random();
    private long nextClickTime;

    public GUIClicker() {
        super("GUIClicker", "Automatically clicks in GUIs with human randomization", Category.MISC);
        addSettings(minCPS, maxCPS);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();

        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.currentScreen == null) {
            return;
        }

        if (mc.currentScreen instanceof ClickGuiScreen) return;

        if (Mouse.isButtonDown(0) && GuiScreen.isShiftKeyDown() && !isHotbarFull()) {
            long currentTime = System.currentTimeMillis();
            if (nextClickTime == 0) {
                nextClickTime = currentTime;
            }

            while (currentTime >= nextClickTime) {
                performClick();
                generateNextDelay();
            }
        } else {
            nextClickTime = 0;
        }
    }

    private boolean isHotbarFull() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;

        for (int i = 0; i < 9; i++) {
            if (mc.thePlayer.inventory.mainInventory[i] == null) return false;
        }
        return true;
    }

    private void performClick() {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen currentScreen = mc.currentScreen;
        if (currentScreen == null) return;

        int mouseX = Mouse.getX() * currentScreen.width / mc.displayWidth;
        int mouseY = currentScreen.height - Mouse.getY() * currentScreen.height / mc.displayHeight - 1;

        try {
            IGuiScreen invoker = (IGuiScreen) currentScreen;

            invoker.invokeMouseClicked(mouseX, mouseY, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateNextDelay() {
        double min = Math.min(minCPS.value, maxCPS.value);
        double max = Math.max(minCPS.value, maxCPS.value);


        if (min > max) min = max;
        double targetCPS = min + (random.nextDouble() * (max - min));
        long delay = (long) (1000.0 / targetCPS);
        double jitter = random.nextGaussian() * (delay * 0.10);
        delay += (long) jitter;
        if (random.nextInt(100) == 0) {
            delay += random.nextInt(100) + 50;
        }

        this.nextClickTime += delay;
    }
}
