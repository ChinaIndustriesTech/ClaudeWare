package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Arrays;
import java.util.List;

public class AutoBlock extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public NumberSetting range = new NumberSetting("Range", 3.8, 1.0, 6.0, 0.1);
    public NumberSetting blockTicks = new NumberSetting("Block Ticks", 2, 1, 10, 1);

    private int activeBlockTicks = 0;

    public AutoBlock() {
        super("AutoBlock", "Simulates a legit right-click block on hit", Category.COMBAT);
        addSettings(range, blockTicks);
    }

    /**
     * Packet-level trigger (Mixin). Fires the millisecond a swing is received.
     */
    public void handleEntitySwing(EntityPlayer opponent) {
        if (!isEnabled() || mc.thePlayer == null || opponent == null) return;
        checkAndTrigger(opponent);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.START || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();

        // Requirement: Holding sword and attacking (Mouse 0)
        boolean holdingSword = mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
        if (!holdingSword || !Mouse.isButtonDown(0)) {
            activeBlockTicks = 0;
            if (!Mouse.isButtonDown(1)) {
                KeyBinding.setKeyBindState(useKey, false);
            }
            return;
        }

        // 1. Handle active block countdown
        if (activeBlockTicks > 0) {
            KeyBinding.setKeyBindState(useKey, true);
            activeBlockTicks--;
            return;
        }

        // 2. Backup Scan: Check for swinging entities in range (in case packet was missed)
        for (EntityPlayer opponent : mc.theWorld.playerEntities) {
            if (opponent == mc.thePlayer || opponent.isDead) continue;
            if (opponent.isSwingInProgress) {
                checkAndTrigger(opponent);
            }
        }

        // 3. Release if no active block
        if (!Mouse.isButtonDown(1)) {
            KeyBinding.setKeyBindState(useKey, false);
        }
    }

    private void checkAndTrigger(EntityPlayer opponent) {
        double dist = mc.thePlayer.getDistanceToEntity(opponent);
        if (dist <= range.value) {
            // Generous Prediction: Aim check with hitbox expansion
            if (isAimingAtMe(opponent) || dist < 1.5) { // Force block if very close (trade range)
                triggerBlock();
            }
        }
    }

    private void triggerBlock() {
        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        activeBlockTicks = (int) blockTicks.value;
        KeyBinding.setKeyBindState(useKey, true);
    }

    private boolean isAimingAtMe(EntityPlayer opponent) {
        Vec3 opponentEyes = opponent.getPositionEyes(1.0f);
        Vec3 look = opponent.getLookVec();
        Vec3 rayEnd = opponentEyes.addVector(look.xCoord * 5.0, look.yCoord * 5.0, look.zCoord * 5.0);
        
        // Expand bounding box by 0.3 for a more generous "threat" area
        AxisAlignedBB myBB = mc.thePlayer.getEntityBoundingBox().expand(0.3, 0.3, 0.3);
        
        return myBB.calculateIntercept(opponentEyes, rayEnd) != null;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("Range: " + range.value, "Ticks: " + (int) blockTicks.value);
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4) return false;
        try {
            String setting = parts[2].toLowerCase();
            double val = Double.parseDouble(parts[3]);
            if (setting.equals("range")) { range.setValue(val); return true; }
            if (setting.equals("ticks")) { blockTicks.setValue(val); return true; }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public void onDisable() {
        activeBlockTicks = 0;
        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        if (mc.thePlayer != null && !Mouse.isButtonDown(1)) {
            KeyBinding.setKeyBindState(useKey, false);
        }
    }
}
