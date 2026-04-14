package com.atl.module.modules;

import com.atl.module.ExampleMod;
import com.atl.module.management.Category;
import com.atl.module.management.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HUD extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();

    public HUD() {
        super("HUD", "Draws the module list on screen", Category.MISC);
        setEnabled(true);
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        // Only render on the TEXT element to avoid drawing multiple times per tick
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT || !isEnabled()) return;

        FontRenderer fr = mc.fontRendererObj;

        // Get all enabled modules, excluding the HUD itself for a cleaner look
        List<Module> enabledModules = ExampleMod.moduleManager.getAll().stream()
                .filter(m -> m.isEnabled() && !m.getName().equalsIgnoreCase("HUD"))
                .sorted(Comparator.comparingInt(m -> -fr.getStringWidth(m.getName())))
                .collect(Collectors.toList());

        int y = 2;
        for (Module m : enabledModules) {
            // Draw module names aligned to the right side of the screen
            String name = m.getName();
            int x = event.resolution.getScaledWidth() - fr.getStringWidth(name) - 2;
            
            fr.drawStringWithShadow(name, x, y, -1); // -1 is white
            y += fr.FONT_HEIGHT + 2;
        }
    }
}