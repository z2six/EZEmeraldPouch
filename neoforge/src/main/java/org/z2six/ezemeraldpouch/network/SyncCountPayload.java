// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/network/SyncCountPayload.java
package org.z2six.ezemeraldpouch.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.z2six.ezemeraldpouch.ModConstants;
import org.z2six.ezemeraldpouch.client.EZEPClientState;

public record SyncCountPayload(long emeralds) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncCountPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, "sync_count"));

    public static final StreamCodec<FriendlyByteBuf, SyncCountPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> buf.writeLong(msg.emeralds),
                    buf -> new SyncCountPayload(buf.readLong())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SyncCountPayload msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            EZEPClientState.setEmeralds(msg.emeralds);
            ModConstants.LOG.debug("[EZEP] Client sync received emeralds={}", msg.emeralds);
        });
    }
}
