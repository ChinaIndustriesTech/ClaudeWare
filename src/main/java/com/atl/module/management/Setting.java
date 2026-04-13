package com.atl.module.management;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;

@Getter
@Setter
@RequiredArgsConstructor
public class Setting {
    public final String name;
    private boolean focused; // Used by the GUI for text input or sliders
}