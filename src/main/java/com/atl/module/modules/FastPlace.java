package com.atl.module.modules;

import com.atl.mixin.IMinecraft;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class FastPlace extends Module {

    public FastPlace() {
        super("FastPlace", "Removes the delay when placing blocks", Category.PLAYER);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!isEnabled() || mc.thePlayer == null) return;

        // Only apply if holding a block to avoid messing up other interactions
        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) {
            ((IMinecraft) mc).setRightClickDelayTimer(0);
        }
    }
}