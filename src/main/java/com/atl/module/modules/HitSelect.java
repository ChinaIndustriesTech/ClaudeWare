package com.atl.module.modules;

import com.atl.module.management.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class HitSelect extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // -------------------------------------------------------------------------
    // Settings
    // -------------------------------------------------------------------------

    private final NumberSetting pauseDuration = new NumberSetting(
            "Pause Duration", 150, 50, 500, 10
    );

    private final NumberSetting waitForFirstHit = new NumberSetting(
            "Wait For First Hit", 0, 0, 5000, 100
    );

    private final NumberSetting cancelRate = new NumberSetting(
            "Cancel Rate", 0, 0, 100, 1
    );

    private final NumberSetting inCombatRate = new NumberSetting(
            "In Combat", 100, 0, 100, 1
    );

    private final NumberSetting missedSwingsRate = new NumberSetting(
            "Missed Swings", 50, 0, 100, 1
    );

    // -------------------------------------------------------------------------
    // Internal state
    // -------------------------------------------------------------------------

    /** Independent RNG per cancel category — no correlated rolls. */
    private final Random baseRng   = new Random();
    private final Random combatRng = new Random();
    private final Random missedRng = new Random();

    /** Timestamp after which clicks are unblocked. */
    private long pauseUntil = 0L;

    /** State machine for wait-for-first-hit. */
    private enum WaitState { IDLE, WAITING, ACTIVE, TIMED_OUT }
    private WaitState waitState   = WaitState.IDLE;
    private long      waitStartMs = 0L;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public HitSelect() {
        super("HitSelect", "Filters clicks for better timing and lower average CPS.", Category.COMBAT);
        addSettings(pauseDuration, waitForFirstHit, cancelRate, inCombatRate, missedSwingsRate);
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4) return false;
        switch (parts[2].toLowerCase()) {
            case "pauseduration":
                pauseDuration.setValue(Double.parseDouble(parts[3])); return true;
            case "waitforfirsthit":
                waitForFirstHit.setValue(Double.parseDouble(parts[3])); return true;
            case "cancelrate":
                cancelRate.setValue(Double.parseDouble(parts[3])); return true;
            case "incombat":
                inCombatRate.setValue(Double.parseDouble(parts[3])); return true;
            case "missedswings":
                missedSwingsRate.setValue(Double.parseDouble(parts[3])); return true;
        }
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
                "Pause Duration: " + (int) pauseDuration.value + "ms",
                "Wait For First Hit: " + (int) waitForFirstHit.value + "ms",
                "Cancel Rate: " + (int) cancelRate.value + "%",
                "In Combat: " + (int) inCombatRate.value + "%",
                "Missed Swings: " + (int) missedSwingsRate.value + "%"
        );
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onDisable() {
        waitState  = WaitState.IDLE;
        pauseUntil = 0L;
    }

    // -------------------------------------------------------------------------
    // Tick — used to detect when the player gets hit (resolves wait-for-first-hit)
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // hurtTime is set to 10 on the tick the player is damaged
        if (mc.thePlayer.hurtTime == 10 && waitState == WaitState.WAITING) {
            waitState = WaitState.ACTIVE;
        }
    }

    // -------------------------------------------------------------------------
    // Main gate — call this from AutoClicker before sending a click.
    // Returns true if the click should be BLOCKED.
    // -------------------------------------------------------------------------

    public boolean shouldBlock() {
        if (!isEnabled()) return false;
        if (mc.thePlayer == null) return false;

        tickWaitState();

        // Hard block while waiting to be hit first
        if (waitState == WaitState.WAITING) return true;

        // Hard block during active pause window
        if (isPaused()) return true;

        // Classify the swing
        boolean lookingAtEntity = mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY;

        if (!lookingAtEntity) {
            // Missed swing — swinging at air or a block
            if (shouldCancel(missedRng, (int) missedSwingsRate.value)) return true;
        } else {
            EntityLivingBase target = getTargetEntity();
            boolean willDamage = target != null && willDealDamage(target);

            if (!willDamage) {
                // In range but hit won't register (target still invulnerable)
                if (shouldCancel(combatRng, (int) inCombatRate.value)) return true;
            } else {
                // Normal damaging hit — apply base cancel rate
                if (shouldCancel(baseRng, (int) cancelRate.value)) return true;
            }
        }

        // Click is going through — start a randomised pause window
        triggerPause();

        if (waitState == WaitState.TIMED_OUT) waitState = WaitState.ACTIVE;

        return false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void tickWaitState() {
        int waitMs = (int) waitForFirstHit.value;

        if (waitMs <= 0) {
            // Feature disabled — jump straight to ACTIVE
            if (waitState == WaitState.IDLE || waitState == WaitState.WAITING)
                waitState = WaitState.ACTIVE;
            return;
        }

        switch (waitState) {
            case IDLE:
                // First attack attempt — start waiting
                waitState   = WaitState.WAITING;
                waitStartMs = System.currentTimeMillis();
                break;
            case WAITING:
                // Check if the configured timeout has elapsed
                if (System.currentTimeMillis() - waitStartMs >= waitMs)
                    waitState = WaitState.TIMED_OUT;
                break;
            default:
                break;
        }
    }

    private boolean isPaused() {
        return System.currentTimeMillis() < pauseUntil;
    }

    /** Randomise the actual pause length within [0, pauseDuration] for natural variance. */
    private void triggerPause() {
        long duration = (long) (Math.random() * pauseDuration.value);
        pauseUntil    = System.currentTimeMillis() + duration;
    }

    private boolean shouldCancel(Random rng, int rate) {
        if (rate <= 0)   return false;
        if (rate >= 100) return true;
        return rng.nextFloat() * 100f < rate;
    }

    private EntityLivingBase getTargetEntity() {
        if (mc.objectMouseOver == null) return null;
        if (mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) return null;
        if (mc.objectMouseOver.entityHit instanceof EntityLivingBase)
            return (EntityLivingBase) mc.objectMouseOver.entityHit;
        return null;
    }

    /**
     * Predicts whether a swing will deal damage to the target.
     * hurtResistantTime counts down from 20 to 0 after a hit;
     * the server only registers damage when it reaches 0.
     */
    private boolean willDealDamage(EntityLivingBase target) {
        if (mc.thePlayer.getDistanceToEntity(target) > 3.0) return false;
        return target.hurtResistantTime <= 0;
    }
}
