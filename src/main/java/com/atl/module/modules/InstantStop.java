package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class InstantStop extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    

    private boolean wasW = false;
    private boolean wasA = false;
    private boolean wasD = false;

    private int tapSTicks = 0;
    private int tapDTicks = 0;
    private int tapATicks = 0;

    public InstantStop() {
        super("InstantStop", "Taps opposite keys to cancel forward and sideways momentum", Category.MOVEMENT);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || event.phase != TickEvent.Phase.START) {
            return;
        }

        int wKey = mc.gameSettings.keyBindForward.getKeyCode();
        int aKey = mc.gameSettings.keyBindLeft.getKeyCode();
        int sKey = mc.gameSettings.keyBindBack.getKeyCode();
        int dKey = mc.gameSettings.keyBindRight.getKeyCode();

        boolean isW = Keyboard.isKeyDown(wKey);
        boolean isA = Keyboard.isKeyDown(aKey);
        boolean isD = Keyboard.isKeyDown(dKey);

        if (wasW && !isW) tapSTicks = 1;
        if (wasA && !isA) tapDTicks = 1;
        if (wasD && !isD) tapATicks = 1;

        wasW = isW;
        wasA = isA;
        wasD = isD;

        handleKeyTap(sKey, tapSTicks);
        handleKeyTap(dKey, tapDTicks);
        handleKeyTap(aKey, tapATicks);

        if (tapSTicks > 0) tapSTicks--;
        if (tapDTicks > 0) tapDTicks--;
        if (tapATicks > 0) tapATicks--;
    }

    private void handleKeyTap(int keyCode, int ticksRemaining) {
        if (ticksRemaining > 0) {
            KeyBinding.setKeyBindState(keyCode, true);
        } else {
            if (!Keyboard.isKeyDown(keyCode)) {
                KeyBinding.setKeyBindState(keyCode, false);
            }
        }
    }

    @Override
    public void onDisable() {
        wasW = wasA = wasD = false;
        tapSTicks = tapDTicks = tapATicks = 0;
        
        if (mc.thePlayer != null) {
            syncKey(mc.gameSettings.keyBindForward);
            syncKey(mc.gameSettings.keyBindBack);
            syncKey(mc.gameSettings.keyBindLeft);
            syncKey(mc.gameSettings.keyBindRight);
        }
    }

    private void syncKey(KeyBinding bind) {
        KeyBinding.setKeyBindState(bind.getKeyCode(), Keyboard.isKeyDown(bind.getKeyCode()));
    }
}
