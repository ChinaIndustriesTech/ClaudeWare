package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;

import java.util.Collections;
import java.util.List;

public class Reach extends Module {

    public final NumberSetting distance = new NumberSetting("Distance", 3.0, 3.0, 6.0, 0.01);
    public final NumberSetting chance = new NumberSetting("Chance", 100.0, 0.0, 100.0, 1.0);

    public Reach() {
        super("Reach", "Increases your combat reach distance", Category.COMBAT);
        addSettings(distance, chance);
    }

    @Override
    public List<String> getSettings() {
        return java.util.Arrays.asList("Dist: " + distance.value, "Chance: " + chance.value + "%");
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4) return false;
        if (parts[2].equalsIgnoreCase("distance") || parts[2].equalsIgnoreCase("value")) {
            try {
                double val = Double.parseDouble(parts[3]);
                distance.setValue(val);
                return true;
            } catch (NumberFormatException ignored) {}
        } else if (parts[2].equalsIgnoreCase("chance")) {
            try {
                double val = Double.parseDouble(parts[3]);
                chance.setValue(val);
                return true;
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }
}
