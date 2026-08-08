package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.pvp_utils.client.modules.impl.Render.GammaOverrideManager;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightTextureMixin {
    @ModifyExpressionValue(
            method = "extract",
            at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0)
    )
    private float overrideGamma(float gamma) {
        return GammaOverrideManager.apply(gamma);
    }
}
