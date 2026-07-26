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

    public static float pageScale(float minX, float minY, float maxX, float maxY) {
        float centerX = pageWidth() * 0.5f;
        float centerY = pageHeight() * 0.5f;
        float availableX = Math.max(1f, centerX - 12f);
        float availableY = Math.max(1f, centerY - 12f);
        float factor = 0.95f;
        factor = Math.min(factor, availableX / Math.max(1f, centerX - minX));
        factor = Math.min(factor, availableX / Math.max(1f, maxX - centerX));
        factor = Math.min(factor, availableY / Math.max(1f, centerY - minY));
        factor = Math.min(factor, availableY / Math.max(1f, maxY - centerY));
        Minecraft minecraft = Minecraft.getInstance();
        float guiScale = minecraft == null ? 2f : Math.max(1f, (float) minecraft.getWindow().getGuiScale());
        return Math.max(0.35f, factor * 2f / guiScale);
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
        applyPage(canvas, guiWidth, guiHeight, pageScale());
    }

    public static void applyPage(Canvas canvas, int guiWidth, int guiHeight, float scale) {
        canvas.translate(guiWidth * 0.5f, guiHeight * 0.5f);
        canvas.scale(scale, scale);
        canvas.translate(-pageWidth() * 0.5f, -pageHeight() * 0.5f);
    }

    public static int pageX(int mouseX, int guiWidth) {
        return pageX(mouseX, guiWidth, pageScale());
    }

    public static int pageY(int mouseY, int guiHeight) {
        return pageY(mouseY, guiHeight, pageScale());
    }

    public static int pageX(int mouseX, int guiWidth, float scale) {
        return Math.round(pageWidth() * 0.5f + (mouseX - guiWidth * 0.5f) / scale);
    }

    public static int pageY(int mouseY, int guiHeight, float scale) {
        return Math.round(pageHeight() * 0.5f + (mouseY - guiHeight * 0.5f) / scale);
    }

    public static MouseButtonEvent pageEvent(MouseButtonEvent event, int guiWidth, int guiHeight) {
        return pageEvent(event, guiWidth, guiHeight, pageScale());
    }

    public static MouseButtonEvent pageEvent(MouseButtonEvent event, int guiWidth, int guiHeight, float scale) {
        return new MouseButtonEvent(pageX((int) event.x(), guiWidth, scale), pageY((int) event.y(), guiHeight, scale), event.buttonInfo());
    }

    public static float pageScreenX(float x, int guiWidth) {
        return pageScreenX(x, guiWidth, pageScale());
    }

    public static float pageScreenY(float y, int guiHeight) {
        return pageScreenY(y, guiHeight, pageScale());
    }

    public static float pageScreenSize(float size) {
        return pageScreenSize(size, pageScale());
    }

    public static float pageScreenX(float x, int guiWidth, float scale) {
        return guiWidth * 0.5f + (x - pageWidth() * 0.5f) * scale;
    }

    public static float pageScreenY(float y, int guiHeight, float scale) {
        return guiHeight * 0.5f + (y - pageHeight() * 0.5f) * scale;
    }

    public static float pageScreenSize(float size, float scale) {
        return size * scale;
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
