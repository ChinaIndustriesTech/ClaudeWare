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
    private int keybind = 0; // 0 = no keybind

    public List<Setting> settings = new ArrayList<>();
    public boolean settingsExpanded = false;

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
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

    // Methods for handling settings
    public void loadSettings(JsonObject settings) {
    }

    public JsonObject saveSettings() {
        return new JsonObject();
    }

    public boolean isHoldModule() { return false; }

    public boolean handleSetCommand(String[] parts) {
        return false;
    }

    public List<String> getSettings() {
        return Collections.emptyList();
    }
}
