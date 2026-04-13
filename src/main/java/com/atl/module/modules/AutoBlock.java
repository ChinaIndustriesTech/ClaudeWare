package com.atl.module.modules;

import com.atl.mixin.IMinecraft;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class AutoBlock extends Module {

    private final NumberSetting range = new NumberSetting("Range", 3.8, 1.0, 6.0, 0.1);
    private final NumberSetting blockDuration = new NumberSetting("Block Duration", 90, 50, 500, 10);

    private long blockEndTime = 0;
    private boolean pendingBlock = false;
    private final Random random = new Random();

    public AutoBlock() {
        super("AutoBlock", "BROKEN DONT USE", Category.COMBAT);
        addSettings(range, blockDuration);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    /**
     * Called by MixinNetHandlerPlayClient at the exact millisecond 
     * an animation packet is received.
     */
    public void handleEntitySwing(EntityPlayer opponent) {
        Minecraft mc = Minecraft.getMinecraft();

        // 1. Basic checks: Must be holding sword and attacking
        boolean holdingSword = mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
        if (!holdingSword || !Mouse.isButtonDown(0) || mc.currentScreen != null) return;

        // 2. Reach Check
        double distSq = mc.thePlayer.getDistanceSqToEntity(opponent);
        if (distSq > range.value * range.value) return;

        // 3. Rotational Check (Dot Product)
        if (isEntityFacingMe(opponent)) {
            // If we have high invincibility, queue the block for later
            if (mc.thePlayer.hurtTime > 1) {
                pendingBlock = true;
            } else {
                block(mc);
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || !isEnabled()) return;

        long currentTime = System.currentTimeMillis();

        // Handle HurtTime logic: Release block during high invincibility
        if (mc.thePlayer.hurtTime > 1) {
            blockEndTime = 0; // Force release
        }

        // Trigger pending block right before hurtTime expires (hurtTime 1 = ~50ms left)
        // Jitter added: 50ms (the tick) +/- ~30ms via small logic variations
        if (pendingBlock && mc.thePlayer.hurtTime <= 1) {
            block(mc);
            pendingBlock = false;
        }

        // Maintain or release the block state
        int bind = mc.gameSettings.keyBindUseItem.getKeyCode();
        if (currentTime < blockEndTime) {
            KeyBinding.setKeyBindState(bind, true);
        } else if (blockEndTime != 0) {
            // Only release if the user isn't manually holding right click
            KeyBinding.setKeyBindState(bind, Mouse.isButtonDown(1));
            blockEndTime = 0;
        }
    }

    private boolean isEntityFacingMe(EntityPlayer opponent) {
        Minecraft mc = Minecraft.getMinecraft();

        // Vector from opponent to us
        Vec3 diff = new Vec3(
                mc.thePlayer.posX - opponent.posX,
                (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()) - (opponent.posY + opponent.getEyeHeight()),
                mc.thePlayer.posZ - opponent.posZ
        ).normalize();

        // Opponent's looking direction
        Vec3 look = opponent.getLookVec();

        // Dot product: 1.0 = perfectly facing, 0.0 = perpendicular, -1.0 = facing away
        // 0.8 is roughly a 36-degree cone.
        double dot = look.dotProduct(diff);
        return dot > 0.5;
    }

    private void block(Minecraft mc) {
        IMinecraft imc = (IMinecraft) mc;
        imc.setRightClickDelayTimer(0);
        imc.invokeRightClickMouse();
        
        // Set the timer for how long we should hold the block
        this.blockEndTime = System.currentTimeMillis() + (long) blockDuration.value;
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
                "Range (range/dist): " + range.value,
                "Duration (blockduration/duration/ms): " + blockDuration.value + "ms"
        );
    }

    @Override
    public boolean handleSetCommand(String[] parts) {
        if (parts.length < 4) return false;
        try {
            String setting = parts[2].toLowerCase();
            double val = Double.parseDouble(parts[3]);
            if (setting.equals("range") || setting.equals("dist")) {
                range.setValue(val);
                return true;
            } else if (setting.equals("blockduration") || setting.equals("duration") || setting.equals("ms")) {
                blockDuration.setValue(val);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
