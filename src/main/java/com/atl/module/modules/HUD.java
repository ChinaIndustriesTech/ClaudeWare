package com.atl.module.modules;

import com.atl.module.ExampleMod;
import com.atl.module.management.BooleanSetting;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HUD extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();

    public BooleanSetting rainbow = new BooleanSetting("Rainbow", true);
    public NumberSetting red = new NumberSetting("Red", 1.0, 0.0, 1.0, 0.01);
    public NumberSetting green = new NumberSetting("Green", 1.0, 0.0, 1.0, 0.01);
    public NumberSetting blue = new NumberSetting("Blue", 1.0, 0.0, 1.0, 0.01);
    public NumberSetting alpha = new NumberSetting("Alpha", 1.0, 0.0, 1.0, 0.01);
    public NumberSetting rainbowSpeed = new NumberSetting("Rainbow Speed", 5000, 1000, 20000, 100); // Default 5 seconds cycle

    public HUD() {
        super("HUD", "Draws the module list on screen", Category.RENDER);
        addSettings(rainbow, red, green, blue, alpha, rainbowSpeed);
        setEnabled(true);
    }

    @Override
    public void loadSettings(JsonObject settings) {
        if (settings.has("rainbow")) rainbow.enabled = settings.get("rainbow").getAsBoolean();
        if (settings.has("red")) red.setValue(settings.get("red").getAsDouble());
        if (settings.has("green")) green.setValue(settings.get("green").getAsDouble());
        if (settings.has("blue")) blue.setValue(settings.get("blue").getAsDouble());
        if (settings.has("alpha")) alpha.setValue(settings.get("alpha").getAsDouble());
        if (settings.has("rainbowSpeed")) rainbowSpeed.setValue(settings.get("rainbowSpeed").getAsDouble());
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject settings = new JsonObject();
        settings.addProperty("rainbow", rainbow.enabled);
        settings.addProperty("red", red.value);
        settings.addProperty("green", green.value);
        settings.addProperty("blue", blue.value);
        settings.addProperty("alpha", alpha.value);
        settings.addProperty("rainbowSpeed", rainbowSpeed.value);
        return settings;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("rainbow (true/false)", "red (0-1)", "green (0-1)", "blue (0-1)", "alpha (0-1)", "rainbowSpeed (1000-20000)");
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        // Only render on the TEXT element to avoid drawing multiple times per tick
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT || !isEnabled()) return;

        FontRenderer fr = mc.fontRendererObj;

        // Get all enabled modules, excluding the HUD itself for a cleaner look
        List<Module> enabledModules = ExampleMod.moduleManager.getAll().stream()
                .filter(m -> m.isEnabled() && m.isDrawn() && !m.getName().equalsIgnoreCase("HUD"))
                .sorted(Comparator.comparingInt(m -> -fr.getStringWidth(m.getName())))
                .collect(Collectors.toList());

        int y = 2;
        int count = 0;
        for (Module m : enabledModules) {
            // Draw module names aligned to the right side of the screen
            String name = m.getName();
            int screenWidth = event.resolution.getScaledWidth();
            int textWidth = fr.getStringWidth(name);
            int x = screenWidth - textWidth - 2;

            // Calculate the text color using the configurable alpha value
            int textColor;
            if (rainbow.enabled) {
                textColor = getRainbow(count * 200, (int)rainbowSpeed.value, (float)alpha.value);
            } else {
                textColor = new Color((float)red.value, (float)green.value, (float)blue.value, (float)alpha.value).getRGB();
            }

            fr.drawStringWithShadow(name, x, y, textColor);
            y += fr.FONT_HEIGHT + 2;
            count++;
        }
    }

    private int getRainbow(int delay, int speed, float alphaValue) {
        float hue = (System.currentTimeMillis() + delay) % speed;
        hue /= (float)speed; // Cast speed to float for correct division
        Color c = Color.getHSBColor(hue, 0.7f, 1f);
        return new Color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alphaValue).getRGB();
    }
}