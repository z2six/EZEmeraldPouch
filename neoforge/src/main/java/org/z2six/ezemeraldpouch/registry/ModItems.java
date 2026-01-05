// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/registry/ModItems.java
package org.z2six.ezemeraldpouch.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.z2six.ezemeraldpouch.ModConstants;
import org.z2six.ezemeraldpouch.item.EmeraldPouchItem;

public final class ModItems {

    // NeoForge helper for Items registry
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ModConstants.MODID);

    public static final DeferredHolder<Item, Item> EMERALD_POUCH =
            ITEMS.register("emerald_pouch", () -> new EmeraldPouchItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ModConstants.LOG.info("[EZEP] Registering items...");
        ITEMS.register(modBus);
    }
}
