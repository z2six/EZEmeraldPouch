// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/event/EZEPClientModBusEvents.java
package org.z2six.ezemeraldpouch.event;

import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.z2six.ezemeraldpouch.ModConstants;
import org.z2six.ezemeraldpouch.client.EZEPKeyMappings;

/**
 * Mod-bus client-only event handlers.
 * (Key mappings are registered on the mod event bus, not the game event bus.)
 */
public final class EZEPClientModBusEvents {

    private EZEPClientModBusEvents() {
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        try {
            EZEPKeyMappings.createIfMissing();
            event.register(EZEPKeyMappings.OPEN_PLACEMENT_EDITOR);
            ModConstants.LOG.info("[EZEP] Registered key mapping: OPEN_PLACEMENT_EDITOR");
        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Key mapping registration failed (non-fatal): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] Key mapping registration failure details", t);
        }
    }
}
