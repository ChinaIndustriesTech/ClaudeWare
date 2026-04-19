package com.atl.module.modules;

import com.atl.mixin.accessor.IAccessorEntityLivingBase;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.List;

public class NoJumpDelay extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", 0.0, 0.0, 8.0, 1.0);

    public NoJumpDelay() {
        super("NoJumpDelay", "Removes the delay between jumps", Category.MOVEMENT);
        addSettings(delay);
        this.setEnabled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || mc.thePlayer == null || event.phase != TickEvent.Phase.START) return;

        IAccessorEntityLivingBase accessor = (IAccessorEntityLivingBase) mc.thePlayer;

        // Replicate logic: clamp jumpTicks to the user-defined delay + 1
        if (accessor.getJumpTicks() > delay.value) {
            accessor.setJumpTicks((int) delay.value);
        }
    }

    @Override
    public List<String> getSettings() {
        return Collections.singletonList("Ticks: " + (int) delay.value);
    }
}
