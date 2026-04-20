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

        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) {
            IMinecraft accessor = (IMinecraft) mc;

            int target;
            switch ((int) delay.value) {
                case 0:
                    target = 0;
                    break;
                case 1:
                    target = (mc.thePlayer.ticksExisted % 4 == 0) ? 1 : 0;
                    break;
                case 2:
                    target = 1;
                    break;
                case 3:
                    target = 3;
                    break;
                default:
                    return;
            }

            if (accessor.getRightClickDelayTimer() > target) {
                accessor.setRightClickDelayTimer(target);
            }
        }
    }
}