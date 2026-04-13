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

    // Cache to store the sorted list of enabled modules
    private List<Module> enabledModulesCache;

    public HUD() {
        super("HUD", "Draws the module list on screen", Category.MISC);
        setEnabled(true);
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT || !isEnabled()) return;

        // Only rebuild the list if the cache is null (invalidated)
        if (enabledModulesCache == null) {
            FontRenderer fr = mc.fontRendererObj;
            enabledModulesCache = ExampleMod.moduleManager.getAll().stream()
                    .filter(Module::isEnabled)
                    .sorted(Comparator.comparingInt(m -> -fr.getStringWidth(m.getName())))
                    .collect(Collectors.toList());
        }

        FontRenderer fr = mc.fontRendererObj;
        int y = 2;
        for (Module m : enabledModulesCache) {
            fr.drawStringWithShadow(m.getName(), event.resolution.getScaledWidth() - fr.getStringWidth(m.getName()) - 2, y, -1);
            y += fr.FONT_HEIGHT + 2;
        }
    }

    public void invalidateCache() {
        this.enabledModulesCache = null;
    }
}