package com.pvp_utils.client.alt;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pvp_utils.Config;
import com.pvp_utils.client.render.MainUI.MainUISharedBackground;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

public final class AltManagerScreen extends Screen {
    private final Screen parent;
    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private AltManager.Account selected;
    private String input = "";
    private String status = "";
    private String deviceCode = "";
    private String deviceUrl = "";
    private long deviceExpiresAt;
    private boolean addOpen;
    private boolean offlineOpen;
    private boolean microsoftWaiting;
    private int mouseX;
    private int mouseY;
    private int scroll;

    public AltManagerScreen(Screen parent) {
        super(Component.literal("Alt Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AltManager.init();
        scroll = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (microsoftWaiting && deviceExpiresAt > 0L && System.currentTimeMillis() >= deviceExpiresAt) {
            microsoftWaiting = false;
            status = text("微软登录已过期", "Microsoft login expired.");
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        MainUISharedBackground.render(graphics, mouseX, mouseY);
        Canvas canvas = glBackend.begin(mainFramebufferId());
        if (canvas == null) return;
        try {
            draw(canvas);
        } finally {
            glBackend.end();
        }
    }

    private void draw(Canvas canvas) {
        float cardW = Math.max(360f, Math.min(500f, width * 0.52f));
        float cardH = Math.max(360f, Math.min(height - 150f, height * 0.72f));
        float cardX = (width - cardW) * 0.5f;
        float cardY = 76f;

        SkiaBlurRenderer.getInstance().render(
                canvas, glBackend.getContext(), Minecraft.getInstance(), mainFramebufferId(),
                cardX, cardY, cardW, cardH, 18f, 0x12000000, 1.05f
        );
        drawCard(canvas, cardX, cardY, cardW, cardH);
        drawTitle(canvas);
        drawAccountList(canvas, cardX, cardY, cardW, cardH);
        drawActions(canvas, cardX, cardY, cardW, cardH);

        if (addOpen || microsoftWaiting) {
            SkiaBlurRenderer.getInstance().render(
                    canvas, glBackend.getContext(), Minecraft.getInstance(), mainFramebufferId(),
                    0f, 0f, width, height, 0f, 0x4A070B12, 0.95f
            );
        }
        if (addOpen) drawAddDialog(canvas);
        if (microsoftWaiting) drawMicrosoftDialog(canvas);
    }

    private void drawTitle(Canvas canvas) {
        String title = "Alt Manager";
        float size = 30f;
        FontRenderer.drawText(canvas, title,
                (width - FontRenderer.measureTextWidth(title, size)) * 0.5f, 38f, size, 0xFFFFFFFF);
        if (!status.isBlank()) {
            FontRenderer.drawText(canvas, status,
                    (width - FontRenderer.measureTextWidth(status, 11f)) * 0.5f, 58f, 11f, 0xFFFFD176);
        }
    }

    private void drawCard(Canvas canvas, float x, float y, float w, float h) {
        try (Paint bg = new Paint(); Paint stroke = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor(0x32101010);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 18f), bg);
            stroke.setAntiAlias(true);
            stroke.setMode(PaintMode.STROKE);
            stroke.setStrokeWidth(1f);
            stroke.setColor(0x22FFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x + 0.5f, y + 0.5f, w - 1f, h - 1f, 18f), stroke);
        }
    }

    private void drawAccountList(Canvas canvas, float cardX, float cardY, float cardW, float cardH) {
        float x = cardX + 14f;
        float y = cardY + 16f;
        float w = cardW - 28f;
        float h = cardH - 128f;
        canvas.save();
        canvas.clipRect(Rect.makeXYWH(x, y, w, h));
        List<AltManager.Account> accounts = AltManager.accounts();
        float rowY = y - scroll;
        for (AltManager.Account account : accounts) {
            drawAccount(canvas, account, x, rowY, w);
            rowY += 80f;
        }
        if (accounts.isEmpty()) {
            String empty = text("暂无账号", "No saved accounts");
            FontRenderer.drawText(canvas, empty,
                    x + (w - FontRenderer.measureTextWidth(empty, 13f)) * 0.5f,
                    y + h * 0.5f, 13f, 0xFFB9C7DB);
        }
        canvas.restore();
    }

    private void drawAccount(Canvas canvas, AltManager.Account account, float x, float y, float w) {
        boolean hovered = hit(mouseX, mouseY, x, y, w, 70f);
        boolean active = account.isCurrent();
        boolean chosen = selected != null && selected.name().equalsIgnoreCase(account.name());
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor(chosen ? 0x443B6686 : hovered ? 0x302A4660 : 0x18FFFFFF);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, 70f, 14f), bg);
        }
        drawAvatar(canvas, x + 10f, y + 9f, 52f);
        FontRenderer.drawText(canvas, account.name(), x + 74f, y + 23f, 16f, 0xFFFFFFFF);
        FontRenderer.drawText(canvas, account.typeName(), x + 74f, y + 44f, 13f, 0xFF6EFF75);
        FontRenderer.drawText(canvas, active ? "Active account" : "Unknown ban status",
                x + 74f, y + 62f, 12f, 0xFFB2B2B2);
    }

    private void drawAvatar(Canvas canvas, float x, float y, float size) {
        try (Paint bg = new Paint(); Paint skin = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor(0xFF6D4C39);
            canvas.drawRRect(RRect.makeXYWH(x, y, size, size, 12f), bg);
            skin.setAntiAlias(true);
            skin.setColor(0xFFD69C70);
            canvas.drawRRect(RRect.makeXYWH(x + 8f, y + 12f, size - 16f, size - 18f, 8f), skin);
            skin.setColor(0xFF38261E);
            canvas.drawCircle(x + 19f, y + 27f, 4f, skin);
            canvas.drawCircle(x + 33f, y + 27f, 4f, skin);
        }
    }

    private void drawActions(Canvas canvas, float cardX, float cardY, float cardW, float cardH) {
        float gap = 4f;
        float x = cardX + 14f;
        float w = cardW - 28f;
        float half = (w - gap) * 0.5f;
        float top = cardY + cardH + 14f;
        drawButton(canvas, x, top, half, 30f, text("登录", "Login"),
                hit(mouseX, mouseY, x, top, half, 30f), 0xFF67B9EA);
        drawButton(canvas, x + half + gap, top, half, 30f, text("删除", "Delete"),
                hit(mouseX, mouseY, x + half + gap, top, half, 30f), 0xFF4E83B0);

        float bottom = top + 36f;
        float third = (w - gap * 2f) / 3f;
        drawButton(canvas, x, bottom, third, 30f, text("添加", "Add"),
                hit(mouseX, mouseY, x, bottom, third, 30f), 0xFF67B9EA);
        drawButton(canvas, x + third + gap, bottom, third, 30f, text("离线", "Offline"),
                hit(mouseX, mouseY, x + third + gap, bottom, third, 30f), 0xFF67B9EA);
        drawButton(canvas, x + (third + gap) * 2f, bottom, third, 30f, text("返回", "Go Back"),
                hit(mouseX, mouseY, x + (third + gap) * 2f, bottom, third, 30f), 0xFF4E83B0);
    }

    private void drawAddDialog(Canvas canvas) {
        float x = width * 0.5f - 190f;
        float y = height * 0.5f - 100f;
        drawDialog(canvas, x, y, 380f, 210f);
        String title = text("添加离线账号", "Add Offline");
        FontRenderer.drawText(canvas, title,
                width * 0.5f - FontRenderer.measureTextWidth(title, 18f) * 0.5f, y + 34f, 18f, 0xFFFFFFFF);
        FontRenderer.drawText(canvas, text("输入离线用户名", "Enter offline username"), x + 28f, y + 68f, 13f, 0xFFB9C7DB);
        drawInput(canvas, x + 28f, y + 80f, 324f, 34f);
        drawButton(canvas, x + 28f, y + 132f, 154f, 30f, text("确认添加", "Confirm"),
                hit(mouseX, mouseY, x + 28f, y + 132f, 154f, 30f), 0xFF67B9EA);
        drawButton(canvas, x + 198f, y + 132f, 154f, 30f, text("返回", "Back"),
                hit(mouseX, mouseY, x + 198f, y + 132f, 154f, 30f), 0xFF4E83B0);
    }

    private void drawMicrosoftDialog(Canvas canvas) {
        float x = width * 0.5f - 182f;
        float y = height * 0.5f - 78f;
        drawDialog(canvas, x, y, 364f, 156f);
        String title = text("微软登录", "Microsoft Login");
        FontRenderer.drawText(canvas, title,
                width * 0.5f - FontRenderer.measureTextWidth(title, 18f) * 0.5f, y + 34f, 18f, 0xFFFFFFFF);
        FontRenderer.drawText(canvas, text("请查看浏览器完成登录", "Please check your browser"), x + 78f, y + 72f, 13f, 0xFFFFFFFF);
        drawSpinner(canvas, x + 38f, y + 66f);
        if (!deviceCode.isBlank()) {
            FontRenderer.drawText(canvas, text("设备码：", "Code: ") + deviceCode, x + 28f, y + 100f, 11f, 0xFFB9C7DB);
        }
        drawButton(canvas, x + 238f, y + 112f, 96f, 26f, text("取消", "Cancel"),
                hit(mouseX, mouseY, x + 238f, y + 112f, 96f, 26f), 0xFF4E83B0);
    }

    private void drawSpinner(Canvas canvas, float x, float y) {
        try (Paint paint = new Paint()) {
            paint.setAntiAlias(true);
            paint.setMode(PaintMode.STROKE);
            paint.setStrokeWidth(3f);
            paint.setColor(0xFFFFB28B);
            canvas.drawArc(x - 15f, y - 15f, x + 15f, y + 15f,
                    (System.currentTimeMillis() % 1200L) / 1200f * 360f, 270f, false, paint);
        }
    }

    private void drawDialog(Canvas canvas, float x, float y, float w, float h) {
        try (Paint bg = new Paint(); Paint stroke = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor(0xE8192637);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 18f), bg);
            stroke.setAntiAlias(true);
            stroke.setMode(PaintMode.STROKE);
            stroke.setStrokeWidth(1f);
            stroke.setColor(0x6697BCE4);
            canvas.drawRRect(RRect.makeXYWH(x + 0.5f, y + 0.5f, w - 1f, h - 1f, 18f), stroke);
        }
    }

    private void drawInput(Canvas canvas, float x, float y, float w, float h) {
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor(hit(mouseX, mouseY, x, y, w, h) ? 0x33486C8D : 0x242A3A4D);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 10f), bg);
        }
        String value = input.isBlank() ? text("用户名", "Username") : input;
        FontRenderer.drawText(canvas, value, x + 12f, y + 22f, 12f, input.isBlank() ? 0xFF718097 : 0xFFFFFFFF);
    }

    private void drawButton(Canvas canvas, float x, float y, float w, float h, String label, boolean hovered, int color) {
        try (Paint bg = new Paint()) {
            bg.setAntiAlias(true);
            bg.setColor(hovered ? brighten(color) : color);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 10f), bg);
        }
        float size = h <= 28f ? 12f : 13f;
        FontRenderer.drawText(canvas, label,
                x + (w - FontRenderer.measureTextWidth(label, size)) * 0.5f,
                y + h * 0.64f, size, 0xFFFFFFFF);
    }

    private String text(String chinese, String english) {
        return Config.isChinese ? chinese : english;
    }

    private int brighten(int color) {
        return 0xFF000000
                | (Math.min(255, ((color >> 16) & 255) + 18) << 16)
                | (Math.min(255, ((color >> 8) & 255) + 18) << 8)
                | Math.min(255, (color & 255) + 18);
    }

    private boolean hit(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() != 0) return true;
        float mx = (float) event.x();
        float my = (float) event.y();
        float cardW = Math.max(360f, Math.min(500f, width * 0.52f));
        float cardH = Math.max(360f, Math.min(height - 150f, height * 0.72f));
        float cardX = (width - cardW) * 0.5f;
        float cardY = 76f;
        float gap = 4f;
        float actionX = cardX + 14f;
        float actionW = cardW - 28f;
        float half = (actionW - gap) * 0.5f;
        float top = cardY + cardH + 14f;
        float bottom = top + 36f;
        float third = (actionW - gap * 2f) / 3f;

        if (microsoftWaiting) {
            float x = width * 0.5f - 182f;
            float y = height * 0.5f - 78f;
            if (hit(mx, my, x + 238f, y + 112f, 96f, 26f)) {
                microsoftWaiting = false;
                status = text("已取消微软登录", "Microsoft login cancelled.");
            }
            return true;
        }
        if (addOpen) return handleAddDialogClick(mx, my);
        if (hit(mx, my, actionX, top, half, 30f)) {
            if (selected != null) status = AltManager.login(selected) ? "Logged in as " + selected.name() : "Login failed.";
            return true;
        }
        if (hit(mx, my, actionX + half + gap, top, half, 30f)) {
            if (selected != null) {
                AltManager.remove(selected);
                selected = null;
                status = text("账号已删除", "Account deleted.");
            }
            return true;
        }
        if (hit(mx, my, actionX, bottom, third, 30f)) {
            startMicrosoftLogin();
            return true;
        }
        if (hit(mx, my, actionX + third + gap, bottom, third, 30f)) {
            addOpen = true;
            offlineOpen = true;
            input = "";
            return true;
        }
        if (hit(mx, my, actionX + (third + gap) * 2f, bottom, third, 30f)) {
            onClose();
            return true;
        }
        float rowY = cardY + 16f - scroll;
        for (AltManager.Account account : AltManager.accounts()) {
            if (hit(mx, my, cardX + 14f, rowY, actionW, 70f)) {
                selected = account;
                return true;
            }
            rowY += 80f;
        }
        return true;
    }

    private boolean handleAddDialogClick(float mx, float my) {
        float x = width * 0.5f - 190f;
        float y = height * 0.5f - 100f;
        if (offlineOpen && hit(mx, my, x + 28f, y + 80f, 324f, 34f)) return true;
        if (offlineOpen && hit(mx, my, x + 28f, y + 132f, 154f, 30f)) {
            AltManager.Account account = AltManager.addOffline(input);
            status = account == null ? text("账号名称无效", "Invalid account name.") : text("账号已添加", "Account added.");
            selected = account;
            input = "";
            addOpen = false;
            offlineOpen = false;
            return true;
        }
        if (offlineOpen && hit(mx, my, x + 198f, y + 132f, 154f, 30f)) {
            addOpen = false;
            offlineOpen = false;
            input = "";
            return true;
        }
        return true;
    }

    private void startMicrosoftLogin() {
        microsoftWaiting = true;
        status = text("正在等待微软授权", "Waiting for Microsoft authorization.");
        MicrosoftAuth.login(info -> {
            String[] parts = info.split("\\n", 3);
            deviceCode = parts.length > 0 ? parts[0] : "";
            deviceUrl = parts.length > 1 ? parts[1] : "";
            long seconds = parts.length > 2 ? Long.parseLong(parts[2]) : 900L;
            deviceExpiresAt = System.currentTimeMillis() + seconds * 1000L;
        }, account -> {
            AltManager.add(account);
            selected = account;
            microsoftWaiting = false;
            status = "Microsoft login succeeded: " + account.name();
        }, error -> {
            microsoftWaiting = false;
            status = error;
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, scroll - (int) (verticalAmount * 60f));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (addOpen && offlineOpen && event.key() == 259 && !input.isEmpty()) {
            input = input.substring(0, input.length() - 1);
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (addOpen && offlineOpen && event.isAllowedChatCharacter() && input.length() < 16) {
            input += event.codepointAsString();
            return true;
        }
        return true;
    }

    private void copy(String value) {
        if (minecraft != null) minecraft.keyboardHandler.setClipboard(value == null ? "" : value);
    }

    private void open(String value) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(value));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onClose() {
        glBackend.destroy();
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private int mainFramebufferId() {
        if (minecraft.getMainRenderTarget().getColorTexture() instanceof GlTexture texture
                && RenderSystem.getDevice() instanceof GlDevice device) {
            return texture.getFbo(device.directStateAccess(), minecraft.getMainRenderTarget().getDepthTexture());
        }
        return 0;
    }
}
