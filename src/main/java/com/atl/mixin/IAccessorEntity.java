package com.atl.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SideOnly(Side.CLIENT)
@Mixin(Entity.class)
public interface IAccessorEntity {
    @Invoker
    Vec3 callGetVectorForRotation(float pitch, float yaw);
}