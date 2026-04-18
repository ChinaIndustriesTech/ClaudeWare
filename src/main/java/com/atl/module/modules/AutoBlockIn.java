package com.atl.module.modules;

import com.atl.module.ExampleMod;
import com.atl.module.management.Category;
import com.atl.module.management.BooleanSetting;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.init.Blocks;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.concurrent.ThreadLocalRandom;

public class AutoBlockIn extends Module {

    private final NumberSetting delay = new NumberSetting("Place Delay", 50, 0, 200, 10);
    private final BooleanSetting autoDisable = new BooleanSetting("AutoDisable", true);
    
    // State machine
    private enum State { IDLE, SWAP, ROTATE, PLACE, WAIT }
    private State state = State.IDLE;

    // Placement data
    private BlockData pendingBlockData;
    private int pendingSlot;
    private int originalSlot;
    private int currentWorkingSlot = -1;
    private long stateStartTime;
    private boolean needsSwap = true;
    
    // Rotation restoration data
    private float originalYaw, originalPitch;
    private boolean rotationCaptured = false;

    public AutoBlockIn() {
        super("AutoBlockIn", "Surrounds the player with blocks for protection", Category.PLAYER);
        addSettings(delay, autoDisable);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            // Lock in the original state the moment the module is turned on
            originalYaw = mc.thePlayer.rotationYaw;
            originalPitch = mc.thePlayer.rotationPitch;
            originalSlot = mc.thePlayer.inventory.currentItem;
            rotationCaptured = true;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null || event.phase != TickEvent.Phase.START) {
            return;
        }

        // Auto-disable if we take damage (hurtTime > 0)
        if (mc.thePlayer.hurtTime > 0) {
            setEnabled(false);
            return;
        }

        // Auto-disable if we have no blocks left in the hotbar
        if (findBlockSlot() == -1) {
            setEnabled(false);
            return;
        }

        if (autoDisable.isEnabled() && state == State.IDLE && isFullyCovered(mc)) {
             setEnabled(false);
             return;
        }
        
        // Execute state machine with chaining. 
        // This allows multiple state transitions in a single tick (e.g., IDLE -> SWAP -> ROTATE).
        for (int i = 0; i < 4; i++) {
            State prevState = state;
            processState(mc);
            if (state == prevState || state == State.WAIT) break;
        }
    }

    private void processState(Minecraft mc) {
        switch (state) {
            case IDLE: findAndInitiatePlacement(mc); break;
            case SWAP: handleSwap(mc); break;
            case ROTATE: handleRotate(mc); break;
            case PLACE: handlePlace(mc); break;
            case WAIT: handleWait(mc); break;
        }
    }

    private void findAndInitiatePlacement(Minecraft mc) {
        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        BlockPos[] targets = getTargets(playerPos);
        BlockPos headPos = playerPos.up(2);

        for (BlockPos pos : targets) {
            if (!mc.theWorld.getBlockState(pos).getBlock().getMaterial().isReplaceable()) continue;

            BlockData data = getBlockData(pos);
            if (data != null) {
                int slot = -1;
                if (pos.equals(headPos)) {
                    slot = findSpecificBlockSlot(Blocks.end_stone);
                }
                if (slot == -1) {
                    slot = findBlockSlot();
                }

                if (slot != -1) {
                    pendingBlockData = data;
                    pendingSlot = slot;

                    if (needsSwap || slot != currentWorkingSlot) {
                        state = State.SWAP;
                    } else {
                        state = State.ROTATE;
                    }
                    
                    // Optimization: If we are already on the correct slot, skip SWAP state immediately
                    if (state == State.SWAP && mc.thePlayer.inventory.currentItem == slot) {
                        state = State.ROTATE;
                    }

                    stateStartTime = System.currentTimeMillis();
                    return;
                }
            }
        }
    }

    private boolean isFullyCovered(Minecraft mc) {
        BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        for (BlockPos pos : getTargets(playerPos)) {
            // If any block in our protection radius is replaceable (air/tall grass/etc), we aren't covered
            if (mc.theWorld.getBlockState(pos).getBlock().getMaterial().isReplaceable()) return false;
        }
        return true;
    }


    private void handleSwap(Minecraft mc) {
        if (pendingSlot == mc.thePlayer.inventory.currentItem) {
            currentWorkingSlot = pendingSlot;
            state = State.ROTATE;
            stateStartTime = System.currentTimeMillis();
        } else {
            // Simulate pressing the hotbar key
            KeyBinding.onTick(mc.gameSettings.keyBindsHotbar[pendingSlot].getKeyCode());
            
            // To allow same-tick chaining (IDLE -> SWAP -> ROTATE), 
            // we manually update the currentItem so the next loop iteration sees the change.
            mc.thePlayer.inventory.currentItem = pendingSlot;
        }
    }

    private void handleRotate(Minecraft mc) {
        float[] rotations = getRotations(pendingBlockData.pos, pendingBlockData.face);
        // Higher priority (10) to ensure AutoBlockIn overrides AimAssist during placement
        ExampleMod.rotationManager.setRotation(rotations[0], rotations[1], 10, true);

        float yawDiff = MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw);
        float pitchDiff = rotations[1] - mc.thePlayer.rotationPitch;

        // Widened tolerance to 2.0F to account for GCD rounding steps
        if (Math.abs(yawDiff) < 2.0F && Math.abs(pitchDiff) < 2.0F) {
            state = State.PLACE;
            stateStartTime = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - stateStartTime > 250) {
            reset();
        }
    }

    private void handlePlace(Minecraft mc) {
        ItemStack blockStack = mc.thePlayer.inventory.getStackInSlot(pendingSlot);

        if (blockStack != null && blockStack.getItem() instanceof ItemBlock) {
            Vec3 hitVec = new Vec3(
                    pendingBlockData.pos.getX() + 0.5 + pendingBlockData.face.getFrontOffsetX() * 0.5,
                    pendingBlockData.pos.getY() + 0.5 + pendingBlockData.face.getFrontOffsetY() * 0.5,
                    pendingBlockData.pos.getZ() + 0.5 + pendingBlockData.face.getFrontOffsetZ() * 0.5
            );

            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, blockStack, pendingBlockData.pos, pendingBlockData.face, hitVec)) {
                mc.thePlayer.swingItem();
            }
        }

        state = State.WAIT;
        stateStartTime = System.currentTimeMillis();
    }

    private BlockPos[] getTargets(BlockPos playerPos) {
        BlockPos headLevel = playerPos.up();
        return new BlockPos[]{
                playerPos.north(), playerPos.south(), playerPos.east(), playerPos.west(),
                headLevel.north(), headLevel.south(), headLevel.east(), headLevel.west(),
                playerPos.up(2)
        };
    }

    private void handleWait(Minecraft mc) {
        long effectiveDelay = (long) delay.value;

        // If the player is in the air, increase the delay to at least 120ms to bypass placement heuristics
        if (!mc.thePlayer.onGround) {
            effectiveDelay = Math.max(effectiveDelay, 120L);
        }

        if (System.currentTimeMillis() - stateStartTime >= effectiveDelay) {
            reset();
        }
    }

    private void reset() {
        state = State.IDLE;
        pendingBlockData = null;
        pendingSlot = -1;
        currentWorkingSlot = -1;
        needsSwap = true;
    }

    private BlockData getBlockData(BlockPos pos) {
        Minecraft mc = Minecraft.getMinecraft();
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos neighbor = pos.offset(facing);
            if (mc.theWorld.getBlockState(neighbor).getBlock().getMaterial() != Material.air) {
                return new BlockData(neighbor, facing.getOpposite());
            }
        }
        return null;
    }

    private float[] getRotations(BlockPos pos, EnumFacing facing) {
        Minecraft mc = Minecraft.getMinecraft();

        // Calculate the sensitivity-based GCD (Greatest Common Divisor)
        // This is the step-size Minecraft uses for mouse movement.
        float sensitivity = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = sensitivity * sensitivity * sensitivity * 1.2F;

        double x = pos.getX() + 0.5 - mc.thePlayer.posX + facing.getFrontOffsetX() * 0.5;
        double z = pos.getZ() + 0.5 - mc.thePlayer.posZ + facing.getFrontOffsetZ() * 0.5;
        double y = pos.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()) + facing.getFrontOffsetY() * 0.5;

        double dist = MathHelper.sqrt_double(x * x + z * z);
        
        // Calculate raw rotations
        float targetYaw = (float) (MathHelper.atan2(z, x) * 180.0D / Math.PI) - 90.0F;
        float targetPitch = (float) -(MathHelper.atan2(y, dist) * 180.0D / Math.PI);

        // Add tiny randomized micro-jitter to prevent "DuplicateRot" flags.
        // This mimics human hand tremors and ensures no two packets are identical.
        targetYaw += (float) (ThreadLocalRandom.current().nextDouble(-0.05, 0.05));
        targetPitch += (float) (ThreadLocalRandom.current().nextDouble(-0.05, 0.05));

        // Round the rotations to the nearest sensitivity grid step
        float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw);
        float pitchDiff = MathHelper.clamp_float(targetPitch - mc.thePlayer.rotationPitch, -90.0F, 90.0F);

        yawDiff = Math.round(yawDiff / gcd) * gcd;
        pitchDiff = Math.round(pitchDiff / gcd) * gcd;

        return new float[]{
                mc.thePlayer.rotationYaw + yawDiff,
                mc.thePlayer.rotationPitch + pitchDiff
        };
    }

    private int findBlockSlot() {
        Minecraft mc = Minecraft.getMinecraft();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                return i;
            }
        }
        return -1;
    }

    private int findSpecificBlockSlot(Block block) {
        Minecraft mc = Minecraft.getMinecraft();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                ItemBlock itemBlock = (ItemBlock) stack.getItem();
                if (itemBlock.getBlock() == block) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static class BlockData {
        public final BlockPos pos;
        public final EnumFacing face;

        public BlockData(BlockPos pos, EnumFacing face) {
            this.pos = pos;
            this.face = face;
        }
    }

    @Override
    public void onDisable() {
        // Restore the original rotation if it was captured
        if (rotationCaptured) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                // Set force to 'true' to ensure the RotationManager applies the reset immediately
                ExampleMod.rotationManager.setRotation(originalYaw, originalPitch, 11, true);
                
                // Restore the original held item slot
                if (originalSlot != -1) {
                    mc.thePlayer.inventory.currentItem = originalSlot;
                }
            }
        }

        reset();
        rotationCaptured = false;
        originalSlot = -1;
    }
}