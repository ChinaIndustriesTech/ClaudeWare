package com.atl.module.management;

public class NumberSetting extends Setting {
    public double value, min, max, increment;

    public NumberSetting(String name, double defaultValue, double min, double max, double increment) {
        super(name);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public void setValue(double newValue) {
        // Clamps the value between min/max and snaps it to the increment
        double precision = 1.0 / increment;
        this.value = Math.round(Math.max(min, Math.min(max, newValue)) * precision) / precision;
    }
}