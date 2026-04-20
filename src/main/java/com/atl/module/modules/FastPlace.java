package com.atl.module.modules;

import com.atl.mixin.IMinecraft;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.List;

public class FastPlace extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", 0, 0, 4, 1);

    public FastPlace() {
        super("FastPlace", "Removes the delay when placing blocks", Category.PLAYER);
        addSettings(delay);
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4) return false;
        if (parts[2].equalsIgnoreCase("delay")) {
            try {
                delay.setValue(Double.parseDouble(parts[3]));
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public List<String> getSettings() {
        String modeLabel;
        switch ((int) delay.value) {
            case 0: modeLabel = "20 CPS"; break;
            case 1: modeLabel = "15 CPS"; break;
            case 2: modeLabel = "10 CPS"; break;
            case 3: modeLabel = "5 CPS"; break;
            default: modeLabel = "Vanilla"; break;
        }
        return Arrays.asList("Mode: " + modeLabel);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || mc.thePlayer == null || event.phase != TickEvent.Phase.START) return;

        // Only apply if holding a block to avoid messing up other interactions
        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) {
            IMinecraft accessor = (IMinecraft) mc;

            int target;
            switch ((int) delay.value) {
                case 0: // 20 CPS: 1 block every 1 tick
                    target = 0;
                    break;
                case 1: // 15 CPS: 3 blocks every 4 ticks (0, 0, 0, 1 pattern)
                    target = (mc.thePlayer.ticksExisted % 4 == 0) ? 1 : 0;
                    break;
                case 2: // 10 CPS: 1 block every 2 ticks
                    target = 1;
                    break;
                case 3: // 5 CPS: 1 block every 4 ticks
                    target = 3;
                    break;
                default: // Vanilla
                    return;
            }

            // If the vanilla timer is higher than our target speed, force it down
            if (accessor.getRightClickDelayTimer() > target) {
                accessor.setRightClickDelayTimer(target);
            }
        }
    }
}