package com.atl.module.modules;

import com.atl.module.management.BooleanSetting;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoClicker extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    
    private final NumberSetting targetCps = new NumberSetting("Average CPS", 10, 1, 20, 1);
    private final BooleanSetting butterfly = new BooleanSetting("Butterfly", false);
    private final BooleanSetting exhaustion = new BooleanSetting("Exhaustion", true);

    private long nextClickTime;
    private long sessionStartTime;
    private long exhaustionStartTime;
    private boolean isExhausted;
    private boolean isClicking;
    private boolean secondButterflyClick = false;
    private long lastFastDelay;
    private double currentFluctuation;

    public AutoClicker() {
        super("AutoClicker", "Automatically clicks while holding Left Click", Category.COMBAT);
        addSettings(targetCps, butterfly, exhaustion);
    }

    @Override
    public void loadSettings(JsonObject settings) {
        if (settings.has("cps")) targetCps.setValue(settings.get("cps").getAsDouble());
        if (settings.has("butterfly")) butterfly.enabled = settings.get("butterfly").getAsBoolean();
        if (settings.has("exhaustion")) exhaustion.enabled = settings.get("exhaustion").getAsBoolean();
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject settings = new JsonObject();
        settings.addProperty("cps", targetCps.value);
        settings.addProperty("butterfly", butterfly.enabled);
        settings.addProperty("exhaustion", exhaustion.enabled);
        return settings;
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 3) return false;
        String setting = parts[2].toLowerCase();
        if (setting.equals("cps")) {
            targetCps.setValue(Double.parseDouble(parts[3]));
            return true;
        }
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
            "cps (1-20) - Current: " + targetCps.value,
            "butterfly (true/false) - Current: " + butterfly.enabled,
            "exhaustion (true/false) - Current: " + exhaustion.enabled
        );
    }

    @Override
    public void onEnable() {
        sessionStartTime = System.currentTimeMillis();
        isExhausted = false;
        nextClickTime = 0;
        secondButterflyClick = false;
        currentFluctuation = 0;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        // Using RenderTickEvent for higher precision and smoother high-CPS clicking
        if (!isEnabled() || mc.thePlayer == null || mc.currentScreen != null || event.phase != TickEvent.Phase.START) {
            return;
        }

        // Only click if the physical Left Click (Button 0) is held down
        if (!Mouse.isButtonDown(0)) {
            sessionStartTime = System.currentTimeMillis();
            isExhausted = false;
            secondButterflyClick = false;
            releaseClick();
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Fatigue / Exhaustion Logic
        if (exhaustion.isEnabled()) {
            if (isExhausted) {
                if (now - exhaustionStartTime > 3000) { // 3 second pause
                    isExhausted = false;
                    sessionStartTime = now;
                }
                // We no longer return here; we allow clicking to continue at a reduced rate
            } else if (now - sessionStartTime > 10000) { // Exhaust after 10 seconds
                isExhausted = true;
                exhaustionStartTime = now;
            }
        }

        // 2. Click Pacing Logic
        if (now >= nextClickTime) {
            doClick();

            // Sync the timer if we are starting fresh or lagging significantly (>150ms)
            // This prevents the "burst" and ensures the clicker doesn't stall.
            if (nextClickTime == 0 || (now - nextClickTime) > 200) nextClickTime = now;

            generateNextDelay(nextClickTime);
        } else if (isClicking && now > (nextClickTime - getReleaseDelay())) {
            // Simulates a human-like hold time (duty cycle)
            releaseClick();
        }
    }

    private void doClick() {
        int key = mc.gameSettings.keyBindAttack.getKeyCode();
        KeyBinding.setKeyBindState(key, true);
        KeyBinding.onTick(key); // Triggers standard Minecraft attack logic
        isClicking = true;
    }

    private void releaseClick() {
        if (isClicking) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            isClicking = false;
        }
    }

    private void generateNextDelay(long referenceTime) {
        double avg = targetCps.value;

        // Apply exhaustion penalty: Reduce average CPS by 3
        if (isExhausted) {
            avg = Math.max(1.0, avg - 3.0);
        }

        // Ease into and out of spikes using an Exponential Moving Average (EMA)
        // We cube the Gaussian for high kurtosis, then blend it with the previous value.
        double gauss = ThreadLocalRandom.current().nextGaussian();
        double targetFluctuation = Math.pow(gauss, 3) * 3.0;

        // EMA Smoothing: 60% previous state, 40% new target. 
        // This creates "streaks" of speed rather than isolated sharp jumps.
        // We clamp the fluctuation to +/- 6 CPS as requested.
        currentFluctuation = Math.max(-6.0, Math.min(6.0, (currentFluctuation * 0.6) + (targetFluctuation * 0.4)));
        
        // Hard cap at 22 CPS and a higher safety floor of 4.0 CPS to prevent "stopping" feel.
        double actualCps = Math.min(22.0, Math.max(4.0, avg + currentFluctuation));

        long baseDelay = (long) (1000.0 / Math.max(1.0, actualCps));

        if (butterfly.isEnabled()) {
            if (secondButterflyClick) {
                nextClickTime = referenceTime + (baseDelay * 2) - lastFastDelay;
                secondButterflyClick = false;
            } else {
                // Rapid double-tap simulation (5ms to 15ms)
                lastFastDelay = ThreadLocalRandom.current().nextLong(5, 16);
                nextClickTime = referenceTime + lastFastDelay;
                secondButterflyClick = true;
            }
        } else {
            nextClickTime = referenceTime + baseDelay;
        }

        // Peaky Jitter: Using a higher power here also increases interval kurtosis
        double jitterGauss = ThreadLocalRandom.current().nextGaussian();
        // Toned down multiplier from 8.0 to 4.0 to prevent accidental long pauses.
        nextClickTime += (long) (Math.pow(jitterGauss, 3) * 4.0);
    }

    private long getReleaseDelay() {
        // Human-like button hold duration (10ms - 40ms)
        return ThreadLocalRandom.current().nextLong(10, 41);
    }
}
