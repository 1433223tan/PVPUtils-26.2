package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Render.CustomEnchantmentGlint;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RenderSetup.class)
public class RenderTypeMixin {
    @ModifyArg(
            method = "prepareTextures",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureManager;getTexture(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/texture/AbstractTexture;"
            ),
            index = 0
    )
    private Identifier pvp_utils$replaceGlintTexture(Identifier original) {
        return CustomEnchantmentGlint.replaceTexture(original);
    }
}
