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

    // Settings for Clicks Per Second

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
        // 1. Critical Fix: Check if module is enabled
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();

        // 2. Anti-Cheat Fix: Revert to ClientTick Phase.START to synchronize with the server heartbeat and avoid "Post" flags
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.currentScreen == null) {
            return;
        }

        // 3. Logic Fix: Don't autoclick while the ClickGUI is open
        if (mc.currentScreen instanceof ClickGuiScreen) return;

        // Only click if the user is holding down the Left Mouse Button (0), Shift is held, and the hotbar isn't full
        if (Mouse.isButtonDown(0) && GuiScreen.isShiftKeyDown() && !isHotbarFull()) {
            long currentTime = System.currentTimeMillis();

// Initialize the timer on the first press
            if (nextClickTime == 0) {
                nextClickTime = currentTime;
            }

            /*
             * Using a while loop allows the module to "catch up" if the target CPS
             * is higher than the game's 20Hz tick rate (50ms).
             */
            while (currentTime >= nextClickTime) {
                performClick();
                generateNextDelay();
            }
        } else {
            // Reset the timer when not clicking so the first click is responsive
            nextClickTime = 0;
        }
    }

    /**
     * Checks if all 9 slots of the hotbar are currently occupied.
     */
    private boolean isHotbarFull() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;

        for (int i = 0; i < 9; i++) {
            if (mc.thePlayer.inventory.mainInventory[i] == null) return false;
        }
        return true;
    }

    /**
     * Simulates a mouse click within the current GUI.
     */
    private void performClick() {
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen currentScreen = mc.currentScreen;
        if (currentScreen == null) return;

        // Calculate mouse coordinates scaled to the GUI resolution
        int mouseX = Mouse.getX() * currentScreen.width / mc.displayWidth;
        int mouseY = currentScreen.height - Mouse.getY() * currentScreen.height / mc.displayHeight - 1;

        try {
            // Access the protected mouseClicked method via our IGuiScreen invoker
            IGuiScreen invoker = (IGuiScreen) currentScreen;
            
            // 0 is the index for Left Click
            invoker.invokeMouseClicked(mouseX, mouseY, 0);
        } catch (Exception e) {
            // Log error if the Mixin fails to cast or invoke
            e.printStackTrace();
        }
    }

    /**
     * Generates a randomized delay until the next click.
     * 
     * Algorithm:
     * 1. Calculate base delay from CPS.
     * 2. Apply a Gaussian (Normal) distribution jitter to make timings less uniform.
     * 3. Occasionally simulate a "mis-click" or slight pause typical of human fatigue.
     */
    private void generateNextDelay() {
        double min = Math.min(minCPS.value, maxCPS.value);
        double max = Math.max(minCPS.value, maxCPS.value);

        // Ensure min doesn't exceed max
        if (min > max) min = max;

        // Randomize target CPS within range
        double targetCPS = min + (random.nextDouble() * (max - min));
        
        // Convert CPS to milliseconds (1000ms / CPS)
        long delay = (long) (1000.0 / targetCPS);

        // Add a human-like "Jitter"
        // Gaussian random gives us a bell curve: most delays stay near the target,
        // but some are significantly longer or shorter.
        double jitter = random.nextGaussian() * (delay * 0.10);
        delay += (long) jitter;

        // Human behavior: Occasionally pause slightly longer (fatigue/thinking)
        if (random.nextInt(100) == 0) {
            delay += random.nextInt(100) + 50;
        }

        this.nextClickTime += delay;
    }
}
