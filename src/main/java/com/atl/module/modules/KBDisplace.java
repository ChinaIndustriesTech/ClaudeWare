package com.atl.module.modules;

import com.atl.module.Claude;
import com.atl.mixin.IMinecraft;
import com.atl.module.management.*;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.Arrays;
import java.util.List;

public class KBDisplace extends Module {
    
    private final Minecraft mc = Minecraft.getMinecraft();

    private final NumberSetting displaceAngle = new NumberSetting("Angle", 28.0, 5.0, 180.0, 1.0);
    private final NumberSetting rotationCooldownTicks = new NumberSetting("Cooldown", 4, 0, 40, 1);
    private final BooleanSetting kbEnchantOnly = new BooleanSetting("KB Enchant Only", false);
    private final BooleanSetting onlyWhileHoldingW = new BooleanSetting("Only W", true);
    private final BooleanSetting checkTargetMotion = new BooleanSetting("Target moving towards us", true);

    private enum Phase { IDLE, ROTATE_IN, ROTATE_OUT }
    private Phase phase = Phase.IDLE;

    private float baseYaw;
    private float targetYaw;
    private int cooldownLeft = 0;
    private int ticksInPhase = 0;

    public KBDisplace() {
        super("KBDisplace", "Displacement rotation based on hittable ticks", Category.COMBAT);
        addSettings(displaceAngle, rotationCooldownTicks, kbEnchantOnly, onlyWhileHoldingW, checkTargetMotion);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null || event.phase != TickEvent.Phase.START) return;
        
        if (cooldownLeft > 0) cooldownLeft--;

        if (mc.currentScreen != null) {
            reset();
            return;
        }

        if (phase == Phase.IDLE) {
            boolean requirementsMet = Mouse.isButtonDown(0) &&
                                     (!onlyWhileHoldingW.isEnabled() || Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) &&
                                     (!kbEnchantOnly.isEnabled() || hasKB(mc.thePlayer.getHeldItem()));

            if (requirementsMet && cooldownLeft <= 0) {
                double ticksToHit = getPredictHittableTicks();
                if (ticksToHit <= 3.0 && ticksToHit > 2.0) {
                    startRotation();
                }
            }
        }

        // --- Execution Logic ---
        if (phase == Phase.ROTATE_IN) {
            Claude.rotationManager.setRotation(targetYaw, mc.thePlayer.rotationPitch, 10, false);
            ticksInPhase++;
            if (ticksInPhase >= 2) {
                phase = Phase.ROTATE_OUT;
                ticksInPhase = 0;
            }
        } else if (phase == Phase.ROTATE_OUT) {
            Claude.rotationManager.setRotation(baseYaw, mc.thePlayer.rotationPitch, 10, false);
            ticksInPhase++;
            if (ticksInPhase >= 2) {
                finish();
            }
        }
    }

    private void startRotation() {
        baseYaw = mc.thePlayer.rotationYaw;
        float dir = mc.thePlayer.moveStrafing >= 0 ? 1.0f : -1.0f;
        targetYaw = baseYaw + (float) displaceAngle.value * dir;
        phase = Phase.ROTATE_IN;
        ticksInPhase = 0;
    }

    private void finish() {
        reset();
        cooldownLeft = (int) rotationCooldownTicks.value;
    }

    private void reset() {
        phase = Phase.IDLE;
        ticksInPhase = 0;
    }

    private boolean hasKB(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemSword && 
               EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack) > 0;
    }

    private double getPredictHittableTicks() {
        Vec3 myEyes = mc.thePlayer.getPositionEyes(1.0f);
        double bestTicks = Double.MAX_VALUE;
        double HIT_RANGE = 3.00D;
        double MAX_PREDICT_DISTANCE = 6.0D;

        for (Entity e : mc.theWorld.playerEntities) {
            if (e == mc.thePlayer || e.isDead || ((EntityPlayer)e).getHealth() <= 0 || ((EntityPlayer)e).deathTime > 0) continue;
            EntityPlayer ep = (EntityPlayer) e;
            
            Vec3 targetEyes = ep.getPositionEyes(1.0f);
            double dist = myEyes.distanceTo(targetEyes);
            if (dist > MAX_PREDICT_DISTANCE) continue;

            Vec3 toTargetVec = targetEyes.subtract(myEyes); 
            Vec3 dirToTarget = toTargetVec.normalize();

            // --- OPTIONAL MOTION CHECK ---
            if (checkTargetMotion.isEnabled()) {
                Vec3 targetMotion = new Vec3(ep.motionX, 0, ep.motionZ);
                Vec3 dirToMe = new Vec3(mc.thePlayer.posX - ep.posX, 0, mc.thePlayer.posZ - ep.posZ).normalize();
                
                // If they are moving, check if it's generally in our direction (Dot > 0.2)
                if (targetMotion.lengthVector() > 0.05 && targetMotion.normalize().dotProduct(dirToMe) < 0.2) {
                    continue;
                }
            }

            Vec3 relVel = new Vec3(mc.thePlayer.motionX - ep.motionX, mc.thePlayer.motionY - ep.motionY, mc.thePlayer.motionZ - ep.motionZ);
            double closingSpeed = relVel.dotProduct(dirToTarget);

            double ticks;
            if (dist <= HIT_RANGE) {
                ticks = 0.0;
            } else if (closingSpeed <= 0.01D) {
                ticks = Double.MAX_VALUE;
            } else {
                ticks = (dist - HIT_RANGE) / closingSpeed;
            }

            if (ticks >= 0 && ticks < bestTicks) {
                bestTicks = ticks;
            }
        }
        return bestTicks;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("Angle: " + (int)displaceAngle.value, "Phase: " + phase.name());
    }

    @Override
    public void onDisable() {
        reset();
    }
}
