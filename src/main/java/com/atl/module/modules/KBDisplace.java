package com.atl.module.modules;

import com.atl.event.PacketEvent;
import com.atl.module.Claude;
import com.atl.module.management.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.*;

public class KBDisplace extends Module {
    private static final int DISPLACE_WINDOW_TICKS = 10;
    private final Minecraft mc = Minecraft.getMinecraft();

    // Settings
    private final NumberSetting yawOffset = new NumberSetting("Yaw offset", 90, 0, 180, 1);
    private final NumberSetting delay = new NumberSetting("Delay (ms)", 0, 0, 1000, 50);
    private final NumberSetting range = new NumberSetting("Range", 3.5, 3.0, 9.0, 0.1);
    private final ModeSetting direction = new ModeSetting("Direction", "Left", "Left", "Right");
    private final BooleanSetting onlyLooking = new BooleanSetting("Only looking", false);
    private final BooleanSetting findVoid = new BooleanSetting("Find void", false);
    private final BooleanSetting blink = new BooleanSetting("Blink", false);
    private final BooleanSetting hasKnockback = new BooleanSetting("Has knockback", false);

    // State
    private boolean displaceThisTick = false;
    private boolean active = false;
    private boolean hasKB = false;
    private boolean compensateNextTick = false;
    private boolean displaceLeft = false;
    private boolean wasDisplacingLastTick = false;
    private boolean releaseBlinkNextTick = false;
    private int tickCounter = 0;
    private float baseYaw;
    private final Map<Integer, Integer> targetWindowStartTicks = new HashMap<>();

    public KBDisplace() {
        super("KBDisplace", "Displace movement on attack", Category.COMBAT);
        addSettings(yawOffset, delay, range, direction, onlyLooking, findVoid, blink, hasKnockback);
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    private void resetState() {
        displaceThisTick = false;
        active = false;
        hasKB = false;
        compensateNextTick = false;
        displaceLeft = false;
        wasDisplacingLastTick = false;
        releaseBlinkNextTick = false;
        tickCounter = 0;
        targetWindowStartTicks.clear();
        if (mc.thePlayer != null) {
            Claude.blinkManager.setBlinkState(false, BlinkModules.KBDELAY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        if (event.phase == TickEvent.Phase.START) {
            if (releaseBlinkNextTick) {
                Claude.blinkManager.setBlinkState(false, BlinkModules.KBDELAY);
                releaseBlinkNextTick = false;
            }
            
            updateLogic();
        } else {
            handlePostInput();
        }
    }

    private void updateLogic() {
        tickCounter++;
        pruneTargetDelayStates();

        if (hasKnockback.isEnabled() && EnchantmentHelper.getKnockbackModifier(mc.thePlayer) <= 0) {
            active = false;
            displaceThisTick = false;
            return;
        }

        EntityPlayer target = findClosestTarget(range.value);
        boolean attacking = Mouse.isButtonDown(0);
        
        if (onlyLooking.isEnabled()) {
            if (mc.objectMouseOver == null || mc.objectMouseOver.entityHit != target) {
                target = null;
            }
        }

        boolean hasKBEnchant = EnchantmentHelper.getKnockbackModifier(mc.thePlayer) > 0;
        active = target != null && attacking && (hasKBEnchant || anyMovementKey());

        if (!active) {
            if (wasDisplacingLastTick) {
                Claude.rotationManager.setRotation(baseYaw, mc.thePlayer.rotationPitch, 1, true);
            }
            displaceThisTick = false;
            compensateNextTick = false;
            wasDisplacingLastTick = false;
            return;
        }

        if (!findVoid.isEnabled() || !tryFindVoidDirection(target)) {
            displaceLeft = direction.getValue().equals("Left");
        }

        hasKB = hasKBEnchant;

        if (!displaceThisTick && !wasDisplacingLastTick) {
            baseYaw = mc.thePlayer.rotationYaw;
        }

        displaceThisTick = !displaceThisTick;

        if (displaceThisTick && !shouldDisplaceInCurrentWindow(target, tickCounter)) {
            displaceThisTick = false;
        }

        if (!displaceThisTick && wasDisplacingLastTick) {
            int key = mc.gameSettings.keyBindAttack.getKeyCode();
            if (key != 0) {
                KeyBinding.onTick(key);
            }
        }

        if (displaceThisTick) {
            float offset = (float) yawOffset.value;
            float targetYaw = baseYaw + (displaceLeft ? -offset : offset);
            Claude.rotationManager.setRotation(targetYaw, mc.thePlayer.rotationPitch, 1, true);
        } else if (wasDisplacingLastTick) {
            Claude.rotationManager.setRotation(baseYaw, mc.thePlayer.rotationPitch, 1, true);
        }

        wasDisplacingLastTick = displaceThisTick;
    }

    private void handlePostInput() {
        if (!active) {
            compensateNextTick = false;
            return;
        }

        if (compensateNextTick && !displaceThisTick) {
            compensateNextTick = false;
            mc.thePlayer.movementInput.moveStrafe = displaceLeft ? -1 : 1;
            return;
        }

        if (!displaceThisTick || hasKB) return;
        if (!anyMovementKey()) return;

        mc.thePlayer.movementInput.moveForward = 1;
        compensateNextTick = true;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPacketSend(PacketEvent.Send event) {
        if (!isEnabled() || !blink.isEnabled() || !active || !displaceThisTick || releaseBlinkNextTick) return;
        if (!(event.getPacket() instanceof C03PacketPlayer)) return;
        if (Claude.blinkManager.isBlinking()) return;

        Claude.blinkManager.setBlinkState(true, BlinkModules.KBDELAY);
        releaseBlinkNextTick = true;
    }

    private boolean shouldDisplaceInCurrentWindow(EntityPlayer target, int currentTick) {
        if (target == null) return true;

        int targetId = target.getEntityId();
        Integer windowStartTick = targetWindowStartTicks.get(targetId);
        if (windowStartTick == null || currentTick - windowStartTick >= DISPLACE_WINDOW_TICKS) {
            targetWindowStartTicks.put(targetId, currentTick);
            return true;
        }

        int delayTicks = (int) Math.ceil(delay.value / 50.0);
        if (delayTicks <= 0) return true;

        return (currentTick - windowStartTick) >= delayTicks;
    }

    private boolean tryFindVoidDirection(EntityPlayer target) {
        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.001) return false;

        dx /= dist; dz /= dist;
        double rightX = -dz; double rightZ = dx;
        double eyeY = target.posY + (double) target.getEyeHeight();

        int leftVoidCount = 0, rightVoidCount = 0;
        for (int i = 1; i <= 12; i++) {
            double off = i * 0.5;
            if (mc.theWorld.rayTraceBlocks(new Vec3(target.posX + rightX * off, eyeY, target.posZ + rightZ * off), 
                    new Vec3(target.posX + rightX * off, eyeY - 10, target.posZ + rightZ * off)) == null) rightVoidCount++;
            if (mc.theWorld.rayTraceBlocks(new Vec3(target.posX - rightX * off, eyeY, target.posZ - rightZ * off), 
                    new Vec3(target.posX - rightX * off, eyeY - 10, target.posZ - rightZ * off)) == null) leftVoidCount++;
        }

        if (leftVoidCount == 0 && rightVoidCount == 0) return false;
        if (leftVoidCount != rightVoidCount) displaceLeft = leftVoidCount > rightVoidCount;
        return true;
    }

    private void pruneTargetDelayStates() {
        if (mc.theWorld == null) {
            targetWindowStartTicks.clear();
            return;
        }
        targetWindowStartTicks.keySet().removeIf(id -> {
            Entity e = mc.theWorld.getEntityByID(id);
            return !(e instanceof EntityPlayer) || e.isDead || ((EntityPlayer) e).deathTime != 0;
        });
    }

    private EntityPlayer findClosestTarget(double range) {
        EntityPlayer closest = null;
        double minDist = range * range;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isDead || AntiBot.isBot(player)) continue;
            double d = mc.thePlayer.getDistanceSqToEntity(player);
            if (d < minDist) {
                minDist = d;
                closest = player;
            }
        }
        return closest;
    }

    private boolean anyMovementKey() {
        return mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown() ||
               mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown();
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("Angle: " + (int)yawOffset.value, "Delay: " + (int)delay.value + "ms", "Range: " + range.value);
    }
}
