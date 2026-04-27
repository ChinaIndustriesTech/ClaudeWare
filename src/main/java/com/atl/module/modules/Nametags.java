package com.atl.module.modules;

import com.atl.module.management.BooleanSetting;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import com.atl.module.management.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

public class Nametags extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public BooleanSetting scaling = new BooleanSetting("Scaling", true);
    public NumberSetting scaleFactor = new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.1);
    public BooleanSetting healthNumber = new BooleanSetting("Health Number", true);

    public Nametags() {
        super("Nametags", "Renders better nametags on players", Category.RENDER);
        addSettings(scaling, scaleFactor, healthNumber);
    }

    @Override
    public List<String> getSettings() {
        return Arrays.asList(
                "scaling: " + scaling.enabled,
                "scale: " + scaleFactor.value,
                "healthNumber: " + healthNumber.enabled
        );
    }

    private int getHealthColor(float hp, float maxHp) {
        float ratio = hp / maxHp;
        return Color.HSBtoRGB(Math.max(0f, Math.min(ratio, 1f)) / 3.0f, 1.0f, 1.0f);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!isEnabled()) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || !player.isEntityAlive()) continue;
            if (AntiBot.isBot(player)) continue;

            RenderManager rm = mc.getRenderManager();

            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks - rm.viewerPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks - rm.viewerPosY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks - rm.viewerPosZ;

            y += player.height + 0.5;

            String name = player.getDisplayName().getFormattedText();
            double distance = mc.thePlayer.getDistanceToEntity(player);

            // Build display string
            String displayText;
            if (healthNumber.enabled) {
                float hp = player.getHealth();
                float absAmount = player.getAbsorptionAmount();
                String hpText = (int)(hp + absAmount) + "§c❤";
                displayText = name + " " + hpText;
            } else {
                displayText = name;
            }

            int textWidth = mc.fontRendererObj.getStringWidth(displayText);

            double baseScale;
            if (scaling.enabled) {
                baseScale = Math.pow(Math.min(Math.max(distance, 6.0), 128.0), 0.75) * 0.0075 * scaleFactor.value;
            } else {
                baseScale = 0.025;
            }

            float viewRotation = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(rm.playerViewX, viewRotation, 0.0F, 0.0F);
            GlStateManager.scale(-baseScale, -baseScale, 1.0);

            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.enableAlpha();

            if (healthNumber.enabled) {
                // Render name in white
                int nameWidth = mc.fontRendererObj.getStringWidth(name);
                mc.fontRendererObj.drawStringWithShadow(
                        name,
                        -textWidth / 2.0f,
                        0,
                        0xFFFFFF
                );

                // Render health in its own color after the name
                float hp = player.getHealth();
                float maxHp = player.getMaxHealth();
                float absAmount = player.getAbsorptionAmount();
                String hpText = " " + (int)(hp + absAmount) + "§c❤";
                int healthColor = getHealthColor(hp, maxHp);

                mc.fontRendererObj.drawStringWithShadow(
                        hpText,
                        -textWidth / 2.0f + nameWidth,
                        0,
                        healthColor
                );
            } else {
                mc.fontRendererObj.drawStringWithShadow(
                        displayText,
                        -textWidth / 2.0f,
                        0,
                        0xFFFFFF
                );
            }

            GlStateManager.enableLighting();
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }
}