package com.atl.module.modules;

import com.atl.module.Claude;
import com.atl.module.management.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayList extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();

    public BooleanSetting rainbow = new BooleanSetting("Rainbow", false);
    public NumberSetting red = new NumberSetting("Red", 182, 0, 255, 1);
    public NumberSetting green = new NumberSetting("Green", 0, 0, 255, 1);
    public NumberSetting blue = new NumberSetting("Blue", 0, 0, 255, 1);
    public NumberSetting rainbowSpeed = new NumberSetting("Rainbow Speed", 5, 1, 20, 0.1); // Scaled down by 1000
    public NumberSetting scale = new NumberSetting("Scale %", 100, 50, 150, 5);
    public ModeSetting format = new ModeSetting("Format", "Default", "Default", "Lowercase");

    public ArrayList() {
        super("ArrayList", "Draws the module list on screen", Category.RENDER);
        addSettings(rainbow, red, green, blue, rainbowSpeed, scale, format);
        setEnabled(true);
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
            "rainbow: " + rainbow.enabled, 
            "red: " + (int)red.value, 
            "green: " + (int)green.value, 
            "blue: " + (int)blue.value, 
            "rainbowSpeed: " + String.format("%.1f", rainbowSpeed.value),
            "scale: " + (int)scale.value + "%",
            "format: " + format.getValue()
        );
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT || !isEnabled()) return;

        FontRenderer fr = mc.fontRendererObj;
        float s = (float) (scale.value / 100.0);

        List<Module> enabledModules = Claude.moduleManager.getAll().stream()
                .filter(m -> m.isEnabled() && m.isDrawn() && !m.getName().equalsIgnoreCase("ArrayList"))
                .sorted(Comparator.comparingInt(m -> -fr.getStringWidth(getFormattedName(m))))
                .collect(Collectors.toList());

        GlStateManager.pushMatrix();
        GlStateManager.scale(s, s, 1.0f);

        int count = 0;
        float currentY = 2.0f / s;

        for (Module m : enabledModules) {
            String name = getFormattedName(m);
            float screenWidth = event.resolution.getScaledWidth() / s;
            int textWidth = fr.getStringWidth(name);
            float x = screenWidth - textWidth - (2.0f / s);

            int textColor;
            if (rainbow.enabled) {
                textColor = getRainbow(count * 200, (int)(rainbowSpeed.value * 1000), 1.0f) ;
            } else {
                textColor = new Color((int)red.value, (int)green.value, (int)blue.value, 255).getRGB();
            }

            fr.drawStringWithShadow(name, x, currentY, textColor);
            currentY += (fr.FONT_HEIGHT + 2);
            count++;
        }

        GlStateManager.popMatrix();
    }

    private String getFormattedName(Module m) {
        String name = m.getName();
        if (format.getValue().equals("Lowercase")) {
            return name.replaceAll("(?<=[a-z])(?=[A-Z])", " ").toLowerCase().trim();
        }
        return name;
    }

    private int getRainbow(int delay, int speed, float alphaValue) {
        float hue = (System.currentTimeMillis() + delay) % speed;
        hue /= (float)speed;
        Color c = Color.getHSBColor(hue, 0.7f, 1f);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(alphaValue * 255)).getRGB();
    }
}
