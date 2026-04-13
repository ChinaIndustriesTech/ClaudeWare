package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class FullBright extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private float savedGamma = 0.0f;

    public FullBright() {
        super("FullBright", "think its broken idk", Category.RENDER);
    }

    @Override
    public void onEnable() {
        savedGamma = mc.gameSettings.gammaSetting;
    }

    @Override
    public void onDisable() {
        mc.gameSettings.gammaSetting = savedGamma;
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!isEnabled()) return;
        if (event.phase != TickEvent.Phase.END) return;

        mc.gameSettings.gammaSetting = 10000.0f;
    }
}