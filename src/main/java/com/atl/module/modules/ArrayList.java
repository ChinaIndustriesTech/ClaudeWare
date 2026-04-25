package com.atl.module.modules;

import com.atl.module.Claude;
import com.atl.module.management.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayList extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();

    public ModeSetting colorMode   = new ModeSetting("Mode", "Solid", "Solid", "Rainbow", "Gradient");

    public NumberSetting red       = new NumberSetting("Red",   182, 0, 255, 1);
    public NumberSetting green     = new NumberSetting("Green",   0, 0, 255, 1);
    public NumberSetting blue      = new NumberSetting("Blue",    0, 0, 255, 1);

    public NumberSetting red2      = new NumberSetting("Accent Red",   255, 0, 255, 1);
    public NumberSetting green2    = new NumberSetting("Accent Green",   0, 0, 255, 1);
    public NumberSetting blue2     = new NumberSetting("Accent Blue",  182, 0, 255, 1);

    public NumberSetting speed     = new NumberSetting("Speed", 5, 1, 20, 0.1);

    public BooleanSetting background = new BooleanSetting("Background", true);
    public NumberSetting bgRed       = new NumberSetting("BG Red",      0, 0, 255, 1);
    public NumberSetting bgGreen     = new NumberSetting("BG Green",    0, 0, 255, 1);
    public NumberSetting bgBlue      = new NumberSetting("BG Blue",     0, 0, 255, 1);
    public NumberSetting bgOpacity   = new NumberSetting("BG Opacity", 120, 0, 255, 1);
    public NumberSetting bgRounding  = new NumberSetting("BG Rounding",  3, 0, 10,  0.5); // Smaller increments
    public NumberSetting bgPadding   = new NumberSetting("BG Padding",   2, 0,  8,  1);

    public NumberSetting scale     = new NumberSetting("Scale %", 100, 50, 150, 5);
    public ModeSetting format      = new ModeSetting("Format", "Default", "Default", "Lowercase");

    public ArrayList() {
        super("ArrayList", "Draws the module list on screen", Category.RENDER);
        addSettings(colorMode, red, green, blue, red2, green2, blue2, speed,
                background, bgRed, bgGreen, bgBlue, bgOpacity, bgRounding, bgPadding,
                scale, format);
        setEnabled(true);
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
                "mode: "       + colorMode.getValue(),
                "background: " + background.enabled,
                "scale: "      + (int) scale.value + "%"
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

        if (enabledModules.isEmpty()) return;

        int total = enabledModules.size();
        float pad = (float) bgPadding.value;

        GlStateManager.pushMatrix();
        GlStateManager.scale(s, s, 1.0f);
        
        float scaledScreenWidth = (float) event.resolution.getScaledWidth_double() / s;
        float currentY = 2.0f / s;

        for (int i = 0; i < total; i++) {
            String name = getFormattedName(enabledModules.get(i));
            int textWidth = fr.getStringWidth(name);
            float x = scaledScreenWidth - textWidth - (2.0f / s);

            if (background.enabled) {
                int bgColor = new Color((int)bgRed.value, (int)bgGreen.value, (int)bgBlue.value, (int)bgOpacity.value).getRGB();
                drawLegitRoundedRect(x - pad, currentY - 1, textWidth + (pad * 2), fr.FONT_HEIGHT + 2, (float) bgRounding.value, bgColor);
            }

            fr.drawStringWithShadow(name, x, currentY, getColor(i, total));
            currentY += (fr.FONT_HEIGHT + 2);
        }

        GlStateManager.popMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawLegitRoundedRect(double x, double y, double w, double h, double radius, int color) {
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        radius = Math.min(radius, Math.min(w / 2.0, h / 2.0));

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        
        // Anti-aliasing setup
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        
        // Single continuous TRIANGLE_FAN for pixel-perfect smoothness
        worldrenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        
        // 1. Center of the box (for the fan)
        worldrenderer.pos(x + w / 2.0, y + h / 2.0, 0).color(r, g, b, a).endVertex();

        // 2. Top-Right Corner (FLAT)
        worldrenderer.pos(x + w, y, 0).color(r, g, b, a).endVertex();

        // 3. Top-Left Corner (ROUNDED)
        for (int i = 270; i >= 180; i--) {
            double rad = Math.toRadians(i);
            worldrenderer.pos(x + radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius, 0).color(r, g, b, a).endVertex();
        }

        // 4. Bottom-Left Corner (ROUNDED)
        for (int i = 180; i >= 90; i--) {
            double rad = Math.toRadians(i);
            worldrenderer.pos(x + radius + Math.cos(rad) * radius, y + h - radius + Math.sin(rad) * radius, 0).color(r, g, b, a).endVertex();
        }

        // 5. Bottom-Right Corner (FLAT)
        worldrenderer.pos(x + w, y + h, 0).color(r, g, b, a).endVertex();
        
        // 6. Close the fan at Top-Right
        worldrenderer.pos(x + w, y, 0).color(r, g, b, a).endVertex();

        tessellator.draw();

        // Restore GL state
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private int getColor(int index, int total) {
        switch (colorMode.getValue()) {
            case "Rainbow":  return getRainbow(index * 200, (int)(speed.value * 1000));
            case "Gradient": return getGradient(index, total);
            default:         return new Color((int)red.value, (int)green.value, (int)blue.value, 255).getRGB();
        }
    }

    private int getGradient(int index, int total) {
        if (total <= 1) return new Color((int)red.value, (int)green.value, (int)blue.value, 255).getRGB();
        int speedMs = (int)(speed.value * 1000);
        float timeOffset = (System.currentTimeMillis() % speedMs) / (float) speedMs;
        float listPos    = (float) index / (total - 1);
        float phase      = (listPos + timeOffset) % 1.0f;
        if (phase > 0.5f) phase = 1.0f - phase;
        phase *= 2.0f;
        phase = phase * phase * (3f - 2f * phase);
        return lerpColor(new Color((int)red.value, (int)green.value, (int)blue.value), new Color((int)red2.value, (int)green2.value, (int)blue2.value), phase);
    }

    private int lerpColor(Color a, Color b, float t) {
        int r  = (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t);
        int g  = (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t);
        return new Color(r, g, bl, 255).getRGB();
    }

    private int getRainbow(int delay, int speed) {
        float hue = (System.currentTimeMillis() + delay) % speed;
        hue /= (float) speed;
        Color c = Color.getHSBColor(hue, 0.7f, 1f);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), 255).getRGB();
    }

    private String getFormattedName(Module m) {
        String name = m.getName();
        if (format.getValue().equals("Lowercase"))
            return name.replaceAll("(?<=[a-z])(?=[A-Z])", " ").toLowerCase().trim();
        return name;
    }
}
