package com.atl.ui.clickgui;

import com.atl.module.Claude;
import com.atl.module.management.*;
import com.atl.module.modules.ClickGUI;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
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
        int frameWidth = 105;
        int headerHeight = 18;

        for (Category cat : Category.values()) {
            frames.add(new Frame(cat, initialX, 20, frameWidth, headerHeight));
            initialX += frameWidth + 15;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        handleMovement();

        for (Frame frame : frames) {
            frame.render(mouseX, mouseY, fontRendererObj);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void handleMovement() {
        if (mc.thePlayer == null) return;
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
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
    public void onGuiClosed() {
        parent.setEnabled(false);
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private class Frame {
        public Category category;
        public int x, y;
        public int width, headerHeight;
        public boolean collapsed;
        public boolean dragging;
        public int dragX, dragY;

        public Frame(Category category, int x, int y, int width, int headerHeight) {
            this.category = category;
            this.x = x;
            this.y = y;
            this.width = width;
            this.headerHeight = headerHeight;
            this.collapsed = false;
        }

        public void render(int mouseX, int mouseY, FontRenderer fr) {
            if (this.dragging) {
                this.x = mouseX - dragX;
                this.y = mouseY - dragY;
            }

            int themeColor = parent.getThemeColor();
            int headerTextColor = parent.getHeaderTextColor();
            int backgroundColor = 0xCC101010;

            Gui.drawRect(x - 2, y - 2, x + width + 2, y + headerHeight, themeColor);
            fr.drawString(category.name(), (int)(x + (width / 2.0f) - (fr.getStringWidth(category.name()) / 2.0f)), y + 5, headerTextColor);

            int currentY = y + headerHeight;

            if (!collapsed) {
                Gui.drawRect(x - 2, currentY, x + width + 2, currentY + getFrameHeight(), backgroundColor);

                for (Module m : Claude.moduleManager.getAll()) {
                    if (m.getCategory() != category) continue;

                    boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + headerHeight;
                    int moduleColor = m.isEnabled() ? themeColor : (hovered ? 0xFF353535 : 0xFF1a1a1a);
                    
                    Gui.drawRect(x, currentY, x + width, currentY + headerHeight - 1, moduleColor);
                    fr.drawStringWithShadow(m.getName(), x + 5, currentY + 5, m.isEnabled() ? -1 : 0xFFBBBBBB);

                    currentY += headerHeight;

                    if (m.settingsExpanded) {
                        for (Setting s : m.settings) {
                            int sHeight = (s instanceof NumberSetting ? 16 : 14);
                            Gui.drawRect(x, currentY, x + width, currentY + sHeight, 0x40000000);

                            if (s instanceof BooleanSetting) {
                                BooleanSetting b = (BooleanSetting) s;
                                fr.drawStringWithShadow(s.name, x + 10, currentY + 3, b.enabled ? themeColor : 0xFF777777);
                            } else if (s instanceof NumberSetting) {
                                NumberSetting n = (NumberSetting) s;
                                double renderWidth = (width - 14) * ((n.value - n.min) / (n.max - n.min));
                                
                                fr.drawString(s.name + ": " + String.format("%.2f", n.value), x + 8, currentY + 1, 0xFFAAAAAA, false);
                                Gui.drawRect(x + 7, currentY + 11, x + width - 7, currentY + 13, 0xFF333333);
                                Gui.drawRect(x + 7, currentY + 11, x + 7 + (int) renderWidth, currentY + 13, themeColor);
                                
                                if (Mouse.isButtonDown(0) && mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + sHeight) {
                                    double diff = Math.min(width - 14, Math.max(0, mouseX - (x + 7)));
                                    n.setValue(((diff / (width - 14)) * (n.max - n.min)) + n.min);
                                }
                            } else if (s instanceof ModeSetting) {
                                ModeSetting ms = (ModeSetting) s;
                                fr.drawStringWithShadow(s.name + ": ", x + 10, currentY + 3, 0xFFBBBBBB);
                                fr.drawStringWithShadow(ms.getValue(), x + 12 + fr.getStringWidth(s.name + ": "), currentY + 3, themeColor);
                            }
                            currentY += sHeight;
                        }
                        Gui.drawRect(x, currentY, x + width, currentY + 2, 0x20FFFFFF);
                        currentY += 2;
                    }
                }
            }
        }

        private int getFrameHeight() {
            int height = 0;
            for (Module m : Claude.moduleManager.getAll()) {
                if (m.getCategory() == category) {
                    height += headerHeight;
                    if (m.settingsExpanded) {
                        for (Setting s : m.settings) {
                            height += (s instanceof NumberSetting ? 16 : 14);
                        }
                        height += 2;
                    }
                }
            }
            return height;
        }

        public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
            if (mouseX >= x - 2 && mouseX <= x + width + 2 && mouseY >= y - 2 && mouseY <= y + headerHeight) {
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
                for (Module m : Claude.moduleManager.getAll()) {
                    if (m.getCategory() != category) continue;

                    if (mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + headerHeight) {
                        if (mouseButton == 0) m.toggle();
                        else if (mouseButton == 1) m.settingsExpanded = !m.settingsExpanded;
                        return;
                    }
                    currentY += headerHeight;

                    if (m.settingsExpanded) {
                        for (Setting s : m.settings) {
                            int sHeight = (s instanceof NumberSetting ? 16 : 14);
                            if (mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + sHeight) {
                                if (s instanceof BooleanSetting) ((BooleanSetting) s).toggle();
                                else if (s instanceof ModeSetting) ((ModeSetting) s).cycle();
                            }
                            currentY += sHeight;
                        }
                        currentY += 2;
                    }
                }
            }
        }

        public void mouseReleased(int mouseX, int mouseY, int state) {
            dragging = false;
        }
    }
}
