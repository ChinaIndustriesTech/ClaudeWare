package com.atl.module.modules;

import com.atl.mixin.accessor.IGuiScreen;
import com.atl.module.management.Category;
import com.atl.module.management.BooleanSetting;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.concurrent.ThreadLocalRandom;

public class InvClicker extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    public NumberSetting delay = new NumberSetting("Click Delay", 2, 0, 20, 1);
    public BooleanSetting randomize = new BooleanSetting("Randomize", true);
    private int ticks;
    private int currentTarget = 0;

    public InvClicker() {
        super("InvClicker", "Auto-clicks while holding mouse in menus", Category.MISC);
        addSettings(delay, randomize);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        // We only want to run logic once per tick, at the start
        if (!isEnabled() || mc.thePlayer == null || event.phase != TickEvent.Phase.START) {
            return;
        }

        // Check if we are currently looking at a container (Inventory, Chest, etc.)
        if (mc.currentScreen instanceof GuiContainer) {
            GuiContainer screen = (GuiContainer) mc.currentScreen;

            // Calculate mouse position relative to the GUI scale
            final int mouseX = Mouse.getX() * screen.width / mc.displayWidth;
            final int mouseY = screen.height - Mouse.getY() * screen.height / mc.displayHeight - 1;

            // Button 0 is Left Click
            if (Mouse.isButtonDown(0)) {
                ticks++;

                // If we don't have a target for this click cycle, set one
                if (currentTarget == 0) {
                    double base = delay.value;
                    currentTarget = randomize.enabled ? (int) (base + ThreadLocalRandom.current().nextInt(-1, 2)) : (int) base;
                }

                if (ticks >= currentTarget) {
                    // Use our Mixin Accessor to trigger the click
                    ((IGuiScreen) screen).invokeMouseClicked(mouseX, mouseY, 0);

                    // Reset for the next click
                    ticks = 0;
                    currentTarget = 0;
                }
            } else {
                // Reset if they let go of the mouse
                ticks = 0;
            }
        }
    }
}