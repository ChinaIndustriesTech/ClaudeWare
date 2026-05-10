package com.atl.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FakeGuiInventory extends GuiScreen {

    private static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation("textures/gui/container/inventory.png");

    private final Minecraft mc = Minecraft.getMinecraft();
    private final EntityPlayer player;
    private final List<SlotOperation> recordedOps = new ArrayList<>();

    private int guiLeft, guiTop;
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private final int[][] mainSlots = new int[27][2];
    private final int[][] hotbarSlots = new int[9][2];

    public FakeGuiInventory(EntityPlayer player) {
        this.player = player;
    }

    @Override
    public void initGui() {
        super.initGui();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                mainSlots[index][0] = guiLeft + 8 + col * 18;
                mainSlots[index][1] = guiTop + 84 + row * 18;
            }
        }

        for (int col = 0; col < 9; col++) {
            hotbarSlots[col][0] = guiLeft + 8 + col * 18;
            hotbarSlots[col][1] = guiTop + 142;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        syncMovement();

        drawDefaultBackground();
        mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        for (int i = 0; i < 27; i++) {
            int slotIndex = i + 9;
            ItemStack stack = player.inventory.getStackInSlot(slotIndex);
            if (stack != null) {
                mc.getRenderItem().renderItemIntoGUI(stack, mainSlots[i][0], mainSlots[i][1]);
                mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, mainSlots[i][0], mainSlots[i][1]);
            }
            if (isHovered(mainSlots[i][0], mainSlots[i][1], mouseX, mouseY)) {
                drawSlotHighlight(mainSlots[i][0], mainSlots[i][1]);
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack != null) {
                mc.getRenderItem().renderItemIntoGUI(stack, hotbarSlots[i][0], hotbarSlots[i][1]);
                mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, hotbarSlots[i][0], hotbarSlots[i][1]);
            }
            if (isHovered(hotbarSlots[i][0], hotbarSlots[i][1], mouseX, mouseY)) {
                drawSlotHighlight(hotbarSlots[i][0], hotbarSlots[i][1]);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSlotHighlight(int x, int y) {
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        drawRect(x, y, x + 16, y + 16, 0x80FFFFFF);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
    }

    // THIS is the key fix — sync movement on every input event, not just render
    @Override
    public void handleKeyboardInput() throws IOException {
        syncMovement();

        // Close on inventory key press
        if (Keyboard.getEventKeyState() &&
                Keyboard.getEventKey() == mc.gameSettings.keyBindInventory.getKeyCode()) {
            mc.displayGuiScreen(null);
            return;
        }

        super.handleKeyboardInput();
    }

    @Override
    public void handleMouseInput() throws IOException {
        syncMovement();
        super.handleMouseInput();
    }

    private void syncMovement() {
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
        for (int i = 0; i < 27; i++) {
            if (isHovered(mainSlots[i][0], mainSlots[i][1], mouseX, mouseY)) {
                recordedOps.add(new SlotOperation(i + 9, mouseButton, isShiftKeyDown() ? 1 : 0));
                return;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (isHovered(hotbarSlots[i][0], hotbarSlots[i][1], mouseX, mouseY)) {
                recordedOps.add(new SlotOperation(i + 36, mouseButton, isShiftKeyDown() ? 1 : 0));
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
        if (!recordedOps.isEmpty()) {
            InventoryMoveReplayer.schedule(new ArrayList<>(recordedOps));
            recordedOps.clear();
        }
        super.onGuiClosed();
    }

    private boolean isHovered(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static class SlotOperation {
        public final int slot;
        public final int button;
        public final int mode;

        public SlotOperation(int slot, int button, int mode) {
            this.slot = slot;
            this.button = button;
            this.mode = mode;
        }
    }
}