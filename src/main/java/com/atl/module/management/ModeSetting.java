package com.atl.module.management;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting {
    public int index;
    public List<String> modes;

    public ModeSetting(String name, String defaultMode, String... modes) {
        super(name);
        this.modes = Arrays.asList(modes);
        this.index = this.modes.indexOf(defaultMode);
    }

    public String getValue() {
        return modes.get(index);
    }

    public void cycle() {
        if (index < modes.size() - 1) {
            index++;
        } else {
            index = 0;
        }
    }
}
