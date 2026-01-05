// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/client/screen/EZEPPlacementScreen.java
package org.z2six.ezemeraldpouch.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.z2six.ezemeraldpouch.ModConstants;
import org.z2six.ezemeraldpouch.config.EZEPClientConfig;
import org.z2six.ezemeraldpouch.util.EZEPClientConfigSaver;

import java.util.Locale;

/**
 * Placement editor screen:
 * - Shows a fake inventory outline.
 * - Shows a fake HUD preview (icon + example number).
 * - Shows a fake withdraw button preview.
 * - User can drag both around.
 * - Save writes positions to client config.
 * - Cancel restores positions and closes.
 *
 * Design choice:
 * - HUD position is stored as absolute screen pixels (same as existing config).
 * - Withdraw button position is stored as offsets relative to inventory GUI top-left.
 *   (so it moves correctly with GUI scale / screen size)
 */
public final class EZEPPlacementScreen extends Screen {

    private static final ResourceLocation WITHDRAW_ICON =
            ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, "textures/gui/withdraw_button.png");

    // Fake inventory outline size: vanilla-ish base inventory background is 176x166.
    private static final int INV_W = 176;
    private static final int INV_H = 166;

    // Preview sizes
    private static final int ICON_SIZE = 16;
    private static final int HUD_TEXT_PAD_X = 4;
    private static final int HUD_TEXT_PAD_Y = 4;

    private enum DragTarget {
        NONE,
        HUD,
        WITHDRAW_BUTTON
    }

    // Original values (for cancel)
    private int origHudX;
    private int origHudY;
    private double origHudScale;
    private int origBtnOffX;
    private int origBtnOffY;

    // Working preview state
    private int hudX;
    private int hudY;
    private double hudScale;

    // Button offsets relative to inventory outline top-left
    private int btnOffX;
    private int btnOffY;

    private DragTarget dragging = DragTarget.NONE;
    private int dragGrabDX = 0;
    private int dragGrabDY = 0;

    // Cached inventory outline position
    private int invLeft;
    private int invTop;

    public EZEPPlacementScreen() {
        super(Component.literal("EZ Emerald Pouch Placement Editor"));
    }

    @Override
    protected void init() {
        super.init();

        // Snapshot original config (for cancel)
        origHudX = EZEPClientConfig.HUD_X.get();
        origHudY = EZEPClientConfig.HUD_Y.get();
        origHudScale = EZEPClientConfig.HUD_SCALE.get();

        origBtnOffX = EZEPClientConfig.WITHDRAW_BTN_OFFSET_X.get();
        origBtnOffY = EZEPClientConfig.WITHDRAW_BTN_OFFSET_Y.get();

        // Working copies
        hudX = origHudX;
        hudY = origHudY;
        hudScale = origHudScale;

        btnOffX = origBtnOffX;
        btnOffY = origBtnOffY;

        // Center the fake inventory outline
        invLeft = Math.max(0, (this.width - INV_W) / 2);
        invTop = Math.max(0, (this.height - INV_H) / 2);

        int btnW = 90;
        int btnH = 20;
        int pad = 8;

        int bottomY = Math.min(this.height - btnH - pad, invTop + INV_H + 18);

        // Save / Cancel buttons
        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> onSave())
                .bounds(this.width / 2 - btnW - 6, bottomY, btnW, btnH)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onCancel())
                .bounds(this.width / 2 + 6, bottomY, btnW, btnH)
                .build());

        ModConstants.LOG.info("[EZEP] Placement editor opened. invLeft={},invTop={}, size={}x{}", invLeft, invTop, INV_W, INV_H);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        ModConstants.LOG.info("[EZEP] Placement editor closed.");
    }

    private void onSave() {
        try {
            // Write working values to config
            EZEPClientConfig.HUD_X.set(hudX);
            EZEPClientConfig.HUD_Y.set(hudY);
            EZEPClientConfig.HUD_SCALE.set(hudScale);

            EZEPClientConfig.WITHDRAW_BTN_OFFSET_X.set(btnOffX);
            EZEPClientConfig.WITHDRAW_BTN_OFFSET_Y.set(btnOffY);

            // Attempt to force-save to disk
            EZEPClientConfigSaver.saveClientConfigBestEffort();

            ModConstants.LOG.info("[EZEP] Placement saved. HUD=({},{} @ scale {}) BTN_OFF=({},{}).",
                    hudX, hudY, hudScale, btnOffX, btnOffY);

        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Placement save failed (non-fatal): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] Placement save failure details", t);
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.setScreen(null);
    }

    private void onCancel() {
        try {
            // Restore original values
            EZEPClientConfig.HUD_X.set(origHudX);
            EZEPClientConfig.HUD_Y.set(origHudY);
            EZEPClientConfig.HUD_SCALE.set(origHudScale);

            EZEPClientConfig.WITHDRAW_BTN_OFFSET_X.set(origBtnOffX);
            EZEPClientConfig.WITHDRAW_BTN_OFFSET_Y.set(origBtnOffY);

            EZEPClientConfigSaver.saveClientConfigBestEffort();

            ModConstants.LOG.info("[EZEP] Placement canceled. Restored original config values.");
        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Placement cancel restore failed (non-fatal): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] Placement cancel restore failure details", t);
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.setScreen(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC behaves like cancel
        if (keyCode == 256 /* GLFW.GLFW_KEY_ESCAPE */) {
            onCancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        try {
            if (button != 0) {
                return super.mouseClicked(mouseX, mouseY, button);
            }

            // Determine click target: HUD preview or withdraw preview
            if (isMouseOverHud(mouseX, mouseY)) {
                dragging = DragTarget.HUD;
                int bx = (int) mouseX - hudX;
                int by = (int) mouseY - hudY;
                dragGrabDX = bx;
                dragGrabDY = by;
                ModConstants.LOG.debug("[EZEP] Drag start HUD (grab {},{})", dragGrabDX, dragGrabDY);
                return true;
            }

            if (isMouseOverWithdrawButton(mouseX, mouseY)) {
                dragging = DragTarget.WITHDRAW_BUTTON;
                int btnXAbs = invLeft + btnOffX;
                int btnYAbs = invTop + btnOffY;
                dragGrabDX = (int) mouseX - btnXAbs;
                dragGrabDY = (int) mouseY - btnYAbs;
                ModConstants.LOG.debug("[EZEP] Drag start WITHDRAW_BUTTON (grab {},{})", dragGrabDX, dragGrabDY);
                return true;
            }

        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] mouseClicked failed (non-fatal): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] mouseClicked failure details", t);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        try {
            if (button == 0 && dragging != DragTarget.NONE) {
                ModConstants.LOG.debug("[EZEP] Drag end {}", dragging);
                dragging = DragTarget.NONE;
                return true;
            }
        } catch (Throwable t) {
            ModConstants.LOG.debug("[EZEP] mouseReleased failed (non-fatal).", t);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        try {
            if (button != 0) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);

            if (dragging == DragTarget.HUD) {
                int nx = (int) mouseX - dragGrabDX;
                int ny = (int) mouseY - dragGrabDY;

                // Soft clamp inside screen bounds (allow small overflow, but avoid losing it completely).
                nx = clamp(nx, -50, this.width - 10);
                ny = clamp(ny, -50, this.height - 10);

                hudX = nx;
                hudY = ny;
                return true;
            }

            if (dragging == DragTarget.WITHDRAW_BUTTON) {
                // Convert drag to offsets relative to inventory outline
                int nxAbs = (int) mouseX - dragGrabDX;
                int nyAbs = (int) mouseY - dragGrabDY;

                // Convert to offsets
                int nxOff = nxAbs - invLeft;
                int nyOff = nyAbs - invTop;

                // Soft clamp so it stays near inventory (still allow some range).
                nxOff = clamp(nxOff, -64, INV_W + 64);
                nyOff = clamp(nyOff, -64, INV_H + 64);

                btnOffX = nxOff;
                btnOffY = nyOff;
                return true;
            }

        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] mouseDragged failed (non-fatal): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] mouseDragged failure details", t);
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean isMouseOverHud(double mx, double my) {
        try {
            Font font = Minecraft.getInstance().font;
            if (font == null) return false;

            String example = exampleHudText();
            int textW = font.width(example);

            // HUD preview bounds are scaled by hudScale
            float s = (float) hudScale;

            int w = (int) ((ICON_SIZE + HUD_TEXT_PAD_X + textW) * s);
            int h = (int) (Math.max(ICON_SIZE, font.lineHeight + 2 * HUD_TEXT_PAD_Y) * s);

            return mx >= hudX && mx <= (hudX + w) && my >= hudY && my <= (hudY + h);
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean isMouseOverWithdrawButton(double mx, double my) {
        int x = invLeft + btnOffX;
        int y = invTop + btnOffY;
        return mx >= x && mx <= (x + ICON_SIZE) && my >= y && my <= (y + ICON_SIZE);
    }

    private String exampleHudText() {
        // Explicitly example only (per your requirement).
        return "123.4k";
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // NOTE: 1.21.1 signature requires mouseX/mouseY/partialTick
        this.renderBackground(gg, mouseX, mouseY, partialTick);

        // Title + instructions
        Font font = Minecraft.getInstance().font;
        if (font != null) {
            gg.drawString(font, "EZ Emerald Pouch Placement Editor", 10, 10, 0xFFFFFF, true);
            gg.drawString(font, "Drag the HUD preview and the withdraw button preview.", 10, 24, 0xCFCFCF, false);
            gg.drawString(font, "Save applies to config. Cancel restores old values. ESC = Cancel.", 10, 36, 0xCFCFCF, false);
        }

        // Fake inventory outline
        drawOutlineRect(gg, invLeft, invTop, INV_W, INV_H, 0xFFAAAAAA);
        drawCheckerInteriorHint(gg, invLeft + 1, invTop + 1, INV_W - 2, INV_H - 2);

        if (font != null) {
            gg.drawString(font, "Inventory (example outline)", invLeft + 6, invTop + 6, 0xFFDDDDDD, false);
        }

        // Draw withdraw button preview at (invLeft + btnOffX, invTop + btnOffY)
        int btnX = invLeft + btnOffX;
        int btnY = invTop + btnOffY;

        RenderSystem.enableBlend();
        gg.blit(WITHDRAW_ICON, btnX, btnY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        // Hover/selection highlight
        if (isMouseOverWithdrawButton(mouseX, mouseY) || dragging == DragTarget.WITHDRAW_BUTTON) {
            gg.fill(btnX, btnY, btnX + ICON_SIZE, btnY + ICON_SIZE, 0x40FFFFFF);
            drawOutlineRect(gg, btnX, btnY, ICON_SIZE, ICON_SIZE, 0xFFFFFFFF);
        }

        // Draw HUD preview (absolute screen position, scaled)
        renderHudPreview(gg, mouseX, mouseY);

        // Also show numeric debug values
        if (font != null) {
            int infoY = Math.min(this.height - 60, invTop + INV_H + 4);
            String hudInfo = String.format(Locale.ROOT, "HUD: x=%d y=%d scale=%.2f", hudX, hudY, hudScale);
            String btnInfo = String.format(Locale.ROOT, "Withdraw Btn (offset): x=%d y=%d  (abs=%d,%d)",
                    btnOffX, btnOffY, btnX, btnY);

            gg.drawString(font, hudInfo, 10, infoY, 0xFFBFBFBF, false);
            gg.drawString(font, btnInfo, 10, infoY + 12, 0xFFBFBFBF, false);
        }

        super.render(gg, mouseX, mouseY, partialTick);
    }

    private void renderHudPreview(GuiGraphics gg, int mouseX, int mouseY) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;
            if (font == null) return;

            String example = exampleHudText();
            int textW = font.width(example);
            int baseW = ICON_SIZE + HUD_TEXT_PAD_X + textW;
            int baseH = Math.max(ICON_SIZE, font.lineHeight + 2 * HUD_TEXT_PAD_Y);

            float s = (float) hudScale;

            gg.pose().pushPose();
            gg.pose().translate(hudX, hudY, 0);
            gg.pose().scale(s, s, 1.0f);

            RenderSystem.enableBlend();
            gg.blit(WITHDRAW_ICON, 0, 0, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            gg.drawString(font, example, ICON_SIZE + HUD_TEXT_PAD_X, HUD_TEXT_PAD_Y, 0xFFFFFF, true);

            // highlight when hovered/dragging
            boolean hovered = isMouseOverHud(mouseX, mouseY);
            if (hovered || dragging == DragTarget.HUD) {
                gg.fill(0, 0, baseW, baseH, 0x30FFFFFF);
                drawOutlineRect(gg, 0, 0, baseW, baseH, 0xFFFFFFFF);
            }

            gg.pose().popPose();
        } catch (Throwable t) {
            ModConstants.LOG.debug("[EZEP] renderHudPreview failed (non-fatal).", t);
        }
    }

    private static void drawOutlineRect(GuiGraphics gg, int x, int y, int w, int h, int argb) {
        // top
        gg.fill(x, y, x + w, y + 1, argb);
        // bottom
        gg.fill(x, y + h - 1, x + w, y + h, argb);
        // left
        gg.fill(x, y, x + 1, y + h, argb);
        // right
        gg.fill(x + w - 1, y, x + w, y + h, argb);
    }

    private static void drawCheckerInteriorHint(GuiGraphics gg, int x, int y, int w, int h) {
        // Very light checker so player sees it’s just an outline area.
        int c1 = 0x10101010;
        int c2 = 0x08080808;

        int step = 8;
        for (int yy = 0; yy < h; yy += step) {
            for (int xx = 0; xx < w; xx += step) {
                int c = (((xx / step) + (yy / step)) % 2 == 0) ? c1 : c2;
                gg.fill(x + xx, y + yy, x + Math.min(xx + step, w), y + Math.min(yy + step, h), c);
            }
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
