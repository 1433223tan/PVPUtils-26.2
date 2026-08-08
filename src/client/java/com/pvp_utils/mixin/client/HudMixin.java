package com.pvp_utils.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Combat.HitMarkerRenderer;
import com.pvp_utils.client.modules.impl.Optimize.BetterScoreboard.BetterScoreboardRenderer;
import com.pvp_utils.client.modules.impl.Render.ArmorHudRenderer;
import com.pvp_utils.client.modules.impl.Render.ArraylistRenderer;
import com.pvp_utils.client.modules.impl.Render.DamageNumberRenderer;
import com.pvp_utils.client.modules.impl.Render.DiggingStatusRenderer;
import com.pvp_utils.client.modules.impl.Render.DynamicIsland.DynamicIslandRenderer;
import com.pvp_utils.client.modules.impl.Render.FallDamagePredictor;
import com.pvp_utils.client.modules.impl.Render.HudEditOverlay;
import com.pvp_utils.client.modules.impl.Render.ItemUseStatusRenderer;
import com.pvp_utils.client.modules.impl.Render.KeystrokesRenderer;
import com.pvp_utils.client.modules.impl.Render.LyricsDisplayRenderer;
import com.pvp_utils.client.modules.impl.Render.MusicInfoHudRenderer;
import com.pvp_utils.client.modules.impl.Render.NotificationOverlay;
import com.pvp_utils.client.modules.impl.Render.PotionStatusRenderer;
import com.pvp_utils.client.modules.impl.Render.TargetHudRenderer;
import com.pvp_utils.client.modules.impl.Tool.BlockCountDisplayRenderer;
import com.pvp_utils.client.modules.impl.Tool.TitleDetector;
import com.pvp_utils.client.render.skia.SkiaRenderer;
import com.pvp_utils.client.render.skia.SkiaScreen;
import io.github.humbleui.skija.Canvas;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudMixin {
    @ModifyExpressionValue(
            method = "extractCrosshair",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z")
    )
    private boolean pvp_utils$showCrosshairInThirdPerson(boolean original) {
        return true;
    }

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void pvp_utils$onSetTitle(Component title, CallbackInfo ci) {
        TitleDetector.check(title != null ? title.getString() : null, null);
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void pvp_utils$onSetSubtitle(Component subtitle, CallbackInfo ci) {
        TitleDetector.check(null, subtitle != null ? subtitle.getString() : null);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void pvp_utils$renderOverlays(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        int guiWidth = client.getWindow().getGuiScaledWidth();
        int guiHeight = client.getWindow().getGuiScaledHeight();
        Canvas canvas = null;

        boolean skiaScreenOpen = client.gui.screen() instanceof SkiaScreen;
        if (!skiaScreenOpen && NotificationOverlay.getInstance().needsStandaloneCanvas()) {
            int[] bounds = NotificationOverlay.getInstance().getCanvasBounds(guiWidth, guiHeight);
            if (bounds != null) {
                canvas = SkiaRenderer.beginRegion(bounds[0], bounds[1], bounds[2], bounds[3]);
            }
        }

        if (!skiaScreenOpen) {
            NotificationOverlay.getInstance().render(guiGraphics, canvas);
        }
        HitMarkerRenderer.getInstance().render(guiGraphics);
        TargetHudRenderer.getInstance().render(guiGraphics);
        ItemUseStatusRenderer.getInstance().render(guiGraphics);
        DynamicIslandRenderer.getInstance().render(guiGraphics);
        DamageNumberRenderer.getInstance().render(guiGraphics);
        FallDamagePredictor.getInstance().render(guiGraphics);
        DiggingStatusRenderer.getInstance().render(guiGraphics);
        ArraylistRenderer.getInstance().render(guiGraphics);
        KeystrokesRenderer.getInstance().render(guiGraphics);
        ArmorHudRenderer.getInstance().render(guiGraphics);
        PotionStatusRenderer.getInstance().render(guiGraphics);
        BetterScoreboardRenderer.getInstance().render(guiGraphics);
        HudEditOverlay.getInstance().render(guiGraphics, canvas);

        if (canvas != null) {
            SkiaRenderer.endRegion(guiGraphics);
        }

        LyricsDisplayRenderer.getInstance().render(guiGraphics);
        MusicInfoHudRenderer.getInstance().render(guiGraphics);
        BlockCountDisplayRenderer.getInstance().render(guiGraphics, null);
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$hideVanillaPotionEffects(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (PotionStatusRenderer.getInstance().shouldHideVanillaEffects()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$hideVignette(GuiGraphicsExtractor guiGraphics, Entity entity, CallbackInfo ci) {
        if (Config.hideVignette) {
            ci.cancel();
        }
    }

    @Inject(method = "extractBossOverlay", at = @At("HEAD"), cancellable = true)
    private void pvp_utils$hideBossBar(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Config.hideBossBar) {
            ci.cancel();
        }
    }
}
