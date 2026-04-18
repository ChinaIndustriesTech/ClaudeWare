package com.atl.module.config;

import com.atl.module.management.Module;
import com.atl.module.management.ModuleManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {

    private final ModuleManager moduleManager;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ConfigManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    private Path getConfigDir() {
        // Fetched lazily to prevent startup crashes when mcDataDir is null
        return Paths.get(Minecraft.getMinecraft().mcDataDir.getAbsolutePath(), "config", "atl");
    }

    public boolean save(String name) {
        try {
            Path configDir = getConfigDir();
            Files.createDirectories(configDir);

            JsonObject root = new JsonObject();

            for (Module module : moduleManager.getAll()) {
                JsonObject moduleObj = new JsonObject();
                moduleObj.addProperty("enabled", module.isEnabled());
                moduleObj.addProperty("keybind", module.getKeybind());
                moduleObj.addProperty("drawn", module.isDrawn());

                // Save custom settings for each module
                moduleObj.add("settings", module.saveSettings());
                
                root.add(module.getName().toLowerCase(), moduleObj);
            }

            File file = configDir.resolve(name + ".json").toFile();
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                writer.write(gson.toJson(root));
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean load(String name) {
        try {
            Path configDir = getConfigDir();
            File file = configDir.resolve(name + ".json").toFile();
            if (!file.exists()) return false;

            JsonObject root;
            try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                root = gson.fromJson(reader, JsonObject.class);
            }

            if (root == null) return false;

            for (Module module : moduleManager.getAll()) {
                String key = module.getName().toLowerCase();
                if (!root.has(key)) continue;

                JsonObject moduleObj = root.getAsJsonObject(key);

                if (moduleObj.has("enabled")) {
                    module.setEnabled(moduleObj.get("enabled").getAsBoolean());
                }
                if (moduleObj.has("keybind")) {
                    module.setKeybind(moduleObj.get("keybind").getAsInt());
                }
                
                if (moduleObj.has("drawn")) {
                    module.setDrawn(moduleObj.get("drawn").getAsBoolean());
                }
                
                // Load custom settings
                if (moduleObj.has("settings")) {
                    module.loadSettings(moduleObj.getAsJsonObject("settings"));
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String[] listConfigs() {
        Path configDir = getConfigDir();
        File dir = configDir.toFile();
        if (!dir.exists()) return new String[0];
        File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return new String[0];
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            names[i] = files[i].getName().replace(".json", "");
        }
        return names;
    }
}
