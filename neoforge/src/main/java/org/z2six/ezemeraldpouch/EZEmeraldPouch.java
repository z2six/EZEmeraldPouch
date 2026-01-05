// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/EZEmeraldPouch.java
package org.z2six.ezemeraldpouch;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import org.z2six.ezemeraldpouch.client.EZEPClientEvents;
import org.z2six.ezemeraldpouch.config.EZEPClientConfig;
import org.z2six.ezemeraldpouch.data.ModAttachments;
import org.z2six.ezemeraldpouch.event.EZEPClientModBusEvents;
import org.z2six.ezemeraldpouch.event.EZEPCommonEvents;
import org.z2six.ezemeraldpouch.event.EZEPModBusEvents;
import org.z2six.ezemeraldpouch.network.EZEPPayloads;
import org.z2six.ezemeraldpouch.registry.ModItems;

@Mod(ModConstants.MODID)
public final class EZEmeraldPouch {

    public EZEmeraldPouch(IEventBus modBus, ModContainer container) {
        ModConstants.LOG.info("[EZEP] Constructing mod. modid={}", ModConstants.MODID);

        // Registries
        ModItems.register(modBus);
        ModAttachments.register(modBus);

        // Networking
        modBus.addListener(EZEPPayloads::registerPayloads);

        // Creative tabs (mod bus)
        modBus.addListener(EZEPModBusEvents::onBuildCreativeTabContents);

        // Config
        if (FMLEnvironment.dist.isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, EZEPClientConfig.SPEC);
            ModConstants.LOG.info("[EZEP] Registered CLIENT config");

            // Client-only mod-bus events (key mapping registration)
            modBus.addListener(EZEPClientModBusEvents::onRegisterKeyMappings);
            ModConstants.LOG.info("[EZEP] Registered client mod-bus listeners");
        }

        // Game events
        NeoForge.EVENT_BUS.register(EZEPCommonEvents.class);

        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.register(EZEPClientEvents.class);
        }

        ModConstants.LOG.info("[EZEP] Mod constructed successfully.");
    }
}
