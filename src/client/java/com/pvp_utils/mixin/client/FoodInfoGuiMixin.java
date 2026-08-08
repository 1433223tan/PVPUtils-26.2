package com.pvp_utils.mixin.client;

import com.pvp_utils.client.modules.impl.Tool.FoodInfo.FoodInfoHudOverlay;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class FoodInfoGuiMixin {
    @Inject(method = "extractFood", at = @At("HEAD"))
    private void pvp_utils$foodInfoPreFood(GuiGraphicsExtractor graphics, Player player, int top, int right, CallbackInfo ci) {
        FoodInfoHudOverlay.getInstance().onPreRenderFood(graphics, player, top, right);
    }

    @Inject(method = "extractFood", at = @At("RETURN"))
    private void pvp_utils$foodInfoPostFood(GuiGraphicsExtractor graphics, Player player, int top, int right, CallbackInfo ci) {
        FoodInfoHudOverlay.getInstance().onRenderFood(graphics, player, top, right);
    }

    @Inject(method = "extractHearts", at = @At("RETURN"))
    private void pvp_utils$foodInfoPostHearts(GuiGraphicsExtractor graphics, Player player, int left, int top, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking, CallbackInfo ci) {
        FoodInfoHudOverlay.getInstance().onRenderHealth(graphics, player, left, top);
    }
}
