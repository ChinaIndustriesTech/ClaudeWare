package com.atl.module.modules;

import com.atl.module.Claude;
import com.atl.module.management.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AimAssist extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final NumberSetting hSpeed = new NumberSetting("HSpeed", 3.0, 0.0, 10.0, 0.1);
    private final NumberSetting vSpeed = new NumberSetting("vSpeed", 1.0, 0.0, 10.0, 0.1);
    private final NumberSetting range = new NumberSetting("Range", 4.5, 3.0, 8.0, 0.1);
    private final NumberSetting fov = new NumberSetting("FOV", 90, 10, 360, 1);

    private final BooleanSetting weaponsOnly = new BooleanSetting("Weapons Only", true);
    private final BooleanSetting clickAim = new BooleanSetting("Click Aim", true);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", true);
    private final BooleanSetting ignoreInvis = new BooleanSetting("Ignore Invis", true);
    private final BooleanSetting mouseMove = new BooleanSetting("Mouse Moved", true);
    private final BooleanSetting switchMode = new BooleanSetting("Switch Mode", false);
    private final BooleanSetting multipoint = new BooleanSetting("Multipoint", true);
    
    private final NumberSetting hScale = new NumberSetting("hScale %", 80, 0, 100, 1);
    private final NumberSetting vScale = new NumberSetting("vScale %", 80, 0, 100, 1);
    
    private final ModeSetting targetPriority = new ModeSetting("Priority", "Distance", "Distance", "Health", "HurtTime");

    private EntityPlayer currentTarget = null;

    public AimAssist() {
        super("AimAssist", "Smoothly helps you aim at players", Category.COMBAT);
        addSettings(targetPriority, switchMode, multipoint, hScale, vScale, hSpeed, vSpeed, range, fov, weaponsOnly, clickAim, breakBlocks, ignoreInvis, mouseMove);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (event.phase != TickEvent.Phase.START) return;

        if (weaponsOnly.isEnabled() && !(mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
            currentTarget = null;
            return;
        }

        boolean isAttacking = Mouse.isButtonDown(0);
        if (clickAim.isEnabled() && !isAttacking) {
            currentTarget = null;
            return;
        }

        if (mouseMove.isEnabled() && Mouse.getDX() == 0 && Mouse.getDY() == 0) {
            return;
        }

        if (breakBlocks.isEnabled() && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }

        EntityPlayer target;
        if (switchMode.isEnabled()) {
            target = findBestTarget(mc);
            currentTarget = target; 
        } else {
            if (currentTarget != null && !isValid(currentTarget)) {
                currentTarget = null;
            }
            if (currentTarget == null) {
                currentTarget = findBestTarget(mc);
            }
            target = currentTarget;
        }

        if (target == null) return;

        float[] rotations = getRotationsToEntity(target);

        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 8.0F * 0.15F;

        float yawDiff = MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw);
        float pitchDiff = rotations[1] - mc.thePlayer.rotationPitch;

        double angularDistance = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
        double proximityMultiplier = Math.max(0.3, Math.min(1.5, angularDistance / 15.0));

        double randomSpeed = 0.9 + (Math.random() * 0.2);
        float targetYawDelta = (float) (yawDiff * (hSpeed.value / 10.0F) * randomSpeed * proximityMultiplier);
        float targetPitchDelta = (float) (pitchDiff * (vSpeed.value / 10.0F) * randomSpeed * proximityMultiplier);

        targetPitchDelta += (float) ((Math.random() - 0.5) * gcd * 0.8);
        targetYawDelta += (float) ((Math.random() - 0.5) * gcd * 0.8);

        targetYawDelta = Math.round(targetYawDelta / gcd) * gcd;
        targetPitchDelta = Math.round(targetPitchDelta / gcd) * gcd;

        float smoothedYaw = mc.thePlayer.rotationYaw + targetYawDelta;
        float smoothedPitch = mc.thePlayer.rotationPitch + targetPitchDelta;

        Claude.rotationManager.setRotation(smoothedYaw, smoothedPitch, 1, false);
    }

    private boolean isValid(EntityPlayer target) {
        if (target == null || target.isDead || target.deathTime > 0) return false;
        if (mc.thePlayer.getDistanceSqToEntity(target) > range.value * range.value) return false;
        if (AntiBot.isBot(target) || Teams.isTeammate(target)) return false;
        if (ignoreInvis.isEnabled() && target.isInvisible()) return false;
        return mc.thePlayer.canEntityBeSeen(target);
    }

    private EntityPlayer findBestTarget(Minecraft mc) {
        List<EntityPlayer> targets = mc.theWorld.playerEntities.stream()
                .filter(e -> e != mc.thePlayer && !e.isDead && e.deathTime == 0)
                .filter(e -> !AntiBot.isBot(e))
                .filter(e -> !Teams.isTeammate(e))
                .filter(e -> !ignoreInvis.isEnabled() || !e.isInvisible())
                .filter(e -> mc.thePlayer.getDistanceSqToEntity(e) <= range.value * range.value)
                .filter(this::isInFOV)
                .filter(e -> mc.thePlayer.canEntityBeSeen(e))
                .collect(Collectors.toList());

        if (targets.isEmpty()) return null;

        String mode = targetPriority.getValue();
        if (mode.equals("Distance")) {
            targets.sort(Comparator.comparingDouble(mc.thePlayer::getDistanceSqToEntity));
        } else if (mode.equals("Health")) {
            targets.sort(Comparator.comparingDouble(EntityPlayer::getHealth));
        } else if (mode.equals("HurtTime")) {
            targets.sort((e1, e2) -> {
                if (e1.hurtTime == 0 && e2.hurtTime > 0) return -1;
                if (e2.hurtTime == 0 && e1.hurtTime > 0) return 1;
                if (e1.hurtTime != e2.hurtTime) return Integer.compare(e1.hurtTime, e2.hurtTime);
                return Double.compare(mc.thePlayer.getDistanceSqToEntity(e1), mc.thePlayer.getDistanceSqToEntity(e2));
            });
        }

        return targets.get(0);
    }

    private boolean isInFOV(EntityPlayer target) {
        float[] rotations = getRotationsToEntity(target);
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - rotations[0]));
        float pitchDiff = Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationPitch - rotations[1]));
        return yawDiff <= fov.value / 2.0 && pitchDiff <= fov.value / 2.0;
    }

    private float[] getRotationsToEntity(EntityPlayer target) {
        AxisAlignedBB bb = target.getEntityBoundingBox().contract(0.2, 0.2, 0.2);

        if (!multipoint.isEnabled()) {
            double centerX = bb.minX + (bb.maxX - bb.minX) * 0.5;
            double centerY = bb.minY + (bb.maxY - bb.minY) * 0.5;
            double centerZ = bb.minZ + (bb.maxZ - bb.minZ) * 0.5;

            double diffX = centerX - mc.thePlayer.posX;
            double diffZ = centerZ - mc.thePlayer.posZ;
            double diffY = centerY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
            double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
            float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0F;
            float pitch = (float) -(Math.atan2(diffY, dist) * 180.0 / Math.PI);
            return new float[]{yaw, pitch};
        }

        List<Vec3> points = new ArrayList<>();
        double hs = hScale.value / 100.0;
        double vs = vScale.value / 100.0;
        double minXFactor = 0.5 - (hs / 2.0);
        double maxXFactor = 0.5 + (hs / 2.0);
        double minYFactor = 0.5 - (vs / 2.0);
        double maxYFactor = 0.5 + (vs / 2.0);
        double minZFactor = 0.5 - (hs / 2.0);
        double maxZFactor = 0.5 + (hs / 2.0);

        for (double x = minXFactor; x <= maxXFactor; x += Math.max(0.1, hs / 2.0)) {
            for (double y = minYFactor; y <= maxYFactor; y += Math.max(0.1, vs / 2.0)) {
                for (double z = minZFactor; z <= maxZFactor; z += Math.max(0.1, hs / 2.0)) {
                    points.add(new Vec3(
                        bb.minX + (bb.maxX - bb.minX) * x,
                        bb.minY + (bb.maxY - bb.minY) * y,
                        bb.minZ + (bb.maxZ - bb.minZ) * z
                    ));
                }
            }
        }

        if (points.isEmpty()) {
             points.add(new Vec3(bb.minX + (bb.maxX-bb.minX)*0.5, bb.minY + (bb.maxY-bb.minY)*0.5, bb.minZ + (bb.maxZ-bb.minZ)*0.5));
        }

        float bestDiff = Float.MAX_VALUE;
        float[] bestRots = new float[]{0, 0};
        for (Vec3 point : points) {
            double diffX = point.xCoord - mc.thePlayer.posX;
            double diffY = point.yCoord - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
            double diffZ = point.zCoord - mc.thePlayer.posZ;
            double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
            float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
            float pitch = (float) -(Math.atan2(diffY, dist) * 180.0 / Math.PI);
            float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw));
            float pitchDiff = Math.abs(pitch - mc.thePlayer.rotationPitch);
            if (yawDiff + pitchDiff < bestDiff) {
                bestDiff = yawDiff + pitchDiff;
                bestRots = new float[]{yaw, pitch};
            }
        }
        return bestRots;
    }
}
