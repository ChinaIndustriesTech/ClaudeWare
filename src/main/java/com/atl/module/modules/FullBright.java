package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;

public class FullBright extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private float savedGamma = 0.0f;

    public FullBright() {
        super("FullBright", "Maximizes brightness by overriding gamma settings", Category.RENDER);
    }

    @Override
    public void onEnable() {
        savedGamma = mc.gameSettings.gammaSetting;
        mc.gameSettings.gammaSetting = 1000.0f;
    }

    @Override
    public void onDisable() {
        mc.gameSettings.gammaSetting = savedGamma;
    }
}