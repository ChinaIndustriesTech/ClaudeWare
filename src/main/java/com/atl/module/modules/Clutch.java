package com.atl.module.modules;

import com.atl.module.Claude;
import com.atl.module.management.*;
import com.atl.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.*;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;



public class Clutch extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── Settings ─────────────────────────────────────────────────────────────
    private final NumberSetting emptyBlocksUnder = new NumberSetting("EmptyBlocksUnder", 3, 1, 16, 1);
    private final NumberSetting predictionTicks  = new NumberSetting("Prediction", 10, 1, 20, 1);
    private final NumberSetting cooldownTicks    = new NumberSetting("Cooldown", 8, 0, 40, 1);
    private final BooleanSetting damagedOnly     = new BooleanSetting("DamagedOnly", false);
    private final ModeSetting placementMode      = new ModeSetting("Mode", "Both", "Both", "Walls Only");

    // Smooth rotation speeds (mirrors scaffold's telly smooth mode)
    private final NumberSetting rotMinSpeed = new NumberSetting("RotMinSpeed", 30.0f, 1.0f, 180.0f, 1.0f);
    private final NumberSetting rotMaxSpeed = new NumberSetting("RotMaxSpeed", 35.0f, 1.0f, 180.0f, 1.0f);
    private final NumberSetting rotStartMin = new NumberSetting("RotStartMin", 90.0f, 1.0f, 180.0f, 1.0f);
    private final NumberSetting rotStartMax = new NumberSetting("RotStartMax", 95.0f, 1.0f, 180.0f, 1.0f);

    // ── State ─────────────────────────────────────────────────────────────────
    private float yaw   = -180.0f;
    private float pitch =    0.0f;
    private int rotationTick = 0;   // counts down; >= 2 means "start" phase
    private boolean canRotate = false;

    private int cooldownLeft = 0;
    private int originalSlot = -1;
    private boolean active   = false;

    public static class BlockData {
        private final BlockPos blockPos;
        private final EnumFacing facing;

        public BlockData(BlockPos blockPos, EnumFacing facing) {
            this.blockPos = blockPos;
            this.facing = facing;
        }

        public BlockPos blockPos() { return this.blockPos; }
        public EnumFacing facing() { return this.facing; }
    }

    // Scaffold's placeOffsets for sub-block hit-vec scanning
    private static final double[] PLACE_OFFSETS = {
            0.03125, 0.09375, 0.15625, 0.21875, 0.28125, 0.34375,
            0.40625, 0.46875, 0.53125, 0.59375, 0.65625, 0.71875,
            0.78125, 0.84375, 0.90625, 0.96875
    };


    public Clutch() {
        super("Clutch", "Grim-safe predictive fall clutch", Category.PLAYER);
        addSettings(emptyBlocksUnder,predictionTicks, cooldownTicks, damagedOnly, placementMode,rotMinSpeed, rotMaxSpeed, rotStartMin, rotStartMax);
    }
    // ── Main tick ─────────────────────────────────────────────────────────────


    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.START
                || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null) { reset(); return; }

        if (cooldownLeft > 0) cooldownLeft--;
        if (rotationTick > 0) rotationTick--;

        // ── Activate ─────────────────────────────────────────────────────────
        if (!active) {
            if (cooldownLeft > 0 || !shouldTrigger()) return;

            // Find a block in hotbar
            int slot = findBlockSlot();
            if (slot == -1) return;

            originalSlot = mc.thePlayer.inventory.currentItem;
            mc.thePlayer.inventory.currentItem = slot;
            mc.playerController.updateController();

            // Reset smooth rotation state (mirrors scaffold onEnabled)
            yaw          = -180.0f;
            pitch        =    0.0f;
            canRotate    = false;
            rotationTick = 3;       // start phase
            active       = true;
        }

        // ── Landed — restore and cool down ───────────────────────────────────
        if (mc.thePlayer.onGround) {
            cooldownLeft = (int) cooldownTicks.value;
            reset();
            return;
        }

        if (!isHoldingBlock()) { reset(); return; }

        // ── Find placement data ───────────────────────────────────────────────
       BlockData blockData = getClutchBlockData();
        Vec3 hitVec = null;

        if (blockData != null) {
            hitVec = findBestHitVec(blockData);
        }

        // ── Smooth rotation (scaffold SMOOTH mode logic) ──────────────────────
        if (blockData != null && hitVec != null) {
            // We have a valid placement — update target yaw/pitch toward it
            float targetYaw   = yaw;
            float targetPitch = pitch;

            // On first lock-in, snap pitch to 85 like scaffold does
            if (yaw == -180.0f && pitch == 0.0f) {
                yaw   = RotationUtil.quantizeAngle(getBackwardsYaw());
                pitch = RotationUtil.quantizeAngle(85.0f);
            } else {
                // Smooth step toward target angles
                float wantYaw   = getBackwardsYaw();
                float wantPitch = 85.0f;

                float yawDiff   = RotationUtil.wrapAngleDiff(wantYaw - yaw, mc.thePlayer.rotationYaw);
                float pitchDiff = RotationUtil.wrapAngleDiff(wantPitch - pitch, mc.thePlayer.rotationPitch);

                float tolerance = rotationTick >= 2
                        ? RandomUtil.nextFloat((float) rotStartMin.value, (float) rotStartMax.value)
                        : RandomUtil.nextFloat((float) rotMinSpeed.value, (float) rotMaxSpeed.value);

                // Clamp step to tolerance (smooth approach)
                if (Math.abs(yawDiff) > tolerance) {
                    yaw = RotationUtil.quantizeAngle(yaw + RotationUtil.clampAngle(yawDiff, tolerance));
                } else {
                    yaw = RotationUtil.quantizeAngle(wantYaw);
                }
                if (Math.abs(pitchDiff) > tolerance) {
                    pitch = RotationUtil.quantizeAngle(pitch + RotationUtil.clampAngle(pitchDiff, tolerance));
                } else {
                    pitch = RotationUtil.quantizeAngle(wantPitch);
                }
            }

            canRotate = true;
        }

        // ── Send silent rotation via event (priority 3, same as scaffold) ─────
        if (canRotate) {
            Claude.rotationManager.setRotation(yaw, pitch, 3, false); // true = silent
        }

        // ── Place ─────────────────────────────────────────────────────────────
        if (blockData != null && hitVec != null && rotationTick <= 0) {
            place(blockData.blockPos(), blockData.facing(), hitVec);
        }
    }

    // ── Block search (adapted from scaffold's getBlockData) ───────────────────
    // Key difference: we scan predicted future positions, not just current feet.

    private BlockData getClutchBlockData() {
        boolean wallsOnly = placementMode.getValue().equals("Walls Only");
        double pt = predictionTicks.value;

        // Simulate trajectory with proper gravity (not just linear extrapolation)
        double vx = mc.thePlayer.motionX;
        double vy = mc.thePlayer.motionY;
        double vz = mc.thePlayer.motionZ;
        double px = mc.thePlayer.posX;
        double py = mc.thePlayer.posY;
        double pz = mc.thePlayer.posZ;

        for (int t = 1; t <= (int) pt; t++) {
            // Apply gravity per tick
            vy = (vy - 0.08) * 0.98;
            px += vx;
            py += vy;
            pz += vz;

            BlockPos targetPos = new BlockPos(
                    MathHelper.floor_double(px),
                    MathHelper.floor_double(py) - 1,
                    MathHelper.floor_double(pz)
            );

            if (!BlockUtil.isReplaceable(targetPos)) continue;

            // Search nearby solid blocks we can click on (mirrors scaffold radius)
            java.util.ArrayList<BlockPos> candidates = new java.util.ArrayList<>();
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 0; y++) {
                    for (int z = -2; z <= 2; z++) {
                        BlockPos pos = targetPos.add(x, y, z);
                        if (BlockUtil.isReplaceable(pos)) continue;
                        if (BlockUtil.isInteractable(pos)) continue;
                        if (mc.thePlayer.getDistance(
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                                > mc.playerController.getBlockReachDistance()) continue;

                        for (EnumFacing facing : EnumFacing.VALUES) {
                            // Respect walls-only mode
                            if (wallsOnly && facing == EnumFacing.UP) continue;
                            if (facing == EnumFacing.DOWN) continue;

                            BlockPos neighbor = pos.offset(facing);
                            if (BlockUtil.isReplaceable(neighbor)) {
                                candidates.add(pos);
                                break;
                            }
                        }
                    }
                }
            }

            if (candidates.isEmpty()) continue;

            // Pick closest to target (same as scaffold)
            candidates.sort(java.util.Comparator.comparingDouble(
                    o -> o.distanceSqToCenter(
                            targetPos.getX() + 0.5,
                            targetPos.getY() + 0.5,
                            targetPos.getZ() + 0.5
                    )
            ));

            BlockPos best   = candidates.get(0);
            EnumFacing face = getBestFacing(best, targetPos);
            if (face != null) return new BlockData(best, face);
        }
        return null;
    }

    // ── Hit-vec scanner (exact scaffold approach) ─────────────────────────────
    // Iterates sub-block offsets on the placement face, ray-traces each,
    // picks the one requiring the smallest rotation delta — Grim-safe.

    private Vec3 findBestHitVec(BlockData blockData) {
        double[] x = PLACE_OFFSETS, y = PLACE_OFFSETS, z = PLACE_OFFSETS;
        switch (blockData.facing()) {
            case NORTH: z = new double[]{0.0}; break;
            case EAST:  x = new double[]{1.0}; break;
            case SOUTH: z = new double[]{1.0}; break;
            case WEST:  x = new double[]{0.0}; break;
            case DOWN:  y = new double[]{0.0}; break;
            case UP:    y = new double[]{1.0}; break;
        }

        float bestYaw = -180.0f, bestPitch = 0.0f, bestDiff = Float.MAX_VALUE;
        Vec3 bestVec = null;

        float baseYaw   = yaw == -180.0f ? mc.thePlayer.rotationYaw : yaw;
        float basePitch = pitch == 0.0f  ? mc.thePlayer.rotationPitch : pitch;

        for (double dx : x) {
            for (double dy : y) {
                for (double dz : z) {
                    double relX = blockData.blockPos().getX() + dx - mc.thePlayer.posX;
                    double relY = blockData.blockPos().getY() + dy - mc.thePlayer.posY - mc.thePlayer.getEyeHeight();
                    double relZ = blockData.blockPos().getZ() + dz - mc.thePlayer.posZ;

                    float[] rots = RotationUtil.getRotationsTo(relX, relY, relZ, baseYaw, basePitch);
                    MovingObjectPosition mop = RotationUtil.rayTrace(
                            rots[0], rots[1], mc.playerController.getBlockReachDistance(), 1.0f
                    );

                    if (mop != null
                            && mop.typeOfHit == MovingObjectType.BLOCK
                            && mop.getBlockPos().equals(blockData.blockPos())
                            && mop.sideHit == blockData.facing()) {
                        float diff = Math.abs(rots[0] - baseYaw) + Math.abs(rots[1] - basePitch);
                        if (diff < bestDiff) {
                            bestYaw   = rots[0];
                            bestPitch = rots[1];
                            bestDiff  = diff;
                            bestVec   = mop.hitVec;
                        }
                    }
                }
            }
        }

        // If we got a valid hit, update our target angles
        if (bestVec != null) {
            yaw   = bestYaw;
            pitch = bestPitch;
            canRotate = true;
        }
        return bestVec;
    }

    // ── Placement ─────────────────────────────────────────────────────────────

    private void place(BlockPos pos, EnumFacing facing, Vec3 hitVec) {
        if (!isHoldingBlock()) return;
        if (mc.playerController.onPlayerRightClick(
                mc.thePlayer, mc.theWorld,
                mc.thePlayer.inventory.getCurrentItem(),
                pos, facing, hitVec)) {
            mc.thePlayer.swingItem();
        }
    }

    // ── Trigger check ─────────────────────────────────────────────────────────

    private boolean shouldTrigger() {
        if (mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying) return false;
        if (mc.thePlayer.motionY > -0.1) return false;
        if (damagedOnly.isEnabled() && mc.thePlayer.hurtTime <= 0) return false;

        BlockPos base = new BlockPos(
                mc.thePlayer.posX,
                mc.thePlayer.posY - 1,
                mc.thePlayer.posZ
        );
        for (int i = 0; i < (int) emptyBlocksUnder.value; i++) {
            if (!BlockUtil.isReplaceable(base.down(i))) return false;
        }
        return true;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    /** Yaw pointing away from player's movement (scaffold backwards mode) */
    private float getBackwardsYaw() {
        return RotationUtil.wrapAngleDiff(mc.thePlayer.rotationYaw - 180.0f, mc.thePlayer.rotationYaw);
    }

    private EnumFacing getBestFacing(BlockPos from, BlockPos target) {
        double best = Double.MAX_VALUE;
        EnumFacing result = null;
        for (EnumFacing f : EnumFacing.VALUES) {
            if (f == EnumFacing.DOWN) continue;
            BlockPos neighbor = from.offset(f);
            if (neighbor.getY() > target.getY()) continue;
            double dist = neighbor.distanceSqToCenter(
                    target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5
            );
            if (result == null || dist < best || (dist == best && f == EnumFacing.UP)) {
                best = dist;
                result = f;
            }
        }
        return result;
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            net.minecraft.item.ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
            if (s != null && s.getItem() instanceof net.minecraft.item.ItemBlock
                    && ((net.minecraft.item.ItemBlock) s.getItem()).getBlock().isFullCube())
                return i;
        }
        return -1;
    }

    private boolean isHoldingBlock() {
        net.minecraft.item.ItemStack held = mc.thePlayer.getHeldItem();
        return held != null && held.getItem() instanceof net.minecraft.item.ItemBlock;
    }

    private void reset() {
        if (originalSlot != -1 && mc.thePlayer != null) {
            mc.thePlayer.inventory.currentItem = originalSlot;
            mc.playerController.updateController();
            originalSlot = -1;
        }
        active    = false;
        canRotate = false;
        yaw       = -180.0f;
        pitch     =    0.0f;
        rotationTick = 0;
    }

    @Override
    public void onDisable() { reset(); }
}