package com.atl.module.modules;

import com.atl.module.management.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Arrays;
import java.util.List;

public class AutoBlock extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public NumberSetting range = new NumberSetting("Range", 3.8, 1.0, 6.0, 0.1);
    public NumberSetting blockTicks = new NumberSetting("Block Ticks", 1, 1, 5, 1);
    public NumberSetting maxHurtTime = new NumberSetting("MaxHurtMS", 200, 0, 500, 10);
    
    private long lastBlockTime = 0;
    private int blockTicksRemaining = 0;
    private boolean pendingBlock = false;

    public AutoBlock() {
        super("AutoBlock", "Predictive legit-input blocking system", Category.COMBAT);
        addSettings(range, blockTicks, maxHurtTime);
    }

    public void handleEntitySwing(EntityPlayer opponent) {
        if (!isEnabled() || mc.thePlayer == null || opponent == null || mc.currentScreen != null) return;

        boolean holdingSword = mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
        if (!holdingSword || !Mouse.isButtonDown(0)) return;

        if (isThreat(opponent)) {
            pendingBlock = true;
        }
    }

    private boolean isThreat(EntityPlayer opponent) {
        double dist = mc.thePlayer.getDistanceToEntity(opponent);
        if (dist > range.value) return false;

        Vec3 opponentEyes = opponent.getPositionEyes(1.0f);
        Vec3 look = opponent.getLookVec();
        
        Vec3 vectorToMe = new Vec3(
            mc.thePlayer.posX - opponent.posX,
            (mc.thePlayer.posY + mc.thePlayer.getEyeHeight() / 2.0) - opponentEyes.yCoord,
            mc.thePlayer.posZ - opponent.posZ
        ).normalize();
        
        double dot = look.dotProduct(vectorToMe);
        return dot > 0.70;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.START || mc.thePlayer == null) return;

        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        int threshold = (int) Math.ceil(maxHurtTime.value / 50.0);

        if (mc.thePlayer.hurtTime > threshold) {
            forceUnblock();
            return;
        }

        if (pendingBlock && mc.thePlayer.hurtTime <= threshold) {
            if (tryBlock()) {
                pendingBlock = false;
            }
        }

        if (blockTicksRemaining > 0) {
            KeyBinding.setKeyBindState(useKey, true);
            blockTicksRemaining--;
        } else if (!Mouse.isButtonDown(1)) {
            KeyBinding.setKeyBindState(useKey, false);
        }
    }

    private boolean tryBlock() {
        long now = System.currentTimeMillis();

        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(useKey, true);
        
        this.blockTicksRemaining = (int) blockTicks.value;
        this.lastBlockTime = now;
        return true;
    }

    private void forceUnblock() {
        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        if (!Mouse.isButtonDown(1)) {
            KeyBinding.setKeyBindState(useKey, false);
        }
        blockTicksRemaining = 0;
    }

    private int getPing() {
        if (mc.getNetHandler() == null) return 0;
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        return (info != null) ? info.getResponseTime() : 0;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("Range: " + range.value, "Ticks: " + (int)blockTicks.value, "MaxHurtTime: " + (int)maxHurtTime.value + "ms");
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4) return false;
        String setting = parts[2].toLowerCase();
        try {
            double val = Double.parseDouble(parts[3]);
            if (setting.equals("range")) { range.setValue(val); return true; }
            if (setting.equals("ticks")) { blockTicks.setValue(val); return true; }
            if (setting.equals("maxhurttime")) { maxHurtTime.setValue(val); return true; }
        } catch (Exception e) { return false; }
        return false;
    }

    @Override
    public void onDisable() {
        forceUnblock();
        pendingBlock = false;
    }
}
