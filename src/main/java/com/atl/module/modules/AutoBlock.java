package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
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
        
        // 1. Predict our position in the next tick to account for strafing/movement
        double predictedX = mc.thePlayer.posX + (mc.thePlayer.posX - mc.thePlayer.prevPosX);
        double predictedY = mc.thePlayer.posY + (mc.thePlayer.posY - mc.thePlayer.prevPosY);
        double predictedZ = mc.thePlayer.posZ + (mc.thePlayer.posZ - mc.thePlayer.prevPosZ);

        // 2. Vector from opponent eyes to our chest/head area
        Vec3 ourPos = new Vec3(predictedX, predictedY + mc.thePlayer.getEyeHeight(), predictedZ);
        Vec3 opponentEyes = new Vec3(opponent.posX, opponent.posY + (double)opponent.getEyeHeight(), opponent.posZ);
        
        // Vector from opponent to us
        Vec3 v = ourPos.subtract(opponentEyes);

        // 2. Opponent's look vector
        Vec3 look = opponent.getLookVec();

        // 3. Dot product to ensure they are looking toward us (not away)
        double dot = v.dotProduct(look);
        if (dot < 0) return false; // They are looking in the opposite direction
        
        // 4. Calculate the closest distance from our center to their look ray
        // Using the formula: |v x look| / |look|
        // Since look is normalized, it's just the magnitude of the cross product
        Vec3 cross = new Vec3(
                v.yCoord * look.zCoord - v.zCoord * look.yCoord,
                v.zCoord * look.xCoord - v.xCoord * look.zCoord,
                v.xCoord * look.yCoord - v.yCoord * look.xCoord
        );
        
        double distanceToRay = Math.sqrt(cross.xCoord * cross.xCoord + cross.yCoord * cross.yCoord + cross.zCoord * cross.zCoord);

        // We normalize the vector 'v' to check the angle as well
        double cosAngle = v.normalize().dotProduct(look);

        // 1.0 is a balanced buffer for 1.8.9 trades. 
        // cosAngle > 0.707 (45 degrees) ensures a realistic attack cone.
        return distanceToRay < 1.0 && cosAngle > 0.707;
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
