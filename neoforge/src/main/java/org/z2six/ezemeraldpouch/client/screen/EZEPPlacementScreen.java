// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/client/screen/EZEPPlacementScreen.java
package org.z2six.ezemeraldpouch.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * Also includes:
 * - Reset button
 * - HUD hide/show toggle
 * - Withdraw button hide/show toggle
 *
 * Note on blur:
 * - NeoForge/MC background dim/blur can make thin outlines hard to see.
 * - We draw a strong dark overlay and thicker/bright outlines for readability.
 *
 * IMPORTANT RENDERING NOTE:
 * - Do NOT call super.render() here, because Screen#render will render the background again (blur/dim),
 *   which ends up overlaying/blur-filtering our custom drawings. Buttons appear fine because they render after.
 * - Instead, we render background ONCE, then our custom preview, then manually render widgets/renderables.
 */
public final class EZEPPlacementScreen extends Screen {

    private static final int INV_W = 176;
    private static final int INV_H = 166;

    private static final int ICON_SIZE = 16;
    private static final int HUD_TEXT_PAD_X = 4;
    private static final int HUD_TEXT_PAD_Y = 4;
    private static final ItemStack EMERALD_ICON = new ItemStack(Items.EMERALD);

    private enum DragTarget {
        NONE,
        HUD,
        WITHDRAW_BUTTON
    }

    // Original values (for cancel)
    private boolean origHudEnabled;
    private int origHudX;
    private int origHudY;
    private double origHudScale;

    private boolean origBtnEnabled;
    private int origBtnOffX;
    private int origBtnOffY;

    // Working preview state
    private boolean hudEnabled;
    private int hudX;
    private int hudY;
    private double hudScale;

    private boolean btnEnabled;
    private int btnOffX;
    private int btnOffY;

    private DragTarget dragging = DragTarget.NONE;
    private int dragGrabDX = 0;
    private int dragGrabDY = 0;

    // Cached inventory outline position
    private int invLeft;
    private int invTop;

    // Widgets we need to update labels on
    private Button hudToggleBtn;
    private Button btnToggleBtn;

    public EZEPPlacementScreen() {
        super(Component.literal("EZ Emerald Pouch Placement Editor"));
    }

    @Override
    protected void init() {
        super.init();

        // Snapshot original config (for cancel)
        origHudEnabled = EZEPClientConfig.HUD_ENABLED.get();
        origHudX = EZEPClientConfig.HUD_X.get();
        origHudY = EZEPClientConfig.HUD_Y.get();
        origHudScale = EZEPClientConfig.HUD_SCALE.get();

        origBtnEnabled = EZEPClientConfig.WITHDRAW_BTN_ENABLED.get();
        origBtnOffX = EZEPClientConfig.WITHDRAW_BTN_OFFSET_X.get();
        origBtnOffY = EZEPClientConfig.WITHDRAW_BTN_OFFSET_Y.get();

        // Working copies
        hudEnabled = origHudEnabled;
        hudX = origHudX;
        hudY = origHudY;
        hudScale = origHudScale;

        btnEnabled = origBtnEnabled;
        btnOffX = origBtnOffX;
        btnOffY = origBtnOffY;

        // Center the fake inventory outline
        invLeft = Math.max(0, (this.width - INV_W) / 2);
        invTop = Math.max(0, (this.height - INV_H) / 2);

        // --- Button layout: 2 rows, centered, equal sizes ---
        // Row A (top): HUD Toggle, Withdraw Toggle, Reset
        // Row B (bottom): Save, Cancel
        //
        // All buttons same width/height and consistent gaps.
        final int btnW = 130;
        final int btnH = 20;
        final int gap = 8;

        // Determine Y positions near/below inventory, but keep on-screen
        int rowBWidth = (btnW * 2) + gap;              // Save + Cancel
        int rowAWidth = (btnW * 3) + (gap * 2);        // HUD + Withdraw + Reset

        int rowBLeft = Math.max(0, (this.width - rowBWidth) / 2);
        int rowALeft = Math.max(0, (this.width - rowAWidth) / 2);

        int padBottom = 10;
        int proposedRowB = invTop + INV_H + 20;
        int maxRowB = this.height - btnH - padBottom;
        int rowBY = Math.min(proposedRowB, maxRowB);

        int rowAY = rowBY - btnH - 6; // small vertical gap between rows
        if (rowAY < 48) rowAY = 48;   // keep away from top text a bit if very small screens

        // Row B buttons (Save, Cancel)
        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> onSave())
                .bounds(rowBLeft, rowBY, btnW, btnH)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onCancel())
                .bounds(rowBLeft + btnW + gap, rowBY, btnW, btnH)
                .build());

        // Row A buttons (HUD toggle, Withdraw toggle, Reset) - all same size
        hudToggleBtn = this.addRenderableWidget(Button.builder(hudToggleLabel(), b -> toggleHud())
                .bounds(rowALeft, rowAY, btnW, btnH)
                .build());

        btnToggleBtn = this.addRenderableWidget(Button.builder(btnToggleLabel(), b -> toggleWithdrawBtn())
                .bounds(rowALeft + btnW + gap, rowAY, btnW, btnH)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset"), b -> onReset())
                .bounds(rowALeft + (btnW + gap) * 2, rowAY, btnW, btnH)
                .build());

        ModConstants.LOG.info("[EZEP] Placement editor opened. invLeft={},invTop={}, size={}x{}", invLeft, invTop, INV_W, INV_H);
        ModConstants.LOG.info("[EZEP] Placement editor state: HUD(enabled={}, x={}, y={}, scale={}) BTN(enabled={}, offX={}, offY={})",
                hudEnabled, hudX, hudY, hudScale, btnEnabled, btnOffX, btnOffY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void onSave() {
        try {
            EZEPClientConfig.HUD_ENABLED.set(hudEnabled);
            EZEPClientConfig.HUD_X.set(hudX);
            EZEPClientConfig.HUD_Y.set(hudY);
            EZEPClientConfig.HUD_SCALE.set(hudScale);

            EZEPClientConfig.WITHDRAW_BTN_ENABLED.set(btnEnabled);
            EZEPClientConfig.WITHDRAW_BTN_OFFSET_X.set(btnOffX);
            EZEPClientConfig.WITHDRAW_BTN_OFFSET_Y.set(btnOffY);

            EZEPClientConfigSaver.saveClientConfigBestEffort();

            ModConstants.LOG.info("[EZEP] Placement saved. HUD(enabled={}, x={}, y={}, scale={}) BTN(enabled={}, offX={}, offY={})",
                    hudEnabled, hudX, hudY, hudScale, btnEnabled, btnOffX, btnOffY);

        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Placement save failed (non-fatal): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] Placement save failure details", t);
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.setScreen(null);
    }

    private void onCancel() {
        try {
            EZEPClientConfig.HUD_ENABLED.set(origHudEnabled);
            EZEPClientConfig.HUD_X.set(origHudX);
            EZEPClientConfig.HUD_Y.set(origHudY);
            EZEPClientConfig.HUD_SCALE.set(origHudScale);

            EZEPClientConfig.WITHDRAW_BTN_ENABLED.set(origBtnEnabled);
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

    private void onReset() {
        try {
            hudEnabled = true;
            hudX = EZEPClientConfig.DEFAULT_HUD_X;
            hudY = EZEPClientConfig.DEFAULT_HUD_Y;
            hudScale = EZEPClientConfig.DEFAULT_HUD_SCALE;

            btnEnabled = true;
            btnOffX = EZEPClientConfig.DEFAULT_WITHDRAW_BTN_OFFSET_X;
            btnOffY = EZEPClientConfig.DEFAULT_WITHDRAW_BTN_OFFSET_Y;

            updateToggleLabels();
            ModConstants.LOG.info("[EZEP] Placement editor reset to defaults (working state only).");
        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Reset failed (non-fatal): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] Reset failure details", t);
        }
    }

    private void toggleHud() {
        hudEnabled = !hudEnabled;
        updateToggleLabels();
        ModConstants.LOG.info("[EZEP] Placement editor toggled HUD -> {}", hudEnabled);
    }

    private void toggleWithdrawBtn() {
        btnEnabled = !btnEnabled;
        updateToggleLabels();
        ModConstants.LOG.info("[EZEP] Placement editor toggled Withdraw Button -> {}", btnEnabled);
    }

    private void updateToggleLabels() {
        try {
            if (hudToggleBtn != null) hudToggleBtn.setMessage(hudToggleLabel());
            if (btnToggleBtn != null) btnToggleBtn.setMessage(btnToggleLabel());
        } catch (Throwable t) {
            ModConstants.LOG.debug("[EZEP] updateToggleLabels failed (non-fatal).", t);
        }
    }

    private Component hudToggleLabel() {
        return Component.literal("HUD: " + (hudEnabled ? "Shown" : "Hidden"));
    }

    private Component btnToggleLabel() {
        return Component.literal("Withdraw: " + (btnEnabled ? "Shown" : "Hidden"));
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
            if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

            if (hudEnabled && isMouseOverHud(mouseX, mouseY)) {
                dragging = DragTarget.HUD;
                dragGrabDX = (int) mouseX - hudX;
                dragGrabDY = (int) mouseY - hudY;
                ModConstants.LOG.debug("[EZEP] Drag start HUD (grab {},{})", dragGrabDX, dragGrabDY);
                return true;
            }

            if (btnEnabled && isMouseOverWithdrawButton(mouseX, mouseY)) {
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
                nx = clamp(nx, -50, this.width - 10);
                ny = clamp(ny, -50, this.height - 10);

                hudX = nx;
                hudY = ny;
                return true;
            }

            if (dragging == DragTarget.WITHDRAW_BUTTON) {
                int nxAbs = (int) mouseX - dragGrabDX;
                int nyAbs = (int) mouseY - dragGrabDY;

                int nxOff = nxAbs - invLeft;
                int nyOff = nyAbs - invTop;

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
        return "123.4k";
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        // Render the blur/dim background ONCE.
        try {
            this.renderBackground(gg, mouseX, mouseY, partialTick);
        } catch (Throwable t) {
            ModConstants.LOG.debug("[EZEP] renderBackground failed (non-fatal).", t);
        }

        // High-contrast overlay so the editor is readable even with blur/dim.
        gg.fill(0, 0, this.width, this.height, 0xB0000000);

        Font font = Minecraft.getInstance().font;
        if (font != null) {
            gg.drawString(font, "EZ Emerald Pouch Placement Editor", 10, 10, 0xFFFFFF, true);
            gg.drawString(font, "Drag HUD preview + withdraw button preview.", 10, 24, 0xFFE0E0E0, false);
            gg.drawString(font, "Save applies to config. Cancel restores old values. ESC = Cancel.", 10, 36, 0xFFE0E0E0, false);
        }

        // Fake inventory outline
        drawOutlineRect(gg, invLeft, invTop, INV_W, INV_H, 0xFFFFFFFF);
        drawOutlineRect(gg, invLeft - 1, invTop - 1, INV_W + 2, INV_H + 2, 0xFF00FF00);

        drawCheckerInteriorHint(gg, invLeft + 1, invTop + 1, INV_W - 2, INV_H - 2);

        if (font != null) {
            gg.drawString(font, "Inventory (example outline)", invLeft + 6, invTop + 6, 0xFFFFFFFF, true);
        }

        // Withdraw button preview
        int btnX = invLeft + btnOffX;
        int btnY = invTop + btnOffY;

        if (btnEnabled) {
            gg.renderItem(EMERALD_ICON, btnX, btnY);

            if (isMouseOverWithdrawButton(mouseX, mouseY) || dragging == DragTarget.WITHDRAW_BUTTON) {
                gg.fill(btnX, btnY, btnX + ICON_SIZE, btnY + ICON_SIZE, 0x60FFFFFF);
                drawOutlineRect(gg, btnX, btnY, ICON_SIZE, ICON_SIZE, 0xFFFFFFFF);
            }
        } else {
            drawOutlineRect(gg, btnX, btnY, ICON_SIZE, ICON_SIZE, 0xFF888888);
            if (font != null) {
                gg.drawString(font, "Hidden", btnX + 2, btnY + 4, 0xFFAAAAAA, false);
            }
        }

        // HUD preview
        if (hudEnabled) {
            renderHudPreview(gg, mouseX, mouseY);
        } else {
            drawOutlineRect(gg, hudX, hudY, 64, 20, 0xFF888888);
            if (font != null) {
                gg.drawString(font, "HUD Hidden", hudX + 4, hudY + 6, 0xFFAAAAAA, false);
            }
        }

        // Render widgets/renderables last so they're on top (and not blurred).
        try {
            for (Renderable r : this.renderables) {
                r.render(gg, mouseX, mouseY, partialTick);
            }
        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Manual renderables render failed (non-fatal): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] Manual renderables failure details", t);
        }
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

            // Solid backing for readability
            gg.fill(-2, -2, baseW + 2, baseH + 2, 0xA0000000);
            gg.renderItem(EMERALD_ICON, 0, 0);

            gg.drawString(font, example, ICON_SIZE + HUD_TEXT_PAD_X, HUD_TEXT_PAD_Y, 0xFFFFFF, true);

            boolean hovered = isMouseOverHud(mouseX, mouseY);
            if (hovered || dragging == DragTarget.HUD) {
                gg.fill(0, 0, baseW, baseH, 0x40FFFFFF);
                drawOutlineRect(gg, 0, 0, baseW, baseH, 0xFFFFFFFF);
            }

            gg.pose().popPose();
        } catch (Throwable t) {
            ModConstants.LOG.debug("[EZEP] renderHudPreview failed (non-fatal).", t);
        }
    }

    private static void drawOutlineRect(GuiGraphics gg, int x, int y, int w, int h, int argb) {
        gg.fill(x, y, x + w, y + 1, argb);                 // top
        gg.fill(x, y + h - 1, x + w, y + h, argb);         // bottom
        gg.fill(x, y, x + 1, y + h, argb);                 // left
        gg.fill(x + w - 1, y, x + w, y + h, argb);         // right
    }

    private static void drawCheckerInteriorHint(GuiGraphics gg, int x, int y, int w, int h) {
        int c1 = 0x10202020;
        int c2 = 0x10101010;

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
