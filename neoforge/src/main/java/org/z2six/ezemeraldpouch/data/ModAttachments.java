// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/data/ModAttachments.java
package org.z2six.ezemeraldpouch.data;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.z2six.ezemeraldpouch.ModConstants;

import java.util.function.Supplier;

public final class ModAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.ATTACHMENT_TYPES, ModConstants.MODID);

    public static final Supplier<AttachmentType<EmeraldPouchData>> EMERALD_POUCH =
            ATTACHMENTS.register("emerald_pouch", () ->
                    AttachmentType.<EmeraldPouchData>builder((java.util.function.Supplier<EmeraldPouchData>) EmeraldPouchData::new)
                            .serialize(EmeraldPouchData.CODEC)
                            .copyOnDeath()
                            .build()
            );

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        ModConstants.LOG.info("[EZEP] Registering attachments...");
        ATTACHMENTS.register(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, path);
    }
}
