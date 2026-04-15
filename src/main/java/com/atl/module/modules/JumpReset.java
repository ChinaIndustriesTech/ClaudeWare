package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class JumpReset extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();

    private final NumberSetting chance = new NumberSetting("Chance", 100, 0, 100, 1);

    private long lastTriggerTime = 0;
    private int jumpDelayTicks = -1;

    public JumpReset() {
        super("JumpReset", "Automatically jumps when taking knockback to reset momentum", Category.MOVEMENT);
        addSettings(chance);
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4 || !parts[2].equalsIgnoreCase("chance")) return false;
        try {
            chance.setValue(Double.parseDouble(parts[3]));
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("Chance: " + chance.value + "%");
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null) return;

        // Handle the delayed jump
        if (jumpDelayTicks > 0) {
            jumpDelayTicks--;
        } else if (jumpDelayTicks == 0) {
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
            }
            jumpDelayTicks = -1;
        }

        // Detection logic: triggered when hurtTime > 0 and 500ms since last trigger
        // We check hurtTime == 9 specifically as it's the first tick of knockback
        long now = System.currentTimeMillis();
        if (mc.thePlayer.hurtTime > 0 && now - lastTriggerTime > 500 && jumpDelayTicks == -1) {
            boolean enemyNearby = false;
            List<Entity> entities = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.thePlayer, 
                    mc.thePlayer.getEntityBoundingBox().expand(6.0, 6.0, 6.0));

            for (Entity entity : entities) {
                if (entity instanceof EntityPlayer || entity instanceof EntityMob) {
                    if (entity instanceof EntityPlayer && AntiBot.isBot((EntityPlayer) entity)) continue;
                    enemyNearby = true;
                    break;
                }
            }

            if (enemyNearby && mc.thePlayer.onGround) {
                if (chance.value == 100 || random.nextInt(100) < chance.value) {
                    // Random delay between 2 and 6 ticks
                    this.jumpDelayTicks = 2 + random.nextInt(5);
                }
                lastTriggerTime = now;
            }
        }
    }

    @Override
    public void onDisable() {
        lastTriggerTime = 0;
        jumpDelayTicks = -1;
    }
}
