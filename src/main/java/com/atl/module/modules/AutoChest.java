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
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoChest extends Module {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Random random = new Random();
    private static final int BASE_DELAY_MS = 50; // Base delay between actions
    private static final int JITTER_MS = 100; // Max random additional delay

    private final List<Item> itemsToMove = Arrays.asList(
            Items.iron_ingot,
            Items.gold_ingot,
            Items.diamond,
            Items.emerald
    );

    private final BooleanSetting chestStealer = new BooleanSetting("ChestStealer", false);

    private ContainerChest lastProcessedContainer = null;

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

        // Check if we are currently looking at a chest GUI
        if (mc.currentScreen instanceof GuiChest) {
            if (mc.thePlayer.openContainer instanceof ContainerChest) {
                ContainerChest container = (ContainerChest) mc.thePlayer.openContainer;

                // Only trigger if this is a new chest we haven't touched yet
                if (container != lastProcessedContainer) {
                    lastProcessedContainer = container;

                    List<Integer> playerValuables = findItemsToMove(mc, container);

                    if (chestStealer.isEnabled() && playerValuables.isEmpty()) {
                        List<Integer> chestValuables = findItemsToSteal(container);
                        if (!chestValuables.isEmpty()) {
                            scheduleItemMoves(mc, container, chestValuables, 0, 1);
                        }
                    } else if (!playerValuables.isEmpty()) {
                        scheduleItemMoves(mc, container, playerValuables, 0, 0);
                    }
                }
            }
        } else {
            // Reset the tracker when the GUI is closed
            lastProcessedContainer = null;
        }
    }

    private List<Integer> findItemsToMove(Minecraft mc, ContainerChest container) {
        List<Integer> slotsToMove = new ArrayList<>();
        // The chest's slots are typically at the beginning of the container's slots
        // Player inventory slots start after the chest slots
        int chestSlotsCount = container.getLowerChestInventory().getSizeInventory();

        // Iterate through player's inventory slots (including hotbar)
        // Player inventory slots are usually from chestSlotsCount to container.inventorySlots.size() - 1
        for (int i = chestSlotsCount; i < container.inventorySlots.size(); i++) {
            Slot slot = container.getSlot(i);
            if (slot != null && slot.getHasStack()) {
                ItemStack stack = slot.getStack();
                if (itemsToMove.contains(stack.getItem())) {
                    slotsToMove.add(i);
                }
            }
        }
        return slotsToMove;
    }

    private List<Integer> findItemsToSteal(ContainerChest container) {
        List<Integer> slotsToMove = new ArrayList<>();
        int chestSlotsCount = container.getLowerChestInventory().getSizeInventory();

        // Iterate through the chest's inventory slots (top half of the GUI)
        for (int i = 0; i < chestSlotsCount; i++) {
            Slot slot = container.getSlot(i);
            if (slot != null && slot.getHasStack()) {
                ItemStack stack = slot.getStack();
                if (itemsToMove.contains(stack.getItem())) {
                    slotsToMove.add(i);
                }
            }
        }
        return slotsToMove;
    }

    private void scheduleItemMoves(Minecraft mc, ContainerChest container, List<Integer> slots, int index, int mouseButton) {
        if (index >= slots.size()) {
            return;
        }

        // Randomized delay between 50ms and 150ms for human emulation
        int slotToMove = slots.get(index);
        long delay = BASE_DELAY_MS + random.nextInt(JITTER_MS + 1);
        boolean isStealing = (mouseButton == 1); // This boolean is now only for message formatting

        scheduler.schedule(() -> {
            mc.addScheduledTask(() -> {
                // Check if we should stop moving items
                if (isEnabled() && mc.thePlayer != null && mc.thePlayer.openContainer == container) {
                    ItemStack stackBeforeMove = container.getSlot(slotToMove).getStack();

                    mc.playerController.windowClick(container.windowId, slotToMove, 0, 1, mc.thePlayer); // Always Left-Click for Shift-Click
                    
                    if (stackBeforeMove != null && !container.getSlot(slotToMove).getHasStack()) {
                        String direction = isStealing ? " from chest." : " to chest.";
                        String action = isStealing ? "Stole " : "Moved ";
                        sendMessage(EnumChatFormatting.GREEN + action + stackBeforeMove.getDisplayName() + direction);
                    }

                    // Only schedule the next move if the container is still valid
                    scheduleItemMoves(mc, container, slots, index + 1, mouseButton);
                }
            });
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void sendMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft(); // Lazy load mc here too
        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[" + EnumChatFormatting.LIGHT_PURPLE + "AutoChest" + EnumChatFormatting.GRAY + "] " + EnumChatFormatting.RESET + message));
    }
}
