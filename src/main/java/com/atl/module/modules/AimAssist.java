package com.atl.module.modules;

import com.atl.module.ExampleMod;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import com.atl.module.management.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AimAssist extends Module {

    private final NumberSetting hSpeed = new NumberSetting("Horizontal Speed", 3.0, 0.0, 10.0, 0.1);
    private final NumberSetting vSpeed = new NumberSetting("Vertical Speed", 1.0, 0.0, 10.0, 0.1);
    private final NumberSetting range = new NumberSetting("Range", 4.5, 3.0, 8.0, 0.1);
    private final NumberSetting fov = new NumberSetting("FOV", 90, 10, 360, 1);

    private final BooleanSetting weaponsOnly = new BooleanSetting("Weapons Only", true);
    private final BooleanSetting clickAim = new BooleanSetting("Click Aim", true);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", true);

    public AimAssist() {
        super("AimAssist", "Smoothly helps you aim at players", Category.COMBAT);
        addSettings(hSpeed, vSpeed, range, fov, weaponsOnly, clickAim, breakBlocks);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (event.phase != TickEvent.Phase.START) return;

        // 1. Requirement Checks
        if (weaponsOnly.isEnabled() && !(mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
            return;
        }

        boolean isAttacking = Mouse.isButtonDown(0);

        if (clickAim.isEnabled() && !isAttacking) {
            return;
        }

        // Breaking Blocks Check: Don't assist if we are looking at a block
        if (breakBlocks.isEnabled() && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }

        // 2. Target Acquisition
        EntityPlayer target = findBestTarget(mc);
        if (target == null) return;

        // 3. Rotation Calculation
        float[] rotations = getRotationsToEntity(target);

        // 4. GCD Fix and Smoothing
        // Minecraft sensitivity formula: sensitivity * 0.6 + 0.2
        float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float gcd = f * f * f * 8.0F * 0.15F;

        // Calculate the raw delta
        float yawDiff = MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw);
        float pitchDiff = rotations[1] - mc.thePlayer.rotationPitch;

        // Calculate total angular distance to the target
        double angularDistance = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        // Dynamic Proximity Multiplier: 
        // We scale the speed based on how far away the crosshair is.
        // Closer = slower (min 0.3x), Farther = faster (max 1.5x) relative to base speed.
        double proximityMultiplier = Math.max(0.3, Math.min(1.5, angularDistance / 15.0));

        // Apply speed and slight randomization (noise)
        // We multiply by a random factor between 0.9 and 1.1 to keep it somewhat linear but inconsistent
        double randomSpeed = 0.9 + (Math.random() * 0.2);
        float targetYawDelta = (float) (yawDiff * (hSpeed.value / 10.0F) * randomSpeed * proximityMultiplier);
        float targetPitchDelta = (float) (pitchDiff * (vSpeed.value / 10.0F) * randomSpeed * proximityMultiplier);

        // Cross-Axis Jitter: Humans cannot move a mouse in a perfectly straight 1D line.
        // We remove the conditional guards to ensure that if the module is tracking, 
        // both axes always receive noise. This forces 2D movement even if the initial delta is 0.
        // We use a factor of 0.8 * gcd to ensure the jitter is strong enough to occasionally 
        // trigger a rounding change, making the "straight line" look imperfect to the server.
        targetPitchDelta += (float) ((Math.random() - 0.5) * gcd * 0.8);
        targetYawDelta += (float) ((Math.random() - 0.5) * gcd * 0.8);

        // The GCD Fix: Round the delta to the nearest sensitivity step
        targetYawDelta = Math.round(targetYawDelta / gcd) * gcd;
        targetPitchDelta = Math.round(targetPitchDelta / gcd) * gcd;

        float smoothedYaw = mc.thePlayer.rotationYaw + targetYawDelta;
        float smoothedPitch = mc.thePlayer.rotationPitch + targetPitchDelta;

        ExampleMod.rotationManager.setRotation(smoothedYaw, smoothedPitch, 1, false);
    }

    private EntityPlayer findBestTarget(Minecraft mc) {
        List<EntityPlayer> targets = mc.theWorld.playerEntities.stream()
                .filter(e -> e != mc.thePlayer && !e.isDead && e.deathTime == 0)
                .filter(e -> !AntiBot.isBot(e))
                .filter(e -> !Teams.isTeammate(e))
                .filter(e -> mc.thePlayer.getDistanceSqToEntity(e) <= range.value * range.value)
                .filter(this::isInFOV)
                .filter(e -> mc.thePlayer.canEntityBeSeen(e))
                .sorted(Comparator.comparingDouble(mc.thePlayer::getDistanceSqToEntity))
                .collect(Collectors.toList());

        return targets.isEmpty() ? null : targets.get(0);
    }

    private boolean isInFOV(EntityPlayer target) {
        Minecraft mc = Minecraft.getMinecraft();
        float[] rotations = getRotationsToEntity(target);
        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - rotations[0]));
        float pitchDiff = Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationPitch - rotations[1]));

        return yawDiff <= fov.value / 2.0 && pitchDiff <= fov.value / 2.0;
    }

    private float[] getRotationsToEntity(EntityPlayer target) {
        Minecraft mc = Minecraft.getMinecraft();

        // Target the chest/neck area rather than the feet for more natural aiming
        double diffX = target.posX - mc.thePlayer.posX;
        double diffZ = target.posZ - mc.thePlayer.posZ;
        double diffY = (target.posY + target.getEyeHeight() * 0.8) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());

        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

        float yaw = (float) (MathHelper.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(MathHelper.atan2(diffY, dist) * 180.0D / Math.PI);

        return new float[]{yaw, pitch};
    }
}
