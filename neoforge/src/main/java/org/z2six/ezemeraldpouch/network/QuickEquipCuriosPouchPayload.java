// MainFile: neoforge/src/main/java/org/z2six/ezemeraldpouch/network/QuickEquipCuriosPouchPayload.java
package org.z2six.ezemeraldpouch.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.z2six.ezemeraldpouch.ModConstants;
import org.z2six.ezemeraldpouch.registry.ModItems;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * C2S: Shift+RMB quick-equip the Emerald Pouch into Curios "pouch" slot (optional integration).
 *
 * Client sends:
 * - containerId
 * - slotIndex
 *
 * Server validates:
 * - player exists
 * - Curios loaded
 * - containerId matches
 * - slotIndex in range
 * - slot contains our emerald pouch item
 *
 * Then inserts into first empty Curios "pouch" slot using reflection.
 */
public record QuickEquipCuriosPouchPayload(int containerId, int slotIndex) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<QuickEquipCuriosPouchPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModConstants.MODID, "quick_equip_curios_pouch"));

    public static final StreamCodec<FriendlyByteBuf, QuickEquipCuriosPouchPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> {
                        int c = (msg == null) ? -1 : msg.containerId();
                        int s = (msg == null) ? -1 : msg.slotIndex();
                        buf.writeVarInt(c);
                        buf.writeVarInt(s);
                    },
                    buf -> new QuickEquipCuriosPouchPayload(buf.readVarInt(), buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final QuickEquipCuriosPouchPayload msg, final IPayloadContext ctx) {
        try {
            ctx.enqueueWork(() -> handleServer(msg, ctx));
        } catch (Throwable t) {
            ModConstants.LOG.error("[EZEP][QuickEquipCuriosPouchPayload] enqueueWork failed (non-fatal).", t);
        }
    }

    private static void handleServer(QuickEquipCuriosPouchPayload msg, IPayloadContext ctx) {
        ServerPlayer sp = null;
        try {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                ModConstants.LOG.warn("[EZEP][QuickEquipCuriosPouchPayload] ctx.player not a ServerPlayer.");
                return;
            }
            sp = player;

            if (msg == null) {
                ModConstants.LOG.debug("[EZEP][QuickEquipCuriosPouchPayload] msg null; ignoring.");
                return;
            }

            if (!ModList.get().isLoaded("curios")) {
                ModConstants.LOG.debug("[EZEP][QuickEquipCuriosPouchPayload] Curios not loaded; ignoring for {}.",
                        sp.getName().getString());
                return;
            }

            AbstractContainerMenu menu = sp.containerMenu;
            if (menu == null) {
                ModConstants.LOG.warn("[EZEP][QuickEquipCuriosPouchPayload] containerMenu null for {}.",
                        sp.getName().getString());
                return;
            }

            if (menu.containerId != msg.containerId()) {
                ModConstants.LOG.warn("[EZEP][QuickEquipCuriosPouchPayload] containerId mismatch for {}. client={} server={}",
                        sp.getName().getString(), msg.containerId(), menu.containerId);
                return;
            }

            int idx = msg.slotIndex();
            if (idx < 0 || idx >= menu.slots.size()) {
                ModConstants.LOG.warn("[EZEP][QuickEquipCuriosPouchPayload] invalid slotIndex {} from {} (slots={}).",
                        idx, sp.getName().getString(), menu.slots.size());
                return;
            }

            Slot slot = menu.slots.get(idx);
            if (slot == null) {
                ModConstants.LOG.warn("[EZEP][QuickEquipCuriosPouchPayload] slot null at index {} for {}.",
                        idx, sp.getName().getString());
                return;
            }

            ItemStack stack = slot.getItem();
            if (stack == null || stack.isEmpty()) {
                ModConstants.LOG.debug("[EZEP][QuickEquipCuriosPouchPayload] slot {} empty for {}; ignoring.",
                        idx, sp.getName().getString());
                return;
            }

            if (!stack.is(ModItems.EMERALD_POUCH.get())) {
                ModConstants.LOG.warn("[EZEP][QuickEquipCuriosPouchPayload] {} attempted quick-equip but slot {} is not emerald_pouch: {}",
                        sp.getName().getString(), idx, stack.getItem());
                return;
            }

            boolean inserted = tryInsertIntoCuriosPouchSlot(sp, stack);
            if (!inserted) {
                ModConstants.LOG.warn("[EZEP][Curios] Could not insert into Curios 'pouch' slot for {}. " +
                                "This usually means: slot type not present, no empty slots, or Curios validator rejected it.",
                        sp.getName().getString());
                return;
            }

            // Clear source slot after successful insertion
            try {
                slot.set(ItemStack.EMPTY);
                slot.setChanged();
            } catch (Throwable t) {
                ModConstants.LOG.warn("[EZEP][QuickEquipCuriosPouchPayload] Failed clearing source slot {} for {} (non-fatal).",
                        idx, sp.getName().getString(), t);
            }

            try {
                menu.broadcastChanges();
                sp.getInventory().setChanged();
            } catch (Throwable t) {
                ModConstants.LOG.debug("[EZEP][QuickEquipCuriosPouchPayload] broadcastChanges failed (non-fatal) for {}.",
                        sp.getName().getString(), t);
            }

            ModConstants.LOG.info("[EZEP] Quick-equipped Emerald Pouch into Curios 'pouch' slot for {}.",
                    sp.getName().getString());

        } catch (Throwable t) {
            ModConstants.LOG.error("[EZEP][QuickEquipCuriosPouchPayload] Server handler failed (non-fatal). player={}",
                    (sp == null ? "null" : sp.getName().getString()), t);
        }
    }

    /**
     * Reflection path (Curios 1.21.1+):
     * CuriosApi.getCuriosInventory(LivingEntity) -> Optional handler
     * handler.getStacksHandler("pouch") -> Optional stacksHandler
     * stacksHandler.getStacks() -> IDynamicStackHandler
     * dynamic.getSlots(), dynamic.getStackInSlot(i), dynamic.setStackInSlot(i, stack)
     */
    private static boolean tryInsertIntoCuriosPouchSlot(ServerPlayer player, ItemStack stack) {
        try {
            if (player == null || stack == null || stack.isEmpty()) return false;
            if (!ModList.get().isLoaded("curios")) return false;

            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getInv = curiosApi.getMethod("getCuriosInventory", Class.forName("net.minecraft.world.entity.LivingEntity"));
            Object invOptObj = getInv.invoke(null, player);

            if (!(invOptObj instanceof Optional<?> invOpt) || invOpt.isEmpty()) {
                ModConstants.LOG.warn("[EZEP][Curios] getCuriosInventory empty for {}.", player.getName().getString());
                return false;
            }

            Object curiosHandler = invOpt.get();
            if (curiosHandler == null) return false;

            Method getStacksHandler = curiosHandler.getClass().getMethod("getStacksHandler", String.class);
            Object stacksOptObj = getStacksHandler.invoke(curiosHandler, "pouch");

            if (!(stacksOptObj instanceof Optional<?> stacksOpt) || stacksOpt.isEmpty()) {
                ModConstants.LOG.warn("[EZEP][Curios] No stacks handler for slotType 'pouch' for {}. " +
                                "=> Your datapack slot/entity json is NOT being applied.",
                        player.getName().getString());
                return false;
            }

            Object stacksHandler = stacksOpt.get();
            if (stacksHandler == null) return false;

            Method getStacks = stacksHandler.getClass().getMethod("getStacks");
            Object dynamic = getStacks.invoke(stacksHandler);
            if (dynamic == null) {
                ModConstants.LOG.warn("[EZEP][Curios] stacksHandler.getStacks returned null for {}.", player.getName().getString());
                return false;
            }

            Method getSlots = dynamic.getClass().getMethod("getSlots");
            int slots = (int) getSlots.invoke(dynamic);
            if (slots <= 0) {
                ModConstants.LOG.warn("[EZEP][Curios] Dynamic handler has 0 slots for {}.", player.getName().getString());
                return false;
            }

            Method getStackInSlot = dynamic.getClass().getMethod("getStackInSlot", int.class);
            Method setStackInSlot = dynamic.getClass().getMethod("setStackInSlot", int.class, ItemStack.class);

            ItemStack toInsert = stack.copy();
            toInsert.setCount(1);

            for (int i = 0; i < slots; i++) {
                Object existingObj = getStackInSlot.invoke(dynamic, i);
                if (!(existingObj instanceof ItemStack existing)) continue;

                if (existing.isEmpty()) {
                    // If Curios rejects it internally, this still might "set" but then bounce back later.
                    // We'll log success; if it immediately ejects, that indicates validator/tag mismatch.
                    setStackInSlot.invoke(dynamic, i, toInsert);
                    ModConstants.LOG.info("[EZEP][Curios] Inserted pouch into 'pouch' slot index {} for {}.",
                            i, player.getName().getString());
                    return true;
                }
            }

            ModConstants.LOG.warn("[EZEP][Curios] No empty 'pouch' Curios slot for {} (slots={}).",
                    player.getName().getString(), slots);
            return false;

        } catch (Throwable t) {
            ModConstants.LOG.warn("[EZEP][Curios] tryInsertIntoCuriosPouchSlot failed (non-fatal).", t);
            return false;
        }
    }
}
