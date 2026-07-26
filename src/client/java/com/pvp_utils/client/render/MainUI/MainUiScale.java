package com.pvp_utils.client.render.MainUI;

import io.github.humbleui.skija.Canvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;

public final class MainUiScale {
    private MainUiScale() {
    }

    public static float pageScale() {
        Minecraft minecraft = Minecraft.getInstance();
        float guiScale = minecraft == null ? 2f : Math.max(1f, (float) minecraft.getWindow().getGuiScale());
        return 1.90f / guiScale;
    }

    public static int pageWidth() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? 960 : Math.max(1, Math.round(minecraft.getWindow().getWidth() * 0.5f));
    }

    public static int pageHeight() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? 540 : Math.max(1, Math.round(minecraft.getWindow().getHeight() * 0.5f));
    }

    public static void applyPage(Canvas canvas, int guiWidth, int guiHeight) {
        canvas.translate(guiWidth * 0.5f, guiHeight * 0.5f);
        canvas.scale(pageScale(), pageScale());
        canvas.translate(-pageWidth() * 0.5f, -pageHeight() * 0.5f);
    }

    public static int pageX(int mouseX, int guiWidth) {
        return Math.round(pageWidth() * 0.5f + (mouseX - guiWidth * 0.5f) / pageScale());
    }

    public static int pageY(int mouseY, int guiHeight) {
        return Math.round(pageHeight() * 0.5f + (mouseY - guiHeight * 0.5f) / pageScale());
    }

    public static MouseButtonEvent pageEvent(MouseButtonEvent event, int guiWidth, int guiHeight) {
        return new MouseButtonEvent(pageX((int) event.x(), guiWidth), pageY((int) event.y(), guiHeight), event.buttonInfo());
    }

    public static float pageScreenX(float x, int guiWidth) {
        return guiWidth * 0.5f + (x - pageWidth() * 0.5f) * pageScale();
    }

    public static float pageScreenY(float y, int guiHeight) {
        return guiHeight * 0.5f + (y - pageHeight() * 0.5f) * pageScale();
    }

    public static float pageScreenSize(float size) {
        return size * pageScale();
    }

    public static void applyTopRight(Canvas canvas, int guiWidth, int guiHeight, float scale) {
        canvas.translate(guiWidth, 0f);
        canvas.scale(scale, scale);
        canvas.translate(-guiWidth, 0f);
    }

    public static void applyBottomLeft(Canvas canvas, int guiWidth, int guiHeight, float scale) {
        canvas.translate(0f, guiHeight);
        canvas.scale(scale, scale);
        canvas.translate(0f, -guiHeight);
    }

    public static int topRightX(int mouseX, int guiWidth, int guiHeight, float scale) {
        return Math.round(guiWidth + (mouseX - guiWidth) / scale);
    }

    public static int topRightY(int mouseY, int guiWidth, int guiHeight, float scale) {
        return Math.round(mouseY / scale);
    }
}
