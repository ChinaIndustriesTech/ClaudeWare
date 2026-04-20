package com.atl.module.modules;

import com.atl.mixin.IMinecraft;
import com.atl.module.management.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class AutoClicker extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // Settings
    private final ModeSetting mode = new ModeSetting("Mode", "Legit", "Legit", "Blatant");
    private final BooleanSetting hitSelect = new BooleanSetting("HitSelect", false);
    private final NumberSetting hitSelectTick = new NumberSetting("HitSelect Tick", 5, 1, 7, 1);
    private final BooleanSetting triggerBot = new BooleanSetting("TriggerBot", false);

    // Legitimate (butterfly) state
    private int nextButterflyLength = 0, nextButterflyDelay = 0;

    // Blatant state
    private int nextBlatantLength = 0, nextBlatantDelay = 0;
    private int blatantBoost = 0;
    private int blatantClickCount = 0;
    private long blatantWindowStart = 0;

    // Blatant timer
    private long blatantTimer = 0;

    public AutoClicker() {
        super("AutoClicker", "Automatically clicks for you", Category.COMBAT);
        addSettings(mode, hitSelect, hitSelectTick, triggerBot);
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4) return false;
        switch (parts[2].toLowerCase()) {
            case "mode":
                if (parts[3].equalsIgnoreCase("legit")) mode.index = 0;
                else if (parts[3].equalsIgnoreCase("blatant")) mode.index = 1;
                return true;
            case "hitselect":
                hitSelect.enabled = Boolean.parseBoolean(parts[3]);
                return true;
            case "hitselecttick":
                hitSelectTick.setValue(Double.parseDouble(parts[3]));
                return true;
            case "triggerbot":
                triggerBot.enabled = Boolean.parseBoolean(parts[3]);
                return true;
        }
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
                "Mode: " + mode.getValue(),
                "hitSelect: " + hitSelect.enabled,
                "hitSelectTick: " + (int) hitSelectTick.value,
                "triggerBot: " + triggerBot.enabled
        );
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.START) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        boolean isOverBlock = mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
        boolean isOverEntity = mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY;

        if (isOverBlock && Mouse.isButtonDown(0)) {
            ((IMinecraft) mc).setLeftClickCounter(0);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
            return;
        }

        if (triggerBot.enabled && !isOverEntity) return;

        if (mode.getValue().equals("Blatant")) {
            blatantClick();
        } else {
            legitClick();
        }
    }

    private void legitClick() {
        if (hitSelectBlocked()) return;

        if (lookingAtEntity()) {
            if (Mouse.isButtonDown(0)) {
                if (nextButterflyLength < 0) {
                    nextButterflyDelay--;
                    if (nextButterflyDelay < 0) {
                        nextButterflyDelay = randomInt(0, 3);
                        nextButterflyLength = randomInt(3, 17);
                    }
                } else if (Math.random() < 0.95) {
                    nextButterflyLength--;
                    sendClick(0, true);
                } else {
                    sendClick(0, false);
                }
            }
        } else if (Mouse.isButtonDown(0)) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
        }
    }

    private void blatantClick() {
        if (hitSelectBlocked()) return;

        int cps = estimateCPS();
        if (cps > 22) return;

        if (lookingAtEntity()) {
            if (Mouse.isButtonDown(0)) {
                Random random = new Random();

                if (nextBlatantLength < 0) {
                    nextBlatantDelay--;
                    if (nextBlatantDelay < 0) {
                        nextBlatantDelay = randomInt(0, 1);
                        nextBlatantLength = randomInt(0, 23);
                    }
                } else if (Math.random() < 0.9289789) {
                    if (Math.random() < 0.09289789 && cps >= 6 && cps < 12) {
                        if (hasTimeElapsed(blatantTimer, (long) random.nextInt(3) * 8)) {
                            blatantTimer = System.currentTimeMillis();
                            sendClick(0, true);
                        }
                    }
                    nextBlatantLength--;
                    sendClick(0, true);
                    if (cps >= 12 && cps <= 16 && Math.random() < 0.3) {
                        if (hasTimeElapsed(blatantTimer, (long) random.nextInt(3) * 12)) {
                            blatantTimer = System.currentTimeMillis();
                            sendClick(0, true);
                        }
                    }
                    if (blatantBoost > 3 && cps > 16 && Math.random() < 0.6) {
                        if (hasTimeElapsed(blatantTimer, (long) random.nextInt(3) * 17)) {
                            blatantTimer = System.currentTimeMillis();
                            sendClick(0, true);
                        }
                        blatantBoost = 0;
                    }
                } else {
                    sendClick(0, false);
                }
            }
        } else if (Mouse.isButtonDown(0)) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
        }
    }

    private boolean hitSelectBlocked() {
        if (!hitSelect.enabled) return false;
        int hurt = mc.thePlayer.hurtTime;
        return hurt != 0 && hurt >= (int) hitSelectTick.value && hurt <= 7;
    }

    private boolean lookingAtEntity() {
        return mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit != null
                && mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK;
    }

    private static boolean hasTimeElapsed(long last, long ms) {
        return System.currentTimeMillis() - last >= ms;
    }

    private static int randomInt(int min, int max) {
        if (min >= max) return min;
        return min + (int) (Math.random() * (max - min));
    }

    private int estimateCPS() {
        long now = System.currentTimeMillis();
        if (now - blatantWindowStart >= 1000) {
            blatantWindowStart = now;
            blatantClickCount = 0;
        }
        return blatantClickCount;
    }

    private static void sendClick(int button, boolean state) {
        int key = button == 0
                ? mc.gameSettings.keyBindAttack.getKeyCode()
                : mc.gameSettings.keyBindUseItem.getKeyCode();

        KeyBinding.setKeyBindState(key, state);
        if (state) {
            KeyBinding.onTick(key);
        }
    }

    @Override
    public void onDisable() {
        if (mc.gameSettings != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        }
        nextButterflyLength = 0;
        nextButterflyDelay = 0;
        nextBlatantLength = 0;
        nextBlatantDelay = 0;
        blatantBoost = 0;
    }
}
