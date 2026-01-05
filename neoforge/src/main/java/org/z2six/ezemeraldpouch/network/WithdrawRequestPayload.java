// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/network/WithdrawRequestPayload.java
package org.z2six.ezemeraldpouch.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.z2six.ezemeraldpouch.ModConstants;
import org.z2six.ezemeraldpouch.util.EmeraldPouchUtil;

public record WithdrawRequestPayload(int stacks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<WithdrawRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, "withdraw_request"));

    public static final StreamCodec<FriendlyByteBuf, WithdrawRequestPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> buf.writeInt(msg.stacks),
                    buf -> new WithdrawRequestPayload(buf.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final WithdrawRequestPayload msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) {
                ModConstants.LOG.warn("[EZEP] WithdrawRequestPayload from non-server player context");
                return;
            }

            int stacks = msg.stacks;
            if (stacks <= 0) stacks = 1;
            if (stacks > 64) stacks = 64;

            if (!EmeraldPouchUtil.hasPouchSomewhere(sp)) {
                ModConstants.LOG.debug("[EZEP] Withdraw denied: player {} has no pouch", sp.getGameProfile().getName());
                PacketDistributor.sendToPlayer(sp, new SyncCountPayload(EmeraldPouchUtil.getCount(sp)));
                return;
            }

            long requested = (long) stacks * 64L;
            long taken = EmeraldPouchUtil.withdrawEmeralds(sp, requested);

            if (taken <= 0L) {
                ModConstants.LOG.debug("[EZEP] Withdraw: nothing to take for {}", sp.getGameProfile().getName());
                PacketDistributor.sendToPlayer(sp, new SyncCountPayload(EmeraldPouchUtil.getCount(sp)));
                return;
            }

            long remaining = taken;
            while (remaining > 0) {
                int give = (int) Math.min(64L, remaining);
                ItemStack out = new ItemStack(Items.EMERALD, give);

                boolean inserted = sp.getInventory().add(out);
                if (!inserted && !out.isEmpty()) {
                    ItemEntity ent = sp.drop(out, false);
                    if (ent != null) {
                        ent.setNoPickUpDelay();
                        ent.setThrower(sp);
                    }
                    ModConstants.LOG.debug("[EZEP] Inventory full; dropped {} emeralds for {}", give, sp.getGameProfile().getName());
                }

                remaining -= give;
            }

            PacketDistributor.sendToPlayer(sp, new SyncCountPayload(EmeraldPouchUtil.getCount(sp)));
            ModConstants.LOG.debug("[EZEP] Withdraw success: stacks={} ({} emeralds) player={}", stacks, taken, sp.getGameProfile().getName());
        });
    }
}
