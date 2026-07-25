package com.pvp_utils.client.alt;

import com.pvp_utils.Config;
import com.pvp_utils.client.render.font.FontRenderer;
import com.pvp_utils.client.render.skia.SkiaBlurRenderer;
import com.pvp_utils.client.render.skia.SkiaGlBackend;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Desktop;
import java.net.URI;

public final class AltManagerScreen extends Screen {
    private final Screen parent;
    private final SkiaGlBackend glBackend = new SkiaGlBackend();
    private EditBox nameBox;
    private String status = "";
    private String deviceCode = "";
    private String deviceUrl = "";
    private long deviceExpiresAt;
    private boolean microsoftWaiting;
    private boolean addModal;
    private boolean offlineModal;
    private float hoverBack;
    private float hoverMicrosoft;
    private float hoverAdd;
    private int scroll;

    public AltManagerScreen(Screen parent) {
        super(Component.literal("Alt Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AltManager.init();
        int center = width / 2;
        nameBox = new EditBox(font, center - 116, height - 66, 232, 22, Component.literal("Offline account name"));
        nameBox.setMaxLength(16);
        nameBox.setBordered(false);
        nameBox.setVisible(false);
        nameBox.setTextColor(0xFFEAF2FF);
        addRenderableWidget(nameBox);
    }

    @Override
    public void tick() {
        super.tick();
        if (microsoftWaiting && deviceExpiresAt > 0L && System.currentTimeMillis() >= deviceExpiresAt) {
            microsoftWaiting = false;
            status = Config.isChinese ? "微软登录已过期" : "Microsoft login expired.";
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int center = width / 2;
        graphics.fill(0, 0, width, height, 0xFF080B12);
        drawPanel(graphics, center - 270, 18, 540, height - 36, 0xCC121925, 0x334A5A76);
        graphics.drawString(font, "Alt Manager", center - 240, 34, 0xFFF5F8FF, false);
        graphics.drawString(font, Config.isChinese ? "账号切换与登录" : "Account switching and login", center - 240, 52, 0xFF8E9AB0, false);

        drawAccounts(graphics, center, mouseX, mouseY);
        drawLoginPanel(graphics, center, mouseX, mouseY);
        drawButton(graphics, center - 116, height - 38, 232, 22,
                Config.isChinese ? "返回" : "Back", hit(mouseX, mouseY, center - 116, height - 38, 232, 22), 0xFF273247);
        if (addModal) renderAddModal(mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, delta);
    }

    private void drawAccounts(GuiGraphics graphics, int center, int mouseX, int mouseY) {
        graphics.drawString(font, Config.isChinese ? "已保存账号" : "Saved accounts", center - 240, 82, 0xFFB9C7DB, false);
        int y = 98 - scroll;
        for (AltManager.Account account : AltManager.accounts()) {
            if (y < 90 || y > height - 112) {
                y += 30;
                continue;
            }
            boolean current = account.isCurrent();
            boolean hovered = hit(mouseX, mouseY, center - 240, y, 480, 24);
            int fill = current ? 0xFF243D55 : hovered ? 0xFF202B3C : 0xFF171F2D;
            drawPanel(graphics, center - 240, y, 480, 24, fill, hovered ? 0x6681B6E8 : 0x22394A63);
            graphics.drawString(font, current ? "●" : "○", center - 228, y + 8, current ? 0xFF73D6A1 : 0xFF728197, false);
            graphics.drawString(font, account.name(), center - 208, y + 8, 0xFFE8EEF8, false);
            graphics.drawString(font, account.typeName(), center + 150, y + 8, 0xFF8FA4BE, false);
            drawButton(graphics, center + 178, y + 3, 52, 18,
                    Config.isChinese ? "删除" : "Delete", hit(mouseX, mouseY, center + 178, y + 3, 52, 18), 0xFF573342);
            y += 30;
        }
    }

    private void drawLoginPanel(GuiGraphics graphics, int center, int mouseX, int mouseY) {
        int top = height - 102;
        drawPanel(graphics, center - 240, top - 8, 480, 74, 0xFF111A28, 0x334A5A76);
        graphics.fill(center - 116, height - 66, center + 116, height - 44, 0xFF0B111B);
        graphics.drawString(font, nameBox.getValue().isEmpty()
                ? (Config.isChinese ? "离线账号名称" : "Offline account name") : "", center - 108, height - 52,
                0xFF65748A, false);
        drawButton(graphics, center - 112, height - 40, 224, 22,
                Config.isChinese ? "添加账号" : "Add account",
                hit(mouseX, mouseY, center - 112, height - 40, 224, 22), 0xFF344C68);

        if (!status.isEmpty()) {
            graphics.drawString(font, status, center - 240, top - 22, 0xFFFFD176, false);
        }
        if (microsoftWaiting) drawMicrosoftOverlay(graphics, center, mouseX, mouseY);
    }

    private void renderAddModal(int mouseX, int mouseY) {
        if (minecraft == null) return;
        Canvas canvas = glBackend.begin(mainFramebufferId());
        if (canvas == null) return;
        try {
            SkiaBlurRenderer.getInstance().render(
                    canvas, glBackend.getContext(), minecraft, mainFramebufferId(),
                    0f, 0f, width, height, 0f, 0x52070B12, 1.15f
            );
            float x = width * 0.5f - 190f;
            float y = height * 0.5f - 100f;
            float w = 380f;
            float h = offlineModal ? 210f : 178f;
            try (Paint bg = new Paint(); Paint border = new Paint()) {
                bg.setAntiAlias(true);
                bg.setColor(0xE8192637);
                canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 18f), bg);
                border.setAntiAlias(true);
                border.setMode(PaintMode.STROKE);
                border.setStrokeWidth(1f);
                border.setColor(0x6697BCE4);
                canvas.drawRRect(RRect.makeXYWH(x + 0.5f, y + 0.5f, w - 1f, h - 1f, 18f), border);
            }
            String title = Config.isChinese ? "选择登录方式" : "Choose a login method";
            FontRenderer.drawText(canvas, title, width * 0.5f - FontRenderer.measureTextWidth(title, 18f) * 0.5f, y + 34f, 18f, 0xFFFFFFFF);
            if (!offlineModal) {
                drawSkiaButton(canvas, x + 28f, y + 68f, 154f, 42f, Config.isChinese ? "离线账号" : "Offline",
                        hit(mouseX, mouseY, (int) x + 28, (int) y + 68, 154, 42), 0xFF3C6388);
                drawSkiaButton(canvas, x + 198f, y + 68f, 154f, 42f, Config.isChinese ? "微软账号" : "Microsoft",
                        hit(mouseX, mouseY, (int) x + 198, (int) y + 68, 154, 42), 0xFF4D6C91);
                drawSkiaButton(canvas, x + 28f, y + 126f, 324f, 30f, Config.isChinese ? "取消" : "Cancel",
                        hit(mouseX, mouseY, (int) x + 28, (int) y + 126, 324, 30), 0xFF2B394D);
            } else {
                FontRenderer.drawText(canvas, Config.isChinese ? "输入离线用户名" : "Enter offline username",
                        x + 28f, y + 68f, 13f, 0xFFB9C7DB);
                drawSkiaButton(canvas, x + 28f, y + 132f, 154f, 30f, Config.isChinese ? "确认添加" : "Add account",
                        hit(mouseX, mouseY, (int) x + 28, (int) y + 132, 154, 30), 0xFF4E739D);
                drawSkiaButton(canvas, x + 198f, y + 132f, 154f, 30f, Config.isChinese ? "返回" : "Back",
                        hit(mouseX, mouseY, (int) x + 198, (int) y + 132, 154, 30), 0xFF2B394D);
            }
        } finally {
            glBackend.end();
        }
    }

    private void drawSkiaButton(Canvas canvas, float x, float y, float w, float h, String text, boolean hovered, int color) {
        try (Paint paint = new Paint()) {
            paint.setAntiAlias(true);
            paint.setColor(hovered ? brighten(color) : color);
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 11f), paint);
        }
        float size = 13f;
        FontRenderer.drawText(canvas, text, x + (w - FontRenderer.measureTextWidth(text, size)) * 0.5f, y + h * 0.63f, size, 0xFFFFFFFF);
    }

    private void drawMicrosoftOverlay(GuiGraphics graphics, int center, int mouseX, int mouseY) {
        int x = center - 205;
        int y = 118;
        drawPanel(graphics, x, y, 410, 158, 0xF5182434, 0x8898B9DE);
        graphics.drawString(font, Config.isChinese ? "微软账号登录" : "Microsoft account login", x + 18, y + 16, 0xFFF5F8FF, false);
        graphics.drawString(font, Config.isChinese ? "请在浏览器中完成验证，或复制下面的网址打开" : "Complete verification in your browser, or copy the URL below", x + 18, y + 36, 0xFFB9C7DB, false);
        graphics.drawString(font, Config.isChinese ? "设备码：" : "Device code:", x + 18, y + 62, 0xFF8FA4BE, false);
        graphics.drawString(font, deviceCode, x + 104, y + 62, 0xFFFFFFFF, false);
        graphics.drawString(font, deviceUrl, x + 18, y + 82, 0xFF8BC7FF, false);
        long left = Math.max(0L, (deviceExpiresAt - System.currentTimeMillis()) / 1000L);
        graphics.drawString(font, (Config.isChinese ? "剩余时间：" : "Expires in: ") + left + "s", x + 18, y + 104, 0xFFB9C7DB, false);
        drawButton(graphics, x + 18, y + 122, 112, 22, Config.isChinese ? "复制设备码" : "Copy code",
                hit(mouseX, mouseY, x + 18, y + 122, 112, 22), 0xFF263A50);
        drawButton(graphics, x + 138, y + 122, 112, 22, Config.isChinese ? "复制网址" : "Copy URL",
                hit(mouseX, mouseY, x + 138, y + 122, 112, 22), 0xFF263A50);
        drawButton(graphics, x + 258, y + 122, 134, 22, Config.isChinese ? "打开浏览器" : "Open browser",
                hit(mouseX, mouseY, x + 258, y + 122, 134, 22), 0xFF344C68);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int w, int h, String text, boolean hovered, int color) {
        drawPanel(graphics, x, y, w, h, hovered ? brighten(color) : color, hovered ? 0x668BC7FF : 0x334A5A76);
        graphics.drawCenteredString(font, text, x + w / 2, y + 7, 0xFFF1F5FC);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int w, int h, int fill, int border) {
        graphics.fill(x, y, x + w, y + h, fill);
        graphics.fill(x, y, x + w, y + 1, border);
        graphics.fill(x, y + h - 1, x + w, y + h, border);
        graphics.fill(x, y, x + 1, y + h, border);
        graphics.fill(x + w - 1, y, x + w, y + h, border);
    }

    private int brighten(int color) {
        return 0xFF000000 | Math.min(255, ((color >> 16) & 255) + 18) << 16
                | Math.min(255, ((color >> 8) & 255) + 18) << 8
                | Math.min(255, (color & 255) + 18);
    }

    private boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() != 0) return true;
        int center = width / 2;
        double mx = event.x();
        double my = event.y();
        if (addModal) {
            int x = center - 190;
            int y0 = height / 2 - 100;
            if (!offlineModal && hit(mx, my, x + 28, y0 + 68, 154, 42)) {
                offlineModal = true;
                nameBox.setX(x + 28);
                nameBox.setY(y0 + 84);
                nameBox.setVisible(true);
                nameBox.setFocused(true);
                return true;
            }
            if (!offlineModal && hit(mx, my, x + 198, y0 + 68, 154, 42)) {
                addModal = false;
                startMicrosoftLogin();
                return true;
            }
            if (!offlineModal && hit(mx, my, x + 28, y0 + 126, 324, 30)) {
                addModal = false;
                return true;
            }
            if (offlineModal && hit(mx, my, x + 28, y0 + 132, 154, 30)) {
                AltManager.Account account = AltManager.addOffline(nameBox.getValue());
                status = account == null ? "Invalid account name." : "Account added.";
                nameBox.setValue("");
                nameBox.setVisible(false);
                nameBox.setFocused(false);
                addModal = false;
                offlineModal = false;
                return true;
            }
            if (offlineModal && hit(mx, my, x + 198, y0 + 132, 154, 30)) {
                nameBox.setVisible(false);
                nameBox.setFocused(false);
                addModal = false;
                offlineModal = false;
                return true;
            }
            if (offlineModal && hit(mx, my, x + 28, y0 + 50, 324, 60)) {
                return super.mouseClicked(event, consumed);
            }
            return true;
        }
        if (hit(mx, my, center - 116, height - 66, 232, 22)) {
            return super.mouseClicked(event, consumed);
        }
        if (hit(mx, my, center - 112, height - 40, 224, 22) && !microsoftWaiting) {
            addModal = true;
            offlineModal = false;
            nameBox.setVisible(false);
            nameBox.setFocused(false);
            return true;
        }
        if (hit(mx, my, center - 116, height - 38, 232, 22)) {
            onClose();
            return true;
        }
        int y = 98 - scroll;
        for (AltManager.Account account : AltManager.accounts()) {
            if (hit(mx, my, center + 178, y + 3, 52, 18)) {
                AltManager.remove(account);
                status = "Account removed.";
                return true;
            }
            if (hit(mx, my, center - 240, y, 480, 24)) {
                status = AltManager.login(account)
                        ? (Config.isChinese ? "已切换到 " + account.name() : "Logged in as " + account.name())
                        : (Config.isChinese ? "登录失败" : "Login failed.");
                return true;
            }
            y += 30;
        }
        if (microsoftWaiting) {
            int x = center - 205;
            int y0 = 118;
            if (hit(mx, my, x + 18, y0 + 122, 112, 22)) copy(deviceCode);
            else if (hit(mx, my, x + 138, y0 + 122, 112, 22)) copy(deviceUrl);
            else if (hit(mx, my, x + 258, y0 + 122, 134, 22)) open(deviceUrl);
            return true;
        }
        return super.mouseClicked(event, consumed);
    }

    private void startMicrosoftLogin() {
        microsoftWaiting = true;
        status = Config.isChinese ? "正在等待微软授权..." : "Waiting for Microsoft authorization...";
        MicrosoftAuth.login(info -> {
            String[] parts = info.split("\\n", 3);
            deviceCode = parts.length > 0 ? parts[0] : "";
            deviceUrl = parts.length > 1 ? parts[1] : "";
            long seconds = parts.length > 2 ? Long.parseLong(parts[2]) : 900L;
            deviceExpiresAt = System.currentTimeMillis() + seconds * 1000L;
            status = Config.isChinese ? "请完成浏览器中的微软登录" : "Complete Microsoft login in the browser.";
        }, account -> {
            AltManager.add(account);
            microsoftWaiting = false;
            status = Config.isChinese ? "微软账号登录成功：" + account.name() : "Microsoft login succeeded: " + account.name();
        }, error -> {
            microsoftWaiting = false;
            status = error;
        });
    }

    private void copy(String value) {
        if (minecraft != null) minecraft.keyboardHandler.setClipboard(value == null ? "" : value);
        status = Config.isChinese ? "已复制到剪贴板" : "Copied to clipboard.";
    }

    private void open(String value) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(value));
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) (scrollY * 24));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        return super.charTyped(event);
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
