// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/client/EZEPClientEvents.java
package org.z2six.ezemeraldpouch.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.z2six.ezemeraldpouch.ModConstants;
import org.z2six.ezemeraldpouch.config.EZEPClientConfig;
import org.z2six.ezemeraldpouch.network.DepositSlotRequestPayload;
import org.z2six.ezemeraldpouch.network.WithdrawRequestPayload;

public final class EZEPClientEvents {

    private static final ResourceLocation WITHDRAW_ICON =
            ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, "textures/gui/withdraw_button.png");

    // Button layout
    private static final int BTN_SIZE = 16;
    private static final int BTN_PAD = 4;

    private EZEPClientEvents() {
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onHudRender(RenderGuiEvent.Post event) {
        if (!EZEPClientConfig.HUD_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (mc.options.hideGui) return;

        GuiGraphics gg = event.getGuiGraphics();
        if (gg == null) return;

        int x = EZEPClientConfig.HUD_X.get();
        int y = EZEPClientConfig.HUD_Y.get();
        double scale = EZEPClientConfig.HUD_SCALE.get();

        long emeralds = EZEPClientState.getEmeralds();
        String text = "Emeralds: " + abbreviate(emeralds);

        Font font = mc.font;
        if (font == null) return;

        gg.pose().pushPose();
        gg.pose().translate(x, y, 0);
        gg.pose().scale((float) scale, (float) scale, 1.0f);

        gg.drawString(font, text, 0, 0, 0xFFFFFF, true);

        gg.pose().popPose();
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen inv)) {
            return;
        }

        int left = inv.getGuiLeft();
        int top = inv.getGuiTop();
        int x = left + inv.getXSize() - BTN_SIZE - BTN_PAD;
        int y = top + BTN_PAD;

        event.addListener(new WithdrawButtonWidget(x, y, BTN_SIZE, BTN_SIZE));
        ModConstants.LOG.debug("[EZEP] Added withdraw button to InventoryScreen at {},{}", x, y);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> cs)) return;

        // Shift + Right click (RMB == 1) on emerald stack deposits it
        if (!Screen.hasShiftDown()) return;
        if (event.getButton() != 1) return;

        Slot hovered = cs.getSlotUnderMouse();
        if (hovered == null) return;

        ItemStack stack = hovered.getItem();
        if (stack.isEmpty() || !stack.is(Items.EMERALD)) return;

        event.setCanceled(true);

        int containerId = cs.getMenu().containerId;
        int slotIndex = hovered.index;

        PacketDistributor.sendToServer(new DepositSlotRequestPayload(containerId, slotIndex));
        ModConstants.LOG.debug("[EZEP] Client requested deposit for containerId={} slot={}", containerId, slotIndex);
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

    /**
     * Minimal image button widget.
     */
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
