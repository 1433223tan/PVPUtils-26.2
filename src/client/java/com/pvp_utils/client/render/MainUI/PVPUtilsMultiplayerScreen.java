package com.pvp_utils.client.render.MainUI;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.SamplingMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PVPUtilsMultiplayerScreen extends Screen {
    private final Screen parent;
    private final String shaderPath;
    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private final List<ServerData> servers = new ArrayList<>();
    private final List<Float> hover = new ArrayList<>();
    private final Map<Integer, Image> serverIcons = new HashMap<>();
    private Image defaultServerIcon;
    private ServerList serverList;
    private boolean pendingFrame;
    private int mouseX;
    private int mouseY;
    private int selected = -1;
    private float scroll;
    private float targetScroll;
    private long openStartMs;
    private long closeStartMs;
    private boolean closingToMain;

    public PVPUtilsMultiplayerScreen(Screen parent) {
        this(parent, MainUISharedBackground.activeShaderPath());
    }

    public PVPUtilsMultiplayerScreen(Screen parent, String shaderPath) {
        super(Component.literal("Multiplayer"));
        this.parent = parent;
        this.shaderPath = shaderPath;
        if (shaderPath != null && !shaderPath.isBlank()) MainUISharedBackground.setActiveShader(shaderPath);
    }

    @Override
    protected void init() {
        openStartMs = System.currentTimeMillis();
        closeStartMs = 0L;
        closingToMain = false;
        serverList = new ServerList(minecraft);
        serverList.load();
        reloadServers();
    }

    private void reloadServers() {
        servers.clear();
        hover.clear();
        if (serverList != null) {
            for (int i = 0; i < serverList.size(); i++) {
                servers.add(serverList.get(i));
                hover.add(0f);
            }
        }
        selected = servers.isEmpty() ? -1 : Math.min(selected < 0 ? 0 : selected, servers.size() - 1);
        clampScroll();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        MainUISharedBackground.render(graphics, mouseX, mouseY);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        scroll += (targetScroll - scroll) * 0.20f;
        pendingFrame = true;
        if (closingToMain && closeProgress() >= 1f && minecraft != null) {
            minecraft.setScreen(PVPUtilsMainUI.returningFromSingleplayer(shaderPath));
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    public void renderFrameEnd() {
        if (!pendingFrame || minecraft == null || minecraft.screen != this) {
            pendingFrame = false;
            return;
        }
        Canvas canvas = glBackend.begin(mainFramebufferId());
        if (canvas == null) return;
        try {
            float x = cardX();
            float y = cardY();
            float w = cardW();
            float h = cardH();
            SkiaBlurRenderer.getInstance().render(canvas, glBackend.getContext(), minecraft, mainFramebufferId(),
                    x, y, w, h, 20f, 0x12000000, 0.95f);
            draw(canvas);
        } finally {
            glBackend.end();
            pendingFrame = false;
        }
    }

    private void draw(Canvas canvas) {
        float contentAlpha = closingToMain ? 1f - ease(closeProgress()) : ease(openProgress());
        float x = cardX();
        float y = cardY();
        float w = cardW();
        float h = cardH();
        try (Paint card = new Paint(); Paint stroke = new Paint()) {
            card.setAntiAlias(true);
            card.setColor(0x32101010);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 20f), card);
            stroke.setAntiAlias(true);
            stroke.setMode(PaintMode.STROKE);
            stroke.setStrokeWidth(1f);
            stroke.setColor(0x22FFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x + .5f, y + .5f, w - 1f, h - 1f, 20f), stroke);
        }
        String title = "Multiplayer";
        int alpha = Math.round(255f * contentAlpha);
        FontRenderer.drawText(canvas, title, width * .5f - FontRenderer.measureTextWidth(title, 30f) * .5f, 50f, 30f, (alpha << 24) | 0xFFFFFF);
        drawServers(canvas, x + 16f, y + 16f, w - 32f, h - 82f, alpha);
        drawButton(canvas, x, y + h + 12f, 112f, 32f, "Add", alpha);
        drawButton(canvas, x + 120f, y + h + 12f, 112f, 32f, "Edit", alpha);
        drawButton(canvas, x + 240f, y + h + 12f, 112f, 32f, "Delete", alpha);
        drawButton(canvas, x + w - 112f, y + h + 12f, 112f, 32f, "Back", alpha);
    }

    private void drawServers(Canvas canvas, float x, float y, float w, float h, int alpha) {
        canvas.save();
        canvas.clipRect(Rect.makeXYWH(x, y, w, h));
        if (servers.isEmpty()) {
            FontRenderer.drawText(canvas, "No servers", x + w * .5f - 42f, y + h * .5f, 14f, (Math.round(alpha * .8f) << 24) | 0xFFFFFF);
        } else {
            float itemY = y - scroll;
            for (int i = 0; i < servers.size(); i++) {
                drawServer(canvas, servers.get(i), i, x, itemY, w, 64f, alpha);
                itemY += 72f;
            }
        }
        canvas.restore();
    }

    private void drawServer(Canvas canvas, ServerData server, int index, float x, float y, float w, float h, int alpha) {
        if (y + h < cardY() + 16f || y > cardY() + cardH() - 66f) return;
        boolean over = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        float p = hover.get(index);
        p += ((over ? 1f : 0f) - p) * .16f;
        hover.set(index, p);
        float curve = p * p * (3f - 2f * p);
        boolean active = selected == index;
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            float opacity = active ? .18f + curve * .10f : .07f + curve * .12f;
            bg.setColor((Math.round(alpha * opacity) << 24) | 0xFFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 13f), bg);
        }
        Image icon = serverIcon(index, server);
        if (icon != null) {
            try (Paint imagePaint = new Paint()) {
                imagePaint.setAntiAlias(true);
                imagePaint.setColor((alpha << 24) | 0xFFFFFF);
                canvas.save();
                canvas.clipRRect(RRect.makeXYWH(x + 10f, y + 10f, 44f, 44f, 10f), true);
                canvas.drawImageRect(icon, Rect.makeXYWH(0f, 0f, icon.getWidth(), icon.getHeight()),
                        Rect.makeXYWH(x + 10f, y + 10f, 44f, 44f), SamplingMode.LINEAR, imagePaint, true);
                canvas.restore();
            }
        } else {
            FontRenderer.drawText(canvas, "\uE88A", x + 26f, y + 39f, 25f, (Math.round(alpha * .86f) << 24) | 0xFFFFFF, FontRenderer.MATERIAL_SYMBOLS);
        }
        FontRenderer.drawText(canvas, server.name, x + 66f, y + 26f, 15f, (alpha << 24) | 0xFFFFFF);
        FontRenderer.drawText(canvas, server.ip, x + 66f, y + 46f, 11f, (Math.round(alpha * .72f) << 24) | 0xFFFFFF);
    }

    private void drawButton(Canvas canvas, float x, float y, float w, float h, String label, int alpha) {
        boolean over = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        float p = over ? 1f : 0f;
        int color = lerpRgb(0x67B9EA, 0xA7E0FF, p);
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor((Math.round(alpha * (.20f + .12f * p)) << 24) | color);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 10f), bg);
        }
        FontRenderer.drawText(canvas, label, x + (w - FontRenderer.measureTextWidth(label, 13f)) * .5f, y + 21f, 13f, (alpha << 24) | 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() != 0) return true;
        float x = cardX();
        float y = cardY() + cardH() + 12f;
        if (inside(event.x(), event.y(), x, y, 112f, 32f)) {
            playClick();
            ServerData data = new ServerData("", "", ServerData.Type.OTHER);
            minecraft.setScreen(new ManageServerScreen(this, Component.literal("Add Server"), ok -> {
                if (ok) {
                    serverList.add(data, false);
                    serverList.save();
                    reloadServers();
                }
                minecraft.setScreen(this);
            }, data));
            return true;
        }
        if (inside(event.x(), event.y(), x + 120f, y, 112f, 32f) && selected >= 0) {
            playClick();
            ServerData data = servers.get(selected);
            minecraft.setScreen(new ManageServerScreen(this, Component.literal("Edit Server"), ok -> {
                if (ok) {
                    serverList.replace(selected, data);
                    serverList.save();
                    reloadServers();
                }
                minecraft.setScreen(this);
            }, data));
            return true;
        }
        if (inside(event.x(), event.y(), x + 240f, y, 112f, 32f) && selected >= 0) {
            playClick();
            serverList.remove(servers.get(selected));
            serverList.save();
            reloadServers();
            return true;
        }
        if (inside(event.x(), event.y(), x + cardW() - 112f, y, 112f, 32f)) {
            playClick();
            startClose();
            return true;
        }
        int hit = serverAt((float) event.x(), (float) event.y());
        if (hit >= 0) {
            if (selected == hit) {
                playClick();
                ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(servers.get(hit).ip), servers.get(hit), false, null);
            } else {
                selected = hit;
            }
        }
        return true;
    }

    private int serverAt(float mx, float my) {
        float x = cardX() + 16f;
        float y = cardY() + 16f;
        float w = cardW() - 32f;
        float h = cardH() - 82f;
        if (mx < x || mx > x + w || my < y || my > y + h) return -1;
        int index = (int) ((my - y + scroll) / 72f);
        float inside = (my - y + scroll) - index * 72f;
        return index >= 0 && index < servers.size() && inside <= 64f ? index : -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        targetScroll -= (float) verticalAmount * 48f;
        clampScroll();
        return true;
    }

    private void clampScroll() {
        float max = Math.max(0f, servers.size() * 72f - (cardH() - 82f));
        targetScroll = Math.max(0f, Math.min(max, targetScroll));
        scroll = Math.max(0f, Math.min(max, scroll));
    }

    @Override
    public void onClose() {
        startClose();
    }

    @Override
    public void removed() {
        pendingFrame = false;
        for (Image image : serverIcons.values()) if (image != null) image.close();
        serverIcons.clear();
        if (defaultServerIcon != null) {
            defaultServerIcon.close();
            defaultServerIcon = null;
        }
        glBackend.destroy();
        super.removed();
    }

    private void startClose() {
        if (closingToMain) return;
        closingToMain = true;
        closeStartMs = System.currentTimeMillis();
    }

    private float openProgress() {
        return Math.max(0f, Math.min(1f, (System.currentTimeMillis() - openStartMs) / 440f));
    }

    private float closeProgress() {
        if (!closingToMain || closeStartMs <= 0L) return 0f;
        return Math.max(0f, Math.min(1f, (System.currentTimeMillis() - closeStartMs) / 440f));
    }

    private float ease(float value) {
        float t = 1f - Math.max(0f, Math.min(1f, value));
        return 1f - t * t * t;
    }

    private Image serverIcon(int index, ServerData server) {
        if (serverIcons.containsKey(index)) return serverIcons.get(index);
        byte[] bytes = server.getIconBytes();
        if (bytes == null || bytes.length == 0) {
            Image fallback = defaultServerIcon();
            serverIcons.put(index, fallback);
            return fallback;
        }
        try {
            Image image = Image.makeFromEncoded(bytes);
            serverIcons.put(index, image);
            return image;
        } catch (RuntimeException ignored) {
            serverIcons.put(index, null);
            Image fallback = defaultServerIcon();
            serverIcons.put(index, fallback);
            return fallback;
        }
    }

    private Image defaultServerIcon() {
        if (defaultServerIcon != null) return defaultServerIcon;
        try {
            Identifier id = Identifier.withDefaultNamespace("textures/misc/unknown_server.png");
            var resource = minecraft.getResourceManager().getResource(id);
            if (resource.isPresent()) {
                try (var stream = resource.get().open()) {
                    defaultServerIcon = Image.makeFromEncoded(stream.readAllBytes());
                }
            }
        } catch (Exception ignored) {
            defaultServerIcon = null;
        }
        return defaultServerIcon;
    }

    private boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void playClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    private int lerpRgb(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
        return (r << 16) | (g << 8) | b;
    }

    private int mainFramebufferId() {
        if (minecraft.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), minecraft.getMainRenderTarget().getDepthTexture());
        }
        return 0;
    }

    private float cardW() {
        return Math.max(320f, Math.min(500f, width * .52f));
    }

    private float cardH() {
        return Math.max(280f, Math.min(height - 150f, height * .70f));
    }

    private float cardX() {
        return (width - cardW()) * .5f;
    }

    private float cardY() {
        return 72f;
    }
}
