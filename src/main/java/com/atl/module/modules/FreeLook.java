package com.atl.module.modules;

import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;


import java.util.Arrays;
import java.util.List;


public class FreeLook extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public float freeYaw, freePitch;
    public float prevFreeYaw, prevFreePitch;
    private int previousPerspective = 0;

    public FreeLook() {
        super("FreeLook", "Look around in 3rd person without changing your direction", Category.RENDER);
    }

    @Override
    public void loadSettings(JsonObject settings) {
      
    }

    @Override
    public JsonObject saveSettings() {
        return new JsonObject();
    }
    

    @Override
    public boolean handleSetCommand(String[] parts) {
        return false;
    }
    
    @Override
    public List<String> getSettings() {
        return Arrays.asList("No configurable settings.");
    }

   public boolean isHoldModule() {
return true;
}   
   @Override
    public void onEnable() {
        if (mc.thePlayer == null) return;

        // Save the current perspective (0 = 1st person, 1 = 3rd person, 2 = 3rd person back)
        previousPerspective = mc.gameSettings.thirdPersonView;

        // Force 3rd person view
        mc.gameSettings.thirdPersonView = 1;

        // Initialize camera angles to current player angles
        freeYaw = prevFreeYaw = mc.thePlayer.rotationYaw;
        freePitch = prevFreePitch = mc.thePlayer.rotationPitch;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || event.phase != TickEvent.Phase.START || !isEnabled()) return;
        
        prevFreeYaw = freeYaw;
        prevFreePitch = freePitch;
    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Specials.Pre event) {
        // Cancel only the local player's nametag rendering if FreeLook is active
        if (isEnabled() && event.entity == mc.thePlayer) {
            event.setCanceled(true);
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) return;

        // Restore the original perspective
        mc.gameSettings.thirdPersonView = previousPerspective;
    }

    // This is called by the Mixin to update our angles
    public void handleMouseInput(float deltaYaw, float deltaPitch) {
        this.freeYaw += deltaYaw * 0.15F;
        this.freePitch -= deltaPitch * 0.15F;

        // Clamp pitch to prevent the camera from flipping upside down
        if (this.freePitch > 90.0F) this.freePitch = 90.0F;
        if (this.freePitch < -90.0F) this.freePitch = -90.0F;
    }
}
