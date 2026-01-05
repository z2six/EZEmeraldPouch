// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/client/EZEPClientEvents.java
package org.z2six.ezemeraldpouch.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.z2six.ezemeraldpouch.ModConstants;
import org.z2six.ezemeraldpouch.client.screen.EZEPPlacementScreen;
import org.z2six.ezemeraldpouch.config.EZEPClientConfig;
import org.z2six.ezemeraldpouch.network.DepositSlotRequestPayload;
import org.z2six.ezemeraldpouch.network.QuickEquipCuriosPouchPayload;
import org.z2six.ezemeraldpouch.network.WithdrawRequestPayload;
import org.z2six.ezemeraldpouch.registry.ModItems;

public final class EZEPClientEvents {

    private static final ResourceLocation WITHDRAW_ICON =
            ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, "textures/gui/withdraw_button.png");

    private static final int BTN_SIZE = 16;

    private EZEPClientEvents() {
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;

            // Consume clicks (per NeoForge docs).
            while (EZEPKeyMappings.OPEN_PLACEMENT_EDITOR != null && EZEPKeyMappings.OPEN_PLACEMENT_EDITOR.consumeClick()) {
                if (mc.screen instanceof EZEPPlacementScreen) {
                    // already open
                    ModConstants.LOG.debug("[EZEP] Placement editor key pressed, but editor is already open.");
                    continue;
                }

                // Open the placement editor
                ModConstants.LOG.info("[EZEP] Opening placement editor screen.");
                mc.setScreen(new EZEPPlacementScreen());
            }
        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] onClientTick failed (non-fatal): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] onClientTick failure details", t);
        }
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onHudRender(RenderGuiEvent.Post event) {
        if (!EZEPClientConfig.HUD_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.options.hideGui) return;

        // Hide regular HUD while using our placement editor flow.
        if (mc.screen instanceof EZEPPlacementScreen) return;

        GuiGraphics gg = event.getGuiGraphics();
        if (gg == null) return;

        int x = EZEPClientConfig.HUD_X.get();
        int y = EZEPClientConfig.HUD_Y.get();
        double scale = EZEPClientConfig.HUD_SCALE.get();

        long emeralds = EZEPClientState.getEmeralds();
        String text = abbreviate(emeralds);

        Font font = mc.font;
        if (font == null) return;

        gg.pose().pushPose();
        gg.pose().translate(x, y, 0);
        gg.pose().scale((float) scale, (float) scale, 1.0f);

        RenderSystem.enableBlend();
        gg.blit(WITHDRAW_ICON, 0, 0, 0, 0, 16, 16, 16, 16);

        int textX = 16 + 4;
        int textY = 4;
        gg.drawString(font, text, textX, textY, 0xFFFFFF, true);

        gg.pose().popPose();
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof InventoryScreen inv)) {
            return;
        }

        // Place the withdraw button based on config offsets relative to GUI top-left.
        int guiLeft;
        int guiTop;
        int guiW;
        int guiH;

        try {
            guiLeft = inv.getGuiLeft();
            guiTop = inv.getGuiTop();
            guiW = inv.getXSize();
            guiH = inv.getYSize();
        } catch (Throwable t) {
            // If something changes with mappings or another mod's screen subclass, don't crash.
            ModConstants.LOG.warn("[EZEP] InventoryScreen init: failed reading gui bounds (non-fatal): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] InventoryScreen init bounds failure details", t);
            return;
        }

        int offX = EZEPClientConfig.WITHDRAW_BTN_OFFSET_X.get();
        int offY = EZEPClientConfig.WITHDRAW_BTN_OFFSET_Y.get();

        // Best-effort clamp to stay within some reasonable range near the GUI.
        // We do NOT hard force it inside; if player wants it outside, allow it.
        int x = guiLeft + offX;
        int y = guiTop + offY;

        event.addListener(new WithdrawButtonWidget(x, y, BTN_SIZE, BTN_SIZE));
        ModConstants.LOG.debug("[EZEP] Added withdraw button to InventoryScreen at {},{} (offset {},{}) guiLeft={},guiTop={},w={},h={}",
                x, y, offX, offY, guiLeft, guiTop, guiW, guiH);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> cs)) return;

        // Right mouse button
        if (event.getButton() != 1) return;

        // Require Shift
        if (!Screen.hasShiftDown()) return;

        Slot hovered;
        try {
            hovered = cs.getSlotUnderMouse();
        } catch (Throwable t) {
            hovered = null;
        }
        if (hovered == null) return;

        ItemStack stack = hovered.getItem();
        if (stack == null || stack.isEmpty()) return;

        // A) Shift+RMB on our pouch item -> quick equip into Curios pouch slot (if Curios present)
        if (stack.is(ModItems.EMERALD_POUCH.get()) && ModList.get().isLoaded("curios")) {
            int containerId;
            int slotIndex;
            try {
                containerId = cs.getMenu().containerId;
                slotIndex = hovered.index;
            } catch (Throwable t) {
                ModConstants.LOG.debug("[EZEP] Failed reading containerId/slotIndex for pouch quick-equip (non-fatal).", t);
                return;
            }

            event.setCanceled(true);
            PacketDistributor.sendToServer(new QuickEquipCuriosPouchPayload(containerId, slotIndex));
            ModConstants.LOG.info("[EZEP] Sent quick-equip pouch request: containerId={} slotIndex={}", containerId, slotIndex);
            return;
        }

        // B) Shift+RMB on emerald / emerald block -> deposit to pouch
        boolean isEmerald = stack.is(Items.EMERALD);
        boolean isEmeraldBlock = stack.is(Items.EMERALD_BLOCK);
        if (!isEmerald && !isEmeraldBlock) return;

        event.setCanceled(true);

        int containerId = cs.getMenu().containerId;
        int slotIndex = hovered.index;

        PacketDistributor.sendToServer(new DepositSlotRequestPayload(containerId, slotIndex));
        ModConstants.LOG.debug("[EZEP] Client requested deposit for containerId={} slot={} item={}",
                containerId, slotIndex, stack.getItem().toString());
    }

    static void requestWithdrawStacks(int stacks) {
        if (stacks <= 0) stacks = 1;
        PacketDistributor.sendToServer(new WithdrawRequestPayload(stacks));
        ModConstants.LOG.debug("[EZEP] Client requested withdraw stacks={}", stacks);
    }

    static String abbreviate(long value) {
        if (value < 0) value = 0;
        if (value < 1000) return Long.toString(value);

        final String[] suffix = {"k", "m", "b", "t", "q"};
        double v = value;
        int i = -1;
        while (v >= 1000.0 && i < suffix.length - 1) {
            v /= 1000.0;
            i++;
        }

        if (v >= 10.0) {
            return ((long) v) + suffix[i];
        }
        return String.format(java.util.Locale.ROOT, "%.1f%s", v, suffix[i]);
    }

    private static final class WithdrawButtonWidget extends net.minecraft.client.gui.components.AbstractWidget {

        public WithdrawButtonWidget(int x, int y, int w, int h) {
            super(x, y, w, h, net.minecraft.network.chat.Component.empty());
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            RenderSystem.enableBlend();
            gg.blit(WITHDRAW_ICON, getX(), getY(), 0, 0, BTN_SIZE, BTN_SIZE, BTN_SIZE, BTN_SIZE);

            if (isHoveredOrFocused()) {
                gg.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x40FFFFFF);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            int stacks = Screen.hasShiftDown() ? 10 : 1;
            requestWithdrawStacks(stacks);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput out) {
            // no narration
        }
    }
}
