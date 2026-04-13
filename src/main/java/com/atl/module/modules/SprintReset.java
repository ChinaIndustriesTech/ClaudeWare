package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Random;

public class SprintReset extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    private int tapTicks = -1;
    private long lastTriggerTime = 0;

    public SprintReset() {
        super("SprintReset", "BROKEN DONT USE", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;

        if (event.phase == TickEvent.Phase.START) {
            int forwardKey = mc.gameSettings.keyBindForward.getKeyCode();

            // Tapping logic
            if (tapTicks > 0) {
                KeyBinding.setKeyBindState(forwardKey, false);
                mc.thePlayer.setSprinting(false);
                tapTicks--;
            } else if (tapTicks == 0) {
                if (Keyboard.isKeyDown(forwardKey)) {
                    KeyBinding.setKeyBindState(forwardKey, true);
                    mc.thePlayer.setSprinting(true);
                }
                tapTicks = -1;
            }

            // HIT DETECTION
            // 1. hurtTime > 0 means the player was hit recently
            // 2. lastTriggerTime ensures we only tap once per damage sequence (500ms for I-frames)
            long now = System.currentTimeMillis();
            if (mc.thePlayer.hurtTime > 0 && now - lastTriggerTime > 500) {
                
                boolean enemyNearby = false;
                for (Entity entity : mc.theWorld.loadedEntityList) {
                    if (entity instanceof EntityPlayer || entity instanceof EntityMob) {
                        if (entity != mc.thePlayer && mc.thePlayer.getDistanceToEntity(entity) < 6.0) {
                            enemyNearby = true;
                            break;
                        }
                    }
                }

                if (enemyNearby && Keyboard.isKeyDown(forwardKey)) {
                    this.tapTicks = 1 + random.nextInt(2);
                    this.lastTriggerTime = now;
                    sendMessage(EnumChatFormatting.BLUE + "Triggered!");
                }
            }
        }
    }

    private void sendMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.BLUE + "SprintReset" + EnumChatFormatting.GRAY + "] " + message));
        }
    }

    @Override
    public void onDisable() {
        tapTicks = -1;
        lastTriggerTime = 0;
    }
}
