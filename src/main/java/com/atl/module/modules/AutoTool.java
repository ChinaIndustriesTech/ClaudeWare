package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.BooleanSetting;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoTool extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final int JITTER = 73; // +/- ms

    private int savedSlot    = -1;
    private boolean switching = false;
    private BooleanSetting swapBlocks = new BooleanSetting("SwapBlocks", true); // Setting for swapping block stacks

    public AutoTool() {
        super("AutoTool", "Automatically switches to the best tool", Category.PLAYER);
        addSettings(swapBlocks);
    }

    @Override
    public void loadSettings(JsonObject settings) {
        if (settings.has("swapBlocks")) {
            this.swapBlocks.enabled = settings.get("swapBlocks").getAsBoolean();
        }
    }

    @Override
    public JsonObject saveSettings() {
        JsonObject settings = new JsonObject();
        settings.addProperty("swapBlocks", swapBlocks.isEnabled());
        return settings;
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4) return false;
        if (parts[2].equalsIgnoreCase("swapblocks")) {
            this.swapBlocks.enabled = Boolean.parseBoolean(parts[3]);
            return true;
        }
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList("swapBlocks (true/false) - Current: " + swapBlocks.isEnabled());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!isEnabled()) return;
        if (event.phase != TickEvent.Phase.END) return;

        if (mc.currentScreen != null) {
            if (savedSlot != -1 && !switching) {
                mc.thePlayer.inventory.currentItem = savedSlot;
                savedSlot = -1;
            }
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null) return;

        // Block Swapping Logic (if enabled)
        if (swapBlocks.isEnabled() && Mouse.isButtonDown(1)) {
            ItemStack currentStack = player.inventory.getCurrentItem();
            if (currentStack != null && currentStack.getItem() instanceof ItemBlock && currentStack.stackSize <= 2) {
                int nextBlockSlot = findNextBlockStack(player, player.inventory.currentItem);
                if (nextBlockSlot != -1) {
                    player.inventory.currentItem = nextBlockSlot;
                    // We don't saveSlot here as this is a permanent swap while bridging/building
                    return;
                }
            }
        }

        if (!Mouse.isButtonDown(0)) {
            if (savedSlot != -1 && !switching) {
                player.inventory.currentItem = savedSlot;
                savedSlot = -1;
            }
            return;
        }

        if (!Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            if (savedSlot != -1 && !switching) {
                player.inventory.currentItem = savedSlot;
                savedSlot = -1;
            }
            return;
        }

        if (mc.objectMouseOver == null) return;

        int bestSlot = -1;

        if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            Block block = mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock();
            bestSlot = getBestToolSlot(block);
        } else if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            if (mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
                bestSlot = getBestSwordSlot();
            }
        }

        if (bestSlot != -1 && bestSlot != player.inventory.currentItem && !switching) {
            if (savedSlot == -1) savedSlot = player.inventory.currentItem;

            final int targetSlot = bestSlot;
            switching = true;

            long delay = (random.nextInt(JITTER * 2 + 1)); 

            scheduler.schedule(() -> {
                mc.addScheduledTask(() -> {
                    player.inventory.currentItem = targetSlot;
                    switching = false;
                });
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    private int findNextBlockStack(EntityPlayerSP player, int currentSlot) {
        for (int i = 0; i < 9; i++) {
            if (i == currentSlot) continue;
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock && stack.stackSize > 2) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null && savedSlot != -1) {
            mc.thePlayer.inventory.currentItem = savedSlot;
            savedSlot  = -1;
            switching  = false;
        }
    }

    private int getBestToolSlot(Block block) {
        int bestSlot = -1;
        float bestSpeed = -1f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null) continue;

            float speed = stack.getItem().getStrVsBlock(stack, block);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot  = i;
            }
        }

        if (bestSpeed <= 1.0f) return -1;
        return bestSlot;
    }

    private int getBestSwordSlot() {
        int bestSlot = -1;
        float bestDamage = -1f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null) continue;
            if (!(stack.getItem() instanceof ItemSword)) continue;

            float damage = (float) stack.getItem()
                    .getItemAttributeModifiers()
                    .get("generic.attackDamage")
                    .stream()
                    .findFirst()
                    .map(mod -> mod.getAmount())
                    .orElse(0.0)
                    .floatValue();

            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot   = i;
            }
        }

        return bestSlot;
    }
}
