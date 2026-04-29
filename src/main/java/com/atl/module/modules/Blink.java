package com.atl.module.modules;

import com.atl.module.Claude;
import com.atl.module.management.BlinkModules;
import com.atl.module.management.Category;
import com.atl.module.management.Module;

import java.util.Collections;
import java.util.List;

public class Blink extends Module {

    public Blink() {
        super("Blink", "Holds your packets then releases them", Category.MISC);
    }

    @Override
    public void onEnable() {
        Claude.blinkManager.setBlinkState(true, BlinkModules.BLINK);
    }

    @Override
    public void onDisable() {
        Claude.blinkManager.setBlinkState(false, BlinkModules.BLINK);
    }

    @Override
    public List<String> getSettings() {
        return Collections.singletonList(
                "queued: " + Claude.blinkManager.blinkedPackets.size()
        );
    }
}