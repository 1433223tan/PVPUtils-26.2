package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.Zoom.ZoomManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class ZoomGameRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void pvp_utils$applyZoomFov(CameraRenderState state, float tickDelta, CallbackInfo ci) {
        state.hudFov /= ZoomManager.getZoomDivisor(tickDelta);
    }
}
