package com.atl.ui.clickgui;

import com.atl.module.ExampleMod;
import com.atl.module.management.*;
import com.atl.module.modules.ClickGUI;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends GuiScreen {

    private final ClickGUI parent;
    private final List<Frame> frames;

    public ClickGuiScreen(ClickGUI parent) {
        this.parent = parent;
        this.frames = new ArrayList<>();
        int initialX = 20;
        int frameWidth = 100;
        int headerHeight = 16;

        for (Category cat : Category.values()) {
            frames.add(new Frame(cat, initialX, 20, frameWidth, headerHeight));
            initialX += frameWidth + 10;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        for (Frame frame : frames) {
            frame.render(mouseX, mouseY, fontRendererObj);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for (Frame frame : frames) {
            frame.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for (Frame frame : frames) {
            frame.mouseReleased(mouseX, mouseY, state);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        for (Frame frame : frames) {
            frame.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
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

    // Inner Frame Class
    private class Frame {
        public Category category;
        public int x, y;
        public int width, headerHeight;
        public boolean collapsed;
        public boolean dragging;
        public int dragX, dragY;
        private final List<Module> modules;

        public Frame(Category category, int x, int y, int width, int headerHeight) {
            this.category = category;
            this.x = x;
            this.y = y;
            this.width = width;
            this.headerHeight = headerHeight;
            this.collapsed = false;
            this.dragging = false;

            // Cache modules to avoid iterating the entire manager every frame
            this.modules = new ArrayList<>();
            for (Module m : ExampleMod.moduleManager.getAll()) {
                if (m.getCategory() == category) {
                    this.modules.add(m);
                }
            }
        }

        public void render(int mouseX, int mouseY, FontRenderer fontRenderer) {
            Gui.drawRect(x, y, x + width, y + headerHeight, 0xFF00AAFF);
            fontRenderer.drawStringWithShadow(category.name(), x + (width / 2) - (fontRenderer.getStringWidth(category.name()) / 2), y + 4, -1);

            int currentY = y + headerHeight;

            if (!collapsed) {
                for (Module m : modules) {
                    Gui.drawRect(x, currentY, x + width, currentY + headerHeight, m.isEnabled() ? 0xFF333333 : 0xFF1a1a1a);
                    fontRenderer.drawStringWithShadow(m.getName(), x + 4, currentY + 4, m.isEnabled() ? 0xFF55FF55 : -1);
                    currentY += headerHeight;

                    if (m.settingsExpanded) {
                        for (Setting s : m.settings) {
                            int sHeight = (s instanceof NumberSetting ? 14 : 12);
                            Gui.drawRect(x, currentY, x + width, currentY + sHeight, 0x90000000);

                            if (s instanceof BooleanSetting) {
                                BooleanSetting b = (BooleanSetting) s;
                                fontRenderer.drawStringWithShadow(s.name, x + 10, currentY + 2, b.enabled ? 0xFF55FF55 : 0xFF999999);
                            } else if (s instanceof NumberSetting) {
                                NumberSetting n = (NumberSetting) s;
                                double renderWidth = (width - 10) * ((n.value - n.min) / (n.max - n.min));
                                Gui.drawRect(x + 5, currentY + 10, x + width - 5, currentY + 12, 0xFF333333);
                                Gui.drawRect(x + 5, currentY + 10, x + 5 + (int) renderWidth, currentY + 12, 0xFF00AAFF);
                                fontRenderer.drawString(s.name + ": " + String.format("%.2f", n.value), x + 8, currentY + 1, -1, false);
                                
                                if (Mouse.isButtonDown(0) && mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + sHeight) {
                                    double diff = Math.min(width - 10, Math.max(0, mouseX - (x + 5)));
                                    n.setValue(((diff / (width - 10)) * (n.max - n.min)) + n.min);
                                }
                            }
                            currentY += sHeight;
                        }
                    }
                }
            }
        }

        public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
                if (mouseButton == 0) {
                    dragging = true;
                    dragX = mouseX - x;
                    dragY = mouseY - y;
                } else if (mouseButton == 1) {
                    collapsed = !collapsed;
                }
                return;
            }

            if (!collapsed) {
                int currentY = y + headerHeight;
                for (Module m : modules) {
                    if (mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + headerHeight) {
                        if (mouseButton == 0) m.toggle();
                        else if (mouseButton == 1) m.settingsExpanded = !m.settingsExpanded;
                        return;
                    }
                    currentY += headerHeight;

                    if (m.settingsExpanded) {
                        for (Setting s : m.settings) {
                            int sHeight = (s instanceof NumberSetting) ? 14 : 12;
                            if (mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + sHeight) {
                                if (s instanceof BooleanSetting && mouseButton == 0) {
                                    ((BooleanSetting) s).toggle();
                                    return; // Prevent clicking through to other elements
                                }
                            }
                            currentY += sHeight;
                        }
                    }
                }
            }
        }

        public void mouseReleased(int mouseX, int mouseY, int state) {
            dragging = false;
        }

        public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
            if (dragging && clickedMouseButton == 0) {
                x = mouseX - dragX;
                y = mouseY - dragY;
            }
        }
    }
}
