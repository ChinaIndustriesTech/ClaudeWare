package com.atl.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface IMinecraft {

    @Invoker("rightClickMouse")
    void invokeRightClickMouse();

    @Invoker("clickMouse")
    void invokeClickMouse();

    @Accessor("leftClickCounter")
    int getLeftClickCounter();

    @Accessor("leftClickCounter")
    void setLeftClickCounter(int value);

    @Accessor("rightClickDelayTimer")
    void setRightClickDelayTimer(int value);

    @Accessor("rightClickDelayTimer")
    int getRightClickDelayTimer();
}
