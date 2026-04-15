package com.atl.module.modules;

import com.atl.module.management.BooleanSetting;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AutoChest extends Module {

    private final Random random = new Random();

    // Tick-based delays (1 tick = ~50ms at 20 TPS)
    // Initial delay simulates the human reaction time after opening a chest
    private static final int MIN_INITIAL_DELAY_TICKS = 1;
    private static final int MAX_INITIAL_DELAY_TICKS = 1;

    // Per-click delay simulates human hand movement between shift-clicks
    private static final int MIN_CLICK_DELAY_TICKS = 1;
    private static final int MAX_CLICK_DELAY_TICKS = 1;

    private final List<Item> itemsToMove = Arrays.asList(
            Items.iron_ingot,
            Items.gold_ingot,
            Items.diamond,
            Items.emerald
    );

    private final BooleanSetting chestStealer = new BooleanSetting("ChestStealer", false);

    // Tick-based state machine — no background threads
    private ContainerChest activeContainer = null;
    private List<Integer> pendingSlots = new ArrayList<>();
    private int ticksUntilNextAction = 0;
    private boolean initialized = false;
    private boolean stealing = false;
    private boolean pendingClose = false;

    public AutoChest() {
        super("AutoChest", "Automatically moves valuable items to opened chests", Category.MISC);
        addSettings(chestStealer);
    }

    @Override
    public void loadSettings(JsonObject settings) {
        if (settings.has("chestStealer")) {
            this.chestStealer.enabled = settings.get("chestStealer").getAsBoolean();
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject settings = new JsonObject();
        settings.addProperty("chestStealer", chestStealer.isEnabled());
        return settings;
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4) return false;
        if (parts[2].equalsIgnoreCase("cheststealer")) {
            this.chestStealer.enabled = Boolean.parseBoolean(parts[3]);
            return true;
        }
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("chestStealer (true/false) - Current: " + chestStealer.isEnabled());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (!(mc.currentScreen instanceof GuiChest) || !(mc.thePlayer.openContainer instanceof ContainerChest)) {
            if (activeContainer != null) {
                resetState();
            }
            return;
        }

        ContainerChest container = (ContainerChest) mc.thePlayer.openContainer;

        if (container != activeContainer) {
            resetState();
            activeContainer = container;
            // Randomized initial delay before the first click — critical for bypassing
            // server-side checks that flag immediate post-open window clicks
            ticksUntilNextAction = MIN_INITIAL_DELAY_TICKS;
            return;
        }

        if (ticksUntilNextAction > 0) {
            ticksUntilNextAction--;
            return;
        }

        if (!initialized) {
            initialized = true;
            prepareSlots(container);
            if (pendingSlots.isEmpty()) {
                closeChest(mc);
                return;
            }
            ticksUntilNextAction = MIN_CLICK_DELAY_TICKS
                    + random.nextInt(MAX_CLICK_DELAY_TICKS - MIN_CLICK_DELAY_TICKS + 1);
            return;
        }

        if (pendingClose) {
            closeChest(mc);
            return;
        }

        if (pendingSlots.isEmpty()) return;

        int slotIndex = pendingSlots.remove(0);

        if (slotIndex >= container.inventorySlots.size()) {
            scheduleNextClick();
            return;
        }

        Slot slot = container.getSlot(slotIndex);

        if (slot == null || !slot.getHasStack()) {
            scheduleNextClick();
            return;
        }

        // Copy stack info before the click for the chat message
        ItemStack stack = slot.getStack().copy();

        // Shift-click: button=0, mode=1 — identical to a player pressing Shift+Left Click
        mc.playerController.windowClick(container.windowId, slotIndex, 0, 1, mc.thePlayer);

        String action = stealing ? "Stole " : "Moved ";
        String direction = stealing ? " from chest." : " to chest.";
        sendMessage(EnumChatFormatting.GREEN + action + stack.getDisplayName() + direction);

        if (pendingSlots.isEmpty()) {
            pendingClose = true;
            ticksUntilNextAction = MIN_CLICK_DELAY_TICKS
                    + random.nextInt(MAX_CLICK_DELAY_TICKS - MIN_CLICK_DELAY_TICKS + 1);
        } else {
            scheduleNextClick();
        }
    }

    private void prepareSlots(ContainerChest container) {
        List<Integer> playerValuables = findPlayerValuables(container);

        if (!playerValuables.isEmpty()) {
            stealing = false;
            pendingSlots = playerValuables;
        } else if (chestStealer.isEnabled()) {
            List<Integer> chestValuables = findChestValuables(container);
            if (!chestValuables.isEmpty()) {
                stealing = true;
                pendingSlots = chestValuables;
            }
        }

        // Slight shuffle to avoid perfectly sequential slot order, which looks robotic
        if (pendingSlots.size() > 2) {
            shuffleSlightly(pendingSlots);
        }
    }

    private void scheduleNextClick() {
        ticksUntilNextAction = MIN_CLICK_DELAY_TICKS
                + random.nextInt(MAX_CLICK_DELAY_TICKS - MIN_CLICK_DELAY_TICKS + 1);
    }

    private void closeChest(Minecraft mc) {
        mc.thePlayer.closeScreen();
        resetState();
    }

    private void resetState() {
        activeContainer = null;
        pendingSlots.clear();
        ticksUntilNextAction = 0;
        initialized = false;
        stealing = false;
        pendingClose = false;
    }

    private List<Integer> findPlayerValuables(ContainerChest container) {
        List<Integer> slots = new ArrayList<>();
        int chestSize = container.getLowerChestInventory().getSizeInventory();

        for (int i = chestSize; i < container.inventorySlots.size(); i++) {
            Slot slot = container.getSlot(i);
            if (slot != null && slot.getHasStack() && itemsToMove.contains(slot.getStack().getItem())) {
                slots.add(i);
            }
        }
        return slots;
    }

    private List<Integer> findChestValuables(ContainerChest container) {
        List<Integer> slots = new ArrayList<>();
        int chestSize = container.getLowerChestInventory().getSizeInventory();

        for (int i = 0; i < chestSize; i++) {
            Slot slot = container.getSlot(i);
            if (slot != null && slot.getHasStack() && itemsToMove.contains(slot.getStack().getItem())) {
                slots.add(i);
            }
        }
        return slots;
    }

    /**
     * Lightly shuffles a list by swapping a small number of adjacent pairs.
     * This avoids perfectly linear slot iteration (bot-like) without being
     * so random it looks unnatural.
     */
    private void shuffleSlightly(List<Integer> slots) {
        int swaps = 1 + random.nextInt(Math.max(1, slots.size() / 3));
        for (int i = 0; i < swaps; i++) {
            int a = random.nextInt(slots.size() - 1);
            Collections.swap(slots, a, a + 1);
        }
    }

    private void sendMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.thePlayer.addChatMessage(new ChatComponentText(
                EnumChatFormatting.GRAY + "[" +
                        EnumChatFormatting.LIGHT_PURPLE + "AutoChest" +
                        EnumChatFormatting.GRAY + "] " +
                        EnumChatFormatting.RESET + message
        ));
    }
}
