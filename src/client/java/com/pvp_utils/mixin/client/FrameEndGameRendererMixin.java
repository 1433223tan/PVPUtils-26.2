package com.pvp_utils.mixin.client;

import com.pvp_utils.client.NeteaseMusic.NeteaseMusicScreen;
import com.pvp_utils.client.alt.AltManagerScreen;
import com.pvp_utils.client.gui.clickgui.NewSettingsScreen;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.modules.impl.Render.KeystrokesRenderer;
import com.pvp_utils.client.modules.impl.Render.PotionStatusRenderer;
import com.pvp_utils.client.render.MainUI.PVPUtilsMainUI;
import com.pvp_utils.client.render.MainUI.PVPUtilsMultiplayerScreen;
import com.pvp_utils.client.render.MainUI.PVPUtilsSingleplayerScreen;
import com.pvp_utils.client.render.MainUI.PVPUtilsViaFabricPlusScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class FrameEndGameRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void pvp_utils$renderDeferredSkiaFrames(CallbackInfo ci) {
        PotionStatusRenderer.getInstance().renderFrameEnd();
        KeystrokesRenderer.getInstance().renderFrameEnd();
        HudEditOverlay.getInstance().renderFrameEnd();

        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen instanceof NewSettingsScreen settingsScreen) {
            settingsScreen.renderFrameEnd();
        } else if (screen instanceof NeteaseMusicScreen musicScreen) {
            musicScreen.renderFrameEnd();
        } else if (screen instanceof PVPUtilsMainUI mainUI) {
            mainUI.renderFrameEnd();
        } else if (screen instanceof PVPUtilsSingleplayerScreen singleplayerScreen) {
            singleplayerScreen.renderFrameEnd();
        } else if (screen instanceof PVPUtilsMultiplayerScreen multiplayerScreen) {
            multiplayerScreen.renderFrameEnd();
        } else if (screen instanceof AltManagerScreen altManagerScreen) {
            altManagerScreen.renderFrameEnd();
        } else if (screen instanceof PVPUtilsViaFabricPlusScreen viaFabricPlusScreen) {
            viaFabricPlusScreen.renderFrameEnd();
        }
    }
}
