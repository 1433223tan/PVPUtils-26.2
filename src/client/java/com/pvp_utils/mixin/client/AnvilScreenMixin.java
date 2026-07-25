package com.pvp_utils.mixin.client;

import com.pvp_utils.Config;
import com.pvp_utils.client.ServerTranslationContents;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {
    @Inject(method = "slotChanged", at = @At("HEAD"))
    private void pvp_utils$blockItemNameTranslationLookup(AbstractContainerMenu menu, int slot, ItemStack stack, CallbackInfo ci) {
        if (Config.modifyTranslationKeys && slot == AnvilMenu.INPUT_SLOT && !stack.isEmpty()) {
            ServerTranslationContents.markComponent(stack.getHoverName());
        }
    }
}
