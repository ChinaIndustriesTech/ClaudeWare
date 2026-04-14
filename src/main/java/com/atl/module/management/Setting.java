package com.atl.module.management;

public class Setting {
    public String name;
    public boolean focused; // Used by the GUI for text input or sliders

    public Setting(String name) {
        this.name = name;
    }
}