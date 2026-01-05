// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/network/DepositSlotRequestPayload.java
package org.z2six.ezemeraldpouch.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.z2six.ezemeraldpouch.ModConstants;
import org.z2six.ezemeraldpouch.util.EmeraldPouchUtil;

public record DepositSlotRequestPayload(int containerId, int slotIndex) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DepositSlotRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, "deposit_slot"));

    public static final StreamCodec<FriendlyByteBuf, DepositSlotRequestPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> {
                        buf.writeInt(msg.containerId);
                        buf.writeInt(msg.slotIndex);
                    },
                    buf -> new DepositSlotRequestPayload(buf.readInt(), buf.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final DepositSlotRequestPayload msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) {
                ModConstants.LOG.warn("[EZEP] DepositSlotRequestPayload from non-server player context");
                return;
            }

            if (!EmeraldPouchUtil.hasPouchSomewhere(sp)) {
                ModConstants.LOG.debug("[EZEP] Deposit denied: player {} has no pouch", sp.getGameProfile().getName());
                PacketDistributor.sendToPlayer(sp, new SyncCountPayload(EmeraldPouchUtil.getCount(sp)));
                return;
            }

            AbstractContainerMenu menu = sp.containerMenu;
            if (menu == null) {
                ModConstants.LOG.warn("[EZEP] Deposit denied: containerMenu null for {}", sp.getGameProfile().getName());
                return;
            }

            if (menu.containerId != msg.containerId) {
                ModConstants.LOG.debug("[EZEP] Deposit denied: containerId mismatch client={} server={} player={}",
                        msg.containerId, menu.containerId, sp.getGameProfile().getName());
                return;
            }

            int idx = msg.slotIndex;
            if (idx < 0 || idx >= menu.slots.size()) {
                ModConstants.LOG.debug("[EZEP] Deposit denied: slotIndex out of range idx={} size={} player={}",
                        idx, menu.slots.size(), sp.getGameProfile().getName());
                return;
            }

            Slot slot = menu.getSlot(idx);
            if (slot == null) {
                ModConstants.LOG.debug("[EZEP] Deposit denied: slot null idx={} player={}", idx, sp.getGameProfile().getName());
                return;
            }

            ItemStack stack = slot.getItem();
            if (!EmeraldPouchUtil.isEmerald(stack)) {
                ModConstants.LOG.debug("[EZEP] Deposit ignored: slot {} not emerald ({}). player={}",
                        idx, stack.isEmpty() ? "empty" : stack.getItem().toString(), sp.getGameProfile().getName());
                return;
            }

            int count = stack.getCount();
            if (count <= 0) return;

            slot.set(ItemStack.EMPTY);
            menu.broadcastChanges();

            EmeraldPouchUtil.depositEmeralds(sp, count);
            PacketDistributor.sendToPlayer(sp, new SyncCountPayload(EmeraldPouchUtil.getCount(sp)));

            ModConstants.LOG.debug("[EZEP] Deposited {} emeralds from slot {} for player={}", count, idx, sp.getGameProfile().getName());
        });
    }
}
