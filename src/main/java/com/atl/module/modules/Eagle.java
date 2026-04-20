package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.BooleanSetting;
import com.atl.module.management.NumberSetting;
import com.google.gson.JsonObject;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Arrays;
import java.util.List;

public class Eagle extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();
    
    public NumberSetting edgeOffset = new NumberSetting("Edge Offset", 0.0, 0.0, 0.3, 0.01);
    public BooleanSetting randomize = new BooleanSetting("Randomize", false);
    public BooleanSetting blockCounter = new BooleanSetting("Block Counter", true);
    public BooleanSetting onlySneaking = new BooleanSetting("Only Sneaking", true);
    public NumberSetting sneakDelay = new NumberSetting("Sneak Delay", 50.0, 0.0, 500.0, 1.0);

    public Eagle() {
        super("Eagle", "legit scaffold", Category.MOVEMENT);
        addSettings(edgeOffset, randomize, blockCounter, onlySneaking, sneakDelay);
    }

    private long nextActionTime = 0L;
    private float currentOffset = 0.0f;
    private final java.util.Random random = new java.util.Random();

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 3) return false;
        String setting = parts[2].toLowerCase();
        
        if (setting.equals("edgeoffset")) {
            try {
                float value = Float.parseFloat(parts[3]);
                this.edgeOffset.setValue(value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (setting.equals("randomize")) {
            this.randomize.enabled = Boolean.parseBoolean(parts[3]);
            return true;
        } else if (setting.equals("blockcounter")) {
            this.blockCounter.enabled = Boolean.parseBoolean(parts[3]);
            return true;
        } else if (setting.equals("onlysneaking")) {
            this.onlySneaking.enabled = Boolean.parseBoolean(parts[3]);
            return true;
        } else if (setting.equals("sneakdelay") || setting.equals("delay")) {
            try {
                float value = Float.parseFloat(parts[3]);
                this.sneakDelay.setValue(value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
            "edgeOffset (0.0 - 0.3) - Current: " + edgeOffset.value,
            "randomize (true/false) - Current: " + randomize.enabled,
            "blockCounter (true/false) - Current: " + blockCounter.enabled,
            "onlySneaking (true/false) - Current: " + onlySneaking.enabled,
            "sneakDelay (ms) - Current: " + sneakDelay.value
        );
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END) return;

        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null) return;

        int sneakKey = mc.gameSettings.keyBindSneak.getKeyCode();
        boolean physicalSneak = Keyboard.isKeyDown(sneakKey);
        boolean canEagle = !onlySneaking.isEnabled() || physicalSneak;

        if (canEagle) {
            boolean isMovingBackwards = player.movementInput.moveForward < 0;
            boolean isLookingDown = player.rotationPitch > 60.0F;

            boolean holdingBlock = player.getHeldItem() != null &&
                    player.getHeldItem().getItem() instanceof ItemBlock;

            if (isMovingBackwards && isLookingDown && holdingBlock) {
                double yaw = Math.toRadians(player.rotationYaw);

                float offsetToUse = randomize.enabled ? currentOffset : (float) edgeOffset.value;
                
                double checkX = player.posX + (Math.sin(yaw) * offsetToUse);
                double checkZ = player.posZ + (-Math.cos(yaw) * offsetToUse);
                
                BlockPos below = new BlockPos(checkX, player.posY - 0.5D, checkZ);
                boolean isAirBelow = mc.theWorld.getBlockState(below).getBlock() instanceof BlockAir;

                if (System.currentTimeMillis() >= nextActionTime) {
                    if (isAirBelow) {
                        if (!mc.gameSettings.keyBindSneak.isKeyDown()) {
                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
                            updateDelay();
                            if (randomize.enabled) {
                                currentOffset = random.nextFloat() * 0.3f;
                            }
                        }
                    } else {
                        if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
                            updateDelay();
                        }
                    }
                }
            } else {
                KeyBinding.setKeyBindState(sneakKey, !onlySneaking.isEnabled() || physicalSneak);
            }
        } else {
            KeyBinding.setKeyBindState(sneakKey, physicalSneak);
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled() || !blockCounter.enabled || event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        boolean canEagle = !onlySneaking.isEnabled() || Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
        boolean isLookingDown = player.rotationPitch > 60.0F;

        if (!canEagle || !isLookingDown) return;

        int totalBlocks = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                totalBlocks += stack.stackSize;
            }
        }

        if (totalBlocks > 0) {
            ScaledResolution sr = new ScaledResolution(mc);
            String text = String.valueOf(totalBlocks);
            int x = sr.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(text) / 2;
            int y = sr.getScaledHeight() / 2 + 10;
            
            int color = 0xFFFFFFFF;
            if (totalBlocks <= 16) color = 0xFFFF0000;
            else if (totalBlocks <= 64) color = 0xFFFFFF00;
            else color = 0xFF00FF00;

            mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        }
    }

    private void updateDelay() {
        this.nextActionTime = System.currentTimeMillis() + (long) sneakDelay.value + random.nextInt(20);
    }
}
