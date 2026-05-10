package com.atl.module.modules;

import com.atl.mixin.IMinecraft;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.List;

public class Timer extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();

    private final NumberSetting speed = new NumberSetting("Speed", 1.0, 0.01, 5.0, 0.01);

    public Timer() {
        super("Timer", "Modifies game timer speed", Category.MOVEMENT);
        addSettings(speed);
    }

    @Override
    public void onDisable() {
        resetTimer();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled()) {
            resetTimer();
            return;
        }
        
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) {
            resetTimer();
            return;
        }

        net.minecraft.util.Timer timer = ((IMinecraft) mc).getTimer();
        if (timer != null) {
            timer.timerSpeed = (float) speed.value;
        }
    }

    private void resetTimer() {
        if (mc == null) return;
        net.minecraft.util.Timer timer = ((IMinecraft) mc).getTimer();
        if (timer != null) {
            timer.timerSpeed = 1.0f;
        }
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("Speed: " + String.format("%.1f", speed.value) + "x");
    }
}
