package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class AutoBlock extends Module {

    private final NumberSetting range = new NumberSetting("Range", 3.8, 1.0, 6.0, 0.1);
    private final NumberSetting blockDuration = new NumberSetting("Block Duration", 90, 50, 500, 10);
    private final NumberSetting chance = new NumberSetting("Chance", 100, 0, 100, 1);

    private long blockEndTime = 0;
    private boolean pendingBlock = false;
    private EntityPlayer lastAttacker = null;
    private final Random random = new Random();

    public AutoBlock() {
        super("AutoBlock", "blocks for you sometimes", Category.COMBAT);
        addSettings(range, blockDuration, chance);
    }

    /**
     * Called by MixinNetHandlerPlayClient at the exact millisecond 
     * an animation packet is received.
     */
    public void handleEntitySwing(EntityPlayer opponent) {
        Minecraft mc = Minecraft.getMinecraft();

        // 1. Basic checks: Must be holding sword and attacking
        boolean holdingSword = mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
        if (!holdingSword || !Mouse.isButtonDown(0) || mc.currentScreen != null) return;

        // 2. Reach Check
        double distSq = mc.thePlayer.getDistanceSqToEntity(opponent);
        if (distSq > range.value * range.value) return;

        // 3. Humanization: Probability check
        if (chance.value < 100 && random.nextInt(100) > chance.value) return;

        // 4. Prediction: Is the entity actually targeting us?
        this.lastAttacker = opponent;
        if (isAimingAtMe(opponent)) {
            int currentHurtTime = mc.thePlayer.hurtTime;
            int pingThreshold = getHurtTimeThreshold();

            if (currentHurtTime <= 0) {
                // If we aren't red, block IMMEDIATELY to catch the first hit
                block(mc);
                pendingBlock = false;
            } else if (currentHurtTime > pingThreshold) {
                // If we are currently invincible, wait for the right moment
                pendingBlock = true;
            }
            // If currentHurtTime is between 1 and threshold, the onTick logic 
            // will catch it in the very next frame.
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || !isEnabled()) return;

        long currentTime = System.currentTimeMillis();
        int bind = mc.gameSettings.keyBindUseItem.getKeyCode();

        // Dynamic threshold for this specific tick
        int threshold = getHurtTimeThreshold();

        // Handle HurtTime logic: Release block during high invincibility
        if (!pendingBlock && mc.thePlayer.hurtTime > threshold && blockEndTime != 0) {
            KeyBinding.setKeyBindState(bind, false);
            blockEndTime = 0; // Force release
        }

        if (pendingBlock && mc.thePlayer.hurtTime <= threshold) {
            // Trust the initial swing packet but verify the player is still alive/valid
            if (lastAttacker != null && !lastAttacker.isDead && lastAttacker.deathTime == 0) {
                block(mc);
            }
            pendingBlock = false;
        }

        // Maintain or release the block state
        if (currentTime < blockEndTime) {
            KeyBinding.setKeyBindState(bind, true);
        } else if (blockEndTime != 0) {
            // Explicitly force an unblock regardless of manual input to reset the animation
            KeyBinding.setKeyBindState(bind, false);
            blockEndTime = 0;
        }
    }

    private int getPing() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null || mc.thePlayer == null) return 0;
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return (info != null) ? info.getResponseTime() : 0;
    }

    private int getHurtTimeThreshold() {
        int ping = getPing();
        // Base threshold: 1 tick + 1 tick per 100ms of latency
        int base = 1 + Math.min(3, ping / 100);

        // Add human-like variance (+/- 1 tick)
        // This ensures you don't always block at the exact same hurtTime
        if (random.nextInt(10) < 3) { // 30% chance to vary
            base += (random.nextBoolean() ? 1 : -1);
        }

        return Math.max(1, base);
    }

    /**
     * Sophisticated prediction: Checks if the opponent's crosshair 
     * actually intersects with our bounding box.
     */
    private boolean isAimingAtMe(EntityPlayer opponent) {
        Minecraft mc = Minecraft.getMinecraft();
        
        // 1. Extrapolate positions for both entities to account for 50ms tick latency
        double myMotionX = mc.thePlayer.posX - mc.thePlayer.prevPosX;
        double myMotionZ = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
        
        double oppMotionX = opponent.posX - opponent.prevPosX;
        double oppMotionY = opponent.posY - opponent.prevPosY;
        double oppMotionZ = opponent.posZ - opponent.prevPosZ;

        // 2. Expand our bounding box slightly based on ping (Backtracking compensation)
        // A 100ms ping means we are roughly 2 ticks "behind" where the server sees us.
        double pingScale = getPing() / 1000.0;
        AxisAlignedBB predictedBB = mc.thePlayer.getEntityBoundingBox().offset(
                myMotionX * (1.0 + pingScale), 
                0, 
                myMotionZ * (1.0 + pingScale)
        ).expand(0.1, 0.1, 0.1);

        // 3. Predict where the opponent is looking from (Eyes)
        Vec3 opponentEyes = new Vec3(
                opponent.posX + oppMotionX, 
                opponent.posY + oppMotionY + (double)opponent.getEyeHeight(), 
                opponent.posZ + oppMotionZ
        );
        
        Vec3 look = opponent.getLookVec();
        
        // 4. Create a ray representing the opponent's attack (Reach-aware)
        double reach = range.value;
        Vec3 rayEnd = opponentEyes.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);

        // 5. Use the engine's intercept logic to see if the ray hits the predicted hitbox
        boolean willHit = predictedBB.calculateIntercept(opponentEyes, rayEnd) != null;
        
        // 6. Secondary check: Threat angle (ensure they are generally facing us)
        Vec3 dirToMe = new Vec3(mc.thePlayer.posX - opponent.posX, 0, mc.thePlayer.posZ - opponent.posZ).normalize();
        double angle = look.dotProduct(dirToMe);

        return willHit || (angle > 0.9 && mc.thePlayer.getDistanceToEntity(opponent) < range.value);
    }

    private void block(Minecraft mc) {
        // Get the keycode for the 'Use Item' (Right Click) bind
        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        
        // Trigger a single "tick" of the keypress logic
        KeyBinding.onTick(useKey);
        
        // Set the timer for how long we should hold the block
        this.blockEndTime = System.currentTimeMillis() + (long) blockDuration.value;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
                "Range (range/dist): " + range.value,
                "Duration (blockduration/duration/ms): " + blockDuration.value + "ms",
                "Chance: " + chance.value + "%"
        );
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4) return false;
        try {
            String setting = parts[2].toLowerCase();
            double val = Double.parseDouble(parts[3]);
            if (setting.equals("range") || setting.equals("dist")) {
                range.setValue(val);
                return true;
            } else if (setting.equals("blockduration") || setting.equals("duration") || setting.equals("ms")) {
                blockDuration.setValue(val);
                return true;
            } else if (setting.equals("chance")) {
                chance.setValue(val);
                return true;
            }
            lastAttacker = null;
        } catch (Exception ignored) {
        }
        return false;
    }
}
