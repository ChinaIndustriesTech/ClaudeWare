package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;

import java.util.Collections;
import java.util.List;

public class InventoryMove extends Module {

    public InventoryMove() {
        super("InventoryMove", "Move while in inventory", Category.PLAYER);
    }

    @Override
    public List<String> getSettings() {
        return Collections.emptyList();
    }
}