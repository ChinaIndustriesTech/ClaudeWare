package com.atl.mixin.accessor;

import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiScreen.class)
public interface IGuiScreen {
    @Invoker("mouseClicked")
    void invokeMouseClicked(int mouseX, int mouseY, int mouseButton);
}