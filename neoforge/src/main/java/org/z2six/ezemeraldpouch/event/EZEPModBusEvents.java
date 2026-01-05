// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/event/EZEPModBusEvents.java
package org.z2six.ezemeraldpouch.event;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.z2six.ezemeraldpouch.ModConstants;
import org.z2six.ezemeraldpouch.registry.ModItems;

public final class EZEPModBusEvents {

    private EZEPModBusEvents() {
    }

    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        try {
            // Put the pouch in Tools & Utilities
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                event.accept(ModItems.EMERALD_POUCH.get());
                ModConstants.LOG.debug("[EZEP] Added Emerald Pouch to CreativeModeTabs.TOOLS_AND_UTILITIES");
            }
        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP] Creative tab injection failed (safe to ignore): {}", t.toString());
            ModConstants.LOG.debug("[EZEP] Creative tab injection failure details", t);
        }
    }
}
