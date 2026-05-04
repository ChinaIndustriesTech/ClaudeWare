package com.atl.module.management;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {

    private final String name;
    private final String description;
    private boolean enabled = false;
    private Category category;
    private int keybind = 0;
    private boolean drawn = true;

    public List<Setting> settings = new ArrayList<>();
    public boolean settingsExpanded = false;

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    public void addSettings(Setting... settings) {
        for (Setting s : settings) this.settings.add(s);
    }
    public Category getCategory() {return category;}
    public void onEnable() {
    }

    public void onDisable() {
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable();
        else onDisable();
    }

    public boolean isDrawn() {
        return drawn;
    }

    public void setDrawn(boolean drawn) {
        this.drawn = drawn;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKeybind() {
        return keybind;
    }

    public void setKeybind(int key) {
        this.keybind = key;
    }

    public void setEnabled(boolean e) {
        if (e != enabled) {
            enabled = e;
            if (enabled) onEnable();
            else onDisable();
        }
    }

    public void loadSettings(JsonObject data) {
        for (Setting s : settings) {
            if (!data.has(s.name)) continue;

            if (s instanceof BooleanSetting) {
                ((BooleanSetting) s).enabled = data.get(s.name).getAsBoolean();
            } else if (s instanceof NumberSetting) {
                ((NumberSetting) s).setValue(data.get(s.name).getAsDouble());
            } else if (s instanceof ModeSetting) {
                ModeSetting ms = (ModeSetting) s;
                String val = data.get(s.name).getAsString();
                if (ms.modes.contains(val)) {
                    ms.index = ms.modes.indexOf(val);
                }
            }
        }
    }

    public JsonObject saveSettings() {
        JsonObject data = new JsonObject();
        for (Setting s : settings) {
            if (s instanceof BooleanSetting) {
                data.addProperty(s.name, ((BooleanSetting) s).isEnabled());
            } else if (s instanceof NumberSetting) {
                data.addProperty(s.name, ((NumberSetting) s).value);
            } else if (s instanceof ModeSetting) {
                data.addProperty(s.name, ((ModeSetting) s).getValue());
            }
        }
        return data;
    }

    public boolean isHoldModule() { return false; }

    public boolean handleSetCommand(String[] parts) {
        return false;
    }

    public List<String> getSettings() {
        return Collections.emptyList();
    }
}
