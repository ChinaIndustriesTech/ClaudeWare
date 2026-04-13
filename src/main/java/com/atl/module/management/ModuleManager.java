package com.atl.module.management;

import com.atl.module.modules.*;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModuleManager {

    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleManager() {
        register(new Eagle());
        register(new AutoTool());
        register(new FullBright());
        register(new PlayerTracker());
        register(new SprintReset());
        register(new BedTracker());
        register(new ItemESP());
        register(new FireballDetector());
        register(new MurderDetect());
        register(new JumpReset());
        register(new TwoDESP());
        register(new Chams());
        register(new AntiBot());
        register(new Trajectories());
        register(new ESP());
        register(new ClickGUI());
        register(new ArmorAlerts());
        register(new ConsumeAlerts());
        register(new AutoChest());
        register(new UpgradeAlerts());
        register(new ReachDisplay());
        register(new FreeLook());
        register(new AutoBlock());
    }

    private void register(Module module) {
        modules.put(module.getName().toLowerCase(), module);
        MinecraftForge.EVENT_BUS.register(module);
    }

    public Module get(String name) {
        return modules.get(name.toLowerCase());
    }

    public Collection<Module> getAll() {
        return modules.values();
    }
}
