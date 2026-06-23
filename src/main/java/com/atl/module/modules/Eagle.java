package com.atl.module.modules;

import com.atl.event.PacketEvent;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.BooleanSetting;
import com.atl.module.management.NumberSetting;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Eagle extends Module {
    private static final EnumFacing[] SIDES = {
            EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST
    };

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();

    // Settings
    private final NumberSetting edgeOffset = new NumberSetting("Edge offset", 0.0, 0.0, 0.3, 0.01);
    private final NumberSetting unsneakDelay = new NumberSetting("Unsneak delay", 50, 0, 300, 5);
    private final NumberSetting sneakOnJump = new NumberSetting("Sneak on jump", 0, 0, 500, 5); // ms
    private final BooleanSetting sneakKeyPressed = new BooleanSetting("Sneak key pressed", false);
    private final BooleanSetting holdingBlocks = new BooleanSetting("Holding blocks", false);
    private final BooleanSetting lookingDown = new BooleanSetting("Looking down", false);
    private final BooleanSetting notMovingForward = new BooleanSetting("Not moving forward", false);
    private final BooleanSetting blockCounter = new BooleanSetting("Block Counter", true); // Kept from old Eagle

    // Internal state
    private boolean sneakingFromModule;
    private boolean placed; // Tracks if a block was placed while sneaking from module
    private int sneakJumpDelayTicks = -1;
    private int sneakJumpStartTick = -1;
    private int unsneakDelayTicks = -1;
    private int unsneakStartTick = -1;

    public Eagle() {
        super("Eagle", "Advanced bridge assist", Category.MOVEMENT);
        addSettings(edgeOffset, unsneakDelay, sneakOnJump, sneakKeyPressed, holdingBlocks, lookingDown, notMovingForward, blockCounter);
    }

    @Override
    public List<String> getSettings() {
        double offset = edgeOffset.value;
        String offsetStr = (offset == Math.rint(offset) ? Integer.toString((int) offset) : Double.toString(Math.round(offset * 100.0) / 100.0)) + " blocks";
        return Arrays.asList("Edge offset: " + offsetStr, "Unsneak delay: " + (int)unsneakDelay.value + "ms");
    }

    @Override
    public void onEnable() {
        sneakingFromModule = false;
        resetUnsneak();
    }

    @Override
    public void onDisable() {
        sneakingFromModule = false;
        resetUnsneak();
        // Ensure sneak key is released if module disabled
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || mc.thePlayer.capabilities.isFlying) return;

        boolean manualSneak = isManualSneak();
        boolean requireSneak = sneakKeyPressed.isEnabled();

        // If manual sneak is active and module doesn't require it, let manual sneak take over
        if (manualSneak && !requireSneak) {
            resetUnsneak();
            return;
        }

        // Conditions check
        if (requireSneak && (!manualSneak || (mc.thePlayer.movementInput.moveForward == 0 && mc.thePlayer.movementInput.moveStrafe == 0))) {
            if (!manualSneak) resetUnsneak();
            repressSneak();
            return;
        }
        if (notMovingForward.isEnabled() && mc.thePlayer.movementInput.moveForward > 0) {
            clearSneak();
            return;
        }
        if (lookingDown.isEnabled() && mc.thePlayer.rotationPitch < 70) {
            clearSneak();
            return;
        }
        if (holdingBlocks.isEnabled()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemBlock)) {
                clearSneak();
                return;
            }
        }

        // Simulate jump input
        boolean isJumpKeyDown = Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());

        // Sneak on jump logic
        if (isJumpKeyDown && mc.thePlayer.onGround && (mc.thePlayer.movementInput.moveForward != 0 || mc.thePlayer.movementInput.moveStrafe != 0) && sneakOnJump.value > 0) {
            if (!requireSneak) { // If not requiring sneak key, or if forceRelease was active
                sneakJumpStartTick = mc.thePlayer.ticksExisted;
                double raw = sneakOnJump.value / 50.0; // Convert ms to ticks (50ms per tick)
                int base = (int) raw;
                sneakJumpDelayTicks = base + (random.nextDouble() < (raw - base) ? 1 : 0);
                pressSneak(true);
                return;
            }
        }

        // Predict next tick's position for edge detection
        AxisAlignedBB predictedBox = predictNextTickBoundingBox(mc.thePlayer);
        double offset = computeEdgeOffset(predictedBox);

        if (Double.isNaN(offset)) { // No ground block found under player
            if (isJumpKeyDown && (sneakOnJump.value <= 0 || (mc.thePlayer.movementInput.moveForward == 0 && mc.thePlayer.movementInput.moveStrafe == 0))) {
                if (sneakingFromModule) tryReleaseSneak(true);
            } else if (mc.thePlayer.onGround) {
                pressSneak(true);
            } else if (sneakingFromModule) {
                tryReleaseSneak(true);
            }
            return;
        }

        if (offset > edgeOffset.value) {
            pressSneak(true);
        } else if (sneakingFromModule) {
            tryReleaseSneak(true);
        }
    }

    @SubscribeEvent
    public void onSendPacket(PacketEvent.Send event) {
        if (!isEnabled()) return;
        if (event.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            // If we are sneaking from the module and a block is placed, mark it
            if (sneakingFromModule) {
                placed = true;
            }
        }
    }

    private void pressSneak(boolean resetDelay) {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
        sneakingFromModule = true;
        if (resetDelay) unsneakStartTick = -1;
        repressSneak();
    }

    private void tryReleaseSneak(boolean resetDelay) {
        int existed = mc.thePlayer.ticksExisted;
        if (unsneakStartTick == -1 && sneakJumpStartTick == -1) {
            unsneakStartTick = existed;
            double raw = unsneakDelay.value / 50.0; // Convert ms to ticks
            int base = (int) raw;
            unsneakDelayTicks = base + (random.nextDouble() < (raw - base) ? 1 : 0);
        }

        if (sneakJumpStartTick != -1 && existed - sneakJumpStartTick < sneakJumpDelayTicks) {
            pressSneak(false); // Keep sneaking due to jump delay
            return;
        }
        if (unsneakStartTick != -1 && existed - unsneakStartTick < unsneakDelayTicks) {
            pressSneak(false); // Keep sneaking due to unsneak delay
            return;
        }

        releaseSneak(resetDelay);
    }

    private void releaseSneak(boolean resetDelay) {
        if (!sneakKeyPressed.isEnabled()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        } else if (sneakingFromModule && isManualSneak() && (placed || !mc.thePlayer.onGround)) {
            // If sneak key is pressed manually, but we initiated sneak, release it
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }

        sneakingFromModule = false;
        placed = false;
        if (resetDelay) resetUnsneak();
    }

    private void repressSneak() {
        // This method in the example seems to handle a specific "forceRelease" state,
        // which isn't directly replicated without the full PrePlayerInputEvent context.
        // For now, it ensures the sneak key is pressed if it was manually held.
        if (isManualSneak()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
        }
    }

    private void clearSneak() {
        sneakingFromModule = false;
        resetUnsneak();
        if (sneakKeyPressed.isEnabled()) repressSneak();
        else KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
    }

    private void resetUnsneak() {
        unsneakStartTick = -1;
        sneakJumpStartTick = -1;
        sneakJumpDelayTicks = -1;
        unsneakDelayTicks = -1;
    }

    private boolean isManualSneak() {
        return Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
    }

    // --- Predictive Movement and Edge Detection ---
    private AxisAlignedBB predictNextTickBoundingBox(EntityPlayerSP player) {
        // player.motionX/Z at Phase.START is the motion AFTER the previous tick's friction was applied.
        // We want to predict where the player will be at the end of this tick's movement.
        double motionX = player.motionX;
        double motionZ = player.motionZ;

        // Get intended movement input (ignoring current sneaking scaling)
        float forward = 0;
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) forward++;
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) forward--;
        float strafe = 0;
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())) strafe++;
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode())) strafe--;

        float accelerationFactor = 0.02f;
        if (player.onGround) {
            // Get slipperiness of block under player for accurate acceleration
            BlockPos bp = new BlockPos(player.posX, player.getEntityBoundingBox().minY - 0.5, player.posZ);
            float slipperiness = mc.theWorld.getBlockState(bp).getBlock().slipperiness;
            
            // Minecraft ground acceleration formula: getAIMoveSpeed() * (0.16277136 / (slipperiness * 0.91)^3)
            float f = slipperiness * 0.91f;
            accelerationFactor = player.getAIMoveSpeed() * (0.16277136f / (f * f * f));
        }

        // moveFlying implementation to add acceleration to current motion
        float f = strafe * strafe + forward * forward;
        if (f >= 1.0E-4F) {
            f = MathHelper.sqrt_float(f);
            if (f < 1.0F) f = 1.0F;
            f = accelerationFactor / f;
            strafe *= f;
            forward *= f;
            float f1 = MathHelper.sin(player.rotationYaw * (float)Math.PI / 180.0F);
            float f2 = MathHelper.cos(player.rotationYaw * (float)Math.PI / 180.0F);
            motionX += (double)(strafe * f2 - forward * f1);
            motionZ += (double)(forward * f2 + strafe * f1);
        }

        // Predict position after motion is applied but BEFORE this tick's friction is applied.
        return player.getEntityBoundingBox().offset(motionX, 0, motionZ);
    }

    private double computeEdgeOffset(AxisAlignedBB simBox) {
        AxisAlignedBB groundCheck = new AxisAlignedBB(
                simBox.minX, simBox.minY - 0.01, simBox.minZ,
                simBox.maxX, simBox.minY, simBox.maxZ
        );

        List<AxisAlignedBB> groundBoxes = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, groundCheck);
        if (groundBoxes.isEmpty()) return Double.NaN;

        double feetX = (simBox.minX + simBox.maxX) / 2.0;
        double feetZ = (simBox.minZ + simBox.maxZ) / 2.0;

        double minDist = Double.MAX_VALUE;
        for (AxisAlignedBB box : groundBoxes) {
            double closestX = Math.max(box.minX, Math.min(feetX, box.maxX));
            double closestZ = Math.max(box.minZ, Math.min(feetZ, box.maxZ));
            double dx = Math.abs(feetX - closestX);
            double dz = Math.abs(feetZ - closestZ);
            double dist = Math.max(dx, dz);
            minDist = Math.min(minDist, dist);
        }

        return minDist;
    }

    // --- Block Counter (Kept from old Eagle) ---
    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled() || !blockCounter.isEnabled() || event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        boolean canEagle = !sneakKeyPressed.isEnabled() || Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
        boolean isLookingDown = player.rotationPitch > 60.0F;

        if (!canEagle || !isLookingDown) return;

        int totalBlocks = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                totalBlocks += stack.stackSize;
            }
        }

        if (totalBlocks > 0) {
            ScaledResolution sr = new ScaledResolution(mc);
            String text = String.valueOf(totalBlocks);
            int x = sr.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(text) / 2;
            int y = sr.getScaledHeight() / 2 + 10;

            int color = 0xFFFFFFFF;
            if (totalBlocks <= 16) color = 0xFFFF0000;
            else if (totalBlocks <= 64) color = 0xFFFFFF00;
            else color = 0xFF00FF00;

            mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        }
    }
}