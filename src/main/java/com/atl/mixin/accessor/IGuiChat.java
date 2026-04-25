package com.atl.mixin.accessor;

import net.minecraft.client.gui.GuiChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiChat.class)
public interface IGuiChat {
    @Accessor("defaultInputFieldText")
    String getDefaultInputFieldText();
}