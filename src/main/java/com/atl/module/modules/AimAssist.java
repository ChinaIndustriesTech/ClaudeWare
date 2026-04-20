package com.atl.module.modules;

import com.atl.module.Claude;
import com.atl.module.management.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AimAssist extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final NumberSetting hSpeed = new NumberSetting("Horizontal Speed", 3.0, 0.0, 10.0, 0.1);
    private final NumberSetting vSpeed = new NumberSetting("Vertical Speed", 1.0, 0.0, 10.0, 0.1);
    private final NumberSetting range = new NumberSetting("Range", 4.5, 3.0, 8.0, 0.1);
    private final NumberSetting fov = new NumberSetting("FOV", 90, 10, 360, 1);

    private final BooleanSetting weaponsOnly = new BooleanSetting("Weapons Only", true);
    private final BooleanSetting clickAim = new BooleanSetting("Click Aim", true);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", true);
    private final BooleanSetting switchMode = new BooleanSetting("Switch Mode", false);
    
    private final ModeSetting targetPriority = new ModeSetting("Priority", "Distance", "Distance", "Health", "HurtTime");

    private EntityPlayer currentTarget = null;

    public AimAssist() {
        super("AimAssist", "Smoothly helps you aim at players", Category.COMBAT);
        addSettings(targetPriority, switchMode, hSpeed, vSpeed, range, fov, weaponsOnly, clickAim, breakBlocks);
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

        if (breakBlocks.isEnabled() && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            currentTarget = null;
            return;
        }

        EntityPlayer target;

        if (switchMode.isEnabled()) {
            // SWITCH MODE: Recalculate the absolute best target every single tick
            target = findBestTarget(mc);
            currentTarget = target; 
        } else {
            // SINGLE TARGET (STICKY) MODE: Acquire once and hold until release or invalid
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
        // Don't check FOV here so we stay locked on even if they move behind us
        return mc.thePlayer.canEntityBeSeen(target);
    }

    private EntityPlayer findBestTarget(Minecraft mc) {
        List<EntityPlayer> targets = mc.theWorld.playerEntities.stream()
                .filter(e -> e != mc.thePlayer && !e.isDead && e.deathTime == 0)
                .filter(e -> !AntiBot.isBot(e))
                .filter(e -> !Teams.isTeammate(e))
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
        double diffX = target.posX - mc.thePlayer.posX;
        double diffZ = target.posZ - mc.thePlayer.posZ;
        double diffY = (target.posY + target.getEyeHeight() * 0.8) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(diffY, dist) * 180.0 / Math.PI);
        return new float[]{yaw, pitch};
    }
}
