package com.pvp_utils.mixin.client;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.FrameBufferCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlDevice")
public interface GlDeviceAccessor {
    @Accessor("directStateAccess")
    DirectStateAccess pvp_utils$getDirectStateAccess();

    @Accessor("frameBufferCache")
    FrameBufferCache pvp_utils$getFrameBufferCache();
}
