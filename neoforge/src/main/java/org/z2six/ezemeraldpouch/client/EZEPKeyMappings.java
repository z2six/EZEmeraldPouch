// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/client/EZEPKeyMappings.java
package org.z2six.ezemeraldpouch.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.z2six.ezemeraldpouch.ModConstants;

/**
 * Holds client key mappings.
 *
 * IMPORTANT:
 * - Registration must happen in RegisterKeyMappingsEvent (mod bus), client-side only.
 * - Usage should happen by listening to ClientTickEvent.Post and consuming clicks.
 */
public final class EZEPKeyMappings {

    /**
     * Translation keys:
     * - key.ezemeraldpouch.open_placement_editor
     * - key.categories.ezemeraldpouch
     *
     * You can optionally add them to en_us.json, but it is not required for functionality.
     */
    public static KeyMapping OPEN_PLACEMENT_EDITOR;

    private EZEPKeyMappings() {
    }

    public static void createIfMissing() {
        if (OPEN_PLACEMENT_EDITOR != null) return;

        // Default key: O (can be changed in Minecraft controls)
        OPEN_PLACEMENT_EDITOR = new KeyMapping(
                "key." + ModConstants.MODID + ".open_placement_editor",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_O,
                "key.categories." + ModConstants.MODID
        );

        ModConstants.LOG.info("[EZEP] Created key mapping OPEN_PLACEMENT_EDITOR (default: O).");
    }
}
