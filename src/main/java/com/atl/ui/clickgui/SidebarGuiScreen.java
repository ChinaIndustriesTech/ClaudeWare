package com.atl.ui.clickgui;

import com.atl.module.Claude;
import com.atl.module.management.*;
import com.atl.module.modules.ClickGUI;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SidebarGuiScreen extends GuiScreen {

    private final ClickGUI parent;

    private int panelX, panelY;
    private int panelWidth = 450, panelHeight = 300;
    private int sidebarWidth = 100;

    private Module selectedModule = null;

    private float moduleScrollOffset = 0;
    private float settingsScrollOffset = 0;
    private float maxModuleScroll = 0;
    private float maxSettingsScroll = 0;

    // Map to store animation progress for each BooleanSetting
    private Map<BooleanSetting, Float> booleanAnimationProgress = new HashMap<>();
    private final float ANIMATION_SPEED = 0.05f; // Lower = slower/smoother, higher = snappier

    public SidebarGuiScreen(ClickGUI parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.panelX = (this.width - panelWidth) / 2;
        this.panelY = (this.height - panelHeight) / 2;

        if (parent.lastCategory == null) {
            parent.lastCategory = Category.values()[0];
        }
        // Initialize animation progress for existing settings
        for (Module m : Claude.moduleManager.getAll()) {
            for (Setting s : m.settings) {
                if (s instanceof BooleanSetting) {
                    BooleanSetting b = (BooleanSetting) s;
                    booleanAnimationProgress.put(b, b.enabled ? 1.0f : 0.0f);
                }
            }
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (Module m : Claude.moduleManager.getAll()) {
            for (Setting s : m.settings) {
                if (s instanceof BooleanSetting) {
                    BooleanSetting b = (BooleanSetting) s;
                    float current = booleanAnimationProgress.getOrDefault(b, b.enabled ? 1.0f : 0.0f);
                    float target = b.enabled ? 1.0f : 0.0f;
                    float next = current + (target - current) * ANIMATION_SPEED;
                    // Snap to target when close enough to avoid endless micro-updates
                    if (Math.abs(next - target) < 0.005f) next = target;
                    booleanAnimationProgress.put(b, next);
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        handleMovement();

        int themeColor = parent.getThemeColor();

        drawHDBox(panelX, panelY, panelWidth, panelHeight, 8, 0xCC101010, false);
        drawHDBox(panelX, panelY, sidebarWidth, panelHeight, 8, 0xFF1a1a1a, true);

        int catY = panelY + 10;
        for (Category cat : Category.values()) {
            boolean hovered = isHovered(panelX, catY, sidebarWidth, 20, mouseX, mouseY);
            boolean selected = parent.lastCategory == cat;
            if (selected) Gui.drawRect(panelX, catY, panelX + 2, catY + 20, themeColor);
            mc.fontRendererObj.drawStringWithShadow(cat.name(), panelX + 10, catY + 6, selected ? themeColor : (hovered ? -1 : 0xFFBBBBBB));
            catY += 25;
        }

        int moduleListX = panelX + sidebarWidth + 5;
        int moduleListY = panelY + 5;
        int moduleListWidth = 150;
        int moduleListHeight = panelHeight - 10;

        int settingsPanelX = moduleListX + moduleListWidth + 5;
        int settingsPanelY = panelY + 5;
        int settingsPanelWidth = panelWidth - sidebarWidth - moduleListWidth - 15;
        int settingsPanelHeight = panelHeight - 10;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scaleFactor = new ScaledResolution(mc).getScaleFactor();
        GL11.glScissor(moduleListX * scaleFactor, (this.height - (moduleListY + moduleListHeight)) * scaleFactor, moduleListWidth * scaleFactor, moduleListHeight * scaleFactor);

        int currentModuleY = (int) (moduleListY + moduleScrollOffset);
        int totalModuleHeight = 0;
        for (Module m : Claude.moduleManager.getAll()) {
            if (m.getCategory() != parent.lastCategory) continue;
            boolean hovered = isHovered(moduleListX, currentModuleY, moduleListWidth, 20, mouseX, mouseY);

            drawHDBox(moduleListX, currentModuleY, moduleListWidth, 20, 4, m.isEnabled() ? themeColor : (hovered ? 0xFF353535 : 0xFF202020), false);
            mc.fontRendererObj.drawStringWithShadow(m.getName(), moduleListX + 5, currentModuleY + 6, m.isEnabled() ? -1 : 0xFFBBBBBB);

            currentModuleY += 25;
            totalModuleHeight += 25;
        }
        maxModuleScroll = Math.max(0, totalModuleHeight - moduleListHeight);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (selectedModule != null) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(settingsPanelX * scaleFactor, (this.height - (settingsPanelY + settingsPanelHeight)) * scaleFactor, settingsPanelWidth * scaleFactor, settingsPanelHeight * scaleFactor);

            int currentSettingY = (int) (settingsPanelY + settingsScrollOffset);
            int totalSettingHeight = 0;
            for (Setting s : selectedModule.settings) {
                if (s instanceof BooleanSetting) {
                    BooleanSetting b = (BooleanSetting) s;
                    boolean hovered = isHovered(settingsPanelX, currentSettingY, settingsPanelWidth, 15, mouseX, mouseY);
                    mc.fontRendererObj.drawStringWithShadow(s.name, settingsPanelX, currentSettingY + 3, hovered ? -1 : 0xFFBBBBBB);

                    // Pill toggle switch
                    int switchWidth = 20;
                    int switchHeight = 10;
                    int switchX = settingsPanelX + settingsPanelWidth - switchWidth - 5;
                    int switchY = currentSettingY + 3;

                    // Read the smoothly interpolated progress (updated in updateScreen)
                    float currentProgress = booleanAnimationProgress.getOrDefault(b, b.enabled ? 1.0f : 0.0f);

                    double knobX = switchX + 1 + (currentProgress * (switchWidth - 9)); // 9 is knob width + 1 padding

                    // Draw background pill
                    drawHDBox(switchX, switchY, switchWidth, switchHeight, 5, b.enabled ? themeColor : 0xFF333333, false);

                    // Draw knob
                    drawHDBox(knobX, switchY + 1, 8, 8, 4, -1, false);

                    currentSettingY += 18;
                    totalSettingHeight += 18;
                } else if (s instanceof NumberSetting) {
                    NumberSetting n = (NumberSetting) s;
                    mc.fontRendererObj.drawString(s.name + ": " + String.format("%.2f", n.value), settingsPanelX, currentSettingY, 0xFFBBBBBB, false);
                    int sliderWidth = settingsPanelWidth - 10;
                    double renderWidth = sliderWidth * ((n.value - n.min) / (n.max - n.min));

                    // Draw pill-shaped background
                    drawHDBox(settingsPanelX + 5, currentSettingY + 12, sliderWidth, 4, 2, 0xFF333333, false);
                    // Draw pill-shaped progress
                    if (renderWidth > 0) {
                        drawHDBox(settingsPanelX + 5, currentSettingY + 12, renderWidth, 4, 2, themeColor, false);
                    }

                    if (Mouse.isButtonDown(0) && isHovered(settingsPanelX + 5, currentSettingY + 5, sliderWidth, 15, mouseX, mouseY)) {
                        double diff = MathHelper.clamp_double(mouseX - (settingsPanelX + 5), 0, sliderWidth);
                        n.setValue(((diff / sliderWidth) * (n.max - n.min)) + n.min);
                    }
                    currentSettingY += 25;
                    totalSettingHeight += 25;
                } else if (s instanceof ModeSetting) {
                    ModeSetting ms = (ModeSetting) s;
                    mc.fontRendererObj.drawStringWithShadow(ms.name + ": ", settingsPanelX, currentSettingY, 0xFFBBBBBB);
                    mc.fontRendererObj.drawStringWithShadow(ms.getValue(), settingsPanelX + mc.fontRendererObj.getStringWidth(ms.name + ": "), currentSettingY, themeColor);
                    currentSettingY += 18;
                    totalSettingHeight += 18;
                }
            }
            maxSettingsScroll = Math.max(0, totalSettingHeight - settingsPanelHeight);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawHDBox(double x, double y, double w, double h, double radius, int color, boolean leftOnly) {
        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        radius = Math.min(radius, Math.min(w / 2.0, h / 2.0));

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION_COLOR);

        if (leftOnly) {
            worldrenderer.pos(x + w, y, 0).color(r, g, b, a).endVertex();
            for (int i = 270; i >= 180; i--) {
                double rad = Math.toRadians(i);
                worldrenderer.pos(x + radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius, 0).color(r, g, b, a).endVertex();
            }
            for (int i = 180; i >= 90; i--) {
                double rad = Math.toRadians(i);
                worldrenderer.pos(x + radius + Math.cos(rad) * radius, y + h - radius + Math.sin(rad) * radius, 0).color(r, g, b, a).endVertex();
            }
            worldrenderer.pos(x + w, y + h, 0).color(r, g, b, a).endVertex();
        } else {
            for (int i = 360; i >= 270; i--) {
                double rad = Math.toRadians(i);
                worldrenderer.pos(x + w - radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius, 0).color(r, g, b, a).endVertex();
            }
            for (int i = 270; i >= 180; i--) {
                double rad = Math.toRadians(i);
                worldrenderer.pos(x + radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius, 0).color(r, g, b, a).endVertex();
            }
            for (int i = 180; i >= 90; i--) {
                double rad = Math.toRadians(i);
                worldrenderer.pos(x + radius + Math.cos(rad) * radius, y + h - radius + Math.sin(rad) * radius, 0).color(r, g, b, a).endVertex();
            }
            for (int i = 90; i >= 0; i--) {
                double rad = Math.toRadians(i);
                worldrenderer.pos(x + w - radius + Math.cos(rad) * radius, y + h - radius + Math.sin(rad) * radius, 0).color(r, g, b, a).endVertex();
            }
        }

        tessellator.draw();

        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private void handleMovement() {
        if (mc.thePlayer == null) return;
        sync(mc.gameSettings.keyBindForward);
        sync(mc.gameSettings.keyBindBack);
        sync(mc.gameSettings.keyBindLeft);
        sync(mc.gameSettings.keyBindRight);
        sync(mc.gameSettings.keyBindJump);
        sync(mc.gameSettings.keyBindSneak);
        sync(mc.gameSettings.keyBindSprint);
    }

    private void sync(KeyBinding bind) {
        KeyBinding.setKeyBindState(bind.getKeyCode(), Keyboard.isKeyDown(bind.getKeyCode()));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int catY = panelY + 10;
        for (Category cat : Category.values()) {
            if (isHovered(panelX, catY, sidebarWidth, 20, mouseX, mouseY)) {
                parent.lastCategory = cat;
                selectedModule = null;
                moduleScrollOffset = 0;
                settingsScrollOffset = 0;
                return;
            }
            catY += 25;
        }

        int moduleListX = panelX + sidebarWidth + 5;
        int moduleListY = panelY + 5;
        int moduleListWidth = 150;
        int moduleListHeight = panelHeight - 10;

        if (mouseX >= moduleListX && mouseX <= moduleListX + moduleListWidth && mouseY >= moduleListY && mouseY <= moduleListY + moduleListHeight) {
            int currentModuleY = (int) (moduleListY + moduleScrollOffset);
            for (Module m : Claude.moduleManager.getAll()) {
                if (m.getCategory() != parent.lastCategory) continue;
                if (isHovered(moduleListX, currentModuleY, moduleListWidth, 20, mouseX, mouseY)) {
                    if (mouseButton == 0) m.toggle();
                    else if (mouseButton == 1) {
                        selectedModule = (selectedModule == m ? null : m);
                        settingsScrollOffset = 0;
                    }
                    return;
                }
                currentModuleY += 25;
            }
        }

        if (selectedModule != null) {
            int settingsPanelX = moduleListX + moduleListWidth + 5;
            int settingsPanelY = panelY + 5;
            int settingsPanelWidth = panelWidth - sidebarWidth - moduleListWidth - 15;
            int settingsPanelHeight = panelHeight - 10;
            if (mouseX >= settingsPanelX && mouseX <= settingsPanelX + settingsPanelWidth && mouseY >= settingsPanelY && mouseY <= settingsPanelY + settingsPanelHeight) {
                int currentSettingY = (int) (settingsPanelY + settingsScrollOffset);
                for (Setting s : selectedModule.settings) {
                    if (s instanceof BooleanSetting) {
                        BooleanSetting b = (BooleanSetting) s;

                        // Click anywhere on the full row (text, gap, or switch) to toggle
                        if (isHovered(settingsPanelX, currentSettingY, settingsPanelWidth, 15, mouseX, mouseY)) {
                            b.toggle();
                            booleanAnimationProgress.put(b, b.enabled ? 1.0f : 0.0f);
                            return;
                        }
                        currentSettingY += 18;
                    } else if (s instanceof NumberSetting) {
                        currentSettingY += 25;
                    } else if (s instanceof ModeSetting) {
                        if (isHovered(settingsPanelX, currentSettingY, settingsPanelWidth, 15, mouseX, mouseY)) ((ModeSetting) s).cycle();
                        currentSettingY += 18;
                    }
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int dWheel = Mouse.getDWheel();

        if (dWheel != 0) {
            int scrollAmount = dWheel / 120 * 15;
            int moduleListX = panelX + sidebarWidth + 5;
            int moduleListY = panelY + 5;
            int moduleListWidth = 150;
            int moduleListHeight = panelHeight - 10;
            if (mouseX >= moduleListX && mouseX <= moduleListX + moduleListWidth && mouseY >= moduleListY && mouseY <= mouseY + moduleListHeight) {
                moduleScrollOffset = MathHelper.clamp_float(moduleScrollOffset + scrollAmount, -maxModuleScroll, 0);
            }

            int settingsPanelX = moduleListX + moduleListWidth + 5;
            int settingsPanelY = panelY + 5;
            int settingsPanelWidth = panelWidth - sidebarWidth - moduleListWidth - 15;
            int settingsPanelHeight = panelHeight - 10;
            if (mouseX >= settingsPanelX && mouseX <= settingsPanelX + settingsPanelWidth && mouseY >= settingsPanelY && mouseY <= settingsPanelY + settingsPanelHeight) {
                settingsScrollOffset = MathHelper.clamp_float(settingsScrollOffset + scrollAmount, -maxSettingsScroll, 0);
            }
        }
    }

    private boolean isHovered(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public void onGuiClosed() {
        parent.setEnabled(false);
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
