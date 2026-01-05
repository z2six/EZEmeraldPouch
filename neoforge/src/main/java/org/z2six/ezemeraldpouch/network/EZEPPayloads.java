// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/network/EZEPPayloads.java
package org.z2six.ezemeraldpouch.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.z2six.ezemeraldpouch.ModConstants;

public final class EZEPPayloads {

    private EZEPPayloads() {
    }

    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        ModConstants.LOG.info("[EZEP] Registering payload handlers...");
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                WithdrawRequestPayload.TYPE,
                WithdrawRequestPayload.STREAM_CODEC,
                WithdrawRequestPayload::handle
        );

        registrar.playToServer(
                DepositSlotRequestPayload.TYPE,
                DepositSlotRequestPayload.STREAM_CODEC,
                DepositSlotRequestPayload::handle
        );

        registrar.playToClient(
                SyncCountPayload.TYPE,
                SyncCountPayload.STREAM_CODEC,
                SyncCountPayload::handle
        );

        ModConstants.LOG.info("[EZEP] Payload handlers registered.");
    }
}
