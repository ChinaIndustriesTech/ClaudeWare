package com.atl.module.modules;

import com.atl.module.management.BooleanSetting;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class AutoClicker extends Module {

    private static class ClickData {
        long interval;
        long gap;

        ClickData(long interval, long gap) { this.interval = interval; this.gap = gap; }
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    
    private final NumberSetting targetCps = new NumberSetting("Average CPS", 10, 1, 20, 1);
    private final BooleanSetting butterfly = new BooleanSetting("Butterfly", false);
    private final BooleanSetting exhaustion = new BooleanSetting("Exhaustion", true);
    private final BooleanSetting recorded = new BooleanSetting("Recorded", false);

    private long nextClickTime;
    private long sessionStartTime;
    private long exhaustionStartTime;
    private boolean isExhausted;
    private boolean isClicking;
    private boolean secondButterflyClick = false;
    private long lastFastDelay;
    private double currentFluctuation;

    // Playback logic
    private final List<ClickData> recordedDelays = new ArrayList<>();
    private int currentRecordIndex = 0;
    private String currentRecordingName = "none";
    private long currentRecordedGap = 0;

    public AutoClicker() {
        super("AutoClicker", "Automatically clicks while holding Left Click", Category.COMBAT);
        addSettings(targetCps, butterfly, exhaustion, recorded);
    }

    @Override
    public void loadSettings(JsonObject settings) {
        if (settings.has("cps")) targetCps.setValue(settings.get("cps").getAsDouble());
        if (settings.has("butterfly")) butterfly.enabled = settings.get("butterfly").getAsBoolean();
        if (settings.has("exhaustion")) exhaustion.enabled = settings.get("exhaustion").getAsBoolean();
        if (settings.has("recorded")) recorded.enabled = settings.get("recorded").getAsBoolean();
        if (settings.has("recordingName")) {
            currentRecordingName = settings.get("recordingName").getAsString();
            loadRecording(currentRecordingName);
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject settings = new JsonObject();
        settings.addProperty("cps", targetCps.value);
        settings.addProperty("butterfly", butterfly.enabled);
        settings.addProperty("exhaustion", exhaustion.enabled);
        settings.addProperty("recorded", recorded.enabled);
        settings.addProperty("recordingName", currentRecordingName);
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
        if (setting.equals("record")) {
            if (parts.length < 4) return false;
            currentRecordingName = parts[3];
            loadRecording(currentRecordingName);
            return true;
        }
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
            "cps (1-20) - Current: " + targetCps.value,
            "butterfly (true/false) - Current: " + butterfly.enabled,
            "exhaustion (true/false) - Current: " + exhaustion.enabled,
            "recorded (true/false) - Current: " + recorded.enabled,
            "record [name] - File: " + currentRecordingName
        );
    }

    @Override
    public void onEnable() {
        sessionStartTime = System.currentTimeMillis();
        isExhausted = false;
        nextClickTime = 0;
        secondButterflyClick = false;
        currentFluctuation = 0;
        
        if (recorded.isEnabled() && !recordedDelays.isEmpty()) {
            currentRecordIndex = ThreadLocalRandom.current().nextInt(recordedDelays.size());
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        // Using RenderTickEvent for higher precision and smoother high-CPS clicking
        if (!isEnabled() || mc.thePlayer == null || mc.currentScreen != null || event.phase != TickEvent.Phase.START) {
            return;
        }

        // Check if we are hovering over a block
        boolean overBlock = mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;

        // Only click if the physical Left Click (Button 0) is held down AND we aren't looking at a block
        if (!Mouse.isButtonDown(0) || overBlock) {
            sessionStartTime = System.currentTimeMillis();
            isExhausted = false;
            secondButterflyClick = false;
            
            // Pick a random starting point for the next session
            if (recorded.isEnabled() && !recordedDelays.isEmpty()) {
                currentRecordIndex = ThreadLocalRandom.current().nextInt(recordedDelays.size());
            }
            
            releaseClick();
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Fatigue / Exhaustion Logic
        if (exhaustion.isEnabled()) {
            if (isExhausted) {
                if (now - exhaustionStartTime > 1500) { // 1.5 second pause
                    isExhausted = false;
                    sessionStartTime = now;
                }
                // We no longer return here; we allow clicking to continue at a reduced rate
            } else if (now - sessionStartTime > 8000) { // Exhaust after 8 seconds (higher frequency)
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

    private void loadRecording(String name) {
        recordedDelays.clear();
        File dir = new File(mc.mcDataDir, "atl" + File.separator + "recordings");
        File file = new File(dir, name + ".txt");
        
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    if (line.contains(":")) {
                        String[] split = line.split(":");
                        recordedDelays.add(new ClickData(Long.parseLong(split[0]), Long.parseLong(split[1])));
                    } else {
                        // Backward compatibility for old recordings
                        recordedDelays.add(new ClickData(Long.parseLong(line.trim()), 30));
                    }
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateNextDelay(long referenceTime) {
        if (recorded.isEnabled() && !recordedDelays.isEmpty()) {
            ClickData data = recordedDelays.get(currentRecordIndex);
            
            nextClickTime = referenceTime + data.interval;
            currentRecordedGap = data.gap;
            
            currentRecordIndex++;
            // If we hit the end, pick a random spot to resume
            if (currentRecordIndex >= recordedDelays.size()) {
                currentRecordIndex = ThreadLocalRandom.current().nextInt(recordedDelays.size());
            }
            return;
        }

        double avg = targetCps.value;

        // Apply exhaustion penalty: Reduce average CPS by 3
        if (isExhausted) {
            avg = Math.max(1.0, avg - 3.0);
        }

        // Ease into and out of spikes using an Exponential Moving Average (EMA)
        // We cube the Gaussian for high kurtosis, then blend it with the previous value.
        double gauss = ThreadLocalRandom.current().nextGaussian();
        double targetFluctuation = Math.pow(gauss, 3) * 5.0; // Increased multiplier for wider CPS swings

        // EMA Smoothing: 50% previous state, 50% new target. 
        // This creates "streaks" of speed rather than isolated sharp jumps.
        // Widened the clamp to +/- 8.0 CPS to allow for more significant "speed-ups" and "slow-downs".
        currentFluctuation = Math.max(-8.0, Math.min(8.0, (currentFluctuation * 0.5) + (targetFluctuation * 0.5)));
        
        // Hard cap at 22 CPS and a higher safety floor of 5.0 CPS as requested.
        double actualCps = Math.min(22.0, Math.max(5.0, avg + currentFluctuation));

        // Easing logic: Cap the randomized target at 15 CPS for the first 2 seconds of clicking
        long elapsed = System.currentTimeMillis() - sessionStartTime;
        if (elapsed < 2000) {
            actualCps = Math.min(actualCps, 15.0);
        }

        long baseDelay = (long) (1000.0 / Math.max(1.0, actualCps));

        // Disable butterfly double-clicks during the ease-in period to stay under the 15 CPS limit
        if (butterfly.isEnabled() && elapsed >= 2000) {
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
        // Raising the power to 5 ensures that the "center" remains tight, while the "tails" 
        // (outliers) become significantly more common and pronounced.
        double jitterGauss = ThreadLocalRandom.current().nextGaussian();
        // Increased multiplier to 12.0 to create large, human-like timing inconsistencies.
        nextClickTime += (long) (Math.pow(jitterGauss, 5) * 12.0);

        // Hard clamp to ensure the outlier never results in an interval longer than 200ms (5 CPS)
        if (nextClickTime > referenceTime + 200) {
            nextClickTime = referenceTime + 200;
        }
    }

    private long getReleaseDelay() {
        if (recorded.isEnabled() && !recordedDelays.isEmpty()) {
            return currentRecordedGap;
        }
        // Default human-like gap (10ms - 40ms)
        return ThreadLocalRandom.current().nextLong(10, 41);
    }
}
