package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
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
    private long lastTriggerTime = 0;
    private int jumpDelayTicks = -1;
    private int chance = 100; // Default 100%

    public JumpReset() {
        super("JumpReset", "autojumpreset", Category.MOVEMENT);
    }

    @Override
    public void loadSettings(JsonObject settings) {
        if (settings.has("chance")) {
            this.chance = settings.get("chance").getAsInt();
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject settings = new JsonObject();
        settings.addProperty("chance", chance);
        return settings;
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 3) return false;
        if (parts[2].equalsIgnoreCase("chance")) {
            try {
                int value = Integer.parseInt(parts[3]);
                this.chance = Math.max(0, Math.min(100, value));
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("chance (0 - 100) - Current: " + chance + "%");
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
        long now = System.currentTimeMillis();
        if (mc.thePlayer.hurtTime > 0 && now - lastTriggerTime > 500 && jumpDelayTicks == -1) {
            
            boolean enemyNearby = false;
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityPlayer || entity instanceof EntityMob) {
                    if (entity != mc.thePlayer && mc.thePlayer.getDistanceToEntity(entity) < 6.0) {
                        // Ignore bots
                        if (entity instanceof EntityPlayer && AntiBot.isBot((EntityPlayer) entity)) continue;
                        enemyNearby = true;
                        break;
                    }
                }
            }

            if (enemyNearby && mc.thePlayer.onGround) {
                // Check chance before initiating delay
                if (chance == 100 || random.nextInt(100) < chance) {
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
